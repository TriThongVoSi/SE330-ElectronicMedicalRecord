import React, { useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'
import { z } from 'zod'
import { Modal } from '../../components/Modal'
import { PageHeader } from '../../components/PageHeader'
import { PaginatedQueryContent } from '../../components/PaginatedQueryContent'
import { normalizeApiError } from '../../core/api/errors'
import { useI18n } from '../../core/i18n/use-i18n'
import { formatDate, formatDateTime } from '../../core/utils/format'
import { useNotify } from '../../core/utils/notify'
import { queryKeys } from '../../core/utils/query-keys'
import { createPatient, getPatientMedicalTimeline, listPatients, updatePatient } from './api'
import type { Patient, PatientUpsertInput } from './types'

const optionalPositiveNumber = z.preprocess(
  (inputValue) =>
    inputValue === '' || inputValue === null || inputValue === undefined ? undefined : inputValue,
  z.coerce.number().positive().optional()
)

const patientSchema = z.object({
  patientCode: z.string().trim().min(3, 'Patient code is required.'),
  fullName: z.string().trim().min(2, 'Full name is required.'),
  dateOfBirth: z.string().optional(),
  email: z.string().email('Invalid email format.').optional().or(z.literal('')),
  gender: z.string().trim().min(1, 'Gender is required.'),
  phone: z.string().trim().min(8, 'Phone is required.'),
  address: z.string().trim().optional(),
  diagnosis: z.string().trim().optional(),
  drugAllergies: z.string().trim().optional(),
  heightCm: optionalPositiveNumber,
  weightKg: optionalPositiveNumber,
})

type PatientFormInput = z.input<typeof patientSchema>
type PatientFormValues = z.output<typeof patientSchema>

const emptyFormValues: PatientFormInput = {
  patientCode: '',
  fullName: '',
  dateOfBirth: '',
  email: '',
  gender: '',
  phone: '',
  address: '',
  diagnosis: '',
  drugAllergies: '',
  heightCm: undefined,
  weightKg: undefined,
}

const toPayload = (values: PatientFormValues): PatientUpsertInput => ({
  patientCode: values.patientCode.trim(),
  fullName: values.fullName.trim(),
  dateOfBirth: values.dateOfBirth || undefined,
  email: values.email || undefined,
  gender: values.gender.trim(),
  phone: values.phone.trim(),
  address: values.address || undefined,
  diagnosis: values.diagnosis || undefined,
  drugAllergies: values.drugAllergies || undefined,
  heightCm: values.heightCm,
  weightKg: values.weightKg,
})

const toFormValues = (patient: Patient): PatientFormInput => ({
  patientCode: patient.patientCode,
  fullName: patient.fullName,
  dateOfBirth: patient.dateOfBirth ?? '',
  email: patient.email ?? '',
  gender: patient.gender,
  phone: patient.phone,
  address: patient.address ?? '',
  diagnosis: patient.diagnosis ?? '',
  drugAllergies: patient.drugAllergies ?? '',
  heightCm: patient.heightCm,
  weightKg: patient.weightKg,
})

const timelineStatusClass = (status: string | undefined): string => {
  if (!status) {
    return 'status-pill status-none'
  }

  return `status-pill status-${status.toLowerCase()}`
}

export const PatientsPage: React.FC = () => {
  const queryClient = useQueryClient()
  const notify = useNotify()
  const { t } = useI18n()

  const [search, setSearch] = useState('')
  const [codeFilter, setCodeFilter] = useState('')
  const [phoneFilter, setPhoneFilter] = useState('')
  const [page, setPage] = useState(1)
  const [isFormModalOpen, setIsFormModalOpen] = useState(false)
  const [editingPatient, setEditingPatient] = useState<Patient | null>(null)
  const [viewingPatient, setViewingPatient] = useState<Patient | null>(null)
  const [timelinePatient, setTimelinePatient] = useState<Patient | null>(null)

  const {
    register,
    handleSubmit,
    reset,
    setError,
    formState: { errors },
  } = useForm<PatientFormInput, unknown, PatientFormValues>({
    resolver: zodResolver(patientSchema),
    defaultValues: emptyFormValues,
  })

  const queryKey = useMemo(
    () => [...queryKeys.patients, page, search, codeFilter, phoneFilter],
    [page, search, codeFilter, phoneFilter]
  )

  const patientsQuery = useQuery({
    queryKey,
    queryFn: () =>
      listPatients({
        page,
        size: 10,
        search,
        code: codeFilter,
        phone: phoneFilter,
      }),
  })

  const timelineQuery = useQuery({
    queryKey: [...queryKeys.patientTimeline, timelinePatient?.id],
    queryFn: () => getPatientMedicalTimeline(timelinePatient!.id, 100),
    enabled: Boolean(timelinePatient),
  })

  const createMutation = useMutation({
    mutationFn: createPatient,
    onSuccess: () => {
      notify.success(
        t('patients.toast.createdTitle', 'Patient created'),
        t('patients.toast.createdDescription', 'New patient record has been saved.')
      )
      setIsFormModalOpen(false)
      reset(emptyFormValues)
      void queryClient.invalidateQueries({ queryKey: queryKeys.patients })
    },
  })

  const updateMutation = useMutation({
    mutationFn: ({ id, payload }: { id: string; payload: PatientUpsertInput }) =>
      updatePatient(id, payload),
    onSuccess: () => {
      notify.success(
        t('patients.toast.updatedTitle', 'Patient updated'),
        t('patients.toast.updatedDescription', 'Patient information has been updated.')
      )
      setIsFormModalOpen(false)
      setEditingPatient(null)
      reset(emptyFormValues)
      void queryClient.invalidateQueries({ queryKey: queryKeys.patients })
    },
  })

  const openCreateModal = (): void => {
    setEditingPatient(null)
    reset(emptyFormValues)
    setIsFormModalOpen(true)
  }

  const openEditModal = (patient: Patient): void => {
    setEditingPatient(patient)
    reset(toFormValues(patient))
    setIsFormModalOpen(true)
  }

  const submitForm = (values: PatientFormValues): void => {
    const payload = toPayload(values)

    const mutation = editingPatient
      ? updateMutation.mutateAsync({ id: editingPatient.id, payload })
      : createMutation.mutateAsync(payload)

    void mutation.catch((error: unknown) => {
      const normalized = normalizeApiError(error)

      if (normalized.fieldErrors) {
        for (const [field, message] of Object.entries(normalized.fieldErrors)) {
          const supportedField = field as keyof PatientFormInput
          setError(supportedField, { message })
        }
      } else {
        notify.error(t('patients.toast.saveErrorTitle', 'Cannot save patient'), normalized.message)
      }
    })
  }

  const isSubmitting = createMutation.isPending || updateMutation.isPending

  return (
    <section className="page-section">
      <PageHeader
        title={t('patients.title', 'Patients')}
        subtitle={t(
          'patients.subtitle',
          'Manage patient records with search, filters, and editable profiles.'
        )}
        action={
          <button type="button" className="btn btn-primary" onClick={openCreateModal}>
            {t('patients.newButton', 'New patient')}
          </button>
        }
      />

      <section className="card-section">
        <div className="toolbar-grid">
          <label className="field">
            <span>{t('patients.filters.search', 'Search')}</span>
            <input
              value={search}
              onChange={(event) => {
                setPage(1)
                setSearch(event.target.value)
              }}
              placeholder={t('patients.filters.searchPlaceholder', 'Code, name, phone')}
            />
          </label>
          <label className="field">
            <span>{t('patients.filters.patientCode', 'Patient code')}</span>
            <input
              value={codeFilter}
              onChange={(event) => {
                setPage(1)
                setCodeFilter(event.target.value)
              }}
              placeholder="PT-0001"
            />
          </label>
          <label className="field">
            <span>{t('patients.filters.phone', 'Phone')}</span>
            <input
              value={phoneFilter}
              onChange={(event) => {
                setPage(1)
                setPhoneFilter(event.target.value)
              }}
              placeholder="090..."
            />
          </label>
        </div>

        <PaginatedQueryContent
          query={patientsQuery}
          emptyTitle={t('patients.emptyTitle', 'No patients found')}
          emptyMessage={t(
            'patients.emptyMessage',
            'Try changing filters or create a new patient record.'
          )}
          getErrorMessage={(error) => normalizeApiError(error).message}
          onRetry={() => void patientsQuery.refetch()}
          onPageChange={setPage}
        >
          {(patients) => (
            <div className="table-wrapper">
              <table>
                <thead>
                  <tr>
                    <th>{t('patients.table.code', 'Code')}</th>
                    <th>{t('patients.table.fullName', 'Full name')}</th>
                    <th>{t('patients.table.phone', 'Phone')}</th>
                    <th>{t('patients.table.gender', 'Gender')}</th>
                    <th>{t('patients.table.dateOfBirth', 'Date of birth')}</th>
                    <th>{t('patients.table.actions', 'Actions')}</th>
                  </tr>
                </thead>
                <tbody>
                  {patients.map((patient) => (
                    <tr key={patient.id}>
                      <td>{patient.patientCode}</td>
                      <td>{patient.fullName}</td>
                      <td>{patient.phone}</td>
                      <td>{patient.gender}</td>
                      <td>{formatDate(patient.dateOfBirth)}</td>
                      <td>
                        <div className="table-actions">
                          <button
                            className="btn btn-secondary"
                            type="button"
                            onClick={() => setViewingPatient(patient)}
                          >
                            {t('common.actions.view', 'View')}
                          </button>
                          <button
                            className="btn btn-secondary"
                            type="button"
                            onClick={() => setTimelinePatient(patient)}
                          >
                            {t('patients.actions.timeline', 'Timeline')}
                          </button>
                          <button
                            className="btn btn-secondary"
                            type="button"
                            onClick={() => openEditModal(patient)}
                          >
                            {t('common.actions.edit', 'Edit')}
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </PaginatedQueryContent>
      </section>

      <Modal
        open={isFormModalOpen}
        title={
          editingPatient
            ? t('patients.modal.updateTitle', 'Update patient')
            : t('patients.modal.createTitle', 'Create patient')
        }
        onClose={() => {
          setIsFormModalOpen(false)
          setEditingPatient(null)
        }}
      >
        <form className="form-grid" onSubmit={handleSubmit(submitForm)}>
          <label className="field">
            <span>{t('patients.form.patientCode', 'Patient code')}</span>
            <input {...register('patientCode')} />
            {errors.patientCode ? <small>{errors.patientCode.message}</small> : null}
          </label>
          <label className="field">
            <span>{t('patients.form.fullName', 'Full name')}</span>
            <input {...register('fullName')} />
            {errors.fullName ? <small>{errors.fullName.message}</small> : null}
          </label>
          <label className="field">
            <span>{t('patients.form.dateOfBirth', 'Date of birth')}</span>
            <input type="date" {...register('dateOfBirth')} />
          </label>
          <label className="field">
            <span>{t('patients.form.email', 'Email')}</span>
            <input type="email" {...register('email')} />
            {errors.email ? <small>{errors.email.message}</small> : null}
          </label>
          <label className="field">
            <span>{t('patients.form.gender', 'Gender')}</span>
            <input
              {...register('gender')}
              placeholder={t('patients.form.genderPlaceholder', 'Male/Female')}
            />
            {errors.gender ? <small>{errors.gender.message}</small> : null}
          </label>
          <label className="field">
            <span>{t('patients.form.phone', 'Phone')}</span>
            <input {...register('phone')} />
            {errors.phone ? <small>{errors.phone.message}</small> : null}
          </label>
          <label className="field field-span-2">
            <span>{t('patients.form.address', 'Address')}</span>
            <input {...register('address')} />
          </label>
          <label className="field field-span-2">
            <span>{t('patients.form.diagnosis', 'Diagnosis')}</span>
            <textarea rows={3} {...register('diagnosis')} />
          </label>
          <label className="field field-span-2">
            <span>{t('patientPortal.profile.fields.drugAllergies', 'Drug allergies')}</span>
            <textarea rows={3} {...register('drugAllergies')} />
          </label>
          <label className="field">
            <span>{t('patients.form.height', 'Height (cm)')}</span>
            <input type="number" step="0.01" {...register('heightCm')} />
          </label>
          <label className="field">
            <span>{t('patients.form.weight', 'Weight (kg)')}</span>
            <input type="number" step="0.01" {...register('weightKg')} />
          </label>

          <div className="form-actions field-span-2">
            <button
              type="button"
              className="btn btn-secondary"
              onClick={() => setIsFormModalOpen(false)}
            >
              {t('common.actions.cancel', 'Cancel')}
            </button>
            <button type="submit" className="btn btn-primary" disabled={isSubmitting}>
              {isSubmitting
                ? t('common.actions.saving', 'Saving...')
                : t('patients.form.saveButton', 'Save patient')}
            </button>
          </div>
        </form>
      </Modal>

      <Modal
        open={Boolean(viewingPatient)}
        title={t('patients.detail.title', 'Patient detail')}
        onClose={() => setViewingPatient(null)}
      >
        {viewingPatient ? (
          <dl className="detail-grid">
            <div>
              <dt>{t('patients.detail.code', 'Code')}</dt>
              <dd>{viewingPatient.patientCode}</dd>
            </div>
            <div>
              <dt>{t('patients.detail.fullName', 'Full name')}</dt>
              <dd>{viewingPatient.fullName}</dd>
            </div>
            <div>
              <dt>{t('patients.detail.phone', 'Phone')}</dt>
              <dd>{viewingPatient.phone}</dd>
            </div>
            <div>
              <dt>{t('patients.detail.email', 'Email')}</dt>
              <dd>{viewingPatient.email ?? '-'}</dd>
            </div>
            <div>
              <dt>{t('patients.detail.dateOfBirth', 'Date of birth')}</dt>
              <dd>{formatDate(viewingPatient.dateOfBirth)}</dd>
            </div>
            <div>
              <dt>{t('patients.detail.gender', 'Gender')}</dt>
              <dd>{viewingPatient.gender}</dd>
            </div>
            <div className="field-span-2">
              <dt>{t('patients.detail.diagnosis', 'Diagnosis')}</dt>
              <dd>{viewingPatient.diagnosis ?? '-'}</dd>
            </div>
            <div className="field-span-2">
              <dt>{t('patientPortal.profile.fields.drugAllergies', 'Drug allergies')}</dt>
              <dd>{viewingPatient.drugAllergies ?? '-'}</dd>
            </div>
            <div className="field-span-2">
              <dt>{t('patients.detail.address', 'Address')}</dt>
              <dd>{viewingPatient.address ?? '-'}</dd>
            </div>
          </dl>
        ) : null}
      </Modal>

      <Modal
        open={Boolean(timelinePatient)}
        title={
          timelinePatient
            ? t('patients.timeline.titleWithName', 'Medical timeline - {fullName}', {
                fullName: timelinePatient.fullName,
              })
            : t('patients.timeline.title', 'Medical timeline')
        }
        onClose={() => setTimelinePatient(null)}
      >
        {timelineQuery.isLoading ? (
          <p>{t('patients.timeline.loading', 'Loading patient timeline...')}</p>
        ) : null}

        {timelineQuery.isError ? (
          <div className="detail-stack">
            <p className="form-error">{normalizeApiError(timelineQuery.error).message}</p>
            <button
              type="button"
              className="btn btn-secondary"
              onClick={() => void timelineQuery.refetch()}
            >
              {t('common.actions.retry', 'Retry')}
            </button>
          </div>
        ) : null}

        {timelineQuery.isSuccess && timelineQuery.data.entries.length === 0 ? (
          <p>
            {t(
              'patients.timeline.empty',
              'No appointment or prescription history found for this patient.'
            )}
          </p>
        ) : null}

        {timelineQuery.isSuccess && timelineQuery.data.entries.length > 0 ? (
          <div className="timeline-list">
            {timelineQuery.data.entries.map((entry) => (
              <article
                key={`${entry.appointmentId}-${entry.prescriptionId ?? 'none'}`}
                className="timeline-card"
              >
                <div className="timeline-card-header">
                  <strong>{entry.appointmentCode}</strong>
                  <span>{formatDateTime(entry.appointmentTime)}</span>
                </div>

                <dl className="timeline-grid">
                  <div>
                    <dt>{t('patients.timeline.doctor', 'Doctor')}</dt>
                    <dd>{entry.doctorName}</dd>
                  </div>
                  <div>
                    <dt>{t('patients.timeline.appointmentStatus', 'Appointment status')}</dt>
                    <dd>
                      <span className={timelineStatusClass(entry.appointmentStatus)}>
                        {t(
                          `status.${(entry.appointmentStatus ?? '').toLowerCase()}`,
                          entry.appointmentStatus ?? '-'
                        )}
                      </span>
                    </dd>
                  </div>
                  <div>
                    <dt>{t('patients.timeline.prescription', 'Prescription')}</dt>
                    <dd>{entry.prescriptionCode ?? '-'}</dd>
                  </div>
                  <div>
                    <dt>{t('patients.timeline.prescriptionStatus', 'Prescription status')}</dt>
                    <dd>
                      {entry.prescriptionStatus ? (
                        <span className={timelineStatusClass(entry.prescriptionStatus)}>
                          {t(
                            `status.${entry.prescriptionStatus.toLowerCase()}`,
                            entry.prescriptionStatus
                          )}
                        </span>
                      ) : (
                        '-'
                      )}
                    </dd>
                  </div>
                  <div className="field-span-2">
                    <dt>{t('patients.timeline.diagnosis', 'Diagnosis')}</dt>
                    <dd>{entry.diagnosis ?? '-'}</dd>
                  </div>
                  <div className="field-span-2">
                    <dt>{t('patients.timeline.advice', 'Advice')}</dt>
                    <dd>{entry.advice ?? '-'}</dd>
                  </div>
                  <div>
                    <dt>{t('patients.timeline.issuedAt', 'Issued at')}</dt>
                    <dd>
                      {entry.prescriptionIssuedAt
                        ? formatDateTime(entry.prescriptionIssuedAt)
                        : '-'}
                    </dd>
                  </div>
                </dl>
              </article>
            ))}
          </div>
        ) : null}
      </Modal>
    </section>
  )
}
