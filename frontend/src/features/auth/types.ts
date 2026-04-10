import type { UserRole } from '../../core/types/common'

export type AuthUser = {
  id: string
  username: string
  fullName: string
  email: string
  role: UserRole
  mustChangePassword: boolean
}

export type LoginPayload = {
  identifier: string
  password: string
}

export type AuthTokenResponse = {
  accessToken: string
  refreshToken: string
  user: AuthUser
}

export type SignUpPayload = {
  username: string
  email: string
  password: string
  fullName?: string
  role?: string
}

export type OtpChallengeResponse = {
  message: string
  nextStep?: string
  emailMasked?: string
  expiresInSeconds: number
}

export type VerifyOtpPayload = {
  email: string
  otp: string
}

export type SignUpVerifyOtpResponse = {
  message: string
  user: AuthUser
}

export type ForgotPasswordPayload = {
  email: string
}

export type ForgotPasswordVerifyOtpResponse = {
  tempResetToken: string
  expiresInSeconds: number
}

export type ResetPasswordPayload = {
  tempResetToken: string
  newPassword: string
}

export type ResetPasswordResponse = {
  message: string
}

export type FirstLoginPasswordChangePayload = {
  currentPassword: string
  newPassword: string
}
