-- F1-11: live accuracy from real use, computed from what people confirmed on the Review screen.
-- Run: npx supabase db query --linked -f tools/metrics.sql
-- Runs as the database owner, so it covers every user's verified receipts.

with verified as (
  select * from receipts where status = 'verified'
),
extracted as (
  select
    v.id as receipt_id,
    l ->> 'raw_name' as raw_name,
    (l ->> 'quantity')::numeric as quantity,
    coalesce(
      (l ->> 'unit_price')::numeric,
      round((l ->> 'line_total')::numeric / nullif((l ->> 'quantity')::numeric, 0))
    ) as unit_price,
    (l ->> 'line_total')::numeric as line_total
  from verified v, jsonb_array_elements(v.extraction -> 'receipt' -> 'lines') l
  where l ->> 'kind' = 'item'
),
compared as (
  -- A line was read correctly when the confirmed numbers equal what the model read.
  select
    o.quantity is not distinct from e.quantity
      and o.unit_price = e.unit_price
      and o.line_total is not distinct from e.line_total as values_ok
  from price_observations o
  join extracted e on e.receipt_id = o.receipt_id and e.raw_name = o.raw_name
)
select
  (select count(*) from verified) as receipts,
  (select sum(lines_total) from verified) as lines_saved,
  (select round(100.0 * avg(values_ok::int), 1) from compared) as numbers_read_correctly_pct,
  (select round(100.0 * sum(lines_auto_accepted) / nullif(sum(lines_total), 0), 1) from verified) as auto_accepted_pct,
  (select round(100.0 * sum(lines_corrected) / nullif(sum(lines_total), 0), 1) from verified) as corrected_by_person_pct,
  (select count(*) filter (where escalated) from verified) as escalated_receipts,
  (select round(avg(extract_ms) / 1000.0, 1) from verified) as avg_read_seconds,
  (select round(avg(verify_ms) / 1000.0, 1) from verified) as avg_review_seconds;
