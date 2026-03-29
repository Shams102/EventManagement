package com.campus.event.repository;

import com.campus.event.domain.Registration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RegistrationRepository extends JpaRepository<Registration, Long> {
    boolean existsByEventIdAndEmail(Long eventId, String email);
    List<Registration> findByEmail(String email);
    void deleteByEvent_Id(Long eventId);
}
