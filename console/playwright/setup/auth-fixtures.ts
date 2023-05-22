// fixtures.ts
import { test as base, Page, Browser, Locator } from '@playwright/test'
export { expect } from '@playwright/test'

// Page Object Model for the "admin" page.
// Here you can add locators and helper methods specific to the admin page.
export class AdminPage {
    // Page signed in as "admin".
    page: Page

    constructor(page: Page) {
        this.page = page
    }

    static async create(browser: Browser, role: string) {
        const context = await browser.newContext({
            storageState: `test-storage/storage-${role}.json`,
            recordVideo: {
                dir: 'test-video/',
            },
        })
        const page = await context.newPage()
        return new AdminPage(page)
    }
}

// Page Object Model for the "user" page.
// Here you can add locators and helper methods specific to the user page.
export class UserPage {
    // Page signed in as "user".
    page: Page

    constructor(page: Page) {
        this.page = page
    }

    static async create(browser: Browser, role: string) {
        const context = await browser.newContext({
            storageState: `test-storage/storage-${role}.json`,
            recordVideo: {
                dir: 'test-video/',
            },
        })
        const page = await context.newPage()
        return new UserPage(page)
    }
}

// // Declare the types of your fixtures.
// type MyFixtures = {
//     admin: AdminPage
//     user: UserPage
// }

// // Extend base test by providing "adminPage" and "userPage".
// // This new "test" can be used in multiple test files, and each of them will get the fixtures.
// export const test = base.extend<MyFixtures>({
//     admin: async ({ browser }, use) => {
//         await use(await AdminPage.create(browser))
//     },
//     user: async ({ browser }, use) => {
//         await use(await UserPage.create(browser))
//     },
// })
