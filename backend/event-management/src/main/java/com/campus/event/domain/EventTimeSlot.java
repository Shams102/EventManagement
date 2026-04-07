package com.campus.event.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * Represents one time window for an event. Single-day events have one slot;
 * multi-day-fixed events have one slot per day; flexible events can have
 * arbitrary user-defined slots.
 */
@Entity
@Table(name = "event_time_slots")
public class EventTimeSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    @NotNull
    private Event event;

    @NotNull
    private LocalDateTime slotStart;

    @NotNull
    private LocalDateTime slotEnd;

    /** Day index within the event (0-based). Useful for display ordering. */
    private Integer dayIndex;

    public EventTimeSlot() {}

    public EventTimeSlot(Event event, LocalDateTime slotStart, LocalDateTime slotEnd, Integer dayIndex) {
        this.event = event;
        this.slotStart = slotStart;
        this.slotEnd = slotEnd;
        this.dayIndex = dayIndex;
    }

    public Long getId() { return id; }
    public Event getEvent() { return event; }
    public void setEvent(Event event) { this.event = event; }
    public LocalDateTime getSlotStart() { return slotStart; }
    public void setSlotStart(LocalDateTime slotStart) { this.slotStart = slotStart; }
    public LocalDateTime getSlotEnd() { return slotEnd; }
    public void setSlotEnd(LocalDateTime slotEnd) { this.slotEnd = slotEnd; }
    public Integer getDayIndex() { return dayIndex; }
    public void setDayIndex(Integer dayIndex) { this.dayIndex = dayIndex; }
}
