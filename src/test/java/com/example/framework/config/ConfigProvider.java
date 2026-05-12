package com.example.framework.config;

import java.time.Duration;

public final class ConfigProvider {

    public static ApiConfig api() {
        return new ApiConfig(
                require("api.base.url"),
                Duration.ofSeconds(getInt("api.timeout.seconds", 10)));
    }

    public static UiConfig ui() {
        return new UiConfig(
                require("ui.base.url"),
                Boolean.parseBoolean(get("ui.headless", "true")),
                Duration.ofSeconds(getInt("ui.wait.seconds", 10)));
    }

    public static String get(String key, String defaultValue) {
        String v = System.getProperty(key);
        if (v != null) return v;
        v = System.getenv(key.toUpperCase().replace('.', '_'));
        return v != null ? v : defaultValue;
    }

    public static String require(String key) {
        String v = get(key, null);
        if (v == null || v.isBlank())
            throw new IllegalStateException("Missing config: " + key);
        return v;
    }

    public static int getInt(String key, int defaultValue) {
        String v = get(key, null);
        return v == null ? defaultValue : Integer.parseInt(v);
    }

    private ConfigProvider() {}
}