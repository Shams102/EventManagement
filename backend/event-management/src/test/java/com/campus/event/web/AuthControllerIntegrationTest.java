package com.campus.event.web;

import com.campus.event.domain.Role;
import com.campus.event.domain.User;
import com.campus.event.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void register_newUser_succeeds() throws Exception {
        String body = """
                {"username":"newuser","password":"Pass@123","email":"new@example.com"}
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Registered")));

        assert userRepository.findByUsername("newuser").isPresent();
    }

    @Test
    void register_withRoleRequest_succeeds() throws Exception {
        String body = """
                {"username":"newfaculty","password":"Pass@123","email":"fac@example.com","role":"FACULTY"}
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Awaiting admin approval")));

        User saved = userRepository.findByUsername("newfaculty").orElseThrow();
        assert saved.getRequestedRole() == Role.FACULTY;
        assert saved.getRoles().contains(Role.GENERAL_USER);
    }

    @Test
    void register_duplicateUsername_fails() throws Exception {
        // Create user first
        User existing = new User();
        existing.setUsername("existinguser");
        existing.setPasswordHash(passwordEncoder.encode("Test@123"));
        existing.setEmail("existing@example.com");
        existing.setRoles(Set.of(Role.GENERAL_USER));
        userRepository.save(existing);

        String body = """
                {"username":"existinguser","password":"Pass@123","email":"other@example.com"}
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Username already exists")));
    }

    @Test
    void register_duplicateEmail_fails() throws Exception {
        User existing = new User();
        existing.setUsername("user1");
        existing.setPasswordHash(passwordEncoder.encode("Test@123"));
        existing.setEmail("taken@example.com");
        existing.setRoles(Set.of(Role.GENERAL_USER));
        userRepository.save(existing);

        String body = """
                {"username":"user2","password":"Pass@123","email":"taken@example.com"}
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Email already registered")));
    }

    @Test
    void login_validCredentials_returnsToken() throws Exception {
        User user = new User();
        user.setUsername("loginuser");
        user.setPasswordHash(passwordEncoder.encode("Pass@123"));
        user.setEmail("login@example.com");
        user.setRoles(Set.of(Role.GENERAL_USER));
        userRepository.save(user);

        String body = """
                {"username":"loginuser","password":"Pass@123"}
                """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void login_wrongPassword_returns401() throws Exception {
        User user = new User();
        user.setUsername("wrongpw");
        user.setPasswordHash(passwordEncoder.encode("Correct@123"));
        user.setEmail("wrongpw@example.com");
        user.setRoles(Set.of(Role.GENERAL_USER));
        userRepository.save(user);

        String body = """
                {"username":"wrongpw","password":"Wrong@456"}
                """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid password"));
    }

    @Test
    void login_unknownUser_returns404() throws Exception {
        String body = """
                {"username":"noone","password":"Pass@123"}
                """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("User not found"));
    }
}
