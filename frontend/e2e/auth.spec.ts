import { expect, test } from '@playwright/test'
import { gotoLogin, login, logout, seedCredentials } from './helpers'

test.describe('auth and security flows', () => {
  test('redirects to login when protected route is opened without session', async ({ page }) => {
    await page.goto('/patients')
    await expect(page).toHaveURL(/\/login$/)
    await expect(page.getByRole('heading', { name: 'Sign in' })).toBeVisible()
  })

  test('shows error for invalid credentials', async ({ page }) => {
    await gotoLogin(page)
    await page.getByLabel('Username or email').fill('invalid.user')
    await page.getByLabel('Password').fill('invalid-pass')
    await page.getByRole('button', { name: 'Sign in' }).click()

    await expect(page.getByText('Invalid username or password.')).toBeVisible()
    await expect(page).toHaveURL(/\/login$/)
  })

  test('keeps public sign-up route unavailable', async ({ page }) => {
    await page.goto('/signup')
    await expect(page.getByRole('heading').first()).toContainText(/Page not found|Sign in/)
  })

  test('blocks route when current role is not allowed', async ({ page }) => {
    await login(page, seedCredentials.doctor)
    await expect
      .poll(async () => {
        return page.evaluate(() => {
          const raw = localStorage.getItem('EMR-auth-session')
          if (!raw) {
            return null
          }
          const parsed = JSON.parse(raw) as { state?: { accessToken?: string | null } }
          return parsed.state?.accessToken ?? null
        })
      })
      .not.toBeNull()

    await page.goto('/services')

    await expect(page).toHaveURL(/\/forbidden$/)
    await expect(page.getByRole('heading', { name: 'Access denied' })).toBeVisible()
  })

  test('clears invalid persisted session and sends user back to login', async ({ page }) => {
    await page.addInitScript(() => {
      localStorage.setItem(
        'EMR-auth-session',
        JSON.stringify({
          state: {
            accessToken: 'invalid-access-token',
            refreshToken: 'invalid-refresh-token',
            user: {
              id: 'seed-user',
              username: 'seed.user',
              fullName: 'Seed User',
              email: 'seed.user@EMR.dev',
              role: 'ADMIN',
            },
          },
          version: 0,
        })
      )
    })

    await page.goto('/dashboard')
    await expect(page).toHaveURL(/\/login$/)
    await expect(page.getByRole('heading', { name: 'Sign in' })).toBeVisible()
  })

  test('supports login and logout success path', async ({ page }) => {
    await login(page, seedCredentials.admin)
    await logout(page)
  })

  test('opens forgot-password flow entry page', async ({ page }) => {
    await page.goto('/forgot-password')
    await expect(page.getByRole('heading', { name: 'Reset password' })).toBeVisible()
    await expect(page.getByRole('link', { name: 'Back to sign in' })).toBeVisible()
  })
})
