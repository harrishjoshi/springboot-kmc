package com.harrish.auth.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

@Validated
@ConfigurationProperties(prefix = "jwt")
data class JwtProperties(
    val secretKey: String,
    val expiration: Long,
    val refreshExpiration: Long
)
