import { useToastStore } from '../../app/toast-store'

export const useNotify = () => {
  const pushToast = useToastStore((state) => state.pushToast)

  return {
    success: (title: string, description?: string) =>
      pushToast({ title, description, type: 'success' }),
    error: (title: string, description?: string) =>
      pushToast({ title, description, type: 'error' }),
    info: (title: string, description?: string) => pushToast({ title, description, type: 'info' }),
  }
}
