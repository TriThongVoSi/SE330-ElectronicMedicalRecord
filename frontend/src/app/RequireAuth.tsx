import React from 'react'
import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuthStore, authSelectors } from '../features/auth/auth-store'
import type { UserRole } from '../core/types/common'
import { resolveRoleHomeRoute } from '../features/auth/route-utils'
import { appRoutes } from './routes'

type RequireAuthProps = {
  roles?: UserRole[]
}

export const RequireAuth: React.FC<RequireAuthProps> = ({ roles }) => {
  const location = useLocation()
  const isAuthenticated = useAuthStore(authSelectors.isAuthenticated)
  const user = useAuthStore((state) => state.user)

  if (!isAuthenticated || !user) {
    return (
      <Navigate
        to={appRoutes.login}
        replace
        state={{ from: `${location.pathname}${location.search}` }}
      />
    )
  }

  if (user.mustChangePassword && location.pathname !== appRoutes.firstLoginPassword) {
    return <Navigate to={appRoutes.firstLoginPassword} replace />
  }

  if (!user.mustChangePassword && location.pathname === appRoutes.firstLoginPassword) {
    return <Navigate to={resolveRoleHomeRoute(user.role)} replace />
  }

  if (roles && !roles.includes(user.role)) {
    return <Navigate to={appRoutes.forbidden} replace />
  }

  return <Outlet />
}
