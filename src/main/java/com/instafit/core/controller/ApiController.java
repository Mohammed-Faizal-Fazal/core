package com.instafit.core.controller;

import com.instafit.core.entity.User;
import com.instafit.core.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;


@RestController
@RequestMapping("/api")
public class ApiController {

    @Autowired
    private UserService userService;


    @GetMapping("/user/info")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getUserInfo(Authentication authentication) {
        // Authentication.getName() returns phoneNumber (because User.getUsername() returns phoneNumber)
        String phoneNumber = authentication.getName();
        User user = userService.findByPhoneNumber(phoneNumber).orElse(null);

        Map<String, Object> response = new HashMap<>();
        if (user != null) {
            response.put("phoneNumber", user.getPhoneNumber());
            response.put("fullName", user.getFullName());
            response.put("email", user.getEmail());
            response.put("role", user.getRole().name());
            response.put("active", user.getActive());
            response.put("createdAt", user.getCreatedAt());
            return ResponseEntity.ok(response);
        }

        response.put("error", "User not found");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }


    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("message", "App is running");
        response.put("timestamp", String.valueOf(System.currentTimeMillis()));
        return ResponseEntity.ok(response);
    }
}