import type { AxiosRequestConfig } from '../../core/api/http-client'
import { normalizeApiError, type ApiError } from '../../core/api/errors'
import { executeApiRequest } from '../../core/api/request'
import { useAuthStore } from './auth-store'
import {
  parseAuthTokenResponse,
  parseCurrentUser,
  parseForgotPasswordVerifyOtpResponse,
  parseOtpChallengeResponse,
  parseResetPasswordResponse,
  parseSignUpVerifyOtpResponse,
} from './parsers'
import type {
  AuthTokenResponse,
  FirstLoginPasswordChangePayload,
  ForgotPasswordPayload,
  ForgotPasswordVerifyOtpResponse,
  LoginPayload,
  OtpChallengeResponse,
  ResetPasswordPayload,
  ResetPasswordResponse,
  SignUpPayload,
  SignUpVerifyOtpResponse,
  VerifyOtpPayload,
} from './types'

const parseSessionOrThrow = (
  raw: unknown,
  currentRefreshToken?: string | null
): AuthTokenResponse => {
  const parsed = parseAuthTokenResponse(raw, currentRefreshToken)
  if (!parsed) {
    throw {
      message: 'Authentication response is missing required fields.',
      status: 500,
    } satisfies ApiError
  }
  return parsed
}

const parseOrThrow = <T>(
  raw: unknown,
  parser: (candidate: unknown) => T | null,
  message: string
): T => {
  const parsed = parser(raw)
  if (!parsed) {
    throw {
      message,
      status: 500,
    } satisfies ApiError
  }
  return parsed
}

export const login = async (payload: LoginPayload): Promise<AuthTokenResponse> => {
  const rawPayload = await executeApiRequest<unknown>({
    request: {
      url: '/api/v1/auth/sign-in',
      method: 'POST',
      data: payload,
    },
  })

  return parseSessionOrThrow(rawPayload)
}

export const refreshSession = async (refreshToken: string): Promise<AuthTokenResponse> => {
  const rawPayload = await executeApiRequest<unknown>({
    request: {
      url: '/api/v1/auth/refresh',
      method: 'POST',
      data: { refreshToken },
    },
  })

  return parseSessionOrThrow(rawPayload, refreshToken)
}

export const fetchCurrentUser = async (): Promise<AuthTokenResponse['user']> => {
  const rawPayload = await executeApiRequest<unknown>({
    request: {
      url: '/api/v1/auth/me',
      method: 'GET',
    },
  })

  const parsed = parseCurrentUser(rawPayload)
  if (!parsed) {
    throw {
      message: 'Could not resolve current user profile.',
      status: 500,
    } satisfies ApiError
  }

  return parsed
}

export const performLogout = async (): Promise<void> => {
  const refreshToken = useAuthStore.getState().refreshToken

  const request: AxiosRequestConfig = {
    url: '/api/v1/auth/sign-out',
    method: 'POST',
    data: { refreshToken },
  }

  await executeApiRequest<void>({
    request,
  })
}

export const signUp = async (payload: SignUpPayload): Promise<OtpChallengeResponse> => {
  const rawPayload = await executeApiRequest<unknown>({
    request: {
      url: '/api/v1/auth/sign-up',
      method: 'POST',
      data: payload,
    },
  })
  return parseOrThrow(rawPayload, parseOtpChallengeResponse, 'Sign-up response is invalid.')
}

export const verifySignUpOtp = async (
  payload: VerifyOtpPayload
): Promise<SignUpVerifyOtpResponse> => {
  const rawPayload = await executeApiRequest<unknown>({
    request: {
      url: '/api/v1/auth/sign-up/verify-otp',
      method: 'POST',
      data: payload,
    },
  })
  return parseOrThrow(
    rawPayload,
    parseSignUpVerifyOtpResponse,
    'OTP verification response is invalid.'
  )
}

export const requestPasswordReset = async (
  payload: ForgotPasswordPayload
): Promise<OtpChallengeResponse> => {
  const rawPayload = await executeApiRequest<unknown>({
    request: {
      url: '/api/v1/auth/forgot-password',
      method: 'POST',
      data: payload,
    },
  })
  return parseOrThrow(rawPayload, parseOtpChallengeResponse, 'Forgot-password response is invalid.')
}

export const verifyForgotPasswordOtp = async (
  payload: VerifyOtpPayload
): Promise<ForgotPasswordVerifyOtpResponse> => {
  const rawPayload = await executeApiRequest<unknown>({
    request: {
      url: '/api/v1/auth/forgot-password/verify-otp',
      method: 'POST',
      data: payload,
    },
  })
  return parseOrThrow(
    rawPayload,
    parseForgotPasswordVerifyOtpResponse,
    'Forgot-password OTP response is invalid.'
  )
}

export const resetPassword = async (
  payload: ResetPasswordPayload
): Promise<ResetPasswordResponse> => {
  const rawPayload = await executeApiRequest<unknown>({
    request: {
      url: '/api/v1/auth/forgot-password/reset',
      method: 'POST',
      data: payload,
    },
  })
  return parseOrThrow(rawPayload, parseResetPasswordResponse, 'Reset-password response is invalid.')
}

export const changeFirstLoginPassword = async (
  payload: FirstLoginPasswordChangePayload
): Promise<void> => {
  await executeApiRequest<void>({
    request: {
      url: '/api/v1/auth/first-login/change-password',
      method: 'POST',
      data: payload,
    },
  })
}

export const bootstrapAuthSession = async (): Promise<void> => {
  const { accessToken, refreshToken, setUser, clearSession } = useAuthStore.getState()

  if (!accessToken && !refreshToken) {
    return
  }

  try {
    const currentUser = await fetchCurrentUser()
    setUser(currentUser)
  } catch (error) {
    const normalized = normalizeApiError(error)
    clearSession()
    console.warn(`Auth bootstrap cleared stale session: ${normalized.message}`)
  }
}
