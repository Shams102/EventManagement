package com.campus.event.web;

import com.campus.event.domain.Role;
import com.campus.event.domain.User;
import com.campus.event.repository.UserRepository;
import com.campus.event.security.JwtTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.*;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenService jwtTokenService;

    private String adminToken;
    private String userToken;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        // Create admin
        User admin = new User();
        admin.setUsername("admin");
        admin.setPasswordHash(passwordEncoder.encode("Admin@123"));
        admin.setEmail("admin@test.com");
        admin.setRoles(Set.of(Role.ADMIN));
        userRepository.save(admin);

        // Create general user
        User user = new User();
        user.setUsername("generaluser");
        user.setPasswordHash(passwordEncoder.encode("User@123"));
        user.setEmail("user@test.com");
        user.setRoles(Set.of(Role.GENERAL_USER));
        userRepository.save(user);

        adminToken = generateToken("admin", List.of("ROLE_ADMIN"));
        userToken = generateToken("generaluser", List.of("ROLE_GENERAL_USER"));
    }

    private String generateToken(String username, List<String> roles) {
        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username(username)
                .password("dummy")
                .authorities(roles.stream()
                        .map(r -> (GrantedAuthority) () -> r)
                        .toList())
                .build();
        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", roles);
        return jwtTokenService.generateToken(claims, userDetails);
    }

    @Test
    void listRoleRequests_asAdmin_succeeds() throws Exception {
        // Create a user with a pending role request
        User pending = new User();
        pending.setUsername("pendinguser");
        pending.setPasswordHash(passwordEncoder.encode("Test@123"));
        pending.setEmail("pending@test.com");
        pending.setRoles(Set.of(Role.GENERAL_USER));
        pending.setRequestedRole(Role.FACULTY);
        userRepository.save(pending);

        mockMvc.perform(get("/api/admin/role-requests")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].username").value("pendinguser"))
                .andExpect(jsonPath("$[0].requestedRole").value("FACULTY"));
    }

    @Test
    void approve_setsRoleAndClearsRequest() throws Exception {
        User pending = new User();
        pending.setUsername("toapprove");
        pending.setPasswordHash(passwordEncoder.encode("Test@123"));
        pending.setEmail("approve@test.com");
        pending.setRoles(new HashSet<>(Set.of(Role.GENERAL_USER)));
        pending.setRequestedRole(Role.FACULTY);
        pending = userRepository.save(pending);

        mockMvc.perform(post("/api/admin/role-requests/" + pending.getId() + "/approve")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Approved")));

        User updated = userRepository.findById(pending.getId()).orElseThrow();
        assertTrue(updated.getRoles().contains(Role.FACULTY));
        assertNull(updated.getRequestedRole());
    }

    @Test
    void reject_clearsRequestedRole() throws Exception {
        User pending = new User();
        pending.setUsername("toreject");
        pending.setPasswordHash(passwordEncoder.encode("Test@123"));
        pending.setEmail("reject@test.com");
        pending.setRoles(new HashSet<>(Set.of(Role.GENERAL_USER)));
        pending.setRequestedRole(Role.CLUB_ASSOCIATE);
        pending = userRepository.save(pending);

        mockMvc.perform(post("/api/admin/role-requests/" + pending.getId() + "/reject")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Rejected")));

        User updated = userRepository.findById(pending.getId()).orElseThrow();
        assertFalse(updated.getRoles().contains(Role.CLUB_ASSOCIATE));
        assertNull(updated.getRequestedRole());
    }

    @Test
    void nonAdmin_denied() throws Exception {
        mockMvc.perform(get("/api/admin/role-requests")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void approve_noRequest_returnsBadRequest() throws Exception {
        // generaluser has no pending role request
        User user = userRepository.findByUsername("generaluser").orElseThrow();

        mockMvc.perform(post("/api/admin/role-requests/" + user.getId() + "/approve")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("No pending role request")));
    }

    @Test
    void setUserClub_asAdmin_succeeds() throws Exception {
        User user = userRepository.findByUsername("generaluser").orElseThrow();
        String body = """
                {"clubId":"TECH_CLUB"}
                """;

        mockMvc.perform(post("/api/admin/users/" + user.getId() + "/club")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Updated clubId")));

        User updated = userRepository.findById(user.getId()).orElseThrow();
        assertEquals("TECH_CLUB", updated.getClubId());
    }
}
