import { expect, Page } from '@playwright/test'
import { test } from '../setup'
import { API, CONST, ROUTES, SELECTOR } from './config'
import { getLastestRowID, selectOption, selectTreeOption, takeScreenshot, wait } from './utils'

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

test.describe('Project with admin setttings', () => {
    test('logined, check homepage & token', async ({}) => {
        await page.waitForURL(/\/projects/)
        await expect(page).toHaveURL(/\/projects/)
        expect(await page.evaluate(() => localStorage.getItem('token'))).not.toBe('')
    })

    test('header show have admin settings', async ({}) => {
        await page.hover(SELECTOR.userWrapper)
        await expect(page.locator(SELECTOR.authAdminSetting).first()).toBeVisible()
        await page.locator(SELECTOR.authAdminSetting).first().click()
        await page.mouse.move(0, 0)
        await expect(page).toHaveURL(ROUTES.adminUsers)
    })

    test('project set to public', async ({ request }) => {
        const token = await page.evaluate(() => localStorage.getItem('token'))
        await request.put(API.project, {
            data: {
                description: '',
                ownerId: '1',
                privacy: 'PUBLIC',
                projectName: 'starwhale',
            },
            headers: {
                Authorization: token as string,
            },
        })
        // expect(resp.ok()).toBeTruthy()
        const resp = await request.get(API.project, {
            headers: {
                Authorization: token as string,
            },
        })
        const obj = await resp.json()
        expect(obj.data.privacy).toBe('PUBLIC')
    })
})

test.describe('Admin Users', () => {
    test.beforeAll(async () => {
        if (!page.url().includes(ROUTES.adminUsers)) await page.goto(ROUTES.adminUsers)
    })

    test('should show add button & user list', async ({}) => {
        await expect(page.getByText(/Add User/)).toBeVisible()
        await expect(await page.locator('tr').count()).toBeGreaterThan(0)
    })

    test('should new user be success added & showing in list', async () => {
        const user = await page.$$(`tr:has-text("${CONST.newUserName}")`)
        test.skip(user.length > 0, 'user exists, skip')

        await page.getByText(/Add User/).click()
        await page.locator(SELECTOR.formItem('Username')).locator('input').fill(CONST.newUserName)
        await page.locator(SELECTOR.formItem('New Password')).first().locator('input').fill(CONST.newUserPassword)
        await page.locator(SELECTOR.formItem('Confirm New Password')).locator('input').fill(CONST.newUserPassword)
        await page.locator(SELECTOR.userSubmit).click()
        await expect(page.locator(SELECTOR.userForm)).toBeHidden()
        await expect(page.locator('td').getByText(CONST.newUserName)).toBeDefined()
    })

    test('disable new user, user should login fail', async ({ request }) => {
        const user = await page.$$(`tr:has-text("${CONST.newUserName}") button:has-text("Enable")`)
        test.skip(user.length > 0, 'user disabled, skip')

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
        page.waitForResponse((response) => response.status() === 200)
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

test.describe('Admin Settings', () => {
    test.beforeAll(async ({ request }) => {
        if (!page.url().includes(ROUTES.adminSettings)) await page.goto(ROUTES.adminSettings)
    })
    test('should show system settings', async ({}) => {
        await page.waitForSelector('.monaco-editor', {
            timeout: 50000,
        })
        expect(page.locator('.view-lines')).toHaveText(/resourcePoolSetting/)
    })
})
