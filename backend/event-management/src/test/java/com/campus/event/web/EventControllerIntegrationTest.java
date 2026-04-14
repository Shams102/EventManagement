package com.campus.event.web;

import com.campus.event.domain.Building;
import com.campus.event.domain.Role;
import com.campus.event.domain.User;
import com.campus.event.repository.BuildingRepository;
import com.campus.event.repository.EventRegistrationRepository;
import com.campus.event.repository.EventRepository;
import com.campus.event.repository.RegistrationRepository;
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
    private EventRegistrationRepository eventRegistrationRepository;

    @Autowired
    private RegistrationRepository registrationRepository;

    @Autowired
    private BuildingRepository buildingRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenService jwtTokenService;

    private String adminToken;
    private String userToken;
    private Long testBuildingId;
    private String testBuildingName;

    @BeforeEach
    void setUp() {
        eventRegistrationRepository.deleteAll();
        registrationRepository.deleteAll();
        eventRepository.deleteAll();
        userRepository.deleteAll();

        // Reuse seeded campus data: deleting buildings breaks FK chains (rooms → fixed_timetable).
        Building building = buildingRepository.findByCode("AB1")
                .or(() -> buildingRepository.findByIsActiveTrue().stream().findFirst())
                .orElseThrow(() -> new IllegalStateException(
                        "No building found; RoomManagementService seed data should create AB1"));
        testBuildingId = building.getId();
        testBuildingName = building.getName();

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

    /** Helper: build a valid event creation JSON body including the mandatory buildingId. */
    private String eventJson(String title, String description, String location) {
        return """
                {
                    "title": "%s",
                    "description": "%s",
                    "start": "%s",
                    "end": "%s",
                    "buildingId": %d,
                    "location": "%s"
                }
                """.formatted(
                title,
                description,
                java.time.LocalDateTime.of(2026, 4, 24, 10, 0).toString(),
                java.time.LocalDateTime.of(2026, 4, 24, 12, 0).toString(),
                testBuildingId,
                location
        );
    }

    @Test
    void createEvent_asAdmin_succeeds() throws Exception {
        String body = eventJson("Integration Test Event", "Test description", "Room 101");

        mockMvc.perform(post("/api/events")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        assert eventRepository.count() == 1;
    }

    @Test
    void createEvent_withoutBuilding_returnsBadRequest() throws Exception {
        // Missing buildingId should trigger @NotNull validation and return 400
        String body = """
                {
                    "title": "No Building Event",
                    "description": "Should fail validation",
                    "start": "%s",
                    "end": "%s",
                    "location": "Room 101"
                }
                """.formatted(
                java.time.LocalDateTime.of(2026, 4, 24, 10, 0).toString(),
                java.time.LocalDateTime.of(2026, 4, 24, 12, 0).toString()
        );

        mockMvc.perform(post("/api/events")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createEvent_asGeneralUser_forbidden() throws Exception {
        String body = eventJson("Unauthorized Event", "Should fail", "Room 101");

        mockMvc.perform(post("/api/events")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void createEvent_noAuth_returnsUnauthorized() throws Exception {
        String body = eventJson("No Auth", "Fail", "Room 101");

        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getPublicEvents_succeeds() throws Exception {
        mockMvc.perform(get("/api/events/mine")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void myEvents_returnsOwnEvents() throws Exception {
        // First create an event
        String body = eventJson("My Event", "Testing mine endpoint", "Lab");

        mockMvc.perform(post("/api/events")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        // Now fetch them — verify building info is included in response
        mockMvc.perform(get("/api/events/mine")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("My Event"))
                .andExpect(jsonPath("$[0].buildingId").value(testBuildingId.intValue()))
                .andExpect(jsonPath("$[0].buildingName").value(testBuildingName));
    }
}
