package org.example.BenhAnDienTu.notification.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import org.example.BenhAnDienTu.notification.api.NotificationApi;
import org.example.BenhAnDienTu.notification.api.NotificationCommand;
import org.example.BenhAnDienTu.notification.api.NotificationReceipt;
import org.example.BenhAnDienTu.shared.error.GlobalExceptionHandler;
import org.example.BenhAnDienTu.shared.logging.RequestCorrelationFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTests {

  @Mock private NotificationApi notificationApi;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    NotificationController controller = new NotificationController(notificationApi);
    mockMvc =
        MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler())
            .addFilters(new RequestCorrelationFilter())
            .build();
  }

  @Test
  void dispatchShouldReturnQueuedReceipt() throws Exception {
    NotificationReceipt receipt =
        new NotificationReceipt(
            "msg-001", "QUEUED", Instant.parse("2026-05-13T10:00:00Z"));
    when(notificationApi.send(any(NotificationCommand.class))).thenReturn(receipt);

    String request =
        """
        {
          "channel": "SYSTEM",
          "subject": "Patient Registered",
          "body": "Patient PT-0003 registered.",
          "recipient": "internal"
        }
        """;

    mockMvc
        .perform(post("/api/notifications").contentType("application/json").content(request))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.messageId").value("msg-001"))
        .andExpect(jsonPath("$.status").value("QUEUED"));

    ArgumentCaptor<NotificationCommand> commandCaptor =
        ArgumentCaptor.forClass(NotificationCommand.class);
    verify(notificationApi).send(commandCaptor.capture());
    assertThat(commandCaptor.getValue().channel()).isEqualTo("SYSTEM");
    assertThat(commandCaptor.getValue().subject()).isEqualTo("Patient Registered");
  }

  @Test
  void dispatchShouldReturnBadRequestWhenChannelUnsupported() throws Exception {
    when(notificationApi.send(any(NotificationCommand.class)))
        .thenThrow(new IllegalArgumentException("Unsupported notification channel: SMS"));

    String request =
        """
        {
          "channel": "SMS",
          "subject": "Nhac lich kham",
          "body": "Ban co lich hen ngay mai luc 9:00.",
          "recipient": "0909123456"
        }
        """;

    mockMvc
        .perform(post("/api/notifications").contentType("application/json").content(request))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Unsupported notification channel: SMS"));
  }
}
