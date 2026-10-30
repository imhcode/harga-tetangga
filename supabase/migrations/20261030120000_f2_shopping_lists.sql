-- F2: shopping list -> cash estimate and cheapest store plan.

create table public.shopping_lists (
  id uuid primary key default gen_random_uuid(),
  owner_id uuid not null default auth.uid() references auth.users on delete cascade,
  name text not null default 'Belanja',
  status text not null default 'draft' check (status in ('draft', 'shopping', 'done')),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

-- product_id is null when the item has no product (and so no price data) yet.
-- Items reach this table only after the person ticked them (H4).
create table public.shopping_list_items (
  id uuid primary key default gen_random_uuid(),
  owner_id uuid not null default auth.uid() references auth.users on delete cascade,
  list_id uuid not null references public.shopping_lists on delete cascade,
  product_id uuid references public.products on delete set null,
  label text not null,
  quantity numeric not null default 1 check (quantity > 0),
  source text not null default 'text' check (source in ('text', 'voice', 'menu', 'agent')),
  created_at timestamptz not null default now()
);
create index shopping_list_items_list on public.shopping_list_items (list_id);

-- Snapshot taken when the person taps "Berangkat belanja", compared later with the receipt total.
create table public.estimates (
  id uuid primary key default gen_random_uuid(),
  owner_id uuid not null default auth.uid() references auth.users on delete cascade,
  list_id uuid not null references public.shopping_lists on delete cascade,
  buffer_pct int not null,
  extra_store_cost numeric not null,
  total numeric not null,
  low numeric not null,
  high numeric not null,
  bring numeric not null,
  items_total int not null,
  items_missing int not null,
  plan jsonb not null,
  created_at timestamptz not null default now()
);

do $$
declare t text;
begin
  foreach t in array array['shopping_lists', 'shopping_list_items', 'estimates'] loop
    execute format('alter table public.%I enable row level security', t);
    execute format(
      'create policy "owner can do everything" on public.%I for all to authenticated
         using (owner_id = (select auth.uid())) with check (owner_id = (select auth.uid()))', t);
  end loop;
end $$;
