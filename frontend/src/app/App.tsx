import React from 'react'
import { AppRouter } from './router'
import { AppErrorBoundary } from './ErrorBoundary'

export const App: React.FC = () => {
  return (
    <AppErrorBoundary>
      <AppRouter />
    </AppErrorBoundary>
  )
}
