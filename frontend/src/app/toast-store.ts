import { create } from 'zustand'

export type ToastType = 'success' | 'error' | 'info'

type ToastItem = {
  id: string
  title: string
  description?: string
  type: ToastType
}

type ToastStore = {
  toasts: ToastItem[]
  pushToast: (input: Omit<ToastItem, 'id'>) => void
  removeToast: (id: string) => void
}

let sequence = 0

export const useToastStore = create<ToastStore>((set) => ({
  toasts: [],
  pushToast: (input) => {
    sequence += 1
    const id = `toast-${sequence}`
    set((state) => ({ toasts: [...state.toasts, { id, ...input }] }))
  },
  removeToast: (id) =>
    set((state) => ({
      toasts: state.toasts.filter((toast) => toast.id !== id),
    })),
}))
