package com.instafit.core.repository;

import com.instafit.core.entity.BookingLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * BookingLog Repository
 * Database operations for BookingLog entity (Audit Trail)
 */
@Repository
public interface BookingLogRepository extends JpaRepository<BookingLog, Long> {

    // Find logs by booking ID, ordered by most recent first
    List<BookingLog> findByBookingIdOrderByCreatedAtDesc(Long bookingId);

    // Find logs by order number
    List<BookingLog> findByOrderNoOrderByCreatedAtDesc(String orderNo);

    // Find logs by user who made changes
    List<BookingLog> findByChangedBy(String changedBy);

    // Find logs by action type
    @Query("SELECT bl FROM BookingLog bl WHERE bl.actionType = :actionType ORDER BY bl.createdAt DESC")
    List<BookingLog> findByActionType(@Param("actionType") String actionType);

    // Count updates for a specific booking
    @Query("SELECT COUNT(bl) FROM BookingLog bl WHERE bl.bookingId = :bookingId AND bl.actionType = 'UPDATED'")
    long countUpdatesForBooking(@Param("bookingId") Long bookingId);

    // Find logs within date range
    @Query("SELECT bl FROM BookingLog bl WHERE bl.createdAt BETWEEN :startDate AND :endDate ORDER BY bl.createdAt DESC")
    List<BookingLog> findLogsBetweenDates(@Param("startDate") LocalDateTime startDate,
                                          @Param("endDate") LocalDateTime endDate);

    // Find all logs for a user
    @Query("SELECT bl FROM BookingLog bl WHERE bl.changedBy = :username ORDER BY bl.createdAt DESC")
    List<BookingLog> findAllLogsByUser(@Param("username") String username);
}