package com.instafit.core.controller;

import com.instafit.core.entity.Item;
import com.instafit.core.service.ItemService;
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
 * Item API Controller
 */
@RestController
@RequestMapping("/api/master/items")
@PreAuthorize("hasRole('OPERATION')")
public class ItemApiController {

    private static final Logger logger = LoggerFactory.getLogger(ItemApiController.class);

    @Autowired
    private ItemService itemService;

    @GetMapping
    public ResponseEntity<List<Item>> getAllItems() {
        return ResponseEntity.ok(itemService.getAllItems());
    }

    @GetMapping("/active")
    public ResponseEntity<List<Item>> getActiveItems() {
        return ResponseEntity.ok(itemService.getActiveItems());
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createItem(
            @Valid @RequestBody Item item,
            Authentication authentication) {

        Map<String, Object> response = new HashMap<>();
        try {
            Item created = itemService.createItem(item, authentication.getName());
            response.put("success", true);
            response.put("message", "Item created successfully");
            response.put("data", created);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error creating item", e);
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateItem(
            @PathVariable Long id,
            @Valid @RequestBody Item item,
            Authentication authentication) {

        Map<String, Object> response = new HashMap<>();
        try {
            Item updated = itemService.updateItem(id, item, authentication.getName());
            response.put("success", true);
            response.put("message", "Item updated successfully");
            response.put("data", updated);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error updating item", e);
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
            Item updated = itemService.toggleActive(id, authentication.getName());
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
    public ResponseEntity<Map<String, Object>> deleteItem(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            itemService.deleteItem(id);
            response.put("success", true);
            response.put("message", "Item deleted successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error deleting item", e);
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
            List<Item> items = itemService.uploadFromExcel(file, authentication.getName());
            response.put("success", true);
            response.put("message", "Successfully uploaded " + items.size() + " items");
            response.put("data", items);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error uploading Excel", e);
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}