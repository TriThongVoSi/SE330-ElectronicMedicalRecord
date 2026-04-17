import type { ListQuery } from '../../core/types/common'

export type PrescriptionItem = {
  drugId: string
  drugName?: string
  quantity: number
  instructions: string
}

export type Prescription = {
  id: string
  prescriptionCode: string
  patientId: string
  patientName?: string
  doctorId: string
  doctorName?: string
  appointmentId: string
  status: string
  diagnosis?: string
  advice?: string
  items: PrescriptionItem[]
  createdAt?: string
}

export type PrescriptionListQuery = ListQuery & {
  status?: string
  patientId?: string
}

export type PrescriptionUpsertInput = {
  prescriptionCode: string
  patientId: string
  doctorId: string
  appointmentId: string
  status: string
  diagnosis?: string
  advice?: string
  items: PrescriptionItem[]
}
