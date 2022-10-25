import { expect, Page } from '@playwright/test'
import { test } from '../setup'
import { SELECTOR } from './config'
import { getLastestRowID, getTableDisplayRow, selectOption, takeScreenshot, wait } from './utils'
// used as one page
let page: Page

test.beforeAll(async ({ admin }) => {
    page = admin.page
    await wait(1000)
    await page.goto('/', {
        waitUntil: 'networkidle',
    })
    await expect(page).toHaveTitle(/Starwhale Console/)
})

test.afterAll(async ({}) => {
    await wait(10000)

    if (process.env.CLOSE_AFTER_TEST === 'true') {
        await page.context().close()
        if (process.env.CLOSE_SAVE_VIDEO === 'true') await page.video()?.saveAs(`test-video/admin.webm`)
    }
})

test.describe('check login status', () => {
    test.afterAll(async () => {
        await takeScreenshot({ testcase: page, route: page.url() })
    })

    test('logined, check homepage & token', async ({}) => {
        await expect(page).toHaveURL(/\/projects/)
        expect(await page.evaluate(() => localStorage.getItem('token'))).not.toBe('')
    })

    test('header show have admin settings', async ({}) => {
        await page.hover(SELECTOR.userWrapper)
        await expect(page.locator(SELECTOR.authAdminSetting).first()).toBeVisible()
        await page.mouse.move(0, 0)
    })
})

test.describe('check project', () => {
    test.afterAll(async () => {
        await takeScreenshot({ testcase: page, route: page.url() })
    })

    test('logined, check project', async ({}) => {
        const el = await page.waitForSelector(SELECTOR.projectCreate)
        await el.click()
    })
})
