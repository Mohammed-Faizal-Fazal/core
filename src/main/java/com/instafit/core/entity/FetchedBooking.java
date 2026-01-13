package com.instafit.core.entity;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * FetchedBooking Entity - Prevents Duplicate Fetches
 */
@Entity
@Table(name = "fetched_bookings",
        uniqueConstraints = @UniqueConstraint(columnNames = "order_no"),
        indexes = {
                @Index(name = "idx_fetched_order_no", columnList = "order_no"),
                @Index(name = "idx_last_fetched", columnList = "last_fetched_at"),
                @Index(name = "idx_fetched_by", columnList = "fetched_by")
        }
)
public class FetchedBooking implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_no", nullable = false, unique = true, length = 50)
    private String orderNo;

    @Column(name = "booking_id")
    private Long bookingId;

    @Column(name = "first_fetched_at", nullable = false)
    private LocalDateTime firstFetchedAt;

    @Column(name = "last_fetched_at", nullable = false)
    private LocalDateTime lastFetchedAt;

    @Column(name = "fetch_count", nullable = false)
    private Integer fetchCount = 1;

    @Column(name = "fetched_by", length = 100)
    private String fetchedBy;

    @PrePersist
    protected void onCreate() {
        if (firstFetchedAt == null) {
            firstFetchedAt = LocalDateTime.now();
        }
        if (lastFetchedAt == null) {
            lastFetchedAt = LocalDateTime.now();
        }
    }

    // Constructors
    public FetchedBooking() {}

    public FetchedBooking(String orderNo, Long bookingId, String fetchedBy) {
        this.orderNo = orderNo;
        this.bookingId = bookingId;
        this.fetchedBy = fetchedBy;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }

    public Long getBookingId() { return bookingId; }
    public void setBookingId(Long bookingId) { this.bookingId = bookingId; }

    public LocalDateTime getFirstFetchedAt() { return firstFetchedAt; }
    public void setFirstFetchedAt(LocalDateTime firstFetchedAt) { this.firstFetchedAt = firstFetchedAt; }

    public LocalDateTime getLastFetchedAt() { return lastFetchedAt; }
    public void setLastFetchedAt(LocalDateTime lastFetchedAt) { this.lastFetchedAt = lastFetchedAt; }

    public Integer getFetchCount() { return fetchCount; }
    public void setFetchCount(Integer fetchCount) { this.fetchCount = fetchCount; }

    public String getFetchedBy() { return fetchedBy; }
    public void setFetchedBy(String fetchedBy) { this.fetchedBy = fetchedBy; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FetchedBooking that = (FetchedBooking) o;
        return Objects.equals(id, that.id) && Objects.equals(orderNo, that.orderNo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, orderNo);
    }

    @Override
    public String toString() {
        return "FetchedBooking{" +
                "id=" + id +
                ", orderNo='" + orderNo + '\'' +
                ", bookingId=" + bookingId +
                ", fetchCount=" + fetchCount +
                ", firstFetchedAt=" + firstFetchedAt +
                '}';
    }
}