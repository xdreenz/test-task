package com.example.api;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.filter.Filter;
import io.restassured.filter.FilterContext;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;
import io.restassured.specification.RequestSpecification;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

import static io.restassured.RestAssured.given;

public class UserApiClient {

    private static final String USERS      = "/api/v1/users";
    private static final String USER_BY_ID = "/api/v1/users/{id}";

    private final RequestSpecification spec;

    public UserApiClient(String baseUrl) {
        // Фильтр для сохранения последнего запроса в файл
        Filter lastRequestLogger = new Filter() {
            @Override
            public Response filter(FilterableRequestSpecification requestSpec,
                                   FilterableResponseSpecification responseSpec,
                                   FilterContext ctx) {
                // Сохраняю информацию о запросе до его выполнения
                saveLastRequest(requestSpec.getMethod(), requestSpec.getURI(), requestSpec.getBody());
                return ctx.next(requestSpec, responseSpec);
            }
        };

        this.spec = new RequestSpecBuilder()
                .setBaseUri(baseUrl)
                .setContentType(ContentType.JSON)
                .addFilter(lastRequestLogger)
                .build();
    }

    public Response createUser(Map<String, Object> payload) {
        return given(spec).body(payload).post(USERS);
    }

    public Response getUserById(String userId) {
        return given(spec).pathParam("id", userId).get(USER_BY_ID);
    }

    public Response deleteUser(String userId) {
        return given(spec).pathParam("id", userId).delete(USER_BY_ID);
    }

    // Сохранение последнего запроса в target/api-logs/last-request.log
    private void saveLastRequest(String method, String uri, Object body) {
        try {
            File logDir = new File("target/api-logs");
            if (!logDir.exists()) {
                logDir.mkdirs();
            }
            try (FileWriter fw = new FileWriter(new File(logDir, "last-request.log"), false)) {
                fw.write("[" + method + "] " + uri + "\n");
                if (body != null) {
                    fw.write("Request body:\n");
                    fw.write(body.toString());
                    fw.write("\n");
                }
                fw.write("---\n");
            }
        } catch (IOException e) {
            // Не прерываю тест из-за ошибки логирования
            e.printStackTrace();
        }
    }
}