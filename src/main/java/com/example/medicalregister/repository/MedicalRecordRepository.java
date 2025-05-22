package com.example.medicalregister.repository;

import com.example.medicalregister.model.MedicalRecord;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for {@link MedicalRecord} entities. Provides CRUD
 * operations and custom finder methods.
 */
@Repository
public interface MedicalRecordRepository extends JpaRepository<MedicalRecord, Long> {
    /**
     * Finds a page of medical records owned by a specific user.
     * 
     * @param ownerId  The identifier of the owner.
     * @param pageable Pagination and sorting information.
     * @return A {@link Page} of medical records.
     */
    Page<MedicalRecord> findByOwnerId(String ownerId, Pageable pageable);

    /**
     * Finds a specific medical record by its ID, only if it's owned by the
     * specified user.
     * 
     * @param id      The ID of the medical record.
     * @param ownerId The identifier of the owner.
     * @return An {@link Optional} containing the medical record if found and owned,
     *         otherwise empty.
     */
    Optional<MedicalRecord> findByIdAndOwnerId(Long id, String ownerId);

    /**
     * Checks if a medical record exists with the given ID and is owned by the
     * specified user.
     * 
     * @param id      The ID of the medical record.
     * @param ownerId The identifier of the owner.
     * @return True if such a record exists, false otherwise.
     */
    boolean existsByIdAndOwnerId(Long id, String ownerId);
}
