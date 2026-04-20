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
import { useNotify } from '../../core/utils/notify'
import { queryKeys } from '../../core/utils/query-keys'
import { createService, listServices, updateService } from './api'
import type { Service, ServiceUpsertInput } from './types'

const serviceSchema = z.object({
  serviceCode: z.string().trim().min(2, 'Service code is required.'),
  serviceName: z.string().trim().min(2, 'Service name is required.'),
  serviceType: z.string().trim().min(1, 'Service type is required.'),
  isActive: z.boolean(),
})

type ServiceFormInput = z.input<typeof serviceSchema>
type ServiceFormValues = z.output<typeof serviceSchema>

const emptyFormValues: ServiceFormInput = {
  serviceCode: '',
  serviceName: '',
  serviceType: 'EXAMINATION',
  isActive: true,
}

const toPayload = (values: ServiceFormValues): ServiceUpsertInput => ({
  serviceCode: values.serviceCode.trim(),
  serviceName: values.serviceName.trim(),
  serviceType: values.serviceType.trim(),
  isActive: values.isActive,
})

const toFormValues = (record: Service): ServiceFormInput => ({
  serviceCode: record.serviceCode,
  serviceName: record.serviceName,
  serviceType: record.serviceType,
  isActive: record.isActive,
})

export const ServicesPage: React.FC = () => {
  const queryClient = useQueryClient()
  const notify = useNotify()
  const { t } = useI18n()

  const [search, setSearch] = useState('')
  const [activeFilter, setActiveFilter] = useState<string>('')
  const [page, setPage] = useState(1)
  const [isFormOpen, setIsFormOpen] = useState(false)
  const [editingRecord, setEditingRecord] = useState<Service | null>(null)

  const {
    register,
    handleSubmit,
    reset,
    setValue,
    setError,
    formState: { errors },
    control,
  } = useForm<ServiceFormInput, unknown, ServiceFormValues>({
    resolver: zodResolver(serviceSchema),
    defaultValues: emptyFormValues,
  })

  const isActiveValue = useWatch({
    control,
    name: 'isActive',
    defaultValue: true,
  })

  const queryKey = useMemo(
    () => [...queryKeys.services, page, search, activeFilter],
    [page, search, activeFilter]
  )

  const listQuery = useQuery({
    queryKey,
    queryFn: () =>
      listServices({
        page,
        size: 10,
        search,
        isActive: activeFilter === '' ? undefined : activeFilter === 'active',
      }),
  })

  const createMutation = useMutation({
    mutationFn: createService,
    onSuccess: () => {
      notify.success(
        t('services.toast.createdTitle', 'Service created'),
        t('services.toast.createdDescription', 'Catalog item added successfully.')
      )
      setIsFormOpen(false)
      reset(emptyFormValues)
      void queryClient.invalidateQueries({ queryKey: queryKeys.services })
    },
  })

  const updateMutation = useMutation({
    mutationFn: ({ id, payload }: { id: string; payload: ServiceUpsertInput }) =>
      updateService(id, payload),
    onSuccess: () => {
      notify.success(
        t('services.toast.updatedTitle', 'Service updated'),
        t('services.toast.updatedDescription', 'Catalog item updated successfully.')
      )
      setIsFormOpen(false)
      setEditingRecord(null)
      reset(emptyFormValues)
      void queryClient.invalidateQueries({ queryKey: queryKeys.services })
    },
  })

  const openCreate = (): void => {
    setEditingRecord(null)
    reset(emptyFormValues)
    setIsFormOpen(true)
  }

  const openEdit = (record: Service): void => {
    setEditingRecord(record)
    reset(toFormValues(record))
    setIsFormOpen(true)
  }

  const submitForm = (values: ServiceFormValues): void => {
    const payload = toPayload(values)

    const promise = editingRecord
      ? updateMutation.mutateAsync({ id: editingRecord.id, payload })
      : createMutation.mutateAsync(payload)

    void promise.catch((error: unknown) => {
      const normalized = normalizeApiError(error)

      if (normalized.fieldErrors) {
        for (const [field, message] of Object.entries(normalized.fieldErrors)) {
          const fieldKey = field as keyof ServiceFormInput
          setError(fieldKey, { message })
        }
      } else {
        notify.error(t('services.toast.saveErrorTitle', 'Cannot save service'), normalized.message)
      }
    })
  }

  const isSubmitting = createMutation.isPending || updateMutation.isPending

  return (
    <section className="page-section">
      <PageHeader
        title={t('services.title', 'Services')}
        subtitle={t('services.subtitle', 'Manage EMRal services from the catalog module.')}
        action={
          <button className="btn btn-primary" type="button" onClick={openCreate}>
            {t('services.newButton', 'New service')}
          </button>
        }
      />

      <section className="card-section">
        <div className="toolbar-grid">
          <label className="field">
            <span>{t('services.filters.search', 'Search')}</span>
            <input
              value={search}
              onChange={(event) => {
                setPage(1)
                setSearch(event.target.value)
              }}
              placeholder={t('services.filters.searchPlaceholder', 'Code, name, type')}
            />
          </label>
          <label className="field">
            <span>{t('services.filters.status', 'Status')}</span>
            <select
              value={activeFilter}
              onChange={(event) => {
                setPage(1)
                setActiveFilter(event.target.value)
              }}
            >
              <option value="">{t('common.filters.all', 'All')}</option>
              <option value="active">{t('status.active', 'Active')}</option>
              <option value="inactive">{t('status.inactive', 'Inactive')}</option>
            </select>
          </label>
        </div>

        <PaginatedQueryContent
          query={listQuery}
          emptyTitle={t('services.emptyTitle', 'No services')}
          emptyMessage={t('services.emptyMessage', 'Catalog services will appear here.')}
          getErrorMessage={(error) => normalizeApiError(error).message}
          onRetry={() => void listQuery.refetch()}
          onPageChange={setPage}
        >
          {(services) => (
            <div className="table-wrapper">
              <table>
                <thead>
                  <tr>
                    <th>{t('services.table.code', 'Code')}</th>
                    <th>{t('services.table.name', 'Name')}</th>
                    <th>{t('services.table.type', 'Type')}</th>
                    <th>{t('services.table.status', 'Status')}</th>
                    <th>{t('services.table.actions', 'Actions')}</th>
                  </tr>
                </thead>
                <tbody>
                  {services.map((service) => (
                    <tr key={service.id}>
                      <td>{service.serviceCode}</td>
                      <td>{service.serviceName}</td>
                      <td>{service.serviceType}</td>
                      <td>
                        <span
                          className={
                            service.isActive
                              ? 'status-pill status-coming'
                              : 'status-pill status-cancel'
                          }
                        >
                          {service.isActive
                            ? t('status.active', 'ACTIVE')
                            : t('status.inactive', 'INACTIVE')}
                        </span>
                      </td>
                      <td>
                        <button
                          type="button"
                          className="btn btn-secondary"
                          onClick={() => openEdit(service)}
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
        open={isFormOpen}
        title={
          editingRecord
            ? t('services.modal.updateTitle', 'Update service')
            : t('services.modal.createTitle', 'Create service')
        }
        onClose={() => {
          setIsFormOpen(false)
          setEditingRecord(null)
        }}
      >
        <form className="form-grid" onSubmit={handleSubmit(submitForm)}>
          <label className="field">
            <span>{t('services.form.serviceCode', 'Service code')}</span>
            <input {...register('serviceCode')} />
            {errors.serviceCode ? <small>{errors.serviceCode.message}</small> : null}
          </label>
          <label className="field">
            <span>{t('services.form.serviceName', 'Service name')}</span>
            <input {...register('serviceName')} />
            {errors.serviceName ? <small>{errors.serviceName.message}</small> : null}
          </label>
          <label className="field">
            <span>{t('services.form.serviceType', 'Service type')}</span>
            <select {...register('serviceType')}>
              <option value="EXAMINATION">EXAMINATION</option>
              <option value="TEST">TEST</option>
            </select>
          </label>
          <label className="field checkbox-field field-span-2">
            <input
              type="checkbox"
              checked={isActiveValue}
              onChange={(event) => setValue('isActive', event.target.checked)}
            />
            <span>{t('services.form.serviceActive', 'Service is active')}</span>
          </label>

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
                : t('services.form.saveButton', 'Save service')}
            </button>
          </div>
        </form>
      </Modal>
    </section>
  )
}
