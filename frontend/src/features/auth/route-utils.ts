import type { UserRole } from '../../core/types/common'
import { appRoutes } from '../../app/routes'

export const resolveRoleHomeRoute = (role: UserRole): string => {
  if (role === 'PATIENT') {
    return appRoutes.patientDashboard
  }
  return appRoutes.dashboard
}
