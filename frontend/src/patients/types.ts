import type { ListQuery } from '../../core/types/common'

export type Patient = {
  id: string
  patientCode: string
  fullName: string
  dateOfBirth?: string
  email?: string
  gender: string
  phone: string
  address?: string
  diagnosis?: string
  drugAllergies?: string
  heightCm?: number
  weightKg?: number
  createdAt?: string
  updatedAt?: string
}

export type PatientListQuery = ListQuery & {
  phone?: string
  code?: string
}

export type PatientUpsertInput = {
  patientCode: string
  fullName: string
  dateOfBirth?: string
  email?: string
  gender: string
  phone: string
  address?: string
  diagnosis?: string
  drugAllergies?: string
  heightCm?: number
  weightKg?: number
}

export type PatientMedicalTimelineEntry = {
  appointmentId: string
  appointmentCode: string
  appointmentTime: string
  appointmentStatus: string
  doctorId: string
  doctorName: string
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
