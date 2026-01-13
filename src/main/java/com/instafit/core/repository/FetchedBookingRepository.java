package com.instafit.core.repository;

import com.instafit.core.entity.FetchedBooking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * FetchedBooking Repository
 * Tracks which bookings have been fetched from Supabase
 */
@Repository
public interface FetchedBookingRepository extends JpaRepository<FetchedBooking, Long> {

    // Find by order number
    Optional<FetchedBooking> findByOrderNo(String orderNo);

    // Check if order has been fetched
    boolean existsByOrderNo(String orderNo);

    // Find all fetched bookings ordered by fetch date
    @Query("SELECT fb FROM FetchedBooking fb ORDER BY fb.lastFetchedAt DESC")
    List<FetchedBooking> findAllOrderByLastFetchedAtDesc();

    // Count total fetched bookings
    @Query("SELECT COUNT(fb) FROM FetchedBooking fb")
    long countTotalFetched();
}