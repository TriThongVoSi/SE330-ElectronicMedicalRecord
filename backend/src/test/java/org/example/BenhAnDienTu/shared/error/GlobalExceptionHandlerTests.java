package org.example.BenhAnDienTu.shared.error;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.example.BenhAnDienTu.shared.logging.RequestCorrelationFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

class GlobalExceptionHandlerTests {

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc =
        MockMvcBuilders.standaloneSetup(new ThrowingController())
            .setControllerAdvice(new GlobalExceptionHandler())
            .addFilters(new RequestCorrelationFilter())
            .build();
  }

  @Test
  void shouldReturnStandardizedApiErrorWithRequestId() throws Exception {
    mockMvc
        .perform(get("/api/test/api-error"))
        .andExpect(status().isBadRequest())
        .andExpect(header().exists(RequestCorrelationFilter.REQUEST_ID_HEADER))
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.code").value("TEST_ERROR"))
        .andExpect(jsonPath("$.message").value("Triggered test exception"))
        .andExpect(jsonPath("$.path").value("/api/test/api-error"))
        .andExpect(jsonPath("$.requestId").isNotEmpty());
  }

  @RestController
  static class ThrowingController {

    @GetMapping("/api/test/api-error")
    String throwApiException() {
      throw new ApiException(HttpStatus.BAD_REQUEST, "TEST_ERROR", "Triggered test exception");
    }
  }
}
