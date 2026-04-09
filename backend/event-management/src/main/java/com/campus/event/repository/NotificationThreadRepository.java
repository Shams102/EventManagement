package com.campus.event.repository;

import com.campus.event.domain.NotificationThread;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationThreadRepository extends JpaRepository<NotificationThread, Long> {
    List<NotificationThread> findByEvent_IdOrderByCreatedAtDesc(Long eventId);

    Optional<NotificationThread> findByNotification_Id(Long notificationId);

    void deleteByEvent_Id(Long eventId);

    void deleteByNotification_Event_Id(Long eventId);
}
