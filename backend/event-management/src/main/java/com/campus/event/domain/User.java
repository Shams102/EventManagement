package com.campus.event.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String username;

    @NotBlank
    private String passwordHash;

    @Email
    @NotBlank
    private String email;

    private String phoneNumber;

    private String clubId;

    private String fullName;

    private Long managedBuildingId;

    /** Required for {@link Role#BUILDING_ADMIN}: which room category this admin approves. */
    @Enumerated(EnumType.STRING)
    private AdminScope adminScope;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    private Set<Role> roles = new HashSet<>();
    
    @Enumerated(EnumType.STRING)
    private Role requestedRole;

    @Column(name = "active_session_token", length = 512)
    private String activeSessionToken;
        // Explicit getters and setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPasswordHash() { return passwordHash; }
        public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPhoneNumber() { return phoneNumber; }
        public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
        public Set<Role> getRoles() { return roles; }
        public void setRoles(Set<Role> roles) { this.roles = roles; }
        public Role getRequestedRole() { return requestedRole; }
        public void setRequestedRole(Role requestedRole) { this.requestedRole = requestedRole; }
        public String getActiveSessionToken() { return activeSessionToken; }
        public void setActiveSessionToken(String activeSessionToken) { this.activeSessionToken = activeSessionToken; }
        public String getClubId() { return clubId; }
        public void setClubId(String clubId) { this.clubId = clubId; }
        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }
        public Long getManagedBuildingId() { return managedBuildingId; }
        public void setManagedBuildingId(Long managedBuildingId) { this.managedBuildingId = managedBuildingId; }
        public AdminScope getAdminScope() { return adminScope; }
        public void setAdminScope(AdminScope adminScope) { this.adminScope = adminScope; }
}


