package com.campus.event.repository;

import com.campus.event.domain.Booking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByRoomIdAndStartTimeLessThanEqualAndEndTimeGreaterThanEqual(Long roomId, LocalDateTime end, LocalDateTime start);
    List<Booking> findByStartTimeAndEndTime(LocalDateTime startTime, LocalDateTime endTime);
}


