package com.campus.event.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "bookings", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"room_id", "start_time", "end_time"})
})
public class Booking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
        public Long getId() { return id; }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id")
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @NotNull
    @Column(name = "start_time")
    private LocalDateTime startTime;

    @NotNull
    @Column(name = "end_time")
    private LocalDateTime endTime;

    private String purpose;
        // Explicit getters and setters
        public Room getRoom() { return room; }
        public void setRoom(Room room) { this.room = room; }
        public User getUser() { return user; }
        public void setUser(User user) { this.user = user; }
        public LocalDateTime getStartTime() { return startTime; }
        public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
        public LocalDateTime getEndTime() { return endTime; }
        public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
        public String getPurpose() { return purpose; }
        public void setPurpose(String purpose) { this.purpose = purpose; }
}


