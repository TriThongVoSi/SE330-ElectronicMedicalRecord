import React from 'react'
import { Navigate, Outlet } from 'react-router-dom'
import { useAuthStore, authSelectors } from '../features/auth/auth-store'
import { resolveRoleHomeRoute } from '../features/auth/route-utils'
import { appRoutes } from './routes'

export const RedirectIfAuthenticated: React.FC = () => {
  const isAuthenticated = useAuthStore(authSelectors.isAuthenticated)
  const user = useAuthStore((state) => state.user)

  if (isAuthenticated && user) {
    if (user.mustChangePassword) {
      return <Navigate to={appRoutes.firstLoginPassword} replace />
    }
    return <Navigate to={resolveRoleHomeRoute(user.role)} replace />
  }

  return <Outlet />
}
