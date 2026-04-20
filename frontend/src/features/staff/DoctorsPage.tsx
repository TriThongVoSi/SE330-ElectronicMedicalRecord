import React, { useEffect, useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { zodResolver } from '@hookform/resolvers/zod'
import type { FieldValues, Path, UseFormSetError } from 'react-hook-form'
import { useForm } from 'react-hook-form'
import { z } from 'zod'
import { EmptyState } from '../../components/EmptyState'
import { ErrorState } from '../../components/ErrorState'
import { LoadingState } from '../../components/LoadingState'
import { Modal } from '../../components/Modal'
import { PageHeader } from '../../components/PageHeader'
import { normalizeApiError } from '../../core/api/errors'
import { useI18n } from '../../core/i18n/use-i18n'
import { useNotify } from '../../core/utils/notify'
import { queryKeys } from '../../core/utils/query-keys'
import { useAuthStore } from '../auth/auth-store'
import { listServiceOptions } from '../services/api'
import {
  createDoctor,
  deactivateDoctor,
  getDoctorProfile,
  getMyProfile,
  listDoctors,
  updateDoctor,
  updateMyProfile,
} from './api'
import type {
  StaffDoctorCreateInput,
  StaffDoctorProfile,
  StaffDoctorUpdateInput,
  StaffMember,
  StaffProfileUpdateInput,
} from './types'

const adminDoctorFormSchema = z.object({
  username: z.string().trim().optional().or(z.literal('')),
  password: z.string().optional().or(z.literal('')),
  email: z.string().email('Invalid email format.'),
  fullName: z.string().trim().min(2, 'Full name is required.'),
  gender: z.string().trim().max(20, 'Gender is too long.').optional().or(z.literal('')),
  phone: z.string().trim().max(20, 'Phone is too long.').optional().or(z.literal('')),
  address: z.string().trim().max(255, 'Address is too long.').optional().or(z.literal('')),
  serviceId: z.string().trim().optional().or(z.literal('')),
  isConfirmed: z.boolean(),
  isActive: z.boolean(),
})

const profileFormSchema = z.object({
  fullName: z.string().trim().min(2, 'Full name is required.'),
  email: z.string().email('Invalid email format.'),
  gender: z.string().trim().max(20, 'Gender is too long.').optional().or(z.literal('')),
  phone: z.string().trim().max(20, 'Phone is too long.').optional().or(z.literal('')),
  address: z.string().trim().max(255, 'Address is too long.').optional().or(z.literal('')),
})

type AdminDoctorFormInput = z.input<typeof adminDoctorFormSchema>
type AdminDoctorFormValues = z.output<typeof adminDoctorFormSchema>

type ProfileFormInput = z.input<typeof profileFormSchema>
type ProfileFormValues = z.output<typeof profileFormSchema>

const emptyAdminDoctorForm: AdminDoctorFormInput = {
  username: '',
  password: '',
  email: '',
  fullName: '',
  gender: '',
  phone: '',
  address: '',
  serviceId: '',
  isConfirmed: false,
  isActive: true,
}

const emptyProfileForm: ProfileFormInput = {
  fullName: '',
  email: '',
  gender: '',
  phone: '',
  address: '',
}

const toNullable = (value: string | undefined): string | undefined => {
  if (!value) {
    return undefined
  }

  const trimmed = value.trim()
  return trimmed.length === 0 ? undefined : trimmed
}

const toAdminFormValues = (profile: StaffDoctorProfile): AdminDoctorFormInput => ({
  username: profile.username,
  password: '',
  email: profile.email,
  fullName: profile.fullName,
  gender: profile.gender ?? '',
  phone: profile.phone ?? '',
  address: profile.address ?? '',
  serviceId: profile.serviceId ?? '',
  isConfirmed: profile.confirmed,
  isActive: profile.active,
})

const toProfileFormValues = (profile: StaffDoctorProfile): ProfileFormInput => ({
  fullName: profile.fullName,
  email: profile.email,
  gender: profile.gender ?? '',
  phone: profile.phone ?? '',
  address: profile.address ?? '',
})

const applyFieldErrors = <TFieldValues extends FieldValues>(
  fieldErrors: Record<string, string>,
  setError: UseFormSetError<TFieldValues>
): void => {
  for (const [fieldName, fieldMessage] of Object.entries(fieldErrors)) {
    setError(fieldName as Path<TFieldValues>, {
      message: fieldMessage,
    })
  }
}

const toCreateDoctorPayload = (values: AdminDoctorFormValues): StaffDoctorCreateInput => ({
  username: (values.username ?? '').trim(),
  password: values.password ?? '',
  email: values.email.trim(),
  fullName: values.fullName.trim(),
  gender: toNullable(values.gender),
  phone: toNullable(values.phone),
  address: toNullable(values.address),
  serviceId: toNullable(values.serviceId),
  isConfirmed: values.isConfirmed,
  isActive: values.isActive,
})

const toUpdateDoctorPayload = (values: AdminDoctorFormValues): StaffDoctorUpdateInput => ({
  email: values.email.trim(),
  fullName: values.fullName.trim(),
  gender: toNullable(values.gender),
  phone: toNullable(values.phone),
  address: toNullable(values.address),
  serviceId: toNullable(values.serviceId),
  isConfirmed: values.isConfirmed,
  isActive: values.isActive,
})

const toProfilePayload = (values: ProfileFormValues): StaffProfileUpdateInput => ({
  fullName: values.fullName.trim(),
  email: values.email.trim(),
  gender: toNullable(values.gender),
  phone: toNullable(values.phone),
  address: toNullable(values.address),
})

const filterDoctors = (doctors: StaffMember[], search: string): StaffMember[] => {
  const keyword = search.trim().toLowerCase()
  if (!keyword) {
    return doctors
  }

  return doctors.filter((doctor) => {
    const doctorLabel = `${doctor.fullName} ${doctor.email} ${doctor.phone ?? ''}`.toLowerCase()
    return doctorLabel.includes(keyword)
  })
}

export const DoctorsPage: React.FC = () => {
  const queryClient = useQueryClient()
  const notify = useNotify()
  const user = useAuthStore((state) => state.user)
  const { t } = useI18n()

  const isAdmin = user?.role === 'ADMIN'
  const isDoctor = user?.role === 'DOCTOR'

  const [search, setSearch] = useState('')
  const [isDoctorModalOpen, setIsDoctorModalOpen] = useState(false)
  const [editingDoctor, setEditingDoctor] = useState<StaffDoctorProfile | null>(null)
  const [loadingDoctorId, setLoadingDoctorId] = useState<string | null>(null)

  const {
    register: registerAdmin,
    handleSubmit: handleAdminSubmit,
    reset: resetAdmin,
    setError: setAdminError,
    formState: { errors: adminErrors },
  } = useForm<AdminDoctorFormInput, unknown, AdminDoctorFormValues>({
    resolver: zodResolver(adminDoctorFormSchema),
    defaultValues: emptyAdminDoctorForm,
  })

  const {
    register: registerProfile,
    handleSubmit: handleProfileSubmit,
    reset: resetProfile,
    setError: setProfileError,
    formState: { errors: profileErrors },
  } = useForm<ProfileFormInput, unknown, ProfileFormValues>({
    resolver: zodResolver(profileFormSchema),
    defaultValues: emptyProfileForm,
  })

  const doctorsQuery = useQuery({
    queryKey: queryKeys.doctors,
    queryFn: listDoctors,
  })

  const serviceOptionsQuery = useQuery({
    queryKey: [...queryKeys.services, 'options'],
    queryFn: listServiceOptions,
    enabled: isAdmin,
  })

  const myProfileQuery = useQuery({
    queryKey: [...queryKeys.staff, 'my-profile'],
    queryFn: getMyProfile,
    enabled: isDoctor,
  })

  useEffect(() => {
    if (myProfileQuery.data) {
      resetProfile(toProfileFormValues(myProfileQuery.data))
    }
  }, [myProfileQuery.data, resetProfile])

  const createDoctorMutation = useMutation({
    mutationFn: createDoctor,
    onSuccess: () => {
      notify.success(
        t('doctors.toast.createdTitle', 'Doctor created'),
        t('doctors.toast.createdDescription', 'Doctor account has been created successfully.')
      )
      setIsDoctorModalOpen(false)
      setEditingDoctor(null)
      resetAdmin(emptyAdminDoctorForm)
      void queryClient.invalidateQueries({ queryKey: queryKeys.doctors })
    },
  })

  const updateDoctorMutation = useMutation({
    mutationFn: ({ doctorId, payload }: { doctorId: string; payload: StaffDoctorUpdateInput }) =>
      updateDoctor(doctorId, payload),
    onSuccess: () => {
      notify.success(
        t('doctors.toast.updatedTitle', 'Doctor updated'),
        t('doctors.toast.updatedDescription', 'Doctor profile has been updated successfully.')
      )
      setIsDoctorModalOpen(false)
      setEditingDoctor(null)
      resetAdmin(emptyAdminDoctorForm)
      void queryClient.invalidateQueries({ queryKey: queryKeys.doctors })
    },
  })

  const deactivateDoctorMutation = useMutation({
    mutationFn: deactivateDoctor,
    onSuccess: () => {
      notify.success(
        t('doctors.toast.deactivatedTitle', 'Doctor deactivated'),
        t('doctors.toast.deactivatedDescription', 'Doctor account has been marked inactive.')
      )
      void queryClient.invalidateQueries({ queryKey: queryKeys.doctors })
    },
  })

  const updateProfileMutation = useMutation({
    mutationFn: updateMyProfile,
    onSuccess: (profile) => {
      notify.success(
        t('doctors.toast.profileUpdatedTitle', 'Profile updated'),
        t('doctors.toast.profileUpdatedDescription', 'Your doctor profile has been updated.')
      )
      resetProfile(toProfileFormValues(profile))
      void queryClient.invalidateQueries({ queryKey: [...queryKeys.staff, 'my-profile'] })
      void queryClient.invalidateQueries({ queryKey: queryKeys.doctors })
    },
  })

  const filteredDoctors = useMemo(
    () => filterDoctors(doctorsQuery.data ?? [], search),
    [doctorsQuery.data, search]
  )

  const openCreateDoctorModal = (): void => {
    setEditingDoctor(null)
    resetAdmin(emptyAdminDoctorForm)
    setIsDoctorModalOpen(true)
  }

  const openEditDoctorModal = (doctorId: string): void => {
    setLoadingDoctorId(doctorId)

    void getDoctorProfile(doctorId)
      .then((profile) => {
        setEditingDoctor(profile)
        resetAdmin(toAdminFormValues(profile))
        setIsDoctorModalOpen(true)
      })
      .catch((error: unknown) => {
        notify.error(
          t('doctors.toast.loadProfileErrorTitle', 'Cannot load doctor profile'),
          normalizeApiError(error).message
        )
      })
      .finally(() => {
        setLoadingDoctorId(null)
      })
  }

  const handleDeactivateDoctor = (doctor: StaffMember): void => {
    const shouldDeactivate = window.confirm(
      t(
        'doctors.confirmDeactivate',
        'Deactivate doctor "{fullName}"? This will set the account as inactive.',
        {
          fullName: doctor.fullName,
        }
      )
    )

    if (!shouldDeactivate) {
      return
    }

    void deactivateDoctorMutation.mutateAsync(doctor.id).catch((error: unknown) => {
      notify.error(
        t('doctors.toast.deactivateErrorTitle', 'Cannot deactivate doctor'),
        normalizeApiError(error).message
      )
    })
  }

  const submitAdminDoctorForm = (values: AdminDoctorFormValues): void => {
    if (!editingDoctor) {
      const normalizedUsername = (values.username ?? '').trim()
      if (!normalizedUsername || normalizedUsername.length < 3) {
        setAdminError('username', { message: 'Username must be at least 3 characters.' })
        return
      }

      if (!values.password || values.password.length < 6) {
        setAdminError('password', { message: 'Password must be at least 6 characters.' })
        return
      }
    }

    const request = editingDoctor
      ? updateDoctorMutation.mutateAsync({
          doctorId: editingDoctor.id,
          payload: toUpdateDoctorPayload(values),
        })
      : createDoctorMutation.mutateAsync(toCreateDoctorPayload(values))

    void request.catch((error: unknown) => {
      const normalized = normalizeApiError(error)
      if (normalized.fieldErrors) {
        applyFieldErrors(normalized.fieldErrors, setAdminError)
      } else {
        notify.error(t('doctors.toast.saveErrorTitle', 'Cannot save doctor'), normalized.message)
      }
    })
  }

  const submitMyProfileForm = (values: ProfileFormValues): void => {
    void updateProfileMutation.mutateAsync(toProfilePayload(values)).catch((error: unknown) => {
      const normalized = normalizeApiError(error)
      if (normalized.fieldErrors) {
        applyFieldErrors(normalized.fieldErrors, setProfileError)
      } else {
        notify.error(
          t('doctors.toast.updateProfileErrorTitle', 'Cannot update profile'),
          normalized.message
        )
      }
    })
  }

  const isDoctorFormSubmitting = createDoctorMutation.isPending || updateDoctorMutation.isPending

  return (
    <section className="page-section">
      <PageHeader
        title={t('doctors.title', 'Doctors')}
        subtitle={t(
          'doctors.subtitle',
          'Manage doctor profiles, confirmation state, and specialty assignment.'
        )}
        action={
          isAdmin ? (
            <button type="button" className="btn btn-primary" onClick={openCreateDoctorModal}>
              {t('doctors.newButton', 'New doctor')}
            </button>
          ) : undefined
        }
      />

      {isDoctor ? (
        <section className="card-section">
          <h2>{t('doctors.myProfile.title', 'My profile')}</h2>
          {myProfileQuery.isError ? (
            <p className="form-error">{normalizeApiError(myProfileQuery.error).message}</p>
          ) : null}

          <form className="form-grid" onSubmit={handleProfileSubmit(submitMyProfileForm)}>
            <label className="field">
              <span>{t('doctors.form.fullName', 'Full name')}</span>
              <input {...registerProfile('fullName')} disabled={myProfileQuery.isLoading} />
              {profileErrors.fullName ? <small>{profileErrors.fullName.message}</small> : null}
            </label>

            <label className="field">
              <span>{t('doctors.form.email', 'Email')}</span>
              <input
                type="email"
                {...registerProfile('email')}
                disabled={myProfileQuery.isLoading}
              />
              {profileErrors.email ? <small>{profileErrors.email.message}</small> : null}
            </label>

            <label className="field">
              <span>{t('doctors.form.gender', 'Gender')}</span>
              <input {...registerProfile('gender')} disabled={myProfileQuery.isLoading} />
              {profileErrors.gender ? <small>{profileErrors.gender.message}</small> : null}
            </label>

            <label className="field">
              <span>{t('doctors.form.phone', 'Phone')}</span>
              <input {...registerProfile('phone')} disabled={myProfileQuery.isLoading} />
              {profileErrors.phone ? <small>{profileErrors.phone.message}</small> : null}
            </label>

            <label className="field field-span-2">
              <span>{t('doctors.form.address', 'Address')}</span>
              <input {...registerProfile('address')} disabled={myProfileQuery.isLoading} />
              {profileErrors.address ? <small>{profileErrors.address.message}</small> : null}
            </label>

            <div className="form-actions field-span-2">
              <button
                type="submit"
                className="btn btn-primary"
                disabled={myProfileQuery.isLoading || updateProfileMutation.isPending}
              >
                {updateProfileMutation.isPending
                  ? t('common.actions.saving', 'Saving...')
                  : t('doctors.myProfile.saveButton', 'Save profile')}
              </button>
            </div>
          </form>
        </section>
      ) : null}

      <section className="card-section">
        <div className="toolbar-grid">
          <label className="field">
            <span>{t('doctors.filters.searchDoctor', 'Search doctor')}</span>
            <input
              value={search}
              onChange={(event) => setSearch(event.target.value)}
              placeholder={t('doctors.filters.searchPlaceholder', 'Name, email, phone')}
            />
          </label>
        </div>

        {doctorsQuery.isLoading ? (
          <LoadingState label={t('doctors.loadingDirectory', 'Loading doctor directory...')} />
        ) : null}

        {doctorsQuery.isError ? (
          <ErrorState
            message={normalizeApiError(doctorsQuery.error).message}
            onRetry={() => void doctorsQuery.refetch()}
          />
        ) : null}

        {doctorsQuery.isSuccess && filteredDoctors.length === 0 ? (
          <EmptyState
            title={t('doctors.emptyTitle', 'No doctors')}
            message={t('doctors.emptyMessage', 'Doctor records will appear here.')}
          />
        ) : null}

        {doctorsQuery.isSuccess && filteredDoctors.length > 0 ? (
          <div className="table-wrapper">
            <table>
              <thead>
                <tr>
                  <th>{t('doctors.table.fullName', 'Full name')}</th>
                  <th>{t('doctors.table.email', 'Email')}</th>
                  <th>{t('doctors.table.phone', 'Phone')}</th>
                  <th>{t('doctors.table.role', 'Role')}</th>
                  {isAdmin ? <th>{t('doctors.table.actions', 'Actions')}</th> : null}
                </tr>
              </thead>
              <tbody>
                {filteredDoctors.map((doctor) => (
                  <tr key={doctor.id}>
                    <td>{doctor.fullName}</td>
                    <td>{doctor.email}</td>
                    <td>{doctor.phone ?? '-'}</td>
                    <td>{doctor.role}</td>
                    {isAdmin ? (
                      <td>
                        <div className="table-actions">
                          <button
                            type="button"
                            className="btn btn-secondary"
                            onClick={() => openEditDoctorModal(doctor.id)}
                            disabled={loadingDoctorId === doctor.id}
                          >
                            {loadingDoctorId === doctor.id
                              ? t('common.loading.default', 'Loading data...')
                              : t('common.actions.edit', 'Edit')}
                          </button>
                          <button
                            type="button"
                            className="btn btn-secondary"
                            onClick={() => handleDeactivateDoctor(doctor)}
                            disabled={deactivateDoctorMutation.isPending}
                          >
                            {t('doctors.actions.deactivate', 'Deactivate')}
                          </button>
                        </div>
                      </td>
                    ) : null}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : null}
      </section>

      <Modal
        open={isDoctorModalOpen}
        title={
          editingDoctor
            ? t('doctors.modal.updateTitle', 'Update doctor profile')
            : t('doctors.modal.createTitle', 'Create doctor')
        }
        onClose={() => {
          setIsDoctorModalOpen(false)
          setEditingDoctor(null)
          resetAdmin(emptyAdminDoctorForm)
        }}
      >
        <form className="form-grid" onSubmit={handleAdminSubmit(submitAdminDoctorForm)}>
          {!editingDoctor ? (
            <>
              <label className="field">
                <span>{t('doctors.form.username', 'Username')}</span>
                <input {...registerAdmin('username')} />
                {adminErrors.username ? <small>{adminErrors.username.message}</small> : null}
              </label>

              <label className="field">
                <span>{t('doctors.form.password', 'Password')}</span>
                <input type="password" {...registerAdmin('password')} />
                {adminErrors.password ? <small>{adminErrors.password.message}</small> : null}
              </label>
            </>
          ) : (
            <label className="field field-span-2">
              <span>{t('doctors.form.username', 'Username')}</span>
              <input {...registerAdmin('username')} disabled />
            </label>
          )}

          <label className="field">
            <span>{t('doctors.form.fullName', 'Full name')}</span>
            <input {...registerAdmin('fullName')} />
            {adminErrors.fullName ? <small>{adminErrors.fullName.message}</small> : null}
          </label>

          <label className="field">
            <span>{t('doctors.form.email', 'Email')}</span>
            <input type="email" {...registerAdmin('email')} />
            {adminErrors.email ? <small>{adminErrors.email.message}</small> : null}
          </label>

          <label className="field">
            <span>{t('doctors.form.gender', 'Gender')}</span>
            <input {...registerAdmin('gender')} />
            {adminErrors.gender ? <small>{adminErrors.gender.message}</small> : null}
          </label>

          <label className="field">
            <span>{t('doctors.form.phone', 'Phone')}</span>
            <input {...registerAdmin('phone')} />
            {adminErrors.phone ? <small>{adminErrors.phone.message}</small> : null}
          </label>

          <label className="field field-span-2">
            <span>{t('doctors.form.address', 'Address')}</span>
            <input {...registerAdmin('address')} />
            {adminErrors.address ? <small>{adminErrors.address.message}</small> : null}
          </label>

          <label className="field">
            <span>{t('doctors.form.specialty', 'Specialty')}</span>
            <select {...registerAdmin('serviceId')}>
              <option value="">{t('doctors.form.unassigned', 'Unassigned')}</option>
              {serviceOptionsQuery.data?.map((service) => (
                <option key={service.id} value={service.id}>
                  {service.serviceCode} - {service.serviceName}
                </option>
              ))}
            </select>
          </label>

          <label className="field checkbox-field">
            <input type="checkbox" {...registerAdmin('isConfirmed')} />
            <span>{t('doctors.form.confirmed', 'Doctor is confirmed')}</span>
          </label>

          <label className="field checkbox-field field-span-2">
            <input type="checkbox" {...registerAdmin('isActive')} />
            <span>{t('doctors.form.accountActive', 'Account is active')}</span>
          </label>

          {serviceOptionsQuery.isError ? (
            <p className="form-error field-span-2">
              {t('doctors.form.specialtyError', 'Cannot load specialty options:')}{' '}
              {normalizeApiError(serviceOptionsQuery.error).message}
            </p>
          ) : null}

          <div className="form-actions field-span-2">
            <button
              type="button"
              className="btn btn-secondary"
              onClick={() => {
                setIsDoctorModalOpen(false)
                setEditingDoctor(null)
                resetAdmin(emptyAdminDoctorForm)
              }}
            >
              {t('common.actions.cancel', 'Cancel')}
            </button>
            <button type="submit" className="btn btn-primary" disabled={isDoctorFormSubmitting}>
              {isDoctorFormSubmitting
                ? t('common.actions.saving', 'Saving...')
                : t('doctors.form.saveButton', 'Save doctor')}
            </button>
          </div>
        </form>
      </Modal>
    </section>
  )
}
