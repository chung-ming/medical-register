package com.example.medicalregister.service;

import com.example.medicalregister.exception.RecordNotFoundException;
import com.example.medicalregister.model.MedicalRecord;
import com.example.medicalregister.repository.MedicalRecordRepository;
import com.example.medicalregister.util.SecurityTestUtils;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@SpringBootTest
@Transactional // Ensures each test method runs in a transaction and rolls back
@ActiveProfiles("test") // Use the profile in application-test.properties for integration tests
class MedicalRecordServiceIntegrationTest {

        @Autowired
        private MedicalRecordService medicalRecordService;

        @Autowired
        private MedicalRecordRepository medicalRecordRepository;

        private MockedStatic<SecurityContextHolder> mockedSecurityContextHolder;
        private SecurityContext securityContext;
        private Authentication authentication;

        private final String USER_SUB_1 = "Test Sub 1";
        private final String USER_SUB_2 = "Test Sub 2";
        private final String USER_NAME_1 = "Test User 1";

        @BeforeEach
        void setUp() {
                // Mock SecurityContextHolder to control the authenticated principal
                securityContext = Mockito.mock(SecurityContext.class);
                authentication = Mockito.mock(Authentication.class);
                mockedSecurityContextHolder = Mockito.mockStatic(SecurityContextHolder.class);
                mockedSecurityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);
                when(securityContext.getAuthentication()).thenReturn(authentication);

                // Clean up repository before each test for good measure, though @Transactional
                // helps
                medicalRecordRepository.deleteAll();
        }

        @AfterEach
        void tearDown() {
                mockedSecurityContextHolder.close();
        }

        private void mockAuthenticatedUser(String sub, String name) {
                // Use SecurityTestUtils, 'sub' is the sub attribute here
                OAuth2User principal = SecurityTestUtils.createOAuth2UserWithSubAndName(sub, name);
                when(authentication.getPrincipal()).thenReturn(principal);
                when(authentication.isAuthenticated()).thenReturn(true); // Ensure isAuthenticated is true
        }

        private void mockAuthenticatedUserWithoutSub(String name) {
                // Use SecurityTestUtils, 'name' is the nameAttributeKey here
                OAuth2User principal = SecurityTestUtils.createOAuth2User(
                                Map.of("name", name), // No 'sub' attribute
                                "name");
                when(authentication.getPrincipal()).thenReturn(principal);
                when(authentication.isAuthenticated()).thenReturn(true);
        }

        @Test
        @DisplayName("findAllRecords should return records for the authenticated user")
        void findAllRecords_whenUserHasRecords_shouldReturnThem() {
                mockAuthenticatedUser(USER_SUB_1, USER_NAME_1);
                medicalRecordRepository
                                .save(new MedicalRecord(null, "Record 1", 30, "History 1", USER_SUB_1, null, null,
                                                null,
                                                null, false));
                medicalRecordRepository
                                .save(new MedicalRecord(null, "Record 2", 40, "History 2", USER_SUB_1, null, null,
                                                null,
                                                null, false));
                medicalRecordRepository.save(
                                new MedicalRecord(null, "Other User Record", 50, "History 3", USER_SUB_2, null, null,
                                                null, null, false));

                List<MedicalRecord> records = medicalRecordService.findAllRecords();

                assertThat(records).hasSize(2);
                assertThat(records).extracting(MedicalRecord::getOwnerId).containsOnly(USER_SUB_1);
        }

        @Test
        @DisplayName("findAllRecords should throw AccessDeniedException if user has no 'sub' claim")
        void findAllRecords_whenUserHasNoSub_shouldThrowAccessDenied() {
                mockAuthenticatedUserWithoutSub(USER_NAME_1);
                assertThatThrownBy(() -> medicalRecordService.findAllRecords())
                                .isInstanceOf(AccessDeniedException.class)
                                .hasMessageContaining("User must be authenticated with a 'sub' claim");
        }

        @Test
        @DisplayName("findRecordById should return record if owned by user")
        void findRecordById_whenOwned_shouldReturnRecord() {
                mockAuthenticatedUser(USER_SUB_1, USER_NAME_1);
                MedicalRecord savedRecord = medicalRecordRepository
                                .save(new MedicalRecord(null, "Owned Record", 30, "No history", USER_SUB_1, null, null,
                                                null, null, false));

                MedicalRecord foundRecord = medicalRecordService.findRecordById(savedRecord.getId());

                assertThat(foundRecord).isNotNull();
                assertThat(foundRecord.getId()).isEqualTo(savedRecord.getId());
                assertThat(foundRecord.getName()).isEqualTo("Owned Record");
        }

        @Test
        @DisplayName("findRecordById should throw RecordNotFoundException if not owned by user")
        void findRecordById_whenNotOwned_shouldThrowRecordNotFound() {
                mockAuthenticatedUser(USER_SUB_1, USER_NAME_1); // User 1 is authenticated
                MedicalRecord otherUserRecord = medicalRecordRepository
                                .save(new MedicalRecord(null, "Other's Record", 30, "No history", USER_SUB_2, null,
                                                null, null, null, false)); // Record
                                                                           // owned
                                                                           // by
                                                                           // User
                                                                           // 2

                assertThatThrownBy(() -> medicalRecordService.findRecordById(otherUserRecord.getId()))
                                .isInstanceOf(RecordNotFoundException.class)
                                .hasMessageContaining("Medical record not found");
        }

        @Test
        @DisplayName("saveRecord should create new record with current user as owner")
        void saveRecord_forNewRecord_shouldSetOwnerAndPersist() {
                mockAuthenticatedUser(USER_SUB_1, USER_NAME_1);
                MedicalRecord newRecord = new MedicalRecord(null, "New Patient", 25, "Initial checkup", null, null,
                                null, null,
                                null, false);

                MedicalRecord savedRecord = medicalRecordService.saveRecord(newRecord);

                assertThat(savedRecord.getId()).isNotNull();
                assertThat(savedRecord.getOwnerId()).isEqualTo(USER_SUB_1);
                assertThat(savedRecord.getName()).isEqualTo("New Patient");

                Optional<MedicalRecord> dbRecord = medicalRecordRepository.findById(savedRecord.getId());
                assertThat(dbRecord).isPresent();
                assertThat(dbRecord.get().getOwnerId()).isEqualTo(USER_SUB_1);
        }

        @Test
        @DisplayName("saveRecord should update existing record if owned by user")
        void saveRecord_forExistingOwnedRecord_shouldUpdate() {
                mockAuthenticatedUser(USER_SUB_1, USER_NAME_1);
                MedicalRecord originalRecord = medicalRecordRepository.save(
                                new MedicalRecord(null, "Original Name", 30, "Original History", USER_SUB_1, null, null,
                                                null, null, false));

                MedicalRecord recordToUpdate = new MedicalRecord(originalRecord.getId(), "Updated Name", 31,
                                "Updated History",
                                USER_SUB_1, null, null, null, null, false);
                MedicalRecord updatedRecord = medicalRecordService.saveRecord(recordToUpdate);

                assertThat(updatedRecord.getName()).isEqualTo("Updated Name");
                assertThat(updatedRecord.getAge()).isEqualTo(31);

                Optional<MedicalRecord> dbRecord = medicalRecordRepository.findById(originalRecord.getId());
                assertThat(dbRecord).isPresent();
                assertThat(dbRecord.get().getName()).isEqualTo("Updated Name");
        }

        @Test
        @DisplayName("saveRecord should throw AccessDeniedException when trying to update unowned record")
        void saveRecord_forExistingUnownedRecord_shouldThrowAccessDenied() {
                mockAuthenticatedUser(USER_SUB_1, USER_NAME_1); // User 1 is authenticated
                MedicalRecord otherUserRecord = medicalRecordRepository.save(
                                new MedicalRecord(null, "Unowned Record", 40, "Belongs to other", USER_SUB_2, null,
                                                null, null, null, false)); // Owned
                                                                           // by
                                                                           // User
                                                                           // 2

                MedicalRecord recordToUpdate = new MedicalRecord(otherUserRecord.getId(), "Attempted Update", 41,
                                "Attempted History", USER_SUB_1, null, null, null, null, false);

                assertThatThrownBy(() -> medicalRecordService.saveRecord(recordToUpdate))
                                .isInstanceOf(AccessDeniedException.class)
                                .hasMessageContaining("You do not have permission to update this record.");
        }

        @Test
        @DisplayName("deleteRecordById should delete owned record")
        void deleteRecordById_whenOwned_shouldDelete() {
                mockAuthenticatedUser(USER_SUB_1, USER_NAME_1);
                MedicalRecord recordToDelete = medicalRecordRepository
                                .save(new MedicalRecord(null, "To Be Deleted", 50, "Delete Me", USER_SUB_1, null, null,
                                                null, null, false));
                Long recordId = recordToDelete.getId();

                medicalRecordService.deleteRecordById(recordId);

                assertThat(medicalRecordRepository.findById(recordId)).isNotPresent();
        }

        @Test
        @DisplayName("deleteRecordById should throw AccessDeniedException for unowned record")
        void deleteRecordById_whenNotOwned_shouldThrowAccessDenied() {
                mockAuthenticatedUser(USER_SUB_1, USER_NAME_1); // User 1 is authenticated
                MedicalRecord otherUserRecord = medicalRecordRepository
                                .save(new MedicalRecord(null, "Cannot Delete", 60, "Not Yours", USER_SUB_2, null, null,
                                                null, null, false)); // Owned
                                                                     // by
                                                                     // User
                                                                     // 2
                Long recordId = otherUserRecord.getId();

                assertThatThrownBy(() -> medicalRecordService.deleteRecordById(recordId))
                                .isInstanceOf(AccessDeniedException.class)
                                .hasMessageContaining("You do not have permission to delete this medical record.");

                assertThat(medicalRecordRepository.findById(recordId)).isPresent(); // Still exists
        }

        @Test
        @DisplayName("deleteRecordById should throw RecordNotFoundException for non-existent record")
        void deleteRecordById_whenRecordNotFound_shouldThrowRecordNotFound() {
                mockAuthenticatedUser(USER_SUB_1, USER_NAME_1);
                Long nonExistentId = 999L;

                assertThatThrownBy(() -> medicalRecordService.deleteRecordById(nonExistentId))
                                .isInstanceOf(RecordNotFoundException.class)
                                .hasMessageContaining("Medical record not found or has already been deleted with ID: "
                                                + nonExistentId);
        }
}
