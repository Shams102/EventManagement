package com.campus.event.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "registrations", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"event_id", "email"})
})
public class Registration {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id")
    private Event event;

    @Email
    @NotBlank
    private String email;

    @NotBlank
    private String fullName;

    @Column(columnDefinition = "TEXT")
    private String answersJson;
        // Explicit getters and setters
        public Event getEvent() { return event; }
        public void setEvent(Event event) { this.event = event; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }
        public String getAnswersJson() { return answersJson; }
        public void setAnswersJson(String answersJson) { this.answersJson = answersJson; }
}


