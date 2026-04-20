import { expect, test } from '@playwright/test'
import { login, seedCredentials } from './helpers'

test.describe('admin flow', () => {
  test('manages catalog and opens dashboard', async ({ page }) => {
    const suffix = Date.now()
    const serviceCode = `SRV-E2E-${suffix}`
    const drugCode = `DRUG-E2E-${suffix}`

    await login(page, seedCredentials.admin)

    await page.getByRole('link', { name: 'Services' }).click()
    await expect(page.getByRole('heading', { name: 'Services' })).toBeVisible()
    await page.getByRole('button', { name: 'New service' }).click()
    const serviceModal = page.getByRole('dialog', { name: 'Create service' })

    await serviceModal.getByLabel('Service code').fill(serviceCode)
    await serviceModal.getByLabel('Service name').fill(`Service ${suffix}`)
    await serviceModal.getByLabel('Service type').selectOption('TEST')
    await serviceModal.getByRole('button', { name: 'Save service' }).click()
    await expect(page.locator('tbody tr', { hasText: serviceCode })).toBeVisible()

    await page.getByRole('link', { name: 'Drugs' }).click()
    await expect(page.getByRole('heading', { name: 'Drugs' })).toBeVisible()
    await page.getByRole('button', { name: 'New drug' }).click()
    const drugModal = page.getByRole('dialog', { name: 'Create drug' })

    await drugModal.getByLabel('Drug code').fill(drugCode)
    await drugModal.getByLabel('Drug name').fill(`Drug ${suffix}`)
    await drugModal.getByLabel('Manufacturer').fill('E2E Pharma')
    await drugModal.getByLabel('Expiry date').fill('2028-12-31')
    await drugModal.getByLabel('Unit').fill('TABLET')
    await drugModal.getByLabel('Price').fill('3001')
    await drugModal.getByLabel('Stock quantity').fill('150')
    await drugModal.getByRole('button', { name: 'Save drug' }).click()
    await expect(page.locator('tbody tr', { hasText: drugCode })).toBeVisible()

    await page.getByRole('link', { name: 'Dashboard' }).click()
    await expect(page.getByRole('heading', { name: 'Dashboard' })).toBeVisible()
    await expect(page.getByText('Total appointments today')).toBeVisible()
  })
})
