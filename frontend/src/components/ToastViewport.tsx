import React, { useEffect } from 'react'
import { useToastStore } from '../app/toast-store'
import { useI18n } from '../core/i18n/use-i18n'

const dismissDelayMs = 3500

export const ToastViewport: React.FC = () => {
  const toasts = useToastStore((state) => state.toasts)
  const removeToast = useToastStore((state) => state.removeToast)
  const { t } = useI18n()

  useEffect(() => {
    if (toasts.length === 0) {
      return undefined
    }

    const timers = toasts.map((toast) =>
      window.setTimeout(() => {
        removeToast(toast.id)
      }, dismissDelayMs)
    )

    return () => {
      timers.forEach((timer) => window.clearTimeout(timer))
    }
  }, [toasts, removeToast])

  if (toasts.length === 0) {
    return null
  }

  return (
    <div className="toast-viewport" aria-live="polite">
      {toasts.map((toast) => (
        <article key={toast.id} className={`toast toast-${toast.type}`}>
          <strong>{toast.title}</strong>
          {toast.description ? <span>{toast.description}</span> : null}
          <button type="button" className="btn btn-ghost" onClick={() => removeToast(toast.id)}>
            {t('common.actions.dismiss', 'Dismiss')}
          </button>
        </article>
      ))}
    </div>
  )
}
