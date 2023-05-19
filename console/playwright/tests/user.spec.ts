import { expect, Locator, Page } from '@playwright/test'
import { test } from '../setup'
import { CONST, ROUTES, SELECTOR } from './config'
import { getLastestRowID, getTableDisplayRow, selectOption, takeScreenshot, wait } from './utils'
let page: Page

test.beforeAll(async ({ user }) => {
    page = user.page
    // await wait(1000)
    await page.goto('/', {
        waitUntil: 'networkidle',
    })
    await expect(page).toHaveTitle(/Starwhale Console/)
})

// test.afterAll(async ({}) => {
//     await wait(5000)

//     if (process.env.CLOSE_AFTER_TEST === 'true') {
//         await page.context().close()
//         if (process.env.CLOSE_SAVE_VIDEO === 'true') await page.video()?.saveAs(`test-video/user.webm`)
//     }
// })

test.describe('Login', () => {
    test.afterAll(async () => {
        await takeScreenshot({ testcase: page, route: page.url() })
    })
    test('default route should be projects', async ({}) => {
        // await page.waitForURL(/\/projects/, { timeout: 20000 })
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
    test.afterAll(async () => {
        await takeScreenshot({ testcase: page, route: page.url() })
    })
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

    // test.describe('Auth', () => {
    //     test('none admin should have no create button', async () => {
    //         await expect(page.locator(SELECTOR.listCreate)).toBeHidden()
    //     })
    // })

    test.describe('List', () => {
        test('should evaluation have toolbar/header/row', async () => {
            const p = page.locator(SELECTOR.table)

            await expect(p.locator('.table-config-query')).toBeTruthy()
            await expect(p.locator('.table-config-column')).toBeTruthy()

            await page.waitForSelector('.table-headers')
            await expect(p.locator('.table-headers').getByText('Evaluation ID')).toBeTruthy()
        })
    })

    // test.describe('Search', () => {
    //     test('should be 2 success status ', async () => {
    //         const p = page.locator(SELECTOR.table)
    //         await p.getByRole('textbox', { name: 'Search by text' }).fill('Succe')
    //         await wait(1000)
    //         await expect(await getTableDisplayRow(p)).toEqual(2)
    //     })
    // })

    // test.describe('Manage columns', () => {
    //     test('remove evaluation column & add accuracy column', async () => {
    //         const checkedButton = page.locator(
    //             'role=button[name="Evaluation ID"] >> label:not(:has([aria-checked="false"]))'
    //         )
    //         if ((await checkedButton.count()) > 0) await checkedButton.click()

    //         const p = page.locator(SELECTOR.table)
    //         const drawer = page.locator('[data-baseweb="drawer"]')
    //         await p.getByRole('button', { name: /Manage Columns/ }).click()

    //         await drawer.getByTitle('Evaluation ID').locator('label').uncheck()
    //         await drawer.getByTitle('accuracy').locator('label').check()
    //         await drawer.getByRole('button', { name: /Apply/ }).click()
    //         await drawer.getByTitle('Close').click()

    //         await expect(p.locator('.table-headers').getByText('Evaluation ID')).toBeHidden()
    //         await expect(p.locator('.table-headers').getByText('accuracy')).toBeVisible()
    //     })
    // })

    // test.describe('Filter', () => {
    //     test('should be no rows when evaluation id = none', async () => {
    //         const p = page.locator(SELECTOR.table)
    //         await p.getByText('Filters').click()

    //         await page.waitForSelector(':has-text("Add filter")')
    //         await page.getByText('Add filter').click()

    //         await selectOption(page, '.filter-ops', 'Evaluation ID')
    //         await page.getByText('Apply').click()

    //         await expect(await getTableDisplayRow(p)).toEqual(0)
    //     })
    // })

    // test.describe('View', () => {
    //     test('should show rows when select all runs', async () => {
    //         const p = page.locator(SELECTOR.table)
    //         await selectOption(page, '.table-config-view', 'All runs')
    //         await expect(await getTableDisplayRow(p)).toBeGreaterThan(0)
    //     })
    // })

    test.describe('Compare', () => {
        test('should show compare table when checked one row', async () => {
            const p = page.locator(SELECTOR.table)

            const isChecked = await p.locator(SELECTOR.headerFirst).locator('label input').isChecked()
            if (isChecked) await p.locator(SELECTOR.headerFirst).locator('label').click()
            await p.locator(SELECTOR.row1column1).locator('label').check()
            await p.locator(SELECTOR.row2column1).locator('label').check()
            await expect(page.getByText(/Compare Evaluations/)).toBeVisible()
            await wait(1000)
            await expect(page.locator(SELECTOR.headerFocused)).toHaveText(/mnist/)
            await wait(1000)
            await expect(await page.locator('.cell--neq').count()).toBeGreaterThan(0)
            await p.locator(SELECTOR.row1column1).locator('label').uncheck()
            await p.locator(SELECTOR.row2column1).locator('label').uncheck()
        })
    })
})

test.describe('Evaluation Create', () => {
    let rowCount: any

    test.beforeAll(async () => {
        await page.goto(ROUTES.evaluations)
        await wait(500)
        rowCount = await getLastestRowID(page)
        await page.getByRole('button', { name: /Create$/ }).click()
        await expect(page).toHaveURL(ROUTES.evaluationNewJob)

        await selectOption(page, SELECTOR.formItem('Resource Pool'), 'default')
        await selectOption(page, SELECTOR.formItem('Model Name'), 'mnist')
        await selectOption(page, SELECTOR.formItem('Dataset Name'), 'mnist')
        await selectOption(page, SELECTOR.formItem('Runtime'), 'pytorch-mnist')
        const versions = page.locator(SELECTOR.formItem('Version'))
        const count = await versions.count()
        for (let i = 0; i < count; i++) {
            await expect(versions.nth(i)).not.toBeEmpty()
        }
    })
    test.afterAll(async () => {
        await takeScreenshot({ testcase: page, route: page.url() })
    })
    test.describe('Overview', () => {
        test('should add resource', async () => {
            const add = page.getByRole('button', { name: /Add/ }).first()
            await expect(add).toBeVisible()

            await add.click()
            const resourceItem = (i: number, j: number) =>
                `[class*=resource] >> nth=${i} >> [data-baseweb="form-control-container"] >> nth=${j} >> div`
            await selectOption(page, resourceItem(0, 0), 'cpu')
            await page.locator(resourceItem(0, 1)).locator('input').fill('1')
            await page.locator(SELECTOR.formItem('Raw Type')).click()
            await page.waitForSelector('.monaco-editor')
            expect(page.locator('.view-lines')).toHaveText(
                '- concurrency: 1  needs: []  resources:    - type: cpu      num: 1  job_name: default  step_name: ppl  task_num: 1- concurrency: 1  needs:    - ppl  resources: []  job_name: default  step_name: cmp  task_num: 1'
            )
        })
    })

    test.describe('Submit', () => {
        test('should select lastest versions', async () => {
            await page.getByRole('button', { name: 'Submit' }).click()
            await expect(page).toHaveURL(ROUTES.evaluations)
            await expect(await getLastestRowID(page)).toBeGreaterThan(rowCount)
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
            await wait(1000)
            await expect(page.getByText('sys/job_status')).toBeVisible()
            await wait(1000)
            await expect(page.locator(SELECTOR.confusionMatrix)).toBeVisible()
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
            await expect(page.getByText('Success')).toBeGreaterThan(0)
        })
        test('should show success task log', async () => {
            await page
                .getByText(/View Log/)
                .first()
                .click()
            await expect(page.locator('.tr--selected')).toBeDefined()
        })
        test('should log count be greater than 10', async () => {
            await page.getByText(/Log\:/).first().click()
            await page.waitForSelector('.ReactVirtualized__Grid__innerScrollContainer > div')
            await expect(
                await page.locator('.ReactVirtualized__Grid__innerScrollContainer > div').count()
            ).toBeGreaterThan(10)
        })
    })
})

test.describe('Models', () => {
    test.afterEach(async () => {
        await takeScreenshot({ testcase: page, route: page.url() })
    })

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
            await page.getByRole('link', { name: /History/ }).click()
            await expect(page).toHaveURL(ROUTES.modelVersions)
        })

        test('lastest model should not be reverted', async () => {
            await expect(page.locator('tr >> nth=0')).not.toHaveText(/Revert/)
            await expect(page.locator('tr >> nth=1').getByRole('button', { name: /Revert/ })).toBeDefined()
        })
    })

    test.describe('Overview', () => {
        test.beforeAll(async () => {
            await page.goto(ROUTES.modelOverview)
        })

        test('should model name be link to model overview', async () => {
            await page.getByRole('button', { name: /Model ID\:/ }).click()
            await expect(page.locator('div:right-of(:text("Version Name"))').first()).toHaveText(
                'mftdoolcgvqwknrtmftdgyjzobvti2q'
            )
            await expect(page.getByRole('cell', { name: 'mftdoolcgvqwknrtmftdgyjzobvti2q.swmp' })).toBeVisible()
        })
    })
})

test.describe('Datasets', () => {
    test.afterEach(async () => {
        await takeScreenshot({ testcase: page, route: page.url() })
    })

    test.describe('List', () => {
        test.beforeAll(async () => {
            if (!page.url().includes(ROUTES.datasets)) await page.goto(ROUTES.datasets)
        })

        test('should have 1 dataset', async () => {
            await expect(page.locator('td').getByText('mnist')).toHaveCount(1)
        })

        test('should dataset name be link to version files', async () => {
            await page.getByRole('link', { name: 'mnist' }).click()
            await expect(page).toHaveURL(ROUTES.datasetVersionFiles)
        })

        test('breadcrumb should be back to dataset list', async () => {
            await page.getByRole('button', { name: 'Datasets' }).click()
            await expect(page).toHaveURL(ROUTES.datasets)
        })
    })

    test.describe('Files', async () => {
        test.beforeAll(async () => {
            if (!page.url().includes(ROUTES.datasetVersionFiles)) await page.goto(ROUTES.datasetVersionFiles)
            await page.locator('.icon-grid').click()
        })

        test('should canvas render at least one dataset', async () => {
            const dom = page.locator('.image-grayscale >> nth=0 >> canvas')
            await wait(1000)
            await expect(await dom.screenshot()).toMatchSnapshot()
        })

        test('should show data when version changed', async () => {
            await selectOption(page, '[data-baseweb="select"]', /v5/)
            await expect(page).toHaveURL(/\/versions\/5\/files/)
            // fix: dom not attached
            await page.click('.image-grayscale >> nth=0 >> canvas')
            await expect(page.locator('.react-transform-wrapper canvas')).toBeVisible()
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
