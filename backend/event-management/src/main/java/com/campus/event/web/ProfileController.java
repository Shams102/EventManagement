package com.campus.event.web;

import com.campus.event.domain.User;
import com.campus.event.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public ProfileController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    public ResponseEntity<?> getProfile(Authentication auth) {
        if (auth == null || auth.getName() == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        User user = userRepository.findByUsername(auth.getName()).orElse(null);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        Map<String, Object> map = new HashMap<>();
        map.put("username", user.getUsername());
        map.put("email", user.getEmail());
        map.put("phoneNumber", user.getPhoneNumber());
        map.put("fullName", user.getFullName());
        map.put("clubId", user.getClubId());
        map.put("roles", user.getRoles());
        map.put("managedBuildingId", user.getManagedBuildingId());
        map.put("requestedRole", user.getRequestedRole());
        map.put("isApproved", user.getRequestedRole() == null);
        return ResponseEntity.ok(map);
    }

    @PutMapping
    public ResponseEntity<?> updateProfile(Authentication auth, @RequestBody Map<String, String> payload) {
        if (auth == null || auth.getName() == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        User user = userRepository.findByUsername(auth.getName()).orElse(null);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        
        if (payload.containsKey("email")) {
            user.setEmail(payload.get("email"));
        }
        if (payload.containsKey("phoneNumber")) {
            user.setPhoneNumber(payload.get("phoneNumber"));
        }
        if (payload.containsKey("fullName")) {
            user.setFullName(payload.get("fullName"));
        }
        if (payload.containsKey("clubId")) {
            user.setClubId(payload.get("clubId"));
        }
        
        userRepository.save(user);
        return ResponseEntity.ok("Profile updated successfully");
    }

    @PutMapping("/password")
    public ResponseEntity<?> updatePassword(Authentication auth, @RequestBody Map<String, String> payload) {
        if (auth == null || auth.getName() == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        User user = userRepository.findByUsername(auth.getName()).orElse(null);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        String currentPassword = payload.get("currentPassword");
        String newPassword = payload.get("newPassword");

        if (currentPassword == null || newPassword == null) {
            return ResponseEntity.badRequest().body("Passwords are required");
        }

        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            return ResponseEntity.badRequest().body("Incorrect current password");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        return ResponseEntity.ok("Password updated successfully");
    }
}
