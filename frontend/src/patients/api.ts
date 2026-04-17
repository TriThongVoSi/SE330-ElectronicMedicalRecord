import { executeApiRequest } from '../../core/api/request'
import { toPagedResult } from '../../core/api/paged-result'
import type { PagedResult } from '../../core/types/common'
import type {
  Patient,
  PatientListQuery,
  PatientMedicalTimeline,
  PatientUpsertInput,
} from './types'

export const listPatients = async (query: PatientListQuery): Promise<PagedResult<Patient>> => {
  const rawPayload = await executeApiRequest<unknown>({
    request: {
      url: '/api/patients',
      method: 'GET',
      params: query,
    },
  })

  return toPagedResult<Patient>(rawPayload, query.page, query.size)
}

export const getPatient = async (id: string): Promise<Patient> => {
  return executeApiRequest<Patient>({
    request: {
      url: `/api/patients/${id}`,
      method: 'GET',
    },
  })
}

export const createPatient = async (payload: PatientUpsertInput): Promise<Patient> => {
  return executeApiRequest<Patient>({
    request: {
      url: '/api/patients',
      method: 'POST',
      data: payload,
    },
  })
}

export const updatePatient = async (id: string, payload: PatientUpsertInput): Promise<Patient> => {
  return executeApiRequest<Patient>({
    request: {
      url: `/api/patients/${id}`,
      method: 'PUT',
      data: payload,
    },
  })
}

export const listPatientOptions = async (): Promise<Patient[]> => {
  const rawPayload = await executeApiRequest<unknown>({
    request: {
      url: '/api/patients',
      method: 'GET',
      params: { page: 1, size: 200 },
    },
  })

  if (Array.isArray(rawPayload)) {
    return rawPayload as Patient[]
  }

  return toPagedResult<Patient>(rawPayload, 1, 200).items
}

export const getPatientMedicalTimeline = async (
  patientId: string,
  limit = 50
): Promise<PatientMedicalTimeline> => {
  return executeApiRequest<PatientMedicalTimeline>({
    request: {
      url: `/api/dashboard/patients/${patientId}/timeline`,
      method: 'GET',
      params: { limit },
    },
  })
}
