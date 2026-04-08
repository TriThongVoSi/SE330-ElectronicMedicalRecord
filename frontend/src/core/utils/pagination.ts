import type { PagedResult } from '../types/common'

export const defaultPageSize = 10

export const buildPagedResult = <T>(items: T[], page: number, size: number): PagedResult<T> => {
  const safeSize = size > 0 ? size : defaultPageSize
  const safePage = page > 0 ? page : 1
  const totalItems = items.length
  const totalPages = Math.max(1, Math.ceil(totalItems / safeSize))
  const startIndex = (safePage - 1) * safeSize
  const pagedItems = items.slice(startIndex, startIndex + safeSize)

  return {
    items: pagedItems,
    page: safePage,
    size: safeSize,
    totalItems,
    totalPages,
  }
}
