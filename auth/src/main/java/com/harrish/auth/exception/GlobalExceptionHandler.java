package com.harrish.auth.exception;

import com.harrish.auth.exception.error.AuthErrorCode;
import com.harrish.auth.exception.error.GenericErrorCode;
import com.harrish.auth.util.MessageResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

@RestControllerAdvice
class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private final MessageResolver messageResolver;

    GlobalExceptionHandler(MessageResolver messageResolver) {
        this.messageResolver = messageResolver;
    }

    /**
     * Builds a standardized ProblemDetail response.
     * Centralizes ProblemDetail construction to eliminate duplication.
     */
    private ResponseEntity<ProblemDetail> buildProblemDetailResponse(
            HttpStatus status,
            String message,
            HttpServletRequest request) {
        var problemDetail = ProblemDetail.forStatus(status);
        problemDetail.setTitle(status.getReasonPhrase());
        problemDetail.setDetail(message);
        problemDetail.setInstance(URI.create(request.getRequestURI()));
        problemDetail.setProperty("timestamp", LocalDateTime.now());
        return new ResponseEntity<>(problemDetail, status);
    }

    /**
     * Resolves localized message from BaseException.
     */
    private String resolveMessage(BaseException ex) {
        return messageResolver.getMessage(ex.getErrorCode(), ex.getParams());
    }

    /**
     * Resolves localized message from message key.
     */
    private String resolveMessage(String messageKey) {
        return messageResolver.getMessage(messageKey);
    }

    /**
     * Resolves message for generic exceptions (BaseException or fallback).
     */
    private String resolveExceptionMessage(Exception ex) {
        if (ex instanceof BaseException baseException) {
            return resolveMessage(baseException);
        }
        log.error("Unexpected exception type: ", ex);
        return resolveMessage(GenericErrorCode.SOMETHING_WENT_WRONG.getMessageKey());
    }

    /**
     * Formats validation errors into a comma-separated string.
     */
    private String formatValidationErrors(MethodArgumentNotValidException ex) {
        return ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> String.format("%s: %s",
                        fieldError.getField(),
                        fieldError.getDefaultMessage()))
                .collect(Collectors.joining(", "));
    }

    @ExceptionHandler({UserNotFoundException.class, UsernameNotFoundException.class, BlogPostNotFoundException.class})
    ResponseEntity<ProblemDetail> handleNotFoundExceptions(
            Exception ex, HttpServletRequest request) {
        String message = resolveExceptionMessage(ex);
        return buildProblemDetailResponse(HttpStatus.NOT_FOUND, message, request);
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    ResponseEntity<ProblemDetail> handleEmailAlreadyExistsException(
            EmailAlreadyExistsException ex, HttpServletRequest request) {
        String message = resolveMessage(ex);
        return buildProblemDetailResponse(HttpStatus.CONFLICT, message, request);
    }

    @ExceptionHandler(InvalidTokenException.class)
    ResponseEntity<ProblemDetail> handleInvalidTokenException(
            InvalidTokenException ex, HttpServletRequest request) {
        String message = resolveMessage(ex);
        return buildProblemDetailResponse(HttpStatus.UNAUTHORIZED, message, request);
    }

    @ExceptionHandler(BadCredentialsException.class)
    ResponseEntity<ProblemDetail> handleBadCredentialsException(
            BadCredentialsException ex, HttpServletRequest request) {
        log.debug("Bad credentials attempt: {}", ex.getMessage());
        String message = resolveMessage(AuthErrorCode.AUTH_BAD_CREDENTIALS.getMessageKey());
        return buildProblemDetailResponse(HttpStatus.UNAUTHORIZED, message, request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ProblemDetail> handleAccessDeniedException(
            AccessDeniedException ex, HttpServletRequest request) {
        log.debug("Access denied: {}", ex.getMessage());
        String message = resolveMessage(AuthErrorCode.AUTH_ACCESS_DENIED.getMessageKey());
        return buildProblemDetailResponse(HttpStatus.FORBIDDEN, message, request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ProblemDetail> handleIllegalArgumentException(
            IllegalArgumentException ex, HttpServletRequest request) {
        log.error("An illegal argument exception occurred while processing request: ", ex);
        return buildProblemDetailResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ProblemDetail> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        String validationErrors = formatValidationErrors(ex);
        return buildProblemDetailResponse(
                HttpStatus.BAD_REQUEST,
                "Validation error: " + validationErrors,
                request
        );
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemDetail> handleGenericException(
            Exception ex, HttpServletRequest request) {
        log.error("An exception occurred while processing the request: ", ex);
        String message = resolveMessage(GenericErrorCode.SOMETHING_WENT_WRONG.getMessageKey());
        return buildProblemDetailResponse(HttpStatus.INTERNAL_SERVER_ERROR, message, request);
    }
}
