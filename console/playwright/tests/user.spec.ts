import { expect, Locator, Page } from '@playwright/test'
import { test } from '../setup/auth'
import { CONST, ROUTES, SELECTOR } from './config'
import { getTableDisplayRow, selectOption, takeScreenshot, wait } from './utils'
let page: Page

test.beforeAll(async ({ user }) => {
    page = user.page
    await page.goto('/')
    await expect(page).toHaveTitle(/Starwhale Console/)
})

test.afterAll(async ({}) => {
    await wait(10000)

    if (process.env.CLOSE_AFTER_TEST === 'true') {
        await page.context().close()
        if (process.env.CLOSE_SAVE_VIDEO === 'true') await page.video()?.saveAs(`test-video/test.webm`)
    }
})

test.describe('Login', () => {
    test('default route should be projects', async ({}) => {
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
        await expect(page.locator(SELECTOR.projectForm)).toBeHidden()
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
        await page.locator(SELECTOR.projectCardDeleteConfirm).click()
        await expect(p).not.toBeVisible()
    })

    test('should project navigate to evaluations when click project name', async () => {
        await page.locator(SELECTOR.projectCardLink).click()
        await expect(page).toHaveURL(ROUTES.evaluations)
    })
})

test.describe('Evaluation', () => {
    test.beforeAll(async () => {
        await page.goto(ROUTES.evaluations)
        const p = page.locator(SELECTOR.table)
        await selectOption(page, '.table-config-view', 'All runs')
        await expect(await getTableDisplayRow(p)).toBeGreaterThan(0)
    })
    test.afterAll(async () => {
        await takeScreenshot({ testcase: page, route: page.url() })
    })

    test.describe('Auth', () => {
        test('none admin should have no create button', async () => {
            await expect(page.locator(SELECTOR.listCreate)).toBeHidden()
        })
    })

    test.describe('List', () => {
        test('should evaluation have toolbar/header/row', async () => {
            const p = page.locator(SELECTOR.table)

            await page.waitForSelector('role=button[name="Evaluation ID"] ')
            // await page.getByRole('button', { name: 'Evaluation ID' }).click()
            // await p.locator('role=button[name="Evaluation ID"] >> role=checkbox[checked=false]').click()
            // await p.locator('role=button[name="Evaluation ID"] >> label').click()
            // console.log(await p.locator('role=button[name="Evaluation ID"] >> input').isChecked())

            await expect(p.getByText('Select a view')).toBeTruthy()
            await expect(p.getByText('Filters')).toBeTruthy()
            await expect(p.getByText('Manage Columns')).toBeTruthy()

            await page.waitForSelector('.table-headers')
            await expect(p.locator('.table-headers')).toHaveCount(1)
            await expect(p.locator('.table-headers').getByText('Evaluation ID')).toBeTruthy()
        })
    })

    test.describe('Search', () => {
        test('should be 2 success status ', async () => {
            const p = page.locator(SELECTOR.table)
            await p.getByRole('textbox', { name: 'Search by text' }).fill('SUCCESS')
            await wait(1000)
            await expect(await getTableDisplayRow(p)).toEqual(2)
        })

        test('should be 5 rows ', async () => {
            const p = page.locator(SELECTOR.table)
            await p.getByRole('textbox', { name: 'Search by text' }).fill('')
            await wait(1000)
            await expect(await getTableDisplayRow(p)).toEqual(5)
        })
    })

    test.describe('Manage columns', () => {
        test('remove evaluation column & add accuracy column', async () => {
            const p = page.locator(SELECTOR.table)
            const drawer = page.locator('[data-baseweb="drawer"]')
            await p.getByRole('button', { name: /Manage Columns/ }).click()

            await drawer.getByTitle('Evaluation ID').locator('label').uncheck()
            await drawer.getByTitle('accuracy').locator('label').check()
            await drawer.getByRole('button', { name: /Apply/ }).click()
            await drawer.getByTitle('Close').click()

            await expect(p.locator('.table-headers').getByText('Evaluation ID')).toBeHidden()
            await expect(p.locator('.table-headers').getByText('accuracy')).toBeVisible()
        })
    })

    test.describe('Filter', () => {
        test('should be no rows when evaluation id = none', async () => {
            const p = page.locator(SELECTOR.table)
            await p.getByText('Filters').click()

            await page.waitForSelector(':has-text("Add filter")')
            await page.getByText('Add filter').click()

            await selectOption(page, '.filter-ops', 'Evaluation ID')
            await page.getByText('Apply').click()

            await expect(await getTableDisplayRow(p)).toEqual(0)
        })
    })

    test.describe('View', () => {
        test('should show rows when select all runs', async () => {
            const p = page.locator(SELECTOR.table)
            await selectOption(page, '.table-config-view', 'All runs')
            await expect(await getTableDisplayRow(p)).toBeGreaterThan(0)
        })
    })

    test.describe('Compare', () => {
        test('should show compare table when checked one row', async () => {
            const p = page.locator(SELECTOR.table)

            const isChecked = await p.locator(SELECTOR.headerFirst).locator('label input').isChecked()
            if (isChecked) await p.locator(SELECTOR.headerFirst).locator('label').click()
            await p.locator(SELECTOR.row1column1).locator('label').check()
            await p.locator(SELECTOR.row2column1).locator('label').check()
            await expect(page.getByText(/Compare Evaluations/)).toBeVisible()
            await wait(1000)
            await expect(page.locator(SELECTOR.headerFocused)).toHaveText(/mnist\-5/)
            await expect(await page.locator('.icon-rise').count()).toBeGreaterThan(0)
            await p.locator(SELECTOR.row1column1).locator('label').uncheck()
            await p.locator(SELECTOR.row2column1).locator('label').uncheck()
        })
    })
})

test.describe('Evaluation Create', () => {
    test.beforeAll(async () => {
        await page.goto(ROUTES.evaluations)
        await wait(500)
        await page.getByRole('button', { name: /Create$/ }).click()
        await expect(page).toHaveURL(ROUTES.evaluationNewJob)
    })
    test.afterAll(async () => {
        await takeScreenshot({ testcase: page, route: page.url() })
    })

    test.describe('Auth', () => {
        test('none admin should have no create button', async () => {
            await expect(page.locator(SELECTOR.listCreate)).toBeHidden()
        })
    })
})

test.describe('Evaluation Results', () => {
    test.afterEach(async () => {
        await takeScreenshot({ testcase: page, route: page.url() })
    })

    test.describe('Results', () => {
        test.beforeAll(async () => {
            if (page.url().includes(ROUTES.evaluations)) await page.getByRole('link', { name: '5' }).click()
            if (!page.url().includes(ROUTES.evaluationResult)) await page.goto(ROUTES.evaluationResult)
        })
        test('should have panels  summary/confusion matrix/label.roc_auc', async () => {
            await expect(page.getByText('Summary')).toBeVisible()
            await expect(page.getByText('roc_auc/0')).toBeVisible()
            await expect(page.getByText('labels')).toBeVisible()
            await expect(page.locator(SELECTOR.confusionMatrix)).toBeVisible()
        })
    })

    test.describe('Actions', () => {
        test.beforeAll(async () => {
            await page.getByRole('tab', { name: /Actions/ }).click()
            if (!page.url().includes(ROUTES.evaluationActions)) await page.goto(ROUTES.evaluationActions)
        })
        test('should have dag', async () => {
            await page.waitForSelector(':has-text("Step")')
            await expect(page.getByText('Step')).toBeDefined()
            await wait(1000)
        })
    })

    test.describe('Tasks', () => {
        test.beforeAll(async () => {
            await page.getByRole('tab', { name: /Tasks/ }).click()
            if (!page.url().includes(ROUTES.evaluationTasks)) await page.goto(ROUTES.evaluationTasks)
        })
        test('should have 3 tasks of success status', async () => {
            await expect(page.getByText('Success')).toHaveCount(3)
        })
        test('should show log & log count greater than 10', async () => {
            await page
                .getByText(/View Log/)
                .first()
                .click()
            await page.getByText(/Log\:/).first().click()
            await page.waitForSelector('.ReactVirtualized__Grid__innerScrollContainer > div')
            await expect(
                await page.locator('.ReactVirtualized__Grid__innerScrollContainer > div').count()
            ).toBeGreaterThan(10)
        })
    })
})
