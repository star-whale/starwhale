import { expect, Locator, Page } from '@playwright/test'

export async function selectOption(page: Page, selector: string, text: string) {
    const ops = page.locator(selector)
    await ops.locator('div').first().click()
    await page.getByRole('option').getByText(text).click()
    await expect(ops.getByText(text)).toBeTruthy()
}

export async function getTableDisplayRow(page: Locator | Page) {
    return await page.locator('.table-inner .table-row').count()
}

export async function hasHeader(page: Locator | Page) {
    return await page.locator('.table-inner .table-row').count()
}

export async function wait(ms: number) {
    return new Promise((resolve) => {
        setTimeout(resolve, ms)
    })
}
