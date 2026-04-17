package org.example.BenhAnDienTu.appointment.infrastructure;

import static org.springframework.http.HttpStatus.NOT_FOUND;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.example.BenhAnDienTu.appointment.api.AppointmentApi;
import org.example.BenhAnDienTu.appointment.api.AppointmentBookingCommand;
import org.example.BenhAnDienTu.appointment.api.AppointmentListQuery;
import org.example.BenhAnDienTu.appointment.api.AppointmentPageView;
import org.example.BenhAnDienTu.appointment.api.AppointmentView;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@Validated
@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

  private final AppointmentApi appointmentApi;

  public AppointmentController(AppointmentApi appointmentApi) {
    this.appointmentApi = appointmentApi;
  }

  @GetMapping
  public AppointmentPageView listAppointments(
      @RequestParam(defaultValue = "1") @Min(1) int page,
      @RequestParam(defaultValue = "10") @Min(1) int size,
      @RequestParam(required = false) String search,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) String doctorId,
      @RequestParam(required = false) String patientId,
      @RequestParam(required = false) LocalDate date) {
    return appointmentApi.listAppointments(
        new AppointmentListQuery(page, size, search, status, doctorId, patientId, date));
  }

  @GetMapping("/{id}")
  public AppointmentView getAppointment(@PathVariable("id") String id) {
    return appointmentApi
        .findAppointment(id)
        .orElseThrow(
            () -> new ResponseStatusException(NOT_FOUND, "Appointment does not exist: " + id));
  }

  @PostMapping
  public AppointmentView createAppointment(@Valid @RequestBody AppointmentUpsertRequest request) {
    return appointmentApi.bookAppointment(request.toCommand());
  }

  @PutMapping("/{id}")
  public AppointmentView updateAppointment(
      @PathVariable("id") String id, @Valid @RequestBody AppointmentUpsertRequest request) {
    return appointmentApi.updateAppointment(id, request.toCommand());
  }

  public record AppointmentUpsertRequest(
      @NotBlank String appointmentCode,
      Instant appointmentTime,
      @NotBlank String status,
      String cancelReason,
      @NotBlank String doctorId,
      @NotBlank String patientId,
      @Min(1) @Max(5) int urgencyLevel,
      @NotBlank String prescriptionStatus,
      boolean isFollowup,
      @Min(1) @Max(10) Integer priorityScore,
      List<String> serviceIds) {

    private AppointmentBookingCommand toCommand() {
      return new AppointmentBookingCommand(
          appointmentCode,
          appointmentTime,
          status,
          cancelReason,
          doctorId,
          patientId,
          urgencyLevel,
          prescriptionStatus,
          isFollowup,
          priorityScore,
          serviceIds == null ? List.of() : serviceIds);
    }
  }
}
