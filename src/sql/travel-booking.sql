-- name: get-travel-bookings
-- Get travel bookings from June 2019 onward.
SELECT
 tb.id,
 tb.licensee_id licensee,
 tb.product_type product,
 tb.adapter_name adapter,
 tb.agent_name agent,
 tb.subsite_agent_name,
 tb.branch_name branch,
 tb.vendor_confirmation ref_conf_number,
 tb.itinerary_name itinerary,
 t.first_name fname,
 t.last_name lname,
 t.email,
 t.phone
FROM TRAVEL_BOOKING tb
INNER JOIN TRAVELLER t ON t.booking_id = tb.id
WHERE t.is_primary = 1
AND tb.create_date > '2019-06-01 00:00:00'
LIMIT 10000
