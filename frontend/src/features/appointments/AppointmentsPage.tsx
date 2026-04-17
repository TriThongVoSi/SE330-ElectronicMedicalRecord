import React, { useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { zodResolver } from '@hookform/resolvers/zod'
import { useForm, useWatch } from 'react-hook-form'
import { z } from 'zod'
import { Modal } from '../../components/Modal'
import { PageHeader } from '../../components/PageHeader'
import { PaginatedQueryContent } from '../../components/PaginatedQueryContent'
import { normalizeApiError } from '../../core/api/errors'
import { useI18n } from '../../core/i18n/use-i18n'
import { formatDateTime } from '../../core/utils/format'
import { useNotify } from '../../core/utils/notify'
import { queryKeys } from '../../core/utils/query-keys'
import { listPatientOptions } from '../patients/api'
import { listDoctors } from '../staff/api'
import { createAppointment, listAppointments, updateAppointment } from './api'
import type { Appointment, AppointmentStatus, AppointmentUpsertInput } from './types'

const optionalPriorityScore = z.preprocess(
  (inputValue) =>
    inputValue === '' || inputValue === null || inputValue === undefined ? undefined : inputValue,
  z.coerce.number().int().min(1).max(10).optional()
)

const appointmentSchema = z.object({
  appointmentCode: z.string().trim().min(3, 'Appointment code is required.'),
  appointmentTime: z.string().min(1, 'Appointment time is required.'),
  status: z.enum(['COMING', 'FINISH', 'CANCEL']),
  cancelReason: z.string().optional(),
  doctorId: z.string().trim().min(1, 'Doctor is required.'),
  patientId: z.string().trim().min(1, 'Patient is required.'),
  urgencyLevel: z.coerce.number().int().min(1).max(5),
  prescriptionStatus: z.string().trim().min(1, 'Prescription status is required.'),
  isFollowup: z.boolean(),
  priorityScore: optionalPriorityScore,
})

type AppointmentFormInput = z.input<typeof appointmentSchema>
type AppointmentFormValues = z.output<typeof appointmentSchema>

const emptyFormValues: AppointmentFormInput = {
  appointmentCode: '',
  appointmentTime: '',
  status: 'COMING',
  cancelReason: '',
  doctorId: '',
  patientId: '',
  urgencyLevel: 1,
  prescriptionStatus: 'NONE',
  isFollowup: false,
  priorityScore: undefined,
}

const toLocalInputValue = (isoString: string): string => {
  const date = new Date(isoString)
  if (Number.isNaN(date.getTime())) {
    return ''
  }

  const adjusted = new Date(date.getTime() - date.getTimezoneOffset() * 60000)
  return adjusted.toISOString().slice(0, 16)
}

const toPayload = (values: AppointmentFormValues): AppointmentUpsertInput => ({
  appointmentCode: values.appointmentCode.trim(),
  appointmentTime: new Date(values.appointmentTime).toISOString(),
  status: values.status,
  cancelReason: values.cancelReason || undefined,
  doctorId: values.doctorId,
  patientId: values.patientId,
  urgencyLevel: values.urgencyLevel,
  prescriptionStatus: values.prescriptionStatus,
  isFollowup: values.isFollowup,
  priorityScore: values.priorityScore,
})

const toFormValues = (appointment: Appointment): AppointmentFormInput => ({
  appointmentCode: appointment.appointmentCode,
  appointmentTime: toLocalInputValue(appointment.appointmentTime),
  status: appointment.status,
  cancelReason: appointment.cancelReason ?? '',
  doctorId: appointment.doctorId,
  patientId: appointment.patientId,
  urgencyLevel: appointment.urgencyLevel,
  prescriptionStatus: appointment.prescriptionStatus,
  isFollowup: appointment.isFollowup,
  priorityScore: appointment.priorityScore,
})

export const AppointmentsPage: React.FC = () => {
  const queryClient = useQueryClient()
  const notify = useNotify()
  const { t } = useI18n()

  const [search, setSearch] = useState('')
  const [statusFilter, setStatusFilter] = useState<AppointmentStatus | ''>('')
  const [dateFilter, setDateFilter] = useState('')
  const [page, setPage] = useState(1)
  const [isFormModalOpen, setIsFormModalOpen] = useState(false)
  const [editingAppointment, setEditingAppointment] = useState<Appointment | null>(null)

  const {
    register,
    handleSubmit,
    setValue,
    reset,
    setError,
    formState: { errors },
    control,
  } = useForm<AppointmentFormInput, unknown, AppointmentFormValues>({
    resolver: zodResolver(appointmentSchema),
    defaultValues: emptyFormValues,
  })

  const selectedStatus = useWatch({
    control,
    name: 'status',
    defaultValue: 'COMING',
  })

  const isFollowup = useWatch({
    control,
    name: 'isFollowup',
    defaultValue: false,
  })

  const patientOptionsQuery = useQuery({
    queryKey: [...queryKeys.patients, 'options'],
    queryFn: listPatientOptions,
  })

  const doctorOptionsQuery = useQuery({
    queryKey: queryKeys.doctors,
    queryFn: listDoctors,
  })

  const referenceDataErrorMessage = patientOptionsQuery.isError
    ? normalizeApiError(patientOptionsQuery.error).message
    : doctorOptionsQuery.isError
      ? normalizeApiError(doctorOptionsQuery.error).message
      : null

  const listQueryKey = useMemo(
    () => [...queryKeys.appointments, page, search, statusFilter, dateFilter],
    [page, search, statusFilter, dateFilter]
  )

  const appointmentsQuery = useQuery({
    queryKey: listQueryKey,
    queryFn: () =>
      listAppointments({
        page,
        size: 10,
        search,
        status: statusFilter || undefined,
        date: dateFilter || undefined,
      }),
  })

  const createMutation = useMutation({
    mutationFn: createAppointment,
    onSuccess: () => {
      notify.success(
        t('appointments.toast.createdTitle', 'Appointment created'),
        t('appointments.toast.createdDescription', 'Booking has been added successfully.')
      )
      setIsFormModalOpen(false)
      reset(emptyFormValues)
      void queryClient.invalidateQueries({ queryKey: queryKeys.appointments })
      void queryClient.invalidateQueries({ queryKey: queryKeys.dashboardSummary })
    },
  })

  const updateMutation = useMutation({
    mutationFn: ({ id, payload }: { id: string; payload: AppointmentUpsertInput }) =>
      updateAppointment(id, payload),
    onSuccess: () => {
      notify.success(
        t('appointments.toast.updatedTitle', 'Appointment updated'),
        t('appointments.toast.updatedDescription', 'Appointment data has been updated.')
      )
      setIsFormModalOpen(false)
      setEditingAppointment(null)
      reset(emptyFormValues)
      void queryClient.invalidateQueries({ queryKey: queryKeys.appointments })
      void queryClient.invalidateQueries({ queryKey: queryKeys.dashboardSummary })
    },
  })

  const openCreateModal = (): void => {
    setEditingAppointment(null)
    reset(emptyFormValues)
    setIsFormModalOpen(true)
  }

  const openEditModal = (appointment: Appointment): void => {
    setEditingAppointment(appointment)
    reset(toFormValues(appointment))
    setIsFormModalOpen(true)
  }

  const submitForm = (values: AppointmentFormValues): void => {
    const payload = toPayload(values)

    const promise = editingAppointment
      ? updateMutation.mutateAsync({ id: editingAppointment.id, payload })
      : createMutation.mutateAsync(payload)

    void promise.catch((error: unknown) => {
      const normalized = normalizeApiError(error)
      if (normalized.fieldErrors) {
        for (const [field, message] of Object.entries(normalized.fieldErrors)) {
          const fieldKey = field as keyof AppointmentFormInput
          setError(fieldKey, { message })
        }
      } else {
        notify.error(
          t('appointments.toast.saveErrorTitle', 'Cannot save appointment'),
          normalized.message
        )
      }
    })
  }

  const isSubmitting = createMutation.isPending || updateMutation.isPending

  return (
    <section className="page-section">
      <PageHeader
        title={t('appointments.title', 'Appointments')}
        subtitle={t(
          'appointments.subtitle',
          'Book, track, and update appointment status for EMR operations.'
        )}
        action={
          <button type="button" className="btn btn-primary" onClick={openCreateModal}>
            {t('appointments.newButton', 'New appointment')}
          </button>
        }
      />

      <section className="card-section">
        <div className="toolbar-grid">
          <label className="field">
            <span>{t('appointments.filters.search', 'Search')}</span>
            <input
              value={search}
              onChange={(event) => {
                setPage(1)
                setSearch(event.target.value)
              }}
              placeholder={t('appointments.filters.searchPlaceholder', 'Code, doctor, patient')}
            />
          </label>

          <label className="field">
            <span>{t('appointments.filters.status', 'Status')}</span>
            <select
              value={statusFilter}
              onChange={(event) => {
                setPage(1)
                const selectedFilterStatus = event.target.value as AppointmentStatus | ''
                setStatusFilter(selectedFilterStatus)
              }}
            >
              <option value="">{t('appointments.filters.allStatus', 'All status')}</option>
              <option value="COMING">{t('status.coming', 'COMING')}</option>
              <option value="FINISH">{t('status.finish', 'FINISH')}</option>
              <option value="CANCEL">{t('status.cancel', 'CANCEL')}</option>
            </select>
          </label>

          <label className="field">
            <span>{t('appointments.filters.date', 'Date')}</span>
            <input
              type="date"
              value={dateFilter}
              onChange={(event) => {
                setPage(1)
                setDateFilter(event.target.value)
              }}
            />
          </label>
        </div>

        <PaginatedQueryContent
          query={appointmentsQuery}
          emptyTitle={t('appointments.emptyTitle', 'No appointments found')}
          emptyMessage={t('appointments.emptyMessage', 'No booking matches current filters.')}
          getErrorMessage={(error) => normalizeApiError(error).message}
          onRetry={() => void appointmentsQuery.refetch()}
          onPageChange={setPage}
        >
          {(appointments) => (
            <div className="table-wrapper">
              <table>
                <thead>
                  <tr>
                    <th>{t('appointments.table.code', 'Code')}</th>
                    <th>{t('appointments.table.time', 'Time')}</th>
                    <th>{t('appointments.table.patient', 'Patient')}</th>
                    <th>{t('appointments.table.doctor', 'Doctor')}</th>
                    <th>{t('appointments.table.status', 'Status')}</th>
                    <th>{t('appointments.table.prescription', 'Prescription')}</th>
                    <th>{t('appointments.table.actions', 'Actions')}</th>
                  </tr>
                </thead>
                <tbody>
                  {appointments.map((appointment) => (
                    <tr key={appointment.id}>
                      <td>{appointment.appointmentCode}</td>
                      <td>{formatDateTime(appointment.appointmentTime)}</td>
                      <td>{appointment.patientName ?? appointment.patientId}</td>
                      <td>{appointment.doctorName ?? appointment.doctorId}</td>
                      <td>
                        <span className={`status-pill status-${appointment.status.toLowerCase()}`}>
                          {t(`status.${appointment.status.toLowerCase()}`, appointment.status)}
                        </span>
                      </td>
                      <td>
                        {t(
                          `status.${appointment.prescriptionStatus.toLowerCase()}`,
                          appointment.prescriptionStatus
                        )}
                      </td>
                      <td>
                        <button
                          type="button"
                          className="btn btn-secondary"
                          onClick={() => openEditModal(appointment)}
                        >
                          {t('common.actions.edit', 'Edit')}
                        </button>
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
          editingAppointment
            ? t('appointments.modal.updateTitle', 'Update appointment')
            : t('appointments.modal.createTitle', 'Create appointment')
        }
        onClose={() => {
          setIsFormModalOpen(false)
          setEditingAppointment(null)
        }}
      >
        <form className="form-grid" onSubmit={handleSubmit(submitForm)}>
          <label className="field">
            <span>{t('appointments.form.appointmentCode', 'Appointment code')}</span>
            <input {...register('appointmentCode')} />
            {errors.appointmentCode ? <small>{errors.appointmentCode.message}</small> : null}
          </label>

          <label className="field">
            <span>{t('appointments.form.appointmentTime', 'Appointment time')}</span>
            <input type="datetime-local" {...register('appointmentTime')} />
            {errors.appointmentTime ? <small>{errors.appointmentTime.message}</small> : null}
          </label>

          <label className="field">
            <span>{t('appointments.form.patient', 'Patient')}</span>
            <select {...register('patientId')}>
              <option value="">{t('appointments.form.selectPatient', 'Select patient')}</option>
              {patientOptionsQuery.data?.map((patient) => (
                <option key={patient.id} value={patient.id}>
                  {patient.patientCode} - {patient.fullName}
                </option>
              ))}
            </select>
            {errors.patientId ? <small>{errors.patientId.message}</small> : null}
          </label>

          <label className="field">
            <span>{t('appointments.form.doctor', 'Doctor')}</span>
            <select {...register('doctorId')}>
              <option value="">{t('appointments.form.selectDoctor', 'Select doctor')}</option>
              {doctorOptionsQuery.data?.map((doctor) => (
                <option key={doctor.id} value={doctor.id}>
                  {doctor.fullName}
                </option>
              ))}
            </select>
            {errors.doctorId ? <small>{errors.doctorId.message}</small> : null}
          </label>

          {referenceDataErrorMessage ? (
            <p className="form-error field-span-2">
              {t('appointments.form.lookupError', 'Cannot load lookup data:')}{' '}
              {referenceDataErrorMessage}
            </p>
          ) : null}

          <label className="field">
            <span>{t('appointments.form.status', 'Status')}</span>
            <select {...register('status')}>
              <option value="COMING">{t('status.coming', 'COMING')}</option>
              <option value="FINISH">{t('status.finish', 'FINISH')}</option>
              <option value="CANCEL">{t('status.cancel', 'CANCEL')}</option>
            </select>
            {errors.status ? <small>{errors.status.message}</small> : null}
          </label>

          <label className="field">
            <span>{t('appointments.form.prescriptionStatus', 'Prescription status')}</span>
            <select {...register('prescriptionStatus')}>
              <option value="NONE">{t('status.none', 'NONE')}</option>
              <option value="CREATED">{t('status.created', 'CREATED')}</option>
              <option value="PAID">{t('status.paid', 'PAID')}</option>
            </select>
          </label>

          <label className="field">
            <span>{t('appointments.form.urgencyLevel', 'Urgency level (1-5)')}</span>
            <input type="number" min={1} max={5} {...register('urgencyLevel')} />
            {errors.urgencyLevel ? <small>{errors.urgencyLevel.message}</small> : null}
          </label>

          <label className="field">
            <span>{t('appointments.form.priorityScore', 'Priority score (1-10)')}</span>
            <input type="number" min={1} max={10} {...register('priorityScore')} />
            {errors.priorityScore ? <small>{errors.priorityScore.message}</small> : null}
          </label>

          <label className="field checkbox-field">
            <input
              type="checkbox"
              checked={isFollowup}
              onChange={(event) => setValue('isFollowup', event.target.checked)}
            />
            <span>{t('appointments.form.followUp', 'Follow-up appointment')}</span>
          </label>

          {selectedStatus === 'CANCEL' ? (
            <label className="field field-span-2">
              <span>{t('appointments.form.cancelReason', 'Cancel reason')}</span>
              <textarea rows={3} {...register('cancelReason')} />
            </label>
          ) : null}

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
                : t('appointments.form.saveButton', 'Save appointment')}
            </button>
          </div>
        </form>
      </Modal>
    </section>
  )
}
