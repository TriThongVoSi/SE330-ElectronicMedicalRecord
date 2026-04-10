import React from 'react'
import { Link } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { PageHeader } from '../../components/PageHeader'
import { LoadingState } from '../../components/LoadingState'
import { ErrorState } from '../../components/ErrorState'
import { normalizeApiError } from '../../core/api/errors'
import { useI18n } from '../../core/i18n/use-i18n'
import { formatDateTime } from '../../core/utils/format'
import { queryKeys } from '../../core/utils/query-keys'
import { appRoutes } from '../../app/routes'
import { getPatientPortalDashboard } from './api'
import type { PatientMedicalTimelineEntry } from './types'

const TimelineHighlight: React.FC<{
  title: string
  entry?: PatientMedicalTimelineEntry | null
  emptyMessage: string
}> = ({ title, entry, emptyMessage }) => {
  if (!entry) {
    return (
      <article className="stat-card">
        <span>{title}</span>
        <p>{emptyMessage}</p>
      </article>
    )
  }

  return (
    <article className="stat-card">
      <span>{title}</span>
      <strong>{entry.appointmentCode}</strong>
      <p>{formatDateTime(entry.appointmentTime)}</p>
      <p>{entry.doctorName ?? '-'}</p>
      <p>{entry.diagnosis ?? '-'}</p>
    </article>
  )
}

export const PatientDashboardPage: React.FC = () => {
  const { t } = useI18n()

  const dashboardQuery = useQuery({
    queryKey: queryKeys.patientPortalDashboard,
    queryFn: getPatientPortalDashboard,
  })

  return (
    <section className="page-section">
      <PageHeader
        title={t('patientPortal.dashboard.title', 'My Health Dashboard')}
        subtitle={t(
          'patientPortal.dashboard.subtitle',
          'Track your upcoming appointments and latest visit updates.'
        )}
      />

      {dashboardQuery.isLoading ? (
        <LoadingState label={t('patientPortal.dashboard.loading', 'Loading dashboard...')} />
      ) : null}

      {dashboardQuery.isError ? (
        <ErrorState
          message={normalizeApiError(dashboardQuery.error).message}
          onRetry={() => void dashboardQuery.refetch()}
        />
      ) : null}

      {dashboardQuery.isSuccess ? (
        <>
          <div className="stats-grid">
            <article className="stat-card">
              <span>{t('patientPortal.dashboard.stats.upcoming', 'Upcoming appointments')}</span>
              <strong>{dashboardQuery.data.upcomingAppointments}</strong>
            </article>
            <TimelineHighlight
              title={t('patientPortal.dashboard.stats.latestVisit', 'Latest visit')}
              entry={dashboardQuery.data.latestVisit}
              emptyMessage={t('patientPortal.dashboard.emptyVisit', 'No visit history yet.')}
            />
            <TimelineHighlight
              title={t('patientPortal.dashboard.stats.latestPrescription', 'Latest prescription')}
              entry={dashboardQuery.data.latestPrescription}
              emptyMessage={t('patientPortal.dashboard.emptyPrescription', 'No prescription yet.')}
            />
          </div>

          <section className="card-section">
            <h2>{t('patientPortal.dashboard.quickActions', 'Quick actions')}</h2>
            <div className="quick-actions-grid">
              <Link to={appRoutes.patientAppointments} className="btn btn-primary">
                {t('patientPortal.dashboard.actions.bookAppointment', 'Book appointment')}
              </Link>
              <Link to={appRoutes.patientMedicalHistory} className="btn btn-secondary">
                {t('patientPortal.dashboard.actions.viewHistory', 'View history')}
              </Link>
              <Link to={appRoutes.patientProfile} className="btn btn-secondary">
                {t('patientPortal.dashboard.actions.updateProfile', 'Update profile')}
              </Link>
            </div>
          </section>
        </>
      ) : null}
    </section>
  )
}
