package com.campus.event.repository;

import com.campus.event.domain.ThreadMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ThreadMessageRepository extends JpaRepository<ThreadMessage, Long> {
    List<ThreadMessage> findByThread_IdOrderByCreatedAtAsc(Long threadId);

    @Query("select m from ThreadMessage m join fetch m.author where m.thread.id = ?1 order by m.createdAt asc")
    List<ThreadMessage> findByThreadIdWithAuthor(Long threadId);

    void deleteByThread_Event_Id(Long eventId);

    void deleteByThread_Notification_Event_Id(Long eventId);
}
