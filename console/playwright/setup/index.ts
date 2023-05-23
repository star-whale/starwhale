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
    guest: async ({ browser }, use) => {
        await use(await UserPage.create(browser, 'guest'))
    },
})
