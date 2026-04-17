import { executeApiRequest } from '../../core/api/request'
import { toPagedResult } from '../../core/api/paged-result'
import type { PagedResult } from '../../core/types/common'
import type { Appointment, AppointmentListQuery, AppointmentUpsertInput } from './types'

export const listAppointments = async (
  query: AppointmentListQuery
): Promise<PagedResult<Appointment>> => {
  const rawPayload = await executeApiRequest<unknown>({
    request: {
      url: '/api/appointments',
      method: 'GET',
      params: query,
    },
  })

  return toPagedResult<Appointment>(rawPayload, query.page, query.size)
}

export const getAppointment = async (id: string): Promise<Appointment> => {
  return executeApiRequest<Appointment>({
    request: {
      url: `/api/appointments/${id}`,
      method: 'GET',
    },
  })
}

export const createAppointment = async (payload: AppointmentUpsertInput): Promise<Appointment> => {
  return executeApiRequest<Appointment>({
    request: {
      url: '/api/appointments',
      method: 'POST',
      data: payload,
    },
  })
}

export const updateAppointment = async (
  id: string,
  payload: AppointmentUpsertInput
): Promise<Appointment> => {
  return executeApiRequest<Appointment>({
    request: {
      url: `/api/appointments/${id}`,
      method: 'PUT',
      data: payload,
    },
  })
}

export const listAppointmentOptions = async (): Promise<Appointment[]> => {
  const rawPayload = await executeApiRequest<unknown>({
    request: {
      url: '/api/appointments',
      method: 'GET',
      params: { page: 1, size: 200 },
    },
  })

  if (Array.isArray(rawPayload)) {
    return rawPayload as Appointment[]
  }

  return toPagedResult<Appointment>(rawPayload, 1, 200).items
}
