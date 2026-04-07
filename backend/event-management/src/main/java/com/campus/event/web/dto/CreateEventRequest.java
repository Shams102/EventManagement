package com.campus.event.web.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public class CreateEventRequest {
    @NotBlank
    private String title;
    private String description;
    @NotNull
    private LocalDateTime start;
    @NotNull
    @Future
    private LocalDateTime end;

    @NotNull(message = "Building is required. Every event must belong to a building.")
    private Long buildingId;

    private String location; // may be "TBD" initially
    private String clubId;
    private Integer maxAttendees;
    private String registrationSchema; // JSON array string of field keys

    /** SINGLE_DAY (default), MULTI_DAY_FIXED, MULTI_DAY_CONTINUOUS, FLEXIBLE */
    private String timingModel;

    /** For MULTI_DAY_FIXED / FLEXIBLE: explicit time slots [{"slotStart": "...", "slotEnd": "..."}] */
    private java.util.List<TimeSlotInput> timeSlots;

    public static class TimeSlotInput {
        private java.time.LocalDateTime slotStart;
        private java.time.LocalDateTime slotEnd;
        public java.time.LocalDateTime getSlotStart() { return slotStart; }
        public void setSlotStart(java.time.LocalDateTime slotStart) { this.slotStart = slotStart; }
        public java.time.LocalDateTime getSlotEnd() { return slotEnd; }
        public void setSlotEnd(java.time.LocalDateTime slotEnd) { this.slotEnd = slotEnd; }
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDateTime getStart() { return start; }
    public void setStart(LocalDateTime start) { this.start = start; }
    public LocalDateTime getEnd() { return end; }
    public void setEnd(LocalDateTime end) { this.end = end; }
    public Long getBuildingId() { return buildingId; }
    public void setBuildingId(Long buildingId) { this.buildingId = buildingId; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getClubId() { return clubId; }
    public void setClubId(String clubId) { this.clubId = clubId; }
    public Integer getMaxAttendees() { return maxAttendees; }
    public void setMaxAttendees(Integer maxAttendees) { this.maxAttendees = maxAttendees; }
    public String getRegistrationSchema() { return registrationSchema; }
    public void setRegistrationSchema(String registrationSchema) { this.registrationSchema = registrationSchema; }
    public String getTimingModel() { return timingModel; }
    public void setTimingModel(String timingModel) { this.timingModel = timingModel; }
    public java.util.List<TimeSlotInput> getTimeSlots() { return timeSlots; }
    public void setTimeSlots(java.util.List<TimeSlotInput> timeSlots) { this.timeSlots = timeSlots; }
}
