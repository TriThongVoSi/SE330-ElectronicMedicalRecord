import React from 'react'
import { useI18n } from '../core/i18n/use-i18n'

type LoadingStateProps = {
  label?: string
}

export const LoadingState: React.FC<LoadingStateProps> = ({ label }) => {
  const { t } = useI18n()

  return (
    <div className="state-box" role="status" aria-live="polite">
      <div className="spinner" />
      <span>{label ?? t('common.loading.default', 'Loading data...')}</span>
    </div>
  )
}
