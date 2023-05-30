import path from 'path'
import fs from 'fs'
import { test as setup, expect, Page } from '@playwright/test'
import config from '../playwright.config'
import { USERS, SELECTOR } from './config'
import { takeScreenshot, wait } from './utils'

setup(`authenticate as ${USERS[0].role}`, async ({ page }) => {
    const fileName = path.join('test-storage', 'storage-' + USERS[0].role + '.json')

    if (fs.existsSync(fileName) && process.env.CLEAN_AUTH !== 'true') return

    await page.goto(config.use?.baseURL as string)
    await expect(page).toHaveTitle(/Starwhale Console/)
    await page.locator(SELECTOR.loginName).fill(USERS[0].username)
    await page.locator(SELECTOR.loginPassword).fill(USERS[0].password)
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

setup(`authenticate as ${USERS[1].role}`, async ({ page }) => {
    const fileName = path.join('test-storage', 'storage-' + USERS[1].role + '.json')

    if (fs.existsSync(fileName) && process.env.CLEAN_AUTH !== 'true') return

    await page.goto(config.use?.baseURL as string)
    await expect(page).toHaveTitle(/Starwhale Console/)
    await page.locator(SELECTOR.loginName).fill(USERS[1].username)
    await page.locator(SELECTOR.loginPassword).fill(USERS[1].password)
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

setup.afterAll(async ({ page }) => {
    // if (process.env.CLOSE_SAVE_VIDEO === 'true') await page.video()?.saveAs(`test-video/admin.webm`)
    if (process.env.CLOSE_AFTER_TEST === 'true') {
        await wait(5000)
        await page.context().close()
    }
})
