import type {
  AuthTokenResponse,
  AuthUser,
  ForgotPasswordVerifyOtpResponse,
  OtpChallengeResponse,
  ResetPasswordResponse,
  SignUpVerifyOtpResponse,
} from './types'

type UnknownRecord = Record<string, unknown>

const isRecord = (value: unknown): value is UnknownRecord =>
  typeof value === 'object' && value !== null

const readString = (value: unknown): string | null => (typeof value === 'string' ? value : null)

const readNumber = (value: unknown): number | null =>
  typeof value === 'number' && Number.isFinite(value) ? value : null

const parseRole = (value: unknown): AuthUser['role'] => {
  if (value === 'ADMIN' || value === 'DOCTOR' || value === 'PATIENT') {
    return value
  }
  return 'PATIENT'
}

const parseUser = (value: unknown): AuthUser | null => {
  if (!isRecord(value)) {
    return null
  }

  const id = readString(value.id)
  const username = readString(value.username)
  const fullName = readString(value.fullName) ?? readString(value.name)
  const email = readString(value.email)
  const role = parseRole(value.role)

  if (!id || !username || !fullName || !email) {
    return null
  }

  return {
    id,
    username,
    fullName,
    email,
    role,
    mustChangePassword: value.mustChangePassword === true,
  }
}

const unwrapResult = (input: unknown): unknown => {
  if (!isRecord(input)) {
    return input
  }
  if ('result' in input) {
    return input.result
  }
  return input
}

export const parseAuthTokenResponse = (
  input: unknown,
  currentRefreshToken?: string | null
): AuthTokenResponse | null => {
  const payload = unwrapResult(input)
  if (!isRecord(payload)) {
    return null
  }

  const accessToken =
    readString(payload.accessToken) ?? readString(payload.access_token) ?? readString(payload.token)
  const refreshToken =
    readString(payload.refreshToken) ??
    readString(payload.refresh_token) ??
    currentRefreshToken ??
    null
  const user = parseUser(payload.user)

  if (!accessToken || !refreshToken || !user) {
    return null
  }

  return {
    accessToken,
    refreshToken,
    user,
  }
}

export const parseCurrentUser = (input: unknown): AuthUser | null => {
  const payload = unwrapResult(input)
  if (!isRecord(payload)) {
    return null
  }
  return parseUser(payload.user) ?? parseUser(payload)
}

export const parseOtpChallengeResponse = (input: unknown): OtpChallengeResponse | null => {
  const payload = unwrapResult(input)
  if (!isRecord(payload)) {
    return null
  }

  const message = readString(payload.message) ?? 'OTP sent.'
  const expiresInSeconds = readNumber(payload.expiresInSeconds)

  if (expiresInSeconds === null) {
    return null
  }

  return {
    message,
    nextStep: readString(payload.nextStep) ?? undefined,
    emailMasked: readString(payload.emailMasked) ?? undefined,
    expiresInSeconds,
  }
}

export const parseSignUpVerifyOtpResponse = (input: unknown): SignUpVerifyOtpResponse | null => {
  const payload = unwrapResult(input)
  if (!isRecord(payload)) {
    return null
  }

  const user = parseUser(payload.user)
  if (!user) {
    return null
  }

  return {
    message: readString(payload.message) ?? 'Verified',
    user,
  }
}

export const parseForgotPasswordVerifyOtpResponse = (
  input: unknown
): ForgotPasswordVerifyOtpResponse | null => {
  const payload = unwrapResult(input)
  if (!isRecord(payload)) {
    return null
  }

  const tempResetToken = readString(payload.tempResetToken)
  const expiresInSeconds = readNumber(payload.expiresInSeconds)
  if (!tempResetToken || expiresInSeconds === null) {
    return null
  }

  return {
    tempResetToken,
    expiresInSeconds,
  }
}

export const parseResetPasswordResponse = (input: unknown): ResetPasswordResponse | null => {
  const payload = unwrapResult(input)
  if (!isRecord(payload)) {
    return null
  }

  return {
    message: readString(payload.message) ?? 'Password updated successfully.',
  }
}
