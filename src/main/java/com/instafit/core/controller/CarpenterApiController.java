package com.instafit.core.controller;

import com.instafit.core.entity.Carpenter;
import com.instafit.core.entity.User;
import com.instafit.core.service.CarpenterService;
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
@RequestMapping("/api/carpenters")

public class CarpenterApiController {

    private static final Logger logger = LoggerFactory.getLogger(CarpenterApiController.class);

    @Autowired
    private CarpenterService carpenterService;

    @GetMapping
    public ResponseEntity<List<Carpenter>> getAllCarpenters() {
        return ResponseEntity.ok(carpenterService.getAllCarpenters());
    }

    @GetMapping("/active")
    public ResponseEntity<List<Carpenter>> getActiveCarpenters() {
        return ResponseEntity.ok(carpenterService.getActiveCarpenters());
    }

    @GetMapping("/city/{cityCode}")
    public ResponseEntity<List<Carpenter>> getCarpentersByCity(@PathVariable String cityCode) {
        return ResponseEntity.ok(carpenterService.getCarpentersByCity(cityCode));
    }

    @GetMapping("/branch/{branchCode}")
    public ResponseEntity<List<Carpenter>> getCarpentersByBranch(@PathVariable String branchCode) {
        return ResponseEntity.ok(carpenterService.getCarpentersByBranch(branchCode));
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createCarpenter(
            @Valid @RequestBody Carpenter carpenter,
            Authentication authentication) {

        Map<String, Object> response = new HashMap<>();
        try {
            Carpenter created = carpenterService.registerCarpenter(carpenter, authentication.getName());
            response.put("success", true);
            response.put("message", "Carpenter registered successfully");
            response.put("data", created);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error creating carpenter", e);
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateCarpenter(
            @PathVariable Long id,
            @Valid @RequestBody Carpenter carpenter,
            Authentication authentication) {

        Map<String, Object> response = new HashMap<>();
        try {
            Carpenter updated = carpenterService.updateCarpenter(id, carpenter, authentication.getName());
            response.put("success", true);
            response.put("message", "Carpenter updated successfully");
            response.put("data", updated);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error updating carpenter", e);
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/{id}/toggle-active")
    public ResponseEntity<Map<String, Object>> toggleActive(
            @PathVariable Long id,
            Authentication authentication) {

        Map<String, Object> response = new HashMap<>();
        try {
            Carpenter updated = carpenterService.toggleActive(id, authentication.getName());
            response.put("success", true);
            response.put("message", "Carpenter status updated successfully");
            response.put("data", updated);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error toggling carpenter status", e);
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteCarpenter(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            carpenterService.deleteCarpenter(id);
            response.put("success", true);
            response.put("message", "Carpenter deleted successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error deleting carpenter", e);
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/{carpenterId}")
    public ResponseEntity<Carpenter> getCarpenterById(@PathVariable String carpenterId) {
        Carpenter carpenter = carpenterService.getCarpenterById(carpenterId);
        return ResponseEntity.ok(carpenter);
    }

    @GetMapping("/current")

    public ResponseEntity<Carpenter> getCurrentCarpenter(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        Carpenter carpenter = carpenterService.getCarpenterByMobile(user.getPhoneNumber());
        return ResponseEntity.ok(carpenter);
    }


}