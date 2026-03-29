package com.campus.event.repository;

import com.campus.event.domain.EventRegistration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface EventRegistrationRepository extends JpaRepository<EventRegistration, Long> {
    boolean existsByEvent_IdAndUser_Username(Long eventId, String username);
    List<EventRegistration> findByEvent_Id(Long eventId);
    List<EventRegistration> findByUser_Username(String username);

    @Query("select r from EventRegistration r join fetch r.event where r.user.username = ?1")
    List<EventRegistration> findByUserUsernameWithEvent(String username);
    @Query("select r.user.username from EventRegistration r where r.event.id = ?1")
    List<String> findUsernamesByEventId(Long eventId);
    void deleteByEvent_Id(Long eventId);
}
