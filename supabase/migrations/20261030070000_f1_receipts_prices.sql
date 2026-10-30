-- F1: receipt photo -> reviewed prices.
-- Every row belongs to one user for now (owner_id). Households and sharing arrive in F5.

create extension if not exists pg_trgm with schema extensions;

create table public.stores (
  id uuid primary key default gen_random_uuid(),
  owner_id uuid not null default auth.uid() references auth.users on delete cascade,
  name text not null,
  branch text,
  kind text not null default 'lainnya'
    check (kind in ('minimarket', 'supermarket', 'grosir', 'warung', 'lainnya')),
  created_at timestamptz not null default now(),
  unique nulls not distinct (owner_id, name, branch)
);

-- The catalog grows from confirmed receipts: the model proposes a canonical product,
-- a person confirms it on the Review screen. No invented SKUs are seeded.
create table public.products (
  id uuid primary key default gen_random_uuid(),
  owner_id uuid not null default auth.uid() references auth.users on delete cascade,
  name text not null,
  brand text,
  size_value numeric,
  size_unit text check (size_unit in ('g', 'kg', 'ml', 'l', 'pcs')),
  category text,
  created_at timestamptz not null default now(),
  unique (owner_id, name)
);

-- Printed receipt name -> product. Only written by confirm_receipt, i.e. after a person confirmed it.
-- raw_name is normalized: upper case, single spaces, trimmed (same rule as normalizeRawName in Edge Functions).
create table public.product_aliases (
  id uuid primary key default gen_random_uuid(),
  owner_id uuid not null default auth.uid() references auth.users on delete cascade,
  store_id uuid references public.stores on delete cascade,
  raw_name text not null,
  product_id uuid not null references public.products on delete cascade,
  confirmations int not null default 1,
  updated_at timestamptz not null default now(),
  unique nulls not distinct (owner_id, store_id, raw_name)
);
create index product_aliases_raw_name_trgm on public.product_aliases
  using gin (raw_name extensions.gin_trgm_ops);

create table public.receipts (
  id uuid primary key default gen_random_uuid(),
  owner_id uuid not null default auth.uid() references auth.users on delete cascade,
  store_id uuid references public.stores on delete set null,
  purchase_date date,
  printed_total numeric,
  status text not null default 'draft' check (status in ('draft', 'verified', 'discarded')),
  -- Raw model output, kept to measure accuracy against what the person confirmed.
  extraction jsonb not null,
  model text,
  effort text,
  escalated boolean not null default false,
  extract_ms int,
  lines_total int,
  lines_auto_accepted int,
  lines_corrected int,
  verify_ms int,
  created_at timestamptz not null default now(),
  verified_at timestamptz
);

create table public.price_observations (
  id uuid primary key default gen_random_uuid(),
  owner_id uuid not null default auth.uid() references auth.users on delete cascade,
  product_id uuid not null references public.products on delete cascade,
  store_id uuid not null references public.stores on delete cascade,
  receipt_id uuid references public.receipts on delete cascade,
  unit_price numeric not null check (unit_price > 0),
  quantity numeric,
  line_total numeric,
  observed_on date not null,
  source text not null default 'receipt' check (source in ('receipt', 'manual', 'voice')),
  raw_name text,
  created_at timestamptz not null default now()
);
create index price_observations_lookup on public.price_observations (owner_id, product_id, observed_on desc);

-- Row level security: each user sees and writes only their own rows.
do $$
declare t text;
begin
  foreach t in array array['stores', 'products', 'product_aliases', 'receipts', 'price_observations'] loop
    execute format('alter table public.%I enable row level security', t);
    execute format(
      'create policy "owner can do everything" on public.%I for all to authenticated
         using (owner_id = (select auth.uid())) with check (owner_id = (select auth.uid()))', t);
  end loop;
end $$;

-- Latest price of each product at each store.
create view public.latest_prices with (security_invoker = true) as
select distinct on (o.product_id, o.store_id)
  o.product_id,
  p.name as product_name,
  p.category,
  o.store_id,
  s.name as store_name,
  s.branch as store_branch,
  o.unit_price,
  o.observed_on,
  o.source
from public.price_observations o
join public.products p on p.id = o.product_id
join public.stores s on s.id = o.store_id
order by o.product_id, o.store_id, o.observed_on desc, o.created_at desc;

-- Saves a reviewed receipt in one transaction: store, new products, aliases, prices, review stats.
-- p_lines: [{raw_name, product_id | null, new_product: {name, brand, size_value, size_unit, category} | null,
--            quantity, unit_price, line_total, auto_accepted, corrected}]
create function public.confirm_receipt(
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
