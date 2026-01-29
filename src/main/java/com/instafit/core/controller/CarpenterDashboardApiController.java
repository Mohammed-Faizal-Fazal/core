package com.instafit.core.controller;

import com.instafit.core.entity.Booking;
import com.instafit.core.entity.Carpenter;
import com.instafit.core.entity.User;
import com.instafit.core.repository.BookingRepository;
import com.instafit.core.service.CarpenterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/carpenters/dashboard")
@PreAuthorize("hasRole('CARPENTER')")
public class CarpenterDashboardApiController {

    private static final Logger logger = LoggerFactory.getLogger(CarpenterDashboardApiController.class);

    @Autowired
    private CarpenterService carpenterService;

    @Autowired
    private BookingRepository bookingRepository;

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getDashboardStats(Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        try {
            User user = (User) authentication.getPrincipal();
            Carpenter carpenter = carpenterService.getCarpenterByMobile(user.getPhoneNumber());

            if (carpenter == null) {
                response.put("success", false);
                response.put("message", "Carpenter profile not found");
                return ResponseEntity.badRequest().body(response);
            }

            String carpenterId = carpenter.getCarpenterId();

            // Get all jobs for this carpenter
            List<Booking> allJobs = bookingRepository.findByCarpenterId(carpenterId);

            // Calculate counts
            long pendingCount = allJobs.stream()
                    .filter(b -> "ASSIGNED".equals(b.getAssignmentStatus()))
                    .count();

            long completedTodayCount = allJobs.stream()
                    .filter(b -> "COMPLETED".equals(b.getAssignmentStatus()) &&
                            isToday(b.getAssignedDate()))
                    .count();

            long monthlyCount = allJobs.stream()
                    .filter(b -> isCurrentMonth(b.getAssignedDate()))
                    .count();

            // Get recent jobs (last 5)
            List<Map<String, Object>> recentJobs = allJobs.stream()
                    .sorted((a, b) -> {
                        LocalDate dateA = a.getAssignedDate() != null ? a.getAssignedDate() : LocalDate.MIN;
                        LocalDate dateB = b.getAssignedDate() != null ? b.getAssignedDate() : LocalDate.MIN;
                        return dateB.compareTo(dateA);
                    })
                    .limit(5)
                    .map(this::mapBookingToJob)
                    .collect(Collectors.toList());

            response.put("success", true);
            response.put("carpenterId", carpenterId);
            response.put("carpenterName", carpenter.getCarpenterName());
            response.put("pendingJobs", pendingCount);
            response.put("completedToday", completedTodayCount);
            response.put("totalThisMonth", monthlyCount);
            response.put("totalJobs", allJobs.size());
            response.put("completedTotal", allJobs.stream().filter(b -> "COMPLETED".equals(b.getAssignmentStatus())).count());
            response.put("inProgress", allJobs.stream().filter(b -> "IN_PROGRESS".equals(b.getAssignmentStatus())).count());
            response.put("recentJobs", recentJobs);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error fetching dashboard stats", e);
            response.put("success", false);
            response.put("message", "Error loading dashboard");
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/profile")
    public ResponseEntity<Map<String, Object>> getProfileInfo(Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        try {
            User user = (User) authentication.getPrincipal();
            Carpenter carpenter = carpenterService.getCarpenterByMobile(user.getPhoneNumber());

            if (carpenter == null) {
                response.put("success", false);
                response.put("message", "Carpenter profile not found");
                return ResponseEntity.badRequest().body(response);
            }

            response.put("success", true);
            response.put("carpenterId", carpenter.getCarpenterId());
            response.put("name", carpenter.getCarpenterName());
            response.put("mobile", carpenter.getMobile());
            response.put("email", user.getEmail());
            response.put("cityCode", carpenter.getCityCode());
            response.put("branchCode", carpenter.getBranchCode());
            response.put("jobType", carpenter.getJobType());
            response.put("active", carpenter.getActive());
            response.put("createdAt", carpenter.getCreatedAt());

            // Get job statistics
            List<Booking> allJobs = bookingRepository.findByCarpenterId(carpenter.getCarpenterId());
            long completed = allJobs.stream().filter(b -> "COMPLETED".equals(b.getAssignmentStatus())).count();
            long total = allJobs.size();
            int successRate = total > 0 ? (int)((completed * 100.0) / total) : 0;

            response.put("totalJobs", total);
            response.put("completedJobs", completed);
            response.put("inProgressJobs", allJobs.stream().filter(b -> "IN_PROGRESS".equals(b.getAssignmentStatus())).count());
            response.put("pendingJobs", allJobs.stream().filter(b -> "ASSIGNED".equals(b.getAssignmentStatus())).count());
            response.put("successRate", successRate);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error fetching profile info", e);
            response.put("success", false);
            response.put("message", "Error loading profile");
            return ResponseEntity.badRequest().body(response);
        }
    }

    private boolean isToday(LocalDate date) {
        if (date == null) return false;
        return date.equals(LocalDate.now());
    }

    private boolean isCurrentMonth(LocalDate date) {
        if (date == null) return false;
        LocalDate now = LocalDate.now();
        return date.getYear() == now.getYear() &&
                date.getMonth() == now.getMonth();
    }

    private Map<String, Object> mapBookingToJob(Booking booking) {
        Map<String, Object> job = new HashMap<>();
        job.put("id", booking.getId());
        job.put("orderNo", booking.getOrderNo());
        job.put("serviceName", booking.getServiceName());
        job.put("customerName", booking.getCustomerName());
        job.put("scheduledDate", booking.getAssignedDate());
        job.put("status", booking.getAssignmentStatus());
        job.put("address", booking.getAddress());
        return job;
    }
}