import path from 'path'
import fs from 'fs'
import { test as setup, expect, Page } from '@playwright/test'
import config from '../playwright.config'
import { USERS, SELECTOR, API, CONST } from './config'
import { takeScreenshot, wait } from './utils'

const admin = USERS[0]
const guest = USERS[1]

setupUser(admin)
setupUser(guest)

function setupUser(user: typeof admin) {
    // testInfo.project.outputDir
    const fileName = path.join('test-storage', 'storage-' + user.role + '.json')

    setup(`authenticate as ${user.role}`, async ({ page, request }) => {
        await page.goto(config.use?.baseURL as string)
        await expect(page).toHaveTitle(/Starwhale Console/)
        await page.locator(SELECTOR.loginName).fill(user.username)
        await page.locator(SELECTOR.loginPassword).fill(user.password)
        const responsePromise = page.waitForResponse((response) => response.status() === 200)
        await page.getByRole('button', { name: 'Log in' }).click()
        await responsePromise
        await page.context().storageState({ path: fileName })
        // create user for admin
        if (user.role === 'admin') {
            const token = await page.evaluate(() => localStorage.getItem('token'))
            await request.post(API.user, {
                data: {
                    userName: guest.username,
                    userPwd: guest.password,
                },
                headers: {
                    Authorization: token as string,
                },
            })
        }
        //
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
}

setup.afterAll(async ({ page }) => {
    // if (process.env.CLOSE_SAVE_VIDEO === 'true') await page.video()?.saveAs(`test-video/admin.webm`)
    if (process.env.CLOSE_AFTER_TEST === 'true') {
        await wait(5000)
        await page.context().close()
    }
})
