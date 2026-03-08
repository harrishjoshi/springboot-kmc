package com.harrish.auth.validation;

import com.harrish.auth.util.ValidationConstants;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.lang.annotation.*;

/**
 * Custom validation annotation for blog post titles.
 * Combines @NotBlank and @Size with centralized ValidationConstants.
 * 
 * Benefits:
 * - DRY: Eliminates duplication of validation rules across DTOs
 * - Single source of truth for blog title validation
 * - Easy to update validation rules in one place
 * - More expressive than combining multiple annotations
 * - Enforces consistency across all blog-related DTOs
 * 
 * Usage:
 * <pre>
 * {@code
 * public record CreateBlogPostRequest(
 *     @ValidBlogTitle
 *     String title
 * ) {}
 * }
 * </pre>
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = {})
@NotBlank(message = "Blog title is required")
@Size(
    min = ValidationConstants.BLOG_TITLE_MIN_LENGTH,
    max = ValidationConstants.BLOG_TITLE_MAX_LENGTH,
    message = ValidationConstants.BLOG_TITLE_SIZE_MESSAGE
)
public @interface ValidBlogTitle {
    
    String message() default "Invalid blog title";
    
    Class<?>[] groups() default {};
    
    Class<? extends Payload>[] payload() default {};
}
