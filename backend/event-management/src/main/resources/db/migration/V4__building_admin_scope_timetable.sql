-- ============================================================================
-- V3: Building admin scope, split approval groups, Building A/B, building timetable
-- ============================================================================
-- Safe on fresh DB: guards every ALTER with to_regclass() checks so Flyway
-- won't blow up when Hibernate hasn't created the tables yet.
-- ============================================================================

-- 1) admin_scope column on users
DO $$
BEGIN
    IF to_regclass('public.users') IS NOT NULL THEN
        ALTER TABLE users ADD COLUMN IF NOT EXISTS admin_scope VARCHAR(32);
    END IF;
END $$;

-- 2) split_group_id column on room_booking_requests
DO $$
BEGIN
    IF to_regclass('public.room_booking_requests') IS NOT NULL THEN
        ALTER TABLE room_booking_requests ADD COLUMN IF NOT EXISTS split_group_id UUID;
    END IF;
END $$;

-- 3) building_timetable table
DO $$
BEGIN
    IF to_regclass('public.buildings') IS NOT NULL THEN
        EXECUTE 'CREATE TABLE IF NOT EXISTS building_timetable (
            id BIGSERIAL PRIMARY KEY,
            building_id BIGINT NOT NULL REFERENCES buildings (id) ON DELETE CASCADE,
            day_of_week VARCHAR(16) NOT NULL,
            start_time TIME NOT NULL,
            end_time TIME NOT NULL
        )';
        EXECUTE 'CREATE INDEX IF NOT EXISTS idx_building_timetable_building_day
            ON building_timetable (building_id, day_of_week)';
    END IF;
END $$;

-- 4) Seed Building A & B (idempotent)
DO $$
BEGIN
    IF to_regclass('public.buildings') IS NOT NULL THEN
        INSERT INTO buildings (name, code, description, is_active)
        SELECT 'Building A', 'BLD_A', 'Structured campus building A', true
        WHERE NOT EXISTS (SELECT 1 FROM buildings WHERE code = 'BLD_A');

        INSERT INTO buildings (name, code, description, is_active)
        SELECT 'Building B', 'BLD_B', 'Structured campus building B', true
        WHERE NOT EXISTS (SELECT 1 FROM buildings WHERE code = 'BLD_B');
    END IF;
END $$;

-- 5) Default operating hours Mon-Sun 08:00-22:00 for Building A and B
DO $$
BEGIN
    IF to_regclass('public.buildings') IS NOT NULL
       AND to_regclass('public.building_timetable') IS NOT NULL THEN
        INSERT INTO building_timetable (building_id, day_of_week, start_time, end_time)
        SELECT b.id, v.dow, TIME '08:00', TIME '22:00'
        FROM buildings b
        CROSS JOIN (VALUES
            ('MONDAY'),
            ('TUESDAY'),
            ('WEDNESDAY'),
            ('THURSDAY'),
            ('FRIDAY'),
            ('SATURDAY'),
            ('SUNDAY')
        ) AS v (dow)
        WHERE b.code IN ('BLD_A', 'BLD_B')
          AND NOT EXISTS (
              SELECT 1 FROM building_timetable t
              WHERE t.building_id = b.id AND t.day_of_week = v.dow
          );
    END IF;
END $$;
