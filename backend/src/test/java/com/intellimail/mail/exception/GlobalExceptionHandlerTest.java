package com.intellimail.mail.exception;

import com.intellimail.mail.util.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleEmailAlreadyExists_returns409WithMessage() {
        ResponseEntity<ApiResponse<Void>> response =
                handler.handleEmailAlreadyExists(new EmailAlreadyExistsException("dup@intellimail.com"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().success()).isFalse();
        assertThat(response.getBody().message()).contains("dup@intellimail.com");
    }

    @Test
    void handleResourceNotFound_returns404() {
        ResponseEntity<ApiResponse<Void>> response =
                handler.handleResourceNotFound(new ResourceNotFoundException("PromptTemplate", "123"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().message()).contains("PromptTemplate");
    }

    @Test
    void handleUnauthorizedAction_returns403() {
        ResponseEntity<ApiResponse<Void>> response =
                handler.handleUnauthorizedAction(new UnauthorizedActionException("Not your template"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void handleAiGenerationFailure_returns502WithGenericClientMessage() {
        ResponseEntity<ApiResponse<Void>> response =
                handler.handleAiGenerationFailure(new AiGenerationException("Azure OpenAI timed out after 3 retries"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        // Internal failure detail must not leak to the client response body.
        assertThat(response.getBody().message()).doesNotContain("Azure OpenAI");
    }

    @Test
    void handleUnexpected_returns500WithGenericMessage_neverLeakingExceptionDetail() {
        ResponseEntity<ApiResponse<Void>> response =
                handler.handleUnexpected(new RuntimeException("db password is hunter2"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().message()).doesNotContain("hunter2");
    }
}
