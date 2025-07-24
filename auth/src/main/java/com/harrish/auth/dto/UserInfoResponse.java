package com.harrish.auth.dto;

import java.util.Collection;

public record UserInfoResponse(
    String message,
    String username,
    Collection<?> authorities
) {
}