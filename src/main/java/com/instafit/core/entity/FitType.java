package com.instafit.core.entity;

import javax.persistence.*;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;

@Entity
@Table(name = "fit_types")
public class FitType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Size(max = 20, message = "Fit type code must not exceed 20 characters")
    @Column(name = "fit_type_code", nullable = false, unique = true, length = 20)
    private String fitTypeCode;

    @Size(max = 200, message = "Fit type description must not exceed 200 characters")
    @Column(name = "fit_type_desc", nullable = false, length = 200)
    private String fitTypeDesc;

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

    public FitType() {
    }

    public FitType(String fitTypeCode, String fitTypeDesc) {
        this.fitTypeCode = fitTypeCode != null ? fitTypeCode.trim() : null;
        this.fitTypeDesc = fitTypeDesc != null ? fitTypeDesc.trim() : null;
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

    public String getFitTypeCode() {
        return fitTypeCode;
    }

    public void setFitTypeCode(String fitTypeCode) {
        this.fitTypeCode = fitTypeCode != null ? fitTypeCode.trim() : null;
    }

    public String getFitTypeDesc() {
        return fitTypeDesc;
    }

    public void setFitTypeDesc(String fitTypeDesc) {
        this.fitTypeDesc = fitTypeDesc != null ? fitTypeDesc.trim() : null;
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