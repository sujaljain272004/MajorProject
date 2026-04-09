package com.chargeup.dto.auth;

import com.chargeup.entity.Role;

public record UserSummary(
    Long id,
    String name,
    String email,
    Role role
) {
}
