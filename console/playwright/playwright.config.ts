import type { PlaywrightTestConfig } from '@playwright/test'
import { devices } from '@playwright/test'
import * as fse from 'fs-extra'
import path from 'path'

// process.env.PROXY = 'http://e2e.pre.intra.starwhale.ai/'
const proxy = process.env.PROXY ?? ''

/**
 * Read environment variables from file.
 * https://github.com/motdotla/dotenv
 */
require('dotenv').config()
fse.ensureDir('test-storage')
// fse.emptyDirSync('test-video')
if (process.env.CLEAN_AUTH === 'true') fse.emptyDirSync('test-storage')
else fse.ensureDir('test-storage')

/**
 * See https://playwright.dev/docs/test-configuration.
 */
const config: PlaywrightTestConfig = {
    testDir: './tests',
    // testIgnore: ['tests-examples/*'],
    /* Maximum time one test can run for. */
    timeout: 20 * 1000,
    expect: {
        /**
         * Maximum time expect() should wait for the condition to be met.
         * For example in `await expect(locator).toHaveText();`
         */
        timeout: 5000,
    },
    /* Run tests in files in parallel */
    fullyParallel: true,
    /* Fail the build on CI if you accidentally left test.only in the source code. */
    forbidOnly: !!process.env.CI,
    /* Retry on CI only */
    retries: process.env.CI ? 2 : 0,
    /* Opt out of parallel tests on CI. */
    workers: process.env.CI ? 2 : 1,
    /* Reporter to use. See https://playwright.dev/docs/test-reporters */
    reporter: 'html',
    /* Shared settings for all the projects below. See https://playwright.dev/docs/api/class-testoptions. */
    use: {
        /* Maximum time each action such as `click()` can take. Defaults to 0 (no limit). */
        actionTimeout: 0,
        /* Base URL to use in actions like `await page.goto('/')`. */
        baseURL: process.env.PROXY ?? 'http://localhost:5177',

        /* Collect trace when retrying the failed test. See https://playwright.dev/docs/trace-viewer */
        trace: 'on-first-retry',

        video: {
            mode: 'on-first-retry',
            size: { width: 1024, height: 760 },
        },
    },

    /* Configure projects for major browsers */
    projects: [
        {
            name: 'setup',
            testMatch: '**/*.setup.ts',
        },
        {
            name: 'chromium',
            use: {
                ...devices['Desktop Chrome'],
                contextOptions: {
                    // chromium-specific permissions
                    permissions: ['clipboard-read', 'clipboard-write'],
                },
            },
            dependencies: ['setup'],
        },

        // {
        //     name: 'firefox',
        //     use: {
        //         ...devices['Desktop Firefox'],
        //     },
        // },

        // {
        //     name: 'webkit',
        //     use: {
        //         ...devices['Desktop Safari'],
        //     },
        // },

        /* Test against mobile viewports. */
        // {
        //   name: 'Mobile Chrome',
        //   use: {
        //     ...devices['Pixel 5'],
        //   },
        // },
        // {
        //   name: 'Mobile Safari',
        //   use: {
        //     ...devices['iPhone 12'],
        //   },
        // },

        /* Test against branded browsers. */
        // {
        //   name: 'Microsoft Edge',
        //   use: {
        //     channel: 'msedge',
        //   },
        // },
        // {
        //   name: 'Google Chrome',
        //   use: {
        //     channel: 'chrome',
        //   },
        // },
    ],

    /* Folder for test artifacts such as screenshots, videos, traces, etc. */
    outputDir: 'test-results/',

    /* Run your local dev server before starting the tests */
    // webServer: {
    //     command: 'npm run start',
    //     port: 3000,
    // },
    // globalSetup: require.resolve('./setup/global-setup'),
}

export default config
