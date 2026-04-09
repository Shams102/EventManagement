package com.campus.event.repository;

import com.campus.event.domain.FixedTimetable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface FixedTimetableRepository extends JpaRepository<FixedTimetable, Long> {
    List<FixedTimetable> findByRoomIdOrderByDayOfWeekAscStartTimeAsc(Long roomId);
    
    List<FixedTimetable> findByRoomIdAndDayOfWeekOrderByStartTimeAsc(Long roomId, DayOfWeek dayOfWeek);
    
    List<FixedTimetable> findByFacultyIdOrderByDayOfWeekAscStartTimeAsc(Long facultyId);
    
    List<FixedTimetable> findByAcademicYearAndIsActiveTrueOrderByDayOfWeekAscStartTimeAsc(String academicYear);
    
    @Query("SELECT ft FROM FixedTimetable ft WHERE ft.room.id = :roomId AND " +
           "ft.dayOfWeek = :dayOfWeek AND ft.isActive = true AND " +
           "ft.startTime < :endTime AND ft.endTime > :startTime")
    List<FixedTimetable> findConflictingClasses(@Param("roomId") Long roomId,
                                               @Param("dayOfWeek") DayOfWeek dayOfWeek,
                                               @Param("startTime") LocalTime startTime,
                                               @Param("endTime") LocalTime endTime);

    @Query("SELECT (COUNT(ft) > 0) FROM FixedTimetable ft WHERE ft.room.id = :roomId AND " +
           "ft.dayOfWeek = :dayOfWeek AND ft.isActive = true AND " +
           "ft.startTime < :endTime AND ft.endTime > :startTime")
    boolean existsConflictingClass(@Param("roomId") Long roomId,
                                   @Param("dayOfWeek") DayOfWeek dayOfWeek,
                                   @Param("startTime") LocalTime startTime,
                                   @Param("endTime") LocalTime endTime);
    
    @Query("SELECT ft FROM FixedTimetable ft WHERE ft.room.id = :roomId AND " +
           "ft.dayOfWeek = :dayOfWeek AND ft.isActive = true ORDER BY ft.startTime ASC")
    List<FixedTimetable> getRoomDaySchedule(@Param("roomId") Long roomId, 
                                           @Param("dayOfWeek") DayOfWeek dayOfWeek);
    
    boolean existsByRoomIdAndDayOfWeekAndStartTimeBeforeAndEndTimeAfterAndIsActiveTrue(
        Long roomId, DayOfWeek dayOfWeek, LocalTime endTime, LocalTime startTime);
    
    @Query("SELECT ft FROM FixedTimetable ft WHERE ft.isActive = true ORDER BY ft.dayOfWeek ASC, ft.startTime ASC")
    List<FixedTimetable> findAllActiveOrderByDayAndTime();
}
