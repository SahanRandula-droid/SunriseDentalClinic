package com.sunrisedental.dto;

public record LoginRequest(String username, String password, Boolean rememberMe) {
}
