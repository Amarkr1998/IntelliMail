package com.intellimail.mail.exception;

import com.intellimail.mail.util.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Single translation point from exceptions to the {@link ApiResponse} JSON
 * envelope every client (React app, Chrome extension, Postman) already
 * expects. URL-pattern-based authorization failures (the {@code .authorizeHttpRequests(...)}
 * rules in {@code SecurityConfig}) are raised inside the filter chain and
 * handled separately by
 * {@link com.intellimail.mail.security.JwtAuthenticationEntryPoint} and
 * {@link com.intellimail.mail.security.JwtAccessDeniedHandler} — they never
 * reach this class.
 *
 * <p>Method-level {@code @PreAuthorize} denials are a different story
 * (verified against a real failing test, not assumed): they throw
 * {@link AuthorizationDeniedException} from inside the AOP proxy wrapping the
 * controller method invocation, which happens <em>inside</em>
 * {@code DispatcherServlet.doDispatch()} - resolved by this
 * {@code @RestControllerAdvice} before it ever gets a chance to escape back
 * out to the filter chain where {@code JwtAccessDeniedHandler} lives. Without
 * the handler below, every {@code @PreAuthorize} denial fell through to
 * {@link #handleUnexpected} and returned a misleading 500.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }
        log.warn("Request validation failed: {}", fieldErrors);
        return ResponseEntity.badRequest().body(ApiResponse.error("Validation failed", fieldErrors));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleConstraintViolation(ConstraintViolationException ex) {
        Map<String, String> violations = new LinkedHashMap<>();
        ex.getConstraintViolations().forEach(violation ->
                violations.put(violation.getPropertyPath().toString(), violation.getMessage()));
        log.warn("Constraint violation: {}", violations);
        return ResponseEntity.badRequest().body(ApiResponse.error("Validation failed", violations));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleMalformedJson(HttpMessageNotReadableException ex) {
        log.warn("Malformed request body: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(ApiResponse.error("Malformed JSON request body"));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingParam(MissingServletRequestParameterException ex) {
        return ResponseEntity.badRequest().body(ApiResponse.error("Missing required parameter: " + ex.getParameterName()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String message = "Invalid value for parameter '%s'".formatted(ex.getName());
        return ResponseEntity.badRequest().body(ApiResponse.error(message));
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Void>> handleEmailAlreadyExists(EmailAlreadyExistsException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidCredentials(InvalidCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidToken(InvalidTokenException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(GoogleTokenVerificationException.class)
    public ResponseEntity<ApiResponse<Void>> handleGoogleTokenVerification(GoogleTokenVerificationException ex) {
        log.warn("Google Sign-In token verification failed: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Google Sign-In failed"));
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleUserNotFound(UserNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(UnauthorizedActionException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnauthorizedAction(UnauthorizedActionException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthorizationDenied(AuthorizationDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error("Access denied: insufficient permissions"));
    }

    @ExceptionHandler(UserAlreadyInOrganizationException.class)
    public ResponseEntity<ApiResponse<Void>> handleUserAlreadyInOrganization(UserAlreadyInOrganizationException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(CannotRemoveSoleOwnerException.class)
    public ResponseEntity<ApiResponse<Void>> handleCannotRemoveSoleOwner(CannotRemoveSoleOwnerException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(OrganizationSlugTakenException.class)
    public ResponseEntity<ApiResponse<Void>> handleOrganizationSlugTaken(OrganizationSlugTakenException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(UserNotInOrganizationException.class)
    public ResponseEntity<ApiResponse<Void>> handleUserNotInOrganization(UserNotInOrganizationException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(FileProcessingException.class)
    public ResponseEntity<ApiResponse<Void>> handleFileProcessing(FileProcessingException ex) {
        log.warn("File processing failed: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException ex) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(ApiResponse.error("Uploaded file is too large. Maximum size is 10 MB."));
    }

    @ExceptionHandler(AiGenerationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAiGenerationFailure(AiGenerationException ex) {
        log.error("AI generation failed", ex);
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(ApiResponse.error("The AI service is temporarily unavailable. Please try again."));
    }

    @ExceptionHandler(NoBillingAccountException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoBillingAccount(NoBillingAccountException ex) {
        return ResponseEntity.badRequest().body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(BillingException.class)
    public ResponseEntity<ApiResponse<Void>> handleBillingFailure(BillingException ex) {
        log.error("Billing operation failed", ex);
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(ApiResponse.error("The billing service is temporarily unavailable. Please try again."));
    }

    @ExceptionHandler(AgentTaskNotAwaitingConfirmationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAgentTaskNotAwaitingConfirmation(AgentTaskNotAwaitingConfirmationException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(ExportException.class)
    public ResponseEntity<ApiResponse<Void>> handleExportFailure(ExportException ex) {
        log.error("Export failed", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Could not generate the export. Please try again."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("An unexpected error occurred. Please try again later."));
    }
}
