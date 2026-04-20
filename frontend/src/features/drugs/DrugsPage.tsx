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
import { formatCurrency, formatDate } from '../../core/utils/format'
import { useNotify } from '../../core/utils/notify'
import { queryKeys } from '../../core/utils/query-keys'
import { createDrug, listDrugs, updateDrug } from './api'
import type { Drug, DrugUpsertInput } from './types'

const drugSchema = z.object({
  drugCode: z.string().trim().min(2, 'Drug code is required.'),
  drugName: z.string().trim().min(2, 'Drug name is required.'),
  manufacturer: z.string().trim().min(2, 'Manufacturer is required.'),
  expiryDate: z.string().trim().min(1, 'Expiry date is required.'),
  unit: z.string().trim().min(1, 'Unit is required.'),
  price: z.coerce.number().positive('Price must be greater than 0.'),
  stockQuantity: z.coerce.number().int().min(0, 'Stock cannot be negative.'),
  isActive: z.boolean(),
})

type DrugFormInput = z.input<typeof drugSchema>
type DrugFormValues = z.output<typeof drugSchema>

const emptyFormValues: DrugFormInput = {
  drugCode: '',
  drugName: '',
  manufacturer: '',
  expiryDate: '',
  unit: 'TABLET',
  price: 0,
  stockQuantity: 0,
  isActive: true,
}

const toPayload = (values: DrugFormValues): DrugUpsertInput => ({
  drugCode: values.drugCode.trim(),
  drugName: values.drugName.trim(),
  manufacturer: values.manufacturer.trim(),
  expiryDate: values.expiryDate,
  unit: values.unit.trim(),
  price: values.price,
  stockQuantity: values.stockQuantity,
  isActive: values.isActive,
})

const toFormValues = (record: Drug): DrugFormInput => ({
  drugCode: record.drugCode,
  drugName: record.drugName,
  manufacturer: record.manufacturer,
  expiryDate: record.expiryDate,
  unit: record.unit,
  price: record.price,
  stockQuantity: record.stockQuantity,
  isActive: record.isActive,
})

export const DrugsPage: React.FC = () => {
  const queryClient = useQueryClient()
  const notify = useNotify()
  const { t } = useI18n()

  const [search, setSearch] = useState('')
  const [activeFilter, setActiveFilter] = useState<string>('')
  const [page, setPage] = useState(1)
  const [isFormOpen, setIsFormOpen] = useState(false)
  const [editingRecord, setEditingRecord] = useState<Drug | null>(null)

  const {
    register,
    handleSubmit,
    setError,
    setValue,
    reset,
    formState: { errors },
    control,
  } = useForm<DrugFormInput, unknown, DrugFormValues>({
    resolver: zodResolver(drugSchema),
    defaultValues: emptyFormValues,
  })

  const isActiveValue = useWatch({
    control,
    name: 'isActive',
    defaultValue: true,
  })

  const queryKey = useMemo(
    () => [...queryKeys.drugs, page, search, activeFilter],
    [page, search, activeFilter]
  )

  const listQuery = useQuery({
    queryKey,
    queryFn: () =>
      listDrugs({
        page,
        size: 10,
        search,
        isActive: activeFilter === '' ? undefined : activeFilter === 'active' ? true : false,
      }),
  })

  const createMutation = useMutation({
    mutationFn: createDrug,
    onSuccess: () => {
      notify.success(
        t('drugs.toast.createdTitle', 'Drug created'),
        t('drugs.toast.createdDescription', 'Drug catalog has been updated.')
      )
      setIsFormOpen(false)
      reset(emptyFormValues)
      void queryClient.invalidateQueries({ queryKey: queryKeys.drugs })
    },
  })

  const updateMutation = useMutation({
    mutationFn: ({ id, payload }: { id: string; payload: DrugUpsertInput }) =>
      updateDrug(id, payload),
    onSuccess: () => {
      notify.success(
        t('drugs.toast.updatedTitle', 'Drug updated'),
        t('drugs.toast.updatedDescription', 'Drug information has been updated.')
      )
      setIsFormOpen(false)
      setEditingRecord(null)
      reset(emptyFormValues)
      void queryClient.invalidateQueries({ queryKey: queryKeys.drugs })
    },
  })

  const openCreate = (): void => {
    setEditingRecord(null)
    reset(emptyFormValues)
    setIsFormOpen(true)
  }

  const openEdit = (record: Drug): void => {
    setEditingRecord(record)
    reset(toFormValues(record))
    setIsFormOpen(true)
  }

  const submitForm = (values: DrugFormValues): void => {
    const payload = toPayload(values)

    const promise = editingRecord
      ? updateMutation.mutateAsync({ id: editingRecord.id, payload })
      : createMutation.mutateAsync(payload)

    void promise.catch((error: unknown) => {
      const normalized = normalizeApiError(error)
      if (normalized.fieldErrors) {
        for (const [field, message] of Object.entries(normalized.fieldErrors)) {
          const fieldKey = field as keyof DrugFormInput
          setError(fieldKey, { message })
        }
      } else {
        notify.error(t('drugs.toast.saveErrorTitle', 'Cannot save drug'), normalized.message)
      }
    })
  }

  const isSubmitting = createMutation.isPending || updateMutation.isPending

  return (
    <section className="page-section">
      <PageHeader
        title={t('drugs.title', 'Drugs')}
        subtitle={t('drugs.subtitle', 'Maintain medicine catalog with stock and pricing data.')}
        action={
          <button className="btn btn-primary" type="button" onClick={openCreate}>
            {t('drugs.newButton', 'New drug')}
          </button>
        }
      />

      <section className="card-section">
        <div className="toolbar-grid">
          <label className="field">
            <span>{t('drugs.filters.search', 'Search')}</span>
            <input
              value={search}
              onChange={(event) => {
                setPage(1)
                setSearch(event.target.value)
              }}
              placeholder={t('drugs.filters.searchPlaceholder', 'Code, name, manufacturer')}
            />
          </label>
          <label className="field">
            <span>{t('drugs.filters.status', 'Status')}</span>
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
          emptyTitle={t('drugs.emptyTitle', 'No drugs')}
          emptyMessage={t('drugs.emptyMessage', 'Drug catalog entries will show here.')}
          getErrorMessage={(error) => normalizeApiError(error).message}
          onRetry={() => void listQuery.refetch()}
          onPageChange={setPage}
        >
          {(drugs) => (
            <div className="table-wrapper">
              <table>
                <thead>
                  <tr>
                    <th>{t('drugs.table.code', 'Code')}</th>
                    <th>{t('drugs.table.name', 'Name')}</th>
                    <th>{t('drugs.table.manufacturer', 'Manufacturer')}</th>
                    <th>{t('drugs.table.expiry', 'Expiry')}</th>
                    <th>{t('drugs.table.price', 'Price')}</th>
                    <th>{t('drugs.table.stock', 'Stock')}</th>
                    <th>{t('drugs.table.status', 'Status')}</th>
                    <th>{t('drugs.table.actions', 'Actions')}</th>
                  </tr>
                </thead>
                <tbody>
                  {drugs.map((drug) => (
                    <tr key={drug.id}>
                      <td>{drug.drugCode}</td>
                      <td>{drug.drugName}</td>
                      <td>{drug.manufacturer}</td>
                      <td>{formatDate(drug.expiryDate)}</td>
                      <td>{formatCurrency(drug.price)}</td>
                      <td>{drug.stockQuantity}</td>
                      <td>
                        <span
                          className={
                            drug.isActive
                              ? 'status-pill status-coming'
                              : 'status-pill status-cancel'
                          }
                        >
                          {drug.isActive
                            ? t('status.active', 'ACTIVE')
                            : t('status.inactive', 'INACTIVE')}
                        </span>
                      </td>
                      <td>
                        <button
                          className="btn btn-secondary"
                          type="button"
                          onClick={() => openEdit(drug)}
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
            ? t('drugs.modal.updateTitle', 'Update drug')
            : t('drugs.modal.createTitle', 'Create drug')
        }
        onClose={() => {
          setIsFormOpen(false)
          setEditingRecord(null)
        }}
      >
        <form className="form-grid" onSubmit={handleSubmit(submitForm)}>
          <label className="field">
            <span>{t('drugs.form.drugCode', 'Drug code')}</span>
            <input {...register('drugCode')} />
            {errors.drugCode ? <small>{errors.drugCode.message}</small> : null}
          </label>
          <label className="field">
            <span>{t('drugs.form.drugName', 'Drug name')}</span>
            <input {...register('drugName')} />
            {errors.drugName ? <small>{errors.drugName.message}</small> : null}
          </label>
          <label className="field">
            <span>{t('drugs.form.manufacturer', 'Manufacturer')}</span>
            <input {...register('manufacturer')} />
            {errors.manufacturer ? <small>{errors.manufacturer.message}</small> : null}
          </label>
          <label className="field">
            <span>{t('drugs.form.expiryDate', 'Expiry date')}</span>
            <input type="date" {...register('expiryDate')} />
            {errors.expiryDate ? <small>{errors.expiryDate.message}</small> : null}
          </label>
          <label className="field">
            <span>{t('drugs.form.unit', 'Unit')}</span>
            <input {...register('unit')} />
            {errors.unit ? <small>{errors.unit.message}</small> : null}
          </label>
          <label className="field">
            <span>{t('drugs.form.price', 'Price')}</span>
            <input type="number" min={1} step={1000} {...register('price')} />
            {errors.price ? <small>{errors.price.message}</small> : null}
          </label>
          <label className="field">
            <span>{t('drugs.form.stockQuantity', 'Stock quantity')}</span>
            <input type="number" min={0} {...register('stockQuantity')} />
            {errors.stockQuantity ? <small>{errors.stockQuantity.message}</small> : null}
          </label>
          <label className="field checkbox-field">
            <input
              type="checkbox"
              checked={isActiveValue}
              onChange={(event) => setValue('isActive', event.target.checked)}
            />
            <span>{t('drugs.form.drugActive', 'Drug is active')}</span>
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
                : t('drugs.form.saveButton', 'Save drug')}
            </button>
          </div>
        </form>
      </Modal>
    </section>
  )
}
