import { expect, Page } from '@playwright/test'
import { test } from '../setup/auth'
import { SELECTOR } from './config'
// used as one page
let page: Page

test.beforeAll(async ({ admin }) => {
    page = admin.page
    await page.goto('/')
    await expect(page).toHaveTitle(/Starwhale Console/)
})

test.afterAll(async () => {
    // await page.close()
    // await page.context().close()
})

test.describe('check login status', () => {
    test('logined, check homepage & token', async ({}) => {
        await expect(page).toHaveURL(/\/projects/)
        expect(await page.evaluate(() => localStorage.getItem('token'))).not.toBe('')
    })

    test('header show have admin settings', async ({ admin }) => {
        await page.hover(SELECTOR.userWrapper)
        await expect(page.locator(SELECTOR.authAdminSetting)).toBeVisible()
        await page.mouse.move(0, 0)
    })
})

test.describe('check project', () => {
    test('logined, check project', async ({}) => {
        const el = await page.waitForSelector(SELECTOR.projectCreate)
        await el.click()
    })
})
