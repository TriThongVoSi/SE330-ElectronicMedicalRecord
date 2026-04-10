import React from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'
import { z } from 'zod'
import { PageHeader } from '../../components/PageHeader'
import { LoadingState } from '../../components/LoadingState'
import { ErrorState } from '../../components/ErrorState'
import { normalizeApiError } from '../../core/api/errors'
import { useI18n } from '../../core/i18n/use-i18n'
import { formatDate } from '../../core/utils/format'
import { useNotify } from '../../core/utils/notify'
import { queryKeys } from '../../core/utils/query-keys'
import { getPatientPortalProfile, updatePatientPortalProfile } from './api'

const optionalPositiveNumber = z.preprocess(
  (value) => (value === '' || value === null || value === undefined ? undefined : value),
  z.coerce.number().positive().optional()
)

const profileSchema = z.object({
  phone: z.string().trim().min(8, 'Phone is required.'),
  address: z.string().trim().optional(),
  heightCm: optionalPositiveNumber,
  weightKg: optionalPositiveNumber,
  drugAllergies: z.string().trim().optional(),
})

type ProfileFormInput = z.input<typeof profileSchema>
type ProfileFormValues = z.output<typeof profileSchema>

const toPayload = (values: ProfileFormValues) => ({
  phone: values.phone.trim(),
  address: values.address || undefined,
  heightCm: values.heightCm,
  weightKg: values.weightKg,
  drugAllergies: values.drugAllergies || undefined,
})

export const PatientProfilePage: React.FC = () => {
  const { t } = useI18n()
  const notify = useNotify()
  const queryClient = useQueryClient()

  const profileQuery = useQuery({
    queryKey: queryKeys.patientPortalProfile,
    queryFn: getPatientPortalProfile,
  })

  const {
    register,
    handleSubmit,
    reset,
    setError,
    formState: { errors },
  } = useForm<ProfileFormInput, unknown, ProfileFormValues>({
    resolver: zodResolver(profileSchema),
    defaultValues: {
      phone: '',
      address: '',
      heightCm: undefined,
      weightKg: undefined,
      drugAllergies: '',
    },
  })

  React.useEffect(() => {
    if (!profileQuery.data) {
      return
    }

    reset({
      phone: profileQuery.data.phone ?? '',
      address: profileQuery.data.address ?? '',
      heightCm: profileQuery.data.heightCm,
      weightKg: profileQuery.data.weightKg,
      drugAllergies: profileQuery.data.drugAllergies ?? '',
    })
  }, [profileQuery.data, reset])

  const updateMutation = useMutation({
    mutationFn: updatePatientPortalProfile,
    onSuccess: () => {
      notify.success(
        t('patientPortal.profile.toast.savedTitle', 'Profile updated'),
        t('patientPortal.profile.toast.savedDescription', 'Your profile has been updated successfully.')
      )
      void queryClient.invalidateQueries({ queryKey: queryKeys.patientPortalProfile })
    },
  })

  const onSubmit = (values: ProfileFormValues): void => {
    updateMutation.mutate(toPayload(values), {
      onError: (error) => {
        const normalized = normalizeApiError(error)

        if (normalized.fieldErrors) {
          for (const [field, message] of Object.entries(normalized.fieldErrors)) {
            const key = field as keyof ProfileFormInput
            setError(key, { message })
          }
          return
        }

        notify.error(
          t('patientPortal.profile.toast.saveErrorTitle', 'Cannot update profile'),
          normalized.message
        )
      },
    })
  }

  return (
    <section className="page-section">
      <PageHeader
        title={t('patientPortal.profile.title', 'My Profile')}
        subtitle={t(
          'patientPortal.profile.subtitle',
          'Update your contact and health profile information.'
        )}
      />

      {profileQuery.isLoading ? (
        <LoadingState label={t('patientPortal.profile.loading', 'Loading profile...')} />
      ) : null}

      {profileQuery.isError ? (
        <ErrorState
          message={normalizeApiError(profileQuery.error).message}
          onRetry={() => void profileQuery.refetch()}
        />
      ) : null}

      {profileQuery.isSuccess ? (
        <>
          <section className="card-section">
            <h2>{t('patientPortal.profile.identityTitle', 'Identity information')}</h2>
            <dl className="detail-grid">
              <div>
                <dt>{t('patientPortal.profile.fields.patientCode', 'Patient code')}</dt>
                <dd>{profileQuery.data.patientCode}</dd>
              </div>
              <div>
                <dt>{t('patientPortal.profile.fields.fullName', 'Full name')}</dt>
                <dd>{profileQuery.data.fullName}</dd>
              </div>
              <div>
                <dt>{t('patientPortal.profile.fields.gender', 'Gender')}</dt>
                <dd>{profileQuery.data.gender ?? '-'}</dd>
              </div>
              <div>
                <dt>{t('patientPortal.profile.fields.dateOfBirth', 'Date of birth')}</dt>
                <dd>{formatDate(profileQuery.data.dateOfBirth)}</dd>
              </div>
              <div>
                <dt>{t('patientPortal.profile.fields.email', 'Email')}</dt>
                <dd>{profileQuery.data.email ?? '-'}</dd>
              </div>
            </dl>
          </section>

          <section className="card-section">
            <h2>{t('patientPortal.profile.editTitle', 'Editable profile')}</h2>
            <form className="form-grid" onSubmit={handleSubmit(onSubmit)}>
              <label className="field">
                <span>{t('patientPortal.profile.fields.phone', 'Phone')}</span>
                <input {...register('phone')} />
                {errors.phone ? <small>{errors.phone.message}</small> : null}
              </label>

              <label className="field">
                <span>{t('patientPortal.profile.fields.address', 'Address')}</span>
                <input {...register('address')} />
              </label>

              <label className="field">
                <span>{t('patientPortal.profile.fields.heightCm', 'Height (cm)')}</span>
                <input type="number" step="0.1" {...register('heightCm')} />
                {errors.heightCm ? <small>{errors.heightCm.message}</small> : null}
              </label>

              <label className="field">
                <span>{t('patientPortal.profile.fields.weightKg', 'Weight (kg)')}</span>
                <input type="number" step="0.1" {...register('weightKg')} />
                {errors.weightKg ? <small>{errors.weightKg.message}</small> : null}
              </label>

              <label className="field field-span-2">
                <span>{t('patientPortal.profile.fields.drugAllergies', 'Drug allergies')}</span>
                <textarea rows={3} {...register('drugAllergies')} />
              </label>

              <div className="form-actions field-span-2">
                <button type="submit" className="btn btn-primary" disabled={updateMutation.isPending}>
                  {updateMutation.isPending
                    ? t('common.actions.saving', 'Saving...')
                    : t('patientPortal.profile.saveButton', 'Save profile')}
                </button>
              </div>
            </form>
          </section>
        </>
      ) : null}
    </section>
  )
}
