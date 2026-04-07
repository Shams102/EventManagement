package com.campus.event.repository;

import com.campus.event.domain.Event;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long> {
    List<Event> findByIsPublicTrue();
    List<Event> findByCreatedBy_Username(String username);

    boolean existsByTitleAndStartTimeAndCreatedBy_Id(String title, java.time.LocalDateTime startTime, Long userId);

    @org.springframework.data.jpa.repository.Query("SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END FROM Event e WHERE e.createdBy.id = :userId AND e.startTime < :endTime AND e.endTime > :startTime")
    boolean hasOverlappingEvents(@org.springframework.data.repository.query.Param("userId") Long userId, 
                                 @org.springframework.data.repository.query.Param("startTime") java.time.LocalDateTime startTime, 
                                 @org.springframework.data.repository.query.Param("endTime") java.time.LocalDateTime endTime);
}


