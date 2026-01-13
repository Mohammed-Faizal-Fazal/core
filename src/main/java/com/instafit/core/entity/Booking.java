package com.instafit.core.entity;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Booking Entity
 */
@Entity
@Table(name = "bookings",
        indexes = {
                @Index(name = "idx_order_no", columnList = "order_no"),
                @Index(name = "idx_status", columnList = "status"),
                @Index(name = "idx_date", columnList = "date"),
                @Index(name = "idx_customer_mobile", columnList = "customer_mobile"),
                @Index(name = "idx_user_id", columnList = "user_id"),
                @Index(name = "idx_created_at", columnList = "created_at"),
                @Index(name = "idx_submitted_at", columnList = "submitted_at")
        }
)
public class Booking implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_no", unique = true, length = 50)
    private String orderNo;

    @Column(name = "user_id", length = 100)
    private String userId;

    @Column(name = "customer_name", length = 200)
    private String customerName;

    @Column(name = "customer_mobile", length = 20)
    private String customerMobile;

    @Column(name = "date")
    private LocalDate date;

    @Column(name = "booking_time")
    private LocalTime bookingTime;

    @Column(name = "service_name", length = 200)
    private String serviceName;

    @Column(name = "service_id")
    private Integer serviceId;

    @Column(name = "service_types", columnDefinition = "TEXT")
    private String serviceTypes;

    @Column(name = "total_price", precision = 10, scale = 2)
    private BigDecimal totalPrice;

    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    @Column(name = "status", length = 50)
    private String status;

    @Column(name = "payment_id", length = 100)
    private String paymentId;

    @Column(name = "employee_name", length = 200)
    private String employeeName;

    @Column(name = "employee_phone", length = 20)
    private String employeePhone;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "submitted_by", length = 100)
    private String submittedBy;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    // Add these fields to existing Booking entity

    @Column(name = "carpenter_id", length = 20)
    private String carpenterId;

    @Column(name = "carpenter_name", length = 100)
    private String carpenterName;

    @Column(name = "assigned_date")
    private LocalDate assignedDate;

    @Column(name = "assignment_status", length = 20)
    private String assignmentStatus = "SUBMITTED";

    @Column(name = "latitude", precision = 10, scale = 8)
    private Double latitude;

    @Column(name = "longitude", precision = 11, scale = 8)
    private Double longitude;

    @Column(name = "geocode_status", length = 20)
    private String geocodeStatus = "PENDING";

    @Column(name = "route_order")
    private Integer routeOrder;



    public String getCarpenterId() {
        return carpenterId;
    }

    public void setCarpenterId(String carpenterId) {
        this.carpenterId = carpenterId;
    }

    public String getCarpenterName() {
        return carpenterName;
    }

    public void setCarpenterName(String carpenterName) {
        this.carpenterName = carpenterName;
    }

    public LocalDate getAssignedDate() {
        return assignedDate;
    }

    public void setAssignedDate(LocalDate assignedDate) {
        this.assignedDate = assignedDate;
    }

    public String getAssignmentStatus() {
        return assignmentStatus;
    }

    public void setAssignmentStatus(String assignmentStatus) {
        this.assignmentStatus = assignmentStatus;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public String getGeocodeStatus() {
        return geocodeStatus;
    }

    public void setGeocodeStatus(String geocodeStatus) {
        this.geocodeStatus = geocodeStatus;
    }

    public Integer getRouteOrder() {
        return routeOrder;
    }

    public void setRouteOrder(Integer routeOrder) {
        this.routeOrder = routeOrder;
    }

    // Constructors
    public Booking() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getCustomerMobile() { return customerMobile; }
    public void setCustomerMobile(String customerMobile) { this.customerMobile = customerMobile; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public LocalTime getBookingTime() { return bookingTime; }
    public void setBookingTime(LocalTime bookingTime) { this.bookingTime = bookingTime; }

    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }

    public Integer getServiceId() { return serviceId; }
    public void setServiceId(Integer serviceId) { this.serviceId = serviceId; }

    public String getServiceTypes() { return serviceTypes; }
    public void setServiceTypes(String serviceTypes) { this.serviceTypes = serviceTypes; }

    public BigDecimal getTotalPrice() { return totalPrice; }
    public void setTotalPrice(BigDecimal totalPrice) { this.totalPrice = totalPrice; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getPaymentId() { return paymentId; }
    public void setPaymentId(String paymentId) { this.paymentId = paymentId; }

    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }

    public String getEmployeePhone() { return employeePhone; }
    public void setEmployeePhone(String employeePhone) { this.employeePhone = employeePhone; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getSubmittedBy() { return submittedBy; }
    public void setSubmittedBy(String submittedBy) { this.submittedBy = submittedBy; }

    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Booking booking = (Booking) o;
        return Objects.equals(id, booking.id) && Objects.equals(orderNo, booking.orderNo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, orderNo);
    }

    @Override
    public String toString() {
        return "Booking{" +
                "id=" + id +
                ", orderNo='" + orderNo + '\'' +
                ", customerName='" + customerName + '\'' +
                ", status='" + status + '\'' +
                ", date=" + date +
                '}';
    }
}