import { expect, Locator, Page } from '@playwright/test'
import path from 'path'
import * as fse from 'fs-extra'
import { CONFIG } from './config'

export async function selectOption(page: Page, selector: string, text: string | RegExp) {
    const ops = page.locator(selector)
    await ops.locator('div').first().click()
    await page.getByRole('option').getByText(text).click()
    await expect(ops.getByText(text)).toBeTruthy()
}

export async function getTableDisplayRow(page: Locator | Page) {
    return await page.locator('.table-inner >> .table-cell').count()
}

export async function getLastestRowID(page: Locator | Page) {
    const rowCount = await page.locator('.column-cell').getByRole('link').first().textContent()
    return Number(rowCount)
}

export async function hasHeader(page: Locator | Page) {
    return await page.locator('.table-inner >> nth=0 >> .table-row').count()
}

export async function wait(ms: number) {
    return new Promise((resolve) => {
        setTimeout(resolve, ms)
    })
}

export async function takeScreenshot({ testcase, route }: any) {
    const url = new URL(route)
    const screenshotPath = path.resolve(CONFIG.screenshotDir, `./${(url.pathname as any).replaceAll('/', '-')}.png`)
    await fse.ensureDir(path.dirname(screenshotPath))
    // const explicitScreenshotTarget = await page.$('[data-testid="screenshot-target"]');
    const screenshotTarget = testcase
    await screenshotTarget.screenshot({ path: screenshotPath, type: 'png', fullPage: true })
}
