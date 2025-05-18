package com.example.medicalregister.service;

import com.example.medicalregister.exception.RecordNotFoundException;
import com.example.medicalregister.model.MedicalRecord;
import com.example.medicalregister.repository.MedicalRecordRepository;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service layer for managing medical records. Handles business logic, data
 * access, and security checks related to medical records.
 */
@Service
@Transactional
public class MedicalRecordService {
    private static final Logger logger = LoggerFactory.getLogger(MedicalRecordService.class);

    private final MedicalRecordRepository medicalRecordRepository;

    /**
     * Constructs the service with the medical record repository.
     * 
     * @param medicalRecordRepository The repository for data access.
     */
    public MedicalRecordService(MedicalRecordRepository medicalRecordRepository) {
        this.medicalRecordRepository = medicalRecordRepository;
    }

    /**
     * Retrieves all medical records for the currently authenticated user.
     * 
     * @return A list of {@link MedicalRecord}s.
     * @throws AccessDeniedException if the user is not authenticated or lacks a
     *                               'sub' claim.
     */
    @Transactional(readOnly = true)
    public List<MedicalRecord> findAllRecords() {
        String ownerId = getCurrentUserSub();
        if (ownerId == null) {
            logger.warn("Attempt to find all records without authenticated user or user without 'sub' claim.");
            throw new AccessDeniedException("User must be authenticated with a 'sub' claim to view records.");
        }
        List<MedicalRecord> records = medicalRecordRepository.findByOwnerId(ownerId);
        logger.info("User {} retrieved {} medical records.", ownerId, records.size());
        return records;
    }

    /**
     * Finds a specific medical record by its ID for the currently authenticated
     * user.
     * 
     * @param id The ID of the record to find.
     * @return The {@link MedicalRecord}.
     * @throws RecordNotFoundException if the record is not found for the user.
     * @throws AccessDeniedException   if the user is not authenticated or lacks a
     *                                 'sub' claim.
     */
    @Transactional(readOnly = true)
    public MedicalRecord findRecordById(Long id) {
        String ownerId = getCurrentUserSub();
        if (ownerId == null) {
            logger.warn("Attempt to find record by id {} without authenticated user or user without 'sub' claim.", id);
            throw new AccessDeniedException("User must be authenticated with a 'sub' claim to view this record.");
        }
        MedicalRecord record = medicalRecordRepository.findByIdAndOwnerId(id, ownerId)
                .orElseThrow(() -> {
                    logger.warn("Record with ID {} not found for ownerId {}", id, ownerId);
                    return new RecordNotFoundException("Medical record not found.");
                });
        logger.info("User {} retrieved medical record with ID: {}.", ownerId, id);
        return record;
    }

    /**
     * Saves a medical record (creates new or updates existing).
     * Ensures the record is associated with the currently authenticated user.
     * 
     * @param record The {@link MedicalRecord} to save.
     * @return The saved {@link MedicalRecord}.
     * @throws AccessDeniedException if the user is not authenticated, lacks a 'sub'
     *                               claim, or attempts to update a record they
     *                               don't own.
     */
    public MedicalRecord saveRecord(MedicalRecord record) {
        String ownerId = getCurrentUserSub();
        if (ownerId == null) {
            logger.warn("Attempt to save record without authenticated user or user without 'sub' claim.");
            throw new AccessDeniedException("User must be authenticated with a 'sub' claim to save records.");
        }

        boolean isNewRecord = record.getId() == null;
        if (record.getId() != null) { // Existing record, check ownership
            if (!medicalRecordRepository.existsByIdAndOwnerId(record.getId(), ownerId)) {
                logger.warn("User {} attempted to update record {} they do not own.", ownerId, record.getId());
                throw new AccessDeniedException("You do not have permission to update this record.");
            }
        }
        record.setOwnerId(ownerId); // Ensure ownerId is set to the current authenticated user
        MedicalRecord savedRecord = medicalRecordRepository.save(record);
        if (isNewRecord) {
            logger.info("User {} created new medical record with ID: {}.", ownerId, savedRecord.getId());
        } else {
            logger.info("User {} updated medical record with ID: {}.", ownerId, savedRecord.getId());
        }
        return savedRecord;
    }

    /**
     * Deletes a medical record by its ID. Ensures the record belongs to the
     * currently authenticated user.
     * 
     * @param id The ID of the record to delete.
     * @throws RecordNotFoundException if the record does not exist.
     * @throws AccessDeniedException   if the user is not authenticated, lacks a
     *                                 'sub' claim, or does not own the record.
     */
    public void deleteRecordById(Long id) {
        String ownerId = getCurrentUserSub();
        if (ownerId == null) {
            logger.warn("Attempt to delete record {} by unauthenticated user or user without 'sub' claim.", id);
            throw new AccessDeniedException("User must be authenticated with a 'sub' claim to delete records.");
        }
        // First, check if the record exists at all using JpaRepository's existsById
        if (!medicalRecordRepository.existsById(id)) {
            logger.warn("Attempt to delete non-existent record with ID: {} by user {}", id, ownerId);
            throw new RecordNotFoundException("Medical record not found with ID: " + id);
        }

        // Then, check if the record belongs to the current user
        if (!medicalRecordRepository.existsByIdAndOwnerId(id, ownerId)) {
            logger.warn("User {} attempted to delete record {} they do not own.", ownerId, id);
            throw new AccessDeniedException("You do not have permission to delete this medical record.");
        }

        medicalRecordRepository.deleteById(id);
        logger.info("User {} successfully deleted medical record with ID: {}", ownerId, id);
    }

    /**
     * Helper method to retrieve the 'sub' (subject) claim of the currently
     * authenticated OAuth2User. This 'sub' claim is used as the unique owner
     * identifier for medical records.
     * 
     * @return The 'sub' claim string, or null if not available or user not
     *         authenticated.
     */
    private String getCurrentUserSub() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            logger.warn("Authentication object is null in SecurityContextHolder.");
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof OAuth2User) {
            OAuth2User oauth2User = (OAuth2User) principal;
            logger.debug("OAuth2User principal found. Attributes: {}", oauth2User.getAttributes());
            String sub = oauth2User.getAttribute("sub");
            if (sub == null) {
                logger.warn("'sub' attribute is null for OAuth2User. Available attributes: {}",
                        oauth2User.getAttributes().keySet());
            }
            return sub;
        } else {
            logger.warn(
                    "Could not get 'sub' claim, principal is not OAuth2User. Principal type: {}. Principal details: {}",
                    principal != null ? principal.getClass().getName() : "null", principal);
        }
        return null;
    }
}
