package com.instafit.core.repository;

import com.instafit.core.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Booking Repository
 * Database operations for Booking entity
 */
@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    // Find by status
    List<Booking> findByStatus(String status);

    // Find by order number
    Optional<Booking> findByOrderNo(String orderNo);

    // Find by customer mobile
    List<Booking> findByCustomerMobile(String customerMobile);

    // Find by date range
    List<Booking> findByDateBetween(LocalDate startDate, LocalDate endDate);

    // Find by customer name (case insensitive)
    @Query("SELECT b FROM Booking b WHERE LOWER(b.customerName) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<Booking> findByCustomerNameContainingIgnoreCase(@Param("name") String name);

    // Find by service name
    @Query("SELECT b FROM Booking b WHERE LOWER(b.serviceName) LIKE LOWER(CONCAT('%', :serviceName, '%'))")
    List<Booking> findByServiceNameContainingIgnoreCase(@Param("serviceName") String serviceName);

    // Count by status
    @Query("SELECT COUNT(b) FROM Booking b WHERE b.status = :status")
    long countByStatus(@Param("status") String status);

    // Find submitted bookings
    @Query("SELECT b FROM Booking b WHERE b.submittedBy IS NOT NULL ORDER BY b.submittedAt DESC")
    List<Booking> findAllSubmittedBookings();

    List<Booking> findByAssignmentStatus(String status);
    List<Booking> findByCarpenterIdAndAssignedDate(String carpenterId, LocalDate assignedDate);
    List<Booking> findByCarpenterId(String carpenterId);

    @Query("SELECT b FROM Booking b WHERE b.assignmentStatus = 'SUBMITTED' ORDER BY b.createdAt DESC")
    List<Booking> findSubmittedOrders();
    List<Booking> findByCarpenterIdIsNotNull();
}