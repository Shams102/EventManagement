-- ============================================================================
-- V2: Mandatory building_id on events (safe + idempotent)
-- ============================================================================
-- - Runs only when public.buildings and public.events exist (fresh DB: Flyway
--   may run before Hibernate; this script exits cleanly and Hibernate can
--   create/align schema).
-- - Ensures at least one building row exists when the table is empty.
-- - Adds building_id if missing, backfills NULLs from any existing building,
--   then SET NOT NULL and FK (only when not already present).
-- ============================================================================

DO $migration$
DECLARE
    building_count integer;
    orphan_events  integer;
BEGIN
    -- Nothing to do until both tables exist (avoids startup failure on empty DB)
    IF to_regclass('public.buildings') IS NULL THEN
        RAISE NOTICE 'V2: public.buildings does not exist yet — skipping building_id migration';
        RETURN;
    END IF;

    IF to_regclass('public.events') IS NULL THEN
        RAISE NOTICE 'V2: public.events does not exist yet — skipping building_id migration';
        RETURN;
    END IF;

    -- -------------------------------------------------------------------------
    -- 1) Default building when the table is completely empty
    --    (ON CONFLICT DO NOTHING needs a unique constraint; we avoid mutating
    --     legacy schemas and use NOT EXISTS instead, which is safe if empty.)
    -- -------------------------------------------------------------------------
    SELECT COUNT(*) INTO building_count FROM public.buildings;
    SELECT COUNT(*) INTO building_count FROM public.buildings;

    -- -------------------------------------------------------------------------
    -- 2) Nullable column first (idempotent)
    -- -------------------------------------------------------------------------
    ALTER TABLE public.events
        ADD COLUMN IF NOT EXISTS building_id BIGINT;

    -- -------------------------------------------------------------------------
    -- 3) Backfill: any building works; ORDER BY id is stable and needs no code
    -- -------------------------------------------------------------------------
    UPDATE public.events AS e
    SET building_id = s.id
    FROM (
        SELECT b.id
        FROM public.buildings AS b
        ORDER BY b.id
        LIMIT 1
    ) AS s
    WHERE e.building_id IS NULL;

    -- Still NULL => no building row (should not happen after insert-into-empty)
    SELECT COUNT(*) INTO orphan_events
    FROM public.events
    WHERE building_id IS NULL;

    IF orphan_events > 0 THEN
        RAISE EXCEPTION
            'V2: % event(s) still have NULL building_id after backfill (buildings table must have at least one row)',
            orphan_events;
    END IF;

    -- -------------------------------------------------------------------------
    -- 4) NOT NULL only after data is populated (idempotent if already NOT NULL)
    -- -------------------------------------------------------------------------
    ALTER TABLE public.events
        ALTER COLUMN building_id SET NOT NULL;

    -- -------------------------------------------------------------------------
    -- 5) Foreign key — skip if any FK already ties events.building_id to buildings
    --    (covers Hibernate-generated constraint names)
    -- -------------------------------------------------------------------------
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.referential_constraints AS rc
        JOIN information_schema.key_column_usage AS kcu
          ON kcu.constraint_catalog = rc.constraint_catalog
         AND kcu.constraint_schema = rc.constraint_schema
         AND kcu.constraint_name = rc.constraint_name
        JOIN information_schema.table_constraints AS uq
          ON uq.constraint_catalog = rc.unique_constraint_catalog
         AND uq.constraint_schema = rc.unique_constraint_schema
         AND uq.constraint_name = rc.unique_constraint_name
        WHERE rc.constraint_schema = 'public'
          AND kcu.table_schema = 'public'
          AND kcu.table_name = 'events'
          AND kcu.column_name = 'building_id'
          AND uq.table_schema = 'public'
          AND uq.table_name = 'buildings'
    ) THEN
        ALTER TABLE public.events
            ADD CONSTRAINT fk_events_building
            FOREIGN KEY (building_id) REFERENCES public.buildings (id);
    END IF;

    -- -------------------------------------------------------------------------
    -- 6) Index
    -- -------------------------------------------------------------------------
    CREATE INDEX IF NOT EXISTS idx_events_building_id ON public.events (building_id);

END $migration$;
