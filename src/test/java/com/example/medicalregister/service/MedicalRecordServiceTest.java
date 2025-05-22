package com.example.medicalregister.service;

import com.example.medicalregister.exception.RecordNotFoundException;
import com.example.medicalregister.model.MedicalRecord;
import com.example.medicalregister.repository.MedicalRecordRepository;
import com.example.medicalregister.util.SecurityTestUtils;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ActiveProfiles("test") // Use the test profile to avoid using production database
@DisplayName("MedicalRecordService Tests")
@ExtendWith(MockitoExtension.class)
/**
 * Unit tests for the {@link MedicalRecordService}. This class tests the service
 * methods to ensure they work as expected.
 */
class MedicalRecordServiceTest {

    @Mock
    private MedicalRecordRepository medicalRecordRepository;

    @InjectMocks
    private MedicalRecordService medicalRecordService;

    private MockedStatic<SecurityContextHolder> mockedSecurityContextHolder;
    @Mock
    private SecurityContext securityContext;
    @Mock
    private Authentication authentication;

    private final String USER_SUB_1 = "auth0|user1";
    private final String USER_SUB_2 = "auth0|user2";

    @BeforeEach
    void setUp() {
        mockedSecurityContextHolder = Mockito.mockStatic(SecurityContextHolder.class);
        mockedSecurityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
    }

    @AfterEach
    void tearDown() {
        mockedSecurityContextHolder.close();
    }

    private void mockAuthenticatedUserWithSub(String sub) {
        // Use SecurityTestUtils, 'sub' is the nameAttributeKey here
        OAuth2User principal = SecurityTestUtils.createOAuth2UserWithSubAndName(sub, "Test User");
        when(authentication.getPrincipal()).thenReturn(principal);
    }

    private void mockAuthenticatedUserWithoutSub() {
        // Use SecurityTestUtils, 'name' is the nameAttributeKey here
        OAuth2User principal = SecurityTestUtils.createOAuth2User(Map.of("name", "Test User"), "name");
        when(authentication.getPrincipal()).thenReturn(principal);
    }

    private void mockUnauthenticated() {
        when(SecurityContextHolder.getContext().getAuthentication()).thenReturn(null);
    }

    @Test
    @DisplayName("findAllRecords should return records for authenticated user with sub")
    void findAllRecords_whenUserAuthenticatedWithSub_shouldReturnRecords() {
        mockAuthenticatedUserWithSub(USER_SUB_1);
        List<MedicalRecord> recordList = List
                .of(new MedicalRecord(1L, "Test", 30, "History", USER_SUB_1, null, null, null, null, false));
        Page<MedicalRecord> recordPage = new PageImpl<>(recordList, Pageable.unpaged(), recordList.size());

        when(medicalRecordRepository.findByOwnerId(eq(USER_SUB_1), any(Pageable.class))).thenReturn(recordPage);

        Page<MedicalRecord> resultPage = medicalRecordService.findAllRecords(Pageable.unpaged());

        assertThat(resultPage.getContent()).hasSize(1);
        assertThat(resultPage.getContent().get(0).getOwnerId()).isEqualTo(USER_SUB_1);
        verify(medicalRecordRepository).findByOwnerId(eq(USER_SUB_1), any(Pageable.class));
    }

    @Test
    @DisplayName("findAllRecords should throw AccessDeniedException if user has no sub")
    void findAllRecords_whenUserHasNoSub_shouldThrowAccessDenied() {
        mockAuthenticatedUserWithoutSub();
        assertThatThrownBy(() -> medicalRecordService.findAllRecords(Pageable.unpaged()))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("User must be authenticated with a 'sub' claim");
    }

    @Test
    @DisplayName("findAllRecords should throw AccessDeniedException if unauthenticated")
    void findAllRecords_whenUnauthenticated_shouldThrowAccessDenied() {
        mockUnauthenticated();
        assertThatThrownBy(() -> medicalRecordService.findAllRecords(Pageable.unpaged()))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("User must be authenticated with a 'sub' claim");
    }

    @Test
    @DisplayName("findRecordById should return record if owned by user with sub")
    void findRecordById_whenOwnedByUserWithSub_shouldReturnRecord() {
        mockAuthenticatedUserWithSub(USER_SUB_1);
        MedicalRecord record = new MedicalRecord(1L, "Test", 30, "History", USER_SUB_1, null, null, null, null, false);
        when(medicalRecordRepository.findByIdAndOwnerId(1L, USER_SUB_1)).thenReturn(Optional.of(record));

        MedicalRecord foundRecord = medicalRecordService.findRecordById(1L);

        assertThat(foundRecord).isNotNull();
        assertThat(foundRecord.getId()).isEqualTo(1L);
        verify(medicalRecordRepository).findByIdAndOwnerId(1L, USER_SUB_1);
    }

    @Test
    @DisplayName("findRecordById should throw RecordNotFoundException if not owned by user")
    void findRecordById_whenNotOwnedByUser_shouldThrowRecordNotFound() {
        mockAuthenticatedUserWithSub(USER_SUB_1);
        when(medicalRecordRepository.findByIdAndOwnerId(1L, USER_SUB_1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> medicalRecordService.findRecordById(1L))
                .isInstanceOf(RecordNotFoundException.class)
                .hasMessageContaining("Medical record not found");
    }

    @Test
    @DisplayName("findRecordById should throw AccessDeniedException if user has no sub")
    void findRecordById_whenUserHasNoSub_shouldThrowAccessDenied() {
        mockAuthenticatedUserWithoutSub();
        assertThatThrownBy(() -> medicalRecordService.findRecordById(1L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("User must be authenticated with a 'sub' claim");
    }

    @Test
    @DisplayName("saveRecord should save new record and set ownerId for user with sub")
    void saveRecord_whenNewRecordAndUserWithSub_shouldSaveAndSetOwnerId() {
        mockAuthenticatedUserWithSub(USER_SUB_1);
        MedicalRecord newRecord = new MedicalRecord(null, "New", 20, "New Hist", null, null, null, null, null, false);
        MedicalRecord savedRecord = new MedicalRecord(1L, "New", 20, "New Hist", USER_SUB_1, null, null, null, null,
                false);

        when(medicalRecordRepository.save(any(MedicalRecord.class))).thenReturn(savedRecord);

        MedicalRecord result = medicalRecordService.saveRecord(newRecord);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getOwnerId()).isEqualTo(USER_SUB_1);
        verify(medicalRecordRepository)
                .save(argThat(r -> r.getOwnerId().equals(USER_SUB_1) && r.getName().equals("New")));
    }

    @Test
    @DisplayName("saveRecord should update existing record if owned by user with sub")
    void saveRecord_whenExistingRecordOwnedByUserWithSub_shouldUpdate() {
        mockAuthenticatedUserWithSub(USER_SUB_1);
        MedicalRecord updatedDetails = new MedicalRecord(1L, "Updated Name", 31, "Updated Hist", USER_SUB_1, null, null,
                null, null, false);

        when(medicalRecordRepository.existsByIdAndOwnerId(1L, USER_SUB_1)).thenReturn(true);
        when(medicalRecordRepository.save(any(MedicalRecord.class))).thenReturn(updatedDetails);

        MedicalRecord result = medicalRecordService.saveRecord(updatedDetails);

        assertThat(result.getName()).isEqualTo("Updated Name");
        assertThat(result.getOwnerId()).isEqualTo(USER_SUB_1);
        verify(medicalRecordRepository).existsByIdAndOwnerId(1L, USER_SUB_1);
        verify(medicalRecordRepository).save(updatedDetails);
    }

    @Test
    @DisplayName("saveRecord should throw AccessDeniedException if updating record not owned by user")
    void saveRecord_whenUpdatingRecordNotOwned_shouldThrowAccessDenied() {
        mockAuthenticatedUserWithSub(USER_SUB_1);
        MedicalRecord recordToUpdate = new MedicalRecord(1L, "Name", 30, "History", USER_SUB_2, null, null, null, null,
                false); // Belongs
        // to
        // USER_SUB_2

        when(medicalRecordRepository.existsByIdAndOwnerId(1L, USER_SUB_1)).thenReturn(false); // User1 does not own
                                                                                              // record 1L

        assertThatThrownBy(() -> medicalRecordService.saveRecord(recordToUpdate))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("You do not have permission to update this record.");
        verify(medicalRecordRepository).existsByIdAndOwnerId(1L, USER_SUB_1);
        verify(medicalRecordRepository, never()).save(any(MedicalRecord.class));
    }

    @Test
    @DisplayName("saveRecord should throw AccessDeniedException if user has no sub")
    void saveRecord_whenUserHasNoSub_shouldThrowAccessDenied() {
        mockAuthenticatedUserWithoutSub();
        MedicalRecord record = new MedicalRecord();
        assertThatThrownBy(() -> medicalRecordService.saveRecord(record))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("User must be authenticated with a 'sub' claim");
    }

    @Test
    @DisplayName("deleteRecordById should delete record if owned by user with sub")
    void deleteRecordById_whenOwnedByUserWithSub_shouldDelete() {
        mockAuthenticatedUserWithSub(USER_SUB_1);
        when(medicalRecordRepository.existsById(1L)).thenReturn(true);
        when(medicalRecordRepository.existsByIdAndOwnerId(1L, USER_SUB_1)).thenReturn(true);
        doNothing().when(medicalRecordRepository).deleteById(1L);

        medicalRecordService.deleteRecordById(1L);

        verify(medicalRecordRepository).existsById(1L);
        verify(medicalRecordRepository).existsByIdAndOwnerId(1L, USER_SUB_1);
        verify(medicalRecordRepository).deleteById(1L);
    }

    @Test
    @DisplayName("deleteRecordById should throw RecordNotFoundException if record does not exist")
    void deleteRecordById_whenRecordDoesNotExist_shouldThrowRecordNotFound() {
        mockAuthenticatedUserWithSub(USER_SUB_1);
        when(medicalRecordRepository.existsById(1L)).thenReturn(false);

        assertThatThrownBy(() -> medicalRecordService.deleteRecordById(1L))
                .isInstanceOf(RecordNotFoundException.class)
                .hasMessageContaining("Medical record not found or has already been deleted with ID: 1");
        verify(medicalRecordRepository).existsById(1L);
        verify(medicalRecordRepository, never()).existsByIdAndOwnerId(anyLong(), anyString());
        verify(medicalRecordRepository, never()).deleteById(anyLong());
    }

    @Test
    @DisplayName("deleteRecordById should throw AccessDeniedException if record not owned by user")
    void deleteRecordById_whenRecordNotOwned_shouldThrowAccessDenied() {
        mockAuthenticatedUserWithSub(USER_SUB_1);
        when(medicalRecordRepository.existsById(1L)).thenReturn(true);
        when(medicalRecordRepository.existsByIdAndOwnerId(1L, USER_SUB_1)).thenReturn(false); // Not owned

        assertThatThrownBy(() -> medicalRecordService.deleteRecordById(1L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("You do not have permission to delete this medical record.");
        verify(medicalRecordRepository).existsById(1L);
        verify(medicalRecordRepository).existsByIdAndOwnerId(1L, USER_SUB_1);
        verify(medicalRecordRepository, never()).deleteById(anyLong());
    }

    @Test
    @DisplayName("deleteRecordById should throw AccessDeniedException if user has no sub")
    void deleteRecordById_whenUserHasNoSub_shouldThrowAccessDenied() {
        mockAuthenticatedUserWithoutSub();
        assertThatThrownBy(() -> medicalRecordService.deleteRecordById(1L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("User must be authenticated with a 'sub' claim");
    }
}
