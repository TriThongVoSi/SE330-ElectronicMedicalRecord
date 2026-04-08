package org.example.BenhAnDienTu.shared.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestCorrelationFilter extends OncePerRequestFilter {

  public static final String REQUEST_ID_HEADER = "X-Request-Id";
  private static final String MDC_REQUEST_ID_KEY = "requestId";
  private static final int MAX_REQUEST_ID_LENGTH = 64;

  private static final Logger log = LoggerFactory.getLogger(RequestCorrelationFilter.class);

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String requestId = resolveRequestId(request.getHeader(REQUEST_ID_HEADER));
    MDC.put(MDC_REQUEST_ID_KEY, requestId);
    response.setHeader(REQUEST_ID_HEADER, requestId);

    long startNanos = System.nanoTime();

    try {
      filterChain.doFilter(request, response);
    } finally {
      long elapsedMs = Duration.ofNanos(System.nanoTime() - startNanos).toMillis();
      log.info(
          "{} {} -> {} ({} ms)",
          request.getMethod(),
          request.getRequestURI(),
          response.getStatus(),
          elapsedMs);
      MDC.remove(MDC_REQUEST_ID_KEY);
    }
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return request.getRequestURI().startsWith("/actuator");
  }

  private static String resolveRequestId(String candidate) {
    if (StringUtils.hasText(candidate)) {
      String trimmed = candidate.trim();
      if (trimmed.length() <= MAX_REQUEST_ID_LENGTH) {
        return trimmed;
      }
      return trimmed.substring(0, MAX_REQUEST_ID_LENGTH);
    }
    return UUID.randomUUID().toString();
  }
}
