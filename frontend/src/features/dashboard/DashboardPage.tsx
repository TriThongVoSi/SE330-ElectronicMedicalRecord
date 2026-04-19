import React from 'react'
import { useQuery } from '@tanstack/react-query'
import { EmptyState } from '../../components/EmptyState'
import { ErrorState } from '../../components/ErrorState'
import { LoadingState } from '../../components/LoadingState'
import { PageHeader } from '../../components/PageHeader'
import { normalizeApiError } from '../../core/api/errors'
import { useI18n } from '../../core/i18n/use-i18n'
import { formatDateTime } from '../../core/utils/format'
import { queryKeys } from '../../core/utils/query-keys'
import { getDashboardSummary } from './api'

export const DashboardPage: React.FC = () => {
  const { t } = useI18n()

  const summaryQuery = useQuery({
    queryKey: queryKeys.dashboardSummary,
    queryFn: getDashboardSummary,
  })

  return (
    <section className="page-section">
      <PageHeader
        title={t('dashboard.title', 'Dashboard')}
        subtitle={t('dashboard.subtitle', 'EMRal overview for appointments and patients.')}
      />

      {summaryQuery.isLoading ? (
        <LoadingState label={t('dashboard.loadingMetrics', 'Loading dashboard metrics...')} />
      ) : null}

      {summaryQuery.isError ? (
        <ErrorState
          message={normalizeApiError(summaryQuery.error).message}
          onRetry={() => void summaryQuery.refetch()}
        />
      ) : null}

      {summaryQuery.isSuccess ? (
        <>
          <div className="stats-grid">
            <article className="stat-card">
              <span>{t('dashboard.stats.totalAppointmentsToday', 'Total appointments today')}</span>
              <strong>{summaryQuery.data.totalAppointmentsToday}</strong>
            </article>
            <article className="stat-card">
              <span>{t('dashboard.stats.coming', 'Coming')}</span>
              <strong>{summaryQuery.data.comingAppointmentsToday}</strong>
            </article>
            <article className="stat-card">
              <span>{t('dashboard.stats.finished', 'Finished')}</span>
              <strong>{summaryQuery.data.finishedAppointmentsToday}</strong>
            </article>
            <article className="stat-card">
              <span>{t('dashboard.stats.cancelled', 'Cancelled')}</span>
              <strong>{summaryQuery.data.cancelledAppointmentsToday}</strong>
            </article>
            <article className="stat-card">
              <span>{t('dashboard.stats.newPatientsToday', 'New patients today')}</span>
              <strong>{summaryQuery.data.newPatientsToday}</strong>
            </article>
          </div>

          <section className="card-section">
            <h2>{t('dashboard.upcoming.title', 'Upcoming appointments')}</h2>
            {summaryQuery.data.upcomingAppointments.length === 0 ? (
              <EmptyState
                title={t('dashboard.upcoming.emptyTitle', 'No upcoming appointments')}
                message={t(
                  'dashboard.upcoming.emptyMessage',
                  'Appointments scheduled for the current timeline will show here.'
                )}
              />
            ) : (
              <div className="table-wrapper">
                <table>
                  <thead>
                    <tr>
                      <th>{t('dashboard.table.time', 'Time')}</th>
                      <th>{t('dashboard.table.patient', 'Patient')}</th>
                      <th>{t('dashboard.table.doctor', 'Doctor')}</th>
                      <th>{t('dashboard.table.status', 'Status')}</th>
                    </tr>
                  </thead>
                  <tbody>
                    {summaryQuery.data.upcomingAppointments.map((item) => (
                      <tr key={item.id}>
                        <td>{formatDateTime(item.appointmentTime)}</td>
                        <td>{item.patientName}</td>
                        <td>{item.doctorName}</td>
                        <td>
                          <span className={`status-pill status-${item.status.toLowerCase()}`}>
                            {t(`status.${item.status.toLowerCase()}`, item.status)}
                          </span>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </section>
        </>
      ) : null}
    </section>
  )
}
