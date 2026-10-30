-- F4-05: what left the phone for each receipt photo, shown to the person in the Akun tab.

alter table public.receipts
  add column sent_bytes int,
  add column sent_width int,
  add column sent_height int,
  add column redactions int not null default 0;
