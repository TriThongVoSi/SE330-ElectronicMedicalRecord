import React from 'react'
import { Link } from 'react-router-dom'
import { useAuthStore } from '../features/auth/auth-store'
import { resolveRoleHomeRoute } from '../features/auth/route-utils'
import { appRoutes } from '../app/routes'
import { useI18n } from '../core/i18n/use-i18n'

export const NotFoundPage: React.FC = () => {
  const { t } = useI18n()
  const user = useAuthStore((state) => state.user)

  const homeRoute = user ? resolveRoleHomeRoute(user.role) : appRoutes.login

  return (
    <section className="card-section">
      <h1>{t('notFound.title', 'Page not found')}</h1>
      <p>{t('notFound.message', 'The route you requested does not exist.')}</p>
      <Link className="btn btn-primary" to={homeRoute}>
        {t('notFound.goToDashboard', 'Go to dashboard')}
      </Link>
    </section>
  )
}
