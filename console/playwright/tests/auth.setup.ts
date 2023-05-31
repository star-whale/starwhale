import path from 'path'
import fs from 'fs'
import { test as setup, expect, Page } from '@playwright/test'
import config from '../playwright.config'
import { USERS, SELECTOR } from './config'
import { takeScreenshot, wait } from './utils'

USERS.map((user) => {
    // testInfo.project.outputDir
    const fileName = path.join('test-storage', 'storage-' + user.role + '.json')

    if (fs.existsSync(fileName) && process.env.CLEAN_AUTH !== 'true') return

    setup(`authenticate as ${user.role}`, async ({ page }) => {
        await page.goto(config.use?.baseURL as string)
        await expect(page).toHaveTitle(/Starwhale Console/)
        await page.locator(SELECTOR.loginName).fill(user.username)
        await page.locator(SELECTOR.loginPassword).fill(user.password)
        await page.getByRole('button', { name: 'Log in' }).click()
        await page.waitForURL(/\/projects/)
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

setup.afterAll(async ({ page }) => {
    // if (process.env.CLOSE_SAVE_VIDEO === 'true') await page.video()?.saveAs(`test-video/admin.webm`)
    if (process.env.CLOSE_AFTER_TEST === 'true') {
        await wait(5000)
        await page.context().close()
    }
})
