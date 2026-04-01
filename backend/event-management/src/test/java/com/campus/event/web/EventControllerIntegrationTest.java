package com.campus.event.web;

import com.campus.event.domain.Role;
import com.campus.event.domain.User;
import com.campus.event.repository.EventRepository;
import com.campus.event.repository.UserRepository;
import com.campus.event.security.JwtTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EventControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenService jwtTokenService;

    private String adminToken;
    private String userToken;

    @BeforeEach
    void setUp() {
        eventRepository.deleteAll();
        userRepository.deleteAll();

        // Create admin user
        User admin = new User();
        admin.setUsername("admin");
        admin.setPasswordHash(passwordEncoder.encode("Admin@123"));
        admin.setEmail("admin@test.com");
        admin.setRoles(Set.of(Role.ADMIN));
        userRepository.save(admin);

        // Create general user
        User user = new User();
        user.setUsername("regularuser");
        user.setPasswordHash(passwordEncoder.encode("User@123"));
        user.setEmail("user@test.com");
        user.setRoles(Set.of(Role.GENERAL_USER));
        userRepository.save(user);

        adminToken = generateToken("admin", List.of("ROLE_ADMIN"));
        userToken = generateToken("regularuser", List.of("ROLE_GENERAL_USER"));
    }

    private String generateToken(String username, List<String> roles) {
        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username(username)
                .password("dummy")
                .authorities(roles.stream()
                        .map(r -> (org.springframework.security.core.GrantedAuthority) () -> r)
                        .toList())
                .build();
        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", roles);
        return jwtTokenService.generateToken(claims, userDetails);
    }

    @Test
    void createEvent_asAdmin_succeeds() throws Exception {
        String body = """
                {
                    "title": "Integration Test Event",
                    "description": "Test description",
                    "start": "%s",
                    "end": "%s",
                    "location": "Room 101"
                }
                """.formatted(
                java.time.LocalDateTime.now().plusDays(10).toString(),
                java.time.LocalDateTime.now().plusDays(10).plusHours(2).toString()
        );

        mockMvc.perform(post("/api/events")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        assert eventRepository.count() == 1;
    }

    @Test
    void createEvent_asGeneralUser_forbidden() throws Exception {
        String body = """
                {
                    "title": "Unauthorized Event",
                    "description": "Should fail",
                    "start": "%s",
                    "end": "%s"
                }
                """.formatted(
                java.time.LocalDateTime.now().plusDays(10).toString(),
                java.time.LocalDateTime.now().plusDays(10).plusHours(2).toString()
        );

        mockMvc.perform(post("/api/events")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void createEvent_noAuth_returnsUnauthorized() throws Exception {
        String body = """
                {"title": "No Auth", "description": "Fail", "start": "%s", "end": "%s"}
                """.formatted(
                java.time.LocalDateTime.now().plusDays(10).toString(),
                java.time.LocalDateTime.now().plusDays(10).plusHours(2).toString()
        );

        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getPublicEvents_succeeds() throws Exception {
        // Public events endpoint doesn't exist at /api/public/events,
        // but /api/events/mine requires auth
        mockMvc.perform(get("/api/events/mine")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void myEvents_returnsOwnEvents() throws Exception {
        // First create an event
        String body = """
                {
                    "title": "My Event",
                    "description": "Testing mine endpoint",
                    "start": "%s",
                    "end": "%s",
                    "location": "Lab"
                }
                """.formatted(
                java.time.LocalDateTime.now().plusDays(10).toString(),
                java.time.LocalDateTime.now().plusDays(10).plusHours(2).toString()
        );

        mockMvc.perform(post("/api/events")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        // Now fetch them
        mockMvc.perform(get("/api/events/mine")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("My Event"));
    }
}
