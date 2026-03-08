package com.harrish.auth.util;

/**
 * Constants for validation rules used across DTOs and domain objects.
 * Centralized to ensure consistency and eliminate magic numbers (Clean Code principle).
 */
public final class ValidationConstants {

    private ValidationConstants() {
        // Prevent instantiation
    }

    // User validation
    public static final int NAME_MIN_LENGTH = 2;
    public static final int NAME_MAX_LENGTH = 50;
    public static final int PASSWORD_MIN_LENGTH = 8;
    public static final int PASSWORD_MAX_LENGTH = 128;

    // Blog post validation
    public static final int BLOG_TITLE_MIN_LENGTH = 3;
    public static final int BLOG_TITLE_MAX_LENGTH = 255;
    public static final int BLOG_CONTENT_MIN_LENGTH = 10;
    public static final int BLOG_CONTENT_MAX_LENGTH = 10000;

    // Validation messages
    public static final String NAME_SIZE_MESSAGE = "Name must be between " + NAME_MIN_LENGTH + " and " + NAME_MAX_LENGTH + " characters";
    public static final String PASSWORD_SIZE_MESSAGE = "Password must be between " + PASSWORD_MIN_LENGTH + " and " + PASSWORD_MAX_LENGTH + " characters";
    public static final String BLOG_TITLE_SIZE_MESSAGE = "Title must be between " + BLOG_TITLE_MIN_LENGTH + " and " + BLOG_TITLE_MAX_LENGTH + " characters";
    public static final String BLOG_CONTENT_SIZE_MESSAGE = "Content must be between " + BLOG_CONTENT_MIN_LENGTH + " and " + BLOG_CONTENT_MAX_LENGTH + " characters";
}
