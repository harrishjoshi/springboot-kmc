package com.harrish.auth.util;

/**
 * Utility class for common validation operations.
 * Provides centralized validation logic to eliminate duplication (DRY principle).
 */
public final class ValidationUtils {

    private ValidationUtils() {
        // Prevent instantiation
    }

    /**
     * Validates that an object is not null.
     *
     * @param obj       the object to validate
     * @param paramName the parameter name for the error message
     * @param <T>       the type of the object
     * @return the validated object
     * @throws IllegalArgumentException if the object is null
     */
    public static <T> T requireNonNull(T obj, String paramName) {
        if (obj == null) {
            throw new IllegalArgumentException(paramName + " must not be null");
        }
        return obj;
    }

    /**
     * Validates that a string is not null or blank.
     *
     * @param str       the string to validate
     * @param paramName the parameter name for the error message
     * @return the validated string
     * @throws IllegalArgumentException if the string is null or blank
     */
    public static String requireNonBlank(String str, String paramName) {
        if (str == null || str.isBlank()) {
            throw new IllegalArgumentException(paramName + " must not be blank");
        }
        return str;
    }
}
