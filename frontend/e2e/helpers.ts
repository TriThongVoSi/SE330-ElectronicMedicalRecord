import { expect, type Locator, type Page } from '@playwright/test'

type LoginCredentials = {
  identifier: string
  password: string
}

const signInHeading = () => ({ name: 'Sign in' as const })

export const seedCredentials = {
  admin: {
    identifier: 'admin.local',
    password: 'admin123',
  },
  doctor: {
    identifier: 'doctor.local',
    password: 'doctor123',
  },
} satisfies Record<string, LoginCredentials>

export const gotoLogin = async (page: Page): Promise<void> => {
  await page.goto('/login')
  await expect(page.getByRole('heading', signInHeading())).toBeVisible()
}

export const login = async (page: Page, credentials: LoginCredentials): Promise<void> => {
  await gotoLogin(page)
  await page.getByLabel('Username or email').fill(credentials.identifier)
  await page.getByLabel('Password').fill(credentials.password)
  await page.getByRole('button', { name: 'Sign in' }).click()
  await expect(page).toHaveURL(/\/dashboard$/)
  await expect(page.getByRole('heading', { name: 'Dashboard' })).toBeVisible()
}

export const logout = async (page: Page): Promise<void> => {
  await page.getByRole('button', { name: 'Logout' }).click()
  await expect(page).toHaveURL(/\/login$/)
  await expect(page.getByRole('heading', signInHeading())).toBeVisible()
}

export const fillDatetimeLocal = async (
  scope: Page | Locator,
  label: string,
  value: string
): Promise<void> => {
  await scope.getByLabel(label).fill(value)
}
