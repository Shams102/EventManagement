package com.campus.event.web;

import com.campus.event.domain.PasswordResetToken;
import com.campus.event.domain.Role;
import com.campus.event.domain.User;
import com.campus.event.repository.UserRepository;
import com.campus.event.security.JwtTokenService;
import com.campus.event.web.dto.LoginRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final UserRepository userRepository;
    private final JwtTokenService jwtTokenService;
    private final com.campus.event.service.CustomUserDetailsService userDetailsService;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    private final com.campus.event.repository.PasswordResetTokenRepository tokenRepository;

    public AuthController(UserRepository userRepository, JwtTokenService jwtTokenService, com.campus.event.service.CustomUserDetailsService userDetailsService, org.springframework.security.crypto.password.PasswordEncoder passwordEncoder, com.campus.event.repository.PasswordResetTokenRepository tokenRepository) {
        this.userRepository = userRepository;
        this.jwtTokenService = jwtTokenService;
        this.userDetailsService = userDetailsService;
        this.passwordEncoder = passwordEncoder;
        this.tokenRepository = tokenRepository;
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
                        user.setActiveSessionToken(token);
                        userRepository.save(user);
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

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@AuthenticationPrincipal UserDetails principal) {
        if (principal != null && principal.getUsername() != null) {
            userRepository.findByUsername(principal.getUsername()).ifPresent(u -> {
                u.setActiveSessionToken(null);
                userRepository.save(u);
            });
        }
        return ResponseEntity.ok(Map.of("message", "Logged out"));
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
        if (request.getRole() != null && !request.getRole().isBlank()) {
            String roleInput = request.getRole().trim().toUpperCase();
            if ("USER".equals(roleInput)) {
                roleInput = "GENERAL_USER";
            }
            try {
                requested = Role.valueOf(roleInput);
            } catch (IllegalArgumentException ignored) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid role. Allowed roles: USER, FACULTY, CLUB_ASSOCIATE"));
            }
            if (requested != Role.GENERAL_USER && requested != Role.FACULTY && requested != Role.CLUB_ASSOCIATE) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid role. Allowed roles: USER, FACULTY, CLUB_ASSOCIATE"));
            }
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

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        if (email == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email is required"));
        }
        
        userRepository.findByEmail(email).ifPresent(user -> {
            String token = UUID.randomUUID().toString();
            PasswordResetToken resetToken = new PasswordResetToken(token, user, LocalDateTime.now().plusMinutes(30));
            tokenRepository.save(resetToken);
            
            // In a real app, send email with Spring Mail.
            // For now, log the token to console.
            log.info("Password reset requested for {}. Token: {}", email, token);
            System.out.println("=================================================");
            System.out.println("PASSWORD RESET LINK: http://localhost:5173/reset-password?token=" + token);
            System.out.println("=================================================");
        });
        
        // Always return success to prevent email enumeration
        return ResponseEntity.ok(Map.of("message", "If your email is registered, you will receive a reset link shortly."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> request) {
        String token = request.get("token");
        String newPassword = request.get("newPassword");
        
        if (token == null || newPassword == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Token and new password are required"));
        }
        
        var resetTokenOpt = tokenRepository.findByToken(token);
        if (resetTokenOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid token"));
        }
        
        PasswordResetToken resetToken = resetTokenOpt.get();
        if (resetToken.isUsed() || resetToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Token has expired or already been used"));
        }
        
        User user = resetToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        
        resetToken.setUsed(true);
        tokenRepository.save(resetToken);
        
        return ResponseEntity.ok(Map.of("message", "Password successfully reset"));
    }
}
