package org.example.BenhAnDienTu.appointment.infrastructure;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.example.BenhAnDienTu.appointment.api.AppointmentPageView;
import org.example.BenhAnDienTu.appointment.api.PatientPortalAppointmentCancelCommand;
import org.example.BenhAnDienTu.appointment.api.PatientPortalAppointmentCreateCommand;
import org.example.BenhAnDienTu.appointment.api.PatientPortalAppointmentRescheduleCommand;
import org.example.BenhAnDienTu.appointment.api.PatientPortalAppointmentView;
import org.example.BenhAnDienTu.appointment.api.PatientPortalAvailableSlotView;
import org.example.BenhAnDienTu.appointment.api.PatientPortalDoctorView;
import org.example.BenhAnDienTu.appointment.application.PatientPortalAppointmentService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/patient-portal")
public class PatientPortalAppointmentController {

  private final PatientPortalAppointmentService appointmentService;

  public PatientPortalAppointmentController(PatientPortalAppointmentService appointmentService) {
    this.appointmentService = appointmentService;
  }

  @GetMapping("/appointments")
  public AppointmentPageView listMyAppointments(
      @RequestAttribute(name = "actorId") String actorId,
      @RequestParam(defaultValue = "1") @Min(1) int page,
      @RequestParam(defaultValue = "10") @Min(1) int size,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) LocalDate date) {
    return appointmentService.listMyAppointments(actorId, status, page, size, date);
  }

  @GetMapping("/appointments/{appointmentId}")
  public PatientPortalAppointmentView getMyAppointment(
      @RequestAttribute(name = "actorId") String actorId,
      @PathVariable("appointmentId") String appointmentId) {
    return appointmentService.getMyAppointment(actorId, appointmentId);
  }

  @GetMapping("/doctors")
  public List<PatientPortalDoctorView> listBookableDoctors() {
    return appointmentService.listBookableDoctors();
  }

  @GetMapping("/available-slots")
  public List<PatientPortalAvailableSlotView> listAvailableSlots(
      @RequestParam String doctorId,
      @RequestParam(required = false) LocalDate fromDate,
      @RequestParam(required = false) LocalDate toDate) {
    return appointmentService.listAvailableSlots(doctorId, fromDate, toDate);
  }

  @PostMapping("/appointments")
  public PatientPortalAppointmentView createAppointment(
      @RequestAttribute(name = "actorId") String actorId,
      @Valid @RequestBody PatientPortalCreateAppointmentRequest request) {
    return appointmentService.createAppointmentForSelf(actorId, request.toCommand());
  }

  @PutMapping("/appointments/{appointmentId}/reschedule")
  public PatientPortalAppointmentView rescheduleAppointment(
      @RequestAttribute(name = "actorId") String actorId,
      @PathVariable("appointmentId") String appointmentId,
      @Valid @RequestBody PatientPortalRescheduleAppointmentRequest request) {
    return appointmentService.rescheduleAppointmentForSelf(actorId, appointmentId, request.toCommand());
  }

  @PostMapping("/appointments/{appointmentId}/cancel")
  public PatientPortalAppointmentView cancelAppointment(
      @RequestAttribute(name = "actorId") String actorId,
      @PathVariable("appointmentId") String appointmentId,
      @Valid @RequestBody PatientPortalCancelAppointmentRequest request) {
    return appointmentService.cancelAppointmentForSelf(actorId, appointmentId, request.toCommand());
  }

  public record PatientPortalCreateAppointmentRequest(
      @NotBlank String doctorId, Instant appointmentTime, List<String> serviceIds, String note) {

    private PatientPortalAppointmentCreateCommand toCommand() {
      return new PatientPortalAppointmentCreateCommand(
          doctorId, appointmentTime, serviceIds == null ? List.of() : serviceIds, note);
    }
  }

  public record PatientPortalRescheduleAppointmentRequest(
      @NotBlank String doctorId, Instant appointmentTime, List<String> serviceIds) {

    private PatientPortalAppointmentRescheduleCommand toCommand() {
      return new PatientPortalAppointmentRescheduleCommand(
          doctorId, appointmentTime, serviceIds == null ? List.of() : serviceIds);
    }
  }

  public record PatientPortalCancelAppointmentRequest(@NotBlank String cancelReason) {

    private PatientPortalAppointmentCancelCommand toCommand() {
      return new PatientPortalAppointmentCancelCommand(cancelReason.trim());
    }
  }
}
