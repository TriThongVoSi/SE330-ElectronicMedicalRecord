import React from 'react'
import { useI18n } from '../core/i18n/use-i18n'

type PaginationControlsProps = {
  page: number
  totalPages: number
  totalItems: number
  onPageChange: (page: number) => void
}

export const PaginationControls: React.FC<PaginationControlsProps> = ({
  page,
  totalPages,
  totalItems,
  onPageChange,
}) => {
  const { t } = useI18n()
  const canPrev = page > 1
  const canNext = page < totalPages

  return (
    <div className="pagination-bar">
      <span>
        {t('common.pagination.summary', 'Page {page} / {totalPages} ({totalItems} items)', {
          page,
          totalPages,
          totalItems,
        })}
      </span>
      <div className="pagination-actions">
        <button
          type="button"
          className="btn btn-secondary"
          disabled={!canPrev}
          onClick={() => onPageChange(page - 1)}
        >
          {t('common.actions.previous', 'Previous')}
        </button>
        <button
          type="button"
          className="btn btn-secondary"
          disabled={!canNext}
          onClick={() => onPageChange(page + 1)}
        >
          {t('common.actions.next', 'Next')}
        </button>
      </div>
    </div>
  )
}
