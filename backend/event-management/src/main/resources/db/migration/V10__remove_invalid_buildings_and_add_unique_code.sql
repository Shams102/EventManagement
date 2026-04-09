-- ============================================================================
-- V10: Remove invalid/unknown buildings and add unique constraint on code
-- ============================================================================
-- ISSUE: A building with code 'RLHC' (or similar unexpected codes) appeared in
-- production despite never being defined in the DataInitializer. This was likely
-- inserted by an earlier version of the seed logic, or manually. We also clean
-- up the fallback 'MAIN' building that DataInitializer creates when no
-- buildings exist at event-seed time.
--
-- ROOT CAUSE OF MIGRATION FAILURE:
--   The original step order deleted "rooms" BEFORE deleting rows in
--   "fixed_timetable", "bookings", and "room_booking_requests" — all of
--   which hold foreign-key references to rooms.id.  PostgreSQL correctly
--   refused the delete.
--
-- FIX:
--   1. Reassign events from invalid buildings to BLD_A.
--   2. Delete ALL tables that reference rooms.id (leaf-level FKs first).
--   3. Delete rooms → floors → building_timetable → buildings (trunk last).
--   4. Add UNIQUE constraint on buildings.code to prevent future duplicates.
--
-- FK DEPENDENCY CHAIN (verified against V8):
--   fixed_timetable.room_id        → rooms.id
--   bookings.room_id               → rooms.id
--   room_booking_requests.pref1/2/3_room_id → rooms.id
--   room_booking_requests.allocated_room_id → rooms.id
--   rooms.floor_id                 → floors.id
--   floors.building_id             → buildings.id
--   building_timetable.building_id → buildings.id
--   events.building_id             → buildings.id
--   users.managed_building_id      → buildings.id
-- ============================================================================

-- Step 1: Reassign events that reference non-standard buildings to BLD_A
UPDATE events
SET building_id = (SELECT id FROM buildings WHERE code = 'BLD_A' LIMIT 1)
WHERE building_id IN (
    SELECT id FROM buildings WHERE code NOT IN ('BLD_A', 'BLD_B')
)
AND EXISTS (SELECT 1 FROM buildings WHERE code = 'BLD_A');

-- Step 2: Delete dependent TIMETABLE entries that reference rooms in invalid buildings
DELETE FROM fixed_timetable
WHERE room_id IN (
    SELECT r.id FROM rooms r
    JOIN floors f ON r.floor_id = f.id
    JOIN buildings b ON f.building_id = b.id
    WHERE b.code NOT IN ('BLD_A', 'BLD_B')
);

-- Step 3: Delete dependent BOOKING entries that reference rooms in invalid buildings
DELETE FROM bookings
WHERE room_id IN (
    SELECT r.id FROM rooms r
    JOIN floors f ON r.floor_id = f.id
    JOIN buildings b ON f.building_id = b.id
    WHERE b.code NOT IN ('BLD_A', 'BLD_B')
);

-- Step 4: Nullify room_booking_requests preferences/allocations referencing invalid rooms
-- (Nullify rather than delete — preserves the request audit trail)
UPDATE room_booking_requests
SET pref1_room_id = NULL
WHERE pref1_room_id IN (
    SELECT r.id FROM rooms r
    JOIN floors f ON r.floor_id = f.id
    JOIN buildings b ON f.building_id = b.id
    WHERE b.code NOT IN ('BLD_A', 'BLD_B')
);

UPDATE room_booking_requests
SET pref2_room_id = NULL
WHERE pref2_room_id IN (
    SELECT r.id FROM rooms r
    JOIN floors f ON r.floor_id = f.id
    JOIN buildings b ON f.building_id = b.id
    WHERE b.code NOT IN ('BLD_A', 'BLD_B')
);

UPDATE room_booking_requests
SET pref3_room_id = NULL
WHERE pref3_room_id IN (
    SELECT r.id FROM rooms r
    JOIN floors f ON r.floor_id = f.id
    JOIN buildings b ON f.building_id = b.id
    WHERE b.code NOT IN ('BLD_A', 'BLD_B')
);

UPDATE room_booking_requests
SET allocated_room_id = NULL
WHERE allocated_room_id IN (
    SELECT r.id FROM rooms r
    JOIN floors f ON r.floor_id = f.id
    JOIN buildings b ON f.building_id = b.id
    WHERE b.code NOT IN ('BLD_A', 'BLD_B')
);

-- Step 5: Now safe to delete rooms (all FK children cleared above)
DELETE FROM rooms
WHERE floor_id IN (
    SELECT f.id FROM floors f
    JOIN buildings b ON f.building_id = b.id
    WHERE b.code NOT IN ('BLD_A', 'BLD_B')
);

-- Step 6: Delete floors of invalid buildings
DELETE FROM floors
WHERE building_id IN (
    SELECT id FROM buildings WHERE code NOT IN ('BLD_A', 'BLD_B')
);

-- Step 7: Delete building timetable entries for invalid buildings
DELETE FROM building_timetable
WHERE building_id IN (
    SELECT id FROM buildings WHERE code NOT IN ('BLD_A', 'BLD_B')
);

-- Step 8: Clear managed_building_id references on users pointing to invalid buildings
UPDATE users
SET managed_building_id = NULL
WHERE managed_building_id IN (
    SELECT id FROM buildings WHERE code NOT IN ('BLD_A', 'BLD_B')
);

-- Step 9: Delete the invalid buildings themselves (all dependents gone)
DELETE FROM buildings WHERE code NOT IN ('BLD_A', 'BLD_B');

-- Step 10: Add unique constraint on building code to prevent future issues
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_buildings_code') THEN
        ALTER TABLE buildings ADD CONSTRAINT uk_buildings_code UNIQUE (code);
    END IF;
END $$;
