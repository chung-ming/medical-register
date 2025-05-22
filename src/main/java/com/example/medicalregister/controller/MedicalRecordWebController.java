package com.example.medicalregister.controller;

import com.example.medicalregister.exception.RecordNotFoundException;
import com.example.medicalregister.model.MedicalRecord;
import com.example.medicalregister.service.MedicalRecordService;

import jakarta.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Web controller for managing medical records. Handles CRUD operations via web
 * UI, ensuring user authentication and authorization. All handler methods in
 * this controller are relative to /records.
 */
@Controller
@RequestMapping("/records")
public class MedicalRecordWebController {

    private static final Logger logger = LoggerFactory.getLogger(MedicalRecordWebController.class);
    private final MedicalRecordService recordService;

    /**
     * Constructs the controller with a {@link MedicalRecordService}.
     * 
     * @param recordService Service for medical record operations.
     */
    public MedicalRecordWebController(MedicalRecordService recordService) {
        this.recordService = recordService;
    }

    /**
     * Adds common user information to the model for all views rendered by this
     * controller. This method is invoked before any @GetMapping or @PostMapping
     * handler in this class.
     * 
     * @param model     The Spring MVC model.
     * @param principal The authenticated OAuth2User. Assumed to be non-null as
     *                  requests to this controller are secured.
     */
    @ModelAttribute
    public void addUserInfoToModel(Model model, @AuthenticationPrincipal OAuth2User principal) {
        // For controllers secured by .anyRequest().authenticated(), principal should
        // not be null. If it were, Spring Security would have intercepted the request.
        String name = principal.getAttribute("name");
        model.addAttribute("userName", name != null ? name : "User");
        // User is confirmed to be authenticated to reach any mapping in this
        // controller.
        model.addAttribute("isAuthenticated", true);
        logger.debug("User '{}' is authenticated and accessing a medical record view.", name != null ? name : "User");
    }

    /**
     * Displays a list of medical records for the authenticated user.
     * 
     * @param model              The Spring MVC model.
     * @param redirectAttributes Used for flash messages on redirect.
     * @return The view name for listing records, or redirects to home on access
     *         denial.
     */
    @GetMapping
    public String listRecords(Model model, RedirectAttributes redirectAttributes) {
        String userName = (String) model.getAttribute("userName");
        try {
            logger.info("User {} attempting to list records.", userName);
            model.addAttribute("records", recordService.findAllRecords());
        } catch (AccessDeniedException e) {
            logger.warn("Access denied for user {} while listing records: {}", userName, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/";
        }
        return "records/list-records";
    }

    /**
     * Shows the form for creating a new medical record.
     * 
     * @param model The Spring MVC model.
     * @return The view name for the record form.
     */
    @GetMapping("/new")
    public String showCreateForm(Model model) {
        String userName = (String) model.getAttribute("userName");
        model.addAttribute("record", new MedicalRecord());
        logger.info("User {} is accessing the new record form.", userName);
        return "records/record-form";
    }

    /**
     * Processes the submission of the medical record form (creation or update).
     * Validates the record, then saves it using {@link MedicalRecordService}.
     * Redirects to the record list on success, or back to the form on validation
     * error or access denial.
     *
     * @param record             The medical record submitted from the form, with
     *                           validation annotations.
     * @param result             BindingResult for validation outcomes.
     * @param model              The Spring MVC model (used here to retrieve
     *                           userName).
     * @param redirectAttributes Used for flash messages on redirect.
     * @return Redirects to the record list view or back to the form view.
     */
    @PostMapping("/save")
    public String saveRecord(@Valid @ModelAttribute("record") MedicalRecord record,
            BindingResult result, Model model, RedirectAttributes redirectAttributes) {
        String userName = (String) model.getAttribute("userName");
        if (result.hasErrors()) {
            logger.warn("User {} encountered validation errors while attempting to save a record. Errors: {}", userName,
                    result.getAllErrors());
            // Validation errors occurred. Return to the form.
            // Model already contains necessary user info from addUserInfoToModel.
            return "records/record-form";
        }
        try {
            boolean isNewRecord = record.getId() == null;
            recordService.saveRecord(record);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Record successfully " + (isNewRecord ? "created." : "updated."));
            logger.info("User {} {} a record with ID: {}.", userName, (isNewRecord ? "created" : "updated"),
                    record.getId());
        } catch (AccessDeniedException e) {
            logger.warn("Access denied for user {} while saving record: {}", userName, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/records";
    }

    /**
     * Shows the form for editing an existing medical record.
     * 
     * @param id                 The ID of the record to edit.
     * @param model              The Spring MVC model.
     * @param redirectAttributes Used for flash messages on redirect.
     * @return The view name for the record form, or redirects to the records list
     *         on error.
     */
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes) {
        String userName = (String) model.getAttribute("userName");
        try {
            MedicalRecord record = recordService.findRecordById(id);
            logger.info("User {} is accessing edit form for record ID: {}.", userName, id);
            model.addAttribute("record", record);
            return "records/record-form";
        } catch (RecordNotFoundException e) {
            logger.warn("User {} encountered RecordNotFoundException when trying to edit record ID {}: {}", userName,
                    id,
                    e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (AccessDeniedException e) {
            // Handles cases where the user does not own the record or other access issues.
            logger.warn("Access denied for user {} trying to edit record ID {}: {}", userName, id, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/records";
    }

    /**
     * Deletes a medical record by its ID.
     * 
     * @param id                 The ID of the record to delete.
     * @param model              The Spring MVC model (used here to retrieve
     *                           userName).
     * @param redirectAttributes Used for flash messages on redirect.
     * @return Redirects to the records list.
     */
    @GetMapping("/delete/{id}")
    public String deleteRecord(@PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes) {
        String userName = (String) model.getAttribute("userName");
        try {
            logger.info("User {} attempting to delete record ID: {}.", userName, id);
            recordService.deleteRecordById(id);
            redirectAttributes.addFlashAttribute("successMessage", "Record successfully deleted.");
        } catch (RecordNotFoundException e) {
            logger.warn("Attempt by user {} to delete non-existent record ID {}: {}", userName, id, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (AccessDeniedException e) {
            // Handles cases where the user does not have permission to delete the record,
            // or if the user authentication is insufficient (e.g. missing 'sub' claim).
            logger.warn("Access denied for user {} trying to delete record ID {}: {}", userName, id, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/records";
    }
}
