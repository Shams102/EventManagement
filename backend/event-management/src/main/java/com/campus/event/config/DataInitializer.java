package com.campus.event.config;

import com.campus.event.domain.Event;
import com.campus.event.domain.Role;
import com.campus.event.domain.User;
import com.campus.event.repository.EventRepository;
import com.campus.event.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Set;

@Configuration
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Bean
    CommandLineRunner seedData(UserRepository users, EventRepository events, PasswordEncoder encoder) {
        return args -> {
            // Migrate any existing users with non-BCrypt passwords to encoded values
            users.findAll().forEach(u -> {
                String ph = u.getPasswordHash();
                if (ph != null && !ph.startsWith("$2")) {
                    u.setPasswordHash(encoder.encode(ph));
                    users.save(u);
                    log.info("Migrated password hash for user: {}", u.getUsername());
                }
            });

            if (users.findByUsername("admin").isEmpty()) {
                User admin = new User();
                admin.setUsername("admin");
                admin.setPasswordHash(encoder.encode("Admin@123"));
                admin.setEmail("admin@example.com");
                admin.setRoles(Set.of(Role.ADMIN));
                users.save(admin);
                log.info("Seeded admin user");
            }

            if (users.findByUsername("faculty").isEmpty()) {
                User faculty = new User();
                faculty.setUsername("faculty");
                faculty.setPasswordHash(encoder.encode("Faculty@123"));
                faculty.setEmail("faculty@example.com");
                faculty.setRoles(Set.of(Role.FACULTY));
                users.save(faculty);
                log.info("Seeded faculty user");
            }

            if (users.findByUsername("club").isEmpty()) {
                User club = new User();
                club.setUsername("club");
                club.setPasswordHash(encoder.encode("Club@123"));
                club.setEmail("club@example.com");
                club.setRoles(Set.of(Role.CLUB_ASSOCIATE));
                users.save(club);
                log.info("Seeded club user");
            }

            if (users.findByUsername("user").isEmpty()) {
                User user = new User();
                user.setUsername("user");
                user.setPasswordHash(encoder.encode("User@123"));
                user.setEmail("user@example.com");
                user.setRoles(Set.of(Role.GENERAL_USER));
                users.save(user);
                log.info("Seeded general user");
            }

            if (events.count() == 0) {
                Event e1 = new Event();
                e1.setTitle("Orientation Week");
                e1.setDescription("Welcome event for new students");
                e1.setStartTime(LocalDateTime.now().plusDays(3).withHour(10).withMinute(0));
                e1.setEndTime(LocalDateTime.now().plusDays(3).withHour(12).withMinute(0));
                e1.setPublic(true);
                events.save(e1);

                Event e2 = new Event();
                e2.setTitle("Tech Talk: Spring Boot Security");
                e2.setDescription("Deep dive into JWT and RBAC");
                e2.setStartTime(LocalDateTime.now().plusDays(7).withHour(15).withMinute(0));
                e2.setEndTime(LocalDateTime.now().plusDays(7).withHour(17).withMinute(0));
                e2.setPublic(true);
                events.save(e2);

                log.info("Seeded sample events");
            }
        };
    }
}
