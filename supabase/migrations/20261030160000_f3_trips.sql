-- F3: one shopping trip = the estimate taken before leaving + the receipt after.
-- Written only after the person checked the "outside the plan" marks on the Trip Result screen.

create table public.trips (
  id uuid primary key default gen_random_uuid(),
  owner_id uuid not null default auth.uid() references auth.users on delete cascade,
  list_id uuid not null references public.shopping_lists on delete cascade,
  estimate_id uuid references public.estimates on delete set null,
  receipt_id uuid not null unique references public.receipts on delete cascade,
  bring numeric,
  receipt_total numeric not null,
  -- Items on the list that were bought and had a price in the estimate: estimated vs paid.
  planned_estimated numeric not null default 0,
  planned_actual numeric not null default 0,
  items_bought int not null,
  items_off_plan int not null,
  off_plan_total numeric not null,
  items_not_bought int not null,
  details jsonb not null,
  created_at timestamptz not null default now()
);

alter table public.trips enable row level security;
create policy "owner can do everything" on public.trips for all to authenticated
  using (owner_id = (select auth.uid())) with check (owner_id = (select auth.uid()));
