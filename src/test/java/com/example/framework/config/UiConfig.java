package com.example.framework.config;

import java.time.Duration;

public record UiConfig(String baseUrl, boolean headless, Duration defaultWait) {}