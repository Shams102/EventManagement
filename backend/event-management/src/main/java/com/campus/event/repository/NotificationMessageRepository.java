package com.campus.event.repository;

import com.campus.event.domain.NotificationMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationMessageRepository extends JpaRepository<NotificationMessage, Long> {
    List<NotificationMessage> findByOriginOrderByCreatedAtDesc(com.campus.event.domain.NotificationOrigin origin);

    List<NotificationMessage> findByEvent_IdOrderByCreatedAtDesc(Long eventId);

    void deleteByEvent_Id(Long eventId);
}
