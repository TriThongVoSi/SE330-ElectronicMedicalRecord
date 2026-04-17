package org.example.BenhAnDienTu.appointment.api;

import java.util.Optional;

/** Public contract for scheduling and reading appointments. */
public interface AppointmentApi {

  AppointmentPageView listAppointments(AppointmentListQuery query);

  AppointmentView bookAppointment(AppointmentBookingCommand command);

  AppointmentView updateAppointment(String appointmentId, AppointmentBookingCommand command);

  Optional<AppointmentView> findAppointment(String appointmentId);
}
