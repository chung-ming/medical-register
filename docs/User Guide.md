# User Guide: Medical Register Application

---

## 1. Welcome to the Medical Register

Welcome! The Medical Register application is designed to help you manage medical records efficiently and securely. With this system, you can easily create, view, update, and delete medical records.

## 2. Accessing the Application

To use the Medical Register, you'll first need to log in.

1.  **Open Your Web Browser:** Launch your preferred web browser (like Chrome, Firefox, Safari, or Edge).
2.  **Go to the Application URL:** https://d3n8lb5zdun1i7.cloudfront.net or http://localhost:8080, depending on whether you're accessing the production or development version.
3.  **Login via Auth0:**
    - You will be automatically redirected to a login screen provided by Auth0.
    - Enter your email and password.
    - Click "Continue" button.

Upon successful login, you will be taken to the home screen of the Medical Register application, which displays a list of existing medical records.

## 3. Navigating the Application

Once logged in, you will be redirected to the Medical Records screen, where you can see:

- **Navigation Options:**
  - **"Add New Record" Button:** Adds a new medical record to the system.
  - **"Back to Home" Button:** Returns you to the home screen.
  - **"Logout" Button:** Logs you out of the application.
- **A List of Medical Records:** Displays a list of existing medical records that you have access to.

## 4. Managing Medical Records

This section explains how to perform common tasks with medical records.

### 4.1. Viewing Medical Records

- **List View:** After logging in, you will typically be presented with a list of all medical records associated with your account.
- **Viewing Details:** The list view shows key information, including the ID, Name, Age, and Medical History.

### 4.2. Adding a New Medical Record

1.  **Find the "Add New Record" Button:** On the Medical Records screen, look for a button labeled "Add New Record." Click on it.
2.  **Fill in the Record Details:** A form will appear asking for the following information:
    - **Name:** The full name of the patient.
    - **Age:** The age of the patient (must be a non-negative number).
    - **Medical History:** A description of the patient's relevant medical history.
3.  **Save the Record:** Once you have entered all the necessary information, click the "Save" button to add the new record to the system.

### 4.3. Editing an Existing Medical Record

1.  **Locate the Record:** On the Medical Records screen, find the medical record you wish to edit in the list view.
2.  **Find the "Edit" Button:** In the "Actions" column for that record, look for the "Edit" button. Click on it.
3.  **Modify the Details:** The record's information will appear in a form. You can now change the Name, Age, or Medical History as needed.
4.  **Save Changes:** After making your changes, click the "Save" button.

### 4.4. Deleting a Medical Record

1.  **Locate the Record:** On the Medical Records screen, , find the medical record you wish to delete in the list view.
2.  **Find the "Delete" Button:** In the "Actions" column for that record, look for a "Delete" button. Click on it.
3.  **Confirm Deletion:** A confirmation modal window will appear asking you to confirm the deletion.
    - Click the "Delete" button in the modal to permanently remove the record.
    - Click "Cancel" if you do not want to delete the record.
      **Note:** Deleting a record is a permanent action and cannot be undone. Please be sure before confirming.

## 5. Logging Out

When you are finished using the Medical Register application, it's important to log out to protect your session and the data.

1.  **Find the "Logout" Button:** On every screen, look for the "Logout" button, often located in the header or near your username/email.
2.  **Click Logout:** Clicking this will end your session in the Medical Register application and also log you out of your Auth0 session. You will be redirected to the home screen with a logout confirmation message.

## 6. H2 Console (Development/Testing Only)

- **Note:** This feature is typically only enabled in development or test environments and is disabled in production for security reasons.
- If enabled, you will see a "H2 Console" button on the home screen. This provides a web interface to view and manage the H2 database used by the application.

## 7. Help & Support

If you encounter any issues while using the Medical Register application or have any questions, please contact me at chungming_tsen[at]outlook[dot]com
