package com.campus.event.service;

import com.campus.event.domain.AdminScope;
import com.campus.event.domain.Event;
import com.campus.event.domain.EventTimeSlot;
import com.campus.event.domain.Room;
import com.campus.event.domain.RoomBookingRequest;
import com.campus.event.domain.RoomBookingStatus;
import com.campus.event.repository.EventTimeSlotRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class EventRoomBookingSplitService {

    private final EventTimeSlotRepository eventTimeSlotRepository;

    public EventRoomBookingSplitService(EventTimeSlotRepository eventTimeSlotRepository) {
        this.eventTimeSlotRepository = eventTimeSlotRepository;
    }

    /**
     * One homogeneous request, or several (same {@link UUID} {@code splitGroupId}) when
     * preferences mix {@link AdminScope} values — see {@link RoomApprovalRules}.
     *
     * Additionally, this method ensures that proper per-day EventTimeSlots exist for
     * overnight and multi-day events. If an event crosses midnight, it splits the
     * event's time range into per-day slots:
     *   Day 1: start → 23:59:59
     *   Middle days: 00:00 → 23:59:59
     *   Last day: 00:00 → end
     */
    public List<RoomBookingRequest> buildEventRequests(Event event, Room r1, Room r2, Room r3, String requestedBy) {
        // Ensure per-day time slots exist for overnight/multi-day events
        ensureTimeSlotsExist(event);

        List<Room> prefs = List.of(r1, r2, r3);
        Map<AdminScope, List<Room>> grouped = new LinkedHashMap<>();
        for (Room r : prefs) {
            AdminScope s = RoomApprovalRules.scopeForRoom(r);
            grouped.computeIfAbsent(s, k -> new ArrayList<>()).add(r);
        }

        if (grouped.size() == 1) {
            RoomBookingRequest rr = baseRequest(event, requestedBy, null);
            rr.setPref1(r1);
            rr.setPref2(r2);
            rr.setPref3(r3);
            return List.of(rr);
        }

        UUID groupId = UUID.randomUUID();
        List<RoomBookingRequest> out = new ArrayList<>();
        for (List<Room> rooms : grouped.values()) {
            RoomBookingRequest rr = baseRequest(event, requestedBy, groupId);
            rr.setPref1(rooms.size() > 0 ? rooms.get(0) : null);
            rr.setPref2(rooms.size() > 1 ? rooms.get(1) : null);
            rr.setPref3(rooms.size() > 2 ? rooms.get(2) : null);
            out.add(rr);
        }
        return out;
    }

    /**
     * Ensures that EventTimeSlots exist for the given event. If the event crosses
     * midnight or spans multiple days AND no slots have been generated yet, this
     * method creates properly split per-day slots.
     *
     * Rules:
     *   - Day 1:      event.startTime → 23:59:59 of that day
     *   - Middle days: 00:00 → 23:59:59
     *   - Last day:    00:00 → event.endTime
     *   - If the event fits within a single calendar day, a single slot is created.
     */
    private void ensureTimeSlotsExist(Event event) {
        if (event == null || event.getStartTime() == null || event.getEndTime() == null) {
            return;
        }

        // Check if slots already exist
        List<EventTimeSlot> existing = eventTimeSlotRepository.findByEvent_IdOrderBySlotStartAsc(event.getId());
        if (!existing.isEmpty()) {
            return; // Slots already generated (e.g., by EventService.createEvent)
        }

        LocalDateTime start = event.getStartTime();
        LocalDateTime end = event.getEndTime();
        LocalDate startDate = start.toLocalDate();
        LocalDate endDate = end.toLocalDate();

        List<EventTimeSlot> slots = new ArrayList<>();

        if (startDate.equals(endDate)) {
            // Same-day event — single slot
            slots.add(new EventTimeSlot(event, start, end, 0));
        } else {
            // Overnight or multi-day event — split into per-day slots
            int dayIndex = 0;
            LocalDate current = startDate;

            while (!current.isAfter(endDate)) {
                LocalDateTime slotStart;
                LocalDateTime slotEnd;

                if (current.equals(startDate)) {
                    // First day: event start → 23:59:59
                    slotStart = start;
                    slotEnd = LocalDateTime.of(current, LocalTime.of(23, 59, 59));
                } else if (current.equals(endDate)) {
                    // Last day: 00:00 → event end
                    slotStart = LocalDateTime.of(current, LocalTime.MIDNIGHT);
                    slotEnd = end;
                } else {
                    // Middle day: 00:00 → 23:59:59
                    slotStart = LocalDateTime.of(current, LocalTime.MIDNIGHT);
                    slotEnd = LocalDateTime.of(current, LocalTime.of(23, 59, 59));
                }

                slots.add(new EventTimeSlot(event, slotStart, slotEnd, dayIndex));
                dayIndex++;
                current = current.plusDays(1);
            }
        }

        eventTimeSlotRepository.saveAll(slots);
    }

    private static RoomBookingRequest baseRequest(Event event, String requestedBy, UUID splitGroupId) {
        RoomBookingRequest rr = new RoomBookingRequest();
        rr.setEvent(event);
        rr.setStatus(RoomBookingStatus.PENDING);
        rr.setRequestedByUsername(requestedBy);
        rr.setSplitGroupId(splitGroupId);
        return rr;
    }
}
