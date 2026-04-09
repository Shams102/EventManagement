CREATE TABLE IF NOT EXISTS public.bookings (
    id BIGSERIAL PRIMARY KEY,
    end_time timestamp(6) without time zone NOT NULL,
    purpose character varying(255),
    start_time timestamp(6) without time zone NOT NULL,
    room_id bigint,
    user_id bigint
);

CREATE TABLE IF NOT EXISTS public.building_timetable (
    id BIGSERIAL PRIMARY KEY,
    day_of_week character varying(16) NOT NULL,
    end_time time(6) without time zone NOT NULL,
    start_time time(6) without time zone NOT NULL,
    building_id bigint NOT NULL
);

CREATE TABLE IF NOT EXISTS public.buildings (
    id BIGSERIAL PRIMARY KEY,
    code character varying(255),
    description character varying(255),
    is_active boolean NOT NULL,
    name character varying(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS public.event_registrations (
    id BIGSERIAL PRIMARY KEY,
    registered_at timestamp(6) without time zone,
    event_id bigint NOT NULL,
    user_id bigint NOT NULL
);

CREATE TABLE IF NOT EXISTS public.event_time_slots (
    id BIGSERIAL PRIMARY KEY,
    day_index integer,
    slot_end timestamp(6) without time zone NOT NULL,
    slot_start timestamp(6) without time zone NOT NULL,
    event_id bigint NOT NULL
);

CREATE TABLE IF NOT EXISTS public.events (
    id BIGSERIAL PRIMARY KEY,
    club_id character varying(255),
    description character varying(255),
    end_time timestamp(6) without time zone NOT NULL,
    is_public boolean NOT NULL,
    location character varying(255),
    max_attendees integer,
    registration_schema text,
    start_time timestamp(6) without time zone NOT NULL,
    timing_model character varying(255) NOT NULL,
    title character varying(255) NOT NULL,
    building_id bigint NOT NULL,
    created_by_id bigint
);

CREATE TABLE IF NOT EXISTS public.fixed_timetable (
    id BIGSERIAL PRIMARY KEY,
    academic_year character varying(255),
    batch character varying(255),
    course_code character varying(255),
    course_name character varying(255) NOT NULL,
    day_of_week character varying(255),
    end_time time(6) without time zone,
    is_active boolean NOT NULL,
    section character varying(255),
    semester character varying(255),
    start_time time(6) without time zone,
    faculty_id bigint,
    room_id bigint
);

CREATE TABLE IF NOT EXISTS public.floors (
    id BIGSERIAL PRIMARY KEY,
    floor_number integer,
    name character varying(255),
    building_id bigint
);

CREATE TABLE IF NOT EXISTS public.notification_deliveries (
    id BIGSERIAL PRIMARY KEY,
    created_at timestamp(6) without time zone,
    delivery_status character varying(255),
    muted boolean NOT NULL,
    read_at timestamp(6) without time zone,
    notification_id bigint,
    user_id bigint
);

CREATE TABLE IF NOT EXISTS public.notification_messages (
    id BIGSERIAL PRIMARY KEY,
    created_at timestamp(6) without time zone,
    message text,
    origin character varying(255),
    thread_enabled boolean NOT NULL,
    title character varying(255),
    urgency character varying(255),
    created_by_id bigint,
    event_id bigint
);

CREATE TABLE IF NOT EXISTS public.notification_threads (
    id BIGSERIAL PRIMARY KEY,
    closed boolean NOT NULL,
    created_at timestamp(6) without time zone,
    pinned boolean NOT NULL,
    title character varying(255),
    created_by_id bigint,
    event_id bigint,
    notification_id bigint
);

CREATE TABLE IF NOT EXISTS public.notifications (
    id BIGSERIAL PRIMARY KEY,
    created_at timestamp(6) without time zone,
    message text,
    sent_at timestamp(6) without time zone,
    status character varying(255),
    subject character varying(255),
    type character varying(255),
    user_id bigint
);

CREATE TABLE IF NOT EXISTS public.password_reset_token (
    id BIGSERIAL PRIMARY KEY,
    expiry_date timestamp(6) without time zone,
    token character varying(255),
    used boolean NOT NULL,
    user_id bigint NOT NULL
);

CREATE TABLE IF NOT EXISTS public.registrations (
    id BIGSERIAL PRIMARY KEY,
    answers_json text,
    email character varying(255) NOT NULL,
    full_name character varying(255) NOT NULL,
    event_id bigint
);

CREATE TABLE IF NOT EXISTS public.room_booking_requests (
    id BIGSERIAL PRIMARY KEY,
    approved_at timestamp(6) without time zone,
    approved_by_username character varying(255),
    confirmed_at timestamp(6) without time zone,
    meeting_end timestamp(6) without time zone,
    meeting_purpose character varying(255),
    meeting_start timestamp(6) without time zone,
    requested_at timestamp(6) without time zone NOT NULL,
    requested_by_username character varying(255),
    split_group_id uuid,
    status character varying(255),
    allocated_room_id bigint,
    event_id bigint,
    pref1_room_id bigint,
    pref2_room_id bigint,
    pref3_room_id bigint
);

CREATE TABLE IF NOT EXISTS public.rooms (
    id BIGSERIAL PRIMARY KEY,
    amenities character varying(255),
    capacity integer,
    is_active boolean NOT NULL,
    name character varying(255),
    room_number character varying(255) NOT NULL,
    type character varying(255),
    floor_id bigint
);

CREATE TABLE IF NOT EXISTS public.thread_messages (
    id BIGSERIAL PRIMARY KEY,
    content text,
    created_at timestamp(6) without time zone,
    deleted boolean NOT NULL,
    edited_at timestamp(6) without time zone,
    author_id bigint,
    thread_id bigint
);

CREATE TABLE IF NOT EXISTS public.user_roles (
    user_id bigint NOT NULL,
    roles character varying(255)
);

CREATE TABLE IF NOT EXISTS public.users (
    id BIGSERIAL PRIMARY KEY,
    admin_scope character varying(255),
    club_id character varying(255),
    email character varying(255) NOT NULL,
    full_name character varying(255),
    managed_building_id bigint,
    password_hash character varying(255) NOT NULL,
    phone_number character varying(255),
    requested_role character varying(255),
    username character varying(255) NOT NULL
);
