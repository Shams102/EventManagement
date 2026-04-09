package com.campus.event.config;

import com.campus.event.domain.AdminScope;
import com.campus.event.domain.Building;
import com.campus.event.domain.BuildingTimetable;
import com.campus.event.domain.Event;
import com.campus.event.domain.FixedTimetable;
import com.campus.event.domain.Floor;
import com.campus.event.domain.Role;
import com.campus.event.domain.Room;
import com.campus.event.domain.RoomType;
import com.campus.event.domain.User;
import com.campus.event.repository.BuildingRepository;
import com.campus.event.repository.BuildingTimetableRepository;
import com.campus.event.repository.EventRepository;
import com.campus.event.repository.FixedTimetableRepository;
import com.campus.event.repository.FloorRepository;
import com.campus.event.repository.RoomRepository;
import com.campus.event.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Random;
import java.util.Set;

@Configuration
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Bean
    CommandLineRunner seedData(UserRepository users, EventRepository events,
                               BuildingRepository buildings, BuildingTimetableRepository timetables,
                               FixedTimetableRepository fixedTimetables,
                               FloorRepository floors, RoomRepository rooms,
                               PasswordEncoder encoder) {
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

            if (users.findByUsername("central_admin").isEmpty()) {
                User centralAdmin = new User();
                centralAdmin.setUsername("central_admin");
                centralAdmin.setPasswordHash(encoder.encode("Central@123"));
                centralAdmin.setEmail("central@example.com");
                centralAdmin.setRoles(Set.of(Role.CENTRAL_ADMIN));
                users.save(centralAdmin);
                log.info("Seeded central admin user");
            }

            // ── Buildings + Timetables (idempotent) ──
            ensureBuildingAndTimetableExists(buildings, timetables, "BLD_A", "Building A");
            ensureBuildingAndTimetableExists(buildings, timetables, "BLD_B", "Building B");

            // ── Floors + Rooms (idempotent) ──
            buildings.findByCode("BLD_A").ifPresent(b -> ensureFloorsAndRoomsExist(b, floors, rooms, true));
            buildings.findByCode("BLD_B").ifPresent(b -> ensureFloorsAndRoomsExist(b, floors, rooms, false));
            buildings.findByCode("BLD_A").ifPresent(b -> seedBuildingAFixedTimetableIfMissing(b, rooms, fixedTimetables));

            // ── Admin users ──
            syncBuildingAdmin(buildings, users, "ab1_admin", "BLD_A", AdminScope.LARGE_HALL);
            syncBuildingAdmin(buildings, users, "ab2_admin", "BLD_B", AdminScope.LARGE_HALL);

            if (users.findByUsername("ab1_admin").isEmpty()) {
                User ab1Admin = new User();
                ab1Admin.setUsername("ab1_admin");
                ab1Admin.setPasswordHash(encoder.encode("Admin@AB1"));
                ab1Admin.setEmail("ab1@example.com");
                ab1Admin.setRoles(Set.of(Role.BUILDING_ADMIN));
                ab1Admin.setAdminScope(AdminScope.LARGE_HALL);
                buildings.findByCode("BLD_A").ifPresentOrElse(
                        b -> ab1Admin.setManagedBuildingId(b.getId()),
                        () -> ab1Admin.setManagedBuildingId(1L));
                users.save(ab1Admin);
                log.info("Seeded AB1 building admin (large hall)");
            }

            if (users.findByUsername("ab2_admin").isEmpty()) {
                User ab2Admin = new User();
                ab2Admin.setUsername("ab2_admin");
                ab2Admin.setPasswordHash(encoder.encode("Admin@AB2"));
                ab2Admin.setEmail("ab2@example.com");
                ab2Admin.setRoles(Set.of(Role.BUILDING_ADMIN));
                ab2Admin.setAdminScope(AdminScope.LARGE_HALL);
                buildings.findByCode("BLD_B").ifPresentOrElse(
                        b -> ab2Admin.setManagedBuildingId(b.getId()),
                        () -> ab2Admin.setManagedBuildingId(2L));
                users.save(ab2Admin);
                log.info("Seeded AB2 building admin (large hall)");
            }

            seedBuildingRoomAdminIfAbsent(users, buildings, encoder, "building_a_room", "RoomA@123",
                    "bld-a-room@example.com", "BLD_A", AdminScope.NORMAL_ROOM);
            seedBuildingRoomAdminIfAbsent(users, buildings, encoder, "building_b_room", "RoomB@123",
                    "bld-b-room@example.com", "BLD_B", AdminScope.NORMAL_ROOM);

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
                // Find the first active building to assign to sample events
                Building defaultBuilding = buildings.findByCode("BLD_A")
                        .orElseGet(() -> buildings.findByIsActiveTrue().stream()
                                .findFirst()
                                .orElseGet(() -> {
                                    log.warn("No active buildings found. Creating a default building for seed events.");
                                    Building b = new Building("Main Building", "MAIN", "Default building for seed data");
                                    return buildings.save(b);
                                }));

                Event e1 = new Event();
                e1.setTitle("Orientation Week");
                e1.setDescription("Welcome event for new students");
                e1.setStartTime(LocalDateTime.now().plusDays(3).withHour(10).withMinute(0));
                e1.setEndTime(LocalDateTime.now().plusDays(3).withHour(12).withMinute(0));
                e1.setPublic(true);
                e1.setBuilding(defaultBuilding);
                events.save(e1);

                Event e2 = new Event();
                e2.setTitle("Tech Talk: Spring Boot Security");
                e2.setDescription("Deep dive into JWT and RBAC");
                e2.setStartTime(LocalDateTime.now().plusDays(7).withHour(15).withMinute(0));
                e2.setEndTime(LocalDateTime.now().plusDays(7).withHour(17).withMinute(0));
                e2.setPublic(true);
                e2.setBuilding(defaultBuilding);
                events.save(e2);

                log.info("Seeded sample events assigned to building: {}", defaultBuilding.getName());
            }
        };
    }

    // ── Building + Timetable (unchanged) ──

    private static void ensureBuildingAndTimetableExists(BuildingRepository buildings, BuildingTimetableRepository timetables, String code, String name) {
        Building b = buildings.findByCode(code).orElseGet(() -> {
            Building newBuilding = new Building(name, code, "Structured campus building " + name);

            buildings.save(newBuilding);
            log.info("Seeded missing building: {}", code);
            return newBuilding;
        });

        // Ensure timetable exists for all 7 days
        for (DayOfWeek day : DayOfWeek.values()) {
            if (timetables.findByBuilding_IdAndDayOfWeekOrderByStartTimeAsc(b.getId(), day).isEmpty()) {
                BuildingTimetable bt = new BuildingTimetable();
                bt.setBuilding(b);
                bt.setDayOfWeek(day);
                bt.setStartTime(LocalTime.of(8, 0));
                bt.setEndTime(LocalTime.of(22, 0));
                timetables.save(bt);
            }
        }
    }

    // ── Floors + Rooms (NEW — idempotent) ──

    private static void ensureFloorsAndRoomsExist(Building building, FloorRepository floors,
                                                   RoomRepository rooms, boolean hasAuditorium) {
        String code = building.getCode();
        String prefix = "BLD_A".equals(code) ? "A" : "B";

        // Floor 0 — Ground Floor
        Floor ground = ensureFloorExists(building, floors, 0, "Ground Floor");
        ensureRoomExists(rooms, ground, prefix + "-G01", "Lecture Hall 1", RoomType.LECTURE_HALL, 120);
        ensureRoomExists(rooms, ground, prefix + "-G02", "Lecture Hall 2", RoomType.LECTURE_HALL, 120);
        ensureRoomExists(rooms, ground, prefix + "-G03", "Computer Lab", RoomType.LAB, 60);
        if (hasAuditorium) {
            ensureRoomExists(rooms, ground, prefix + "-G04", "Main Auditorium", RoomType.AUDITORIUM, 300);
        }

        // Floor 1 — First Floor
        Floor first = ensureFloorExists(building, floors, 1, "First Floor");
        ensureRoomExists(rooms, first, prefix + "-101", "Classroom 1", RoomType.CLASSROOM, 40);
        ensureRoomExists(rooms, first, prefix + "-102", "Classroom 2", RoomType.CLASSROOM, 40);
        ensureRoomExists(rooms, first, prefix + "-103", "Seminar Hall", RoomType.SEMINAR_HALL, 80);
        ensureRoomExists(rooms, first, prefix + "-104", "Meeting Room 1", RoomType.MEETING_ROOM, 20);

        // Floor 2 — Second Floor
        Floor second = ensureFloorExists(building, floors, 2, "Second Floor");
        ensureRoomExists(rooms, second, prefix + "-201", "Classroom 3", RoomType.CLASSROOM, 40);
        ensureRoomExists(rooms, second, prefix + "-202", "Classroom 4", RoomType.CLASSROOM, 40);
        if (hasAuditorium) {
            ensureRoomExists(rooms, second, prefix + "-203", "Physics Lab", RoomType.LAB, 30);
            ensureRoomExists(rooms, second, prefix + "-204", "Chemistry Lab", RoomType.LAB, 30);
        } else {
            ensureRoomExists(rooms, second, prefix + "-203", "Electronics Lab", RoomType.LAB, 30);
            ensureRoomExists(rooms, second, prefix + "-204", "Workshop", RoomType.LAB, 30);
        }

        log.info("Ensured floors and rooms exist for building: {} ({})", building.getName(), code);
    }

    /**
     * Finds or creates a floor for the given building and floor number.
     * Idempotent: uses existsByBuildingIdAndFloorNumber before inserting.
     */
    private static Floor ensureFloorExists(Building building, FloorRepository floors,
                                            int floorNumber, String name) {
        Floor existing = floors.findByBuildingIdAndFloorNumber(building.getId(), floorNumber);
        if (existing != null) {
            return existing;
        }
        Floor floor = new Floor(floorNumber, name, building);
        floors.save(floor);
        log.info("  Created floor: {} (#{}) in {}", name, floorNumber, building.getName());
        return floor;
    }

    /**
     * Finds or creates a room on the given floor by name.
     * Idempotent: uses findByNameAndFloorId before inserting.
     */
    private static void ensureRoomExists(RoomRepository rooms, Floor floor,
                                          String roomNumber, String name,
                                          RoomType type, int capacity) {
        if (rooms.findByNameAndFloorId(name, floor.getId()).isPresent()) {
            return; // already seeded
        }
        Room room = new Room(roomNumber, name, type, capacity, floor);
        room.setAmenities("Projector, Whiteboard, WiFi");
        rooms.save(room);
        log.info("    Created room: {} ({}) — {} seats, type={}", name, roomNumber, capacity, type);
    }

    // ── Admin helpers (unchanged) ──

    private static void syncBuildingAdmin(BuildingRepository buildings, UserRepository users,
                                          String username, String buildingCode, AdminScope scope) {
        users.findByUsername(username).ifPresent(u -> {
            if (!u.getRoles().contains(Role.BUILDING_ADMIN)) {
                return;
            }
            buildings.findByCode(buildingCode).ifPresent(b -> {
                boolean changed = false;
                if (u.getManagedBuildingId() == null || !u.getManagedBuildingId().equals(b.getId())) {
                    u.setManagedBuildingId(b.getId());
                    changed = true;
                }
                if (u.getAdminScope() != scope) {
                    u.setAdminScope(scope);
                    changed = true;
                }
                if (changed) {
                    users.save(u);
                }
            });
        });
    }

    private static void seedBuildingRoomAdminIfAbsent(UserRepository users, BuildingRepository buildings,
                                                      PasswordEncoder encoder, String username, String rawPassword,
                                                      String email, String buildingCode, AdminScope scope) {
        if (users.findByUsername(username).isPresent()) {
            return;
        }
        buildings.findByCode(buildingCode).ifPresent(b -> {
            User u = new User();
            u.setUsername(username);
            u.setPasswordHash(encoder.encode(rawPassword));
            u.setEmail(email);
            u.setRoles(Set.of(Role.BUILDING_ADMIN));
            u.setManagedBuildingId(b.getId());
            u.setAdminScope(scope);
            users.save(u);
            log.info("Seeded user {} for building {}", username, buildingCode);
        });
    }

    private static void seedBuildingAFixedTimetableIfMissing(Building building,
                                                             RoomRepository rooms,
                                                             FixedTimetableRepository fixedTimetables) {
        final Random random = new Random(42L);
        final String academicYear = String.valueOf(LocalDateTime.now().getYear());
        final LocalTime[][] windows = new LocalTime[][]{
                {LocalTime.of(9, 0), LocalTime.of(11, 0)},
                {LocalTime.of(11, 0), LocalTime.of(13, 0)},
                {LocalTime.of(14, 0), LocalTime.of(16, 0)}
        };

        for (Room room : rooms.findByBuildingIdAndIsActiveTrue(building.getId())) {

            for (DayOfWeek day : DayOfWeek.values()) {
                if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) continue;
                if (!fixedTimetables.findByRoomIdAndDayOfWeekOrderByStartTimeAsc(room.getId(), day).isEmpty()) {
                    continue;
                }
                for (int i = 0; i < windows.length; i++) {
                    if (!random.nextBoolean()) continue;
                    FixedTimetable ft = new FixedTimetable();
                    ft.setRoom(room);
                    ft.setCourseName("BLOCKED_SLOT");
                    ft.setCourseCode("BLD-A-FIXED-" + (i + 1));
                    ft.setSection("AUTO");
                    ft.setSemester("AUTO");
                    ft.setBatch("AUTO");
                    ft.setDayOfWeek(day);
                    ft.setStartTime(windows[i][0]);
                    ft.setEndTime(windows[i][1]);
                    ft.setAcademicYear(academicYear);
                    ft.setActive(true);
                    fixedTimetables.save(ft);
                }
            }
        }
    }
}
