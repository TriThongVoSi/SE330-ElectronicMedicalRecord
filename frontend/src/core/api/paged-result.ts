import type { PagedResult } from '../types/common'
import { buildPagedResult, defaultPageSize } from '../utils/pagination'

type UnknownRecord = Record<string, unknown>

const isRecord = (value: unknown): value is UnknownRecord =>
  typeof value === 'object' && value !== null

const readNumber = (candidate: unknown, fallback: number): number =>
  typeof candidate === 'number' && Number.isFinite(candidate) ? candidate : fallback

const readItems = <T>(payload: unknown): T[] => {
  if (Array.isArray(payload)) {
    return payload as T[]
  }

  if (!isRecord(payload)) {
    return []
  }

  const candidates = [payload.items, payload.content, payload.data]
  for (const candidate of candidates) {
    if (Array.isArray(candidate)) {
      return candidate as T[]
    }
  }

  return []
}

export const toPagedResult = <T>(
  payload: unknown,
  fallbackPage = 1,
  fallbackSize = defaultPageSize
): PagedResult<T> => {
  if (Array.isArray(payload)) {
    return buildPagedResult(payload as T[], fallbackPage, fallbackSize)
  }

  if (!isRecord(payload)) {
    return buildPagedResult([], fallbackPage, fallbackSize)
  }

  const items = readItems<T>(payload)

  if (items.length === 0) {
    return buildPagedResult([], fallbackPage, fallbackSize)
  }

  const page = readNumber(payload.page, readNumber(payload.pageNumber, fallbackPage))
  const size = readNumber(payload.size, readNumber(payload.pageSize, fallbackSize))
  const totalItems = readNumber(payload.totalItems, readNumber(payload.totalElements, items.length))
  const totalPages = readNumber(
    payload.totalPages,
    Math.max(1, Math.ceil(totalItems / Math.max(1, size)))
  )

  return {
    items,
    page,
    size,
    totalItems,
    totalPages,
  }
}
