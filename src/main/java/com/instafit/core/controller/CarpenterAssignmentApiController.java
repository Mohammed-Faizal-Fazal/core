package com.instafit.core.controller;

import com.instafit.core.service.CarpenterAssignmentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/carpenter-assignment")
public class CarpenterAssignmentApiController {

    private static final Logger logger = LoggerFactory.getLogger(CarpenterAssignmentApiController.class);

    @Autowired
    private CarpenterAssignmentService carpenterAssignmentService;

    @PostMapping("/assign")
    @PreAuthorize("hasRole('OPERATION')")
    public ResponseEntity<Map<String, Object>> assignOrders(
            @RequestBody AssignmentRequest request,
            Authentication authentication) {

        logger.info("Assigning {} orders to carpenter {} for date {}",
                request.getBookingIds().size(),
                request.getCarpenterId(),
                request.getAssignedDate());

        Map<String, Object> result = carpenterAssignmentService.assignOrdersToCarpenter(
                request.getBookingIds(),
                request.getCarpenterId(),
                request.getAssignedDate(),
                authentication.getName()
        );

        return ResponseEntity.ok(result);
    }

    @PostMapping("/update-geocode/{bookingId}")
    @PreAuthorize("hasRole('OPERATION')")
    public ResponseEntity<Map<String, Object>> updateGeocode(
            @PathVariable Long bookingId,
            @RequestParam String pincode,
            Authentication authentication) {

        Map<String, Object> result = carpenterAssignmentService.updateGeocodeManually(
                bookingId,
                pincode,
                authentication.getName()
        );

        return ResponseEntity.ok(result);
    }

    @PostMapping("/update-address/{bookingId}")
    @PreAuthorize("hasRole('OPERATION')")
    public ResponseEntity<Map<String, Object>> updateAddress(
            @PathVariable Long bookingId,
            @RequestBody Map<String, String> request,
            Authentication authentication) {

        String newAddress = request.get("address");
        logger.info("Updating address for booking {} to: {}", bookingId, newAddress);

        Map<String, Object> result = carpenterAssignmentService.updateAddressAndGeocode(
                bookingId,
                newAddress,
                authentication.getName()
        );

        return ResponseEntity.ok(result);
    }

    @PostMapping("/update-coordinates/{bookingId}")
    @PreAuthorize("hasRole('OPERATION')")
    public ResponseEntity<Map<String, Object>> updateCoordinates(
            @PathVariable Long bookingId,
            @RequestBody CoordinatesRequest request,
            Authentication authentication) {

        logger.info("Updating coordinates for booking {} to: {}, {}",
                bookingId, request.getLatitude(), request.getLongitude());

        Map<String, Object> result = carpenterAssignmentService.updateCoordinatesDirectly(
                bookingId,
                request.getLatitude(),
                request.getLongitude(),
                authentication.getName()
        );

        return ResponseEntity.ok(result);
    }

    @PostMapping("/generate-route")
    @PreAuthorize("hasRole('OPERATION')")
    public ResponseEntity<Map<String, Object>> generateRoute(
            @RequestBody RouteRequest request,
            Authentication authentication) {

        logger.info("Generating route for carpenter {} on date {}",
                request.getCarpenterId(),
                request.getRouteDate());

        Map<String, Object> result = carpenterAssignmentService.generateOptimizedRoute(
                request.getCarpenterId(),
                request.getRouteDate(),
                request.getStartPincode(),
                authentication.getName()
        );

        return ResponseEntity.ok(result);
    }

    @GetMapping("/route/{carpenterId}")
    @PreAuthorize("hasAnyRole('OPERATION', 'CARPENTER')")
    public ResponseEntity<Map<String, Object>> getCarpenterRoute(
            @PathVariable String carpenterId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        Map<String, Object> result = carpenterAssignmentService.getCarpenterRoute(carpenterId, date);
        return ResponseEntity.ok(result);
    }

    public static class AssignmentRequest {
        private List<Long> bookingIds;
        private String carpenterId;
        private LocalDate assignedDate;

        public List<Long> getBookingIds() {
            return bookingIds;
        }

        public void setBookingIds(List<Long> bookingIds) {
            this.bookingIds = bookingIds;
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
    }

    public static class RouteRequest {
        private String carpenterId;
        private LocalDate routeDate;
        private String startPincode;

        public String getCarpenterId() {
            return carpenterId;
        }

        public void setCarpenterId(String carpenterId) {
            this.carpenterId = carpenterId;
        }

        public LocalDate getRouteDate() {
            return routeDate;
        }

        public void setRouteDate(LocalDate routeDate) {
            this.routeDate = routeDate;
        }

        public String getStartPincode() {
            return startPincode;
        }

        public void setStartPincode(String startPincode) {
            this.startPincode = startPincode;
        }
    }

    public static class CoordinatesRequest {
        private Double latitude;
        private Double longitude;

        public Double getLatitude() {
            return latitude;
        }

        public void setLatitude(Double latitude) {
            this.latitude = latitude;
        }

        public Double getLongitude() {
            return longitude;
        }

        public void setLongitude(Double longitude) {
            this.longitude = longitude;
        }
    }
}