package com.campus.event.service;

import com.campus.event.domain.Event;
import com.campus.event.domain.RoomBookingRequest;
import com.campus.event.domain.RoomBookingStatus;
import com.campus.event.repository.FixedTimetableRepository;
import com.campus.event.repository.RoomBookingRequestRepository;
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

    public RoomAvailabilityService(RoomBookingRequestRepository requestRepo,
                                   FixedTimetableRepository fixedTimetableRepository) {
        this.requestRepo = requestRepo;
        this.fixedTimetableRepository = fixedTimetableRepository;
    }

    public boolean isRoomAvailable(Long roomId, LocalDateTime start, LocalDateTime end) {
        if (hasFixedTimetableConflict(roomId, start, end)) {
            return false;
        }
        List<RoomBookingRequest> existing = requestRepo.findByStatusIn(Set.of(RoomBookingStatus.APPROVED, RoomBookingStatus.CONFIRMED));
        return existing.stream()
                .filter(b -> b.getAllocatedRoom() != null && b.getAllocatedRoom().getId().equals(roomId))
                .noneMatch(b -> overlaps(windowStart(b), windowEnd(b), start, end));
    }

    public Map<Long, Boolean> availabilityForRooms(List<Long> roomIds, LocalDateTime start, LocalDateTime end) {
        List<RoomBookingRequest> existing = requestRepo.findByStatusIn(Set.of(RoomBookingStatus.APPROVED, RoomBookingStatus.CONFIRMED));
        return roomIds.stream().collect(Collectors.toMap(
                id -> id,
                id -> !hasFixedTimetableConflict(id, start, end) && existing.stream()
                        .filter(b -> b.getAllocatedRoom() != null && b.getAllocatedRoom().getId().equals(id))
                        .noneMatch(b -> overlaps(windowStart(b), windowEnd(b), start, end))
        ));
    }

    private boolean hasFixedTimetableConflict(Long roomId, LocalDateTime start, LocalDateTime end) {
        if (roomId == null || start == null || end == null || !start.isBefore(end)) {
            return false;
        }
        LocalDate date = start.toLocalDate();
        LocalDate endDate = end.toLocalDate();
        while (!date.isAfter(endDate)) {
            LocalTime dayStart = date.isEqual(start.toLocalDate()) ? start.toLocalTime() : LocalTime.MIN;
            LocalTime dayEnd = date.isEqual(endDate) ? end.toLocalTime() : LocalTime.MAX;
            if (fixedTimetableRepository.existsConflictingClass(roomId, date.getDayOfWeek(), dayStart, dayEnd)) {
                return true;
            }
            date = date.plusDays(1);
        }
        return false;
    }

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
}
