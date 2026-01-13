package com.instafit.core.controller;

import com.instafit.core.entity.Pincode;
import com.instafit.core.service.PincodeService;
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

/**
 * Pincode API Controller
 */
@RestController
@RequestMapping("/api/master/pincodes")
@PreAuthorize("hasRole('OPERATION')")
public class PincodeApiController {

    private static final Logger logger = LoggerFactory.getLogger(PincodeApiController.class);

    @Autowired
    private PincodeService pincodeService;

    @GetMapping
    public ResponseEntity<List<Pincode>> getAllPincodes() {
        return ResponseEntity.ok(pincodeService.getAllPincodes());
    }

    @GetMapping("/active")
    public ResponseEntity<List<Pincode>> getActivePincodes() {
        return ResponseEntity.ok(pincodeService.getActivePincodes());
    }

    @GetMapping("/city/{cityCode}")
    public ResponseEntity<List<Pincode>> getPincodesByCity(@PathVariable String cityCode) {
        return ResponseEntity.ok(pincodeService.getPincodesByCity(cityCode));
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createPincode(
            @Valid @RequestBody Pincode pincode,
            Authentication authentication) {

        Map<String, Object> response = new HashMap<>();
        try {
            Pincode created = pincodeService.createPincode(pincode, authentication.getName());
            response.put("success", true);
            response.put("message", "Pincode created successfully");
            response.put("data", created);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error creating pincode", e);
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updatePincode(
            @PathVariable Long id,
            @Valid @RequestBody Pincode pincode,
            Authentication authentication) {

        Map<String, Object> response = new HashMap<>();
        try {
            Pincode updated = pincodeService.updatePincode(id, pincode, authentication.getName());
            response.put("success", true);
            response.put("message", "Pincode updated successfully");
            response.put("data", updated);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error updating pincode", e);
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
            Pincode updated = pincodeService.toggleActive(id, authentication.getName());
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
    public ResponseEntity<Map<String, Object>> deletePincode(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            pincodeService.deletePincode(id);
            response.put("success", true);
            response.put("message", "Pincode deleted successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error deleting pincode", e);
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadExcel(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {

        Map<String, Object> response = new HashMap<>();
        try {
            List<Pincode> pincodes = pincodeService.uploadFromExcel(file, authentication.getName());
            response.put("success", true);
            response.put("message", "Successfully uploaded " + pincodes.size() + " pincodes");
            response.put("data", pincodes);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error uploading Excel", e);
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}