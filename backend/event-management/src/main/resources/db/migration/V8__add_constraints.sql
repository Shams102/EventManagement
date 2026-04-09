DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk45c3i9pv4vs1s930in6t1ihkr') THEN
        ALTER TABLE ONLY public.bookings
            ADD CONSTRAINT uk45c3i9pv4vs1s930in6t1ihkr UNIQUE (room_id, start_time, end_time);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ukfd5jwq59i87waqf4u1hhuntkr') THEN
        ALTER TABLE ONLY public.registrations
            ADD CONSTRAINT ukfd5jwq59i87waqf4u1hhuntkr UNIQUE (event_id, email);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ukmv1wttabjpboyulu2pql4koqo') THEN
        ALTER TABLE ONLY public.event_registrations
            ADD CONSTRAINT ukmv1wttabjpboyulu2pql4koqo UNIQUE (event_id, user_id);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk234qsvyw2h663eimk4f817970') THEN
        ALTER TABLE ONLY public.notification_threads
            ADD CONSTRAINT fk234qsvyw2h663eimk4f817970 FOREIGN KEY (notification_id) REFERENCES public.notification_messages(id);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk2vakrm2ete6sa7mrossj4rndo') THEN
        ALTER TABLE ONLY public.room_booking_requests
            ADD CONSTRAINT fk2vakrm2ete6sa7mrossj4rndo FOREIGN KEY (pref1_room_id) REFERENCES public.rooms(id);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk6eykq6wu4n23qhn5vwb8kyut5') THEN
        ALTER TABLE ONLY public.event_registrations
            ADD CONSTRAINT fk6eykq6wu4n23qhn5vwb8kyut5 FOREIGN KEY (event_id) REFERENCES public.events(id);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk71tvfklk03awky6oydmacgcoo') THEN
        ALTER TABLE ONLY public.rooms
            ADD CONSTRAINT fk71tvfklk03awky6oydmacgcoo FOREIGN KEY (floor_id) REFERENCES public.floors(id) ON DELETE CASCADE;
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk7kauri7uu3ihtlq6970vaoweq') THEN
        ALTER TABLE ONLY public.notification_threads
            ADD CONSTRAINT fk7kauri7uu3ihtlq6970vaoweq FOREIGN KEY (created_by_id) REFERENCES public.users(id);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk83nsrttkwkb6ym0anu051mtxn') THEN
        ALTER TABLE ONLY public.password_reset_token
            ADD CONSTRAINT fk83nsrttkwkb6ym0anu051mtxn FOREIGN KEY (user_id) REFERENCES public.users(id);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk8mi58jt1s8fxmi56jnau0cxqw') THEN
        ALTER TABLE ONLY public.registrations
            ADD CONSTRAINT fk8mi58jt1s8fxmi56jnau0cxqw FOREIGN KEY (event_id) REFERENCES public.events(id);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk8pmqj3nuybn603e95ay6p7mr4') THEN
        ALTER TABLE ONLY public.notification_deliveries
            ADD CONSTRAINT fk8pmqj3nuybn603e95ay6p7mr4 FOREIGN KEY (notification_id) REFERENCES public.notification_messages(id);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk8tp8renfgx9arghv7j5xwiv76') THEN
        ALTER TABLE ONLY public.room_booking_requests
            ADD CONSTRAINT fk8tp8renfgx9arghv7j5xwiv76 FOREIGN KEY (pref2_room_id) REFERENCES public.rooms(id);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk934ukwhrkhcii8gx7kpr9qn31') THEN
        ALTER TABLE ONLY public.notification_messages
            ADD CONSTRAINT fk934ukwhrkhcii8gx7kpr9qn31 FOREIGN KEY (created_by_id) REFERENCES public.users(id);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk9y21adhxn0ayjhfocscqox7bh') THEN
        ALTER TABLE ONLY public.notifications
            ADD CONSTRAINT fk9y21adhxn0ayjhfocscqox7bh FOREIGN KEY (user_id) REFERENCES public.users(id);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fkbd320pnltw8sobixakxi103t2') THEN
        ALTER TABLE ONLY public.events
            ADD CONSTRAINT fkbd320pnltw8sobixakxi103t2 FOREIGN KEY (created_by_id) REFERENCES public.users(id);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fkbjcwsww8bx7kyd99j7naj1h5h') THEN
        ALTER TABLE ONLY public.events
            ADD CONSTRAINT fkbjcwsww8bx7kyd99j7naj1h5h FOREIGN KEY (building_id) REFERENCES public.buildings(id);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fkbyw9l27eg0ycv14p9ik326niu') THEN
        ALTER TABLE ONLY public.notification_threads
            ADD CONSTRAINT fkbyw9l27eg0ycv14p9ik326niu FOREIGN KEY (event_id) REFERENCES public.events(id);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fkc6l0ms8pyx8s75dmva2wjtoyp') THEN
        ALTER TABLE ONLY public.room_booking_requests
            ADD CONSTRAINT fkc6l0ms8pyx8s75dmva2wjtoyp FOREIGN KEY (pref3_room_id) REFERENCES public.rooms(id);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fkctc050evjtk2qdahmu0207x5m') THEN
        ALTER TABLE ONLY public.room_booking_requests
            ADD CONSTRAINT fkctc050evjtk2qdahmu0207x5m FOREIGN KEY (allocated_room_id) REFERENCES public.rooms(id);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fkdhibx5frs3cwiltccr79uks37') THEN
        ALTER TABLE ONLY public.floors
            ADD CONSTRAINT fkdhibx5frs3cwiltccr79uks37 FOREIGN KEY (building_id) REFERENCES public.buildings(id) ON DELETE CASCADE;
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fkeyog2oic85xg7hsu2je2lx3s6') THEN
        ALTER TABLE ONLY public.bookings
            ADD CONSTRAINT fkeyog2oic85xg7hsu2je2lx3s6 FOREIGN KEY (user_id) REFERENCES public.users(id);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fkgic7d8yh33x41gk01b0n5x5j') THEN
        ALTER TABLE ONLY public.fixed_timetable
            ADD CONSTRAINT fkgic7d8yh33x41gk01b0n5x5j FOREIGN KEY (faculty_id) REFERENCES public.users(id);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fkgwoepvar4pka55e8y2p4oi1ka') THEN
        ALTER TABLE ONLY public.notification_messages
            ADD CONSTRAINT fkgwoepvar4pka55e8y2p4oi1ka FOREIGN KEY (event_id) REFERENCES public.events(id);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fkhfh9dx7w3ubf1co1vdev94g3f') THEN
        ALTER TABLE ONLY public.user_roles
            ADD CONSTRAINT fkhfh9dx7w3ubf1co1vdev94g3f FOREIGN KEY (user_id) REFERENCES public.users(id);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fkkgx92yw5p0w2u038m9hdc7y45') THEN
        ALTER TABLE ONLY public.notification_deliveries
            ADD CONSTRAINT fkkgx92yw5p0w2u038m9hdc7y45 FOREIGN KEY (user_id) REFERENCES public.users(id);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fkkh831m60w9e0m29g20621fdu') THEN
        ALTER TABLE ONLY public.fixed_timetable
            ADD CONSTRAINT fkkh831m60w9e0m29g20621fdu FOREIGN KEY (room_id) REFERENCES public.rooms(id);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fknk7jh3bmmv11csoxkjnb6av4h') THEN
        ALTER TABLE ONLY public.event_registrations
            ADD CONSTRAINT fknk7jh3bmmv11csoxkjnb6av4h FOREIGN KEY (user_id) REFERENCES public.users(id);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fkrc8sp17hfb46mynilt2egpwul') THEN
        ALTER TABLE ONLY public.thread_messages
            ADD CONSTRAINT fkrc8sp17hfb46mynilt2egpwul FOREIGN KEY (author_id) REFERENCES public.users(id);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fkrgg85c1riv95r0mt5vt36icom') THEN
        ALTER TABLE ONLY public.room_booking_requests
            ADD CONSTRAINT fkrgg85c1riv95r0mt5vt36icom FOREIGN KEY (event_id) REFERENCES public.events(id);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fkrgoycol97o21kpjodw1qox4nc') THEN
        ALTER TABLE ONLY public.bookings
            ADD CONSTRAINT fkrgoycol97o21kpjodw1qox4nc FOREIGN KEY (room_id) REFERENCES public.rooms(id);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fkryc673aoph4f9vs38bafohxeg') THEN
        ALTER TABLE ONLY public.building_timetable
            ADD CONSTRAINT fkryc673aoph4f9vs38bafohxeg FOREIGN KEY (building_id) REFERENCES public.buildings(id);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fkthtl1axwss895w6e78ajqhsdc') THEN
        ALTER TABLE ONLY public.thread_messages
            ADD CONSTRAINT fkthtl1axwss895w6e78ajqhsdc FOREIGN KEY (thread_id) REFERENCES public.notification_threads(id);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fktjbww7oyv6oknsyhfo95aebgj') THEN
        ALTER TABLE ONLY public.event_time_slots
            ADD CONSTRAINT fktjbww7oyv6oknsyhfo95aebgj FOREIGN KEY (event_id) REFERENCES public.events(id);
    END IF;
END $$;
