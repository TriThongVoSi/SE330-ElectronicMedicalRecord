import React, { useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient, type UseQueryResult } from '@tanstack/react-query'
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
import {
  cancelPatientAppointment,
  createPatientAppointment,
  getPatientAppointment,
  listAvailableSlots,
  listBookableDoctors,
  listPatientAppointments,
  reschedulePatientAppointment,
} from './api'
import type {
  PatientPortalAppointment,
  PatientPortalAppointmentStatus,
  PatientPortalNormalizedStatus,
  PatientPortalSlot,
} from './types'

type AppointmentTab = 'UPCOMING' | 'PAST' | 'CANCELLED'

const bookingSchema = z.object({
  doctorId: z.string().trim().min(1, 'Doctor is required.'),
  date: z.string().trim().min(1, 'Date is required.'),
  appointmentTime: z.string().trim().min(1, 'Please choose an available slot.'),
  note: z.string().trim().optional(),
})

const rescheduleSchema = z.object({
  doctorId: z.string().trim().min(1, 'Doctor is required.'),
  date: z.string().trim().min(1, 'Date is required.'),
  appointmentTime: z.string().trim().min(1, 'Please choose an available slot.'),
})

const cancelSchema = z.object({
  cancelReason: z.string().trim().min(3, 'Cancel reason is required.'),
})

type BookingFormInput = z.input<typeof bookingSchema>
type BookingFormValues = z.output<typeof bookingSchema>
type RescheduleFormInput = z.input<typeof rescheduleSchema>
type RescheduleFormValues = z.output<typeof rescheduleSchema>
type CancelFormValues = z.output<typeof cancelSchema>

const emptyBookingValues: BookingFormInput = {
  doctorId: '',
  date: '',
  appointmentTime: '',
  note: '',
}

const emptyRescheduleValues: RescheduleFormInput = {
  doctorId: '',
  date: '',
  appointmentTime: '',
}

const tabToStatusFilter: Record<AppointmentTab, PatientPortalAppointmentStatus> = {
  UPCOMING: 'COMING',
  PAST: 'FINISH',
  CANCELLED: 'CANCEL',
}

const statusClassByNormalized: Record<PatientPortalNormalizedStatus, string> = {
  PENDING_CONFIRMATION: 'status-pill status-pending-confirmation',
  CONFIRMED: 'status-pill status-confirmed',
  CANCELLED: 'status-pill status-cancelled',
}

const normalizeStatus = (appointment: PatientPortalAppointment): PatientPortalNormalizedStatus => {
  if (appointment.normalizedStatus === 'CANCELLED') {
    return 'CANCELLED'
  }
  if (appointment.normalizedStatus === 'CONFIRMED') {
    return 'CONFIRMED'
  }

  const raw = appointment.status.toUpperCase()
  if (raw === 'CANCEL') {
    return 'CANCELLED'
  }
  if (raw === 'FINISH') {
    return 'CONFIRMED'
  }

  return 'PENDING_CONFIRMATION'
}

const toDateInputValue = (isoString: string): string => {
  const date = new Date(isoString)
  if (Number.isNaN(date.getTime())) {
    return ''
  }
  const adjusted = new Date(date.getTime() - date.getTimezoneOffset() * 60000)
  return adjusted.toISOString().slice(0, 10)
}

const SlotSelector: React.FC<{
  slotsQuery: UseQueryResult<PatientPortalSlot[], unknown>
  selectedSlotTime: string
  onSelect: (slotTime: string) => void
  emptyLabel: string
  loadingLabel: string
}> = ({ slotsQuery, selectedSlotTime, onSelect, emptyLabel, loadingLabel }) => {
  if (slotsQuery.isLoading) {
    return <p>{loadingLabel}</p>
  }

  if (slotsQuery.isError) {
    return <p className="form-error">{normalizeApiError(slotsQuery.error).message}</p>
  }

  if (!slotsQuery.isSuccess || slotsQuery.data.length === 0) {
    return <p>{emptyLabel}</p>
  }

  return (
    <div className="slot-grid">
      {slotsQuery.data.map((slot) => {
        const disabled = slot.booked
        const active = selectedSlotTime === slot.slotTime

        return (
          <button
            key={slot.slotId}
            type="button"
            className={`slot-button ${active ? 'slot-button-active' : ''}`}
            disabled={disabled}
            onClick={() => onSelect(slot.slotTime)}
          >
            {formatDateTime(slot.slotTime)}
          </button>
        )
      })}
    </div>
  )
}

export const PatientAppointmentsPage: React.FC = () => {
  const { t } = useI18n()
  const notify = useNotify()
  const queryClient = useQueryClient()

  const [tab, setTab] = useState<AppointmentTab>('UPCOMING')
  const [page, setPage] = useState(1)
  const [isBookModalOpen, setIsBookModalOpen] = useState(false)
  const [rescheduleTarget, setRescheduleTarget] = useState<PatientPortalAppointment | null>(null)
  const [cancelTarget, setCancelTarget] = useState<PatientPortalAppointment | null>(null)
  const [detailAppointmentId, setDetailAppointmentId] = useState<string | null>(null)

  const bookingForm = useForm<BookingFormInput, unknown, BookingFormValues>({
    resolver: zodResolver(bookingSchema),
    defaultValues: emptyBookingValues,
  })

  const rescheduleForm = useForm<RescheduleFormInput, unknown, RescheduleFormValues>({
    resolver: zodResolver(rescheduleSchema),
    defaultValues: emptyRescheduleValues,
  })

  const cancelForm = useForm<CancelFormValues>({
    resolver: zodResolver(cancelSchema),
    defaultValues: { cancelReason: '' },
  })

  const bookingDoctorId = useWatch({
    control: bookingForm.control,
    name: 'doctorId',
  })
  const bookingDate = useWatch({
    control: bookingForm.control,
    name: 'date',
  })
  const bookingSelectedSlot = useWatch({
    control: bookingForm.control,
    name: 'appointmentTime',
  })

  const rescheduleDoctorId = useWatch({
    control: rescheduleForm.control,
    name: 'doctorId',
  })
  const rescheduleDate = useWatch({
    control: rescheduleForm.control,
    name: 'date',
  })
  const rescheduleSelectedSlot = useWatch({
    control: rescheduleForm.control,
    name: 'appointmentTime',
  })

  const appointmentsQueryKey = useMemo(
    () => [...queryKeys.patientPortalAppointments, tab, page],
    [page, tab]
  )

  const appointmentsQuery = useQuery({
    queryKey: appointmentsQueryKey,
    queryFn: () =>
      listPatientAppointments({
        page,
        size: 10,
        status: tabToStatusFilter[tab],
      }),
  })

  const doctorsQuery = useQuery({
    queryKey: queryKeys.patientPortalDoctors,
    queryFn: listBookableDoctors,
  })

  const bookingSlotsQuery = useQuery({
    queryKey: [...queryKeys.patientPortalAvailableSlots, 'book', bookingDoctorId, bookingDate],
    queryFn: () => listAvailableSlots(bookingDoctorId, bookingDate, bookingDate),
    enabled: Boolean(isBookModalOpen && bookingDoctorId && bookingDate),
  })

  const rescheduleSlotsQuery = useQuery({
    queryKey: [
      ...queryKeys.patientPortalAvailableSlots,
      'reschedule',
      rescheduleTarget?.id,
      rescheduleDoctorId,
      rescheduleDate,
    ],
    queryFn: () => listAvailableSlots(rescheduleDoctorId, rescheduleDate, rescheduleDate),
    enabled: Boolean(rescheduleTarget && rescheduleDoctorId && rescheduleDate),
  })

  const detailQuery = useQuery({
    queryKey: [...queryKeys.patientPortalAppointments, 'detail', detailAppointmentId],
    queryFn: () => getPatientAppointment(detailAppointmentId!),
    enabled: Boolean(detailAppointmentId),
  })

  const onMutationSuccess = (title: string, description: string): void => {
    notify.success(title, description)
    void queryClient.invalidateQueries({ queryKey: queryKeys.patientPortalAppointments })
    void queryClient.invalidateQueries({ queryKey: queryKeys.patientPortalDashboard })
  }

  const createMutation = useMutation({
    mutationFn: createPatientAppointment,
    onSuccess: () => {
      onMutationSuccess(
        t('patientPortal.appointments.toast.bookedTitle', 'Appointment booked'),
        t('patientPortal.appointments.toast.bookedDescription', 'Your appointment has been booked.')
      )
      setIsBookModalOpen(false)
      bookingForm.reset(emptyBookingValues)
    },
  })

  const rescheduleMutation = useMutation({
    mutationFn: ({ appointmentId, payload }: { appointmentId: string; payload: RescheduleFormValues }) =>
      reschedulePatientAppointment(appointmentId, {
        doctorId: payload.doctorId,
        appointmentTime: payload.appointmentTime,
        serviceIds: [],
      }),
    onSuccess: () => {
      onMutationSuccess(
        t('patientPortal.appointments.toast.rescheduledTitle', 'Appointment rescheduled'),
        t(
          'patientPortal.appointments.toast.rescheduledDescription',
          'Your appointment has been rescheduled.'
        )
      )
      setRescheduleTarget(null)
      rescheduleForm.reset(emptyRescheduleValues)
    },
  })

  const cancelMutation = useMutation({
    mutationFn: ({ appointmentId, reason }: { appointmentId: string; reason: string }) =>
      cancelPatientAppointment(appointmentId, { cancelReason: reason }),
    onSuccess: () => {
      onMutationSuccess(
        t('patientPortal.appointments.toast.cancelledTitle', 'Appointment cancelled'),
        t('patientPortal.appointments.toast.cancelledDescription', 'Your appointment has been cancelled.')
      )
      setCancelTarget(null)
      cancelForm.reset({ cancelReason: '' })
    },
  })

  const openBookModal = (): void => {
    bookingForm.reset(emptyBookingValues)
    setIsBookModalOpen(true)
  }

  const openRescheduleModal = (appointment: PatientPortalAppointment): void => {
    setRescheduleTarget(appointment)
    rescheduleForm.reset({
      doctorId: appointment.doctorId,
      date: toDateInputValue(appointment.appointmentTime),
      appointmentTime: '',
    })
  }

  const canMutateAppointment = (appointment: PatientPortalAppointment): boolean => {
    const normalized = normalizeStatus(appointment)
    return normalized === 'PENDING_CONFIRMATION'
  }

  const submitBookForm = (values: BookingFormValues): void => {
    createMutation.mutate(
      {
        doctorId: values.doctorId,
        appointmentTime: values.appointmentTime,
        serviceIds: [],
        note: values.note || undefined,
      },
      {
        onError: (error) => {
          const normalized = normalizeApiError(error)
          notify.error(
            t('patientPortal.appointments.toast.bookErrorTitle', 'Cannot book appointment'),
            normalized.message
          )
        },
      }
    )
  }

  const submitRescheduleForm = (values: RescheduleFormValues): void => {
    if (!rescheduleTarget) {
      return
    }

    rescheduleMutation.mutate(
      {
        appointmentId: rescheduleTarget.id,
        payload: values,
      },
      {
        onError: (error) => {
          const normalized = normalizeApiError(error)
          notify.error(
            t(
              'patientPortal.appointments.toast.rescheduleErrorTitle',
              'Cannot reschedule appointment'
            ),
            normalized.message
          )
        },
      }
    )
  }

  const submitCancelForm = (values: CancelFormValues): void => {
    if (!cancelTarget) {
      return
    }

    cancelMutation.mutate(
      {
        appointmentId: cancelTarget.id,
        reason: values.cancelReason.trim(),
      },
      {
        onError: (error) => {
          const normalized = normalizeApiError(error)
          notify.error(
            t('patientPortal.appointments.toast.cancelErrorTitle', 'Cannot cancel appointment'),
            normalized.message
          )
        },
      }
    )
  }

  return (
    <section className="page-section">
      <PageHeader
        title={t('patientPortal.appointments.title', 'My Appointments')}
        subtitle={t(
          'patientPortal.appointments.subtitle',
          'Book, reschedule, and cancel your appointments.'
        )}
        action={
          <button type="button" className="btn btn-primary" onClick={openBookModal}>
            {t('patientPortal.appointments.actions.bookNew', 'Book new appointment')}
          </button>
        }
      />

      <section className="card-section">
        <div className="tabs-row" role="tablist" aria-label={t('patientPortal.appointments.tabs.label', 'Appointment tabs')}>
          <button
            type="button"
            className={`tab-button ${tab === 'UPCOMING' ? 'tab-button-active' : ''}`}
            onClick={() => {
              setPage(1)
              setTab('UPCOMING')
            }}
          >
            {t('patientPortal.appointments.tabs.upcoming', 'Upcoming')}
          </button>
          <button
            type="button"
            className={`tab-button ${tab === 'PAST' ? 'tab-button-active' : ''}`}
            onClick={() => {
              setPage(1)
              setTab('PAST')
            }}
          >
            {t('patientPortal.appointments.tabs.past', 'Past')}
          </button>
          <button
            type="button"
            className={`tab-button ${tab === 'CANCELLED' ? 'tab-button-active' : ''}`}
            onClick={() => {
              setPage(1)
              setTab('CANCELLED')
            }}
          >
            {t('patientPortal.appointments.tabs.cancelled', 'Cancelled')}
          </button>
        </div>

        <PaginatedQueryContent
          query={appointmentsQuery}
          emptyTitle={t('patientPortal.appointments.emptyTitle', 'No appointments found')}
          emptyMessage={t(
            'patientPortal.appointments.emptyMessage',
            'Try a different tab or book a new appointment.'
          )}
          getErrorMessage={(error) => normalizeApiError(error).message}
          onRetry={() => void appointmentsQuery.refetch()}
          onPageChange={setPage}
        >
          {(appointments) => (
            <div className="table-wrapper">
              <table>
                <thead>
                  <tr>
                    <th>{t('patientPortal.appointments.table.code', 'Code')}</th>
                    <th>{t('patientPortal.appointments.table.time', 'Time')}</th>
                    <th>{t('patientPortal.appointments.table.doctor', 'Doctor')}</th>
                    <th>{t('patientPortal.appointments.table.status', 'Status')}</th>
                    <th>{t('patientPortal.appointments.table.cancelReason', 'Cancel reason')}</th>
                    <th>{t('patientPortal.appointments.table.actions', 'Actions')}</th>
                  </tr>
                </thead>
                <tbody>
                  {appointments.map((appointment) => {
                    const normalized = normalizeStatus(appointment)
                    const canMutate = canMutateAppointment(appointment)

                    return (
                      <tr key={appointment.id}>
                        <td>{appointment.appointmentCode}</td>
                        <td>{formatDateTime(appointment.appointmentTime)}</td>
                        <td>{appointment.doctorName ?? appointment.doctorId}</td>
                        <td>
                          <span className={statusClassByNormalized[normalized]}>
                            {t(`patientPortal.status.${normalized.toLowerCase()}`, normalized)}
                          </span>
                        </td>
                        <td>{appointment.cancelReason ?? '-'}</td>
                        <td>
                          <div className="table-actions">
                            <button
                              type="button"
                              className="btn btn-secondary"
                              onClick={() => setDetailAppointmentId(appointment.id)}
                            >
                              {t('common.actions.view', 'View')}
                            </button>
                            <button
                              type="button"
                              className="btn btn-secondary"
                              disabled={!canMutate}
                              onClick={() => openRescheduleModal(appointment)}
                            >
                              {t('patientPortal.appointments.actions.reschedule', 'Reschedule')}
                            </button>
                            <button
                              type="button"
                              className="btn btn-secondary"
                              disabled={!canMutate}
                              onClick={() => setCancelTarget(appointment)}
                            >
                              {t('patientPortal.appointments.actions.cancel', 'Cancel')}
                            </button>
                          </div>
                        </td>
                      </tr>
                    )
                  })}
                </tbody>
              </table>
            </div>
          )}
        </PaginatedQueryContent>
      </section>

      <Modal
        open={isBookModalOpen}
        title={t('patientPortal.appointments.bookModal.title', 'Book appointment')}
        onClose={() => setIsBookModalOpen(false)}
      >
        <form className="form-grid" onSubmit={bookingForm.handleSubmit(submitBookForm)}>
          <label className="field">
            <span>{t('patientPortal.appointments.form.doctor', 'Doctor')}</span>
            <select {...bookingForm.register('doctorId')}>
              <option value="">{t('patientPortal.appointments.form.selectDoctor', 'Select doctor')}</option>
              {doctorsQuery.data?.map((doctor) => (
                <option key={doctor.id} value={doctor.id}>
                  {doctor.fullName}
                </option>
              ))}
            </select>
            {bookingForm.formState.errors.doctorId ? (
              <small>{bookingForm.formState.errors.doctorId.message}</small>
            ) : null}
          </label>

          <label className="field">
            <span>{t('patientPortal.appointments.form.date', 'Date')}</span>
            <input
              type="date"
              {...bookingForm.register('date')}
              onChange={(event) => {
                bookingForm.setValue('date', event.target.value)
                bookingForm.setValue('appointmentTime', '')
              }}
            />
            {bookingForm.formState.errors.date ? <small>{bookingForm.formState.errors.date.message}</small> : null}
          </label>

          <div className="field field-span-2">
            <span>{t('patientPortal.appointments.form.slots', 'Available slots')}</span>
            <SlotSelector
              slotsQuery={bookingSlotsQuery}
              selectedSlotTime={bookingSelectedSlot ?? ''}
              onSelect={(slotTime) => bookingForm.setValue('appointmentTime', slotTime)}
              loadingLabel={t('patientPortal.appointments.form.loadingSlots', 'Loading slots...')}
              emptyLabel={t('patientPortal.appointments.form.noSlots', 'No available slots for selected date.')}
            />
            {bookingForm.formState.errors.appointmentTime ? (
              <small>{bookingForm.formState.errors.appointmentTime.message}</small>
            ) : null}
          </div>

          <label className="field field-span-2">
            <span>{t('patientPortal.appointments.form.note', 'Reason / note')}</span>
            <textarea rows={3} {...bookingForm.register('note')} />
          </label>

          <div className="form-actions field-span-2">
            <button
              type="button"
              className="btn btn-secondary"
              onClick={() => setIsBookModalOpen(false)}
            >
              {t('common.actions.cancel', 'Cancel')}
            </button>
            <button type="submit" className="btn btn-primary" disabled={createMutation.isPending}>
              {createMutation.isPending
                ? t('common.actions.saving', 'Saving...')
                : t('patientPortal.appointments.bookModal.submit', 'Confirm booking')}
            </button>
          </div>
        </form>
      </Modal>

      <Modal
        open={Boolean(rescheduleTarget)}
        title={t('patientPortal.appointments.rescheduleModal.title', 'Reschedule appointment')}
        onClose={() => setRescheduleTarget(null)}
      >
        <form className="form-grid" onSubmit={rescheduleForm.handleSubmit(submitRescheduleForm)}>
          <label className="field">
            <span>{t('patientPortal.appointments.form.doctor', 'Doctor')}</span>
            <select
              {...rescheduleForm.register('doctorId')}
              onChange={(event) => {
                rescheduleForm.setValue('doctorId', event.target.value)
                rescheduleForm.setValue('appointmentTime', '')
              }}
            >
              <option value="">{t('patientPortal.appointments.form.selectDoctor', 'Select doctor')}</option>
              {doctorsQuery.data?.map((doctor) => (
                <option key={doctor.id} value={doctor.id}>
                  {doctor.fullName}
                </option>
              ))}
            </select>
            {rescheduleForm.formState.errors.doctorId ? (
              <small>{rescheduleForm.formState.errors.doctorId.message}</small>
            ) : null}
          </label>

          <label className="field">
            <span>{t('patientPortal.appointments.form.date', 'Date')}</span>
            <input
              type="date"
              {...rescheduleForm.register('date')}
              onChange={(event) => {
                rescheduleForm.setValue('date', event.target.value)
                rescheduleForm.setValue('appointmentTime', '')
              }}
            />
            {rescheduleForm.formState.errors.date ? (
              <small>{rescheduleForm.formState.errors.date.message}</small>
            ) : null}
          </label>

          <div className="field field-span-2">
            <span>{t('patientPortal.appointments.form.slots', 'Available slots')}</span>
            <SlotSelector
              slotsQuery={rescheduleSlotsQuery}
              selectedSlotTime={rescheduleSelectedSlot ?? ''}
              onSelect={(slotTime) => rescheduleForm.setValue('appointmentTime', slotTime)}
              loadingLabel={t('patientPortal.appointments.form.loadingSlots', 'Loading slots...')}
              emptyLabel={t('patientPortal.appointments.form.noSlots', 'No available slots for selected date.')}
            />
            {rescheduleForm.formState.errors.appointmentTime ? (
              <small>{rescheduleForm.formState.errors.appointmentTime.message}</small>
            ) : null}
          </div>

          <div className="form-actions field-span-2">
            <button
              type="button"
              className="btn btn-secondary"
              onClick={() => setRescheduleTarget(null)}
            >
              {t('common.actions.cancel', 'Cancel')}
            </button>
            <button type="submit" className="btn btn-primary" disabled={rescheduleMutation.isPending}>
              {rescheduleMutation.isPending
                ? t('common.actions.saving', 'Saving...')
                : t('patientPortal.appointments.rescheduleModal.submit', 'Confirm reschedule')}
            </button>
          </div>
        </form>
      </Modal>

      <Modal
        open={Boolean(cancelTarget)}
        title={t('patientPortal.appointments.cancelModal.title', 'Cancel appointment')}
        onClose={() => setCancelTarget(null)}
      >
        <form className="form-grid" onSubmit={cancelForm.handleSubmit(submitCancelForm)}>
          <label className="field field-span-2">
            <span>{t('patientPortal.appointments.cancelModal.reason', 'Cancellation reason')}</span>
            <textarea rows={3} {...cancelForm.register('cancelReason')} />
            {cancelForm.formState.errors.cancelReason ? (
              <small>{cancelForm.formState.errors.cancelReason.message}</small>
            ) : null}
          </label>

          <div className="form-actions field-span-2">
            <button type="button" className="btn btn-secondary" onClick={() => setCancelTarget(null)}>
              {t('common.actions.cancel', 'Cancel')}
            </button>
            <button type="submit" className="btn btn-primary" disabled={cancelMutation.isPending}>
              {cancelMutation.isPending
                ? t('common.actions.saving', 'Saving...')
                : t('patientPortal.appointments.cancelModal.submit', 'Confirm cancellation')}
            </button>
          </div>
        </form>
      </Modal>

      <Modal
        open={Boolean(detailAppointmentId)}
        title={t('patientPortal.appointments.detailModal.title', 'Appointment details')}
        onClose={() => setDetailAppointmentId(null)}
      >
        {detailQuery.isLoading ? (
          <p>{t('patientPortal.appointments.detailModal.loading', 'Loading appointment details...')}</p>
        ) : null}

        {detailQuery.isError ? (
          <p className="form-error">{normalizeApiError(detailQuery.error).message}</p>
        ) : null}

        {detailQuery.isSuccess ? (
          <dl className="detail-grid">
            <div>
              <dt>{t('patientPortal.appointments.table.code', 'Code')}</dt>
              <dd>{detailQuery.data.appointmentCode}</dd>
            </div>
            <div>
              <dt>{t('patientPortal.appointments.table.time', 'Time')}</dt>
              <dd>{formatDateTime(detailQuery.data.appointmentTime)}</dd>
            </div>
            <div>
              <dt>{t('patientPortal.appointments.table.doctor', 'Doctor')}</dt>
              <dd>{detailQuery.data.doctorName ?? detailQuery.data.doctorId}</dd>
            </div>
            <div>
              <dt>{t('patientPortal.appointments.table.status', 'Status')}</dt>
              <dd>
                <span className={statusClassByNormalized[normalizeStatus(detailQuery.data)]}>
                  {t(
                    `patientPortal.status.${normalizeStatus(detailQuery.data).toLowerCase()}`,
                    normalizeStatus(detailQuery.data)
                  )}
                </span>
              </dd>
            </div>
            <div className="field-span-2">
              <dt>{t('patientPortal.appointments.table.cancelReason', 'Cancel reason')}</dt>
              <dd>{detailQuery.data.cancelReason ?? '-'}</dd>
            </div>
          </dl>
        ) : null}
      </Modal>
    </section>
  )
}
