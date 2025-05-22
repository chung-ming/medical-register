package com.example.medicalregister.controller;

import com.example.medicalregister.config.SecurityConfig;
import com.example.medicalregister.exception.GlobalApiExceptionHandler;
import com.example.medicalregister.exception.RecordNotFoundException;
import com.example.medicalregister.model.MedicalRecord;
import com.example.medicalregister.service.MedicalRecordService;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MedicalRecordApiController.class)
@Import({ SecurityConfig.class, GlobalApiExceptionHandler.class,
                MedicalRecordApiControllerTest.TestControllerConfiguration.class })
@ActiveProfiles("test")
@EnableWebMvc
@DisplayName("MedicalRecordApiController Tests")
class MedicalRecordApiControllerTest {

        private static final String TEST_USER_NAME_ATTRIBUTE_KEY = "name";
        private static final String TEST_USER_SUB_ATTRIBUTE_KEY = "sub";
        private static final String TEST_USER_NAME = "Test User";
        private static final String TEST_USER_SUB_VALUE = "test-user-sub";
        @Autowired
        private MockMvc mockMvc;

        @Autowired // Spring will inject the mock bean from TestControllerConfiguration
        private MedicalRecordService medicalRecordService; // This will now be the mock from the context

        @Autowired
        private ObjectMapper objectMapper;

        private OAuth2User testUser;
        private MedicalRecord sampleRecord1;
        private MedicalRecord sampleRecord2;

        @TestConfiguration
        static class TestControllerConfiguration {
                @Bean
                public MedicalRecordService medicalRecordService() {
                        return Mockito.mock(MedicalRecordService.class);
                }
        }

        @BeforeEach
        void setUp() {
                Map<String, Object> attributes = new HashMap<>();
                attributes.put(TEST_USER_NAME_ATTRIBUTE_KEY, TEST_USER_NAME);
                attributes.put(TEST_USER_SUB_ATTRIBUTE_KEY, TEST_USER_SUB_VALUE);
                testUser = new DefaultOAuth2User(
                                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")),
                                attributes,
                                TEST_USER_NAME_ATTRIBUTE_KEY);

                sampleRecord1 = new MedicalRecord(1L, "Patient Zero", 30, "Initial History", TEST_USER_SUB_VALUE,
                                TEST_USER_SUB_VALUE,
                                TEST_USER_SUB_VALUE, LocalDateTime.now(), LocalDateTime.now(), false);
                sampleRecord2 = new MedicalRecord(2L, "Patient One", 45, "Follow-up History", TEST_USER_SUB_VALUE,
                                TEST_USER_SUB_VALUE,
                                TEST_USER_SUB_VALUE, LocalDateTime.now(), LocalDateTime.now(), false);
        }

        @AfterEach
        void tearDown() {
                Mockito.reset(medicalRecordService);
        }

        @Test
        @DisplayName("GET /api/v1/records - Authenticated - Should return list of records")
        void listRecords_authenticated_shouldReturnListOfRecords() throws Exception {
                when(medicalRecordService.findAllRecords()).thenReturn(List.of(sampleRecord1, sampleRecord2));

                mockMvc.perform(get("/api/v1/records").with(oauth2Login().oauth2User(testUser)))
                                .andExpect(status().isOk())
                                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                                .andExpect(jsonPath("$", hasSize(2)))
                                .andExpect(jsonPath("$[0].name", is(sampleRecord1.getName())));
        }

        @Test
        @DisplayName("POST /api/v1/records - Authenticated - Valid data - Should create record and return 201 Created")
        void createRecord_validData_shouldCreateRecord() throws Exception {
                MedicalRecord newRecordInput = new MedicalRecord(null, "New Patient", 25, "New History", null, null,
                                null, null,
                                null, false);
                MedicalRecord savedRecord = new MedicalRecord(3L, "New Patient", 25, "New History", TEST_USER_SUB_VALUE,
                                TEST_USER_SUB_VALUE, TEST_USER_SUB_VALUE, LocalDateTime.now(), LocalDateTime.now(),
                                false);
                when(medicalRecordService.saveRecord(any(MedicalRecord.class))).thenReturn(savedRecord);

                mockMvc.perform(post("/api/v1/records")
                                .with(oauth2Login().oauth2User(testUser))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(newRecordInput)))
                                .andExpect(status().isCreated())
                                .andExpect(header().string("Location",
                                                endsWith("/api/v1/records/" + savedRecord.getId())))
                                .andExpect(jsonPath("$.id", is(savedRecord.getId().intValue())));
        }

        @Test
        @DisplayName("POST /api/v1/records - Authenticated - Invalid data - Should return 400 Bad Request")
        void createRecord_invalidData_shouldReturnBadRequest() throws Exception {
                MedicalRecord invalidRecordInput = new MedicalRecord(null, null, -5, "", null, null, null, null, null,
                                false);
                // Note: The service method should not be called if validation fails at the
                // controller level due to @Valid.
                // The GlobalApiExceptionHandler will handle MethodArgumentNotValidException.
                mockMvc.perform(post("/api/v1/records")
                                .with(oauth2Login().oauth2User(testUser))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(invalidRecordInput)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.status", is(400)))
                                .andExpect(jsonPath("$.message", containsString("Name is mandatory")))
                                .andExpect(jsonPath("$.message", containsString("Age must be positive")))
                                .andExpect(jsonPath("$.message", containsString("Medical history is mandatory")));
                verify(medicalRecordService, never()).saveRecord(any(MedicalRecord.class));
        }

        @Test
        @DisplayName("GET /api/v1/records/{id} - Authenticated - Record found - Should return record")
        void getRecordById_recordFound_shouldReturnRecord() throws Exception {
                when(medicalRecordService.findRecordById(eq(sampleRecord1.getId()))).thenReturn(sampleRecord1);

                mockMvc.perform(get("/api/v1/records/{id}", sampleRecord1.getId())
                                .with(oauth2Login().oauth2User(testUser)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.name", is(sampleRecord1.getName())));
        }

        @Test
        @DisplayName("GET /api/v1/records/{id} - Authenticated - Record not found - Should return 404 Not Found")
        void getRecordById_recordNotFound_shouldReturnNotFound() throws Exception {
                when(medicalRecordService.findRecordById(anyLong()))
                                .thenThrow(new RecordNotFoundException("Record not found"));

                mockMvc.perform(get("/api/v1/records/{id}", 99L).with(oauth2Login().oauth2User(testUser)))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.message", is("Record not found")));
        }

        @Test
        @DisplayName("PUT /api/v1/records/{id} - Authenticated - Valid data - Should update record")
        void updateRecord_validData_shouldUpdateRecord() throws Exception {
                MedicalRecord updatedDetails = new MedicalRecord(sampleRecord1.getId(), "Updated Name", 32,
                                "Updated History",
                                null, null, null, null, null, false);
                when(medicalRecordService.saveRecord(any(MedicalRecord.class))).thenReturn(updatedDetails);

                mockMvc.perform(put("/api/v1/records/{id}", sampleRecord1.getId())
                                .with(oauth2Login().oauth2User(testUser))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(updatedDetails)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.name", is("Updated Name")));
        }

        @Test
        @DisplayName("PUT /api/v1/records/{id} - Authenticated - Service throws AccessDenied - Should return 403 Forbidden")
        void updateRecord_accessDenied_shouldReturnForbidden() throws Exception {
                String expectedServiceMessage = "You do not have permission to update this record.";
                MedicalRecord updateData = new MedicalRecord(1L, "Attempted Update", 40, "History", null, null, null,
                                null,
                                null, false);
                when(medicalRecordService.saveRecord(any(MedicalRecord.class)))
                                .thenThrow(new AccessDeniedException(expectedServiceMessage));

                mockMvc.perform(put("/api/v1/records/{id}", 1L)
                                .with(oauth2Login().oauth2User(testUser))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(updateData)))
                                .andExpect(status().isForbidden())
                                .andExpect(jsonPath("$.message", is(expectedServiceMessage)));
        }

        @Test
        @DisplayName("DELETE /api/v1/records/{id} - Authenticated - Record exists - Should delete and return 204 No Content")
        void deleteRecord_recordExists_shouldDeleteAndReturnNoContent() throws Exception {
                doNothing().when(medicalRecordService).deleteRecordById(eq(sampleRecord1.getId()));

                mockMvc.perform(delete("/api/v1/records/{id}", sampleRecord1.getId())
                                .with(oauth2Login().oauth2User(testUser)))
                                .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("DELETE /api/v1/records/{id} - Authenticated - Record not found - Should return 404 Not Found")
        void deleteRecord_recordNotFound_shouldReturnNotFound() throws Exception {
                Long recordIdToDelete = 99L;
                String expectedServiceMessage = "Medical record not found or has already been deleted with ID: "
                                + recordIdToDelete;
                doThrow(new RecordNotFoundException(expectedServiceMessage)).when(medicalRecordService)
                                .deleteRecordById(eq(recordIdToDelete));

                mockMvc.perform(delete("/api/v1/records/{id}", recordIdToDelete)
                                .with(oauth2Login().oauth2User(testUser)))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.message", is(expectedServiceMessage)));
        }
}
