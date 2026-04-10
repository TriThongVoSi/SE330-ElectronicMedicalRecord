import React, { useState } from 'react'
import { useMutation } from '@tanstack/react-query'
import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'
import { Link, useNavigate } from 'react-router-dom'
import { z } from 'zod'
import { appRoutes } from '../../app/routes'
import { normalizeApiError } from '../../core/api/errors'
import { useI18n } from '../../core/i18n/use-i18n'
import { requestPasswordReset, resetPassword, verifyForgotPasswordOtp } from './api'

const requestSchema = z.object({
  email: z.string().trim().email('Invalid email address.'),
})

const verifySchema = z.object({
  otp: z
    .string()
    .trim()
    .regex(/^\d{6}$/, 'OTP must be 6 digits.'),
})

const resetSchema = z
  .object({
    newPassword: z.string().min(6, 'Password must be at least 6 characters.'),
    confirmPassword: z.string().min(6, 'Confirm password is required.'),
  })
  .refine((formValues) => formValues.newPassword === formValues.confirmPassword, {
    path: ['confirmPassword'],
    message: 'Passwords do not match.',
  })

type RequestFormValues = z.infer<typeof requestSchema>
type VerifyFormValues = z.infer<typeof verifySchema>
type ResetFormValues = z.infer<typeof resetSchema>

type Step = 'REQUEST_OTP' | 'VERIFY_OTP' | 'RESET_PASSWORD' | 'DONE'

export const ForgotPasswordPage: React.FC = () => {
  const navigate = useNavigate()
  const [step, setStep] = useState<Step>('REQUEST_OTP')
  const [email, setEmail] = useState('')
  const [tempResetToken, setTempResetToken] = useState('')
  const [message, setMessage] = useState<string | null>(null)
  const { t } = useI18n()

  const requestForm = useForm<RequestFormValues>({
    resolver: zodResolver(requestSchema),
    defaultValues: { email: '' },
  })
  const verifyForm = useForm<VerifyFormValues>({
    resolver: zodResolver(verifySchema),
    defaultValues: { otp: '' },
  })
  const resetForm = useForm<ResetFormValues>({
    resolver: zodResolver(resetSchema),
    defaultValues: { newPassword: '', confirmPassword: '' },
  })

  const requestOtpMutation = useMutation({
    mutationFn: requestPasswordReset,
    onSuccess: (_, values) => {
      const normalizedEmail = values.email.trim().toLowerCase()
      setEmail(normalizedEmail)
      setMessage(
        t(
          'auth.forgot.requestDone',
          'If account exists, OTP has been sent. Enter the code to continue.'
        )
      )
      setStep('VERIFY_OTP')
    },
  })

  const verifyOtpMutation = useMutation({
    mutationFn: verifyForgotPasswordOtp,
    onSuccess: (result) => {
      setTempResetToken(result.tempResetToken)
      setMessage(t('auth.forgot.otpVerified', 'OTP verified. Set your new password.'))
      setStep('RESET_PASSWORD')
    },
  })

  const resetPasswordMutation = useMutation({
    mutationFn: resetPassword,
    onSuccess: () => {
      setMessage(
        t('auth.forgot.passwordUpdated', 'Password updated successfully. Redirecting to sign in...')
      )
      setStep('DONE')
      window.setTimeout(() => {
        navigate(appRoutes.login, { replace: true })
      }, 1000)
    },
  })

  const requestError = requestOtpMutation.error
    ? normalizeApiError(requestOtpMutation.error).message
    : null
  const verifyError = verifyOtpMutation.error
    ? normalizeApiError(verifyOtpMutation.error).message
    : null
  const resetError = resetPasswordMutation.error
    ? normalizeApiError(resetPasswordMutation.error).message
    : null

  return (
    <div className="login-page">
      <section className="login-panel">
        <p className="brand-tag">{t('auth.brandTag', 'Electronic Medical Record')}</p>
        <h1>{t('auth.forgot.title', 'Reset password')}</h1>
        <p className="login-subtitle">
          {t('auth.forgot.subtitle', 'Recover your account with email OTP.')}
        </p>

        {step === 'REQUEST_OTP' ? (
          <form
            className="form-grid"
            onSubmit={requestForm.handleSubmit((values) => requestOtpMutation.mutate(values))}
          >
            <label className="field field-span-2">
              <span>{t('auth.forgot.email', 'Email')}</span>
              <input type="email" autoComplete="email" {...requestForm.register('email')} />
              {requestForm.formState.errors.email ? (
                <small>{requestForm.formState.errors.email.message}</small>
              ) : null}
            </label>

            {requestError ? <p className="form-error field-span-2">{requestError}</p> : null}
            {message ? <p className="field-span-2">{message}</p> : null}

            <button
              type="submit"
              className="btn btn-primary field-span-2"
              disabled={requestOtpMutation.isPending}
            >
              {requestOtpMutation.isPending
                ? t('auth.forgot.sendingOtp', 'Sending OTP...')
                : t('auth.forgot.sendOtp', 'Send OTP')}
            </button>
          </form>
        ) : null}

        {step === 'VERIFY_OTP' ? (
          <form
            className="form-grid"
            onSubmit={verifyForm.handleSubmit((values) =>
              verifyOtpMutation.mutate({
                email,
                otp: values.otp,
              })
            )}
          >
            <p className="field-span-2 login-subtitle">
              {t('auth.forgot.otpSentTo', 'Enter OTP sent to')} <strong>{email}</strong>.
            </p>
            <label className="field field-span-2">
              <span>{t('auth.forgot.otpCode', 'OTP code')}</span>
              <input
                type="text"
                inputMode="numeric"
                maxLength={6}
                {...verifyForm.register('otp')}
              />
              {verifyForm.formState.errors.otp ? (
                <small>{verifyForm.formState.errors.otp.message}</small>
              ) : null}
            </label>

            {verifyError ? <p className="form-error field-span-2">{verifyError}</p> : null}
            {message ? <p className="field-span-2">{message}</p> : null}

            <button
              type="submit"
              className="btn btn-primary field-span-2"
              disabled={verifyOtpMutation.isPending}
            >
              {verifyOtpMutation.isPending
                ? t('auth.forgot.verifying', 'Verifying...')
                : t('auth.forgot.verifyOtp', 'Verify OTP')}
            </button>
          </form>
        ) : null}

        {step === 'RESET_PASSWORD' ? (
          <form
            className="form-grid"
            onSubmit={resetForm.handleSubmit((values) =>
              resetPasswordMutation.mutate({
                tempResetToken,
                newPassword: values.newPassword,
              })
            )}
          >
            <label className="field field-span-2">
              <span>{t('auth.forgot.newPassword', 'New password')}</span>
              <input
                type="password"
                autoComplete="new-password"
                {...resetForm.register('newPassword')}
              />
              {resetForm.formState.errors.newPassword ? (
                <small>{resetForm.formState.errors.newPassword.message}</small>
              ) : null}
            </label>

            <label className="field field-span-2">
              <span>{t('auth.forgot.confirmPassword', 'Confirm password')}</span>
              <input
                type="password"
                autoComplete="new-password"
                {...resetForm.register('confirmPassword')}
              />
              {resetForm.formState.errors.confirmPassword ? (
                <small>{resetForm.formState.errors.confirmPassword.message}</small>
              ) : null}
            </label>

            {resetError ? <p className="form-error field-span-2">{resetError}</p> : null}
            {message ? <p className="field-span-2">{message}</p> : null}

            <button
              type="submit"
              className="btn btn-primary field-span-2"
              disabled={resetPasswordMutation.isPending}
            >
              {resetPasswordMutation.isPending
                ? t('auth.forgot.updatingPassword', 'Updating password...')
                : t('auth.forgot.updatePassword', 'Update password')}
            </button>
          </form>
        ) : null}

        {step === 'DONE' && message ? <p>{message}</p> : null}

        <div className="demo-credentials">
          <Link to={appRoutes.login}>{t('auth.forgot.backToSignIn', 'Back to sign in')}</Link>
        </div>
      </section>
    </div>
  )
}
