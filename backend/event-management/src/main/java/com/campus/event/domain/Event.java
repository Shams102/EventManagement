package com.campus.event.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "events")
public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String title;

    private String description;

    @NotNull
    private LocalDateTime startTime;

    @NotNull
    private LocalDateTime endTime;

    @Transient
    public boolean isMultiDay() {
        return startTime != null && endTime != null && !startTime.toLocalDate().isEqual(endTime.toLocalDate());
    }

    private boolean isPublic = true;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "created_by_id")
    private User createdBy;

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "building_id", nullable = false)
    private Building building;

    private String location;

    private String clubId;

    private Integer maxAttendees;

    @Column(columnDefinition = "TEXT")
    private String registrationSchema; // JSON array of field keys

    @Enumerated(EnumType.STRING)
    @Column(name = "timing_model", nullable = false)
    private EventTimingModel timingModel = EventTimingModel.SINGLE_DAY;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("slotStart ASC")
    private java.util.List<EventTimeSlot> timeSlots = new java.util.ArrayList<>();

    // Explicit getters and setters
    public Long getId() { return id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    public boolean isPublic() { return isPublic; }
    public void setPublic(boolean isPublic) { this.isPublic = isPublic; }
    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }
    public Building getBuilding() { return building; }
    public void setBuilding(Building building) { this.building = building; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getClubId() { return clubId; }
    public void setClubId(String clubId) { this.clubId = clubId; }
    public Integer getMaxAttendees() { return maxAttendees; }
    public void setMaxAttendees(Integer maxAttendees) { this.maxAttendees = maxAttendees; }
    public String getRegistrationSchema() { return registrationSchema; }
    public void setRegistrationSchema(String registrationSchema) { this.registrationSchema = registrationSchema; }
    public EventTimingModel getTimingModel() { return timingModel; }
    public void setTimingModel(EventTimingModel timingModel) { this.timingModel = timingModel; }
    public java.util.List<EventTimeSlot> getTimeSlots() { return timeSlots; }
    public void setTimeSlots(java.util.List<EventTimeSlot> timeSlots) { this.timeSlots = timeSlots; }
}
