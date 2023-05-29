import { expect, Locator, Page } from '@playwright/test'
import { test } from '../setup'
import { CONST, ROUTES, SELECTOR } from './config'
import { getLastestRowID, getTableDisplayRow, selectOption, takeScreenshot, wait } from './utils'
let page: Page

test.beforeAll(async ({ guest }) => {
    page = guest.page
    await page.goto('/', {
        waitUntil: 'networkidle',
    })
    await expect(page).toHaveTitle(/Starwhale Console/)
})

test.describe('Login', () => {
    test('default route should be projects', async ({}) => {
        await page.waitForURL(/\/projects/, { timeout: 20000 })
        await expect(page).toHaveURL(/\/projects/)
    })

    test('header show not have admin settings', async ({}) => {
        await page.hover(SELECTOR.userWrapper)
        await expect(page.locator(SELECTOR.userAvtarName)).toBeVisible()
        await expect(page.locator(SELECTOR.userAvtarName)).toHaveText(CONST.user.userName)
        await expect(page.locator(SELECTOR.authAdminSetting)).not.toBeVisible()
        await page.mouse.move(0, 0)
    })
})

test.describe('Project list', () => {
    test('should project form modal act show,submit,close', async ({}) => {
        const el = await page.waitForSelector(SELECTOR.projectCreate)
        await el.click()
        await page.waitForSelector(SELECTOR.projectForm)
        await page.locator(SELECTOR.projectName).fill(CONST.user.projectName)
        await page.locator(SELECTOR.projectDescription).fill(CONST.user.projectDescription)
        await page.locator(SELECTOR.projectPrivacy).check()
        await expect(page.locator(SELECTOR.projectName)).not.toBeEmpty()
        await expect(page.locator(SELECTOR.projectDescription)).not.toBeEmpty()
        await expect(page.locator(SELECTOR.projectPrivacy)).toBeChecked()
        await page.locator(SELECTOR.projectSubmit).click()
    })

    test('check project list and delete project just created', async () => {
        await page.waitForSelector(SELECTOR.projectCard)
        const cardLength = await page.$$eval(SELECTOR.projectCard, (el) => el.length)
        expect(cardLength).toBeGreaterThan(0)

        const p = page.locator(
            SELECTOR.projectCard + `:has(:has-text("${CONST.user.userName}/${CONST.user.projectName}"))`
        )
        await p.hover()
        await expect(p.locator(SELECTOR.projectCardActions)).toBeVisible()
        await p.locator(SELECTOR.projectCardActionDelete).click()
        await page.waitForSelector(SELECTOR.projectCardDeleteConfirm)
        await page.locator(SELECTOR.projectModelInput).fill(CONST.user.projectName)
        await page.locator(SELECTOR.projectCardDeleteConfirm).click()
        await expect(p).not.toBeVisible()
    })

    test('should project navigate to evaluations when click project name', async () => {
        await page.locator(SELECTOR.projectCardLink).click()
        await expect(page).toHaveURL(ROUTES.evaluations)
    })
})

// test.describe('Auth', () => {
//     test('none admin should have no create button', async () => {
//         await expect(page.locator(SELECTOR.listCreate)).toBeHidden()
//     })
// })
