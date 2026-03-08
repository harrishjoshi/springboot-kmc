package com.harrish.auth.validation;

import com.harrish.auth.util.ValidationConstants;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.lang.annotation.*;

/**
 * Custom validation annotation for blog post content.
 * Combines @NotBlank and @Size with centralized ValidationConstants.
 * 
 * Benefits:
 * - DRY: Eliminates duplication of validation rules across DTOs
 * - Single source of truth for blog content validation
 * - Easy to update validation rules in one place
 * - More expressive than combining multiple annotations
 * - Enforces consistency across all blog-related DTOs
 * 
 * Usage:
 * <pre>
 * {@code
 * public record CreateBlogPostRequest(
 *     @ValidBlogContent
 *     String content
 * ) {}
 * }
 * </pre>
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = {})
@NotBlank(message = "Blog content is required")
@Size(
    min = ValidationConstants.BLOG_CONTENT_MIN_LENGTH,
    max = ValidationConstants.BLOG_CONTENT_MAX_LENGTH,
    message = ValidationConstants.BLOG_CONTENT_SIZE_MESSAGE
)
public @interface ValidBlogContent {
    
    String message() default "Invalid blog content";
    
    Class<?>[] groups() default {};
    
    Class<? extends Payload>[] payload() default {};
}
