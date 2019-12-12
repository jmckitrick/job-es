-- name: get-travel-bookings
-- Get columns for elastic search from travel bookings.
SELECT
 tb.id,
 tb.licensee_id licensee,
 tb.product_type product,
 tb.product_sub_type product_sub_type,
 tb.adapter_name adapter,
 tb.agent_name agent,
 tb.subsite_agent_name subsite,
 tb.branch_name branch,
 tb.vendor_confirmation ref,
 tb.itinerary_name itinerary,
 t.first_name fname,
 t.last_name lname,
 t.email,
 t.phone
FROM TRAVEL_BOOKING tb
INNER JOIN TRAVELLER t ON t.booking_id = tb.id
WHERE t.is_primary = 1
AND tb.create_date >= :start_date
AND tb.create_date <  :end_date
-- AND tb.create_date >= '2019-01-01 00:00:00'
-- AND tb.create_date <  '2019-06-08 00:00:00'
-- AND tb.create_date >= '2019-06-08 00:00:00'
-- AND tb.create_date <  '2019-06-15 00:00:00'
-- AND tb.create_date >= '2019-06-15 00:00:00'
-- AND tb.create_date <  '2019-06-22 00:00:00'
-- AND tb.create_date >= '2019-06-22 00:00:00'
-- AND tb.create_date <  '2019-06-22 00:00:00'
-- AND tb.agent_name IS NOT NULL
-- AND tb.subsite_agent_name IS NOT NULL
-- AND (tb.agent_name IS NOT NULL OR tb.subsite_agent_name IS NOT NULL)
-- AND tb.create_date >= '2019-07-01 00:00:00'
-- AND tb.create_date <  '2019-08-01 00:00:00'
-- LIMIT 1000
