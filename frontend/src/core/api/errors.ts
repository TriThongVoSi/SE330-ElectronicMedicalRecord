import axios, { type AxiosError } from 'axios'
import { env } from '../config/env'

export type ApiError = {
  message: string
  status?: number
  code?: string
  details?: unknown
  fieldErrors?: Record<string, string>
}

const isRecord = (value: unknown): value is Record<string, unknown> =>
  typeof value === 'object' && value !== null

const toFieldErrors = (responsePayload: unknown): Record<string, string> | undefined => {
  if (!isRecord(responsePayload)) {
    return undefined
  }

  const fieldErrorSource = isRecord(responsePayload.errors)
    ? responsePayload.errors
    : responsePayload.fieldErrors
  if (!isRecord(fieldErrorSource)) {
    return undefined
  }

  const normalized: Record<string, string> = {}
  for (const [fieldName, fieldMessage] of Object.entries(fieldErrorSource)) {
    if (typeof fieldMessage === 'string') {
      normalized[fieldName] = fieldMessage
    }
  }

  return Object.keys(normalized).length > 0 ? normalized : undefined
}

const endpointLabel = (method?: string, url?: string): string => {
  const normalizedMethod = (method ?? 'GET').toUpperCase()
  const normalizedUrl = url ?? '<unknown-endpoint>'
  return `${normalizedMethod} ${normalizedUrl}`
}

const buildBackendUnavailableMessage = (method?: string, url?: string): string =>
  `Cannot reach backend API (${endpointLabel(method, url)}) at ${env.apiBaseUrl}. ` +
  'Please verify backend is running and VITE_API_BASE_URL is correct.'

const buildTimeoutMessage = (method?: string, url?: string): string =>
  `Backend request timed out (${endpointLabel(method, url)}). ` +
  `Please verify backend health at ${env.apiBaseUrl}.`

const normalizeAxiosErrorMessage = (axiosError: AxiosError): ApiError => {
  const status = axiosError.response?.status
  const responsePayload = axiosError.response?.data
  const requestConfig = axiosError.config

  if (!axiosError.response) {
    if (axiosError.code === 'ECONNABORTED') {
      return {
        message: buildTimeoutMessage(requestConfig?.method, requestConfig?.url),
        code: axiosError.code,
      }
    }

    return {
      message: buildBackendUnavailableMessage(requestConfig?.method, requestConfig?.url),
      code: axiosError.code,
    }
  }

  const serverMessage =
    isRecord(responsePayload) && typeof responsePayload.message === 'string'
      ? responsePayload.message
      : null
  const fallbackMessage = `Backend request failed (${endpointLabel(requestConfig?.method, requestConfig?.url)})`

  return {
    message: serverMessage || fallbackMessage,
    status,
    code:
      isRecord(responsePayload) && typeof responsePayload.code === 'string'
        ? responsePayload.code
        : undefined,
    details: responsePayload,
    fieldErrors: toFieldErrors(responsePayload),
  }
}

export const normalizeApiError = (error: unknown): ApiError => {
  if (axios.isAxiosError(error)) {
    return normalizeAxiosErrorMessage(error)
  }

  if (isRecord(error)) {
    return {
      message: typeof error.message === 'string' ? error.message : 'Unexpected error',
      status: typeof error.status === 'number' ? error.status : undefined,
      code: typeof error.code === 'string' ? error.code : undefined,
      details: 'details' in error ? error.details : undefined,
      fieldErrors: toFieldErrors(error),
    }
  }

  if (error instanceof Error) {
    return { message: error.message }
  }

  return { message: 'Unexpected error' }
}

export const isApiError = (value: unknown): value is ApiError =>
  typeof value === 'object' && value !== null && 'message' in value
