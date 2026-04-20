import axios, {
  AxiosHeaders,
  type AxiosError,
  type AxiosRequestConfig,
  type InternalAxiosRequestConfig,
} from 'axios'
import { env } from '../config/env'
import { useAuthStore } from '../../features/auth/auth-store'
import { parseAuthTokenResponse } from '../../features/auth/parsers'

type RetryRequestConfig = InternalAxiosRequestConfig & {
  _retry?: boolean
}

const requestTimeoutMs = 15000

const authPathMarkers = [
  '/api/v1/auth/sign-in',
  '/api/v1/auth/refresh',
  '/api/v1/auth/sign-out',
]

const isAuthRequest = (url?: string): boolean =>
  typeof url === 'string' && authPathMarkers.some((marker) => url.includes(marker))

const readString = (candidate: unknown): string | null =>
  typeof candidate === 'string' ? candidate : null

const httpClient = axios.create({
  baseURL: env.apiBaseUrl,
  timeout: requestTimeoutMs,
})

const refreshClient = axios.create({
  baseURL: env.apiBaseUrl,
  timeout: requestTimeoutMs,
})

httpClient.interceptors.request.use((config) => {
  const token = useAuthStore.getState().accessToken
  if (token) {
    const headers = AxiosHeaders.from(config.headers)
    headers.set('Authorization', `Bearer ${token}`)
    config.headers = headers
  }
  return config
})

let refreshPromise: Promise<string | null> | null = null

const redirectToLogin = () => {
  if (window.location.pathname !== '/login') {
    window.location.assign('/login')
  }
}

const refreshAccessToken = async (): Promise<string | null> => {
  const state = useAuthStore.getState()
  const currentRefreshToken = state.refreshToken

  if (!currentRefreshToken) {
    return null
  }

  try {
    const response = await refreshClient.post('/api/v1/auth/refresh', {
      refreshToken: currentRefreshToken,
    })

    const parsed = parseAuthTokenResponse(response.data, currentRefreshToken)

    if (parsed) {
      useAuthStore.getState().setSession(parsed)
      return parsed.accessToken
    }

    const accessToken =
      readString((response.data as Record<string, unknown>)?.accessToken) ??
      readString((response.data as Record<string, unknown>)?.token)

    const currentUser = useAuthStore.getState().user
    if (accessToken && currentUser) {
      useAuthStore
        .getState()
        .setSession({ accessToken, refreshToken: currentRefreshToken, user: currentUser })
      return accessToken
    }

    return null
  } catch {
    return null
  }
}

httpClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const originalConfig = error.config as RetryRequestConfig | undefined

    if (!originalConfig || error.response?.status !== 401 || isAuthRequest(originalConfig.url)) {
      throw error
    }

    if (originalConfig._retry) {
      useAuthStore.getState().clearSession()
      redirectToLogin()
      throw error
    }

    originalConfig._retry = true

    if (!refreshPromise) {
      refreshPromise = refreshAccessToken()
    }

    const newAccessToken = await refreshPromise.finally(() => {
      refreshPromise = null
    })

    if (!newAccessToken) {
      useAuthStore.getState().clearSession()
      redirectToLogin()
      throw error
    }

    const headers = AxiosHeaders.from(originalConfig.headers)
    headers.set('Authorization', `Bearer ${newAccessToken}`)
    originalConfig.headers = headers

    return httpClient(originalConfig as AxiosRequestConfig)
  }
)

export { httpClient }
export type { AxiosRequestConfig }
