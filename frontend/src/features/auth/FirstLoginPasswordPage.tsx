import React from 'react'
import { useMutation } from '@tanstack/react-query'
import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'
import { z } from 'zod'
import { useNavigate } from 'react-router-dom'
import { normalizeApiError } from '../../core/api/errors'
import { useI18n } from '../../core/i18n/use-i18n'
import { useNotify } from '../../core/utils/notify'
import { changeFirstLoginPassword } from './api'
import { useAuthStore } from './auth-store'
import { resolveRoleHomeRoute } from './route-utils'

const passwordSchema = z
  .object({
    currentPassword: z.string().min(1, 'Current password is required.'),
    newPassword: z.string().min(8, 'Password must be at least 8 characters.'),
    confirmPassword: z.string().min(8, 'Confirm password is required.'),
  })
  .refine((values) => values.newPassword === values.confirmPassword, {
    path: ['confirmPassword'],
    message: 'Passwords do not match.',
  })

type PasswordFormValues = z.infer<typeof passwordSchema>

export const FirstLoginPasswordPage: React.FC = () => {
  const navigate = useNavigate()
  const notify = useNotify()
  const { t } = useI18n()
  const user = useAuthStore((state) => state.user)
  const setUser = useAuthStore((state) => state.setUser)

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<PasswordFormValues>({
    resolver: zodResolver(passwordSchema),
    defaultValues: {
      currentPassword: '',
      newPassword: '',
      confirmPassword: '',
    },
  })

  const mutation = useMutation({
    mutationFn: changeFirstLoginPassword,
    onSuccess: () => {
      if (user) {
        setUser({ ...user, mustChangePassword: false })
        navigate(resolveRoleHomeRoute(user.role), { replace: true })
      }
      notify.success(
        t('auth.firstLogin.toast.successTitle', 'Password updated'),
        t('auth.firstLogin.toast.successDescription', 'Your account is now fully activated.')
      )
    },
  })

  const onSubmit = (values: PasswordFormValues): void => {
    mutation.mutate({
      currentPassword: values.currentPassword,
      newPassword: values.newPassword,
    })
  }

  const errorMessage = mutation.error ? normalizeApiError(mutation.error).message : null

  return (
    <div className="login-page">
      <section className="login-panel">
        <p className="brand-tag">{t('auth.brandTag', 'Electronic Medical Record')}</p>
        <h1>{t('auth.firstLogin.title', 'Change temporary password')}</h1>
        <p className="login-subtitle">
          {t(
            'auth.firstLogin.subtitle',
            'This is your first login. You must set a new password before continuing.'
          )}
        </p>

        <form className="form-grid" onSubmit={handleSubmit(onSubmit)}>
          <label className="field field-span-2">
            <span>{t('auth.firstLogin.currentPassword', 'Current password')}</span>
            <input type="password" autoComplete="current-password" {...register('currentPassword')} />
            {errors.currentPassword ? <small>{errors.currentPassword.message}</small> : null}
          </label>

          <label className="field field-span-2">
            <span>{t('auth.firstLogin.newPassword', 'New password')}</span>
            <input type="password" autoComplete="new-password" {...register('newPassword')} />
            {errors.newPassword ? <small>{errors.newPassword.message}</small> : null}
          </label>

          <label className="field field-span-2">
            <span>{t('auth.firstLogin.confirmPassword', 'Confirm password')}</span>
            <input type="password" autoComplete="new-password" {...register('confirmPassword')} />
            {errors.confirmPassword ? <small>{errors.confirmPassword.message}</small> : null}
          </label>

          <p className="field-span-2 first-login-hint">
            {t(
              'auth.firstLogin.passwordRule',
              'Password should contain at least 8 characters and should not match your temporary password.'
            )}
          </p>

          {errorMessage ? <p className="form-error field-span-2">{errorMessage}</p> : null}

          <button type="submit" className="btn btn-primary field-span-2" disabled={mutation.isPending}>
            {mutation.isPending
              ? t('auth.firstLogin.submitting', 'Updating password...')
              : t('auth.firstLogin.submit', 'Update password')}
          </button>
        </form>
      </section>
    </div>
  )
}
