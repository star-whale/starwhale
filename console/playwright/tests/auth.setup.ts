import path from 'path'
import fs from 'fs'
import { test as setup, expect, Page } from '@playwright/test'
import config from '../playwright.config'
import { USERS, SELECTOR } from './config'

USERS.map(async (user) => {
    // testInfo.project.outputDir
    const fileName = path.join('test-storage', 'storage-' + user.role + '.json')

    if (fs.existsSync(fileName)) return

    setup(`authenticate as ${user.role}`, async ({ page }) => {
        console.log('-----------------------')
        await page.goto(config.use?.baseURL as string)
        await expect(page).toHaveTitle(/Starwhale Console/)
        await page.locator(SELECTOR.loginName).fill(user.username)
        await page.locator(SELECTOR.loginPassword).fill(user.password)
        await page.getByRole('button', { name: 'Log in' }).click()
        await page.context().storageState({ path: fileName })
        await page.close()

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
    })
})
