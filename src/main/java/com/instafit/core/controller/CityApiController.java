package com.instafit.core.controller;

import com.instafit.core.entity.City;
import com.instafit.core.service.CityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/master/cities")
public class CityApiController {

    private static final Logger logger = LoggerFactory.getLogger(CityApiController.class);

    @Autowired
    private CityService cityService;

    // PUBLIC ENDPOINT - No authentication required
    @GetMapping("/active")
    public ResponseEntity<List<City>> getActiveCities() {
        logger.info("Fetching active cities (public endpoint)");
        return ResponseEntity.ok(cityService.getActiveCities());
    }

    // PROTECTED ENDPOINTS - Require OPERATION role
    @GetMapping
    @PreAuthorize("hasRole('OPERATION')")
    public ResponseEntity<List<City>> getAllCities() {
        return ResponseEntity.ok(cityService.getAllCities());
    }

    @PostMapping
    @PreAuthorize("hasRole('OPERATION')")
    public ResponseEntity<Map<String, Object>> createCity(
            @Valid @RequestBody City city,
            Authentication authentication) {

        Map<String, Object> response = new HashMap<>();
        try {
            City created = cityService.createCity(city, authentication.getName());
            response.put("success", true);
            response.put("message", "City created successfully");
            response.put("data", created);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error creating city", e);
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('OPERATION')")
    public ResponseEntity<Map<String, Object>> updateCity(
            @PathVariable Long id,
            @Valid @RequestBody City city,
            Authentication authentication) {

        Map<String, Object> response = new HashMap<>();
        try {
            City updated = cityService.updateCity(id, city, authentication.getName());
            response.put("success", true);
            response.put("message", "City updated successfully");
            response.put("data", updated);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error updating city", e);
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/{id}/toggle-active")
    @PreAuthorize("hasRole('OPERATION')")
    public ResponseEntity<Map<String, Object>> toggleActive(
            @PathVariable Long id,
            Authentication authentication) {

        Map<String, Object> response = new HashMap<>();
        try {
            City updated = cityService.toggleActive(id, authentication.getName());
            response.put("success", true);
            response.put("message", "City status updated successfully");
            response.put("data", updated);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error toggling city status", e);
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('OPERATION')")
    public ResponseEntity<Map<String, Object>> deleteCity(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            cityService.deleteCity(id);
            response.put("success", true);
            response.put("message", "City deleted successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error deleting city", e);
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}