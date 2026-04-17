package org.example.BenhAnDienTu.appointment.application;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.example.BenhAnDienTu.appointment.api.AppointmentApi;
import org.example.BenhAnDienTu.appointment.api.AppointmentBookingCommand;
import org.example.BenhAnDienTu.appointment.api.AppointmentListQuery;
import org.example.BenhAnDienTu.appointment.api.AppointmentPageView;
import org.example.BenhAnDienTu.appointment.api.AppointmentView;
import org.example.BenhAnDienTu.appointment.api.PatientPortalAppointmentCancelCommand;
import org.example.BenhAnDienTu.appointment.api.PatientPortalAppointmentCreateCommand;
import org.example.BenhAnDienTu.appointment.api.PatientPortalAppointmentRescheduleCommand;
import org.example.BenhAnDienTu.appointment.api.PatientPortalAppointmentView;
import org.example.BenhAnDienTu.appointment.api.PatientPortalAvailableSlotView;
import org.example.BenhAnDienTu.appointment.api.PatientPortalDoctorView;
import org.example.BenhAnDienTu.appointment.infrastructure.AppointmentLedgerAdapter;
import org.example.BenhAnDienTu.patient.api.PatientApi;
import org.example.BenhAnDienTu.patient.api.PatientView;
import org.example.BenhAnDienTu.staff.api.StaffApi;
import org.example.BenhAnDienTu.staff.api.StaffMemberView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PatientPortalAppointmentService {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(PatientPortalAppointmentService.class);
  private static final DateTimeFormatter APPOINTMENT_CODE_DATE =
      DateTimeFormatter.ofPattern("yyyyMMdd", Locale.ROOT);

  private final AppointmentApi appointmentApi;
  private final AppointmentLedgerAdapter ledgerAdapter;
  private final PatientApi patientApi;
  private final StaffApi staffApi;
  private final JavaMailSender mailSender;
  private final Clock clock;

  @Value("${spring.mail.host:}")
  private String smtpHost;

  @Value("${app.mail.from:no-reply@example.com}")
  private String senderEmail;

  @Value("${app.mail.enabled:true}")
  private boolean mailEnabled;

  @Value("${app.mail.dev-fallback-enabled:true}")
  private boolean devFallbackEnabled;

  public PatientPortalAppointmentService(
      AppointmentApi appointmentApi,
      AppointmentLedgerAdapter ledgerAdapter,
      PatientApi patientApi,
      StaffApi staffApi,
      JavaMailSender mailSender,
      Clock clock) {
    this.appointmentApi = appointmentApi;
    this.ledgerAdapter = ledgerAdapter;
    this.patientApi = patientApi;
    this.staffApi = staffApi;
    this.mailSender = mailSender;
    this.clock = clock;
  }

  public AppointmentPageView listMyAppointments(
      String actorId, String status, int page, int size, LocalDate date) {
    String patientId = resolvePatientId(actorId);
    return appointmentApi.listAppointments(
        new AppointmentListQuery(page, size, null, normalizeNullable(status), null, patientId, date));
  }

  public PatientPortalAppointmentView getMyAppointment(String actorId, String appointmentId) {
    String patientId = resolvePatientId(actorId);
    AppointmentView appointment = findOwnedAppointment(patientId, appointmentId);
    return toPatientPortalView(appointment);
  }

  public List<PatientPortalDoctorView> listBookableDoctors() {
    return staffApi.listDoctors().stream().map(PatientPortalAppointmentService::toDoctorView).toList();
  }

  public List<PatientPortalAvailableSlotView> listAvailableSlots(
      String doctorId, LocalDate fromDate, LocalDate toDate) {
    if (normalizeNullable(doctorId) == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Doctor id is required.");
    }

    LocalDate from = fromDate == null ? LocalDate.now(clock) : fromDate;
    LocalDate to = toDate == null ? from.plusDays(14) : toDate;

    if (to.isBefore(from)) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Slot range is invalid: end date must be after start date.");
    }

    if (to.isAfter(from.plusDays(31))) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Slot range cannot exceed 31 days for one request.");
    }

    return ledgerAdapter.listAvailableSlots(doctorId, from, to);
  }

  public PatientPortalAppointmentView createAppointmentForSelf(
      String actorId, PatientPortalAppointmentCreateCommand command) {
    String patientId = resolvePatientId(actorId);
    PatientView patient = loadPatient(patientId);
    Instant appointmentTime = requireFutureTime(command.appointmentTime());
    String doctorId = normalizeRequired(command.doctorId(), "Doctor id is required.");

    ensureDoctorExists(doctorId);
    if (!ledgerAdapter.isSlotAvailable(doctorId, appointmentTime)) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Selected slot is not available for booking.");
    }

    AppointmentView created =
        appointmentApi.bookAppointment(
            new AppointmentBookingCommand(
                generateAppointmentCode(appointmentTime),
                appointmentTime,
                "COMING",
                null,
                doctorId,
                patientId,
                1,
                "NONE",
                false,
                5,
                normalizeServiceIds(command.serviceIds())));

    sendPatientEmail(
        patient,
        "Appointment booked",
        """
        Your appointment has been booked.
        Appointment code: %s
        Appointment time: %s
        Doctor: %s
        """
            .formatted(created.appointmentCode(), created.appointmentTime(), created.doctorName()));

    return toPatientPortalView(created);
  }

  public PatientPortalAppointmentView rescheduleAppointmentForSelf(
      String actorId, String appointmentId, PatientPortalAppointmentRescheduleCommand command) {
    String patientId = resolvePatientId(actorId);
    PatientView patient = loadPatient(patientId);
    AppointmentView existing = findOwnedAppointment(patientId, appointmentId);
    assertCanReschedule(existing);

    String doctorId = normalizeRequired(command.doctorId(), "Doctor id is required.");
    Instant appointmentTime = requireFutureTime(command.appointmentTime());
    ensureDoctorExists(doctorId);

    if (!ledgerAdapter.isSlotAvailableForAppointment(doctorId, appointmentTime, appointmentId)) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Selected slot is not available for reschedule.");
    }

    List<String> serviceIds =
        command.serviceIds() == null || command.serviceIds().isEmpty()
            ? existing.serviceIds()
            : normalizeServiceIds(command.serviceIds());

    AppointmentView updated =
        appointmentApi.updateAppointment(
            appointmentId,
            new AppointmentBookingCommand(
                existing.appointmentCode(),
                appointmentTime,
                "COMING",
                null,
                doctorId,
                patientId,
                existing.urgencyLevel(),
                existing.prescriptionStatus(),
                existing.isFollowup(),
                existing.priorityScore(),
                serviceIds));

    sendPatientEmail(
        patient,
        "Appointment rescheduled",
        """
        Your appointment has been rescheduled.
        Appointment code: %s
        New appointment time: %s
        Doctor: %s
        """
            .formatted(updated.appointmentCode(), updated.appointmentTime(), updated.doctorName()));

    return toPatientPortalView(updated);
  }

  public PatientPortalAppointmentView cancelAppointmentForSelf(
      String actorId, String appointmentId, PatientPortalAppointmentCancelCommand command) {
    String patientId = resolvePatientId(actorId);
    PatientView patient = loadPatient(patientId);
    AppointmentView existing = findOwnedAppointment(patientId, appointmentId);
    assertCanCancel(existing);

    String cancelReason = normalizeRequired(command.cancelReason(), "Cancel reason is required.");

    AppointmentView cancelled =
        appointmentApi.updateAppointment(
            appointmentId,
            new AppointmentBookingCommand(
                existing.appointmentCode(),
                existing.appointmentTime(),
                "CANCEL",
                cancelReason,
                existing.doctorId(),
                patientId,
                existing.urgencyLevel(),
                existing.prescriptionStatus(),
                existing.isFollowup(),
                existing.priorityScore(),
                existing.serviceIds()));

    sendPatientEmail(
        patient,
        "Appointment cancelled",
        """
        Your appointment has been cancelled.
        Appointment code: %s
        Cancel reason: %s
        """
            .formatted(cancelled.appointmentCode(), cancelReason));

    return toPatientPortalView(cancelled);
  }

  private String resolvePatientId(String actorId) {
    return patientApi
        .findPatientIdByActorId(actorId)
        .orElseThrow(
            () ->
                new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Current account is not linked to a patient profile."));
  }

  private AppointmentView findOwnedAppointment(String patientId, String appointmentId) {
    AppointmentView appointment =
        appointmentApi
            .findAppointment(appointmentId)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Appointment does not exist: " + appointmentId));

    if (!patientId.equals(appointment.patientId())) {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "Cannot access appointment outside current patient scope.");
    }
    return appointment;
  }

  private void ensureDoctorExists(String doctorId) {
    if (staffApi.findStaffMember(doctorId).isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Doctor does not exist.");
    }
  }

  private Instant requireFutureTime(Instant appointmentTime) {
    if (appointmentTime == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Appointment time is required.");
    }
    if (!appointmentTime.isAfter(Instant.now(clock))) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Appointment time must be in the future.");
    }
    return appointmentTime;
  }

  private void assertCanReschedule(AppointmentView appointment) {
    if ("CANCEL".equalsIgnoreCase(appointment.status())
        || "FINISH".equalsIgnoreCase(appointment.status())) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Only active appointments can be rescheduled.");
    }

    if (!appointment.appointmentTime().isAfter(Instant.now(clock))) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Cannot reschedule appointment after visit time.");
    }
  }

  private void assertCanCancel(AppointmentView appointment) {
    if ("CANCEL".equalsIgnoreCase(appointment.status())) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Appointment is already cancelled.");
    }

    if ("FINISH".equalsIgnoreCase(appointment.status())
        || !appointment.appointmentTime().isAfter(Instant.now(clock))) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Cannot cancel appointment after visit time.");
    }
  }

  private PatientView loadPatient(String patientId) {
    return patientApi
        .findPatient(patientId)
        .orElseThrow(
            () ->
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Patient does not exist for current account."));
  }

  private void sendPatientEmail(PatientView patient, String subject, String body) {
    if (normalizeNullable(patient.email()) == null || !mailEnabled || !StringUtils.hasText(smtpHost)) {
      return;
    }

    SimpleMailMessage message = new SimpleMailMessage();
    message.setFrom(senderEmail);
    message.setTo(patient.email());
    message.setSubject(subject);
    message.setText(body);

    try {
      mailSender.send(message);
    } catch (MailException exception) {
      if (devFallbackEnabled) {
        LOGGER.warn(
            "Patient email delivery failed. Safe dev fallback used for {}.",
            maskRecipient(patient.email()),
            exception);
        return;
      }
      throw exception;
    }
  }

  private static PatientPortalDoctorView toDoctorView(StaffMemberView doctor) {
    return new PatientPortalDoctorView(
        doctor.id(), doctor.fullName(), doctor.email(), normalizeNullable(doctor.phone()));
  }

  private static PatientPortalAppointmentView toPatientPortalView(AppointmentView appointment) {
    return new PatientPortalAppointmentView(
        appointment.id(),
        appointment.appointmentCode(),
        appointment.appointmentTime(),
        appointment.status(),
        normalizeStatus(appointment.status()),
        appointment.cancelReason(),
        appointment.doctorId(),
        appointment.doctorName(),
        appointment.urgencyLevel(),
        appointment.prescriptionStatus(),
        appointment.isFollowup(),
        appointment.priorityScore(),
        appointment.serviceIds());
  }

  private static String normalizeStatus(String status) {
    if (status == null) {
      return "PENDING_CONFIRMATION";
    }
    return switch (status.toUpperCase(Locale.ROOT)) {
      case "CANCEL" -> "CANCELLED";
      case "FINISH" -> "CONFIRMED";
      default -> "PENDING_CONFIRMATION";
    };
  }

  private static List<String> normalizeServiceIds(List<String> serviceIds) {
    if (serviceIds == null || serviceIds.isEmpty()) {
      return List.of();
    }

    LinkedHashSet<String> normalized = new LinkedHashSet<>();
    for (String rawServiceId : serviceIds) {
      String normalizedValue = normalizeNullable(rawServiceId);
      if (normalizedValue != null) {
        normalized.add(normalizedValue);
      }
    }
    return new ArrayList<>(normalized);
  }

  private String generateAppointmentCode(Instant appointmentTime) {
    String date = APPOINTMENT_CODE_DATE.format(appointmentTime.atZone(clock.getZone()));
    return "AP-PT-" + date + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
  }

  private static String normalizeRequired(String value, String message) {
    String normalized = normalizeNullable(value);
    if (normalized == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
    return normalized;
  }

  private static String normalizeNullable(String value) {
    if (value == null) {
      return null;
    }
    String normalized = value.trim();
    return normalized.isEmpty() ? null : normalized;
  }

  private static String maskRecipient(String recipient) {
    if (recipient == null || recipient.isBlank() || !recipient.contains("@")) {
      return "";
    }
    String[] parts = recipient.split("@", 2);
    if (parts[0].length() <= 2) {
      return "*@" + parts[1];
    }
    return parts[0].charAt(0) + "***" + parts[0].charAt(parts[0].length() - 1) + "@" + parts[1];
  }
}
