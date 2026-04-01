package com.campus.event.web;

import com.campus.event.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserRepository userRepository;

    public AdminController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/role-requests")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> listRoleRequests() {
        List<Map<String, Object>> body = userRepository.findByRequestedRoleIsNotNull().stream()
                .map(u -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", u.getId());
                    m.put("username", u.getUsername());
                    m.put("email", u.getEmail());
                    m.put("requestedRole", String.valueOf(u.getRequestedRole()));
                    return m;
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(body);
    }

    @PostMapping("/role-requests/{userId}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> approve(@PathVariable Long userId) {
        return userRepository.findById(userId)
                .map(user -> {
                    if (user.getRequestedRole() == null) {
                        return ResponseEntity.badRequest().body("No pending role request for this user");
                    }
                    user.getRoles().add(user.getRequestedRole());
                    user.setRequestedRole(null);
                    userRepository.save(user);
                    return ResponseEntity.ok("Approved");
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/role-requests/{userId}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> reject(@PathVariable Long userId) {
        return userRepository.findById(userId)
                .map(user -> {
                    if (user.getRequestedRole() == null) {
                        return ResponseEntity.badRequest().body("No pending role request for this user");
                    }
                    user.setRequestedRole(null);
                    userRepository.save(user);
                    return ResponseEntity.ok("Rejected");
                })
                .orElse(ResponseEntity.notFound().build());
    }

    public static class SetClubBody { public String clubId; }

    @PostMapping("/users/{userId}/club")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> setUserClub(@PathVariable Long userId, @RequestBody SetClubBody body) {
        return userRepository.findById(userId)
                .map(user -> {
                    user.setClubId(body != null ? body.clubId : null);
                    userRepository.save(user);
                    return ResponseEntity.ok("Updated clubId");
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
