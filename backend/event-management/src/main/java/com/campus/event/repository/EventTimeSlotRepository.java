package com.campus.event.repository;

import com.campus.event.domain.EventTimeSlot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventTimeSlotRepository extends JpaRepository<EventTimeSlot, Long> {

    List<EventTimeSlot> findByEvent_IdOrderBySlotStartAsc(Long eventId);

    void deleteByEvent_Id(Long eventId);
}
