import React from 'react'
import { useI18n } from '../core/i18n/use-i18n'

type EmptyStateProps = {
  title?: string
  message?: string
}

export const EmptyState: React.FC<EmptyStateProps> = ({ title, message }) => {
  const { t } = useI18n()

  return (
    <div className="state-box state-box-empty">
      <strong>{title ?? t('common.empty.title', 'No data available')}</strong>
      <span>
        {message ?? t('common.empty.message', 'Try adjusting your filters or create a new record.')}
      </span>
    </div>
  )
}
