export type DashboardSummary = {
  totalAppointmentsToday: number
  comingAppointmentsToday: number
  finishedAppointmentsToday: number
  cancelledAppointmentsToday: number
  newPatientsToday: number
  upcomingAppointments: Array<{
    id: string
    appointmentTime: string
    patientName: string
    doctorName: string
    status: string
  }>
}
