import type { ListQuery } from '../../core/types/common'

export type AppointmentStatus = 'COMING' | 'FINISH' | 'CANCEL'

export type Appointment = {
  id: string
  appointmentCode: string
  appointmentTime: string
  status: AppointmentStatus
  cancelReason?: string
  doctorId: string
  doctorName?: string
  patientId: string
  patientName?: string
  urgencyLevel: number
  prescriptionStatus: string
  isFollowup: boolean
  priorityScore?: number
  createdAt?: string
  updatedAt?: string
}

export type AppointmentListQuery = ListQuery & {
  status?: AppointmentStatus
  doctorId?: string
  patientId?: string
  date?: string
}

export type AppointmentUpsertInput = {
  appointmentCode: string
  appointmentTime: string
  status: AppointmentStatus
  cancelReason?: string
  doctorId: string
  patientId: string
  urgencyLevel: number
  prescriptionStatus: string
  isFollowup: boolean
  priorityScore?: number
}
