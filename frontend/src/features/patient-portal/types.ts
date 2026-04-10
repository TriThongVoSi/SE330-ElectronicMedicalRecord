import type { PagedResult } from '../../core/types/common'

export type PatientPortalNormalizedStatus =
  | 'PENDING_CONFIRMATION'
  | 'CONFIRMED'
  | 'CANCELLED'

export type PatientPortalAppointmentStatus = 'COMING' | 'FINISH' | 'CANCEL'

export type PatientPortalAppointment = {
  id: string
  appointmentCode: string
  appointmentTime: string
  status: string
  normalizedStatus?: string
  cancelReason?: string
  doctorId: string
  doctorName?: string
  urgencyLevel: number
  prescriptionStatus: string
  followup?: boolean
  isFollowup?: boolean
  priorityScore?: number
  serviceIds?: string[]
}

export type PatientPortalAppointmentPage = PagedResult<PatientPortalAppointment>

export type PatientPortalAppointmentListQuery = {
  page?: number
  size?: number
  status?: PatientPortalAppointmentStatus
  date?: string
}

export type PatientPortalDoctor = {
  id: string
  fullName: string
  email?: string
  phone?: string
}

export type PatientPortalSlot = {
  slotId: string
  doctorId: string
  slotTime: string
  durationMinutes: number
  booked: boolean
}

export type PatientPortalAppointmentCreateInput = {
  doctorId: string
  appointmentTime: string
  serviceIds?: string[]
  note?: string
}

export type PatientPortalAppointmentRescheduleInput = {
  doctorId: string
  appointmentTime: string
  serviceIds?: string[]
}

export type PatientPortalAppointmentCancelInput = {
  cancelReason: string
}

export type PatientMedicalTimelineEntry = {
  appointmentId: string
  appointmentCode: string
  appointmentTime: string
  appointmentStatus?: string
  doctorId?: string
  doctorName?: string
  prescriptionId?: string
  prescriptionCode?: string
  prescriptionStatus?: string
  prescriptionIssuedAt?: string
  diagnosis?: string
  advice?: string
}

export type PatientMedicalTimeline = {
  patientId: string
  patientName: string
  entries: PatientMedicalTimelineEntry[]
}

export type PatientMedicalVisitMedication = {
  drugId: string
  drugName: string
  dosage?: string
  quantity: number
  instructions?: string
}

export type PatientMedicalVisitDetail = {
  appointmentId: string
  appointmentCode: string
  visitTime: string
  appointmentStatus?: string
  doctorId?: string
  doctorName?: string
  diagnosis?: string
  notes?: string
  prescriptionId?: string
  prescriptionCode?: string
  prescriptionStatus?: string
  medications: PatientMedicalVisitMedication[]
}

export type PatientPortalDashboard = {
  upcomingAppointments: number
  latestVisit?: PatientMedicalTimelineEntry | null
  latestPrescription?: PatientMedicalTimelineEntry | null
}

export type PatientPortalProfile = {
  id: string
  patientCode: string
  fullName: string
  dateOfBirth?: string
  email?: string
  gender?: string
  phone: string
  address?: string
  diagnosis?: string
  drugAllergies?: string
  heightCm?: number
  weightKg?: number
  createdAt?: string
  updatedAt?: string
}

export type PatientPortalProfileUpdateInput = {
  phone: string
  address?: string
  heightCm?: number
  weightKg?: number
  drugAllergies?: string
}
