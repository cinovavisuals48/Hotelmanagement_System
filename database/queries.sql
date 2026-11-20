-- =====================================================================
-- Handy queries to inspect the hotel data in MySQL Workbench
-- =====================================================================
USE hotel_db;

-- All bookings with guest and room details
SELECT b.id,
       CONCAT(c.first_name, ' ', c.last_name) AS guest,
       c.email,
       c.phone,
       r.room_number,
       r.room_type,
       b.check_in_date,
       b.check_out_date,
       DATEDIFF(b.check_out_date, b.check_in_date) AS nights,
       b.number_of_guests,
       b.total_amount,
       b.status
FROM bookings b
JOIN customers c ON c.id = b.customer_id
JOIN rooms r     ON r.id = b.room_id
ORDER BY b.created_at DESC;

-- Rooms free for a date range (change the dates)
SET @check_in  = '2026-12-01';
SET @check_out = '2026-12-04';
SELECT r.*
FROM rooms r
WHERE r.status <> 'MAINTENANCE'
  AND NOT EXISTS (
      SELECT 1 FROM bookings b
      WHERE b.room_id = r.id
        AND b.status IN ('BOOKED', 'CHECKED_IN')
        AND b.check_in_date  < @check_out
        AND b.check_out_date > @check_in
  )
ORDER BY r.price_per_night;

-- Guests currently in the hotel
SELECT r.room_number, CONCAT(c.first_name, ' ', c.last_name) AS guest, b.check_out_date
FROM bookings b
JOIN customers c ON c.id = b.customer_id
JOIN rooms r     ON r.id = b.room_id
WHERE b.status = 'CHECKED_IN'
ORDER BY r.room_number;

-- Revenue by room type (checked-in and completed stays)
SELECT r.room_type, COUNT(*) AS stays, SUM(b.total_amount) AS revenue
FROM bookings b
JOIN rooms r ON r.id = b.room_id
WHERE b.status IN ('CHECKED_IN', 'CHECKED_OUT')
GROUP BY r.room_type
ORDER BY revenue DESC;
