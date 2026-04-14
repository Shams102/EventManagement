package com.campus.event.repository;

import com.campus.event.domain.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByRoomIdAndStartTimeLessThanEqualAndEndTimeGreaterThanEqual(Long roomId, LocalDateTime end, LocalDateTime start);
    List<Booking> findByStartTimeAndEndTime(LocalDateTime startTime, LocalDateTime endTime);

    @Modifying
    @Query("DELETE FROM Booking b WHERE b.purpose LIKE %:token%")
    void deleteByPurposeContaining(@Param("token") String token);
}


