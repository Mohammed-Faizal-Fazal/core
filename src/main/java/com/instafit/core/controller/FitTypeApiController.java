package com.instafit.core.controller;

import com.instafit.core.entity.FitType;
import com.instafit.core.service.FitTypeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/master/fit-types")

public class FitTypeApiController {

    private static final Logger logger = LoggerFactory.getLogger(FitTypeApiController.class);

    @Autowired
    private FitTypeService fitTypeService;

    @GetMapping
    public ResponseEntity<List<FitType>> getAllFitTypes() {
        return ResponseEntity.ok(fitTypeService.getAllFitTypes());
    }

    @GetMapping("/active")
    public ResponseEntity<List<FitType>> getActiveFitTypes() {
        return ResponseEntity.ok(fitTypeService.getActiveFitTypes());
    }

    @PostMapping
    @PreAuthorize("hasRole('OPERATION')")
    public ResponseEntity<Map<String, Object>> createFitType(
            @Valid @RequestBody FitType fitType,
            Authentication authentication) {

        Map<String, Object> response = new HashMap<>();
        try {
            FitType created = fitTypeService.createFitType(fitType, authentication.getName());
            response.put("success", true);
            response.put("message", "Fit type created successfully");
            response.put("data", created);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error creating fit type", e);
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('OPERATION')")
    public ResponseEntity<Map<String, Object>> updateFitType(
            @PathVariable Long id,
            @Valid @RequestBody FitType fitType,
            Authentication authentication) {

        Map<String, Object> response = new HashMap<>();
        try {
            FitType updated = fitTypeService.updateFitType(id, fitType, authentication.getName());
            response.put("success", true);
            response.put("message", "Fit type updated successfully");
            response.put("data", updated);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error updating fit type", e);
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
            FitType updated = fitTypeService.toggleActive(id, authentication.getName());
            response.put("success", true);
            response.put("message", "Status updated successfully");
            response.put("data", updated);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error toggling active status", e);
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('OPERATION')")
    public ResponseEntity<Map<String, Object>> deleteFitType(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            fitTypeService.deleteFitType(id);
            response.put("success", true);
            response.put("message", "Fit type deleted successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error deleting fit type", e);
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/upload")
    @PreAuthorize("hasRole('OPERATION')")
    public ResponseEntity<Map<String, Object>> uploadExcel(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {

        Map<String, Object> response = new HashMap<>();
        try {
            List<FitType> fitTypes = fitTypeService.uploadFromExcel(file, authentication.getName());
            response.put("success", true);
            response.put("message", "Successfully uploaded " + fitTypes.size() + " fit types");
            response.put("data", fitTypes);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error uploading Excel", e);
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}