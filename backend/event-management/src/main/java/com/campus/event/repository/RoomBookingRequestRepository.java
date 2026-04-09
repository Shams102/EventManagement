package com.campus.event.repository;

import com.campus.event.domain.RoomBookingRequest;
import com.campus.event.domain.RoomBookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Repository
public interface RoomBookingRequestRepository extends JpaRepository<RoomBookingRequest, Long> {
    List<RoomBookingRequest> findByRequestedByUsernameOrderByRequestedAtDesc(String username);
    
    List<RoomBookingRequest> findByStatusOrderByRequestedAtDesc(RoomBookingStatus status);
    
    List<RoomBookingRequest> findByStatusIn(Set<RoomBookingStatus> statuses);
    
    @Query("SELECT rbr FROM RoomBookingRequest rbr WHERE rbr.allocatedRoom.id = :roomId AND " +
           "(rbr.status = 'APPROVED' OR rbr.status = 'CONFIRMED') AND " +
           "((rbr.event IS NOT NULL AND CAST(rbr.event.startTime AS date) = :date) OR " +
           "(rbr.meetingStart IS NOT NULL AND CAST(rbr.meetingStart AS date) = :date))")
    List<RoomBookingRequest> findByAllocatedRoomIdAndDateBookings(@Param("roomId") Long roomId, @Param("date") LocalDate date);
    
    @Query("SELECT rbr FROM RoomBookingRequest rbr WHERE rbr.allocatedRoom.id = :roomId AND " +
           "(rbr.status = 'APPROVED' OR rbr.status = 'CONFIRMED') AND " +
           "((rbr.event IS NOT NULL AND rbr.event.startTime < :endTime AND rbr.event.endTime > :startTime) OR " +
           "(rbr.meetingStart IS NOT NULL AND rbr.meetingStart < :endTime AND rbr.meetingEnd > :startTime))")
    List<RoomBookingRequest> findConflictingBookings(@Param("roomId") Long roomId,
                                                     @Param("startTime") LocalDateTime startTime,
                                                     @Param("endTime") LocalDateTime endTime);
    
    @Query("select r from RoomBookingRequest r where r.status = 'PENDING' and r.requestedAt <= ?1")
    List<RoomBookingRequest> findPendingOlderThan(LocalDateTime cutoff);
    
    @Query("SELECT rbr FROM RoomBookingRequest rbr WHERE rbr.status = 'APPROVED' AND rbr.approvedAt <= :cutoff")
    List<RoomBookingRequest> findApprovedToConfirm(@Param("cutoff") LocalDateTime cutoff);

    List<RoomBookingRequest> findBySplitGroupId(UUID splitGroupId);

    boolean existsByEvent_IdAndStatus(Long eventId, RoomBookingStatus status);

    boolean existsByEvent_IdAndStatusIn(Long eventId, Set<RoomBookingStatus> statuses);

    void deleteByEvent_Id(Long eventId);
    
    @Modifying
    @Query("UPDATE RoomBookingRequest r SET r.status = 'REJECTED' WHERE r.splitGroupId = :groupId AND r.id != :approvedId AND r.status = 'PENDING'")
    void rejectSplitSiblingsBulk(@Param("groupId") UUID groupId, @Param("approvedId") Long approvedId);
    
    @Query("SELECT r FROM RoomBookingRequest r ORDER BY r.requestedAt DESC")
    org.springframework.data.domain.Page<RoomBookingRequest> findRecentBookings(org.springframework.data.domain.Pageable pageable);
}
