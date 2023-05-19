import path from 'path'
import fs from 'fs'
import { test as baseTest, expect, Page } from '@playwright/test'
import { AdminPage, UserPage } from './auth-fixtures'
import config from '../playwright.config'
import { USERS, SELECTOR } from '../tests/config'
export { expect } from '@playwright/test'

export const test = baseTest.extend({
    admin: async ({ browser }, use) => {
        await use(await AdminPage.create(browser, 'admin'))
    },
    user: async ({ browser }, use) => {
        await use(await UserPage.create(browser, 'maintainer'))
    },
})

test.beforeAll(async ({ browser }, testInfo) => {
    await Promise.all(
        USERS.map(async (user, index) => {
            // testInfo.project.outputDir
            const fileName = path.join('test-storage', 'storage-' + user.role + '.json')
            if (!fs.existsSync(fileName)) {
                const context = await browser.newContext({
                    storageState: undefined,
                })
                const page = await context.newPage()
                await page.goto(config.use?.baseURL as string)
                await expect(page).toHaveTitle(/Starwhale Console/)
                await page.locator(SELECTOR.loginName).fill(user.username)
                await page.locator(SELECTOR.loginPassword).fill(user.password)
                await page.getByRole('button', { name: 'Log in' }).click()
                await page.context().storageState({ path: fileName })
                await page.close()
                await context.close()

                if (process.env.LITE === 'true') {
                    await page.route(/./, async (route, request) => {
                        const type = await request.resourceType()
                        if (type === 'image' || type === 'font') {
                            route.abort()
                        } else {
                            route.continue()
                        }
                    })
                }
            }
        })
    )
})
