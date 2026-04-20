import React from 'react'
import { useI18n } from '../core/i18n/use-i18n'

type ModalProps = {
  open: boolean
  title: string
  onClose: () => void
  children: React.ReactNode
}

export const Modal: React.FC<ModalProps> = ({ open, title, onClose, children }) => {
  const { t } = useI18n()

  if (!open) {
    return null
  }

  return (
    <div className="modal-backdrop" role="dialog" aria-modal="true" aria-label={title}>
      <div className="modal-panel">
        <header className="modal-header">
          <h3>{title}</h3>
          <button className="btn btn-ghost" type="button" onClick={onClose}>
            {t('common.actions.close', 'Close')}
          </button>
        </header>
        <div className="modal-content">{children}</div>
      </div>
    </div>
  )
}
