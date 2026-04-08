import type { UserRole } from '../types/common'

export const ALL_ROLES: UserRole[] = ['ADMIN', 'DOCTOR', 'PATIENT']

export const roleLabelMap: Record<UserRole, string> = {
  ADMIN: 'Admin',
  DOCTOR: 'Doctor',
  PATIENT: 'Patient',
}
