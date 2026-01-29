package com.instafit.core.controller;

import com.instafit.core.entity.Booking;
import com.instafit.core.entity.BookingLog;
import com.instafit.core.service.BookingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Booking Controller
 * Handles booking-related pages and API endpoints
 */
@Controller
public class BookingController {

    @Autowired
    private BookingService bookingService;

    /**
     * Bookings management page
     */
    @GetMapping("/bookings")
    @PreAuthorize("hasRole('OPERATION')")
    public String bookingsPage(Model model) {

        model.addAttribute("activePage", "bookings");
        return "bookings";
    }

    /**
     * Submitted orders page
     */
   /* @GetMapping("/submitted-orders")
    @PreAuthorize("hasRole('OPERATION')")
    public String submittedOrdersPage(Model model) {
        model.addAttribute("activePage", "submitted-orders");
        return "submitted-orders";
    }*/

    /**
     * API: Fetch bookings from Supabase
     */
    @GetMapping("/api/bookings/fetch")
    @ResponseBody
    @PreAuthorize("hasRole('OPERATION')")
    public ResponseEntity<Map<String, Object>> fetchBookings() {
        try {
            List<Booking> bookings = bookingService.fetchAndSaveBookings();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Bookings fetched successfully");
            response.put("count", bookings.size());
            response.put("data", bookings);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error fetching bookings: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * API: Get all bookings
     */
    @GetMapping("/api/bookings")
    @ResponseBody
    @PreAuthorize("hasRole('OPERATION')")
    public ResponseEntity<List<Booking>> getAllBookings() {
        return ResponseEntity.ok(bookingService.getAllBookings());
    }

    /**
     * API: Get booking by ID
     */
    @GetMapping("/api/bookings/{id}")
    @ResponseBody
    @PreAuthorize("hasRole('OPERATION')")
    public ResponseEntity<Map<String, Object>> getBookingById(@PathVariable Long id) {
        try {
            Booking booking = bookingService.getBookingById(id);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", booking);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(404).body(response);
        }
    }

    /**
     * API: Get booking history (audit trail)
     */
    @GetMapping("/api/bookings/{id}/history")
    @ResponseBody
    @PreAuthorize("hasRole('OPERATION')")
    public ResponseEntity<Map<String, Object>> getBookingHistory(@PathVariable Long id) {
        try {
            List<BookingLog> history = bookingService.getBookingHistory(id);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", history);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * API: Update booking
     */
    @PutMapping("/api/bookings/{id}")
    @ResponseBody
    @PreAuthorize("hasRole('OPERATION')")
    public ResponseEntity<Map<String, Object>> updateBooking(
            @PathVariable Long id,
            @RequestBody Booking updatedBooking,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            Booking booking = bookingService.updateBooking(id, updatedBooking, username);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Booking updated successfully");
            response.put("data", booking);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error updating booking: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * API: Submit booking
     */
    @PostMapping("/api/bookings/{id}/submit")
    @ResponseBody
    @PreAuthorize("hasRole('OPERATION')")
    public ResponseEntity<Map<String, Object>> submitBooking(
            @PathVariable Long id,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            Booking booking = bookingService.submitBooking(id, username);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Booking submitted successfully");
            response.put("data", booking);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error submitting booking: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * API: Get bookings by status
     */
    @GetMapping("/api/bookings/status/{status}")
    @ResponseBody
    @PreAuthorize("hasRole('OPERATION')")
    public ResponseEntity<List<Booking>> getBookingsByStatus(@PathVariable String status) {
        return ResponseEntity.ok(bookingService.getBookingsByStatus(status));
    }
}