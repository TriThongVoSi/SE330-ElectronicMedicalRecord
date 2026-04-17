import React, { useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { zodResolver } from '@hookform/resolvers/zod'
import { useFieldArray, useForm } from 'react-hook-form'
import { z } from 'zod'
import { Modal } from '../../components/Modal'
import { PageHeader } from '../../components/PageHeader'
import { PaginatedQueryContent } from '../../components/PaginatedQueryContent'
import { normalizeApiError } from '../../core/api/errors'
import { useI18n } from '../../core/i18n/use-i18n'
import { formatDateTime } from '../../core/utils/format'
import { useNotify } from '../../core/utils/notify'
import { queryKeys } from '../../core/utils/query-keys'
import { listAppointmentOptions } from '../appointments/api'
import { listDrugOptions } from '../drugs/api'
import { listPatientOptions } from '../patients/api'
import { listDoctors } from '../staff/api'
import { createPrescription, listPrescriptions, updatePrescription } from './api'
import type { Prescription, PrescriptionUpsertInput } from './types'

const prescriptionItemSchema = z.object({
  drugId: z.string().trim().min(1, 'Drug is required.'),
  quantity: z.coerce.number().int().min(1, 'Quantity must be at least 1.'),
  instructions: z.string().trim().min(3, 'Instructions are required.'),
})

const prescriptionSchema = z.object({
  prescriptionCode: z.string().trim().min(3, 'Prescription code is required.'),
  patientId: z.string().trim().min(1, 'Patient is required.'),
  doctorId: z.string().trim().min(1, 'Doctor is required.'),
  appointmentId: z.string().trim().min(1, 'Appointment is required.'),
  status: z.string().trim().min(1, 'Status is required.'),
  diagnosis: z.string().optional(),
  advice: z.string().optional(),
  items: z.array(prescriptionItemSchema).min(1, 'At least one item is required.'),
})

type PrescriptionFormInput = z.input<typeof prescriptionSchema>
type PrescriptionFormValues = z.output<typeof prescriptionSchema>

const emptyItem: PrescriptionFormInput['items'][number] = {
  drugId: '',
  quantity: 1,
  instructions: '',
}

const emptyFormValues: PrescriptionFormInput = {
  prescriptionCode: '',
  patientId: '',
  doctorId: '',
  appointmentId: '',
  status: 'CREATED',
  diagnosis: '',
  advice: '',
  items: [emptyItem],
}

const toPayload = (values: PrescriptionFormValues): PrescriptionUpsertInput => ({
  prescriptionCode: values.prescriptionCode.trim(),
  patientId: values.patientId,
  doctorId: values.doctorId,
  appointmentId: values.appointmentId,
  status: values.status.trim(),
  diagnosis: values.diagnosis || undefined,
  advice: values.advice || undefined,
  items: values.items.map((item) => ({
    drugId: item.drugId,
    quantity: item.quantity,
    instructions: item.instructions.trim(),
  })),
})

const toFormValues = (prescription: Prescription): PrescriptionFormInput => ({
  prescriptionCode: prescription.prescriptionCode,
  patientId: prescription.patientId,
  doctorId: prescription.doctorId,
  appointmentId: prescription.appointmentId,
  status: prescription.status,
  diagnosis: prescription.diagnosis ?? '',
  advice: prescription.advice ?? '',
  items: prescription.items.map((item) => ({
    drugId: item.drugId,
    quantity: item.quantity,
    instructions: item.instructions,
  })),
})

export const PrescriptionsPage: React.FC = () => {
  const queryClient = useQueryClient()
  const notify = useNotify()
  const { t } = useI18n()

  const [search, setSearch] = useState('')
  const [statusFilter, setStatusFilter] = useState('')
  const [page, setPage] = useState(1)
  const [isFormOpen, setIsFormOpen] = useState(false)
  const [editingRecord, setEditingRecord] = useState<Prescription | null>(null)
  const [viewingRecord, setViewingRecord] = useState<Prescription | null>(null)

  const {
    register,
    control,
    handleSubmit,
    reset,
    setError,
    formState: { errors },
  } = useForm<PrescriptionFormInput, unknown, PrescriptionFormValues>({
    resolver: zodResolver(prescriptionSchema),
    defaultValues: emptyFormValues,
  })

  const { fields, append, remove } = useFieldArray({
    control,
    name: 'items',
  })

  const queryKey = useMemo(
    () => [...queryKeys.prescriptions, page, search, statusFilter],
    [page, search, statusFilter]
  )

  const listQuery = useQuery({
    queryKey,
    queryFn: () =>
      listPrescriptions({
        page,
        size: 10,
        search,
        status: statusFilter || undefined,
      }),
  })

  const patientOptionsQuery = useQuery({
    queryKey: [...queryKeys.patients, 'options'],
    queryFn: listPatientOptions,
  })

  const doctorOptionsQuery = useQuery({
    queryKey: queryKeys.doctors,
    queryFn: listDoctors,
  })

  const appointmentOptionsQuery = useQuery({
    queryKey: [...queryKeys.appointments, 'options'],
    queryFn: listAppointmentOptions,
  })

  const drugOptionsQuery = useQuery({
    queryKey: [...queryKeys.drugs, 'options'],
    queryFn: listDrugOptions,
  })

  const referenceDataErrorMessage = patientOptionsQuery.isError
    ? normalizeApiError(patientOptionsQuery.error).message
    : doctorOptionsQuery.isError
      ? normalizeApiError(doctorOptionsQuery.error).message
      : appointmentOptionsQuery.isError
        ? normalizeApiError(appointmentOptionsQuery.error).message
        : drugOptionsQuery.isError
          ? normalizeApiError(drugOptionsQuery.error).message
          : null

  const createMutation = useMutation({
    mutationFn: createPrescription,
    onSuccess: () => {
      notify.success(
        t('prescriptions.toast.createdTitle', 'Prescription created'),
        t('prescriptions.toast.createdDescription', 'Prescription has been issued successfully.')
      )
      setIsFormOpen(false)
      reset(emptyFormValues)
      void queryClient.invalidateQueries({ queryKey: queryKeys.prescriptions })
      void queryClient.invalidateQueries({ queryKey: queryKeys.dashboardSummary })
    },
  })

  const updateMutation = useMutation({
    mutationFn: ({ id, payload }: { id: string; payload: PrescriptionUpsertInput }) =>
      updatePrescription(id, payload),
    onSuccess: () => {
      notify.success(
        t('prescriptions.toast.updatedTitle', 'Prescription updated'),
        t('prescriptions.toast.updatedDescription', 'Prescription has been updated successfully.')
      )
      setIsFormOpen(false)
      setEditingRecord(null)
      reset(emptyFormValues)
      void queryClient.invalidateQueries({ queryKey: queryKeys.prescriptions })
    },
  })

  const openCreate = (): void => {
    setEditingRecord(null)
    reset(emptyFormValues)
    setIsFormOpen(true)
  }

  const openEdit = (record: Prescription): void => {
    setEditingRecord(record)
    reset(toFormValues(record))
    setIsFormOpen(true)
  }

  const submitForm = (values: PrescriptionFormValues): void => {
    const payload = toPayload(values)

    const promise = editingRecord
      ? updateMutation.mutateAsync({ id: editingRecord.id, payload })
      : createMutation.mutateAsync(payload)

    void promise.catch((error: unknown) => {
      const normalized = normalizeApiError(error)
      if (normalized.fieldErrors) {
        for (const [field, message] of Object.entries(normalized.fieldErrors)) {
          const fieldKey = field as keyof PrescriptionFormInput
          setError(fieldKey, { message })
        }
      } else {
        notify.error(
          t('prescriptions.toast.saveErrorTitle', 'Cannot save prescription'),
          normalized.message
        )
      }
    })
  }

  const isSubmitting = createMutation.isPending || updateMutation.isPending

  return (
    <section className="page-section">
      <PageHeader
        title={t('prescriptions.title', 'Prescriptions')}
        subtitle={t(
          'prescriptions.subtitle',
          'Issue prescriptions and manage medicine instructions.'
        )}
        action={
          <button type="button" className="btn btn-primary" onClick={openCreate}>
            {t('prescriptions.newButton', 'New prescription')}
          </button>
        }
      />

      <section className="card-section">
        <div className="toolbar-grid">
          <label className="field">
            <span>{t('prescriptions.filters.search', 'Search')}</span>
            <input
              value={search}
              onChange={(event) => {
                setPage(1)
                setSearch(event.target.value)
              }}
              placeholder={t('prescriptions.filters.searchPlaceholder', 'Code, patient, doctor')}
            />
          </label>
          <label className="field">
            <span>{t('prescriptions.filters.status', 'Status')}</span>
            <select
              value={statusFilter}
              onChange={(event) => {
                setPage(1)
                setStatusFilter(event.target.value)
              }}
            >
              <option value="">{t('common.filters.all', 'All')}</option>
              <option value="CREATED">{t('status.created', 'CREATED')}</option>
              <option value="ISSUED">{t('status.issued', 'ISSUED')}</option>
              <option value="CANCEL">{t('status.cancel', 'CANCEL')}</option>
            </select>
          </label>
        </div>

        <PaginatedQueryContent
          query={listQuery}
          emptyTitle={t('prescriptions.emptyTitle', 'No prescriptions')}
          emptyMessage={t(
            'prescriptions.emptyMessage',
            'Issued prescriptions will appear in this list.'
          )}
          getErrorMessage={(error) => normalizeApiError(error).message}
          onRetry={() => void listQuery.refetch()}
          onPageChange={setPage}
        >
          {(prescriptions) => (
            <div className="table-wrapper">
              <table>
                <thead>
                  <tr>
                    <th>{t('prescriptions.table.code', 'Code')}</th>
                    <th>{t('prescriptions.table.patient', 'Patient')}</th>
                    <th>{t('prescriptions.table.doctor', 'Doctor')}</th>
                    <th>{t('prescriptions.table.status', 'Status')}</th>
                    <th>{t('prescriptions.table.createdAt', 'Created at')}</th>
                    <th>{t('prescriptions.table.actions', 'Actions')}</th>
                  </tr>
                </thead>
                <tbody>
                  {prescriptions.map((prescription) => (
                    <tr key={prescription.id}>
                      <td>{prescription.prescriptionCode}</td>
                      <td>{prescription.patientName ?? prescription.patientId}</td>
                      <td>{prescription.doctorName ?? prescription.doctorId}</td>
                      <td>
                        <span className={`status-pill status-${prescription.status.toLowerCase()}`}>
                          {t(`status.${prescription.status.toLowerCase()}`, prescription.status)}
                        </span>
                      </td>
                      <td>{formatDateTime(prescription.createdAt)}</td>
                      <td>
                        <div className="table-actions">
                          <button
                            className="btn btn-secondary"
                            type="button"
                            onClick={() => setViewingRecord(prescription)}
                          >
                            {t('common.actions.view', 'View')}
                          </button>
                          <button
                            className="btn btn-secondary"
                            type="button"
                            onClick={() => openEdit(prescription)}
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
        open={isFormOpen}
        title={
          editingRecord
            ? t('prescriptions.modal.updateTitle', 'Update prescription')
            : t('prescriptions.modal.createTitle', 'Create prescription')
        }
        onClose={() => {
          setIsFormOpen(false)
          setEditingRecord(null)
        }}
      >
        <form className="form-grid" onSubmit={handleSubmit(submitForm)}>
          <label className="field">
            <span>{t('prescriptions.form.prescriptionCode', 'Prescription code')}</span>
            <input {...register('prescriptionCode')} />
            {errors.prescriptionCode ? <small>{errors.prescriptionCode.message}</small> : null}
          </label>

          <label className="field">
            <span>{t('prescriptions.form.status', 'Status')}</span>
            <select {...register('status')}>
              <option value="CREATED">{t('status.created', 'CREATED')}</option>
              <option value="ISSUED">{t('status.issued', 'ISSUED')}</option>
              <option value="CANCEL">{t('status.cancel', 'CANCEL')}</option>
            </select>
            {errors.status ? <small>{errors.status.message}</small> : null}
          </label>

          <label className="field">
            <span>{t('prescriptions.form.patient', 'Patient')}</span>
            <select {...register('patientId')}>
              <option value="">{t('prescriptions.form.selectPatient', 'Select patient')}</option>
              {patientOptionsQuery.data?.map((patient) => (
                <option key={patient.id} value={patient.id}>
                  {patient.patientCode} - {patient.fullName}
                </option>
              ))}
            </select>
            {errors.patientId ? <small>{errors.patientId.message}</small> : null}
          </label>

          <label className="field">
            <span>{t('prescriptions.form.doctor', 'Doctor')}</span>
            <select {...register('doctorId')}>
              <option value="">{t('prescriptions.form.selectDoctor', 'Select doctor')}</option>
              {doctorOptionsQuery.data?.map((doctor) => (
                <option key={doctor.id} value={doctor.id}>
                  {doctor.fullName}
                </option>
              ))}
            </select>
            {errors.doctorId ? <small>{errors.doctorId.message}</small> : null}
          </label>

          <label className="field field-span-2">
            <span>{t('prescriptions.form.appointment', 'Appointment')}</span>
            <select {...register('appointmentId')}>
              <option value="">
                {t('prescriptions.form.selectAppointment', 'Select appointment')}
              </option>
              {appointmentOptionsQuery.data?.map((appointment) => (
                <option key={appointment.id} value={appointment.id}>
                  {appointment.appointmentCode} - {appointment.patientName ?? appointment.patientId}
                </option>
              ))}
            </select>
            {errors.appointmentId ? <small>{errors.appointmentId.message}</small> : null}
          </label>

          {referenceDataErrorMessage ? (
            <p className="form-error field-span-2">
              {t('prescriptions.form.lookupError', 'Cannot load lookup data:')}{' '}
              {referenceDataErrorMessage}
            </p>
          ) : null}

          <label className="field field-span-2">
            <span>{t('prescriptions.form.diagnosis', 'Diagnosis')}</span>
            <textarea rows={2} {...register('diagnosis')} />
          </label>

          <label className="field field-span-2">
            <span>{t('prescriptions.form.advice', 'Advice')}</span>
            <textarea rows={2} {...register('advice')} />
          </label>

          <div className="items-panel field-span-2">
            <div className="items-header">
              <h3>{t('prescriptions.form.itemsTitle', 'Prescription items')}</h3>
              <button
                type="button"
                className="btn btn-secondary"
                onClick={() => append({ ...emptyItem })}
              >
                {t('common.actions.addItem', 'Add item')}
              </button>
            </div>

            {fields.map((field, index) => (
              <div key={field.id} className="item-row">
                <label className="field">
                  <span>{t('prescriptions.form.drug', 'Drug')}</span>
                  <select {...register(`items.${index}.drugId`)}>
                    <option value="">{t('prescriptions.form.selectDrug', 'Select drug')}</option>
                    {drugOptionsQuery.data?.map((drug) => (
                      <option key={drug.id} value={drug.id}>
                        {drug.drugCode} - {drug.drugName}
                      </option>
                    ))}
                  </select>
                  {errors.items?.[index]?.drugId ? (
                    <small>{errors.items[index]?.drugId?.message}</small>
                  ) : null}
                </label>

                <label className="field">
                  <span>{t('prescriptions.form.quantity', 'Quantity')}</span>
                  <input type="number" min={1} {...register(`items.${index}.quantity`)} />
                  {errors.items?.[index]?.quantity ? (
                    <small>{errors.items[index]?.quantity?.message}</small>
                  ) : null}
                </label>

                <label className="field field-span-2">
                  <span>{t('prescriptions.form.instructions', 'Instructions')}</span>
                  <input {...register(`items.${index}.instructions`)} />
                  {errors.items?.[index]?.instructions ? (
                    <small>{errors.items[index]?.instructions?.message}</small>
                  ) : null}
                </label>

                <button
                  type="button"
                  className="btn btn-ghost"
                  onClick={() => remove(index)}
                  disabled={fields.length === 1}
                >
                  {t('common.actions.remove', 'Remove')}
                </button>
              </div>
            ))}
          </div>

          <div className="form-actions field-span-2">
            <button
              type="button"
              className="btn btn-secondary"
              onClick={() => setIsFormOpen(false)}
            >
              {t('common.actions.cancel', 'Cancel')}
            </button>
            <button type="submit" className="btn btn-primary" disabled={isSubmitting}>
              {isSubmitting
                ? t('common.actions.saving', 'Saving...')
                : t('prescriptions.form.saveButton', 'Save prescription')}
            </button>
          </div>
        </form>
      </Modal>

      <Modal
        open={Boolean(viewingRecord)}
        title={t('prescriptions.detail.title', 'Prescription detail')}
        onClose={() => setViewingRecord(null)}
      >
        {viewingRecord ? (
          <div className="detail-stack">
            <p>
              <strong>{t('prescriptions.detail.code', 'Code:')}</strong>{' '}
              {viewingRecord.prescriptionCode}
            </p>
            <p>
              <strong>{t('prescriptions.detail.patient', 'Patient:')}</strong>{' '}
              {viewingRecord.patientName ?? viewingRecord.patientId}
            </p>
            <p>
              <strong>{t('prescriptions.detail.doctor', 'Doctor:')}</strong>{' '}
              {viewingRecord.doctorName ?? viewingRecord.doctorId}
            </p>
            <p>
              <strong>{t('prescriptions.detail.status', 'Status:')}</strong>{' '}
              {t(`status.${viewingRecord.status.toLowerCase()}`, viewingRecord.status)}
            </p>

            <h4>{t('prescriptions.detail.itemsTitle', 'Items')}</h4>
            <div className="table-wrapper">
              <table>
                <thead>
                  <tr>
                    <th>{t('prescriptions.detail.drug', 'Drug')}</th>
                    <th>{t('prescriptions.detail.quantity', 'Quantity')}</th>
                    <th>{t('prescriptions.detail.instructions', 'Instructions')}</th>
                  </tr>
                </thead>
                <tbody>
                  {viewingRecord.items.map((item, index) => (
                    <tr key={`${item.drugId}-${index}`}>
                      <td>{item.drugName ?? item.drugId}</td>
                      <td>{item.quantity}</td>
                      <td>{item.instructions}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        ) : null}
      </Modal>
    </section>
  )
}
