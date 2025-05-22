package com.example.medicalregister.repository;

import com.example.medicalregister.model.MedicalRecord;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test") // Use the test profile to avoid using production database
@DisplayName("MedicalRecordRepository Tests")
/**
 * Unit tests for the {@link MedicalRecordRepository}. This class tests the
 * repository methods to ensure they work as expected.
 */
class MedicalRecordRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private MedicalRecordRepository medicalRecordRepository;

    private final String ownerId1 = "user-sub-1";
    private final String ownerId2 = "user-sub-2";

    private MedicalRecord record1Owner1;
    private MedicalRecord record2Owner1;
    private MedicalRecord record1Owner2;

    @BeforeEach
    void setUp() {
        // @DataJpaTest manages transactions and rolls back changes after each test.

        record1Owner1 = new MedicalRecord(null, "Patient A", 30, "History A", ownerId1, "Test User", "Test User", null,
                null, false);
        record2Owner1 = new MedicalRecord(null, "Patient B", 40, "History B", ownerId1, "Test User", "Test User", null,
                null, false);
        record1Owner2 = new MedicalRecord(null, "Patient C", 50, "History C", ownerId2, "Test User", "Test User", null,
                null, false);

        entityManager.persist(record1Owner1);
        entityManager.persist(record2Owner1);
        entityManager.persist(record1Owner2);
        entityManager.flush(); // Ensure data is written to DB before queries
    }

    @Test
    @DisplayName("findByOwnerId should return records for the given owner")
    void findByOwnerId_whenRecordsExistForOwner_shouldReturnRecords() {
        List<MedicalRecord> records = medicalRecordRepository.findByOwnerId(ownerId1);
        assertThat(records).hasSize(2).extracting(MedicalRecord::getName)
                .containsExactlyInAnyOrder("Patient A", "Patient B");
    }

    @Test
    @DisplayName("findByOwnerId should return empty list if no records for the owner")
    void findByOwnerId_whenNoRecordsForOwner_shouldReturnEmptyList() {
        List<MedicalRecord> records = medicalRecordRepository.findByOwnerId("non-existent-owner");
        assertThat(records).isEmpty();
    }

    @Test
    @DisplayName("findByIdAndOwnerId should return record if ID and owner match")
    void findByIdAndOwnerId_whenRecordExistsAndOwned_shouldReturnRecord() {
        Optional<MedicalRecord> foundRecord = medicalRecordRepository.findByIdAndOwnerId(record1Owner1.getId(),
                ownerId1);
        assertThat(foundRecord).isPresent();
        assertThat(foundRecord.get().getName()).isEqualTo("Patient A");
    }

    @Test
    @DisplayName("findByIdAndOwnerId should return empty if ID does not match owner")
    void findByIdAndOwnerId_whenRecordExistsButNotOwned_shouldReturnEmpty() {
        Optional<MedicalRecord> foundRecord = medicalRecordRepository.findByIdAndOwnerId(record1Owner1.getId(),
                ownerId2);
        assertThat(foundRecord).isNotPresent();
    }

    @Test
    @DisplayName("findByIdAndOwnerId should return empty if ID does not exist")
    void findByIdAndOwnerId_whenRecordDoesNotExist_shouldReturnEmpty() {
        Optional<MedicalRecord> foundRecord = medicalRecordRepository.findByIdAndOwnerId(999L, ownerId1);
        assertThat(foundRecord).isNotPresent();
    }

    @Test
    @DisplayName("existsByIdAndOwnerId should return true if record exists and is owned")
    void existsByIdAndOwnerId_whenRecordExistsAndOwned_shouldReturnTrue() {
        boolean exists = medicalRecordRepository.existsByIdAndOwnerId(record1Owner1.getId(), ownerId1);
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("existsByIdAndOwnerId should return false if record exists but not owned")
    void existsByIdAndOwnerId_whenRecordExistsButNotOwned_shouldReturnFalse() {
        boolean exists = medicalRecordRepository.existsByIdAndOwnerId(record1Owner1.getId(), ownerId2);
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("existsByIdAndOwnerId should return false if record does not exist")
    void existsByIdAndOwnerId_whenRecordDoesNotExist_shouldReturnFalse() {
        boolean exists = medicalRecordRepository.existsByIdAndOwnerId(999L, ownerId1);
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("save should persist a new medical record")
    void save_shouldPersistNewRecord() {
        MedicalRecord newRecord = new MedicalRecord(null, "New Patient", 25, "New History", "new-owner", "Test User",
                "Test User",
                null, null, false);
        MedicalRecord savedRecord = medicalRecordRepository.save(newRecord);

        assertThat(savedRecord.getId()).isNotNull();
        Optional<MedicalRecord> found = medicalRecordRepository.findById(savedRecord.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("New Patient");
    }
}
