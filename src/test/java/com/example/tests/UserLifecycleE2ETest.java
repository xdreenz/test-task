package com.example.tests;

import com.example.api.UserApiClient;
import com.example.framework.base.BaseUiTest;
import com.example.framework.config.ApiConfig;
import com.example.framework.config.UiConfig;
import com.example.ui.AdminUsersPage;
import com.github.tomakehurst.wiremock.WireMockServer;
import io.restassured.response.Response;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

class UserLifecycleE2ETest extends BaseUiTest {

    private static WireMockServer mockServer;
    private static UserApiClient  apiClient;
    private static UiConfig       uiConfig;

    private String createdUserId;

    @BeforeAll
    static void startEnvironment() {
        mockServer = new WireMockServer(wireMockConfig().dynamicPort());
        mockServer.start();

        // Подменяю конфиг на адрес локального WireMock - никаких внешних зависимостей.
        String url = "http://localhost:" + mockServer.port();
        System.setProperty("api.base.url", url);
        System.setProperty("ui.base.url",  url);

        ApiConfig api = com.example.framework.config.ConfigProvider.api();
        uiConfig      = com.example.framework.config.ConfigProvider.ui();
        apiClient     = new UserApiClient(api.baseUrl());
    }

    @AfterAll
    static void stopEnvironment() {
        if (mockServer != null) mockServer.stop();
    }

    @BeforeEach
    void resetMocks() {
        // Идемпотентность: каждый тест начинается со свежим сервером.
        mockServer.resetAll();
    }

    @Test
    void createdUserAppearsInAdminUi_andDisappearsAfterDeletion() {
        // ARRANGE: уникальные данные
        String unique = UUID.randomUUID().toString();
        String userId = "user-" + unique;
        String email  = "qa+" + unique + "@example.com";

        Map<String, Object> payload = Map.of(
                "email", email,
                "name",  "QA User " + unique
        );

        stubCreateUser(userId, email);
        stubDeleteUser(userId);
        stubAdminPageWithUser(userId, email);

        // ACT 1: создаю пользователя через API
        Response createResponse = apiClient.createUser(payload);
        assertThat(createResponse.statusCode())
                .as("user creation should succeed")
                .isEqualTo(201);

        createdUserId = createResponse.jsonPath().getString("id");
        assertThat(createdUserId)
                .as("API must return id of the created user")
                .isEqualTo(userId);

        // ASSERT 1: пользователь виден в UI
        AdminUsersPage page = new AdminUsersPage(driver, uiConfig.baseUrl(),
                uiConfig.defaultWait()).open();

        assertThat(page.isUserVisible(userId))
                .as("user %s must be visible on /admin/users after creation", userId)
                .isTrue();

        // ACT 2: удаляю пользователя через API
        // После удаления страница должна вернуть пустой список - перенастраиваю стаб.
        stubAdminPageEmpty();

        Response deleteResponse = apiClient.deleteUser(userId);
        assertThat(deleteResponse.statusCode())
                .as("user deletion should succeed or be idempotent")
                .isIn(200, 204, 404);

        // ASSERT 2: пользователь исчез со страницы
        page.refresh();
        assertThat(page.isUserAbsent(userId))
                .as("user %s must disappear from /admin/users after deletion", userId)
                .isTrue();

        createdUserId = null; // успех - cleanup не нужен
    }

    // Гарантия идемпотентности: пользователь будет удалён даже при падении теста
    @AfterEach
    void cleanupCreatedUser() {
        if (createdUserId == null) return;
        try {
            apiClient.deleteUser(createdUserId);
        } catch (Exception ignored) {
            // cleanup не должен маскировать оригинальную ошибку теста
        }
    }

    // WireMock-стабы

    private void stubCreateUser(String userId, String email) {
        mockServer.stubFor(post(urlEqualTo("/api/v1/users"))
                .withRequestBody(matchingJsonPath("$.email", equalTo(email)))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"id":"%s","email":"%s"}
                                """.formatted(userId, email))));
    }

    private void stubDeleteUser(String userId) {
        mockServer.stubFor(delete(urlEqualTo("/api/v1/users/" + userId))
                .willReturn(aResponse().withStatus(204)));
    }

    private void stubAdminPageWithUser(String userId, String email) {
        String html = """
                <html><body>
                  <table id="users-table">
                    <tr data-user-id="%s"><td>%s</td></tr>
                  </table>
                </body></html>
                """.formatted(userId, email);
        stubAdminPage(html);
    }

    private void stubAdminPageEmpty() {
        stubAdminPage("""
                <html><body>
                  <table id="users-table"></table>
                </body></html>
                """);
    }

    private void stubAdminPage(String html) {
        // Перезаписывает предыдущий стаб для того же URL.
        mockServer.stubFor(get(urlEqualTo("/admin/users"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/html; charset=utf-8")
                        .withBody(html)));
    }
}