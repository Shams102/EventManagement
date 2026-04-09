package com.campus.event.repository;

import com.campus.event.domain.NotificationDelivery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface NotificationDeliveryRepository extends JpaRepository<NotificationDelivery, Long> {
    List<NotificationDelivery> findByUser_UsernameOrderByCreatedAtDesc(String username);
    List<NotificationDelivery> findByNotification_Event_IdAndUser_UsernameOrderByCreatedAtDesc(Long eventId, String username);

    @Query("select d.notification.id from NotificationDelivery d where d.user.id = ?1 and d.notification.event.id = ?2")
    List<Long> findDeliveredNotificationIdsForUserEvent(Long userId, Long eventId);

    @Query("select d from NotificationDelivery d join fetch d.notification n left join fetch n.event where d.user.username = ?1 order by d.createdAt desc")
    List<NotificationDelivery> findInboxByUsernameWithNotification(String username);

    void deleteByNotification_Event_Id(Long eventId);
}
