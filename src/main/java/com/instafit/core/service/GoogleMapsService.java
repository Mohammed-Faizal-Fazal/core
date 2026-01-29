package com.instafit.core.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import javax.net.ssl.*;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.*;

@Service
public class GoogleMapsService {

    private static final Logger logger = LoggerFactory.getLogger(GoogleMapsService.class);

    @Value("${google.maps.api.key:}")
    private String apiKey;

    private RestTemplate createTrustAllRestTemplate() {
        try {
            // Create a trust manager that accepts all certificates
            TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        public X509Certificate[] getAcceptedIssuers() {
                            return null;
                        }
                        public void checkClientTrusted(X509Certificate[] certs, String authType) {
                        }
                        public void checkServerTrusted(X509Certificate[] certs, String authType) {
                        }
                    }
            };

            // Install the all-trusting trust manager
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());

            // Create an ssl socket factory with our all-trusting manager
            SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

            // Configure HttpsURLConnection to use this
            HttpsURLConnection.setDefaultSSLSocketFactory(sslSocketFactory);
            HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);

            return new RestTemplate();
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            logger.error("Failed to create SSL trust-all context", e);
            return new RestTemplate();
        }
    }

    /**
     * Geocode an address to get latitude and longitude
     */
    public Map<String, Object> geocodeAddress(String address) {
        Map<String, Object> result = new HashMap<>();

        if (apiKey == null || apiKey.isEmpty()) {
            logger.error("Google Maps API key not configured");
            result.put("success", false);
            result.put("message", "Google Maps API key not configured");
            return result;
        }

        try {
            String url = UriComponentsBuilder
                    .fromHttpUrl("https://maps.googleapis.com/maps/api/geocode/json")
                    .queryParam("address", address)
                    .queryParam("key", apiKey)
                    .toUriString();

            logger.info("Geocoding address: {}", address);

            // Use the trust-all REST template
            RestTemplate restTemplate = createTrustAllRestTemplate();
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            Map<String, Object> responseBody = response.getBody();

            if (responseBody != null && "OK".equals(responseBody.get("status"))) {
                List<Map<String, Object>> results = (List<Map<String, Object>>) responseBody.get("results");

                if (results != null && !results.isEmpty()) {
                    Map<String, Object> firstResult = results.get(0);
                    Map<String, Object> geometry = (Map<String, Object>) firstResult.get("geometry");
                    Map<String, Object> location = (Map<String, Object>) geometry.get("location");

                    result.put("success", true);
                    result.put("latitude", location.get("lat"));
                    result.put("longitude", location.get("lng"));
                    result.put("formattedAddress", firstResult.get("formatted_address"));

                    logger.info("Successfully geocoded: {} -> {}, {}",
                            address, location.get("lat"), location.get("lng"));
                } else {
                    result.put("success", false);
                    result.put("message", "No results found for address");
                }
            } else {
                String status = responseBody != null ? (String) responseBody.get("status") : "UNKNOWN";
                logger.warn("Geocoding failed with status: {}", status);
                result.put("success", false);
                result.put("message", "Geocoding failed: " + status);
            }
        } catch (Exception e) {
            logger.error("Error geocoding address: " + address, e);
            result.put("success", false);
            result.put("message", "Error geocoding address: " + e.getMessage());
        }

        return result;
    }

    /**
     * Geocode a pincode to get latitude and longitude
     */
    public Map<String, Object> geocodePincode(String pincode, String country) {
        if (country == null || country.isEmpty()) {
            country = "India";
        }
        String address = pincode + ", " + country;
        return geocodeAddress(address);
    }

    /**
     * Get optimized route for multiple waypoints
     */
    public Map<String, Object> getOptimizedRoute(
            Double startLat, Double startLng,
            List<Map<String, Double>> waypoints) {

        Map<String, Object> result = new HashMap<>();

        if (apiKey == null || apiKey.isEmpty()) {
            result.put("success", false);
            result.put("message", "Google Maps API key not configured");
            return result;
        }

        if (waypoints == null || waypoints.isEmpty()) {
            result.put("success", false);
            result.put("message", "No waypoints provided");
            return result;
        }

        try {
            StringBuilder waypointsStr = new StringBuilder("optimize:true");
            for (Map<String, Double> wp : waypoints) {
                waypointsStr.append("|");
                waypointsStr.append(wp.get("lat")).append(",").append(wp.get("lng"));
            }

            String url = UriComponentsBuilder
                    .fromHttpUrl("https://maps.googleapis.com/maps/api/directions/json")
                    .queryParam("origin", startLat + "," + startLng)
                    .queryParam("destination", startLat + "," + startLng)
                    .queryParam("waypoints", waypointsStr.toString())
                    .queryParam("key", apiKey)
                    .toUriString();

            logger.info("Optimizing route with {} waypoints", waypoints.size());

            // Use the trust-all REST template
            RestTemplate restTemplate = createTrustAllRestTemplate();
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            Map<String, Object> responseBody = response.getBody();

            if (responseBody != null && "OK".equals(responseBody.get("status"))) {
                result.put("success", true);
                result.put("data", responseBody);

                List<Map<String, Object>> routes = (List<Map<String, Object>>) responseBody.get("routes");
                if (routes != null && !routes.isEmpty()) {
                    Map<String, Object> route = routes.get(0);
                    List<Integer> waypointOrder = (List<Integer>) route.get("waypoint_order");
                    logger.info("Route optimized successfully. Waypoint order: {}", waypointOrder);
                }
            } else {
                String status = responseBody != null ? (String) responseBody.get("status") : "UNKNOWN";
                logger.warn("Route optimization failed with status: {}", status);
                result.put("success", false);
                result.put("message", "Route optimization failed: " + status);
            }
        } catch (Exception e) {
            logger.error("Error optimizing route", e);
            result.put("success", false);
            result.put("message", "Error optimizing route: " + e.getMessage());
        }

        return result;
    }
}