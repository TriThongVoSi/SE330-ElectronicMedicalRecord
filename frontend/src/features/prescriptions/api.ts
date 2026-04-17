import { executeApiRequest } from '../../core/api/request'
import { toPagedResult } from '../../core/api/paged-result'
import type { PagedResult } from '../../core/types/common'
import type { Prescription, PrescriptionListQuery, PrescriptionUpsertInput } from './types'

export const listPrescriptions = async (
  query: PrescriptionListQuery
): Promise<PagedResult<Prescription>> => {
  const rawPayload = await executeApiRequest<unknown>({
    request: {
      url: '/api/prescriptions',
      method: 'GET',
      params: query,
    },
  })

  return toPagedResult<Prescription>(rawPayload, query.page, query.size)
}

export const getPrescription = async (id: string): Promise<Prescription> => {
  return executeApiRequest<Prescription>({
    request: {
      url: `/api/prescriptions/${id}`,
      method: 'GET',
    },
  })
}

export const createPrescription = async (
  payload: PrescriptionUpsertInput
): Promise<Prescription> => {
  return executeApiRequest<Prescription>({
    request: {
      url: '/api/prescriptions',
      method: 'POST',
      data: payload,
    },
  })
}

export const updatePrescription = async (
  id: string,
  payload: PrescriptionUpsertInput
): Promise<Prescription> => {
  return executeApiRequest<Prescription>({
    request: {
      url: `/api/prescriptions/${id}`,
      method: 'PUT',
      data: payload,
    },
  })
}
