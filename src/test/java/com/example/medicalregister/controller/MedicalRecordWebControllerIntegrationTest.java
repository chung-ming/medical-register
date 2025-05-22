package com.example.medicalregister.controller;

import com.example.medicalregister.model.MedicalRecord;
import com.example.medicalregister.repository.MedicalRecordRepository;
import com.example.medicalregister.util.SecurityTestUtils;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional // Ensures each test runs in a transaction and rolls back afterwards
@ActiveProfiles("test") // Use the profile in application-test.properties for integration tests
@DisplayName("MedicalRecordWebController Integration Tests")
/**
 * Integration tests for the {@link MedicalRecordWebController}. This class
 * tests the controller methods to ensure they work as expected in a full
 * application context.
 */
class MedicalRecordWebControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MedicalRecordRepository medicalRecordRepository;

    private OAuth2User testUser; // Use the interface type for the field
    private final String TEST_USER_SUB = "Test User Sub";
    private final String TEST_USER_NAME = "Test User Name";

    @BeforeEach
    void setUp() {
        // Create a test user with a specific sub and name
        testUser = SecurityTestUtils.createOAuth2UserWithSubAndName(TEST_USER_SUB, TEST_USER_NAME);
        // Clean up any existing records for this specific test user to ensure test
        // isolation, even though @Transactional handles rollback for test-managed data.
        // medicalRecordRepository.deleteAll(medicalRecordRepository.findByOwnerId(TEST_USER_SUB));
    }

    @AfterEach
    void tearDown() {
        // @Transactional should handle rollback, but explicit cleanup can be added if
        // needed
        // medicalRecordRepository.deleteAll(medicalRecordRepository.findByOwnerId(TEST_USER_SUB));
    }

    @Test
    @DisplayName("GET /records should redirect to OAuth2 login if unauthenticated")
    void listRecords_unauthenticated_shouldRedirectToLogin() throws Exception {
        mockMvc.perform(get("/records"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrlPattern("**/oauth2/authorization/*"));
    }

    @Test
    @DisplayName("Full CRUD Flow: Create, List, Edit, Delete Medical Record")
    void medicalRecord_fullCrudFlow_shouldSucceed() throws Exception {
        // 1. Show Create Form (Authenticated)
        mockMvc.perform(get("/records/new").with(oauth2Login().oauth2User(testUser)))
                .andExpect(status().isOk())
                .andExpect(view().name("records/record-form"))
                .andExpect(model().attribute("record", instanceOf(MedicalRecord.class)))
                .andExpect(model().attribute("userName", TEST_USER_NAME));

        // 2. Save New Record (Authenticated)
        String patientName = "Integration Patient";
        Integer patientAge = 45;
        String medicalHistory = "Integration test history.";

        mockMvc.perform(post("/records/save")
                .param("name", patientName)
                .param("age", String.valueOf(patientAge))
                .param("medicalHistory", medicalHistory)
                .with(csrf())
                .with(oauth2Login().oauth2User(testUser)))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/records"))
                .andExpect(flash().attribute("successMessage", "Record successfully created."));

        // Verify record in DB
        Page<MedicalRecord> recordsInDb = medicalRecordRepository.findByOwnerId(TEST_USER_SUB, Pageable.unpaged());
        assertThat(recordsInDb).hasSize(1);
        MedicalRecord savedRecord = recordsInDb.getContent().get(0);
        assertThat(savedRecord.getName()).isEqualTo(patientName);
        assertThat(savedRecord.getAge()).isEqualTo(patientAge);
        assertThat(savedRecord.getMedicalHistory()).isEqualTo(medicalHistory);
        assertThat(savedRecord.getOwnerId()).isEqualTo(TEST_USER_SUB);
        Long savedRecordId = savedRecord.getId();

        // 3. List Records (Authenticated) - should show the new record
        mockMvc.perform(get("/records").with(oauth2Login().oauth2User(testUser)))
                .andExpect(status().isOk())
                .andExpect(view().name("records/list-records"))
                .andExpect(model().attribute("recordPage", hasProperty("content", hasSize(1))))
                .andExpect(model().attribute("recordPage", hasProperty("content", contains(
                        allOf(
                                hasProperty("id", is(savedRecordId)),
                                hasProperty("name", is(patientName)))))));

        // 4. Show Edit Form (Authenticated)
        mockMvc.perform(get("/records/edit/" + savedRecordId).with(oauth2Login().oauth2User(testUser)))
                .andExpect(status().isOk())
                .andExpect(view().name("records/record-form"))
                .andExpect(model().attribute("record", hasProperty("id", is(savedRecordId))))
                .andExpect(model().attribute("record", hasProperty("name", is(patientName))));

        // 5. Update Record (Authenticated)
        String updatedPatientName = "Updated Integration Patient";
        mockMvc.perform(post("/records/save")
                .param("id", String.valueOf(savedRecordId)) // Important for update
                .param("name", updatedPatientName)
                .param("age", String.valueOf(patientAge))
                .param("medicalHistory", medicalHistory)
                .with(csrf())
                .with(oauth2Login().oauth2User(testUser)))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/records"))
                .andExpect(flash().attribute("successMessage", "Record successfully updated."));

        // Verify update in DB
        MedicalRecord updatedRecord = medicalRecordRepository.findById(savedRecordId).orElseThrow();
        assertThat(updatedRecord.getName()).isEqualTo(updatedPatientName);

        // 6. Delete Record (Authenticated)
        mockMvc.perform(get("/records/delete/" + savedRecordId).with(oauth2Login().oauth2User(testUser)))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/records"))
                .andExpect(flash().attribute("successMessage", "Record successfully deleted."));

        // Verify deletion from DB
        assertThat(medicalRecordRepository.findById(savedRecordId)).isNotPresent();
        assertThat(medicalRecordRepository.findByOwnerId(TEST_USER_SUB, Pageable.unpaged())).isEmpty();
    }

    @Test
    @DisplayName("POST /records/save with invalid data should return to form with errors")
    void saveRecord_invalidData_shouldReturnFormWithErrors() throws Exception {
        mockMvc.perform(post("/records/save") // Missing required fields like name
                .param("age", "invalidAge") // Example of invalid type
                .with(csrf())
                .with(oauth2Login().oauth2User(testUser)))
                .andExpect(status().isOk())
                .andExpect(view().name("records/record-form"))
                .andExpect(model().attributeHasFieldErrors("record", "name")) // Expect error on 'name'
                .andExpect(model().attributeHasFieldErrorCode("record", "age", "typeMismatch")); // Expect
                                                                                                 // type
                                                                                                 // error
                                                                                                 // on
                                                                                                 // 'age'

        // Verify no record was saved
        assertThat(medicalRecordRepository.findByOwnerId(TEST_USER_SUB, Pageable.unpaged())).isEmpty();
    }
}
