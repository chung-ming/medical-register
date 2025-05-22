package com.example.medicalregister.controller;

import com.example.medicalregister.model.MedicalRecord;
import com.example.medicalregister.service.MedicalRecordService;

import jakarta.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * RESTful API controller for managing medical records. Handles CRUD operations
 * via HTTP, returning JSON responses. All endpoints are relative to
 * /api/v1/records.
 */
@RestController
@RequestMapping("/api/v1/records")
public class MedicalRecordApiController {

    private static final Logger logger = LoggerFactory.getLogger(MedicalRecordApiController.class);
    private final MedicalRecordService recordService;

    public MedicalRecordApiController(MedicalRecordService recordService) {
        this.recordService = recordService;
    }

    private String getUserName(OAuth2User principal) {
        if (principal == null) {
            return "Unknown User";
        }
        return principal.getAttribute("name") != null ? principal.getAttribute("name") : "User";
    }

    /**
     * Retrieves all medical records for the authenticated user.
     * GET /api/v1/records
     *
     * @param principal The authenticated OAuth2User.
     * @return A list of medical records.
     */
    @GetMapping
    public ResponseEntity<List<MedicalRecord>> listRecords(@AuthenticationPrincipal OAuth2User principal) {
        String userName = getUserName(principal);
        logger.info("API: User {} attempting to list records.", userName);
        List<MedicalRecord> records = recordService.findAllRecords(); // Assumes service handles auth
        return ResponseEntity.ok(records);
    }

    /**
     * Creates a new medical record.
     * POST /api/v1/records
     *
     * @param medicalRecord The medical record data from the request body.
     * @param principal     The authenticated OAuth2User.
     * @return ResponseEntity with the created record and 201 status, or error
     *         status.
     */
    @PostMapping
    public ResponseEntity<MedicalRecord> createRecord(@Valid @RequestBody MedicalRecord medicalRecord,
            @AuthenticationPrincipal OAuth2User principal) {
        String userName = getUserName(principal);
        logger.info("API: User {} attempting to create a new record.", userName);
        MedicalRecord savedRecord = recordService.saveRecord(medicalRecord); // Assumes service sets ownerId
        logger.info("API: User {} created a record with ID: {}.", userName, savedRecord.getId());

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(savedRecord.getId())
                .toUri();
        return ResponseEntity.created(location).body(savedRecord);
    }

    /**
     * Retrieves a specific medical record by its ID.
     * GET /api/v1/records/{id}
     *
     * @param id        The ID of the record to retrieve.
     * @param principal The authenticated OAuth2User.
     * @return ResponseEntity with the record or 404 if not found.
     */
    @GetMapping("/{id}")
    public ResponseEntity<MedicalRecord> getRecordById(@PathVariable Long id,
            @AuthenticationPrincipal OAuth2User principal) {
        String userName = getUserName(principal);
        logger.info("API: User {} attempting to retrieve record ID: {}.", userName, id);
        MedicalRecord record = recordService.findRecordById(id); // Throws RecordNotFound or AccessDenied
        return ResponseEntity.ok(record);
    }

    /**
     * Updates an existing medical record.
     * PUT /api/v1/records/{id}
     *
     * @param id            The ID of the record to update.
     * @param recordDetails The updated medical record data.
     * @param principal     The authenticated OAuth2User.
     * @return ResponseEntity with the updated record or error status.
     */
    @PutMapping("/{id}")
    public ResponseEntity<MedicalRecord> updateRecord(@PathVariable Long id,
            @Valid @RequestBody MedicalRecord recordDetails,
            @AuthenticationPrincipal OAuth2User principal) {
        String userName = getUserName(principal);
        logger.info("API: User {} attempting to update record ID: {}.", userName, id);
        // Ensure the ID in the path matches the ID in the body, or set it.
        // The service layer should verify ownership and existence.
        recordDetails.setId(id); // Make sure ID is consistent for the update
        MedicalRecord updatedRecord = recordService.saveRecord(recordDetails);
        logger.info("API: User {} updated record ID: {}.", userName, updatedRecord.getId());
        return ResponseEntity.ok(updatedRecord);
    }

    /**
     * Deletes a medical record by its ID.
     * DELETE /api/v1/records/{id}
     *
     * @param id        The ID of the record to delete.
     * @param principal The authenticated OAuth2User.
     * @return ResponseEntity with 204 No Content or error status.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRecord(@PathVariable Long id,
            @AuthenticationPrincipal OAuth2User principal) {
        String userName = getUserName(principal);
        logger.info("API: User {} attempting to delete record ID: {}.", userName, id);
        recordService.deleteRecordById(id); // Throws RecordNotFound or AccessDenied
        logger.info("API: User {} successfully deleted record ID: {}.", userName, id);
        return ResponseEntity.noContent().build();
    }
}
