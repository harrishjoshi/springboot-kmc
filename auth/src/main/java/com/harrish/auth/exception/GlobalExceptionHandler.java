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

@RestControllerAdvice
class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private final MessageResolver messageResolver;

    GlobalExceptionHandler(MessageResolver messageResolver) {
        this.messageResolver = messageResolver;
    }

    @ExceptionHandler({UserNotFoundException.class, UsernameNotFoundException.class, BlogPostNotFoundException.class})
    ResponseEntity<ProblemDetail> handleNotFoundExceptions(
            Exception ex, HttpServletRequest request) {
        var problemDetail = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problemDetail.setTitle(HttpStatus.NOT_FOUND.getReasonPhrase());

        if (ex instanceof BaseException baseException) {
            var localizedMessage = messageResolver.getMessage(
                    baseException.getErrorCode(), baseException.getParams());
            problemDetail.setDetail(localizedMessage);
        } else {
            var localizedMessage = messageResolver.getMessage(GenericErrorCode.SOMETHING_WENT_WRONG.getMessageKey());
            problemDetail.setDetail(localizedMessage);
            log.error("Not found exception occurred while processing the request: ", ex);
        }

        problemDetail.setInstance(URI.create(request.getRequestURI()));
        problemDetail.setProperty("timestamp", LocalDateTime.now());

        return new ResponseEntity<>(problemDetail, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    ResponseEntity<ProblemDetail> handleEmailAlreadyExistsException(
            EmailAlreadyExistsException ex, HttpServletRequest request) {
        var problemDetail = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        problemDetail.setTitle(HttpStatus.CONFLICT.getReasonPhrase());

        var localizedMessage = messageResolver.getMessage(ex.getErrorCode(), ex.getParams());
        problemDetail.setDetail(localizedMessage);
        problemDetail.setInstance(URI.create(request.getRequestURI()));
        problemDetail.setProperty("timestamp", LocalDateTime.now());

        return new ResponseEntity<>(problemDetail, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(InvalidTokenException.class)
    ResponseEntity<ProblemDetail> handleInvalidTokenException(
            InvalidTokenException ex, HttpServletRequest request) {
        var problemDetail = ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED);
        problemDetail.setTitle(HttpStatus.UNAUTHORIZED.getReasonPhrase());

        var localizedMessage = messageResolver.getMessage(ex.getErrorCode(), ex.getParams());
        problemDetail.setDetail(localizedMessage);

        problemDetail.setInstance(URI.create(request.getRequestURI()));
        problemDetail.setProperty("timestamp", LocalDateTime.now());

        return new ResponseEntity<>(problemDetail, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(BadCredentialsException.class)
    ResponseEntity<ProblemDetail> handleBadCredentialsException(HttpServletRequest request) {
        var problemDetail = ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED);
        problemDetail.setTitle(HttpStatus.UNAUTHORIZED.getReasonPhrase());

        var localizedMessage = messageResolver.getMessage(AuthErrorCode.AUTH_BAD_CREDENTIALS.getMessageKey());
        problemDetail.setDetail(localizedMessage);

        problemDetail.setInstance(URI.create(request.getRequestURI()));
        problemDetail.setProperty("timestamp", LocalDateTime.now());

        return new ResponseEntity<>(problemDetail, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ProblemDetail> handleAccessDeniedException(HttpServletRequest request) {
        var problemDetail = ProblemDetail.forStatus(HttpStatus.FORBIDDEN);
        problemDetail.setTitle(HttpStatus.FORBIDDEN.getReasonPhrase());

        var localizedMessage = messageResolver.getMessage(AuthErrorCode.AUTH_ACCESS_DENIED.getMessageKey());
        problemDetail.setDetail(localizedMessage);

        problemDetail.setInstance(URI.create(request.getRequestURI()));
        problemDetail.setProperty("timestamp", LocalDateTime.now());

        return new ResponseEntity<>(problemDetail, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ProblemDetail> handleIllegalArgumentException(
            IllegalArgumentException ex, HttpServletRequest request) {
        var problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problemDetail.setTitle(HttpStatus.BAD_REQUEST.getReasonPhrase());

        problemDetail.setDetail(ex.getMessage());
        log.error("An illegal argument exception occurred while processing request: ", ex);

        problemDetail.setInstance(URI.create(request.getRequestURI()));
        problemDetail.setProperty("timestamp", LocalDateTime.now());

        return new ResponseEntity<>(problemDetail, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ProblemDetail> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        // Collect all field errors into a single message
        var errorMessage = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .reduce("", (a, b) -> a + (a.isEmpty() ? "" : ", ") + b);

        var problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problemDetail.setTitle(HttpStatus.BAD_REQUEST.getReasonPhrase());
        problemDetail.setDetail("Validation error: " + errorMessage);
        problemDetail.setInstance(URI.create(request.getRequestURI()));
        problemDetail.setProperty("timestamp", LocalDateTime.now());

        return new ResponseEntity<>(problemDetail, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemDetail> handleGenericException(
            Exception ex, HttpServletRequest request) {
        var problemDetail = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        problemDetail.setTitle(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase());

        var localizedMessage = messageResolver.getMessage(GenericErrorCode.SOMETHING_WENT_WRONG.getMessageKey());
        problemDetail.setDetail(localizedMessage);

        log.error("An exception occurred while processing the request: ", ex);

        problemDetail.setInstance(URI.create(request.getRequestURI()));
        problemDetail.setProperty("timestamp", LocalDateTime.now());

        return new ResponseEntity<>(problemDetail, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
