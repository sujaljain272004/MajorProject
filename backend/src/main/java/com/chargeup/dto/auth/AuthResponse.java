package com.chargeup.dto.auth;

public record AuthResponse(
    String token,
    UserSummary user
) {
}
