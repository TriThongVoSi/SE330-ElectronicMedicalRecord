package org.example.BenhAnDienTu.shared.error;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(ApiException.class)
  ResponseEntity<ApiErrorResponse> handleApiException(
      ApiException exception, HttpServletRequest request) {
    return build(
        exception.status(),
        exception.code(),
        exception.getMessage(),
        null,
        request,
        shouldLogAsError(exception.status()),
        exception);
  }

  @ExceptionHandler(ResponseStatusException.class)
  ResponseEntity<ApiErrorResponse> handleResponseStatusException(
      ResponseStatusException exception, HttpServletRequest request) {
    HttpStatus status = HttpStatus.valueOf(exception.getStatusCode().value());
    String message =
        StringUtils.hasText(exception.getReason())
            ? exception.getReason()
            : status.getReasonPhrase();
    String code = status.name();

    return build(status, code, message, null, request, shouldLogAsError(status), exception);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<ApiErrorResponse> handleMethodArgumentNotValid(
      MethodArgumentNotValidException exception, HttpServletRequest request) {
    Map<String, String> errors = new LinkedHashMap<>();
    for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
      errors.putIfAbsent(fieldError.getField(), defaultMessage(fieldError));
    }

    return build(
        HttpStatus.BAD_REQUEST,
        "VALIDATION_ERROR",
        "Request validation failed.",
        errors,
        request,
        false,
        exception);
  }

  @ExceptionHandler(BindException.class)
  ResponseEntity<ApiErrorResponse> handleBindException(
      BindException exception, HttpServletRequest request) {
    Map<String, String> errors = new LinkedHashMap<>();
    for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
      errors.putIfAbsent(fieldError.getField(), defaultMessage(fieldError));
    }

    return build(
        HttpStatus.BAD_REQUEST,
        "BIND_ERROR",
        "Request binding failed.",
        errors,
        request,
        false,
        exception);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  ResponseEntity<ApiErrorResponse> handleConstraintViolation(
      ConstraintViolationException exception, HttpServletRequest request) {
    Map<String, String> errors = new LinkedHashMap<>();
    for (ConstraintViolation<?> violation : exception.getConstraintViolations()) {
      String propertyPath =
          violation.getPropertyPath() == null ? "request" : violation.getPropertyPath().toString();
      errors.putIfAbsent(propertyPath, violation.getMessage());
    }

    return build(
        HttpStatus.BAD_REQUEST,
        "CONSTRAINT_VIOLATION",
        "Request validation failed.",
        errors,
        request,
        false,
        exception);
  }

  @ExceptionHandler(MissingServletRequestParameterException.class)
  ResponseEntity<ApiErrorResponse> handleMissingParameter(
      MissingServletRequestParameterException exception, HttpServletRequest request) {
    return build(
        HttpStatus.BAD_REQUEST,
        "MISSING_PARAMETER",
        "Missing request parameter: " + exception.getParameterName(),
        null,
        request,
        false,
        exception);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  ResponseEntity<ApiErrorResponse> handleHttpMessageNotReadable(
      HttpMessageNotReadableException exception, HttpServletRequest request) {
    return build(
        HttpStatus.BAD_REQUEST,
        "MALFORMED_REQUEST_BODY",
        "Request body is malformed or unreadable.",
        null,
        request,
        false,
        exception);
  }

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  ResponseEntity<ApiErrorResponse> handleMethodNotSupported(
      HttpRequestMethodNotSupportedException exception, HttpServletRequest request) {
    return build(
        HttpStatus.METHOD_NOT_ALLOWED,
        "METHOD_NOT_ALLOWED",
        "HTTP method is not supported for this endpoint.",
        null,
        request,
        false,
        exception);
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<ApiErrorResponse> handleUnhandledException(
      Exception exception, HttpServletRequest request) {
    return build(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "INTERNAL_SERVER_ERROR",
        "An unexpected server error occurred.",
        null,
        request,
        true,
        exception);
  }

  private ResponseEntity<ApiErrorResponse> build(
      HttpStatus status,
      String code,
      String message,
      Map<String, String> errors,
      HttpServletRequest request,
      boolean logAsError,
      Exception exception) {
    String path = request.getRequestURI();
    String requestId = resolveRequestId(request);
    ApiErrorResponse body = ApiErrorResponse.of(status, code, message, path, requestId, errors);

    if (logAsError) {
      log.error(
          "Request failed: status={}, code={}, path={}, requestId={}",
          status.value(),
          code,
          path,
          requestId,
          exception);
    } else {
      log.warn(
          "Request rejected: status={}, code={}, path={}, requestId={}, message={}",
          status.value(),
          code,
          path,
          requestId,
          message);
    }

    return ResponseEntity.status(status).body(body);
  }

  private static String defaultMessage(FieldError fieldError) {
    return Objects.requireNonNullElse(fieldError.getDefaultMessage(), "Invalid value.");
  }

  private static boolean shouldLogAsError(HttpStatus status) {
    return status.is5xxServerError();
  }

  private static String resolveRequestId(HttpServletRequest request) {
    String headerRequestId = request.getHeader("X-Request-Id");
    if (StringUtils.hasText(headerRequestId)) {
      return headerRequestId;
    }
    return MDC.get("requestId");
  }
}
