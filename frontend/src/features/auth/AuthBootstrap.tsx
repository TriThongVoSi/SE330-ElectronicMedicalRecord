import React, { useEffect, useRef } from 'react'
import { bootstrapAuthSession } from './api'
import { useAuthStore } from './auth-store'

type AuthBootstrapProps = {
  children: React.ReactNode
}

export const AuthBootstrap: React.FC<AuthBootstrapProps> = ({ children }) => {
  const initialized = useAuthStore((state) => state.initialized)
  const setInitialized = useAuthStore((state) => state.setInitialized)
  const didRunRef = useRef(false)

  useEffect(() => {
    if (didRunRef.current) {
      return
    }

    didRunRef.current = true

    void bootstrapAuthSession().finally(() => {
      setInitialized(true)
    })
  }, [setInitialized])

  if (!initialized) {
    return (
      <div className="full-screen-loader">
        <div className="spinner" />
        <span>Preparing session...</span>
      </div>
    )
  }

  return <>{children}</>
}
