package com.example.framework.base;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@ExtendWith(BaseUiTest.ScreenshotOnFailureExtension.class)
public abstract class BaseUiTest {
    protected WebDriver driver;

    @BeforeEach
    void setUpDriver() {
        // HtmlUnitDriver - headless без зависимости от Chrome.
        driver = new HtmlUnitDriver(true);
        // implicitlyWait не задаю - все ожидания через WebDriverWait
    }

    @AfterEach
    void quitDriver() {
        if (driver != null) driver.quit();
    }

     // Расширение JUnit 5, которое делает скриншот, если тест упал
    static class ScreenshotOnFailureExtension implements TestWatcher {
        @Override
        public void testFailed(ExtensionContext context, Throwable cause) {
            Object testInstance = context.getRequiredTestInstance();
            if (testInstance instanceof BaseUiTest) {
                WebDriver driver = ((BaseUiTest) testInstance).driver;
                if (driver instanceof TakesScreenshot) {
                    try {
                        File screenshotDir = new File("target/screenshots");
                        if (!screenshotDir.exists()) {
                            screenshotDir.mkdirs();
                        }
                        String testName = context.getDisplayName().replaceAll("[^a-zA-Z0-9.-]", "_");
                        Path screenshotPath = Paths.get(screenshotDir.getAbsolutePath(), testName + ".png");
                        File screenshotFile = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
                        Files.move(screenshotFile.toPath(), screenshotPath, StandardCopyOption.REPLACE_EXISTING);
                        System.err.println("Screenshot saved: " + screenshotPath.toAbsolutePath());
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (Exception e) {
                        // Если скриншот не удалось сделать (например, driver уже закрыт)
                        System.err.println("Failed to capture screenshot: " + e.getMessage());
                    }
                }
            }
        }
    }
}