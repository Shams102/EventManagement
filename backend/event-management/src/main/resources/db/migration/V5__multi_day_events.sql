-- V4: Multi-day event support
-- Add timing_model column to events (defaults to SINGLE_DAY for all existing rows)
ALTER TABLE events ADD COLUMN IF NOT EXISTS timing_model VARCHAR(30) NOT NULL DEFAULT 'SINGLE_DAY';

-- New table: event_time_slots
CREATE TABLE IF NOT EXISTS event_time_slots (
    id BIGSERIAL PRIMARY KEY,
    event_id BIGINT NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    slot_start TIMESTAMP NOT NULL,
    slot_end TIMESTAMP NOT NULL,
    day_index INTEGER,
    CONSTRAINT chk_slot_times CHECK (slot_end > slot_start)
);

CREATE INDEX IF NOT EXISTS idx_event_time_slots_event ON event_time_slots(event_id);
CREATE INDEX IF NOT EXISTS idx_event_time_slots_start ON event_time_slots(slot_start);

-- Backfill: create one time slot per existing event using its start/end times
INSERT INTO event_time_slots (event_id, slot_start, slot_end, day_index)
SELECT id, start_time, end_time, 0
FROM events
WHERE id NOT IN (SELECT event_id FROM event_time_slots);
