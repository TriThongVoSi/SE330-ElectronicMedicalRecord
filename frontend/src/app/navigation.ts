import type { UserRole } from '../core/types/common'
import { appRoutes } from './routes'

type NavItem = {
  label: string
  to: string
  roles: UserRole[]
}

export const navigationItems: NavItem[] = [
  {
    label: 'Dashboard',
    to: appRoutes.dashboard,
    roles: ['ADMIN', 'DOCTOR'],
  },
  {
    label: 'Patients',
    to: appRoutes.patients,
    roles: ['ADMIN', 'DOCTOR'],
  },
  {
    label: 'Doctors',
    to: appRoutes.doctors,
    roles: ['ADMIN', 'DOCTOR'],
  },
  {
    label: 'Appointments',
    to: appRoutes.appointments,
    roles: ['ADMIN', 'DOCTOR'],
  },
  {
    label: 'Services',
    to: appRoutes.services,
    roles: ['ADMIN'],
  },
  {
    label: 'Drugs',
    to: appRoutes.drugs,
    roles: ['ADMIN', 'DOCTOR'],
  },
  {
    label: 'Prescriptions',
    to: appRoutes.prescriptions,
    roles: ['ADMIN', 'DOCTOR'],
  },
  {
    label: 'My Dashboard',
    to: appRoutes.patientDashboard,
    roles: ['PATIENT'],
  },
  {
    label: 'Medical History',
    to: appRoutes.patientMedicalHistory,
    roles: ['PATIENT'],
  },
  {
    label: 'My Appointments',
    to: appRoutes.patientAppointments,
    roles: ['PATIENT'],
  },
  {
    label: 'My Profile',
    to: appRoutes.patientProfile,
    roles: ['PATIENT'],
  },
]

export const canAccessRoute = (role: UserRole | undefined, allowedRoles: UserRole[]): boolean => {
  if (!role) {
    return false
  }
  return allowedRoles.includes(role)
}
