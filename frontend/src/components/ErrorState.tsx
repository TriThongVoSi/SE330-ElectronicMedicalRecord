import React from 'react'
import { useI18n } from '../core/i18n/use-i18n'

type ErrorStateProps = {
  message: string
  onRetry?: () => void
}

export const ErrorState: React.FC<ErrorStateProps> = ({ message, onRetry }) => {
  const { t } = useI18n()

  return (
    <div className="state-box state-box-error">
      <strong>{t('common.error.title', 'Something went wrong')}</strong>
      <span>{message}</span>
      {onRetry ? (
        <button className="btn btn-secondary" type="button" onClick={onRetry}>
          {t('common.actions.retry', 'Retry')}
        </button>
      ) : null}
    </div>
  )
}
