import React from 'react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { ToastViewport } from '../components/ToastViewport'
import { I18nProvider } from '../core/i18n/i18n-provider'
import { AuthBootstrap } from '../features/auth/AuthBootstrap'

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 30000,
      retry: 1,
      refetchOnWindowFocus: false,
    },
    mutations: {
      retry: 0,
    },
  },
})

type AppProvidersProps = {
  children: React.ReactNode
}

export const AppProviders: React.FC<AppProvidersProps> = ({ children }) => {
  return (
    <QueryClientProvider client={queryClient}>
      <I18nProvider>
        <AuthBootstrap>{children}</AuthBootstrap>
        <ToastViewport />
      </I18nProvider>
    </QueryClientProvider>
  )
}
