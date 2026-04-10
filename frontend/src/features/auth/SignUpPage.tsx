import React, { useState } from 'react'
import { useMutation } from '@tanstack/react-query'
import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'
import { Link, useNavigate } from 'react-router-dom'
import { z } from 'zod'
import { appRoutes } from '../../app/routes'
import { normalizeApiError } from '../../core/api/errors'
import { useI18n } from '../../core/i18n/use-i18n'
import { signUp, verifySignUpOtp } from './api'

const signUpSchema = z
  .object({
    username: z.string().trim().min(3, 'Username must be at least 3 characters.'),
    email: z.string().trim().email('Invalid email address.'),
    fullName: z.string().trim().min(1, 'Full name is required.'),
    password: z.string().min(6, 'Password must be at least 6 characters.'),
    confirmPassword: z.string().min(6, 'Confirm password is required.'),
  })
  .refine((formValues) => formValues.password === formValues.confirmPassword, {
    path: ['confirmPassword'],
    message: 'Passwords do not match.',
  })

const verifySchema = z.object({
  otp: z
    .string()
    .trim()
    .regex(/^\d{6}$/, 'OTP must be 6 digits.'),
})

type SignUpFormValues = z.infer<typeof signUpSchema>
type VerifyFormValues = z.infer<typeof verifySchema>

export const SignUpPage: React.FC = () => {
  const navigate = useNavigate()
  const [pendingEmail, setPendingEmail] = useState<string | null>(null)
  const [verificationMessage, setVerificationMessage] = useState<string | null>(null)
  const { t } = useI18n()

  const signUpForm = useForm<SignUpFormValues>({
    resolver: zodResolver(signUpSchema),
    defaultValues: {
      username: '',
      email: '',
      fullName: '',
      password: '',
      confirmPassword: '',
    },
  })

  const verifyForm = useForm<VerifyFormValues>({
    resolver: zodResolver(verifySchema),
    defaultValues: { otp: '' },
  })

  const signUpMutation = useMutation({
    mutationFn: signUp,
    onSuccess: (_, values) => {
      setPendingEmail(values.email.trim())
      setVerificationMessage(
        t('auth.signup.otpSent', 'OTP sent. Enter the code to activate your account.')
      )
    },
  })

  const verifyMutation = useMutation({
    mutationFn: verifySignUpOtp,
    onSuccess: () => {
      setVerificationMessage(
        t('auth.signup.accountVerified', 'Account verified. You can now sign in.')
      )
      window.setTimeout(() => {
        navigate(appRoutes.login, { replace: true })
      }, 1000)
    },
  })

  const signUpError = signUpMutation.error ? normalizeApiError(signUpMutation.error).message : null
  const verifyError = verifyMutation.error ? normalizeApiError(verifyMutation.error).message : null

  return (
    <div className="login-page">
      <section className="login-panel">
        <p className="brand-tag">{t('auth.brandTag', 'Electronic Medical Record')}</p>
        <h1>{t('auth.signup.title', 'Create account')}</h1>
        <p className="login-subtitle">
          {t('auth.signup.subtitle', 'Register a local account and verify it with OTP.')}
        </p>

        {!pendingEmail ? (
          <form
            className="form-grid"
            onSubmit={signUpForm.handleSubmit((values) => signUpMutation.mutate(values))}
          >
            <label className="field field-span-2">
              <span>{t('auth.signup.fullName', 'Full name')}</span>
              <input type="text" autoComplete="name" {...signUpForm.register('fullName')} />
              {signUpForm.formState.errors.fullName ? (
                <small>{signUpForm.formState.errors.fullName.message}</small>
              ) : null}
            </label>

            <label className="field">
              <span>{t('auth.signup.username', 'Username')}</span>
              <input type="text" autoComplete="username" {...signUpForm.register('username')} />
              {signUpForm.formState.errors.username ? (
                <small>{signUpForm.formState.errors.username.message}</small>
              ) : null}
            </label>

            <label className="field">
              <span>{t('auth.signup.email', 'Email')}</span>
              <input type="email" autoComplete="email" {...signUpForm.register('email')} />
              {signUpForm.formState.errors.email ? (
                <small>{signUpForm.formState.errors.email.message}</small>
              ) : null}
            </label>

            <label className="field">
              <span>{t('auth.signup.password', 'Password')}</span>
              <input
                type="password"
                autoComplete="new-password"
                {...signUpForm.register('password')}
              />
              {signUpForm.formState.errors.password ? (
                <small>{signUpForm.formState.errors.password.message}</small>
              ) : null}
            </label>

            <label className="field">
              <span>{t('auth.signup.confirmPassword', 'Confirm password')}</span>
              <input
                type="password"
                autoComplete="new-password"
                {...signUpForm.register('confirmPassword')}
              />
              {signUpForm.formState.errors.confirmPassword ? (
                <small>{signUpForm.formState.errors.confirmPassword.message}</small>
              ) : null}
            </label>

            {signUpError ? <p className="form-error field-span-2">{signUpError}</p> : null}

            <button
              type="submit"
              className="btn btn-primary field-span-2"
              disabled={signUpMutation.isPending}
            >
              {signUpMutation.isPending
                ? t('auth.signup.submitting', 'Submitting...')
                : t('auth.signup.sendOtp', 'Send OTP')}
            </button>
          </form>
        ) : (
          <form
            className="form-grid"
            onSubmit={verifyForm.handleSubmit((values) =>
              verifyMutation.mutate({
                email: pendingEmail,
                otp: values.otp,
              })
            )}
          >
            <p className="field-span-2 login-subtitle">
              {t('auth.signup.otpSentTo', 'OTP was sent to')} <strong>{pendingEmail}</strong>.
            </p>
            <label className="field field-span-2">
              <span>{t('auth.signup.otpCode', 'OTP code')}</span>
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
            {verificationMessage ? <p className="field-span-2">{verificationMessage}</p> : null}

            <button
              type="submit"
              className="btn btn-primary field-span-2"
              disabled={verifyMutation.isPending}
            >
              {verifyMutation.isPending
                ? t('auth.signup.verifying', 'Verifying...')
                : t('auth.signup.verifyAccount', 'Verify account')}
            </button>
          </form>
        )}

        <div className="demo-credentials">
          <Link to={appRoutes.login}>{t('auth.signup.backToSignIn', 'Back to sign in')}</Link>
        </div>
      </section>
    </div>
  )
}
