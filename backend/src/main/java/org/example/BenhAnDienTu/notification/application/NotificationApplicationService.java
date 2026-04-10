package org.example.BenhAnDienTu.notification.application;

import java.time.Instant;
import java.util.UUID;
import org.example.BenhAnDienTu.appointment.api.AppointmentBookedEvent;
import org.example.BenhAnDienTu.notification.api.NotificationApi;
import org.example.BenhAnDienTu.notification.api.NotificationCommand;
import org.example.BenhAnDienTu.notification.api.NotificationReceipt;
import org.example.BenhAnDienTu.notification.domain.NotificationChannel;
import org.example.BenhAnDienTu.notification.domain.NotificationEnvelope;
import org.example.BenhAnDienTu.notification.infrastructure.NotificationChannelFactory;
import org.example.BenhAnDienTu.patient.api.PatientRegisteredEvent;
import org.example.BenhAnDienTu.prescription.api.PrescriptionIssuedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
public class NotificationApplicationService implements NotificationApi {

  private final NotificationChannelFactory channelFactory;

  public NotificationApplicationService(NotificationChannelFactory channelFactory) {
    this.channelFactory = channelFactory;
  }

  @Override
  public NotificationReceipt send(NotificationCommand command) {
    String normalizedChannel = NotificationChannel.normalizeChannel(command.channel());
    NotificationChannel channel = channelFactory.resolve(normalizedChannel);

    NotificationEnvelope envelope =
        new NotificationEnvelope(
            normalizedChannel, command.subject(), command.body(), command.recipient(), Instant.now());

    String messageId = UUID.randomUUID().toString();
    channel.dispatch(messageId, envelope);

    return new NotificationReceipt(messageId, "QUEUED", Instant.now());
  }

  @EventListener
  public void onPatientRegistered(PatientRegisteredEvent event) {
    send(
        new NotificationCommand(
            "SYSTEM",
            "Patient Registered",
            "Patient " + event.patientId() + " registered at " + event.occurredAt(),
            null));
  }

  @EventListener
  public void onAppointmentBooked(AppointmentBookedEvent event) {
    send(
        new NotificationCommand(
            "SYSTEM",
            "Appointment Booked",
            "Appointment " + event.appointmentId() + " for patient " + event.patientId(),
            null));
  }

  @EventListener
  public void onPrescriptionIssued(PrescriptionIssuedEvent event) {
    send(
        new NotificationCommand(
            "SYSTEM",
            "Prescription Issued",
            "Prescription "
                + event.prescriptionId()
                + " issued for appointment "
                + event.appointmentId(),
            null));
  }
}
