import { executeApiRequest } from '../../core/api/request'
import { toPagedResult } from '../../core/api/paged-result'
import type { PagedResult } from '../../core/types/common'
import type { Service, ServiceListQuery, ServiceUpsertInput } from './types'

export const listServices = async (query: ServiceListQuery): Promise<PagedResult<Service>> => {
  const rawPayload = await executeApiRequest<unknown>({
    request: {
      url: '/api/services',
      method: 'GET',
      params: query,
    },
  })

  return toPagedResult<Service>(rawPayload, query.page, query.size)
}

export const createService = async (payload: ServiceUpsertInput): Promise<Service> => {
  return executeApiRequest<Service>({
    request: {
      url: '/api/services',
      method: 'POST',
      data: payload,
    },
  })
}

export const updateService = async (id: string, payload: ServiceUpsertInput): Promise<Service> => {
  return executeApiRequest<Service>({
    request: {
      url: `/api/services/${id}`,
      method: 'PUT',
      data: payload,
    },
  })
}

export const listServiceOptions = async (): Promise<Service[]> => {
  const rawPayload = await executeApiRequest<unknown>({
    request: {
      url: '/api/services',
      method: 'GET',
      params: { page: 1, size: 200 },
    },
  })

  if (Array.isArray(rawPayload)) {
    return rawPayload as Service[]
  }

  return toPagedResult<Service>(rawPayload, 1, 200).items
}
