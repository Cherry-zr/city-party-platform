import { defineConfig } from '@playwright/test'
import { loadEnv } from 'vite'

const developmentEnv = loadEnv('development', process.cwd(), '')
const localHttpsEnabled = Boolean(
  developmentEnv.DEV_HTTPS_CERT_PATH && developmentEnv.DEV_HTTPS_KEY_PATH
)
const defaultProtocol = localHttpsEnabled ? 'https' : 'http'
const baseURL = process.env.PLAYWRIGHT_BASE_URL || defaultProtocol + '://127.0.0.1:5173'

export default defineConfig({
  testDir: './e2e',
  timeout: 90_000,
  expect: {
    timeout: 10_000
  },
  fullyParallel: false,
  forbidOnly: Boolean(process.env.CI),
  retries: process.env.CI ? 1 : 0,
  reporter: [
    ['list'],
    ['html', { open: 'never', outputFolder: 'playwright-report' }]
  ],
  use: {
    baseURL,
    ignoreHTTPSErrors: localHttpsEnabled,
    channel: 'chrome',
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure'
  },
  webServer: process.env.PLAYWRIGHT_SKIP_WEB_SERVER
    ? undefined
    : {
        command: 'npm run dev -- --host 127.0.0.1',
        url: baseURL,
        reuseExistingServer: true,
        ignoreHTTPSErrors: localHttpsEnabled,
        timeout: 120_000,
        stdout: 'ignore',
        stderr: 'pipe'
      }
})
