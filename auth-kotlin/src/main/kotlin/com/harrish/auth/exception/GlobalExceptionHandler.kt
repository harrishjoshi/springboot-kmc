package com.harrish.auth.exception

import com.harrish.auth.exception.error.AuthErrorCode
import com.harrish.auth.exception.error.GenericErrorCode
import com.harrish.auth.util.MessageResolver
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.net.URI
import java.time.LocalDateTime

@RestControllerAdvice
class GlobalExceptionHandler(
    private val messageResolver: MessageResolver
) {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)
    }

    @ExceptionHandler(UserNotFoundException::class, UsernameNotFoundException::class, BlogPostNotFoundException::class)
    fun handleNotFoundExceptions(ex: Exception, request: HttpServletRequest): ResponseEntity<ProblemDetail> {
        val problemDetail = ProblemDetail.forStatus(HttpStatus.NOT_FOUND)
        problemDetail.title = HttpStatus.NOT_FOUND.reasonPhrase

        val localizedMessage = if (ex is BaseException) {
            messageResolver.getMessage(ex.errorCode, *ex.params)
        } else {
            messageResolver.getMessage(GenericErrorCode.SOMETHING_WENT_WRONG.messageKey)
            log.error("Not found exception occurred while processing the request: ", ex)
        }
        problemDetail.detail = localizedMessage

        problemDetail.instance = URI.create(request.requestURI)
        problemDetail.setProperty("timestamp", LocalDateTime.now())

        return ResponseEntity(problemDetail, HttpStatus.NOT_FOUND)
    }

    @ExceptionHandler(EmailAlreadyExistsException::class)
    fun handleEmailAlreadyExistsException(ex: EmailAlreadyExistsException, request: HttpServletRequest): ResponseEntity<ProblemDetail> {
        val problemDetail = ProblemDetail.forStatus(HttpStatus.CONFLICT)
        problemDetail.title = HttpStatus.CONFLICT.reasonPhrase

        val localizedMessage = messageResolver.getMessage(ex.errorCode, *ex.params)
        problemDetail.detail = localizedMessage
        problemDetail.instance = URI.create(request.requestURI)
        problemDetail.setProperty("timestamp", LocalDateTime.now())

        return ResponseEntity(problemDetail, HttpStatus.CONFLICT)
    }

    @ExceptionHandler(InvalidTokenException::class)
    fun handleInvalidTokenException(ex: InvalidTokenException, request: HttpServletRequest): ResponseEntity<ProblemDetail> {
        val problemDetail = ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED)
        problemDetail.title = HttpStatus.UNAUTHORIZED.reasonPhrase

        val localizedMessage = messageResolver.getMessage(ex.errorCode, *ex.params)
        problemDetail.detail = localizedMessage

        problemDetail.instance = URI.create(request.requestURI)
        problemDetail.setProperty("timestamp", LocalDateTime.now())

        return ResponseEntity(problemDetail, HttpStatus.UNAUTHORIZED)
    }

    @ExceptionHandler(BadCredentialsException::class)
    fun handleBadCredentialsException(request: HttpServletRequest): ResponseEntity<ProblemDetail> {
        val problemDetail = ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED)
        problemDetail.title = HttpStatus.UNAUTHORIZED.reasonPhrase

        val localizedMessage = messageResolver.getMessage(AuthErrorCode.AUTH_BAD_CREDENTIALS.messageKey)
        problemDetail.detail = localizedMessage

        problemDetail.instance = URI.create(request.requestURI)
        problemDetail.setProperty("timestamp", LocalDateTime.now())

        return ResponseEntity(problemDetail, HttpStatus.UNAUTHORIZED)
    }

    @ExceptionHandler(AccessDeniedException::class)
    fun handleAccessDeniedException(request: HttpServletRequest): ResponseEntity<ProblemDetail> {
        val problemDetail = ProblemDetail.forStatus(HttpStatus.FORBIDDEN)
        problemDetail.title = HttpStatus.FORBIDDEN.reasonPhrase

        val localizedMessage = messageResolver.getMessage(AuthErrorCode.AUTH_ACCESS_DENIED.messageKey)
        problemDetail.detail = localizedMessage

        problemDetail.instance = URI.create(request.requestURI)
        problemDetail.setProperty("timestamp", LocalDateTime.now())

        return ResponseEntity(problemDetail, HttpStatus.FORBIDDEN)
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(ex: IllegalArgumentException, request: HttpServletRequest): ResponseEntity<ProblemDetail> {
        val problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST)
        problemDetail.title = HttpStatus.BAD_REQUEST.reasonPhrase

        problemDetail.detail = ex.message
        log.error("An illegal argument exception occurred while processing request: ", ex)

        problemDetail.instance = URI.create(request.requestURI)
        problemDetail.setProperty("timestamp", LocalDateTime.now())

        return ResponseEntity(problemDetail, HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleMethodArgumentNotValidException(ex: MethodArgumentNotValidException, request: HttpServletRequest): ResponseEntity<ProblemDetail> {
        val errorMessage = ex.bindingResult.fieldErrors
            .joinToString(", ") { "${it.field}: ${it.defaultMessage}" }

        val problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST)
        problemDetail.title = HttpStatus.BAD_REQUEST.reasonPhrase
        problemDetail.detail = "Validation error: $errorMessage"
        problemDetail.instance = URI.create(request.requestURI)
        problemDetail.setProperty("timestamp", LocalDateTime.now())

        return ResponseEntity(problemDetail, HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(Exception::class)
    fun handleGenericException(ex: Exception, request: HttpServletRequest): ResponseEntity<ProblemDetail> {
        val problemDetail = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR)
        problemDetail.title = HttpStatus.INTERNAL_SERVER_ERROR.reasonPhrase

        val localizedMessage = messageResolver.getMessage(GenericErrorCode.SOMETHING_WENT_WRONG.messageKey)
        problemDetail.detail = localizedMessage

        log.error("An exception occurred while processing the request: ", ex)

        problemDetail.instance = URI.create(request.requestURI)
        problemDetail.setProperty("timestamp", LocalDateTime.now())

        return ResponseEntity(problemDetail, HttpStatus.INTERNAL_SERVER_ERROR)
    }
}
