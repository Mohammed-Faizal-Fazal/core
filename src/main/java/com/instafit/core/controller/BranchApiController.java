package com.instafit.core.controller;

import com.instafit.core.entity.Branch;
import com.instafit.core.service.BranchService;
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
@RequestMapping("/api/master/branches")
public class BranchApiController {

    private static final Logger logger = LoggerFactory.getLogger(BranchApiController.class);

    @Autowired
    private BranchService branchService;

    // PUBLIC ENDPOINT - No authentication required
    @GetMapping("/active")
    public ResponseEntity<List<Branch>> getActiveBranches() {
        logger.info("Fetching active branches (public endpoint)");
        return ResponseEntity.ok(branchService.getActiveBranches());
    }

    // PROTECTED ENDPOINTS - Require OPERATION role
    @GetMapping
    @PreAuthorize("hasRole('OPERATION')")
    public ResponseEntity<List<Branch>> getAllBranches() {
        return ResponseEntity.ok(branchService.getAllBranches());
    }

    @PostMapping
    @PreAuthorize("hasRole('OPERATION')")
    public ResponseEntity<Map<String, Object>> createBranch(
            @Valid @RequestBody Branch branch,
            Authentication authentication) {

        Map<String, Object> response = new HashMap<>();
        try {
            Branch created = branchService.createBranch(branch, authentication.getName());
            response.put("success", true);
            response.put("message", "Branch created successfully");
            response.put("data", created);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error creating branch", e);
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('OPERATION')")
    public ResponseEntity<Map<String, Object>> updateBranch(
            @PathVariable Long id,
            @Valid @RequestBody Branch branch,
            Authentication authentication) {

        Map<String, Object> response = new HashMap<>();
        try {
            Branch updated = branchService.updateBranch(id, branch, authentication.getName());
            response.put("success", true);
            response.put("message", "Branch updated successfully");
            response.put("data", updated);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error updating branch", e);
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
            Branch updated = branchService.toggleActive(id, authentication.getName());
            response.put("success", true);
            response.put("message", "Branch status updated successfully");
            response.put("data", updated);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error toggling branch status", e);
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('OPERATION')")
    public ResponseEntity<Map<String, Object>> deleteBranch(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            branchService.deleteBranch(id);
            response.put("success", true);
            response.put("message", "Branch deleted successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error deleting branch", e);
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}