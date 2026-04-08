package org.example.BenhAnDienTu.shared.error;

import java.time.Instant;
import java.util.Map;
import org.springframework.http.HttpStatus;

public record ApiErrorResponse(
    Instant timestamp,
    int status,
    String error,
    String code,
    String message,
    String path,
    String requestId,
    Map<String, String> errors) {

  public static ApiErrorResponse of(
      HttpStatus status,
      String code,
      String message,
      String path,
      String requestId,
      Map<String, String> errors) {
    return new ApiErrorResponse(
        Instant.now(),
        status.value(),
        status.getReasonPhrase(),
        code,
        message,
        path,
        requestId,
        errors == null || errors.isEmpty() ? null : Map.copyOf(errors));
  }
}
