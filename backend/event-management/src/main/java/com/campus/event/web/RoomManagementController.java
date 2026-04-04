package com.campus.event.web;

import com.campus.event.domain.*;
import com.campus.event.service.RoomManagementService;
import com.campus.event.service.ScheduleService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/room-management")
public class RoomManagementController {
    
    private final RoomManagementService roomManagementService;
    private final ScheduleService scheduleService;
    
    public RoomManagementController(RoomManagementService roomManagementService,
                                  ScheduleService scheduleService) {
        this.roomManagementService = roomManagementService;
        this.scheduleService = scheduleService;
    }
    
    // Building endpoints
    @GetMapping("/buildings")
    @Transactional(readOnly = true)
    public ResponseEntity<List<Map<String, Object>>> getAllBuildings() {
        List<Building> buildings = roomManagementService.getAllBuildings();
        List<Map<String, Object>> response = buildings.stream().map(building -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", building.getId());
            map.put("name", building.getName());
            map.put("code", building.getCode());
            map.put("description", building.getDescription());
            map.put("floorCount", building.getFloors().size());
            return map;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/buildings")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<?> createBuilding(@RequestBody Map<String, String> request) {
        try {
            Building building = roomManagementService.createBuilding(
                request.get("name"),
                request.get("code"),
                request.get("description")
            );
            return ResponseEntity.ok(Map.of("id", building.getId(), "message", "Building created successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    
    // Floor endpoints
    @GetMapping("/buildings/{buildingId}/floors")
    public ResponseEntity<List<Map<String, Object>>> getFloorsByBuilding(@PathVariable Long buildingId) {
        List<Floor> floors = roomManagementService.getFloorsByBuilding(buildingId);
        List<Map<String, Object>> response = floors.stream().map(floor -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", floor.getId());
            map.put("floorNumber", floor.getFloorNumber());
            map.put("name", floor.getName());
            map.put("roomCount", floor.getRooms().size());
            return map;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/buildings/{buildingId}/floors")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<?> createFloor(@PathVariable Long buildingId, @RequestBody Map<String, Object> request) {
        try {
            Floor floor = roomManagementService.createFloor(
                buildingId,
                (Integer) request.get("floorNumber"),
                (String) request.get("name")
            );
            return ResponseEntity.ok(Map.of("id", floor.getId(), "message", "Floor created successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    
    // Room endpoints
    @GetMapping("/floors/{floorId}/rooms")
    public ResponseEntity<List<Map<String, Object>>> getRoomsByFloor(@PathVariable Long floorId) {
        List<Room> rooms = roomManagementService.getRoomsByFloor(floorId);
        return ResponseEntity.ok(rooms.stream().map(this::roomToMap).collect(Collectors.toList()));
    }
    
    @GetMapping("/buildings/{buildingId}/rooms")
    public ResponseEntity<List<Map<String, Object>>> getRoomsByBuilding(@PathVariable Long buildingId) {
        List<Room> rooms = roomManagementService.getRoomsByBuilding(buildingId);
        return ResponseEntity.ok(rooms.stream().map(this::roomToMap).collect(Collectors.toList()));
    }
    
    @GetMapping("/rooms/{roomId}")
    public ResponseEntity<Map<String, Object>> getRoomDetails(@PathVariable Long roomId) {
        Room room = roomManagementService.getRoomById(roomId);
        if (room == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(roomToMap(room));
    }
    
    @PostMapping("/floors/{floorId}/rooms")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<?> createRoom(@PathVariable Long floorId, @RequestBody Map<String, Object> request) {
        try {
            Room room = roomManagementService.createRoom(
                floorId,
                (String) request.get("roomNumber"),
                (String) request.get("name"),
                RoomType.valueOf((String) request.get("type")),
                (Integer) request.get("capacity"),
                (String) request.get("amenities")
            );
            return ResponseEntity.ok(Map.of("id", room.getId(), "message", "Room created successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    
    // Availability check
    @GetMapping("/rooms/{roomId}/availability")
    public ResponseEntity<?> getRoomAvailability(@PathVariable Long roomId, @RequestParam String date) {
        try {
            LocalDate localDate = LocalDate.parse(date);
            List<String> availableSlots = scheduleService.getAvailableSlots(roomId, localDate);
            return ResponseEntity.ok(Map.of("availableSlots", availableSlots));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Invalid date format");
        }
    }
    
    // Initialize default data
    @PostMapping("/initialize")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<?> initializeDefaultData() {
        try {
            roomManagementService.initializeBuildings();
            return ResponseEntity.ok(Map.of("message", "Building structures initialized successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to initialize: " + e.getMessage());
        }
    }
    
    private Map<String, Object> roomToMap(Room room) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", room.getId());
        map.put("roomNumber", room.getRoomNumber());
        map.put("name", room.getName());
        map.put("type", room.getType().name());
        map.put("capacity", room.getCapacity());
        map.put("amenities", room.getAmenities());
        map.put("isActive", room.isActive());
        
        if (room.getFloor() != null) {
            map.put("floorId", room.getFloor().getId());
            map.put("floorName", room.getFloor().getName());
            map.put("floorNumber", room.getFloor().getFloorNumber());
        }
        
        if (room.getFloor() != null && room.getFloor().getBuilding() != null) {
            map.put("buildingId", room.getFloor().getBuilding().getId());
            map.put("buildingName", room.getFloor().getBuilding().getName());
            map.put("buildingCode", room.getFloor().getBuilding().getCode());
        }
        
        return map;
    }
}
