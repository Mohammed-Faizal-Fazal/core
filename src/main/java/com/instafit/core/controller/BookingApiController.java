package com.instafit.core.controller;

import com.instafit.core.entity.Booking;
import com.instafit.core.repository.BookingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@PreAuthorize("hasRole('OPERATION')")
public class BookingApiController {

    @Autowired
    private BookingRepository bookingRepository;

    @GetMapping("/submitted")
    public ResponseEntity<List<Booking>> getSubmittedOrders() {
        List<Booking> orders = bookingRepository.findSubmittedOrders();
        return ResponseEntity.ok(orders);
    }
}