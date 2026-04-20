import React from 'react'
import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { roleLabelMap } from '../core/auth/roles'
import { useI18n } from '../core/i18n/use-i18n'
import type { UserRole } from '../core/types/common'
import { useNotify } from '../core/utils/notify'
import { performLogout } from '../features/auth/api'
import { useAuthStore } from '../features/auth/auth-store'
import { appRoutes } from './routes'
import { navigationItems } from './navigation'

const navLabelKeyByRoute: Record<string, string> = {
  [appRoutes.dashboard]: 'navigation.dashboard',
  [appRoutes.patients]: 'navigation.patients',
  [appRoutes.doctors]: 'navigation.doctors',
  [appRoutes.appointments]: 'navigation.appointments',
  [appRoutes.services]: 'navigation.services',
  [appRoutes.drugs]: 'navigation.drugs',
  [appRoutes.prescriptions]: 'navigation.prescriptions',
  [appRoutes.patientDashboard]: 'navigation.patientDashboard',
  [appRoutes.patientMedicalHistory]: 'navigation.patientMedicalHistory',
  [appRoutes.patientAppointments]: 'navigation.patientAppointments',
  [appRoutes.patientProfile]: 'navigation.patientProfile',
}

const roleLabelKeyByRole: Record<UserRole, string> = {
  ADMIN: 'roles.admin',
  DOCTOR: 'roles.doctor',
  PATIENT: 'roles.patient',
}

export const AppLayout: React.FC = () => {
  const user = useAuthStore((state) => state.user)
  const clearSession = useAuthStore((state) => state.clearSession)
  const navigate = useNavigate()
  const notify = useNotify()
  const { language, setLanguage, t } = useI18n()

  const allowedItems = navigationItems.filter((item) =>
    user ? item.roles.includes(user.role) : false
  )

  const roleLabel = user
    ? t(roleLabelKeyByRole[user.role], roleLabelMap[user.role])
    : t('appLayout.guest', 'Guest')

  const handleLogout = async (): Promise<void> => {
    try {
      await performLogout()
    } catch {
      // Ignore network issues on logout and still clear local session.
    }

    clearSession()
    notify.info(
      t('appLayout.signedOutTitle', 'Signed out'),
      t('appLayout.signedOutDescription', 'Your session has been cleared.')
    )
    navigate(appRoutes.login, { replace: true })
  }

  return (
    <div className="shell-layout">
      <aside className="shell-sidebar">
        <div className="brand-block">
          <p className="brand-tag">{t('appLayout.brandTag', 'Electronic Medical Record')}</p>
          <h2>SE330-G4-EMR</h2>
        </div>
        <nav className="nav-menu" aria-label={t('appLayout.mainNavigation', 'Main navigation')}>
          {allowedItems.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              className={({ isActive }) => (isActive ? 'nav-link nav-link-active' : 'nav-link')}
            >
              {t(navLabelKeyByRoute[item.to], item.label)}
            </NavLink>
          ))}
        </nav>
      </aside>

      <div className="shell-main">
        <header className="shell-topbar">
          <div>
            <strong>{user?.fullName ?? t('appLayout.unknownUser', 'Unknown user')}</strong>
            <span>{roleLabel}</span>
          </div>
          <div className="topbar-actions">
            <div
              className="language-toggle"
              role="group"
              aria-label={t('appLayout.language', 'Language')}
            >
              <button
                type="button"
                className={`language-toggle-btn ${language === 'en' ? 'language-toggle-btn-active' : ''}`}
                onClick={() => setLanguage('en')}
                aria-pressed={language === 'en'}
              >
                {t('appLayout.languageShort.en', 'EN')}
              </button>
              <button
                type="button"
                className={`language-toggle-btn ${language === 'vi' ? 'language-toggle-btn-active' : ''}`}
                onClick={() => setLanguage('vi')}
                aria-pressed={language === 'vi'}
              >
                {t('appLayout.languageShort.vi', 'VI')}
              </button>
            </div>
            <button type="button" className="btn btn-secondary" onClick={handleLogout}>
              {t('appLayout.logout', 'Logout')}
            </button>
          </div>
        </header>
        <main className="shell-content">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
