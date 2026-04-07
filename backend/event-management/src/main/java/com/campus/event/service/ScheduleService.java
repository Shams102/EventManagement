package com.campus.event.service;

import com.campus.event.domain.*;
import com.campus.event.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class ScheduleService {
    
    private final FixedTimetableRepository fixedTimetableRepository;
    private final RoomRepository roomRepository;
    private final RoomBookingRequestRepository bookingRepository;
    private final UserRepository userRepository;
    private final BuildingTimetableService buildingTimetableService;

    public ScheduleService(FixedTimetableRepository fixedTimetableRepository,
                         RoomRepository roomRepository,
                         RoomBookingRequestRepository bookingRepository,
                         UserRepository userRepository,
                         BuildingTimetableService buildingTimetableService) {
        this.fixedTimetableRepository = fixedTimetableRepository;
        this.roomRepository = roomRepository;
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.buildingTimetableService = buildingTimetableService;
    }
    
    // Fixed Timetable operations
    public List<FixedTimetable> getRoomWeeklySchedule(Long roomId) {
        return fixedTimetableRepository.findByRoomIdOrderByDayOfWeekAscStartTimeAsc(roomId);
    }
    
    public List<FixedTimetable> getRoomDaySchedule(Long roomId, DayOfWeek dayOfWeek) {
        return fixedTimetableRepository.getRoomDaySchedule(roomId, dayOfWeek);
    }
    
    public List<FixedTimetable> getAllFixedTimetables() {
        return fixedTimetableRepository.findAllActiveOrderByDayAndTime();
    }
    
    public FixedTimetable addFixedClass(FixedTimetableRequest request) {
        if (request.getRoomId() == null) {
            throw new IllegalArgumentException("Room ID cannot be null");
        }
        Room room = roomRepository.findById(request.getRoomId())
            .orElseThrow(() -> new IllegalArgumentException("Room not found"));
            
        User faculty = request.getFacultyId() != null ? 
            userRepository.findById(request.getFacultyId()).orElse(null) : null;
        
        // Check for conflicts
        if (hasTimeSlotConflict(request.getRoomId(), request.getDayOfWeek(), 
                               request.getStartTime(), request.getEndTime())) {
            throw new IllegalArgumentException("Time slot already occupied");
        }
        
        FixedTimetable timetable = new FixedTimetable(
            room, request.getCourseName(), request.getCourseCode(), request.getSection(),
            request.getSemester(), request.getBatch(), faculty, request.getDayOfWeek(),
            request.getStartTime(), request.getEndTime(), request.getAcademicYear()
        );
        
        return fixedTimetableRepository.save(timetable);
    }
    
    // Collision detection
    public boolean hasTimeSlotConflict(Long roomId, DayOfWeek dayOfWeek, 
                                     LocalTime startTime, LocalTime endTime) {
        return fixedTimetableRepository.existsByRoomIdAndDayOfWeekAndStartTimeBeforeAndEndTimeAfterAndIsActiveTrue(
            roomId, dayOfWeek, endTime, startTime
        );
    }
    
    public boolean hasBookingConflict(Long roomId, LocalDate date, 
                                    LocalTime startTime, LocalTime endTime) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        
        // Check against fixed timetable
        boolean hasFixedConflict = hasTimeSlotConflict(roomId, dayOfWeek, startTime, endTime);
        if (hasFixedConflict) return true;
        
        // Check against existing bookings
        List<RoomBookingRequest> conflictingBookings = bookingRepository.findConflictingBookings(roomId, 
            LocalDateTime.of(date, startTime), LocalDateTime.of(date, endTime));
        return !conflictingBookings.isEmpty();
    }
    
    public Map<String, List<String>> validateEventRoomPreferences(Long pref1Id, Long pref2Id, Long pref3Id, LocalDateTime startTime, LocalDateTime endTime) {
        Map<String, List<String>> conflicts = new HashMap<>();
        if (pref1Id != null) conflicts.put(pref1Id.toString(), getRoomConflicts(pref1Id, startTime, endTime));
        if (pref2Id != null) conflicts.put(pref2Id.toString(), getRoomConflicts(pref2Id, startTime, endTime));
        if (pref3Id != null) conflicts.put(pref3Id.toString(), getRoomConflicts(pref3Id, startTime, endTime));
        return conflicts;
    }

    /**
     * Multi-slot-aware validation: checks each time slot individually for conflicts.
     * Used for MULTI_DAY_FIXED and FLEXIBLE events where conflicts must be checked per-day.
     */
    public Map<String, List<String>> validateEventRoomPreferencesMultiSlot(
            Long pref1Id, Long pref2Id, Long pref3Id,
            List<com.campus.event.domain.EventTimeSlot> timeSlots) {
        if (timeSlots == null || timeSlots.isEmpty()) {
            return new HashMap<>();
        }

        // If single slot, delegate to the standard method
        if (timeSlots.size() == 1) {
            com.campus.event.domain.EventTimeSlot slot = timeSlots.get(0);
            return validateEventRoomPreferences(pref1Id, pref2Id, pref3Id, slot.getSlotStart(), slot.getSlotEnd());
        }

        Map<String, List<String>> conflicts = new HashMap<>();
        Long[] prefIds = {pref1Id, pref2Id, pref3Id};
        for (Long prefId : prefIds) {
            if (prefId == null) continue;
            List<String> allMessages = new ArrayList<>();
            for (com.campus.event.domain.EventTimeSlot slot : timeSlots) {
                List<String> slotConflicts = getRoomConflicts(prefId, slot.getSlotStart(), slot.getSlotEnd());
                for (String msg : slotConflicts) {
                    allMessages.add("[Day " + (slot.getDayIndex() != null ? slot.getDayIndex() + 1 : "?") + "] " + msg);
                }
            }
            conflicts.put(prefId.toString(), allMessages);
        }
        return conflicts;
    }

    public List<String> getRoomConflicts(Long roomId, LocalDateTime start, LocalDateTime end) {
        List<String> messages = new ArrayList<>();
        Room room = roomRepository.findById(roomId).orElse(null);
        if (room == null) return messages;

        Long buildingId = room.getFloor() != null && room.getFloor().getBuilding() != null
                ? room.getFloor().getBuilding().getId() : null;
        if (buildingId != null && !buildingTimetableService.isBookingWithinBuildingHours(buildingId, start, end)) {
            messages.add("Requested time is outside this building's operating hours (see building timetable).");
        }

        // Check against existing bookings for the whole period
        List<RoomBookingRequest> conflictingBookings = bookingRepository.findConflictingBookings(roomId, start, end);
        for (RoomBookingRequest b : conflictingBookings) {
            String title = b.getEvent() != null ? b.getEvent().getTitle() : b.getMeetingPurpose();
            messages.add("Booking conflict: " + title);
        }

        // Check against timetable for each day spanned by the event
        LocalDate currentDate = start.toLocalDate();
        LocalDate endDate = end.toLocalDate();
        LocalTime timeStart = start.toLocalTime();
        LocalTime timeEnd = end.toLocalTime();

        while (!currentDate.isAfter(endDate)) {
            DayOfWeek day = currentDate.getDayOfWeek();
            
            LocalTime checkStart = (currentDate.isEqual(start.toLocalDate())) ? timeStart : LocalTime.MIN;
            LocalTime checkEnd = (currentDate.isEqual(endDate)) ? timeEnd : LocalTime.MAX;

            List<FixedTimetable> timetableConflicts = fixedTimetableRepository.findByRoomIdOrderByDayOfWeekAscStartTimeAsc(roomId).stream()
                .filter(ft -> ft.isActive() && ft.getDayOfWeek() == day &&
                        ft.getStartTime().isBefore(checkEnd) && ft.getEndTime().isAfter(checkStart))
                .collect(Collectors.toList());

            for (FixedTimetable ft : timetableConflicts) {
                messages.add("Timetable conflict on " + day + " (" + currentDate + "): " + ft.getCourseCode() + " from " + ft.getStartTime() + " to " + ft.getEndTime());
            }

            currentDate = currentDate.plusDays(1);
        }

        return messages;
    }
    
    public List<String> getAvailableSlots(Long roomId, LocalDate date) {
        List<String> availableSlots = new ArrayList<>();
        
        for (TimeSlot slot : TimeSlot.values()) {
            if (!hasBookingConflict(roomId, date, slot.getStart(), slot.getEnd())) {
                availableSlots.add(slot.getDisplayName());
            }
        }
        
        return availableSlots;
    }
    
    // Combined schedule for a specific day
    public List<ScheduleItem> getRoomCombinedSchedule(Long roomId, LocalDate date) {
        List<ScheduleItem> scheduleItems = new ArrayList<>();
        
        // Get fixed classes for that day
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        List<FixedTimetable> fixedClasses = getRoomDaySchedule(roomId, dayOfWeek);
        
        // Convert fixed classes to schedule items
        for (FixedTimetable ft : fixedClasses) {
            ScheduleItem item = new ScheduleItem();
            item.setType("FIXED_CLASS");
            item.setTitle(ft.getCourseName());
            item.setSubtitle(ft.getCourseCode() + " - " + ft.getSection());
            item.setStartTime(ft.getStartTime());
            item.setEndTime(ft.getEndTime());
            item.setFacultyName(ft.getFaculty() != null ? ft.getFaculty().getUsername() : "");
            scheduleItems.add(item);
        }
        
        // Get bookings for that day
        List<RoomBookingRequest> bookings = bookingRepository.findByAllocatedRoomIdAndDateBookings(roomId, date);
        
        // Convert bookings to schedule items
        for (RoomBookingRequest booking : bookings) {
            if (booking.getStatus() == RoomBookingStatus.APPROVED || booking.getStatus() == RoomBookingStatus.CONFIRMED) {
                ScheduleItem item = new ScheduleItem();
                item.setType("BOOKING");
                item.setTitle(booking.getEvent() != null ? booking.getEvent().getTitle() : "Meeting");
                item.setSubtitle("Booking");
                item.setStartTime(booking.getStartTime());
                item.setEndTime(booking.getEndTime());
                item.setFacultyName(booking.getEvent() != null && booking.getEvent().getCreatedBy() != null ? 
                    booking.getEvent().getCreatedBy().getUsername() : "");
                scheduleItems.add(item);
            }
        }
        
        // Sort by start time
        return scheduleItems.stream()
            .sorted(Comparator.comparing(ScheduleItem::getStartTime))
            .collect(Collectors.toList());
    }
    
    // DTO for requests
    public static class FixedTimetableRequest {
        private Long roomId;
        private String courseName;
        private String courseCode;
        private String section;
        private String semester;
        private String batch;
        private Long facultyId;
        private DayOfWeek dayOfWeek;
        private LocalTime startTime;
        private LocalTime endTime;
        private String academicYear;
        
        // Getters and setters
        public Long getRoomId() { return roomId; }
        public void setRoomId(Long roomId) { this.roomId = roomId; }
        public String getCourseName() { return courseName; }
        public void setCourseName(String courseName) { this.courseName = courseName; }
        public String getCourseCode() { return courseCode; }
        public void setCourseCode(String courseCode) { this.courseCode = courseCode; }
        public String getSection() { return section; }
        public void setSection(String section) { this.section = section; }
        public String getSemester() { return semester; }
        public void setSemester(String semester) { this.semester = semester; }
        public String getBatch() { return batch; }
        public void setBatch(String batch) { this.batch = batch; }
        public Long getFacultyId() { return facultyId; }
        public void setFacultyId(Long facultyId) { this.facultyId = facultyId; }
        public DayOfWeek getDayOfWeek() { return dayOfWeek; }
        public void setDayOfWeek(DayOfWeek dayOfWeek) { this.dayOfWeek = dayOfWeek; }
        public LocalTime getStartTime() { return startTime; }
        public void setStartTime(LocalTime startTime) { this.startTime = startTime; }
        public LocalTime getEndTime() { return endTime; }
        public void setEndTime(LocalTime endTime) { this.endTime = endTime; }
        public String getAcademicYear() { return academicYear; }
        public void setAcademicYear(String academicYear) { this.academicYear = academicYear; }
    }
    
    // DTO for schedule items
    public static class ScheduleItem {
        private String type;
        private String title;
        private String subtitle;
        private LocalTime startTime;
        private LocalTime endTime;
        private String facultyName;
        
        // Getters and setters
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getSubtitle() { return subtitle; }
        public void setSubtitle(String subtitle) { this.subtitle = subtitle; }
        public LocalTime getStartTime() { return startTime; }
        public void setStartTime(LocalTime startTime) { this.startTime = startTime; }
        public LocalTime getEndTime() { return endTime; }
        public void setEndTime(LocalTime endTime) { this.endTime = endTime; }
        public String getFacultyName() { return facultyName; }
        public void setFacultyName(String facultyName) { this.facultyName = facultyName; }
    }
}
