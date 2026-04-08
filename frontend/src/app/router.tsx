import React from 'react'
import { Navigate, Route, Routes } from 'react-router-dom'
import { AppLayout } from './AppLayout'
import { RequireAuth } from './RequireAuth'
import { RedirectIfAuthenticated } from './RedirectIfAuthenticated'
import { appRoutes } from './routes'
import { LoginPage } from '../features/auth/LoginPage'
import { ForgotPasswordPage } from '../features/auth/ForgotPasswordPage'
import { FirstLoginPasswordPage } from '../features/auth/FirstLoginPasswordPage'
import { resolveRoleHomeRoute } from '../features/auth/route-utils'
import { useAuthStore } from '../features/auth/auth-store'
import { DashboardPage } from '../features/dashboard/DashboardPage'
import { PatientsPage } from '../features/patients/PatientsPage'
import { DoctorsPage } from '../features/staff/DoctorsPage'
import { AppointmentsPage } from '../features/appointments/AppointmentsPage'
import { ServicesPage } from '../features/services/ServicesPage'
import { DrugsPage } from '../features/drugs/DrugsPage'
import { PrescriptionsPage } from '../features/prescriptions/PrescriptionsPage'
import { PatientDashboardPage } from '../features/patient-portal/PatientDashboardPage'
import { PatientMedicalHistoryPage } from '../features/patient-portal/PatientMedicalHistoryPage'
import { PatientAppointmentsPage } from '../features/patient-portal/PatientAppointmentsPage'
import { PatientProfilePage } from '../features/patient-portal/PatientProfilePage'
import { ForbiddenPage } from '../components/ForbiddenPage'
import { NotFoundPage } from '../components/NotFoundPage'

const RoleHomeRedirect: React.FC = () => {
  const user = useAuthStore((state) => state.user)

  if (!user) {
    return <Navigate to={appRoutes.login} replace />
  }

  return <Navigate to={resolveRoleHomeRoute(user.role)} replace />
}

export const AppRouter: React.FC = () => {
  return (
    <Routes>
      <Route element={<RedirectIfAuthenticated />}>
        <Route path={appRoutes.login} element={<LoginPage />} />
        <Route path={appRoutes.forgotPassword} element={<ForgotPasswordPage />} />
      </Route>

      <Route element={<RequireAuth />}>
        <Route path={appRoutes.firstLoginPassword} element={<FirstLoginPasswordPage />} />

        <Route element={<AppLayout />}>
          <Route index element={<RoleHomeRedirect />} />

          <Route element={<RequireAuth roles={['ADMIN', 'DOCTOR']} />}>
            <Route path={appRoutes.dashboard} element={<DashboardPage />} />
            <Route path={appRoutes.patients} element={<PatientsPage />} />
            <Route path={appRoutes.doctors} element={<DoctorsPage />} />
            <Route path={appRoutes.appointments} element={<AppointmentsPage />} />
            <Route path={appRoutes.drugs} element={<DrugsPage />} />
            <Route path={appRoutes.prescriptions} element={<PrescriptionsPage />} />
          </Route>

          <Route element={<RequireAuth roles={['ADMIN']} />}>
            <Route path={appRoutes.services} element={<ServicesPage />} />
          </Route>

          <Route element={<RequireAuth roles={['PATIENT']} />}>
            <Route path={appRoutes.patientDashboard} element={<PatientDashboardPage />} />
            <Route path={appRoutes.patientMedicalHistory} element={<PatientMedicalHistoryPage />} />
            <Route path={appRoutes.patientAppointments} element={<PatientAppointmentsPage />} />
            <Route path={appRoutes.patientProfile} element={<PatientProfilePage />} />
          </Route>
        </Route>
      </Route>

      <Route path={appRoutes.forbidden} element={<ForbiddenPage />} />
      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  )
}
