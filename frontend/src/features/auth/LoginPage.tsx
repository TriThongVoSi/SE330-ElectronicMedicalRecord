import React from 'react'
import { useMutation } from '@tanstack/react-query'
import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { z } from 'zod'
import { appRoutes } from '../../app/routes'
import { normalizeApiError } from '../../core/api/errors'
import { useI18n } from '../../core/i18n/use-i18n'
import { login } from './api'
import { useAuthStore } from './auth-store'
import { resolveRoleHomeRoute } from './route-utils'
import type { AuthTokenResponse } from './types'

const loginSchema = z.object({
  identifier: z.string().trim().min(1, 'Username or email is required.'),
  password: z.string().min(1, 'Password is required.'),
})

type LoginFormValues = z.infer<typeof loginSchema>

type LoginLocationState = {
  from?: string
}

const resolveRedirectTarget = (
  locationState: LoginLocationState | null,
  session: AuthTokenResponse
): string => {
  if (session.user.mustChangePassword) {
    return appRoutes.firstLoginPassword
  }

  if (locationState?.from && locationState.from !== appRoutes.firstLoginPassword) {
    return locationState.from
  }

  return resolveRoleHomeRoute(session.user.role)
}

export const LoginPage: React.FC = () => {
  const navigate = useNavigate()
  const location = useLocation()
  const setSession = useAuthStore((state) => state.setSession)
  const { t } = useI18n()

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<LoginFormValues>({
    resolver: zodResolver(loginSchema),
    defaultValues: {
      identifier: '',
      password: '',
    },
  })

  const completeLogin = React.useCallback(
    (session: AuthTokenResponse) => {
      setSession(session)
      const state = location.state as LoginLocationState | null
      navigate(resolveRedirectTarget(state, session), { replace: true })
    },
    [location.state, navigate, setSession]
  )

  const mutation = useMutation({
    mutationFn: login,
    onSuccess: completeLogin,
  })

  const onSubmit = (values: LoginFormValues): void => {
    mutation.mutate(values)
  }

  const isSubmitting = mutation.isPending
  const errorMessage = mutation.error ? normalizeApiError(mutation.error).message : null

  return (
    <div className="login-page">
      <section className="login-panel">
        <p className="brand-tag">{t('auth.brandTag', 'Electronic Medical Record')}</p>
        <h1>{t('auth.login.title', 'Sign in')}</h1>
        <p className="login-subtitle">
          {t('auth.login.subtitle', 'Use your account to access doctor and patient EMR workflows.')}
        </p>

        <form className="form-grid" onSubmit={handleSubmit(onSubmit)}>
          <label className="field">
            <span>{t('auth.login.identifier', 'Username or email')}</span>
            <input type="text" autoComplete="username" {...register('identifier')} />
            {errors.identifier ? <small>{errors.identifier.message}</small> : null}
          </label>

          <label className="field">
            <span>{t('auth.login.password', 'Password')}</span>
            <input type="password" autoComplete="current-password" {...register('password')} />
            {errors.password ? <small>{errors.password.message}</small> : null}
          </label>

          {errorMessage ? <p className="form-error field-span-2">{errorMessage}</p> : null}

          <button type="submit" className="btn btn-primary field-span-2" disabled={isSubmitting}>
            {mutation.isPending
              ? t('auth.login.signingIn', 'Signing in...')
              : t('auth.login.signIn', 'Sign in')}
          </button>
        </form>

        <div className="demo-credentials">
          <p className="auth-links">
            <Link to={appRoutes.forgotPassword}>
              {t('auth.login.forgotPassword', 'Forgot password?')}
            </Link>
          </p>
          <strong>{t('auth.login.seedTitle', 'Local seed credentials')}</strong>
          <ul>
            <li>admin.local / admin123</li>
            <li>doctor.local / doctor123</li>
            <li>patient.local / patient123</li>
          </ul>
          <p>{t('auth.login.provisioningOnly', 'Patient accounts are provisioned internally.')}</p>
        </div>
      </section>
    </div>
  )
}

