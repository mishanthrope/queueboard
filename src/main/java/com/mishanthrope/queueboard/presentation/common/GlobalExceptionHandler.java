package com.mishanthrope.queueboard.presentation.common;

import com.mishanthrope.queueboard.application.auth.InvalidCredentialsError;
import com.mishanthrope.queueboard.application.common.ApplicationError;
import com.mishanthrope.queueboard.application.poster.InvalidWebhookSignatureError;
import com.mishanthrope.queueboard.domain.common.DomainError;
import com.mishanthrope.queueboard.domain.employee.DuplicateEmailError;
import com.mishanthrope.queueboard.domain.employee.EmployeeNotFoundError;
import com.mishanthrope.queueboard.domain.employee.InvalidEmployeeError;
import com.mishanthrope.queueboard.domain.order.DuplicatePosterCheckError;
import com.mishanthrope.queueboard.domain.order.IllegalStatusTransitionError;
import com.mishanthrope.queueboard.domain.order.InvalidOrderError;
import com.mishanthrope.queueboard.domain.order.OrderNotFoundError;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler({OrderNotFoundError.class, EmployeeNotFoundError.class})
    public ResponseEntity<ErrorResponse> handleNotFound(RuntimeException e) {
        return error(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler({
            IllegalStatusTransitionError.class,
            DuplicatePosterCheckError.class,
            DuplicateEmailError.class})
    public ResponseEntity<ErrorResponse> handleConflict(RuntimeException e) {
        return error(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler({InvalidOrderError.class, InvalidEmployeeError.class})
    public ResponseEntity<ErrorResponse> handleBadRequest(RuntimeException e) {
        return error(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler({InvalidCredentialsError.class, InvalidWebhookSignatureError.class})
    public ResponseEntity<ErrorResponse> handleUnauthorized(RuntimeException e) {
        return error(HttpStatus.UNAUTHORIZED, e.getMessage());
    }

    @ExceptionHandler({DomainError.class, ApplicationError.class})
    public ResponseEntity<ErrorResponse> handleUnmappedError(RuntimeException e) {
        return error(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException e) {
        log.warn("Data integrity violation", e);
        return error(HttpStatus.CONFLICT, "Request conflicts with existing data");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception e) {
        log.error("Unexpected error", e);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected server error");
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        String message = ex.getBindingResult().getAllErrors().stream()
                .map(error -> error instanceof FieldError fieldError
                        ? fieldError.getField() + ": " + fieldError.getDefaultMessage()
                        : error.getDefaultMessage())
                .sorted()
                .collect(Collectors.joining("; "));
        return handleExceptionInternal(
                ex, ProblemDetail.forStatusAndDetail(status, message), headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(
            HandlerMethodValidationException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        String message = ex.getParameterValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream()
                        .map(error -> parameterName(result) + ": " + error.getDefaultMessage()))
                .sorted()
                .collect(Collectors.joining("; "));
        return handleExceptionInternal(
                ex, ProblemDetail.forStatusAndDetail(status, message), headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                status, "Malformed JSON request or invalid field value");
        return handleExceptionInternal(ex, problem, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex,
            Object body,
            HttpHeaders headers,
            HttpStatusCode statusCode,
            WebRequest request) {

        ResponseEntity<Object> response =
                super.handleExceptionInternal(ex, body, headers, statusCode, request);
        HttpStatus status = HttpStatus.valueOf(statusCode.value());
        String message = response != null
                && response.getBody() instanceof ProblemDetail problem
                && problem.getDetail() != null
                ? problem.getDetail()
                : status.getReasonPhrase();
        return ResponseEntity.status(statusCode).headers(headers)
                .body(ErrorResponse.of(status, message));
    }

    private static String parameterName(ParameterValidationResult result) {
        String name = result.getMethodParameter().getParameterName();
        return name != null ? name : "parameter";
    }

    private static ResponseEntity<ErrorResponse> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(ErrorResponse.of(status, message));
    }
}
