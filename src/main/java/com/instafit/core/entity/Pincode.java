package com.instafit.core.entity;

import javax.persistence.*;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;

@Entity
@Table(name = "pincodes")
public class Pincode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Size(max = 10, message = "Pincode must not exceed 10 characters")
    @Column(name = "pincode", nullable = false, unique = true, length = 10)
    private String pincode;

    @Size(max = 20, message = "City code must not exceed 20 characters")
    @Column(name = "city_code", nullable = false, length = 20)
    private String cityCode;

    @Size(max = 200, message = "City description must not exceed 200 characters")
    @Column(name = "city_desc", length = 200)
    private String cityDesc;

    @Size(max = 200, message = "Area must not exceed 200 characters")
    @Column(name = "area", length = 200)
    private String area;

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

    public Pincode() {
    }

    public Pincode(String pincode, String cityCode) {
        this.pincode = pincode != null ? pincode.trim() : null;
        this.cityCode = cityCode != null ? cityCode.trim() : null;
        this.active = true;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPincode() {
        return pincode;
    }

    public void setPincode(String pincode) {
        this.pincode = pincode != null ? pincode.trim() : null;
    }

    public String getCityCode() {
        return cityCode;
    }

    public void setCityCode(String cityCode) {
        this.cityCode = cityCode != null ? cityCode.trim() : null;
    }

    public String getCityDesc() {
        return cityDesc;
    }

    public void setCityDesc(String cityDesc) {
        this.cityDesc = cityDesc != null ? cityDesc.trim() : null;
    }

    public String getArea() {
        return area;
    }

    public void setArea(String area) {
        this.area = area != null ? area.trim() : null;
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
}