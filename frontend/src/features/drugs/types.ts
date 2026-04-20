import type { ListQuery } from '../../core/types/common'

export type Drug = {
  id: string
  drugCode: string
  drugName: string
  manufacturer: string
  expiryDate: string
  unit: string
  price: number
  stockQuantity: number
  isActive: boolean
}

export type DrugListQuery = ListQuery & {
  isActive?: boolean
}

export type DrugUpsertInput = {
  drugCode: string
  drugName: string
  manufacturer: string
  expiryDate: string
  unit: string
  price: number
  stockQuantity: number
  isActive: boolean
}
