package com.instafit.core.service;

import com.instafit.core.entity.Booking;
import com.instafit.core.entity.Carpenter;
import com.instafit.core.entity.OrderRoute;
import com.instafit.core.repository.BookingRepository;
import com.instafit.core.repository.CarpenterRepository;
import com.instafit.core.repository.OrderRouteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CarpenterAssignmentService {

    private static final Logger logger = LoggerFactory.getLogger(CarpenterAssignmentService.class);

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private CarpenterRepository carpenterRepository;

    @Autowired
    private OrderRouteRepository orderRouteRepository;

    @Autowired
    private GoogleMapsService googleMapsService;

    /**
     * Manually assign orders to carpenter with date
     */
    @Transactional
    public Map<String, Object> assignOrdersToCarpenter(
            List<Long> bookingIds,
            String carpenterId,
            LocalDate assignedDate,
            String username) {

        Map<String, Object> result = new HashMap<>();

        try {
            Carpenter carpenter = carpenterRepository.findByCarpenterId(carpenterId)
                    .orElseThrow(() -> new RuntimeException("Carpenter not found: " + carpenterId));

            List<Booking> assignedBookings = new ArrayList<>();

            for (Long bookingId : bookingIds) {
                Booking booking = bookingRepository.findById(bookingId)
                        .orElseThrow(() -> new RuntimeException("Booking not found: " + bookingId));

                booking.setCarpenterId(carpenter.getCarpenterId());
                booking.setCarpenterName(carpenter.getCarpenterName());
                booking.setAssignedDate(assignedDate);
                booking.setAssignmentStatus("ASSIGNED");

                assignedBookings.add(bookingRepository.save(booking));

                logger.info("Assigned booking {} to carpenter {} for date {}",
                        bookingId, carpenterId, assignedDate);
            }

            result.put("success", true);
            result.put("message", "Successfully assigned " + bookingIds.size() + " orders to " + carpenter.getCarpenterName());
            result.put("assignedBookings", assignedBookings);

            // Trigger geocoding for assigned orders
            geocodeAssignedOrders(bookingIds);

        } catch (Exception e) {
            logger.error("Error assigning orders to carpenter", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }

        return result;
    }

    /**
     * Geocode customer addresses for assigned orders
     */
    @Transactional
    public void geocodeAssignedOrders(List<Long> bookingIds) {
        for (Long bookingId : bookingIds) {
            try {
                Booking booking = bookingRepository.findById(bookingId).orElse(null);
                if (booking == null) continue;

                String address = buildAddress(booking);

                logger.info("Geocoding booking {}: {}", bookingId, address);

                Map<String, Object> geocodeResult = googleMapsService.geocodeAddress(address);

                if (geocodeResult.get("success") != null && (Boolean) geocodeResult.get("success")) {
                    booking.setLatitude((Double) geocodeResult.get("latitude"));
                    booking.setLongitude((Double) geocodeResult.get("longitude"));
                    booking.setGeocodeStatus("SUCCESS");
                    logger.info("Successfully geocoded booking {}: lat={}, lng={}",
                            bookingId, booking.getLatitude(), booking.getLongitude());
                } else {
                    booking.setGeocodeStatus("FAILED");
                    logger.warn("Failed to geocode booking {}: {}", bookingId, geocodeResult.get("message"));
                }

                bookingRepository.save(booking);

            } catch (Exception e) {
                logger.error("Error geocoding booking " + bookingId, e);
            }
        }
    }

    /**
     * Manually update geocode for a booking
     */
    @Transactional
    public Map<String, Object> updateGeocodeManually(
            Long bookingId,
            String pincode,
            String username) {

        Map<String, Object> result = new HashMap<>();

        try {
            Booking booking = bookingRepository.findById(bookingId)
                    .orElseThrow(() -> new RuntimeException("Booking not found"));

            Map<String, Object> geocodeResult = googleMapsService.geocodePincode(pincode, "India");

            if (geocodeResult.get("success") != null && (Boolean) geocodeResult.get("success")) {
                booking.setLatitude((Double) geocodeResult.get("latitude"));
                booking.setLongitude((Double) geocodeResult.get("longitude"));
                booking.setGeocodeStatus("SUCCESS_MANUAL");

                bookingRepository.save(booking);

                result.put("success", true);
                result.put("message", "Location updated successfully");
                result.put("latitude", booking.getLatitude());
                result.put("longitude", booking.getLongitude());
            } else {
                result.put("success", false);
                result.put("message", "Failed to geocode pincode: " + geocodeResult.get("message"));
            }

        } catch (Exception e) {
            logger.error("Error updating geocode manually", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }

        return result;
    }

    /**
     * Generate optimized route for carpenter's assigned orders
     */
    @Transactional
    public Map<String, Object> generateOptimizedRoute(
            String carpenterId,
            LocalDate routeDate,
            String startPincode,
            String username) {

        Map<String, Object> result = new HashMap<>();

        try {
            List<Booking> bookings = bookingRepository.findByCarpenterIdAndAssignedDate(carpenterId, routeDate);

            if (bookings.isEmpty()) {
                result.put("success", false);
                result.put("message", "No orders assigned for this date");
                return result;
            }

            List<Booking> geocodedBookings = bookings.stream()
                    .filter(b -> b.getLatitude() != null && b.getLongitude() != null)
                    .collect(Collectors.toList());

            if (geocodedBookings.isEmpty()) {
                result.put("success", false);
                result.put("message", "No orders have valid locations");
                return result;
            }

            Map<String, Object> startGeocode = googleMapsService.geocodePincode(startPincode, "India");

            if (startGeocode.get("success") == null || !(Boolean) startGeocode.get("success")) {
                result.put("success", false);
                result.put("message", "Failed to geocode start location");
                return result;
            }

            Double startLat = (Double) startGeocode.get("latitude");
            Double startLng = (Double) startGeocode.get("longitude");

            List<Map<String, Double>> waypoints = geocodedBookings.stream()
                    .map(b -> {
                        Map<String, Double> wp = new HashMap<>();
                        wp.put("lat", b.getLatitude());
                        wp.put("lng", b.getLongitude());
                        return wp;
                    })
                    .collect(Collectors.toList());

            Map<String, Object> routeResult = googleMapsService.getOptimizedRoute(startLat, startLng, waypoints);

            if (routeResult.get("success") == null || !(Boolean) routeResult.get("success")) {
                result.put("success", false);
                result.put("message", "Failed to optimize route");
                return result;
            }

            Map<String, Object> routeData = (Map<String, Object>) routeResult.get("data");
            List<Map<String, Object>> routes = (List<Map<String, Object>>) routeData.get("routes");

            if (routes == null || routes.isEmpty()) {
                result.put("success", false);
                result.put("message", "No route found");
                return result;
            }

            Map<String, Object> route = routes.get(0);
            List<Integer> waypointOrder = (List<Integer>) route.get("waypoint_order");

            Double totalDistance = 0.0;
            Integer totalDuration = 0;

            List<Object> legs = (List<Object>) route.get("legs");
            if (legs != null) {
                for (Object leg : legs) {
                    Map<String, Object> legMap = (Map<String, Object>) leg;
                    Map<String, Object> distance = (Map<String, Object>) legMap.get("distance");
                    Map<String, Object> duration = (Map<String, Object>) legMap.get("duration");

                    if (distance != null) {
                        totalDistance += ((Number) distance.get("value")).doubleValue() / 1000.0;
                    }
                    if (duration != null) {
                        totalDuration += ((Number) duration.get("value")).intValue() / 60;
                    }
                }
            }

            for (int i = 0; i < waypointOrder.size(); i++) {
                int originalIndex = waypointOrder.get(i);
                Booking booking = geocodedBookings.get(originalIndex);
                booking.setRouteOrder(i + 1);
                bookingRepository.save(booking);
            }

            OrderRoute orderRoute = orderRouteRepository
                    .findByCarpenterIdAndRouteDate(carpenterId, routeDate)
                    .orElse(new OrderRoute());

            orderRoute.setCarpenterId(carpenterId);
            orderRoute.setRouteDate(routeDate);
            orderRoute.setStartLocation(startPincode);
            orderRoute.setStartLatitude(startLat);
            orderRoute.setStartLongitude(startLng);
            orderRoute.setTotalDistance(totalDistance);
            orderRoute.setTotalDuration(totalDuration);
            orderRoute.setOrderSequence(waypointOrder.toString());
            orderRoute.setCreatedBy(username);
            orderRoute.setActive(true);

            orderRouteRepository.save(orderRoute);

            result.put("success", true);
            result.put("message", "Route optimized successfully");
            result.put("totalDistance", String.format("%.2f km", totalDistance));
            result.put("totalDuration", totalDuration + " minutes");
            result.put("ordersInRoute", geocodedBookings.size());

        } catch (Exception e) {
            logger.error("Error generating optimized route", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }

        return result;
    }

    /**
     * Get carpenter's route for a specific date
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getCarpenterRoute(String carpenterId, LocalDate routeDate) {
        Map<String, Object> result = new HashMap<>();

        try {
            List<Booking> bookings = bookingRepository.findByCarpenterIdAndAssignedDate(carpenterId, routeDate);

            List<Booking> sortedBookings = bookings.stream()
                    .filter(b -> b.getRouteOrder() != null)
                    .sorted(Comparator.comparing(Booking::getRouteOrder))
                    .collect(Collectors.toList());

            Optional<OrderRoute> orderRoute = orderRouteRepository.findByCarpenterIdAndRouteDate(carpenterId, routeDate);

            result.put("success", true);
            result.put("bookings", sortedBookings);
            result.put("route", orderRoute.orElse(null));
            result.put("totalOrders", bookings.size());
            result.put("ordersWithRoute", sortedBookings.size());

        } catch (Exception e) {
            logger.error("Error getting carpenter route", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }

        return result;
    }

    /**
     * Build address string from booking for geocoding
     * Handles address format: "address details - pincode"
     */
    private String buildAddress(Booking booking) {
        if (booking.getAddress() == null || booking.getAddress().isEmpty()) {
            return "";
        }

        String fullAddress = booking.getAddress();

        // Extract pincode if present (6 digits after last dash)
        String pincode = extractPincode(fullAddress);

        // Build clean address for geocoding
        StringBuilder address = new StringBuilder();

        if (pincode != null && !pincode.isEmpty()) {
            // Remove pincode from address for cleaner geocoding
            String addressWithoutPincode = fullAddress.substring(0, fullAddress.lastIndexOf('-')).trim();
            address.append(addressWithoutPincode);
            address.append(", ").append(pincode);
        } else {
            address.append(fullAddress);
        }

        // Add India for better geocoding accuracy
        address.append(", India");

        return address.toString();
    }

    /**
     * Extract 6-digit pincode from address string
     * Format: "address details - 560024"
     */
    private String extractPincode(String address) {
        if (address == null || address.isEmpty()) {
            return null;
        }

        // Find last occurrence of dash
        int lastDashIndex = address.lastIndexOf('-');
        if (lastDashIndex == -1) {
            return null;
        }

        // Get text after last dash
        String afterDash = address.substring(lastDashIndex + 1).trim();

        // Check if it's a 6-digit pincode
        if (afterDash.matches("^[0-9]{6}$")) {
            return afterDash;
        }

        return null;
    }

    /**
     * Get customer pincode from booking address
     */
    public String getCustomerPincode(Booking booking) {
        return extractPincode(booking.getAddress());
    }

    /**
     * Get customer city from booking address
     * Extracts the text before the pincode
     */
    public String getCustomerCity(Booking booking) {
        if (booking.getAddress() == null || booking.getAddress().isEmpty()) {
            return "Unknown";
        }

        String address = booking.getAddress();
        int lastDashIndex = address.lastIndexOf('-');

        if (lastDashIndex != -1) {
            String addressPart = address.substring(0, lastDashIndex).trim();
            // Extract last part before dash as city (approximate)
            String[] parts = addressPart.split(",");
            if (parts.length >= 2) {
                // Return second-to-last part as city
                return parts[parts.length - 1].trim();
            }
        }

        return "Unknown";
    }
}