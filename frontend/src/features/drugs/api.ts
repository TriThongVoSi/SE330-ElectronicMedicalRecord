import { executeApiRequest } from '../../core/api/request'
import { toPagedResult } from '../../core/api/paged-result'
import type { PagedResult } from '../../core/types/common'
import type { Drug, DrugListQuery, DrugUpsertInput } from './types'

export const listDrugs = async (query: DrugListQuery): Promise<PagedResult<Drug>> => {
  const rawPayload = await executeApiRequest<unknown>({
    request: {
      url: '/api/drugs',
      method: 'GET',
      params: query,
    },
  })

  return toPagedResult<Drug>(rawPayload, query.page, query.size)
}

export const createDrug = async (payload: DrugUpsertInput): Promise<Drug> => {
  return executeApiRequest<Drug>({
    request: {
      url: '/api/drugs',
      method: 'POST',
      data: payload,
    },
  })
}

export const updateDrug = async (id: string, payload: DrugUpsertInput): Promise<Drug> => {
  return executeApiRequest<Drug>({
    request: {
      url: `/api/drugs/${id}`,
      method: 'PUT',
      data: payload,
    },
  })
}

export const listDrugOptions = async (): Promise<Drug[]> => {
  const rawPayload = await executeApiRequest<unknown>({
    request: {
      url: '/api/drugs',
      method: 'GET',
      params: { page: 1, size: 200 },
    },
  })

  if (Array.isArray(rawPayload)) {
    return rawPayload as Drug[]
  }

  return toPagedResult<Drug>(rawPayload, 1, 200).items
}
