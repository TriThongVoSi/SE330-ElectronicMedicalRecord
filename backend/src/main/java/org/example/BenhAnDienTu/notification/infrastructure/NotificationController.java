package org.example.BenhAnDienTu.notification.infrastructure;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.example.BenhAnDienTu.notification.api.NotificationApi;
import org.example.BenhAnDienTu.notification.api.NotificationCommand;
import org.example.BenhAnDienTu.notification.api.NotificationReceipt;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@Validated
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

  private final NotificationApi notificationApi;

  public NotificationController(NotificationApi notificationApi) {
    this.notificationApi = notificationApi;
  }

  @PostMapping
  public NotificationReceipt dispatch(@Valid @RequestBody NotificationDispatchRequest request) {
    try {
      return notificationApi.send(request.toCommand());
    } catch (IllegalArgumentException exception) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
    }
  }

  public record NotificationDispatchRequest(
      @NotBlank String channel, @NotBlank String subject, @NotBlank String body, String recipient) {

    private NotificationCommand toCommand() {
      return new NotificationCommand(channel, subject, body, recipient);
    }
  }
}
