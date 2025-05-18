package com.example.medicalregister.e2e;

import io.github.bonigarcia.wdm.WebDriverManager;

import org.junit.jupiter.api.*;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@ActiveProfiles("local")
@DisplayName("E2E Tests for Medical Record CRUD Operations")
/**
 * E2E tests for the Medical Record CRUD operations.
 * This class uses Selenium WebDriver to automate browser interactions.
 * It assumes the application is running locally and accessible at the specified
 * BASE_URL.
 */
@Tag("e2e")
public class MedicalRecordE2ETest {

    private static WebDriver driver;
    private static WebDriverWait wait;
    // Consider making BASE_URL configurable via environment variables or system
    // properties for CI/CD
    private static final String BASE_URL = "http://localhost:8080";

    // Variables to carry state between ordered tests
    private static String uniquePatientName;
    private static String createdRecordId;

    @BeforeAll
    static void setUpClass() {
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        // options.addArguments("--headless"); // Uncomment for headless execution
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1920,1080");
        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(20)); // Slightly increased default wait
        uniquePatientName = "E2E Patient " + UUID.randomUUID().toString().substring(0, 8);
    }

    @AfterAll
    static void tearDownClass() {
        if (driver != null) {
            driver.quit();
        }
    }

    void ensureLoggedIn() {
        // Uses a dedicated /test/login endpoint to simulate authentication for E2E
        // tests.
        driver.get(BASE_URL + "/test/login");

        // The current check for "Simulated login successful" in page source is a bit
        // brittle.
        // A more robust check would be to wait for a specific element indicating
        // successful login
        // or a redirect to an expected page.
        wait.until(driver -> driver.getPageSource().contains("Simulated login successful"));

        // After simulated login, navigate to a protected page or check for a logged-in
        // indicator
        driver.get(BASE_URL + "/"); // Go to home page
        try {
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.linkText("Logout")));
        } catch (Exception e) {
            System.err.println("Failed to confirm login after calling /test/login: " + e.getMessage());
            // Optionally, fail the test here if login confirmation is critical
            // Assert.fail("Login confirmation failed after calling /test/login");
        }
    }

    @Test
    @Order(1)
    @DisplayName("Navigate to home, then to records list, and show create form")
    void step1_navigateToCreateRecordForm() {
        ensureLoggedIn();

        driver.get(BASE_URL + "/records");
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.linkText("Add New Record"))).click();

        // Verify we are on the record form page
        wait.until(ExpectedConditions.urlContains("/records/new"));
        WebElement pageTitle = driver.findElement(By.tagName("h1"));
        assertThat(pageTitle.getText()).isEqualTo("Add New Medical Record");
    }

    @Test
    @Order(2)
    @DisplayName("Create a new medical record")
    void step2_createNewMedicalRecord() {
        // Assumes continuation from step1 or navigates if necessary
        if (!driver.getCurrentUrl().endsWith("/records/new")) {
            driver.get(BASE_URL + "/records/new");
            wait.until(ExpectedConditions.urlContains("/records/new"));
        }

        driver.findElement(By.id("name")).sendKeys(uniquePatientName);
        driver.findElement(By.id("age")).sendKeys("35");
        driver.findElement(By.id("medicalHistory")).sendKeys("E2E Test: No significant medical history.");
        driver.findElement(By.xpath("//button[@type='submit' and text()='Save']")).click();

        // Verify redirection to records list and success message
        wait.until(ExpectedConditions.urlToBe(BASE_URL + "/records"));
        WebElement successMessage = wait
                .until(ExpectedConditions.visibilityOfElementLocated(By.className("alert-success")));
        assertThat(successMessage.getText()).contains("Record successfully created.");

        // Verify the new record is in the list
        WebElement recordsTable = driver.findElement(By.className("table"));
        List<WebElement> rows = recordsTable.findElements(By.xpath(".//tbody/tr"));
        boolean found = rows.stream().anyMatch(row -> row.getText().contains(uniquePatientName));
        assertThat(found).isTrue();

        // Store the ID of the created record for later tests.
        // Note: Extracting ID from the first column text is fragile.
        // A more robust approach would be to have a data attribute (e.g.,
        // data-record-id) on the row
        // or parse it from an edit/view link if available.
        for (WebElement row : rows) {
            if (row.getText().contains(uniquePatientName)) {
                createdRecordId = row.findElement(By.xpath("./td[1]")).getText(); // First column is ID
                break;
            }
        }
        assertThat(createdRecordId).isNotNull().isNotEmpty();
    }

    @Test
    @Order(3)
    @DisplayName("Edit the created medical record")
    void step3_editMedicalRecord() {
        assertThat(createdRecordId).as("Created Record ID must be set from previous test").isNotNull();
        driver.get(BASE_URL + "/records"); // Go to list to find the edit button

        // Find the row for our specific record and click Edit
        WebElement editButton = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//tr[contains(.,'" + uniquePatientName + "')]//a[text()='Edit']")));
        editButton.click();

        // Verify we are on the edit form
        wait.until(ExpectedConditions.urlContains("/records/edit/" + createdRecordId));
        WebElement ageField = driver.findElement(By.id("age"));
        ageField.clear();
        ageField.sendKeys("37");
        driver.findElement(By.xpath("//button[@type='submit' and text()='Save']")).click();

        // Verify redirection and success message
        wait.until(ExpectedConditions.urlToBe(BASE_URL + "/records"));
        WebElement successMessage = wait
                .until(ExpectedConditions.visibilityOfElementLocated(By.className("alert-success")));
        assertThat(successMessage.getText()).contains("Record successfully updated.");

        // Verify the updated age in the list
        WebElement recordRow = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//tr[contains(.,'" + uniquePatientName + "') and contains(.,'37')]")));
        assertThat(recordRow.isDisplayed()).isTrue();
    }

    @Test
    @Order(4)
    @DisplayName("Delete the medical record")
    void step4_deleteMedicalRecord() {
        assertThat(createdRecordId).as("Created Record ID must be set from previous test").isNotNull();
        driver.get(BASE_URL + "/records");

        // Find the delete button for our specific record and click it to open the
        // confirmation modal
        WebElement deleteButtonInRow = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//tr[td[text()='" + createdRecordId + "']]//button[contains(@class, 'delete-record-btn')]")));
        deleteButtonInRow.click();

        // Wait for the modal to be visible and click the confirm delete button
        WebElement confirmDeleteButton = wait
                .until(ExpectedConditions.visibilityOfElementLocated(By.id("confirmDeleteButton")));
        // Ensure the modal body shows the correct patient name before clicking delete
        WebElement modalBody = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("recordNameToDelete")));
        assertThat(modalBody.getText()).isEqualTo(uniquePatientName);
        confirmDeleteButton.click();

        // Verify redirection and success message
        wait.until(ExpectedConditions.urlToBe(BASE_URL + "/records"));
        WebElement successMessage = wait
                .until(ExpectedConditions.visibilityOfElementLocated(By.className("alert-success")));
        assertThat(successMessage.getText()).contains("Record successfully deleted.");

        // Verify the record is no longer in the list
        driver.navigate().refresh(); // Refresh to ensure list is updated
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.className("table"))); // Wait for table
        List<WebElement> rows = driver.findElements(By.xpath(
                "//table[contains(@class, 'table')]//tbody/tr/td[contains(text(),'" + uniquePatientName + "')]"));
        assertThat(rows).isEmpty();
    }
}
