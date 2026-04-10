import React, { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Modal } from '../../components/Modal'
import { PageHeader } from '../../components/PageHeader'
import { EmptyState } from '../../components/EmptyState'
import { ErrorState } from '../../components/ErrorState'
import { LoadingState } from '../../components/LoadingState'
import { normalizeApiError } from '../../core/api/errors'
import { useI18n } from '../../core/i18n/use-i18n'
import { formatDateTime } from '../../core/utils/format'
import { queryKeys } from '../../core/utils/query-keys'
import { getPatientMedicalTimeline, getPatientMedicalVisitDetail } from './api'

const statusClassName = (status?: string): string => {
  if (!status) {
    return 'status-pill status-none'
  }

  const normalized = status.toUpperCase()
  if (normalized === 'CANCEL' || normalized === 'CANCELLED') {
    return 'status-pill status-cancelled'
  }
  if (normalized === 'FINISH' || normalized === 'CONFIRMED') {
    return 'status-pill status-confirmed'
  }
  return 'status-pill status-pending-confirmation'
}

const statusLabel = (status?: string): string => {
  if (!status) {
    return 'PENDING_CONFIRMATION'
  }

  const normalized = status.toUpperCase()
  if (normalized === 'CANCEL' || normalized === 'CANCELLED') {
    return 'CANCELLED'
  }
  if (normalized === 'FINISH' || normalized === 'CONFIRMED') {
    return 'CONFIRMED'
  }
  return 'PENDING_CONFIRMATION'
}

export const PatientMedicalHistoryPage: React.FC = () => {
  const { t } = useI18n()
  const [selectedAppointmentId, setSelectedAppointmentId] = useState<string | null>(null)

  const timelineQuery = useQuery({
    queryKey: queryKeys.patientPortalHistory,
    queryFn: () => getPatientMedicalTimeline(100),
  })

  const detailQuery = useQuery({
    queryKey: [...queryKeys.patientPortalVisitDetail, selectedAppointmentId],
    queryFn: () => getPatientMedicalVisitDetail(selectedAppointmentId!),
    enabled: Boolean(selectedAppointmentId),
  })

  return (
    <section className="page-section">
      <PageHeader
        title={t('patientPortal.history.title', 'Medical History')}
        subtitle={t(
          'patientPortal.history.subtitle',
          'Review your visit timeline, diagnosis, and prescription details.'
        )}
      />

      <section className="card-section">
        {timelineQuery.isLoading ? (
          <LoadingState label={t('patientPortal.history.loading', 'Loading medical history...')} />
        ) : null}

        {timelineQuery.isError ? (
          <ErrorState
            message={normalizeApiError(timelineQuery.error).message}
            onRetry={() => void timelineQuery.refetch()}
          />
        ) : null}

        {timelineQuery.isSuccess && timelineQuery.data.entries.length === 0 ? (
          <EmptyState
            title={t('patientPortal.history.emptyTitle', 'No history records yet')}
            message={t(
              'patientPortal.history.emptyMessage',
              'Your completed visits and prescriptions will appear here.'
            )}
          />
        ) : null}

        {timelineQuery.isSuccess && timelineQuery.data.entries.length > 0 ? (
          <div className="timeline-list">
            {timelineQuery.data.entries.map((entry) => (
              <article key={entry.appointmentId} className="timeline-card">
                <div className="timeline-card-header">
                  <strong>{entry.appointmentCode}</strong>
                  <span>{formatDateTime(entry.appointmentTime)}</span>
                </div>

                <dl className="timeline-grid">
                  <div>
                    <dt>{t('patientPortal.history.fields.doctor', 'Doctor')}</dt>
                    <dd>{entry.doctorName ?? '-'}</dd>
                  </div>
                  <div>
                    <dt>{t('patientPortal.history.fields.status', 'Status')}</dt>
                    <dd>
                      <span className={statusClassName(entry.appointmentStatus)}>
                        {t(
                          `patientPortal.status.${statusLabel(entry.appointmentStatus).toLowerCase()}`,
                          statusLabel(entry.appointmentStatus)
                        )}
                      </span>
                    </dd>
                  </div>
                  <div className="field-span-2">
                    <dt>{t('patientPortal.history.fields.diagnosis', 'Diagnosis')}</dt>
                    <dd>{entry.diagnosis ?? '-'}</dd>
                  </div>
                  <div className="field-span-2">
                    <dt>{t('patientPortal.history.fields.notes', 'Notes')}</dt>
                    <dd>{entry.advice ?? '-'}</dd>
                  </div>
                </dl>

                <div className="timeline-actions">
                  <button
                    type="button"
                    className="btn btn-secondary"
                    onClick={() => setSelectedAppointmentId(entry.appointmentId)}
                  >
                    {t('patientPortal.history.actions.viewDetail', 'View details')}
                  </button>
                </div>
              </article>
            ))}
          </div>
        ) : null}
      </section>

      <Modal
        open={Boolean(selectedAppointmentId)}
        title={t('patientPortal.history.detailTitle', 'Visit details')}
        onClose={() => setSelectedAppointmentId(null)}
      >
        {detailQuery.isLoading ? (
          <LoadingState label={t('patientPortal.history.detailLoading', 'Loading visit detail...')} />
        ) : null}

        {detailQuery.isError ? (
          <ErrorState
            message={normalizeApiError(detailQuery.error).message}
            onRetry={() => void detailQuery.refetch()}
          />
        ) : null}

        {detailQuery.isSuccess ? (
          <div className="detail-stack">
            <dl className="detail-grid">
              <div>
                <dt>{t('patientPortal.history.fields.visitCode', 'Visit code')}</dt>
                <dd>{detailQuery.data.appointmentCode}</dd>
              </div>
              <div>
                <dt>{t('patientPortal.history.fields.visitTime', 'Visit time')}</dt>
                <dd>{formatDateTime(detailQuery.data.visitTime)}</dd>
              </div>
              <div>
                <dt>{t('patientPortal.history.fields.doctor', 'Doctor')}</dt>
                <dd>{detailQuery.data.doctorName ?? '-'}</dd>
              </div>
              <div>
                <dt>{t('patientPortal.history.fields.status', 'Status')}</dt>
                <dd>
                  <span className={statusClassName(detailQuery.data.appointmentStatus)}>
                    {t(
                      `patientPortal.status.${statusLabel(detailQuery.data.appointmentStatus).toLowerCase()}`,
                      statusLabel(detailQuery.data.appointmentStatus)
                    )}
                  </span>
                </dd>
              </div>
              <div className="field-span-2">
                <dt>{t('patientPortal.history.fields.diagnosis', 'Diagnosis')}</dt>
                <dd>{detailQuery.data.diagnosis ?? '-'}</dd>
              </div>
              <div className="field-span-2">
                <dt>{t('patientPortal.history.fields.notes', 'Notes')}</dt>
                <dd>{detailQuery.data.notes ?? '-'}</dd>
              </div>
            </dl>

            <section className="items-panel">
              <div className="items-header">
                <h3>{t('patientPortal.history.medications.title', 'Prescription items')}</h3>
              </div>

              {detailQuery.data.medications.length === 0 ? (
                <p>
                  {t(
                    'patientPortal.history.medications.empty',
                    'No medication data for this visit.'
                  )}
                </p>
              ) : (
                <div className="table-wrapper">
                  <table>
                    <thead>
                      <tr>
                        <th>{t('patientPortal.history.medications.drug', 'Drug')}</th>
                        <th>{t('patientPortal.history.medications.dosage', 'Dosage')}</th>
                        <th>{t('patientPortal.history.medications.quantity', 'Quantity')}</th>
                        <th>{t('patientPortal.history.medications.instructions', 'Instructions')}</th>
                      </tr>
                    </thead>
                    <tbody>
                      {detailQuery.data.medications.map((medication) => (
                        <tr key={medication.drugId}>
                          <td>{medication.drugName}</td>
                          <td>{medication.dosage ?? '-'}</td>
                          <td>{medication.quantity}</td>
                          <td>{medication.instructions ?? '-'}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </section>
          </div>
        ) : null}
      </Modal>
    </section>
  )
}
