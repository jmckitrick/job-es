-- name: get-travel-bookings
-- Get columns for elastic search from travel bookings.
-- SQL query compiled to a function.
SELECT
 tb.id,
 tb.licensee_id licensee,
 tb.adapter_name adapter,
 tb.agent_name agent,
 tb.subsite_agent_name subsite,
 tb.branch_name branch,
 tb.vendor_confirmation ref,
 tb.itinerary_name itinerary,
 t.first_name fname,
 t.middle_name mname,
 t.last_name lname,
 CONCAT_WS(' ', t.first_name, t.middle_name, t.last_name) full_name,
 t.email,
 t.phone
FROM TRAVEL_BOOKING tb
INNER JOIN TRAVELLER t ON t.booking_id = tb.id
WHERE t.is_primary = 1
AND tb.create_date >= :start_date
AND tb.create_date <  :end_date
