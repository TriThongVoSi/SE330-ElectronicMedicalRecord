import React from 'react'
import { Link } from 'react-router-dom'
import { useAuthStore } from '../features/auth/auth-store'
import { resolveRoleHomeRoute } from '../features/auth/route-utils'
import { appRoutes } from '../app/routes'
import { useI18n } from '../core/i18n/use-i18n'

export const ForbiddenPage: React.FC = () => {
  const { t } = useI18n()
  const user = useAuthStore((state) => state.user)

  const homeRoute = user ? resolveRoleHomeRoute(user.role) : appRoutes.login

  return (
    <section className="card-section">
      <h1>{t('forbidden.title', 'Access denied')}</h1>
      <p>
        {t('forbidden.message', 'Your current role does not have permission to open this page.')}
      </p>
      <Link className="btn btn-primary" to={homeRoute}>
        {t('forbidden.backToDashboard', 'Back to dashboard')}
      </Link>
    </section>
  )
}
