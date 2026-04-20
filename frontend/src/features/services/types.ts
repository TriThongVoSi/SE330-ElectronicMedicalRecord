import type { ListQuery } from '../../core/types/common'

export type Service = {
  id: string
  serviceCode: string
  serviceName: string
  serviceType: string
  isActive: boolean
}

export type ServiceListQuery = ListQuery & {
  isActive?: boolean
}

export type ServiceUpsertInput = {
  serviceCode: string
  serviceName: string
  serviceType: string
  isActive: boolean
}
