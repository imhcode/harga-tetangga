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
  (select round(avg(verify_ms) / 1000.0, 1) from verified) as avg_review_seconds,
  -- F3: trips (estimate before leaving vs receipt after).
  (select count(*) from trips) as trips,
  (select count(*) filter (where bring >= receipt_total) from trips) as trips_cash_was_enough,
  -- Median absolute error of the estimate on planned items that had a price, in percent of what was paid.
  (select round(100.0 * percentile_cont(0.5) within group (order by abs(planned_actual - planned_estimated) / planned_actual)::numeric, 1)
     from trips where planned_actual > 0 and planned_estimated > 0) as estimate_median_error_pct,
  (select round(100.0 * sum(items_off_plan) / nullif(sum(items_bought), 0), 1) from trips) as off_plan_items_pct,
  (select round(100.0 * sum(off_plan_total) / nullif(sum(receipt_total), 0), 1) from trips) as off_plan_spend_pct;
