-- V5: Performance Tuning for Pessimistic Locking

-- Accelerate conflict sweeps by indexing the targeted room and its operational status.
-- (Because start/end times are polymorphic across events and meetings, we index the room lookup natively).
CREATE INDEX IF NOT EXISTS idx_room_booking_status ON room_booking_requests (allocated_room_id, status);

-- Accelerate internal overlaps and slot loading for flexible boundaries.
CREATE INDEX IF NOT EXISTS idx_event_time_slots_event ON event_time_slots (event_id, slot_start);
