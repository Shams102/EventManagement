package com.campus.event.service;

import com.campus.event.domain.Event;
import com.campus.event.domain.EventTimeSlot;
import com.campus.event.domain.Room;
import com.campus.event.domain.RoomBookingRequest;
import com.campus.event.domain.RoomBookingStatus;
import com.campus.event.repository.EventTimeSlotRepository;
import com.campus.event.repository.FixedTimetableRepository;
import com.campus.event.repository.RoomRepository;
import com.campus.event.repository.RoomBookingRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RoomAvailabilityService {
    private final RoomBookingRequestRepository requestRepo;
    private final FixedTimetableRepository fixedTimetableRepository;
    private final EventTimeSlotRepository eventTimeSlotRepository;
    private final RoomRepository roomRepository;

    @Autowired
    public RoomAvailabilityService(RoomBookingRequestRepository requestRepo,
                                   FixedTimetableRepository fixedTimetableRepository,
                                   EventTimeSlotRepository eventTimeSlotRepository,
                                   RoomRepository roomRepository) {
        this.requestRepo = requestRepo;
        this.fixedTimetableRepository = fixedTimetableRepository;
        this.eventTimeSlotRepository = eventTimeSlotRepository;
        this.roomRepository = roomRepository;
    }

    // Backward-compatible constructor for existing unit tests
    public RoomAvailabilityService(RoomBookingRequestRepository requestRepo) {
        this(requestRepo, null, null, null);
    }

    public boolean isRoomAvailable(Long roomId, LocalDateTime start, LocalDateTime end) {
        if (hasFixedTimetableConflict(roomId, start, end)) {
            return false;
        }
        List<RoomBookingRequest> existing = requestRepo.findByStatusIn(Set.of(RoomBookingStatus.APPROVED, RoomBookingStatus.CONFIRMED));
        return existing.stream()
                .filter(b -> b.getAllocatedRoom() != null && b.getAllocatedRoom().getId().equals(roomId))
                .noneMatch(b -> hasSlotOverlap(b, start, end));
    }

    public Map<Long, Boolean> availabilityForRooms(List<Long> roomIds, LocalDateTime start, LocalDateTime end) {
        List<RoomBookingRequest> existing = requestRepo.findByStatusIn(Set.of(RoomBookingStatus.APPROVED, RoomBookingStatus.CONFIRMED));
        return roomIds.stream().collect(Collectors.toMap(
                id -> id,
                id -> !hasFixedTimetableConflict(id, start, end) && existing.stream()
                        .filter(b -> b.getAllocatedRoom() != null && b.getAllocatedRoom().getId().equals(id))
                        .noneMatch(b -> hasSlotOverlap(b, start, end))
        ));
    }

    /**
     * Slot-based overlap check: if the existing booking is linked to an event with
     * EventTimeSlots, check overlap against each slot individually. Otherwise fall
     * back to the event/meeting-level window.
     *
     * Overlap formula: existing.start < candidate.end AND existing.end > candidate.start
     */
    private boolean hasSlotOverlap(RoomBookingRequest existing, LocalDateTime candidateStart, LocalDateTime candidateEnd) {
        // Try to get slots for this existing booking's event
        if (existing.getEvent() != null && eventTimeSlotRepository != null) {
            List<EventTimeSlot> slots = eventTimeSlotRepository.findByEvent_IdOrderBySlotStartAsc(existing.getEvent().getId());
            if (!slots.isEmpty()) {
                // Check per-slot overlap
                return slots.stream().anyMatch(slot ->
                        slot.getSlotStart().isBefore(candidateEnd) && candidateStart.isBefore(slot.getSlotEnd()));
            }
        }
        // Fallback: use the event/meeting-level window
        LocalDateTime aStart = windowStart(existing);
        LocalDateTime aEnd = windowEnd(existing);
        return overlaps(aStart, aEnd, candidateStart, candidateEnd);
    }

    /**
     * Fixed timetable conflict check — operates per-slot (per day).
     * Compares slot.dayOfWeek, slot.startTime, slot.endTime against timetable entries.
     */
    private boolean hasFixedTimetableConflict(Long roomId, LocalDateTime start, LocalDateTime end) {
        if (roomId == null || start == null || end == null || !start.isBefore(end)) {
            return false;
        }
        if (fixedTimetableRepository == null) {
            return false;
        }
        LocalDate date = start.toLocalDate();
        LocalDate endDate = end.toLocalDate();
        while (!date.isAfter(endDate)) {
            // Per-slot: determine the time window for this specific day
            LocalTime dayStart = date.isEqual(start.toLocalDate()) ? start.toLocalTime() : LocalTime.MIN;
            LocalTime dayEnd = date.isEqual(endDate) ? end.toLocalTime() : LocalTime.MAX;
            if (fixedTimetableRepository.existsConflictingClass(roomId, date.getDayOfWeek(), dayStart, dayEnd)) {
                return true;
            }
            date = date.plusDays(1);
        }
        return false;
    }

    /**
     * Standard overlap: aStart < bEnd AND bStart < aEnd
     */
    private static boolean overlaps(LocalDateTime aStart, LocalDateTime aEnd, LocalDateTime bStart, LocalDateTime bEnd) {
        if (aStart == null || aEnd == null || bStart == null || bEnd == null) return false;
        return aStart.isBefore(bEnd) && bStart.isBefore(aEnd);
    }

    private static LocalDateTime windowStart(RoomBookingRequest r) {
        Event e = r.getEvent();
        if (e != null && e.getStartTime() != null) return e.getStartTime();
        return r.getMeetingStart();
        
    }

    private static LocalDateTime windowEnd(RoomBookingRequest r) {
        Event e = r.getEvent();
        if (e != null && e.getEndTime() != null) return e.getEndTime();
        return r.getMeetingEnd();
    }

    // ─── Aggregated per-slot availability for an event ───

    /**
     * For every room in the system, checks availability against EACH EventTimeSlot
     * of the given event. Returns aggregated availability (fully/partially/unavailable)
     * plus a per-day breakdown.
     */
    public List<RoomEventAvailability> getEventAvailability(Long eventId) {
        if (eventId == null || eventTimeSlotRepository == null || roomRepository == null) {
            return java.util.Collections.emptyList();
        }
        List<EventTimeSlot> slots = eventTimeSlotRepository.findByEvent_IdOrderBySlotStartAsc(eventId);
        if (slots.isEmpty()) {
            return java.util.Collections.emptyList();
        }

        Event event = slots.get(0).getEvent();
        if (event == null || event.getBuilding() == null || event.getBuilding().getId() == null) {
            return java.util.Collections.emptyList();
        }

        List<Room> rooms = roomRepository.findByBuildingIdAndIsActiveTrue(event.getBuilding().getId()).stream()
                .filter(r -> r.getFloor() != null && r.getFloor().getBuilding() != null)
                .toList();
        return getEventAvailability(eventId, rooms);
    }

    /**
     * Compute event availability for a specific list of rooms.
     */
    public List<RoomEventAvailability> getEventAvailability(Long eventId, List<com.campus.event.domain.Room> rooms) {
        if (eventId == null || eventTimeSlotRepository == null || rooms == null || rooms.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        List<EventTimeSlot> slots = eventTimeSlotRepository.findByEvent_IdOrderBySlotStartAsc(eventId);
        if (slots.isEmpty()) {
            return java.util.Collections.emptyList();
        }

        int totalDays = slots.size();
        List<RoomEventAvailability> result = new java.util.ArrayList<>();

        for (com.campus.event.domain.Room room : rooms) {
            int availableDays = 0;
            List<PerDayAvailability> perDay = new java.util.ArrayList<>();

            for (EventTimeSlot slot : slots) {
                boolean available = isRoomAvailable(room.getId(), slot.getSlotStart(), slot.getSlotEnd());
                if (available) availableDays++;
                perDay.add(new PerDayAvailability(
                        slot.getSlotStart().toLocalDate(),
                        slot.getSlotStart(),
                        slot.getSlotEnd(),
                        slot.getDayIndex(),
                        available
                ));
            }

            result.add(new RoomEventAvailability(
                    room.getId(),
                    room.getName(),
                    room.getCapacity(),
                    room.getType() != null ? room.getType().name() : null,
                    room.getFloor() != null && room.getFloor().getBuilding() != null ? room.getFloor().getBuilding().getId() : null,
                    room.getFloor() != null && room.getFloor().getBuilding() != null ? room.getFloor().getBuilding().getName() : null,
                    availableDays,
                    totalDays,
                    availableDays == totalDays,
                    availableDays > 0 && availableDays < totalDays,
                    perDay
            ));
        }

        return result;
    }

    // ─── DTOs (inner classes) ───

    public static class RoomEventAvailability {
        public final Long roomId;
        public final String roomName;
        public final Integer capacity;
        public final String roomType;
        public final Long buildingId;
        public final String buildingName;
        public final int availableDays;
        public final int totalDays;
        public final boolean fullyAvailable;
        public final boolean partiallyAvailable;
        public final List<PerDayAvailability> perDay;

        public RoomEventAvailability(Long roomId, String roomName, Integer capacity, String roomType,
                                     Long buildingId, String buildingName,
                                     int availableDays, int totalDays,
                                     boolean fullyAvailable, boolean partiallyAvailable,
                                     List<PerDayAvailability> perDay) {
            this.roomId = roomId;
            this.roomName = roomName;
            this.capacity = capacity;
            this.roomType = roomType;
            this.buildingId = buildingId;
            this.buildingName = buildingName;
            this.availableDays = availableDays;
            this.totalDays = totalDays;
            this.fullyAvailable = fullyAvailable;
            this.partiallyAvailable = partiallyAvailable;
            this.perDay = perDay;
        }
    }

    public static class PerDayAvailability {
        public final LocalDate date;
        public final LocalDateTime slotStart;
        public final LocalDateTime slotEnd;
        public final Integer dayIndex;
        public final boolean available;

        public PerDayAvailability(LocalDate date, LocalDateTime slotStart, LocalDateTime slotEnd,
                                  Integer dayIndex, boolean available) {
            this.date = date;
            this.slotStart = slotStart;
            this.slotEnd = slotEnd;
            this.dayIndex = dayIndex;
            this.available = available;
        }
    }
}
