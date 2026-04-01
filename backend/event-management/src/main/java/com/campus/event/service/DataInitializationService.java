package com.campus.event.service;

import com.campus.event.domain.*;
import com.campus.event.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Component
public class DataInitializationService implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializationService.class);
    
    private final RoomManagementService roomManagementService;
    private final ScheduleService scheduleService;
    
    public DataInitializationService(RoomManagementService roomManagementService,
                                   ScheduleService scheduleService) {
        this.roomManagementService = roomManagementService;
        this.scheduleService = scheduleService;
    }
    
    @Override
    public void run(String... args) throws Exception {
        // Initialize default building structure
        roomManagementService.initializeDefaultBuilding();
        
        // Add some sample fixed timetable entries
        addSampleFixedTimetable();
    }
    
    private void addSampleFixedTimetable() {
        try {
            // Get some rooms for sample classes
            var rooms = roomManagementService.getRoomsByBuilding(1L); // Assuming first building
            
            if (!rooms.isEmpty()) {
                // Seed a realistic weekly schedule across multiple rooms.
                // Intentionally leaves many slots free for meetings / events.

                int roomCount = rooms.size();
                int maxRoomsToSeed = Math.min(roomCount, 8);

                // Room 0: heavier load
                addFixedClass(rooms.get(0), "BTECH-005 MACHINE LEARNING", "BTECH-005",
                    "A", "5th", "2023", DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(9, 50));
                addFixedClass(rooms.get(0), "BTECH-005 MACHINE LEARNING LAB", "BTECH-005L",
                    "A", "5th", "2023", DayOfWeek.MONDAY, LocalTime.of(9, 50), LocalTime.of(10, 40));
                addFixedClass(rooms.get(0), "BTECH-008 OPERATING SYSTEMS", "BTECH-008",
                    "A", "5th", "2023", DayOfWeek.WEDNESDAY, LocalTime.of(11, 30), LocalTime.of(12, 20));

                if (maxRoomsToSeed > 1) {
                    addFixedClass(rooms.get(1), "BTECH-006 DATA STRUCTURES", "BTECH-006",
                        "B", "3rd", "2024", DayOfWeek.TUESDAY, LocalTime.of(10, 40), LocalTime.of(11, 30));
                    addFixedClass(rooms.get(1), "BTECH-006 DATA STRUCTURES", "BTECH-006",
                        "B", "3rd", "2024", DayOfWeek.THURSDAY, LocalTime.of(14, 0), LocalTime.of(14, 50));
                }

                if (maxRoomsToSeed > 2) {
                    addFixedClass(rooms.get(2), "BTECH-007 WEB DEVELOPMENT", "BTECH-007",
                        "C", "5th", "2023", DayOfWeek.WEDNESDAY, LocalTime.of(14, 0), LocalTime.of(14, 50));
                    addFixedClass(rooms.get(2), "BTECH-009 DATABASE SYSTEMS", "BTECH-009",
                        "C", "5th", "2023", DayOfWeek.FRIDAY, LocalTime.of(9, 0), LocalTime.of(9, 50));
                }

                if (maxRoomsToSeed > 3) {
                    addFixedClass(rooms.get(3), "MTECH-101 ADVANCED AI", "MTECH-101",
                        "A", "1st", "2025", DayOfWeek.MONDAY, LocalTime.of(13, 10), LocalTime.of(14, 0));
                    addFixedClass(rooms.get(3), "MTECH-102 DISTRIBUTED SYSTEMS", "MTECH-102",
                        "A", "1st", "2025", DayOfWeek.WEDNESDAY, LocalTime.of(15, 40), LocalTime.of(16, 30));
                }

                if (maxRoomsToSeed > 4) {
                    addFixedClass(rooms.get(4), "BCA-201 JAVA PROGRAMMING", "BCA-201",
                        "A", "3rd", "2024", DayOfWeek.TUESDAY, LocalTime.of(9, 0), LocalTime.of(9, 50));
                    addFixedClass(rooms.get(4), "BCA-202 WEB TECH", "BCA-202",
                        "A", "3rd", "2024", DayOfWeek.THURSDAY, LocalTime.of(9, 50), LocalTime.of(10, 40));
                }

                if (maxRoomsToSeed > 5) {
                    addFixedClass(rooms.get(5), "MBA-301 BUSINESS ANALYTICS", "MBA-301",
                        "A", "3rd", "2024", DayOfWeek.FRIDAY, LocalTime.of(14, 0), LocalTime.of(14, 50));
                }

                if (maxRoomsToSeed > 6) {
                    addFixedClass(rooms.get(6), "PHY-101 PHYSICS", "PHY-101",
                        "A", "1st", "2025", DayOfWeek.MONDAY, LocalTime.of(10, 40), LocalTime.of(11, 30));
                    addFixedClass(rooms.get(6), "CHE-101 CHEMISTRY", "CHE-101",
                        "A", "1st", "2025", DayOfWeek.WEDNESDAY, LocalTime.of(10, 40), LocalTime.of(11, 30));
                }

                if (maxRoomsToSeed > 7) {
                    addFixedClass(rooms.get(7), "ENG-101 COMMUNICATION", "ENG-101",
                        "A", "1st", "2025", DayOfWeek.TUESDAY, LocalTime.of(11, 30), LocalTime.of(12, 20));
                }
            }
        } catch (Exception e) {
            // Classes might already exist, ignore
            log.debug("Sample timetable seeding skipped: {}", e.getMessage());
        }
    }
    
    private void addFixedClass(Room room, String courseName, String courseCode, String section,
                              String semester, String batch, DayOfWeek dayOfWeek, 
                              LocalTime startTime, LocalTime endTime) {
        try {
            ScheduleService.FixedTimetableRequest request = new ScheduleService.FixedTimetableRequest();
            request.setRoomId(room.getId());
            request.setCourseName(courseName);
            request.setCourseCode(courseCode);
            request.setSection(section);
            request.setSemester(semester);
            request.setBatch(batch);
            request.setDayOfWeek(dayOfWeek);
            request.setStartTime(startTime);
            request.setEndTime(endTime);
            request.setAcademicYear("2025-2026");
            
            scheduleService.addFixedClass(request);
        } catch (Exception e) {
            // Class might already exist, ignore
            log.debug("Fixed class already exists: {}", courseName);
        }
    }
}
