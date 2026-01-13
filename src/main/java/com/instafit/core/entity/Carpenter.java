package com.instafit.core.entity;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "carpenters",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_carpenter_id", columnNames = "carpenter_id"),
                @UniqueConstraint(name = "uk_carpenter_mobile", columnNames = "mobile")
        },
        indexes = {
                @Index(name = "idx_carpenter_id", columnList = "carpenter_id"),
                @Index(name = "idx_carpenter_mobile", columnList = "mobile"),
                @Index(name = "idx_carpenter_city", columnList = "city_code")
        })
public class Carpenter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "carpenter_id", nullable = false, unique = true, length = 20)
    private String carpenterId;

    @Column(name = "carpenter_name", nullable = false, length = 100)
    private String carpenterName;

    @Column(name = "mobile", nullable = false, unique = true, length = 20)
    private String mobile;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "job_type", length = 50)
    private String jobType;

    @Column(name = "city_code", length = 10)
    private String cityCode;

    @Column(name = "city_desc", length = 100)
    private String cityDesc;

    @Column(name = "branch_code", length = 10)
    private String branchCode;

    @Column(name = "pincode", length = 10)
    private String pincode;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @Column(name = "created_by", length = 50)
    private String createdBy;

    @Column(name = "updated_by", length = 50)
    private String updatedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Constructors
    public Carpenter() {
    }

    public Carpenter(String carpenterId, String carpenterName, String mobile) {
        this.carpenterId = carpenterId;
        this.carpenterName = carpenterName;
        this.mobile = mobile;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getJobType() {
        return jobType;
    }

    public void setJobType(String jobType) {
        this.jobType = jobType;
    }

    public String getCityCode() {
        return cityCode;
    }

    public void setCityCode(String cityCode) {
        this.cityCode = cityCode;
    }

    public String getCityDesc() {
        return cityDesc;
    }

    public void setCityDesc(String cityDesc) {
        this.cityDesc = cityDesc;
    }

    public String getBranchCode() {
        return branchCode;
    }

    public void setBranchCode(String branchCode) {
        this.branchCode = branchCode;
    }

    public String getPincode() {
        return pincode;
    }

    public void setPincode(String pincode) {
        this.pincode = pincode;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Carpenter carpenter = (Carpenter) o;
        return Objects.equals(id, carpenter.id) &&
                Objects.equals(carpenterId, carpenter.carpenterId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, carpenterId);
    }

    @Override
    public String toString() {
        return "Carpenter{" +
                "id=" + id +
                ", carpenterId='" + carpenterId + '\'' +
                ", carpenterName='" + carpenterName + '\'' +
                ", mobile='" + mobile + '\'' +
                ", pincode='" + pincode + '\'' +
                ", active=" + active +
                '}';
    }
}