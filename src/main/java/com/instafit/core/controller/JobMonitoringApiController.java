package com.instafit.core.controller;

import com.instafit.core.entity.Booking;
import com.instafit.core.entity.Carpenter;
import com.instafit.core.repository.BookingRepository;
import com.instafit.core.repository.CarpenterRepository;
import com.instafit.core.service.CarpenterAssignmentService;
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

@RestController
@RequestMapping("/api/job-monitoring")
@PreAuthorize("hasRole('OPERATION')")
public class JobMonitoringApiController {

    private static final Logger logger = LoggerFactory.getLogger(JobMonitoringApiController.class);

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private CarpenterRepository carpenterRepository;

    @Autowired
    private CarpenterAssignmentService carpenterAssignmentService;

    @GetMapping("/all")
    public ResponseEntity<List<Booking>> getAllAssignedJobs() {
        try {
            // Get all jobs that have been assigned to carpenters
            List<Booking> jobs = bookingRepository.findByCarpenterIdIsNotNull();

            // Sort by updated date descending
            jobs.sort((a, b) -> {
                if (b.getAssignedDate() == null) return -1;
                if (a.getAssignedDate() == null) return 1;
                return b.getAssignedDate().compareTo(a.getAssignedDate());
            });

            return ResponseEntity.ok(jobs);
        } catch (Exception e) {
            logger.error("Error fetching all jobs", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/reassign")
    public ResponseEntity<Map<String, Object>> reassignJob(
            @RequestBody ReassignRequest request,
            Authentication authentication) {

        Map<String, Object> response = new HashMap<>();

        try {
            // Get the job
            Booking job = bookingRepository.findById(request.getJobId())
                    .orElseThrow(() -> new RuntimeException("Job not found"));

            // Get the new carpenter
            Carpenter carpenter = carpenterRepository.findByCarpenterId(request.getCarpenterId())
                    .orElseThrow(() -> new RuntimeException("Carpenter not found"));

            String oldCarpenter = job.getCarpenterName();
            String oldStatus = job.getAssignmentStatus();

            // Update job assignment
            job.setCarpenterId(carpenter.getCarpenterId());
            job.setCarpenterName(carpenter.getCarpenterName());
            job.setAssignedDate(request.getAssignedDate());
            job.setAssignmentStatus("ASSIGNED");
            job.setRouteOrder(null); // Reset route order

            // Add reassignment notes
            String reassignNote = String.format(
                    "\n[%s] REASSIGNED by %s\nFrom: %s (%s)\nTo: %s\nReason: %s",
                    LocalDateTime.now(),
                    authentication.getName(),
                    oldCarpenter,
                    oldStatus,
                    carpenter.getCarpenterName(),
                    request.getNotes() != null ? request.getNotes() : "Manual reassignment"
            );

            String existingNotes = job.getNotes() != null ? job.getNotes() : "";
            job.setNotes(existingNotes + reassignNote);

            bookingRepository.save(job);

            logger.info("Job {} reassigned from {} to {}",
                    job.getOrderNo(), oldCarpenter, carpenter.getCarpenterName());

            // Handle routing based on option
            if ("auto".equals(request.getRoutingOption())) {
                // Geocode the job if not already done
                if (job.getLatitude() == null || job.getLongitude() == null) {
                    carpenterAssignmentService.geocodeAssignedOrders(Arrays.asList(job.getId()));
                }

                // Generate optimized route
                Map<String, Object> routeResult = carpenterAssignmentService.generateOptimizedRoute(
                        carpenter.getCarpenterId(),
                        request.getAssignedDate(),
                        request.getStartPincode(),
                        authentication.getName()
                );

                if (routeResult.get("success") != null && (Boolean) routeResult.get("success")) {
                    response.put("message", "Job reassigned successfully with optimized route!");
                    response.put("routeInfo", routeResult);
                } else {
                    response.put("message", "Job reassigned but route optimization failed: " + routeResult.get("message"));
                }
            } else {
                response.put("message", "Job reassigned successfully (manual routing - no auto-route generated)");
            }

            response.put("success", true);
            response.put("newCarpenter", carpenter.getCarpenterName());
            response.put("newCarpenterId", carpenter.getCarpenterId());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error reassigning job", e);
            response.put("success", false);
            response.put("message", "Error reassigning job: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    public static class ReassignRequest {
        private Long jobId;
        private String carpenterId;
        private LocalDate assignedDate;
        private String startPincode;
        private String routingOption; // "auto" or "manual"
        private String notes;

        public Long getJobId() {
            return jobId;
        }

        public void setJobId(Long jobId) {
            this.jobId = jobId;
        }

        public String getCarpenterId() {
            return carpenterId;
        }

        public void setCarpenterId(String carpenterId) {
            this.carpenterId = carpenterId;
        }

        public LocalDate getAssignedDate() {
            return assignedDate;
        }

        public void setAssignedDate(LocalDate assignedDate) {
            this.assignedDate = assignedDate;
        }

        public String getStartPincode() {
            return startPincode;
        }

        public void setStartPincode(String startPincode) {
            this.startPincode = startPincode;
        }

        public String getRoutingOption() {
            return routingOption;
        }

        public void setRoutingOption(String routingOption) {
            this.routingOption = routingOption;
        }

        public String getNotes() {
            return notes;
        }

        public void setNotes(String notes) {
            this.notes = notes;
        }
    }
}