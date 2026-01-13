package com.instafit.core.controller;

import com.instafit.core.entity.Booking;
import com.instafit.core.repository.BookingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/carpenter-jobs")
@PreAuthorize("hasRole('CARPENTER')")
public class CarpenterJobsApiController {

    private static final Logger logger = LoggerFactory.getLogger(CarpenterJobsApiController.class);

    private static final String UPLOAD_DIR = "uploads/job-images/";

    @Autowired
    private BookingRepository bookingRepository;

    @GetMapping("/{carpenterId}")
    public ResponseEntity<List<Booking>> getCarpenterJobs(@PathVariable String carpenterId) {
        try {
            List<Booking> jobs = bookingRepository.findByCarpenterId(carpenterId);
            return ResponseEntity.ok(jobs);
        } catch (Exception e) {
            logger.error("Error fetching carpenter jobs", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{jobId}/update-status")
    public ResponseEntity<Map<String, Object>> updateJobStatus(
            @PathVariable Long jobId,
            @RequestParam("status") String status,
            @RequestParam(value = "notes", required = false) String notes,
            @RequestParam(value = "images", required = false) List<MultipartFile> images) {

        Map<String, Object> response = new HashMap<>();

        try {
            Booking job = bookingRepository.findById(jobId)
                    .orElseThrow(() -> new RuntimeException("Job not found"));

            // Update status
            job.setAssignmentStatus(status);

            // Update notes if provided
            if (notes != null && !notes.trim().isEmpty()) {
                String existingNotes = job.getNotes() != null ? job.getNotes() : "";
                job.setNotes(existingNotes + "\n[" + LocalDateTime.now() + "] " + notes);
            }

            // Handle image uploads
            if (images != null && !images.isEmpty()) {
                List<String> imagePaths = saveImages(jobId, images);
                String imagePathsStr = String.join(",", imagePaths);

                // Store image paths in notes or create a separate field
                String imageNote = "\n[Images uploaded: " + imagePathsStr + "]";
                job.setNotes((job.getNotes() != null ? job.getNotes() : "") + imageNote);
            }

            bookingRepository.save(job);

            response.put("success", true);
            response.put("message", "Job status updated successfully");
            response.put("newStatus", status);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error updating job status", e);
            response.put("success", false);
            response.put("message", "Error updating job status: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    private List<String> saveImages(Long jobId, List<MultipartFile> images) throws IOException {
        List<String> imagePaths = new ArrayList<>();

        // Create directory if it doesn't exist
        File uploadDir = new File(UPLOAD_DIR + jobId);
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }

        for (int i = 0; i < images.size(); i++) {
            MultipartFile image = images.get(i);
            String originalFilename = image.getOriginalFilename();
            String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String filename = "job_" + jobId + "_" + System.currentTimeMillis() + "_" + i + extension;

            Path filePath = Paths.get(UPLOAD_DIR + jobId, filename);
            Files.write(filePath, image.getBytes());

            imagePaths.add(filePath.toString());
            logger.info("Saved image: {}", filePath);
        }

        return imagePaths;
    }
}