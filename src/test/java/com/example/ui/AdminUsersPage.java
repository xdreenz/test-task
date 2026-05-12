package com.example.ui;

import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public class AdminUsersPage {

    private final WebDriver driver;
    private final WebDriverWait wait;
    private final String baseUrl;

    public AdminUsersPage(WebDriver driver, String baseUrl, Duration timeout) {
        this.driver  = driver;
        this.baseUrl = baseUrl;
        this.wait    = new WebDriverWait(driver, timeout);
    }

    public AdminUsersPage open() {
        driver.get(baseUrl + "/admin/users");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("users-table")));
        return this;
    }

    public AdminUsersPage refresh() {
        driver.navigate().refresh();
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("users-table")));
        return this;
    }

    private By userRow(String userId) {
        return By.cssSelector("tr[data-user-id='" + userId + "']");
    }

    public boolean isUserVisible(String userId) {
        try {
            wait.until(ExpectedConditions.visibilityOfElementLocated(userRow(userId)));
            return true;
        } catch (TimeoutException e) {
            return false;
        }
    }

    public boolean isUserAbsent(String userId) {
        try {
            wait.until(ExpectedConditions.invisibilityOfElementLocated(userRow(userId)));
            return true;
        } catch (TimeoutException e) {
            return false;
        }
    }
}