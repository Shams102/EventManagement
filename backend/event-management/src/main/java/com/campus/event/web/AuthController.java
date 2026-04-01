package com.campus.event.web;

import com.campus.event.domain.Role;
import com.campus.event.domain.User;
import com.campus.event.repository.UserRepository;
import com.campus.event.security.JwtTokenService;
import com.campus.event.web.dto.LoginRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final UserRepository userRepository;
    private final JwtTokenService jwtTokenService;
    private final com.campus.event.service.CustomUserDetailsService userDetailsService;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    public AuthController(UserRepository userRepository, JwtTokenService jwtTokenService, com.campus.event.service.CustomUserDetailsService userDetailsService, org.springframework.security.crypto.password.PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.jwtTokenService = jwtTokenService;
        this.userDetailsService = userDetailsService;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody LoginRequest request) {
        String uname = request.getUsername();
        return userRepository.findByUsername(uname)
                .map(user -> {
                    boolean ok = passwordEncoder.matches(request.getPassword(), user.getPasswordHash());
                    log.debug("Login attempt user='{}' match={}", user.getUsername(), ok);
                    if (ok) {
                        org.springframework.security.core.userdetails.User userDetails =
                                (org.springframework.security.core.userdetails.User) userDetailsService.loadUserByUsername(user.getUsername());
                        List<String> roles = userDetails.getAuthorities().stream()
                                .map(org.springframework.security.core.GrantedAuthority::getAuthority)
                                .toList();
                        Map<String, Object> claims = new HashMap<>();
                        claims.put("roles", roles);
                        if (user.getClubId() != null) {
                            claims.put("clubId", user.getClubId());
                        }
                        String token = jwtTokenService.generateToken(claims, userDetails);
                        return ResponseEntity.ok(Map.<String, Object>of("token", token));
                    } else {
                        return ResponseEntity.status(401).body(Map.<String, Object>of("error", "Invalid password"));
                    }
                })
                .orElseGet(() -> {
                    log.debug("Login attempt user='{}' not found", uname);
                    return ResponseEntity.status(404).body(Map.<String, Object>of("error", "User not found"));
                });
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody com.campus.event.web.dto.RegisterRequest request) {
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Username already exists"));
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email already registered"));
        }
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        Role requested = null;
        if (request.getRole() != null) {
            try {
                requested = Role.valueOf(request.getRole().toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }
        // Always grant GENERAL_USER by default using a mutable set
        user.getRoles().add(Role.GENERAL_USER);
        // If a higher role was requested, store it for admin approval
        if (requested != null && requested != Role.GENERAL_USER) {
            user.setRequestedRole(requested);
        }
        userRepository.save(user);
        if (user.getRequestedRole() != null) {
            return ResponseEntity.ok(Map.of("message", "Registered. Awaiting admin approval for role: " + user.getRequestedRole()));
        }
        return ResponseEntity.ok(Map.of("message", "Registered"));
    }
}
