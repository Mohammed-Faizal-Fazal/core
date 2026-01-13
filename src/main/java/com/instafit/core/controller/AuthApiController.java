package com.instafit.core.controller;

import com.instafit.core.entity.Carpenter;
import com.instafit.core.entity.User;
import com.instafit.core.service.AuthService;
import com.instafit.core.service.CarpenterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthApiController {

    private static final Logger logger = LoggerFactory.getLogger(AuthApiController.class);

    @Autowired
    private AuthService authService;

    @Autowired
    private CarpenterService carpenterService;

    @GetMapping("/check-phone")
    public ResponseEntity<Map<String, Object>> checkPhoneNumber(@RequestParam String phoneNumber) {
        Map<String, Object> response = new HashMap<>();
        try {
            boolean available = authService.isPhoneNumberAvailable(phoneNumber);
            response.put("success", true);
            response.put("available", available);
            response.put("message", available ? "Phone number is available" : "Phone number already registered");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error checking phone number", e);
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody RegistrationRequest request) {
        Map<String, Object> response = new HashMap<>();
        try {
            if (request.getPhoneNumber() == null || !request.getPhoneNumber().matches("^[0-9]{10}$")) {
                response.put("success", false);
                response.put("message", "Invalid phone number. Must be 10 digits.");
                return ResponseEntity.badRequest().body(response);
            }

            if (request.getPassword() == null || request.getPassword().length() < 6) {
                response.put("success", false);
                response.put("message", "Password must be at least 6 characters");
                return ResponseEntity.badRequest().body(response);
            }

            if (request.getFullName() == null || request.getFullName().trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "Full name is required");
                return ResponseEntity.badRequest().body(response);
            }

            if (request.getRole() == null || (!request.getRole().equals("OPERATION") && !request.getRole().equals("CARPENTER"))) {
                response.put("success", false);
                response.put("message", "Invalid role. Must be OPERATION or CARPENTER");
                return ResponseEntity.badRequest().body(response);
            }

            User user = new User();
            user.setPhoneNumber(request.getPhoneNumber());
            user.setFullName(request.getFullName());
            user.setEmail(request.getEmail());
            user.setPassword(request.getPassword());
            user.setRole(User.Role.valueOf(request.getRole()));
            user.setActive(true);

            User registeredUser = authService.registerUser(user);

            if (request.getRole().equals("CARPENTER") && request.getCarpenterData() != null) {
                CarpenterData carpData = request.getCarpenterData();

                if (carpData.getCarpenterId() == null || carpData.getCarpenterId().trim().isEmpty()) {
                    response.put("success", false);
                    response.put("message", "Carpenter ID is required");
                    return ResponseEntity.badRequest().body(response);
                }

                if (carpData.getPincode() == null || !carpData.getPincode().matches("^[0-9]{6}$")) {
                    response.put("success", false);
                    response.put("message", "Valid 6-digit pincode is required");
                    return ResponseEntity.badRequest().body(response);
                }

                Carpenter carpenter = new Carpenter();
                carpenter.setCarpenterId(carpData.getCarpenterId().toUpperCase());
                carpenter.setCarpenterName(request.getFullName());
                carpenter.setMobile(request.getPhoneNumber());
                carpenter.setEmail(request.getEmail());
                carpenter.setJobType(carpData.getJobType());
                carpenter.setCityCode(carpData.getCityCode());
                carpenter.setBranchCode(carpData.getBranchCode());
                carpenter.setPincode(carpData.getPincode());
                carpenter.setActive(true);

                carpenterService.registerCarpenterFromRegistration(carpenter, "SELF_REGISTER");

                response.put("message", "Carpenter registered successfully! You can now login with your mobile number and password.");
            } else {
                response.put("message", "Registration successful! You can now login with your mobile number and password.");
            }

            response.put("success", true);

            Map<String, Object> userData = new HashMap<>();
            userData.put("phoneNumber", registeredUser.getPhoneNumber());
            userData.put("fullName", registeredUser.getFullName());
            userData.put("role", registeredUser.getRole().name());
            response.put("data", userData);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error during registration", e);
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    public static class RegistrationRequest {
        private String phoneNumber;
        private String fullName;
        private String email;
        private String password;
        private String role;
        private CarpenterData carpenterData;

        public RegistrationRequest() {
        }

        public String getPhoneNumber() {
            return phoneNumber;
        }

        public void setPhoneNumber(String phoneNumber) {
            this.phoneNumber = phoneNumber;
        }

        public String getFullName() {
            return fullName;
        }

        public void setFullName(String fullName) {
            this.fullName = fullName;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public CarpenterData getCarpenterData() {
            return carpenterData;
        }

        public void setCarpenterData(CarpenterData carpenterData) {
            this.carpenterData = carpenterData;
        }
    }

    public static class CarpenterData {
        private String carpenterId;
        private String jobType;
        private String cityCode;
        private String branchCode;
        private String pincode;

        public CarpenterData() {
        }

        public String getCarpenterId() {
            return carpenterId;
        }

        public void setCarpenterId(String carpenterId) {
            this.carpenterId = carpenterId;
        }

        public String getJobType() {
            return jobType;
        }

        public void setJobType(String jobType) {
            this.jobType = jobType;
        }

        public String getCityCode() {
            return cityCode;
        }

        public void setCityCode(String cityCode) {
            this.cityCode = cityCode;
        }

        public String getBranchCode() {
            return branchCode;
        }

        public void setBranchCode(String branchCode) {
            this.branchCode = branchCode;
        }

        public String getPincode() {
            return pincode;
        }

        public void setPincode(String pincode) {
            this.pincode = pincode;
        }
    }
}