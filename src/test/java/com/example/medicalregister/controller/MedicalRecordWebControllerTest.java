package com.example.medicalregister.controller;

import com.example.medicalregister.exception.RecordNotFoundException;
import com.example.medicalregister.model.MedicalRecord;
import com.example.medicalregister.service.MedicalRecordService;
import com.example.medicalregister.util.SecurityTestUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.instanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MedicalRecordWebController.class)
@ActiveProfiles("test") // Use the test profile to avoid using production database
@DisplayName("MedicalRecordWebController Unit Tests")
/**
 * Unit tests for {@link MedicalRecordWebController}. This class tests the
 * controller's behavior with both authenticated and unauthenticated users,
 * including various scenarios for creating, updating, and deleting medical
 * records.
 */
class MedicalRecordWebControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean(name = "recordService")
    private MedicalRecordService recordService;

    @Test
    @DisplayName("GET /records should return list-records view for authenticated user")
    void listRecords_authenticated_shouldReturnViewAndRecords() throws Exception {
        // Arrange
        List<MedicalRecord> mockRecords = Collections.singletonList(new MedicalRecord());
        when(recordService.findAllRecords()).thenReturn(mockRecords);

        var mockPrincipal = SecurityTestUtils.createOAuth2User(
                Map.of("name", "Test User", "email", "test@example.com"), // Attributes
                "name");

        // Act & Assert
        mockMvc.perform(get("/records").with(oauth2Login().oauth2User(mockPrincipal)))
                .andExpect(status().isOk())
                .andExpect(view().name("records/list-records"))
                .andExpect(model().attributeExists("records"))
                .andExpect(model().attribute("records", mockRecords));

        verify(recordService).findAllRecords();
    }

    @Test
    @DisplayName("GET /records should redirect to login for unauthenticated user")
    void listRecords_unauthenticated_shouldRedirectToLogin() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/records"))
                .andExpect(status().isFound())
                // Expect redirect to the OAuth2 authorization endpoint
                .andExpect(redirectedUrlPattern("**/oauth2/authorization/*"));
        verify(recordService, never()).findAllRecords();
    }

    @Test
    @DisplayName("GET /records/new should return record-form view for authenticated user")
    void showCreateForm_authenticated_shouldReturnViewAndNewRecord() throws Exception {
        // Arrange
        var mockPrincipal = SecurityTestUtils.createOAuth2User(
                Map.of("name", "Test User", "email", "test@example.com"),
                "name");

        // Act & Assert
        mockMvc.perform(get("/records/new").with(oauth2Login().oauth2User(mockPrincipal)))
                .andExpect(status().isOk())
                .andExpect(view().name("records/record-form"))
                .andExpect(model().attributeExists("record"))
                .andExpect(model().attribute("record", instanceOf(MedicalRecord.class)));
    }

    @Test
    @DisplayName("POST /records/save should save valid record and redirect for authenticated user")
    void saveRecord_authenticated_validData_shouldSaveAndRedirect() throws Exception {
        // Arrange
        MedicalRecord validRecord = new MedicalRecord();
        validRecord.setName("John Doe");
        validRecord.setAge(33);
        validRecord.setMedicalHistory("No known allergies");

        // Mock service to simulate ID generation upon saving a new record.
        when(recordService.saveRecord(any(MedicalRecord.class))).thenAnswer(invocation -> {
            MedicalRecord recordToSave = invocation.getArgument(0);
            if (recordToSave.getId() == null) {
                recordToSave.setId(1L); // Simulate ID generation for a new record
            }
            return recordToSave;
        });

        var mockPrincipal = SecurityTestUtils.createOAuth2User(
                Map.of("name", "Test User", "email", "test@example.com"),
                "name");

        // Act & Assert
        mockMvc.perform(post("/records/save")
                .flashAttr("record", validRecord) // Use flashAttr to simulate @ModelAttribute binding
                .with(SecurityMockMvcRequestPostProcessors.csrf()) // Include CSRF token
                .with(oauth2Login().oauth2User(mockPrincipal))) // Provide OAuth2 principal
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/records"))
                .andExpect(flash().attributeExists("successMessage"));

        verify(recordService).saveRecord(any(MedicalRecord.class));
    }

    @Test
    @DisplayName("POST /records/save should return to form view with errors for invalid data")
    void saveRecord_authenticated_invalidData_shouldReturnToForm() throws Exception {
        // Arrange
        MedicalRecord invalidRecord = new MedicalRecord();

        // Intentionally leave required fields unset to trigger validation errors.

        var mockPrincipal = SecurityTestUtils.createOAuth2User(
                Map.of("name", "Test User", "email", "test@example.com"),
                "name");

        // Act & Assert
        mockMvc.perform(post("/records/save")
                .flashAttr("record", invalidRecord) // Simulate @ModelAttribute binding
                .with(SecurityMockMvcRequestPostProcessors.csrf()) // Include CSRF token for POST
                .with(oauth2Login().oauth2User(mockPrincipal))) // Provide OAuth2 principal
                .andExpect(status().isOk()) // Stay on the form page
                .andExpect(view().name("records/record-form"))
                .andExpect(model().attributeHasErrors("record"));

        verify(recordService, never()).saveRecord(any(MedicalRecord.class));
    }

    @Test
    @DisplayName("GET /records/edit/{id} should return form view with record for authenticated user")
    void showEditForm_authenticated_recordFound_shouldReturnViewAndRecord()
            throws Exception {
        // Arrange
        Long recordId = 1L;
        MedicalRecord mockRecord = new MedicalRecord();
        mockRecord.setId(recordId);
        mockRecord.setName("Jane Doe");

        when(recordService.findRecordById(recordId)).thenReturn(mockRecord);

        var mockPrincipal = SecurityTestUtils.createOAuth2User(
                Map.of("name", "Test User", "email", "test@example.com"),
                "name");

        // Act & Assert
        mockMvc.perform(get("/records/edit/{id}", recordId).with(oauth2Login().oauth2User(mockPrincipal)))
                .andExpect(status().isOk())
                .andExpect(view().name("records/record-form"))
                .andExpect(model().attributeExists("record"))
                .andExpect(model().attribute("record", mockRecord));

        verify(recordService).findRecordById(recordId);
    }

    @Test
    @DisplayName("GET /records/edit/{id} should redirect with error if record not found")
    void showEditForm_authenticated_recordNotFound_shouldRedirectWithError()
            throws Exception {
        // Arrange
        Long recordId = 99L; // Use an ID assumed not to exist
        when(recordService.findRecordById(recordId)).thenThrow(new RecordNotFoundException("Record not found"));

        var mockPrincipal = SecurityTestUtils.createOAuth2User(
                Map.of("name", "Test User", "email", "test@example.com"),
                "name");

        // Act & Assert
        mockMvc.perform(get("/records/edit/{id}", recordId).with(oauth2Login().oauth2User(mockPrincipal)))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/records"))
                .andExpect(flash().attributeExists("errorMessage"));

        verify(recordService).findRecordById(recordId);
    }

    @Test
    @DisplayName("GET /records/edit/{id} should redirect with error if access denied")
    void showEditForm_authenticated_accessDenied_shouldRedirectWithError() throws Exception {
        // Arrange
        Long recordId = 1L;
        when(recordService.findRecordById(recordId)).thenThrow(new AccessDeniedException("Not your record"));

        var mockPrincipal = SecurityTestUtils.createOAuth2User(
                Map.of("name", "Test User", "email", "test@example.com"),
                "name");

        // Act & Assert
        mockMvc.perform(get("/records/edit/{id}", recordId).with(oauth2Login().oauth2User(mockPrincipal)))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/records"))
                .andExpect(flash().attributeExists("errorMessage"));

        verify(recordService).findRecordById(recordId);
    }

    @Test
    @DisplayName("GET /records/delete/{id} should delete record and redirect with success for authenticated user")
    void deleteRecord_authenticated_shouldDeleteAndRedirect() throws Exception {
        // Arrange
        Long recordId = 1L;
        // Service call should complete without exception for successful deletion.
        doNothing().when(recordService).deleteRecordById(recordId);

        var mockPrincipal = SecurityTestUtils.createOAuth2User(
                Map.of("name", "Test User", "email", "test@example.com"),
                "name");

        // Act & Assert
        mockMvc.perform(get("/records/delete/{id}", recordId).with(oauth2Login().oauth2User(mockPrincipal)))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/records"))
                .andExpect(flash().attributeExists("successMessage"));

        verify(recordService).deleteRecordById(recordId);
    }

    @Test
    @DisplayName("GET /records/delete/{id} should redirect with error if record not found")
    void deleteRecord_authenticated_recordNotFound_shouldRedirectWithError()
            throws Exception {
        // Arrange
        Long recordId = 99L;
        doThrow(new RecordNotFoundException("Record not found")).when(recordService).deleteRecordById(recordId);

        var mockPrincipal = SecurityTestUtils.createOAuth2User(
                Map.of("name", "Test User", "email", "test@example.com"),
                "name");

        // Act & Assert
        mockMvc.perform(get("/records/delete/{id}", recordId).with(oauth2Login().oauth2User(mockPrincipal)))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/records"))
                .andExpect(flash().attributeExists("errorMessage"));

        verify(recordService).deleteRecordById(recordId);
    }

    @Test
    @DisplayName("GET /records/delete/{id} should redirect with error if access denied")
    void deleteRecord_authenticated_accessDenied_shouldRedirectWithError() throws Exception {
        // Arrange
        Long recordId = 1L;
        doThrow(new AccessDeniedException("Not allowed")).when(recordService).deleteRecordById(recordId);

        var mockPrincipal = SecurityTestUtils.createOAuth2User(
                Map.of("name", "Test User", "email", "test@example.com"),
                "name");

        // Act & Assert
        mockMvc.perform(get("/records/delete/{id}", recordId).with(oauth2Login().oauth2User(mockPrincipal)))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/records"))
                .andExpect(flash().attributeExists("errorMessage"));

        verify(recordService).deleteRecordById(recordId);
    }
}