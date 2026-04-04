package com.campus.event.service;

import com.campus.event.domain.*;
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
        roomManagementService.initializeBuildings();
        
        // Add some sample fixed timetable entries
        addSampleFixedTimetable();
    }
    
    private void addSampleFixedTimetable() {
        try {
            Long[] buildingIds = {1L, 2L};
            for (Long buildingId : buildingIds) {
                var rooms = roomManagementService.getRoomsByBuilding(buildingId);
                
                if (!rooms.isEmpty()) {
                    int roomCount = rooms.size();
                    int maxRoomsToSeed = Math.min(roomCount, 8);

                    if (maxRoomsToSeed > 0) {
                        addFixedClass(rooms.get(0), "BTECH-005 MACHINE LEARNING", "BTECH-005",
                            "A", "5th", "2023", DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(9, 50));
                        addFixedClass(rooms.get(0), "BTECH-005 MACHINE LEARNING LAB", "BTECH-005L",
                            "A", "5th", "2023", DayOfWeek.MONDAY, LocalTime.of(9, 50), LocalTime.of(10, 40));
                        addFixedClass(rooms.get(0), "BTECH-008 OPERATING SYSTEMS", "BTECH-008",
                            "A", "5th", "2023", DayOfWeek.WEDNESDAY, LocalTime.of(11, 30), LocalTime.of(12, 20));
                    }

                    if (maxRoomsToSeed > 1) {
                        addFixedClass(rooms.get(1), "BTECH-006 DATA STRUCTURES", "BTECH-006",
                            "B", "3rd", "2024", DayOfWeek.TUESDAY, LocalTime.of(10, 40), LocalTime.of(11, 30));
                        addFixedClass(rooms.get(1), "BTECH-006 DATA STRUCTURES", "BTECH-006",
                            "B", "3rd", "2024", DayOfWeek.THURSDAY, LocalTime.of(14, 0), LocalTime.of(14, 50));
                    }
                }
            }
        } catch (Exception e) {
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
