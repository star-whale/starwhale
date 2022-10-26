import { expect, Page } from '@playwright/test'
import { test } from '../setup'
import { CONST, ROUTES, SELECTOR } from './config'
import { takeScreenshot, wait } from './utils'

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

test.describe('Login', () => {
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
        // await page.mouse.move(0, 0)
        await page.locator(SELECTOR.authAdminSetting).first().click()
        await expect(page).toHaveURL(ROUTES.adminUsers)
    })
})

test.describe('Admin', () => {
    test.afterAll(async () => {
        await takeScreenshot({ testcase: page, route: page.url() })
    })

    test.describe('Users', () => {
        test.beforeAll(async () => {
            if (!page.url().includes(ROUTES.adminUsers)) await page.goto(ROUTES.adminUsers)
        })

        test('should show add button & user list', async ({}) => {
            await expect(page.getByText(/Add User/)).toBeVisible()
            await expect(await page.locator('tr').count()).toBeGreaterThan(0)
        })

        test('should new user be success added & showing in list', async () => {
            test.skip(!!(await page.$$(`tr:has-text("${CONST.newUserName}")`)), 'user exists, skip')

            await page.getByText(/Add User/).click()
            await page.locator(SELECTOR.formItem('Username')).locator('input').fill(CONST.newUserName)
            await page.locator(SELECTOR.formItem('New Password')).first().locator('input').fill(CONST.newUserPassword)
            await page.locator(SELECTOR.formItem('Confirm New Password')).locator('input').fill(CONST.newUserPassword)
            await page.locator(SELECTOR.userSubmit).click()
            await expect(page.locator(SELECTOR.userForm)).toBeHidden()
            await expect(page.locator('td').getByText(CONST.newUserName)).toBeDefined()
        })

        test('disable new user, user should login fail', async ({ request }) => {
            page.locator(`tr:has-text("${CONST.newUserName}")`).getByRole('button', { name: 'Disable' }).click()
            await page.waitForSelector(SELECTOR.userDisableConfirm)
            await page.locator(SELECTOR.userDisableConfirm).click()

            const resp = await request.post('/api/v1/login', {
                multipart: {
                    userName: CONST.newUserName,
                    userPwd: CONST.newUserPassword,
                    agreement: true,
                },
            })
            expect(resp.ok()).toBeFalsy()
        })

        test('enable new user, user should login success', async ({ request }) => {
            page.locator(`tr:has-text("${CONST.newUserName}")`).getByRole('button', { name: 'Enable' }).click()
            await page.waitForSelector(SELECTOR.userDisableConfirm)
            await page.locator(SELECTOR.userDisableConfirm).click()

            const resp = await request.post('/api/v1/login', {
                multipart: {
                    userName: CONST.newUserName,
                    userPwd: CONST.newUserPassword,
                    agreement: true,
                },
            })
            expect(resp.ok()).toBeTruthy()
        })
    })

    test.describe('Settings', () => {
        test.beforeAll(async ({ request }) => {
            const token = await page.evaluate(() => localStorage.getItem('token'))
            const resp = await request.post('/api/v1/system/setting', {
                data: '---\ndockerSetting:\n registry: "abcd.com"\n',
                headers: {
                    'Content-Type': 'text/plain',
                    'Authorization': token as string,
                },
            })
            expect(resp.ok()).toBeTruthy()
            if (!page.url().includes(ROUTES.adminSettings)) await page.goto(ROUTES.adminSettings)
        })

        test('should show system settings', async ({}) => {
            await page.waitForSelector('.monaco-editor')
            expect(page.locator('.view-lines')).toHaveText('---dockerSetting:  registry: "abcd.com"')
        })

        test('should setting be successful updated', async ({}) => {
            await page.waitForSelector('.monaco-editor')
            await page.getByRole('button', { name: 'Update' }).click()
            await expect(page.getByText('Success')).toBeVisible()
        })
    })
})
