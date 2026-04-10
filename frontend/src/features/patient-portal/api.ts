import { executeApiRequest } from '../../core/api/request'
import { toPagedResult } from '../../core/api/paged-result'
import type {
  PatientMedicalTimeline,
  PatientMedicalVisitDetail,
  PatientPortalAppointment,
  PatientPortalAppointmentCancelInput,
  PatientPortalAppointmentCreateInput,
  PatientPortalAppointmentListQuery,
  PatientPortalAppointmentPage,
  PatientPortalAppointmentRescheduleInput,
  PatientPortalDashboard,
  PatientPortalDoctor,
  PatientPortalProfile,
  PatientPortalProfileUpdateInput,
  PatientPortalSlot,
} from './types'

export const getPatientPortalDashboard = async (): Promise<PatientPortalDashboard> => {
  return executeApiRequest<PatientPortalDashboard>({
    request: {
      url: '/api/v1/patient-portal/dashboard',
      method: 'GET',
    },
  })
}

export const getPatientPortalProfile = async (): Promise<PatientPortalProfile> => {
  return executeApiRequest<PatientPortalProfile>({
    request: {
      url: '/api/v1/patient-portal/profile',
      method: 'GET',
    },
  })
}

export const updatePatientPortalProfile = async (
  payload: PatientPortalProfileUpdateInput
): Promise<PatientPortalProfile> => {
  return executeApiRequest<PatientPortalProfile>({
    request: {
      url: '/api/v1/patient-portal/profile',
      method: 'PUT',
      data: payload,
    },
  })
}

export const getPatientMedicalTimeline = async (limit = 50): Promise<PatientMedicalTimeline> => {
  return executeApiRequest<PatientMedicalTimeline>({
    request: {
      url: '/api/v1/patient-portal/medical-history',
      method: 'GET',
      params: { limit },
    },
  })
}

export const getPatientMedicalVisitDetail = async (
  appointmentId: string
): Promise<PatientMedicalVisitDetail> => {
  return executeApiRequest<PatientMedicalVisitDetail>({
    request: {
      url: `/api/v1/patient-portal/medical-history/${appointmentId}`,
      method: 'GET',
    },
  })
}

export const listPatientAppointments = async (
  query: PatientPortalAppointmentListQuery
): Promise<PatientPortalAppointmentPage> => {
  const rawPayload = await executeApiRequest<unknown>({
    request: {
      url: '/api/v1/patient-portal/appointments',
      method: 'GET',
      params: query,
    },
  })

  return toPagedResult<PatientPortalAppointment>(rawPayload, query.page, query.size)
}

export const getPatientAppointment = async (appointmentId: string): Promise<PatientPortalAppointment> => {
  return executeApiRequest<PatientPortalAppointment>({
    request: {
      url: `/api/v1/patient-portal/appointments/${appointmentId}`,
      method: 'GET',
    },
  })
}

export const listBookableDoctors = async (): Promise<PatientPortalDoctor[]> => {
  return executeApiRequest<PatientPortalDoctor[]>({
    request: {
      url: '/api/v1/patient-portal/doctors',
      method: 'GET',
    },
  })
}

export const listAvailableSlots = async (
  doctorId: string,
  fromDate?: string,
  toDate?: string
): Promise<PatientPortalSlot[]> => {
  return executeApiRequest<PatientPortalSlot[]>({
    request: {
      url: '/api/v1/patient-portal/available-slots',
      method: 'GET',
      params: {
        doctorId,
        fromDate,
        toDate,
      },
    },
  })
}

export const createPatientAppointment = async (
  payload: PatientPortalAppointmentCreateInput
): Promise<PatientPortalAppointment> => {
  return executeApiRequest<PatientPortalAppointment>({
    request: {
      url: '/api/v1/patient-portal/appointments',
      method: 'POST',
      data: payload,
    },
  })
}

export const reschedulePatientAppointment = async (
  appointmentId: string,
  payload: PatientPortalAppointmentRescheduleInput
): Promise<PatientPortalAppointment> => {
  return executeApiRequest<PatientPortalAppointment>({
    request: {
      url: `/api/v1/patient-portal/appointments/${appointmentId}/reschedule`,
      method: 'PUT',
      data: payload,
    },
  })
}

export const cancelPatientAppointment = async (
  appointmentId: string,
  payload: PatientPortalAppointmentCancelInput
): Promise<PatientPortalAppointment> => {
  return executeApiRequest<PatientPortalAppointment>({
    request: {
      url: `/api/v1/patient-portal/appointments/${appointmentId}/cancel`,
      method: 'POST',
      data: payload,
    },
  })
}
