export type UserRole = 'ADMIN' | 'DOCTOR' | 'PATIENT'

export type PagedResult<T> = {
  items: T[]
  page: number
  size: number
  totalItems: number
  totalPages: number
}

export type ListQuery = {
  page?: number
  size?: number
  search?: string
}

export type OptionItem = {
  value: string
  label: string
}
