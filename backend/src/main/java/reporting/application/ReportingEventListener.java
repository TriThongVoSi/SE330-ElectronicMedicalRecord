package org.example.BenhAnDienTu.reporting.application;

import org.example.BenhAnDienTu.appointment.api.AppointmentBookedEvent;
import org.example.BenhAnDienTu.patient.api.PatientRegisteredEvent;
import org.example.BenhAnDienTu.prescription.api.PrescriptionIssuedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class ReportingEventListener {

  private static final Logger LOGGER = LoggerFactory.getLogger(ReportingEventListener.class);

  @EventListener
  public void onPatientRegistered(PatientRegisteredEvent event) {
    LOGGER.info(
        "Reporting observer captured patient registration: patientId={}, occurredAt={}",
        event.patientId(),
        event.occurredAt());
  }

  @EventListener
  public void onAppointmentBooked(AppointmentBookedEvent event) {
    LOGGER.info(
        "Reporting observer captured appointment booking: appointmentId={}, patientId={}, occurredAt={}",
        event.appointmentId(),
        event.patientId(),
        event.occurredAt());
  }

  @EventListener
  public void onPrescriptionIssued(PrescriptionIssuedEvent event) {
    LOGGER.info(
        "Reporting observer captured prescription issue: prescriptionId={}, appointmentId={}, occurredAt={}",
        event.prescriptionId(),
        event.appointmentId(),
        event.occurredAt());
  }
}
