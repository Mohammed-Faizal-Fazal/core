package com.instafit.core.entity;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * BookingLog Entity - Audit Trail
 */
@Entity
@Table(name = "booking_logs",
        indexes = {
                @Index(name = "idx_booking_id", columnList = "booking_id"),
                @Index(name = "idx_order_no", columnList = "order_no"),
                @Index(name = "idx_action_type", columnList = "action_type"),
                @Index(name = "idx_changed_by", columnList = "changed_by"),
                @Index(name = "idx_created_at", columnList = "created_at")
        }
)
public class BookingLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "booking_id", nullable = false)
    private Long bookingId;

    @Column(name = "order_no", length = 50)
    private String orderNo;

    @Column(name = "action_type", nullable = false, length = 50)
    private String actionType;

    @Column(name = "changed_by", nullable = false, length = 100)
    private String changedBy;

    @Column(name = "old_value", columnDefinition = "TEXT")
    private String oldValue;

    @Column(name = "new_value", columnDefinition = "TEXT")
    private String newValue;

    @Column(name = "field_changed", length = 100)
    private String fieldChanged;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    // Constructors
    public BookingLog() {}

    public BookingLog(Long bookingId, String orderNo, String actionType, String changedBy) {
        this.bookingId = bookingId;
        this.orderNo = orderNo;
        this.actionType = actionType;
        this.changedBy = changedBy;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getBookingId() { return bookingId; }
    public void setBookingId(Long bookingId) { this.bookingId = bookingId; }

    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }

    public String getActionType() { return actionType; }
    public void setActionType(String actionType) { this.actionType = actionType; }

    public String getChangedBy() { return changedBy; }
    public void setChangedBy(String changedBy) { this.changedBy = changedBy; }

    public String getOldValue() { return oldValue; }
    public void setOldValue(String oldValue) { this.oldValue = oldValue; }

    public String getNewValue() { return newValue; }
    public void setNewValue(String newValue) { this.newValue = newValue; }

    public String getFieldChanged() { return fieldChanged; }
    public void setFieldChanged(String fieldChanged) { this.fieldChanged = fieldChanged; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BookingLog that = (BookingLog) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "BookingLog{" +
                "id=" + id +
                ", bookingId=" + bookingId +
                ", orderNo='" + orderNo + '\'' +
                ", actionType='" + actionType + '\'' +
                ", changedBy='" + changedBy + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}