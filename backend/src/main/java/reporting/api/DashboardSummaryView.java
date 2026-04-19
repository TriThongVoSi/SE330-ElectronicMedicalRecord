package org.example.BenhAnDienTu.reporting.api;

import java.util.List;

/** Dashboard summary projection consumed by frontend. */
public record DashboardSummaryView(
    long totalAppointmentsToday,
    long comingAppointmentsToday,
    long finishedAppointmentsToday,
    long cancelledAppointmentsToday,
    long newPatientsToday,
    List<UpcomingAppointmentView> upcomingAppointments) {}
