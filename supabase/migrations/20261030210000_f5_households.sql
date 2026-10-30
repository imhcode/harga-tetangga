-- F5: one household shares prices, products, stores, shopping lists, estimates and trips.
-- Receipts (and their photos' egress log) stay private to the person who sent them.
-- Joining needs the owner's approval (H7). All membership changes go through the RPCs below.

create table public.households (
  id uuid primary key default gen_random_uuid(),
  name text not null default 'Rumah',
  owner_id uuid not null default auth.uid() references auth.users on delete cascade,
  invite_code text not null unique,
  created_at timestamptz not null default now()
);

create table public.household_members (
  household_id uuid not null references public.households on delete cascade,
  user_id uuid not null references auth.users on delete cascade,
  email text,
  role text not null check (role in ('owner', 'member')),
  status text not null check (status in ('pending', 'active')),
  requested_at timestamptz not null default now(),
  approved_at timestamptz,
  primary key (household_id, user_id),
  unique (user_id) -- one household per person
);

-- Security definer helpers avoid recursive policies on household_members.
create function public.my_household_id(active_only boolean default true) returns uuid
language sql stable security definer set search_path = public as $$
  select household_id from household_members
  where user_id = auth.uid() and (not active_only or status = 'active')
$$;

-- The caller plus every active member of the caller's household.
create function public.household_peers() returns setof uuid
language sql stable security definer set search_path = public as $$
  select auth.uid()
  union
  select m.user_id from household_members m
  where m.status = 'active' and m.household_id = (select my_household_id(true))
$$;

alter table public.households enable row level security;
alter table public.household_members enable row level security;
create policy "members see their household" on public.households for select to authenticated
  using (id = (select public.my_household_id(false)));
create policy "members see each other" on public.household_members for select to authenticated
  using (user_id = (select auth.uid()) or household_id = (select public.my_household_id(true)));

-- Shared tables: replace "owner only" with "anyone in my household".
do $$
declare t text;
begin
  foreach t in array array['stores', 'products', 'product_aliases', 'price_observations',
                           'shopping_lists', 'shopping_list_items', 'estimates', 'trips'] loop
    execute format('drop policy "owner can do everything" on public.%I', t);
    execute format(
      'create policy "household can do everything" on public.%I for all to authenticated
         using (owner_id in (select public.household_peers()))
         with check (owner_id in (select public.household_peers()))', t);
  end loop;
end $$;

create function public.create_household(p_name text default 'Rumah') returns public.households
language plpgsql security definer set search_path = public as $$
declare
  v households%rowtype;
begin
  if exists (select 1 from household_members where user_id = auth.uid()) then
    raise exception 'Kamu sudah tergabung atau sedang menunggu persetujuan di rumah tangga lain';
  end if;
  insert into households (name, owner_id, invite_code)
  values (coalesce(nullif(trim(p_name), ''), 'Rumah'), auth.uid(), upper(substr(md5(gen_random_uuid()::text), 1, 6)))
  returning * into v;
  insert into household_members (household_id, user_id, email, role, status, approved_at)
  values (v.id, auth.uid(), auth.jwt() ->> 'email', 'owner', 'active', now());
  return v;
end $$;

-- Joining only creates a pending request; nothing is shared until the owner approves.
create function public.request_join(p_code text) returns void
language plpgsql security definer set search_path = public as $$
declare
  v_household uuid;
begin
  select id into v_household from households where invite_code = upper(trim(p_code));
  if v_household is null then
    raise exception 'Kode undangan tidak ditemukan';
  end if;
  if exists (select 1 from household_members where user_id = auth.uid()) then
    raise exception 'Kamu sudah tergabung atau sedang menunggu persetujuan';
  end if;
  insert into household_members (household_id, user_id, email, role, status)
  values (v_household, auth.uid(), auth.jwt() ->> 'email', 'member', 'pending');
end $$;

-- H7: only the owner decides.
create function public.decide_member(p_user_id uuid, p_approve boolean) returns void
language plpgsql security definer set search_path = public as $$
declare
  v_household uuid;
begin
  select id into v_household from households where owner_id = auth.uid();
  if v_household is null then
    raise exception 'Hanya pemilik rumah tangga yang bisa menyetujui';
  end if;
  if p_approve then
    update household_members set status = 'active', approved_at = now()
    where household_id = v_household and user_id = p_user_id and status = 'pending';
  else
    delete from household_members
    where household_id = v_household and user_id = p_user_id and role = 'member';
  end if;
end $$;

-- A member can leave (or cancel a pending request). The owner cannot leave their own household.
create function public.leave_household() returns void
language plpgsql security definer set search_path = public as $$
begin
  delete from household_members where user_id = auth.uid() and role = 'member';
end $$;

-- F5-04: products the household marks as important, for the monthly report filter.
alter table public.products add column important boolean not null default false;

-- F5: confirm_receipt now reuses stores and products other household members already saved.
-- Saves a reviewed receipt in one transaction: store, new products, aliases, prices, review stats.
-- p_lines: [{raw_name, product_id | null, new_product: {name, brand, size_value, size_unit, category} | null,
--            quantity, unit_price, line_total, auto_accepted, corrected}]
create or replace function public.confirm_receipt(
  p_receipt_id uuid,
  p_store jsonb,
  p_purchase_date date,
  p_lines jsonb,
  p_verify_ms int
) returns int
language plpgsql
security invoker
set search_path = public
as $$
declare
  v_store uuid;
  v_line jsonb;
  v_product uuid;
  v_count int := 0;
begin
  if not exists (select 1 from receipts where id = p_receipt_id and status = 'draft') then
    raise exception 'Receipt % not found or already confirmed', p_receipt_id;
  end if;

  v_store := nullif(p_store ->> 'id', '')::uuid;
  -- Reuse a store another household member already saved (rows visible through RLS).
  if v_store is null then
    select id into v_store from stores
    where lower(name) = lower(trim(p_store ->> 'name'))
      and coalesce(lower(branch), '') = coalesce(lower(nullif(trim(p_store ->> 'branch'), '')), '')
    limit 1;
  end if;
  if v_store is null then
    insert into stores (name, branch, kind)
    values (
      trim(p_store ->> 'name'),
      nullif(trim(p_store ->> 'branch'), ''),
      coalesce(nullif(p_store ->> 'kind', ''), 'lainnya')
    )
    on conflict (owner_id, name, branch) do update set kind = excluded.kind
    returning id into v_store;
  end if;

  for v_line in select * from jsonb_array_elements(p_lines) loop
    v_product := nullif(v_line ->> 'product_id', '')::uuid;
    -- Same product name already in the household catalog: link to it instead of duplicating.
    if v_product is null then
      select id into v_product from products
      where lower(name) = lower(trim(v_line -> 'new_product' ->> 'name'))
      limit 1;
    end if;
    if v_product is null then
      insert into products (name, brand, size_value, size_unit, category)
      values (
        trim(v_line -> 'new_product' ->> 'name'),
        nullif(v_line -> 'new_product' ->> 'brand', ''),
        (v_line -> 'new_product' ->> 'size_value')::numeric,
        nullif(v_line -> 'new_product' ->> 'size_unit', ''),
        nullif(v_line -> 'new_product' ->> 'category', '')
      )
      on conflict (owner_id, name) do update set name = excluded.name
      returning id into v_product;
    end if;

    insert into product_aliases (store_id, raw_name, product_id)
    values (v_store, upper(regexp_replace(trim(v_line ->> 'raw_name'), '\s+', ' ', 'g')), v_product)
    on conflict (owner_id, store_id, raw_name) do update
      set product_id = excluded.product_id,
          confirmations = product_aliases.confirmations + 1,
          updated_at = now();

    insert into price_observations (product_id, store_id, receipt_id, unit_price, quantity, line_total, observed_on, raw_name)
    values (
      v_product,
      v_store,
      p_receipt_id,
      (v_line ->> 'unit_price')::numeric,
      (v_line ->> 'quantity')::numeric,
      (v_line ->> 'line_total')::numeric,
      coalesce(p_purchase_date, current_date),
      v_line ->> 'raw_name'
    );
    v_count := v_count + 1;
  end loop;

  update receipts set
    status = 'verified',
    store_id = v_store,
    purchase_date = p_purchase_date,
    verified_at = now(),
    verify_ms = p_verify_ms,
    lines_total = jsonb_array_length(p_lines),
    lines_auto_accepted = (select count(*) from jsonb_array_elements(p_lines) e where (e ->> 'auto_accepted')::boolean),
    lines_corrected = (select count(*) from jsonb_array_elements(p_lines) e where (e ->> 'corrected')::boolean)
  where id = p_receipt_id;

  return v_count;
end;
$$;
