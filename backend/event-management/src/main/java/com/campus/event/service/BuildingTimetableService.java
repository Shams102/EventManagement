package com.campus.event.service;

import com.campus.event.domain.BuildingTimetable;
import com.campus.event.repository.BuildingTimetableRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
public class BuildingTimetableService {

    private final BuildingTimetableRepository buildingTimetableRepository;

    public BuildingTimetableService(BuildingTimetableRepository buildingTimetableRepository) {
        this.buildingTimetableRepository = buildingTimetableRepository;
    }

    /**
     * True when every local-time segment of [start, end] on each calendar day touched
     * lies fully inside at least one configured window for {@code buildingId}.
     */
    @Transactional(readOnly = true)
    public boolean isBookingWithinBuildingHours(Long buildingId, LocalDateTime start, LocalDateTime end) {
        if (buildingId == null || start == null || end == null || !end.isAfter(start)) {
            return true;
        }
        LocalDate day = start.toLocalDate();
        LocalDate last = end.toLocalDate();
        while (!day.isAfter(last)) {
            DayOfWeek dow = day.getDayOfWeek();
            LocalTime segStart = day.equals(start.toLocalDate()) ? start.toLocalTime() : LocalTime.MIN;
            LocalTime segEnd = day.equals(end.toLocalDate()) ? end.toLocalTime() : LocalTime.of(23, 59, 59, 999_999_999);
            if (!segmentCovered(buildingId, dow, segStart, segEnd)) {
                return false;
            }
            day = day.plusDays(1);
        }
        return true;
    }

    private boolean segmentCovered(Long buildingId, DayOfWeek dow, LocalTime segStart, LocalTime segEnd) {
        List<BuildingTimetable> rows = buildingTimetableRepository.findByBuilding_IdAndDayOfWeekOrderByStartTimeAsc(buildingId, dow);
        // No rows for this building/day → do not restrict (tests, legacy DBs without seed)
        if (rows.isEmpty()) {
            return true;
        }
        return rows.stream().anyMatch(r ->
                !r.getStartTime().isAfter(segStart) && !r.getEndTime().isBefore(segEnd));
    }

    /**
     * Checks whether a single time point is within configured opening windows
     * for that day. If no timetable exists for the day, returns true.
     */
    @Transactional(readOnly = true)
    public boolean isTimeWithinBuildingHours(Long buildingId, LocalDateTime dateTime) {
        if (buildingId == null || dateTime == null) return true;
        List<BuildingTimetable> rows = buildingTimetableRepository
                .findByBuilding_IdAndDayOfWeekOrderByStartTimeAsc(buildingId, dateTime.getDayOfWeek());
        if (rows.isEmpty()) return true;
        LocalTime t = dateTime.toLocalTime();
        return rows.stream().anyMatch(r -> !r.getStartTime().isAfter(t) && !r.getEndTime().isBefore(t));
    }
}
