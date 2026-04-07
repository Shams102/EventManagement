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

ALTER TABLE ONLY public.bookings
    ADD CONSTRAINT uk45c3i9pv4vs1s930in6t1ihkr UNIQUE (room_id, start_time, end_time);

ALTER TABLE ONLY public.registrations
    ADD CONSTRAINT ukfd5jwq59i87waqf4u1hhuntkr UNIQUE (event_id, email);

ALTER TABLE ONLY public.event_registrations
    ADD CONSTRAINT ukmv1wttabjpboyulu2pql4koqo UNIQUE (event_id, user_id);

ALTER TABLE ONLY public.notification_threads
    ADD CONSTRAINT fk234qsvyw2h663eimk4f817970 FOREIGN KEY (notification_id) REFERENCES public.notification_messages(id);

ALTER TABLE ONLY public.room_booking_requests
    ADD CONSTRAINT fk2vakrm2ete6sa7mrossj4rndo FOREIGN KEY (pref1_room_id) REFERENCES public.rooms(id);

ALTER TABLE ONLY public.event_registrations
    ADD CONSTRAINT fk6eykq6wu4n23qhn5vwb8kyut5 FOREIGN KEY (event_id) REFERENCES public.events(id);

ALTER TABLE ONLY public.rooms
    ADD CONSTRAINT fk71tvfklk03awky6oydmacgcoo FOREIGN KEY (floor_id) REFERENCES public.floors(id) ON DELETE CASCADE;

ALTER TABLE ONLY public.notification_threads
    ADD CONSTRAINT fk7kauri7uu3ihtlq6970vaoweq FOREIGN KEY (created_by_id) REFERENCES public.users(id);

ALTER TABLE ONLY public.password_reset_token
    ADD CONSTRAINT fk83nsrttkwkb6ym0anu051mtxn FOREIGN KEY (user_id) REFERENCES public.users(id);

ALTER TABLE ONLY public.registrations
    ADD CONSTRAINT fk8mi58jt1s8fxmi56jnau0cxqw FOREIGN KEY (event_id) REFERENCES public.events(id);

ALTER TABLE ONLY public.notification_deliveries
    ADD CONSTRAINT fk8pmqj3nuybn603e95ay6p7mr4 FOREIGN KEY (notification_id) REFERENCES public.notification_messages(id);

ALTER TABLE ONLY public.room_booking_requests
    ADD CONSTRAINT fk8tp8renfgx9arghv7j5xwiv76 FOREIGN KEY (pref2_room_id) REFERENCES public.rooms(id);

ALTER TABLE ONLY public.notification_messages
    ADD CONSTRAINT fk934ukwhrkhcii8gx7kpr9qn31 FOREIGN KEY (created_by_id) REFERENCES public.users(id);

ALTER TABLE ONLY public.notifications
    ADD CONSTRAINT fk9y21adhxn0ayjhfocscqox7bh FOREIGN KEY (user_id) REFERENCES public.users(id);

ALTER TABLE ONLY public.events
    ADD CONSTRAINT fkbd320pnltw8sobixakxi103t2 FOREIGN KEY (created_by_id) REFERENCES public.users(id);

ALTER TABLE ONLY public.events
    ADD CONSTRAINT fkbjcwsww8bx7kyd99j7naj1h5h FOREIGN KEY (building_id) REFERENCES public.buildings(id);

ALTER TABLE ONLY public.notification_threads
    ADD CONSTRAINT fkbyw9l27eg0ycv14p9ik326niu FOREIGN KEY (event_id) REFERENCES public.events(id);

ALTER TABLE ONLY public.room_booking_requests
    ADD CONSTRAINT fkc6l0ms8pyx8s75dmva2wjtoyp FOREIGN KEY (pref3_room_id) REFERENCES public.rooms(id);

ALTER TABLE ONLY public.room_booking_requests
    ADD CONSTRAINT fkctc050evjtk2qdahmu0207x5m FOREIGN KEY (allocated_room_id) REFERENCES public.rooms(id);

ALTER TABLE ONLY public.floors
    ADD CONSTRAINT fkdhibx5frs3cwiltccr79uks37 FOREIGN KEY (building_id) REFERENCES public.buildings(id) ON DELETE CASCADE;

ALTER TABLE ONLY public.bookings
    ADD CONSTRAINT fkeyog2oic85xg7hsu2je2lx3s6 FOREIGN KEY (user_id) REFERENCES public.users(id);

ALTER TABLE ONLY public.fixed_timetable
    ADD CONSTRAINT fkgic7d8yh33x41gk01b0n5x5j FOREIGN KEY (faculty_id) REFERENCES public.users(id);

ALTER TABLE ONLY public.notification_messages
    ADD CONSTRAINT fkgwoepvar4pka55e8y2p4oi1ka FOREIGN KEY (event_id) REFERENCES public.events(id);

ALTER TABLE ONLY public.user_roles
    ADD CONSTRAINT fkhfh9dx7w3ubf1co1vdev94g3f FOREIGN KEY (user_id) REFERENCES public.users(id);

ALTER TABLE ONLY public.notification_deliveries
    ADD CONSTRAINT fkkgx92yw5p0w2u038m9hdc7y45 FOREIGN KEY (user_id) REFERENCES public.users(id);

ALTER TABLE ONLY public.fixed_timetable
    ADD CONSTRAINT fkkh831m60w9e0m29g20621fdu FOREIGN KEY (room_id) REFERENCES public.rooms(id);

ALTER TABLE ONLY public.event_registrations
    ADD CONSTRAINT fknk7jh3bmmv11csoxkjnb6av4h FOREIGN KEY (user_id) REFERENCES public.users(id);

ALTER TABLE ONLY public.thread_messages
    ADD CONSTRAINT fkrc8sp17hfb46mynilt2egpwul FOREIGN KEY (author_id) REFERENCES public.users(id);

ALTER TABLE ONLY public.room_booking_requests
    ADD CONSTRAINT fkrgg85c1riv95r0mt5vt36icom FOREIGN KEY (event_id) REFERENCES public.events(id);

ALTER TABLE ONLY public.bookings
    ADD CONSTRAINT fkrgoycol97o21kpjodw1qox4nc FOREIGN KEY (room_id) REFERENCES public.rooms(id);

ALTER TABLE ONLY public.building_timetable
    ADD CONSTRAINT fkryc673aoph4f9vs38bafohxeg FOREIGN KEY (building_id) REFERENCES public.buildings(id);

ALTER TABLE ONLY public.thread_messages
    ADD CONSTRAINT fkthtl1axwss895w6e78ajqhsdc FOREIGN KEY (thread_id) REFERENCES public.notification_threads(id);

ALTER TABLE ONLY public.event_time_slots
    ADD CONSTRAINT fktjbww7oyv6oknsyhfo95aebgj FOREIGN KEY (event_id) REFERENCES public.events(id);
