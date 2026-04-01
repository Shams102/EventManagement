package com.campus.event.web;

import com.campus.event.domain.FixedTimetable;
import com.campus.event.service.ScheduleService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/timetable")
public class TimetableController {
    
    private final ScheduleService scheduleService;
    
    public TimetableController(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }
    
    // Fixed timetable endpoints
    @PostMapping("/fixed")
    @PreAuthorize("hasAnyRole('ADMIN','FACULTY')")
    public ResponseEntity<?> addFixedClass(@RequestBody ScheduleService.FixedTimetableRequest request) {
        try {
            FixedTimetable timetable = scheduleService.addFixedClass(request);
            return ResponseEntity.ok(Map.of(
                "id", timetable.getId(),
                "message", "Fixed class added successfully"
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    
    @GetMapping("/room/{roomId}/week")
    public ResponseEntity<?> getRoomWeeklySchedule(@PathVariable Long roomId) {
        List<FixedTimetable> schedule = scheduleService.getRoomWeeklySchedule(roomId);
        List<Map<String, Object>> response = schedule.stream().map(this::fixedTimetableToMap).collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/room/{roomId}/day/{date}")
    public ResponseEntity<?> getRoomDailySchedule(
        @PathVariable Long roomId,
        @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        try {
            DayOfWeek dayOfWeek = date.getDayOfWeek();
            List<FixedTimetable> fixedClasses = scheduleService.getRoomDaySchedule(roomId, dayOfWeek);
            List<ScheduleService.ScheduleItem> combinedSchedule = scheduleService.getRoomCombinedSchedule(roomId, date);
            
            Map<String, Object> response = new HashMap<>();
            response.put("fixedClasses", fixedClasses.stream().map(this::fixedTimetableToMap).collect(Collectors.toList()));
            response.put("combinedSchedule", combinedSchedule.stream().map(this::scheduleItemToMap).collect(Collectors.toList()));
            response.put("date", date.toString());
            response.put("dayOfWeek", dayOfWeek.toString());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error fetching schedule: " + e.getMessage());
        }
    }
    
    // Time slot availability
    @GetMapping("/room/{roomId}/available-slots")
    public ResponseEntity<?> getAvailableSlots(
        @PathVariable Long roomId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        try {
            List<String> availableSlots = scheduleService.getAvailableSlots(roomId, date);
            return ResponseEntity.ok(Map.of("availableSlots", availableSlots));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error checking availability: " + e.getMessage());
        }
    }
    
    // Collision check
    @PostMapping("/check-conflict")
    public ResponseEntity<?> checkTimeSlotConflict(@RequestBody Map<String, Object> request) {
        try {
            Long roomId = Long.valueOf(request.get("roomId").toString());
            String dateStr = (String) request.get("date");
            LocalDate date = LocalDate.parse(dateStr);
            
            String startTimeStr = (String) request.get("startTime");
            String endTimeStr = (String) request.get("endTime");
            
            boolean hasConflict = scheduleService.hasBookingConflict(
                roomId, date, 
                java.time.LocalTime.parse(startTimeStr),
                java.time.LocalTime.parse(endTimeStr)
            );
            
            return ResponseEntity.ok(Map.of("hasConflict", hasConflict));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error checking conflict: " + e.getMessage());
        }
    }
    
    // Get all fixed timetables (for admin)
    @GetMapping("/fixed/all")
    @PreAuthorize("hasAnyRole('ADMIN','FACULTY')")
    public ResponseEntity<?> getAllFixedTimetables() {
        List<FixedTimetable> timetables = scheduleService.getAllFixedTimetables();
        List<Map<String, Object>> response = timetables.stream().map(this::fixedTimetableToMap).collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }
    
    private Map<String, Object> fixedTimetableToMap(FixedTimetable ft) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", ft.getId());
        map.put("courseName", ft.getCourseName());
        map.put("courseCode", ft.getCourseCode());
        map.put("section", ft.getSection());
        map.put("semester", ft.getSemester());
        map.put("batch", ft.getBatch());
        map.put("dayOfWeek", ft.getDayOfWeek().toString());
        map.put("startTime", ft.getStartTime());
        map.put("endTime", ft.getEndTime());
        map.put("academicYear", ft.getAcademicYear());
        map.put("isActive", ft.isActive());
        
        if (ft.getRoom() != null) {
            map.put("roomId", ft.getRoom().getId());
            map.put("roomNumber", ft.getRoom().getRoomNumber());
            map.put("roomName", ft.getRoom().getName());
        }
        
        if (ft.getFaculty() != null) {
            map.put("facultyId", ft.getFaculty().getId());
            map.put("facultyName", ft.getFaculty().getUsername());
        }
        
        return map;
    }
    
    private Map<String, Object> scheduleItemToMap(ScheduleService.ScheduleItem item) {
        Map<String, Object> map = new HashMap<>();
        map.put("type", item.getType());
        map.put("title", item.getTitle());
        map.put("subtitle", item.getSubtitle());
        map.put("startTime", item.getStartTime());
        map.put("endTime", item.getEndTime());
        map.put("facultyName", item.getFacultyName());
        return map;
    }
}
