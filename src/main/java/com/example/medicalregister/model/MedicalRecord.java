package com.example.medicalregister.model;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Represents a medical record entity. Includes JPA auditing for
 * creation/modification tracking.
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@SQLDelete(sql = "UPDATE medical_record SET deleted = true WHERE id = ?") // Soft delete
@SQLRestriction("deleted = false") // Restrict queries to non-deleted records
@JsonPropertyOrder({ "id", "name", "age", "medicalHistory", "deleted", "ownerId", "createdBy", "lastModifiedBy",
        "createdAt", "updatedAt" })
public class MedicalRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Name is mandatory")
    private String name;

    @NotNull(message = "Age is mandatory")
    @Min(value = 0, message = "Age must be positive")
    private Integer age;

    @Column(columnDefinition = "TEXT")
    @NotBlank(message = "Medical history is mandatory")
    private String medicalHistory;
    /**
     * Identifier of the user who owns this record (typically Auth0 'sub' claim).
     * Used by {@link com.example.medicalregister.service.MedicalRecordService}
     * to enforce record isolation and access control.
     */
    private String ownerId;

    @CreatedBy
    @Column(nullable = false, updatable = false)
    private String createdBy;

    @LastModifiedBy
    @Column(nullable = false)
    private String lastModifiedBy;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private boolean deleted = false; // Flag for soft delete
}
