package com.example.framework.config;

import java.time.Duration;

public record ApiConfig(String baseUrl, Duration timeout) {}