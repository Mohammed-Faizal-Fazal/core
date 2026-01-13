package com.instafit.core.service;

import com.instafit.core.entity.Booking;
import com.instafit.core.entity.BookingLog;
import com.instafit.core.entity.FetchedBooking;
import com.instafit.core.repository.BookingRepository;
import com.instafit.core.repository.BookingLogRepository;
import com.instafit.core.repository.FetchedBookingRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Booking Service
 * Business logic for booking management, Supabase integration, and audit trail
 */
@Service
@Transactional
public class BookingService {

    private static final Logger logger = LoggerFactory.getLogger(BookingService.class);

    // Supabase Configuration
    private static final String SUPABASE_URL = "https://vvvjxzaqrxivvitrrsjr.supabase.co/rest/v1/bookings?select*";
    private static final String API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InZ2dmp4emFxcnhpdnZpdHJyc2pyIiwicm9sZSI6ImFub24iLCJpYXQiOjE3Njc2ODcyMzQsImV4cCI6MjA4MzI2MzIzNH0.Z-0tHOWVeMBx1TAHd69g2NC7KyTvJdQo5FZYj3puIe8";

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private BookingLogRepository bookingLogRepository;

    @Autowired
    private FetchedBookingRepository fetchedBookingRepository;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired(required = false)
    private HttpServletRequest request;

    /**
     * Fetch bookings from Supabase and save to database
     * Only fetches orders that haven't been fetched before
     */
    public List<Booking> fetchAndSaveBookings() {
        try {
            logger.info("Fetching bookings from Supabase...");

            // Setup HTTP headers with API key
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("apikey", API_KEY);
            headers.set("Authorization", "Bearer " + API_KEY);

            HttpEntity<String> entity = new HttpEntity<>(headers);

            // Make API call to Supabase
            ResponseEntity<String> response = restTemplate.exchange(
                    SUPABASE_URL,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                // Parse JSON response
                List<Map<String, Object>> bookingsData = objectMapper.readValue(
                        response.getBody(),
                        new TypeReference<List<Map<String, Object>>>() {}
                );

                logger.info("Fetched {} bookings from Supabase", bookingsData.size());

                int newCount = 0;
                int skippedCount = 0;

                // Process each booking
                for (Map<String, Object> data : bookingsData) {
                    try {
                        String orderNo = getString(data, "order_no");

                        // Check if already fetched (prevent duplicates)
                        if (fetchedBookingRepository.existsByOrderNo(orderNo)) {
                            logger.debug("Skipping already fetched order: {}", orderNo);
                            skippedCount++;
                            continue;
                        }

                        // Map data to Booking entity
                        Booking booking = mapToBooking(data);
                        Booking savedBooking = bookingRepository.save(booking);

                        // Mark as fetched
                        FetchedBooking fetchedBooking = new FetchedBooking(
                                orderNo,
                                savedBooking.getId(),
                                "SYSTEM"
                        );
                        fetchedBookingRepository.save(fetchedBooking);

                        // Create audit log
                        createLog(savedBooking.getId(), orderNo, "FETCHED", "SYSTEM",
                                null, "Fetched from Supabase API");

                        newCount++;
                        logger.debug("Saved new booking: {}", orderNo);

                    } catch (Exception e) {
                        logger.error("Error processing booking: {}", data, e);
                    }
                }

                logger.info("Fetch complete: {} new, {} skipped", newCount, skippedCount);
                return bookingRepository.findAll();
            }

            logger.error("Failed to fetch bookings. Status: {}", response.getStatusCode());
            return bookingRepository.findAll();

        } catch (Exception e) {
            logger.error("Error fetching bookings from Supabase", e);
            return bookingRepository.findAll();
        }
    }

    /**
     * Map Supabase JSON data to Booking entity
     */
    private Booking mapToBooking(Map<String, Object> data) throws Exception {
        Booking booking = new Booking();

        booking.setOrderNo(getString(data, "order_no"));
        booking.setUserId(getString(data, "user_id"));
        booking.setCustomerName(getString(data, "customer_name"));
        booking.setCustomerMobile(getString(data, "customer_mobile"));
        booking.setServiceName(getString(data, "service_name"));
        booking.setServiceId(getInteger(data, "service_id"));
        booking.setStatus(getString(data, "status"));
        booking.setPaymentId(getString(data, "payment_id"));
        booking.setAddress(getString(data, "address"));
        booking.setEmployeeName(getString(data, "employee_name"));
        booking.setEmployeePhone(getString(data, "employee_phone"));

        // Parse service types (JSON array to string)
        if (data.get("service_types") != null) {
            booking.setServiceTypes(objectMapper.writeValueAsString(data.get("service_types")));
        }

        // Parse date
        if (data.get("date") != null) {
            booking.setDate(LocalDate.parse(data.get("date").toString()));
        }

        // Parse time
        if (data.get("booking_time") != null) {
            String timeStr = data.get("booking_time").toString();
            booking.setBookingTime(LocalTime.parse(timeStr));
        }

        // Parse created_at timestamp
        if (data.get("created_at") != null) {
            String createdAtStr = data.get("created_at").toString();
            createdAtStr = createdAtStr.replace("Z", "").replace("T", " ");
            if (createdAtStr.contains(".")) {
                createdAtStr = createdAtStr.substring(0, createdAtStr.indexOf("."));
            }
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            booking.setCreatedAt(LocalDateTime.parse(createdAtStr, formatter));
        }

        // Parse price
        if (data.get("total_price") != null) {
            booking.setTotalPrice(new BigDecimal(data.get("total_price").toString()));
        }

        return booking;
    }

    /**
     * Helper: Extract string from map
     */
    private String getString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }

    /**
     * Helper: Extract integer from map
     */
    private Integer getInteger(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Get all bookings
     */
    @Transactional(readOnly = true)
    public List<Booking> getAllBookings() {
        return bookingRepository.findAll();
    }

    /**
     * Get booking by ID
     */
    @Transactional(readOnly = true)
    public Booking getBookingById(Long id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Booking not found with id: " + id));
    }

    /**
     * Update booking with audit trail
     */
    public Booking updateBooking(Long id, Booking updatedBooking, String username) {
        Booking booking = getBookingById(id);

        Map<String, String> changes = new HashMap<>();

        // Track and update changes
        if (updatedBooking.getCustomerName() != null &&
                !updatedBooking.getCustomerName().equals(booking.getCustomerName())) {
            changes.put("customerName", booking.getCustomerName() + " → " + updatedBooking.getCustomerName());
            booking.setCustomerName(updatedBooking.getCustomerName());
        }

        if (updatedBooking.getCustomerMobile() != null &&
                !updatedBooking.getCustomerMobile().equals(booking.getCustomerMobile())) {
            changes.put("customerMobile", booking.getCustomerMobile() + " → " + updatedBooking.getCustomerMobile());
            booking.setCustomerMobile(updatedBooking.getCustomerMobile());
        }

        if (updatedBooking.getAddress() != null &&
                !updatedBooking.getAddress().equals(booking.getAddress())) {
            changes.put("address", "Address updated");
            booking.setAddress(updatedBooking.getAddress());
        }

        if (updatedBooking.getDate() != null &&
                !updatedBooking.getDate().equals(booking.getDate())) {
            changes.put("date", booking.getDate() + " → " + updatedBooking.getDate());
            booking.setDate(updatedBooking.getDate());
        }

        if (updatedBooking.getBookingTime() != null &&
                !updatedBooking.getBookingTime().equals(booking.getBookingTime())) {
            changes.put("bookingTime", booking.getBookingTime() + " → " + updatedBooking.getBookingTime());
            booking.setBookingTime(updatedBooking.getBookingTime());
        }

        if (updatedBooking.getEmployeeName() != null &&
                !updatedBooking.getEmployeeName().equals(booking.getEmployeeName())) {
            changes.put("employeeName",
                    (booking.getEmployeeName() != null ? booking.getEmployeeName() : "None") +
                            " → " + updatedBooking.getEmployeeName());
            booking.setEmployeeName(updatedBooking.getEmployeeName());
        }

        if (updatedBooking.getEmployeePhone() != null &&
                !updatedBooking.getEmployeePhone().equals(booking.getEmployeePhone())) {
            changes.put("employeePhone",
                    (booking.getEmployeePhone() != null ? booking.getEmployeePhone() : "None") +
                            " → " + updatedBooking.getEmployeePhone());
            booking.setEmployeePhone(updatedBooking.getEmployeePhone());
        }

        if (updatedBooking.getNotes() != null) {
            booking.setNotes(updatedBooking.getNotes());
            changes.put("notes", "Notes updated");
        }

        Booking saved = bookingRepository.save(booking);

        // Create audit log for changes
        if (!changes.isEmpty()) {
            try {
                String changesJson = objectMapper.writeValueAsString(changes);
                createLog(saved.getId(), saved.getOrderNo(), "UPDATED", username,
                        changesJson, "Booking updated");
            } catch (Exception e) {
                logger.error("Error logging changes", e);
            }
        }

        return saved;
    }


    public Booking submitBooking(Long bookingId, String username) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        booking.setStatus("Submitted");
        booking.setSubmittedBy(username);
        booking.setSubmittedAt(LocalDateTime.now());

        Booking saved = bookingRepository.save(booking);

        // Create audit log
        createLog(saved.getId(), saved.getOrderNo(), "SUBMITTED", username,
                null, "Booking submitted for processing");

        return saved;
    }


    @Transactional(readOnly = true)
    public List<Booking> getBookingsByStatus(String status) {
        return bookingRepository.findByStatus(status);
    }


    @Transactional(readOnly = true)
    public List<BookingLog> getBookingHistory(Long bookingId) {
        return bookingLogRepository.findByBookingIdOrderByCreatedAtDesc(bookingId);
    }


    private void createLog(Long bookingId, String orderNo, String actionType,
                           String changedBy, String changes, String notes) {
        BookingLog log = new BookingLog(bookingId, orderNo, actionType, changedBy);
        log.setNewValue(changes);
        log.setNotes(notes);


        if (request != null) {
            String ipAddress = request.getHeader("X-Forwarded-For");
            if (ipAddress == null || ipAddress.isEmpty()) {
                ipAddress = request.getRemoteAddr();
            }
            log.setIpAddress(ipAddress);
        }

        bookingLogRepository.save(log);
        logger.debug("Created {} log for booking {}", actionType, orderNo);
    }
}