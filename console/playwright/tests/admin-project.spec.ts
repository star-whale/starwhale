import { expect, Locator, Page } from '@playwright/test'
import { test } from '../setup'
import { CONST, ROUTES, SELECTOR } from './config'
import { getLastestRowID, getTableDisplayRow, selectOption, selectTreeOption, takeScreenshot, wait } from './utils'
let page: Page

test.beforeAll(async ({ admin }) => {
    page = admin.page
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
})

test.describe('Evaluation', () => {
    test.beforeAll(async () => {
        await page.goto(ROUTES.evaluations)
        const p = page.locator(SELECTOR.table)
        await selectOption(page, '.table-config-view', 'All runs')
        await expect(await getTableDisplayRow(p)).toBeGreaterThan(0)
    })

    test.describe('Evaluation Create', () => {
        let rowCount: any

        test.beforeAll(async () => {
            await page.goto(ROUTES.evaluations)
            await wait(500)
            rowCount = await getLastestRowID(page)
            await page.getByRole('button', { name: /Create$/ }).click()
            await expect(page).toHaveURL(ROUTES.evaluationNewJob)
        })

        test('should form be selected', async () => {
            await selectOption(page, SELECTOR.formItem('Resource Pool'), 'default')
            await selectTreeOption(page, SELECTOR.formItem('Model Version'), /starwhale/)
            await page.getByRole('button', { name: 'Select...' }).click()
            await selectTreeOption(page, SELECTOR.formItem('Dataset Version'), /starwhale/)
            // await selectTreeOption(page, SELECTOR.formItem('Runtime'), /starwhale/)
            const versions = page.locator(SELECTOR.formItem('Version'))
            const count = await versions.count()
            for (let i = 0; i < count; i++) {
                await expect(versions.nth(i)).not.toBeEmpty()
            }
        })

        // test.describe('Submit', () => {
        //     test('should select lastest versions', async () => {
        //         await page.getByRole('button', { name: 'Submit' }).click()
        //         await expect(page).toHaveURL(ROUTES.evaluations)
        //         await expect(await getLastestRowID(page)).toBeGreaterThan(rowCount)
        //     })
        // })
    })

    test.describe('List', () => {
        test.beforeAll(async () => {
            if (!page.url().includes(ROUTES.evaluations)) await page.goto(ROUTES.evaluations)
        })

        test('should evaluation have toolbar/header/row', async () => {
            const p = page.locator(SELECTOR.table)

            await expect(p.locator('.table-config-query')).toBeTruthy()
            await expect(p.locator('.table-config-column')).toBeTruthy()

            await page.waitForSelector('.table-headers')
            await expect(p.locator('.table-headers').getByText('Evaluation ID')).toBeTruthy()
        })
    })

    test.describe('Compare', () => {
        test.beforeAll(async () => {
            if (!page.url().includes(ROUTES.evaluations)) await page.goto(ROUTES.evaluations)
        })

        test('should show compare table when checked one row', async () => {
            const p = page.locator(SELECTOR.table)

            const isChecked = await p.locator(SELECTOR.headerFirst).locator('label input').isChecked()
            if (isChecked) await p.locator(SELECTOR.headerFirst).locator('label').click()
            await p.locator(SELECTOR.row1column1).locator('input').check()
            await p.locator(SELECTOR.row2column1).locator('input').check()
            await expect(page.getByText(/Compare/)).toBeVisible()
            await expect(await page.locator('.cell--neq').count()).toBeGreaterThan(0)
            await p.locator(SELECTOR.row1column1).locator('label').uncheck()
            await p.locator(SELECTOR.row2column1).locator('label').uncheck()
        })
    })
})

test.describe('Evaluation Results', () => {
    test.describe('Results', () => {
        test.beforeAll(async () => {
            if (page.url().includes(ROUTES.evaluations)) await page.getByRole('link', { name: '5' }).click()
            if (!page.url().includes(ROUTES.evaluationResult)) await page.goto(ROUTES.evaluationResult)
        })
        test('should have panels num > 1', async () => {
            await expect(page.getByText('Summary')).toBeVisible()
            await wait(1000)
            await expect(page.getByText('sys/job_status')).toBeVisible()
            await wait(1000)
            await expect(await page.locator(SELECTOR.panels).count()).toBeGreaterThan(1)
        })
    })

    test.describe('Actions', () => {
        test.beforeAll(async () => {
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
            if (!page.url().includes(ROUTES.evaluationTasks)) await page.goto(ROUTES.evaluationTasks)
        })
        test('should have at least 1 tasks of success status', async () => {
            await expect(await page.getByText('Success').count()).toBeGreaterThan(0)
        })
        test('should show success task log', async () => {
            await page
                .getByText(/View Logs/)
                .last()
                .click()
            await expect(page.locator('.tr--selected')).toBeDefined()
        })
        test('should log count be greater than 10', async () => {
            await page
                .getByText(/Execution id\:/)
                .first()
                .click()
            await page.waitForSelector('.ReactVirtualized__Grid__innerScrollContainer > div')
            await expect(
                await page.locator('.ReactVirtualized__Grid__innerScrollContainer > div').count()
            ).toBeGreaterThan(10)
        })
    })
})

test.describe('Models', () => {
    test.describe('List', () => {
        test.beforeAll(async () => {
            if (!page.url().includes(ROUTES.models)) await page.goto(ROUTES.models)
        })

        test('should have 1 model of mnist', async () => {
            await expect(page.locator('td').getByText('mnist')).toHaveCount(1)
        })

        test('should model name be link to model overview', async () => {
            await page.getByRole('link', { name: 'mnist' }).click()
            await expect(page).toHaveURL(ROUTES.modelOverview)
        })

        test('breadcrumb should be back to models', async () => {
            await page.getByRole('button', { name: 'Models' }).click()
            await expect(page).toHaveURL(ROUTES.models)
        })
    })

    test.describe('Versions', () => {
        test.beforeAll(async () => {
            await page.goto(ROUTES.models)
        })

        test('should link to model versions', async () => {
            await page.locator('tr > td >> nth=0').getByRole('link').click()
            await expect(page.locator('tr >> nth=0')).not.toHaveText(/Revert/)
            await expect(page.locator('tr >> nth=1').getByRole('button', { name: /Revert/ })).toBeDefined()
        })
    })

    test.describe('Files', () => {
        test.beforeAll(async () => {
            if (!page.url().includes(ROUTES.modelVersionFiles)) await page.goto(ROUTES.modelVersionFiles)
        })

        test('should model overview show editor', async () => {
            await page
                // .getByRole('button', { name: /model\.yaml/ })
                .locator('[data-nodeid="src/model.yaml"]')
                .click()
            // await expect(page.locator('div:right-of(:text("Version Name"))').first()).toHaveText(
            //     'mftdoolcgvqwknrtmftdgyjzobvti2q'
            // )
            // await page.waitForSelector(':has-text("mnist.evaluator:MNISTInference")', { timeout: 10000 })
            // await expect(page.locator(':right-of(:text("mnist.evaluator:MNISTInference"))')).toBeVisible()
            // await page.waitForResponse((response) => response.url().includes('file') && response.status() === 200)
            // await expect(page.locator('span:has-text("MNISTInference")')).toBeVisible()
        })
    })
})

test.describe('Datasets', () => {
    test.describe('List', () => {
        test.beforeAll(async () => {
            if (!page.url().includes(ROUTES.datasets)) await page.goto(ROUTES.datasets)
        })

        test('should have mnist dataset', async () => {
            await expect(page.locator('td').getByText(CONST.datasetName)).toBeVisible()
        })

        test('should dataset name be link to version files', async () => {
            await page.getByRole('link', { name: CONST.datasetName }).click()
            await page.waitForSelector('.image-grayscale')
            await expect(await page.locator('.image-grayscale').count()).toBeGreaterThan(0)
        })
    })

    test.describe('Versions', async () => {
        test.beforeAll(async () => {
            if (!page.url().includes(ROUTES.datasetVersionFiles)) await page.goto(ROUTES.datasetVersionFiles)
        })

        test('should history be link to version list', async () => {
            await page.getByText(/History/).click()
            await expect(page).toHaveURL(ROUTES.datasetOverview)
            await expect(page.getByText(/History/)).toBeHidden()
        })
    })
})
