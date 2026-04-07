-- ============================================================================
-- V1: Drop Hibernate-generated CHECK constraints on enum columns (if present)
-- ============================================================================
-- Hibernate can create CHECK constraints from Java enums; they break when new
-- enum values are added. Validation stays in the application.
--
-- Safe on fresh DB: Flyway often runs before tables exist — ALTER on missing
-- relations would fail. We only alter tables that are already present.
-- ============================================================================

DO $$
BEGIN
    IF to_regclass('public.user_roles') IS NOT NULL THEN
        ALTER TABLE public.user_roles
            DROP CONSTRAINT IF EXISTS user_roles_roles_check;
    END IF;

    IF to_regclass('public.users') IS NOT NULL THEN
        ALTER TABLE public.users
            DROP CONSTRAINT IF EXISTS users_requested_role_check;
    END IF;
END $$;
