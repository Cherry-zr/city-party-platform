import { expect, test } from '@playwright/test'

const apiBaseURL = process.env.E2E_API_BASE_URL || 'http://127.0.0.1:8080'
const testPrefix = `${process.env.E2E_TEST_PREFIX || 'CP_E2E'}_${Date.now()}`

function credentials(role) {
  const prefix = role.toUpperCase()
  const username = process.env[`E2E_${prefix}_USERNAME`]
  const password = process.env[`E2E_${prefix}_PASSWORD`]
  if (!username || !password) {
    throw new Error(`Missing E2E_${prefix}_USERNAME or E2E_${prefix}_PASSWORD`)
  }
  return { username, password }
}

async function apiOk(request, method, path, options = {}) {
  const response = await request.fetch(`${apiBaseURL}${path}`, {
    method,
    ...options
  })
  expect(response.ok(), `${method} ${path}`).toBeTruthy()
  const payload = await response.json()
  expect(payload.code, `${method} ${path} business code`).toBe(200)
  return payload.data
}

async function login(request, role) {
  const { username, password } = credentials(role)
  const captcha = await apiOk(request, 'GET', '/api/auth/captcha')
  return apiOk(request, 'POST', '/api/auth/login', {
    data: {
      username,
      password,
      captchaKey: captcha.captchaKey,
      captchaCode: captcha.captchaText
    }
  })
}

async function seedSession(page, session) {
  await page.addInitScript(({ token, user }) => {
    window.localStorage.setItem('token', token)
    window.localStorage.setItem('user', JSON.stringify(user))
  }, session)
}

function watchConsole(page) {
  const messages = []
  page.on('console', (message) => {
    if (['error', 'warning'].includes(message.type())) {
      messages.push(`${message.type()}: ${message.text()}`)
    }
  })
  page.on('pageerror', (error) => {
    messages.push(`pageerror: ${error.message}`)
  })
  return messages
}

async function silenceBrowserDefaultRequests(page) {
  await page.route('**/favicon.ico', (route) => route.fulfill({ status: 204, body: '' }))
}

const homeRecommendedActivity = {
  id: 901,
  creatorId: 88,
  title: '周末新手桌游局',
  category: '桌游',
  tags: ['周末', '新手友好'],
  startTime: '2026-09-05T14:00:00',
  endTime: '2026-09-05T17:00:00',
  signupDeadline: '2026-09-04T20:00:00',
  city: '北京',
  address: '朝阳区桌游店',
  longitude: 116.4,
  latitude: 39.9,
  minParticipants: 2,
  maxParticipants: 8,
  approvedCount: 3,
  favoriteCount: 4,
  waitlistCount: 0,
  costType: 'AA',
  costAmount: 45,
  status: 'SIGNING',
  creator: { id: 88, nickname: '桌游发起人', avatarUrl: null, creditScore: 108 }
}

async function mockRecommendationHomeApis(page, onRecommendationRequest = () => {}) {
  await page.route(/^https?:\/\/[^/]+\/api\//, async (route) => {
    const url = new URL(route.request().url())
    const pathname = url.pathname
    let data = { records: [], total: 0 }
    if (pathname === '/api/recommendations/activities') {
      onRecommendationRequest(url)
      const optimized = url.searchParams.has('longitude') && url.searchParams.has('latitude')
      data = [{
        activity: { ...homeRecommendedActivity, distanceKm: optimized ? 0.42 : null },
        recommendationScore: optimized ? 94.12 : 92.35,
        distanceKm: optimized ? 0.42 : null,
        reasons: optimized
          ? ['匹配你的兴趣：周末、新手友好', '距你约 0.42 km', '近期报名热度较高']
          : ['匹配你的兴趣：周末、新手友好', '近期报名热度较高', '发起人信用良好'],
        scoreDetail: {
          interest: 100,
          distance: optimized ? 91.94 : null,
          hotness: 72,
          time: 88,
          credit: 96
        }
      }]
    } else if (pathname === '/api/activities/901') {
      data = homeRecommendedActivity
    } else if (pathname === '/api/notices/unread-count') {
      data = 0
    }
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ code: 200, data })
    })
  })
}

async function mockHomeBrowserEnvironment(page, { geolocation = 'permission-denied', secureContext = true } = {}) {
  await page.addInitScript(({ geolocationMode, isSecureContext }) => {
    window.WebSocket = class MockWebSocket {
      static CONNECTING = 0
      static OPEN = 1
      static CLOSED = 3

      constructor() {
        this.readyState = MockWebSocket.OPEN
        queueMicrotask(() => this.onopen?.())
      }

      send() {}

      close() {
        this.readyState = MockWebSocket.CLOSED
        this.onclose?.()
      }
    }

    window.__geolocationCallCount = 0
    if (!isSecureContext) {
      Object.defineProperty(window, 'isSecureContext', { configurable: true, value: false })
    }
    Object.defineProperty(navigator, 'geolocation', {
      configurable: true,
      value: geolocationMode === 'unavailable'
        ? undefined
        : {
            getCurrentPosition(success, error) {
              window.__geolocationCallCount += 1
              if (geolocationMode === 'success') {
                success({ coords: { latitude: 39.9042, longitude: 116.4074 } })
              } else {
                error({ code: 1, message: 'Permission denied by E2E test' })
              }
            }
          }
    })
  }, { geolocationMode: geolocation, isSecureContext: secureContext })
}

async function seedRecommendationSession(page) {
  await seedSession(page, {
    token: 'recommendation-feature-token',
    user: { id: 7, username: 'recommendation_user', nickname: '推荐用户', role: 'USER' }
  })
}

async function mockMobileNavigationApis(page) {
  await page.route(/^https?:\/\/[^/]+\/api\//, (route) => {
    const pathname = new URL(route.request().url()).pathname
    let data = { records: [], total: 0 }

    if (pathname === '/api/notices/unread-count') {
      data = 0
    } else if (pathname === '/api/user/profile-overview') {
      data = {
        username: 'navigation_user',
        nickname: 'Navigation User',
        city: 'Beijing',
        creditScore: 100,
        creditLevel: 'GOOD',
        publishedActivityCount: 0,
        joinedActivityCount: 0,
        waitingActivityCount: 0,
        receivedReviewCount: 0,
        averageRating: 0,
        unreadNoticeCount: 0,
        interestTags: []
      }
    }

    return route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ code: 200, data })
    })
  })
}

function activityPayload(title, started = false) {
  const now = new Date()
  const start = started
    ? new Date(now.getTime() - 60 * 60 * 1000)
    : new Date(now.getTime() + 7 * 24 * 60 * 60 * 1000)
  const end = started
    ? new Date(now.getTime() + 60 * 60 * 1000)
    : new Date(start.getTime() + 2 * 60 * 60 * 1000)
  const deadline = new Date(start.getTime() - 2 * 60 * 60 * 1000)
  return {
    title,
    category: 'Other',
    tags: ['e2e', 'smoke'],
    startTime: start.toISOString().slice(0, 19),
    endTime: end.toISOString().slice(0, 19),
    signupDeadline: deadline.toISOString().slice(0, 19),
    city: 'Beijing',
    address: 'E2E Test Address',
    longitude: 116.4,
    latitude: 39.9,
    minParticipants: 1,
    maxParticipants: 3,
    costType: 'FREE',
    costAmount: 0,
    aaRule: 'E2E test data',
    coverUrl: '',
    description: `${title} generated by Playwright smoke test.`,
    notes: 'Generated test data with a cleanup prefix.',
    needApproval: false
  }
}

test('login, activity list, detail, publish, edit, cancel and finish flow', async ({ page, request }) => {
  const consoleMessages = watchConsole(page)
  await silenceBrowserDefaultRequests(page)
  const session = await login(request, 'user')
  await seedSession(page, session)

  await page.goto('/')
  await expect(page.locator('.activity-card').first()).toBeVisible()
  await page.locator('.activity-card').first().click()
  await expect(page).toHaveURL(/\/activities\/\d+/)

  const cancelTitle = `${testPrefix}_cancel`
  const created = await apiOk(request, 'POST', '/api/activities', {
    headers: { Authorization: `Bearer ${session.token}` },
    data: activityPayload(cancelTitle)
  })
  expect(created.auditStatus).toBe('PENDING')
  const hiddenBeforeAudit = await apiOk(
    request,
    'GET',
    '/api/activities?keyword=' + encodeURIComponent(cancelTitle)
  )
  expect(hiddenBeforeAudit.records).toEqual([])
  const adminSession = await login(request, 'admin')
  await apiOk(request, 'PATCH', '/api/admin/activities/' + created.id + '/audit', {
    headers: { Authorization: `Bearer ${adminSession.token}` },
    data: { auditStatus: 'APPROVED', rejectReason: null }
  })
  const publicAfterAudit = await apiOk(
    request,
    'GET',
    '/api/activities?keyword=' + encodeURIComponent(cancelTitle)
  )
  expect(publicAfterAudit.records.map((item) => item.id)).toContain(created.id)
  await page.goto(`/activities/${created.id}`)
  await expect(page.getByRole('heading', { name: cancelTitle })).toBeVisible()

  const editedTitle = `${cancelTitle}_edited`
  await apiOk(request, 'PUT', `/api/activities/${created.id}`, {
    headers: { Authorization: `Bearer ${session.token}` },
    data: activityPayload(editedTitle)
  })
  await page.goto(`/publish?editId=${created.id}`)
  await expect(page.locator('input').first()).toHaveValue(editedTitle)
  await apiOk(request, 'PATCH', `/api/activities/${created.id}/cancel`, {
    headers: { Authorization: `Bearer ${session.token}` }
  })
  const cancelled = await apiOk(request, 'GET', `/api/activities/${created.id}`, {
    headers: { Authorization: `Bearer ${session.token}` }
  })
  expect(cancelled.status).toBe('CANCELLED')

  const rejectedTitle = `${testPrefix}_rejected`
  const rejected = await apiOk(request, 'POST', '/api/activities', {
    headers: { Authorization: `Bearer ${session.token}` },
    data: activityPayload(rejectedTitle)
  })
  const adminPage = await page.context().newPage()
  await seedSession(adminPage, adminSession)
  await adminPage.goto('/admin/activities')
  await expect(adminPage.getByRole('heading', { name: '活动管理' })).toBeVisible()
  const rejectedRow = adminPage.locator('.el-table__row', { hasText: rejectedTitle })
  await expect(rejectedRow).toBeVisible()
  await rejectedRow.getByRole('button', { name: '拒绝' }).click()
  await adminPage.locator('.el-message-box textarea').fill('地址信息需要补充')
  await adminPage.getByRole('button', { name: '确认拒绝' }).click()
  await expect(adminPage.locator('.el-message--success')).toContainText('活动已拒绝')
  await adminPage.close()
  await page.evaluate(({ token, user }) => {
    window.localStorage.setItem('token', token)
    window.localStorage.setItem('user', JSON.stringify(user))
  }, session)

  const rejectedDetail = await apiOk(request, 'GET', '/api/activities/' + rejected.id, {
    headers: { Authorization: `Bearer ${session.token}` }
  })
  expect(rejectedDetail.auditStatus).toBe('REJECTED')
  expect(rejectedDetail.rejectReason).toBe('地址信息需要补充')
  await page.goto('/my-activities')
  await expect(page.getByText(rejectedTitle, { exact: true })).toBeVisible()
  await expect(page.getByText(/地址信息需要补充/)).toBeVisible()
  await page.getByRole('button', { name: '修改并重新提交' }).click()
  const resubmittedTitle = rejectedTitle + '_resubmitted'
  await page.locator('.van-field', { hasText: '标题' }).locator('input').fill(resubmittedTitle)
  await page.getByRole('button', { name: '保存修改' }).click()
  await expect(page).toHaveURL(new RegExp('/activities/' + rejected.id))
  const resubmitted = await apiOk(request, 'GET', '/api/activities/' + rejected.id, {
    headers: { Authorization: `Bearer ${session.token}` }
  })
  expect(resubmitted.auditStatus).toBe('PENDING')
  expect(resubmitted.rejectReason).toBeNull()

  const finishTitle = `${testPrefix}_finish`
  const started = await apiOk(request, 'POST', '/api/activities', {
    headers: { Authorization: `Bearer ${session.token}` },
    data: activityPayload(finishTitle, true)
  })
  await apiOk(request, 'PATCH', `/api/activities/${started.id}/finish`, {
    headers: { Authorization: `Bearer ${session.token}` }
  })
  const finished = await apiOk(request, 'GET', `/api/activities/${started.id}`, {
    headers: { Authorization: `Bearer ${session.token}` }
  })
  expect(finished.status).toBe('FINISHED')
  expect(consoleMessages).toEqual([])
})

test('mobile tabbar navigation and active state follow current route', async ({ page }) => {
  await silenceBrowserDefaultRequests(page)
  await mockMobileNavigationApis(page)
  await seedSession(page, {
    token: 'e2e-navigation-token',
    user: {
      id: 1,
      username: 'navigation_user',
      nickname: 'Navigation User',
      role: 'USER',
      city: 'Beijing'
    }
  })

  const tabbar = page.locator('.van-tabbar')
  const tabs = [
    { label: '首页', path: '/' },
    { label: '地图', path: '/map' },
    { label: '发布', path: '/publish' },
    { label: '报名', path: '/my-signups' },
    { label: '我的', path: '/profile' }
  ]

  const expectPath = async (path) => {
    await expect.poll(() => new URL(page.url()).pathname).toBe(path)
  }
  const getTab = (label) => tabbar.locator('[role="tab"]', { hasText: label })
  const expectOnlyActiveTab = async (label) => {
    const activeTabs = tabbar.locator('[role="tab"][aria-selected="true"]')
    await expect(activeTabs).toHaveCount(1)
    await expect(getTab(label)).toHaveAttribute('aria-selected', 'true')
  }

  for (const tab of tabs) {
    await page.goto(tab.path)
    await expectPath(tab.path)
    await expectOnlyActiveTab(tab.label)
  }

  for (const tab of tabs.slice(1)) {
    await page.goto(tab.path)
    await getTab('首页').click()
    await expectPath('/')
    await expectOnlyActiveTab('首页')
  }

  await page.goto('/')
  for (const tab of tabs.slice(1)) {
    await getTab(tab.label).click()
    await expectPath(tab.path)
    await expectOnlyActiveTab(tab.label)
  }

  await getTab('首页').click()
  await expectPath('/')
  await expectOnlyActiveTab('首页')

  await page.goBack()
  await expectPath('/profile')
  await expectOnlyActiveTab('我的')

  await page.goForward()
  await expectPath('/')
  await expectOnlyActiveTab('首页')

  await page.reload()
  await expectPath('/')
  await expectOnlyActiveTab('首页')

  await getTab('首页').click()
  await expectPath('/')
  await expectOnlyActiveTab('首页')
})

test('guest home keeps the original activity flow without requesting recommendations', async ({ page }) => {
  await silenceBrowserDefaultRequests(page)
  let recommendationRequests = 0

  await page.route(/^https?:\/\/[^/]+\/api\//, async (route) => {
    const pathname = new URL(route.request().url()).pathname
    if (pathname === '/api/recommendations/activities') {
      recommendationRequests += 1
    }

    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ code: 200, data: { records: [], total: 0 } })
    })
  })

  await page.goto('/')
  await expect(page.getByText('活动发现', { exact: true })).toBeVisible()
  await expect(page.getByText('为你推荐', { exact: true })).toHaveCount(0)
  expect(recommendationRequests).toBe(0)
})

test('logged-in home shows explainable recommendations and opens activity detail', async ({ page }) => {
  await silenceBrowserDefaultRequests(page)
  await mockHomeBrowserEnvironment(page)
  const recommendationRequests = []
  await mockRecommendationHomeApis(page, (url) => recommendationRequests.push(url))
  await seedRecommendationSession(page)

  await page.goto('/')
  await expect(page.getByText('为你推荐', { exact: true })).toBeVisible()
  await expect(page.getByRole('button', { name: '使用位置优化' })).toBeVisible()
  await expect(page.getByText(homeRecommendedActivity.title)).toBeVisible()
  await expect(page.getByText('匹配你的兴趣：周末、新手友好')).toBeVisible()

  await page.getByPlaceholder('搜索活动、地点、说明').fill('飞盘')
  await page.getByPlaceholder('搜索活动、地点、说明').press('Enter')
  await expect(page.getByText(homeRecommendedActivity.title)).toBeVisible()
  await page.getByText('运动', { exact: true }).first().click()
  await expect(page.getByText(homeRecommendedActivity.title)).toBeVisible()
  await expect.poll(() => recommendationRequests.at(-1)?.searchParams.get('category')).toBe('运动')

  await page.getByRole('button', { name: '使用位置优化' }).click()
  await expect(page.locator('.van-toast')).toContainText('定位权限未开启')
  await expect(page.getByText(homeRecommendedActivity.title)).toBeVisible()
  await expect(page.getByText('活动发现', { exact: true })).toBeVisible()

  await page.locator('.recommendation-item .activity-card').click()
  await expect(page).toHaveURL('/activities/901')
})

test('home location optimization keeps recommendations when geolocation is unavailable', async ({ page }) => {
  await silenceBrowserDefaultRequests(page)
  await mockHomeBrowserEnvironment(page, { geolocation: 'unavailable' })
  await mockRecommendationHomeApis(page)
  await seedRecommendationSession(page)

  await page.goto('/')
  await expect(page.getByText(homeRecommendedActivity.title)).toBeVisible()
  await page.getByRole('button', { name: '使用位置优化' }).click()

  await expect(page.locator('.van-toast')).toContainText('当前浏览器不支持定位')
  await expect(page.getByText(homeRecommendedActivity.title)).toBeVisible()
  await expect(page.getByText('活动发现', { exact: true })).toBeVisible()
})

test('home location optimization explains insecure contexts without calling geolocation', async ({ page }) => {
  await silenceBrowserDefaultRequests(page)
  await mockHomeBrowserEnvironment(page, { secureContext: false })
  await mockRecommendationHomeApis(page)
  await seedRecommendationSession(page)

  await page.goto('/')
  await page.getByRole('button', { name: '使用位置优化' }).click()

  await expect(page.locator('.van-toast')).toContainText('当前访问环境不支持设备定位，请使用 HTTPS 后重试')
  await expect(page.getByText(homeRecommendedActivity.title)).toBeVisible()
  await expect(page.getByText('活动发现', { exact: true })).toBeVisible()
  expect(await page.evaluate(() => window.__geolocationCallCount)).toBe(0)
})

test('home location optimization sends longitude and latitude to recommendations', async ({ page }) => {
  await silenceBrowserDefaultRequests(page)
  const recommendationRequests = []
  await mockHomeBrowserEnvironment(page, { geolocation: 'success' })
  await mockRecommendationHomeApis(page, (url) => recommendationRequests.push(url))
  await seedRecommendationSession(page)

  await page.goto('/')
  await expect(page.getByText(homeRecommendedActivity.title)).toBeVisible()
  await page.getByRole('button', { name: '使用位置优化' }).click()

  await expect.poll(() => recommendationRequests.length).toBe(2)
  expect(recommendationRequests[0].searchParams.has('longitude')).toBeFalsy()
  expect(recommendationRequests[0].searchParams.has('latitude')).toBeFalsy()
  expect(recommendationRequests[1].searchParams.get('longitude')).toBe('116.4074')
  expect(recommendationRequests[1].searchParams.get('latitude')).toBe('39.9042')
  await expect(page.getByText('距你约 0.42 km')).toBeVisible()
  await expect(page.getByText(homeRecommendedActivity.title)).toBeVisible()
  await expect(page.getByText('活动发现', { exact: true })).toBeVisible()
})

test('admin dashboard, analytics ranges and user route guard', async ({ page, request }) => {
  const consoleMessages = watchConsole(page)
  await silenceBrowserDefaultRequests(page)
  const adminSession = await login(request, 'admin')
  await seedSession(page, adminSession)

  await page.goto('/admin/dashboard')
  await expect(page).toHaveURL(/\/admin\/dashboard/)
  await expect(page.locator('canvas').first()).toBeVisible()
  await expect(page.locator('.el-table, table').first()).toBeVisible()

  await page.goto('/admin/analytics')
  await expect(page).toHaveURL(/\/admin\/analytics/)
  await expect(page.locator('.admin-grid-two .admin-panel')).toHaveCount(9)
  await expect.poll(async () => page.locator('canvas').count()).toBeGreaterThan(0)

  await page.locator('label:has(input[value="LAST_90_DAYS"])').click()
  await expect(page.locator('.analytics-range')).toContainText('2026')
  await page.locator('label:has(input[value="THIS_YEAR"])').click()
  await expect(page.locator('.analytics-range')).toContainText('2026-01-01')
  await page.locator('label:has(input[value="CUSTOM"])').click()
  const dateInputs = page.locator('.el-date-editor input')
  await dateInputs.nth(0).fill('2026-04-01')
  await dateInputs.nth(1).fill('2026-07-12')
  await page.keyboard.press('Enter')
  await expect(page.locator('.analytics-range')).toContainText('2026-04-01')

  const userSession = await login(request, 'user')
  const userPage = await page.context().newPage()
  const userConsoleMessages = watchConsole(userPage)
  await silenceBrowserDefaultRequests(userPage)
  await seedSession(userPage, userSession)
  await userPage.goto('/admin/dashboard')
  await expect(userPage).not.toHaveURL(/\/admin\/dashboard/)
  expect(userConsoleMessages).toEqual([])
  await userPage.close()
  expect(consoleMessages).toEqual([])
})

test('registration slider produces a one-time token used by register request', async ({ page }) => {
  await silenceBrowserDefaultRequests(page)
  let verificationPayload = null
  let registrationPayload = null
  const pixel =
    'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII='

  await page.route(/^https?:\/\/[^/]+\/api\//, async (route) => {
    const request = route.request()
    const pathname = new URL(request.url()).pathname
    let data = { records: [], total: 0 }
    if (pathname === '/api/auth/register-captcha/challenge') {
      data = {
        challengeId: 'a'.repeat(32),
        backgroundImage: pixel,
        sliderImage: pixel,
        imageWidth: 320,
        imageHeight: 160,
        sliderWidth: 44,
        sliderHeight: 44,
        sliderY: 50,
        expiresInSeconds: 120
      }
    } else if (pathname === '/api/auth/register-captcha/verify') {
      verificationPayload = request.postDataJSON()
      data = { captchaToken: 'b'.repeat(32), expiresInSeconds: 300 }
    } else if (pathname === '/api/auth/register') {
      registrationPayload = request.postDataJSON()
      data = {
        token: 'registration-session-token',
        user: { id: 99, username: 'slider_user', nickname: '滑块用户', role: 'USER', city: '北京' }
      }
    } else if (pathname === '/api/notices/unread-count') {
      data = 0
    }
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ code: 200, data })
    })
  })

  await page.goto('/register')
  await page.locator('.van-field', { hasText: '账号' }).locator('input').fill('slider_user')
  await page.locator('.van-field', { hasText: '密码' }).locator('input').fill('SafePassword123')
  const slider = page.getByTestId('registration-captcha-slider')
  await expect(slider).toBeVisible()
  await slider.evaluate(async (element) => {
    element.dispatchEvent(new PointerEvent('pointerdown', { bubbles: true }))
    for (const value of [60, 130, 210]) {
      element.value = String(value)
      element.dispatchEvent(new Event('input', { bubbles: true }))
    }
    await new Promise((resolve) => setTimeout(resolve, 450))
    element.dispatchEvent(new Event('change', { bubbles: true }))
  })
  await expect(page.getByText('验证通过', { exact: true })).toBeVisible()
  await page.getByRole('button', { name: '注册并登录' }).click()
  await expect(page).toHaveURL('/')

  expect(verificationPayload.challengeId).toBe('a'.repeat(32))
  expect(verificationPayload.trace.length).toBeGreaterThanOrEqual(3)
  expect(registrationPayload.captchaToken).toBe('b'.repeat(32))
  expect(registrationPayload.captchaCode).toBeUndefined()
})

test('map location keeps browser coordinates, map center and geocoded city in sync', async ({ page }) => {
  await silenceBrowserDefaultRequests(page)
  await installMockAmap(page)
  await page.addInitScript(() => {
    Object.defineProperty(navigator, 'geolocation', {
      configurable: true,
      value: {
        getCurrentPosition(success) {
          success({ coords: { longitude: 113.264385, latitude: 23.129112 } })
        }
      }
    })
  })
  let nearbyRequest = null
  await page.route(/^https?:\/\/[^/]+\/api\//, async (route) => {
    const url = new URL(route.request().url())
    if (url.pathname === '/api/activities/nearby') {
      nearbyRequest = url
    }
    const data = url.pathname === '/api/notices/unread-count'
      ? 0
      : { records: [], total: 0 }
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ code: 200, data })
    })
  })
  await seedSession(page, {
    token: 'map-user-token',
    user: { id: 6, username: 'map_user', nickname: '地图用户', role: 'USER', city: '北京' }
  })

  await page.goto('/map')
  await expect(page.getByText('已定位到当前位置，城市：广州市')).toBeVisible()
  await expect.poll(() => nearbyRequest?.searchParams.get('longitude')).toBe('113.264385')
  expect(nearbyRequest.searchParams.get('latitude')).toBe('23.129112')
  expect(await page.evaluate(() => window.__mapCenter)).toEqual([113.264385, 23.129112])
})

test('publish location picker shows city-scoped suggestions and clears stale point after city change', async ({ page }) => {
  await silenceBrowserDefaultRequests(page)
  await installMockAmap(page)
  await page.route(/^https?:\/\/[^/]+\/api\//, async (route) => {
    const data = new URL(route.request().url()).pathname === '/api/notices/unread-count'
      ? 0
      : { records: [], total: 0 }
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ code: 200, data })
    })
  })
  await seedSession(page, {
    token: 'picker-user-token',
    user: { id: 7, username: 'picker_user', nickname: '选点用户', role: 'USER', city: '广州' }
  })

  await page.goto('/publish')
  const cityInput = page.locator('.van-field', { hasText: '城市' }).first().locator('input')
  await cityInput.fill('广州')
  await page.getByRole('button', { name: '选择地点' }).click()
  await page.getByPlaceholder('搜索地点或地址').fill('万达')
  await expect(page.getByText('广州万达广场', { exact: true })).toBeVisible()
  await page.getByText('广州万达广场', { exact: true }).click()
  await expect.poll(() => page.evaluate(() => window.__autocompleteCity)).toBe('广州')
  await page.locator('.location-picker').getByText('确定', { exact: true }).click()

  const addressInput = page.locator('.van-field', { hasText: '地址' }).first().locator('input')
  const longitudeInput = page.locator('.van-field', { hasText: '经度' }).first().locator('input')
  await expect(addressInput).toHaveValue(/广州万达广场/)
  await expect(longitudeInput).toHaveValue('113.327')

  await cityInput.fill('深圳')
  await expect(addressInput).toHaveValue('')
  await expect(longitudeInput).toHaveValue('')
  await expect(page.locator('.van-toast')).toContainText('城市已变更')
})

async function installMockAmap(page) {
  await page.addInitScript(() => {
    class MockMap {
      constructor() {
        window.__mapCenter = null
      }
      on() {}
      add() {}
      remove() {}
      setCenter(center) {
        window.__mapCenter = Array.from(center)
      }
    }
    class MockMarker {
      constructor(options) {
        this.position = options.position
      }
      on() {}
      setPosition(position) {
        this.position = position
      }
      getPosition() {
        return this.position
      }
    }
    class MockGeocoder {
      getAddress(position, callback) {
        callback('complete', {
          regeocode: {
            formattedAddress: '广东省广州市天河区测试地址',
            addressComponent: { city: '广州市', province: '广东省' }
          }
        })
      }
    }
    class MockAutoComplete {
      constructor(options) {
        window.__autocompleteCity = options.city
      }
      setCity(city) {
        window.__autocompleteCity = city
      }
      search(keyword, callback) {
        callback('complete', {
          tips: [{
            id: 'mock-poi-1',
            name: '广州万达广场',
            district: '广州市天河区',
            address: '天河路',
            cityname: '广州',
            location: { lng: 113.327, lat: 23.132 }
          }]
        })
      }
    }
    class MockPlaceSearch {
      constructor(options) {
        this.city = options.city
      }
      setCity(city) {
        this.city = city
      }
      search(keyword, callback) {
        callback('complete', {
          poiList: {
            pois: [{
              id: 'mock-poi-1',
              name: '广州万达广场',
              address: '天河路',
              cityname: '广州',
              location: { lng: 113.327, lat: 23.132 }
            }]
          }
        })
      }
    }
    class MockInfoWindow {
      setContent() {}
      open() {}
    }
    window.AMap = {
      Map: MockMap,
      Marker: MockMarker,
      Geocoder: MockGeocoder,
      AutoComplete: MockAutoComplete,
      PlaceSearch: MockPlaceSearch,
      InfoWindow: MockInfoWindow,
      Pixel: class {}
    }
  })
}

test('image cropper submits and removes activity covers and profile avatars', async ({ page }) => {
  await silenceBrowserDefaultRequests(page)
  const activityRequests = []
  const profileRequests = []
  let profileFailuresRemaining = 1
  let currentAvatar = null
  const activity = {
    id: 501,
    creatorId: 1,
    title: 'Image Cropper Activity',
    category: '观影',
    tags: ['图片测试'],
    startTime: '2026-08-01T19:00:00',
    endTime: '2026-08-01T22:00:00',
    signupDeadline: '2026-08-01T12:00:00',
    city: '北京',
    address: '图片测试地点',
    longitude: 116.4,
    latitude: 39.9,
    minParticipants: 2,
    maxParticipants: 6,
    costType: 'FREE',
    costAmount: 0,
    aaRule: '',
    coverUrl: '/uploads/activity/existing.jpg',
    description: '图片裁剪端到端测试',
    notes: '',
    needApproval: false,
    approvedCount: 0,
    status: 'SIGNING',
    creator: { id: 1, nickname: 'Image User', avatarUrl: null }
  }

  await page.route(/^https?:\/\/[^/]+\/api\//, async (route) => {
    const request = route.request()
    const pathname = new URL(request.url()).pathname
    const method = request.method()
    let data = { records: [], total: 0 }

    if (pathname === '/api/notices/unread-count') {
      data = 0
    } else if (pathname === '/api/activities' && method === 'POST') {
      activityRequests.push(request.postDataBuffer())
      data = { ...activity, coverUrl: '/uploads/activity/new-cover.jpg' }
    } else if (pathname === '/api/activities/501' && method === 'PUT') {
      activityRequests.push(request.postDataBuffer())
      data = { ...activity, coverUrl: null }
    } else if (pathname === '/api/activities/501') {
      data = activity
    } else if (pathname === '/api/user/profile' && method === 'PUT') {
      profileRequests.push(request.postDataBuffer())
      if (profileFailuresRemaining > 0) {
        profileFailuresRemaining -= 1
        await route.fulfill({
          status: 500,
          contentType: 'application/json',
          body: JSON.stringify({ code: 500, message: 'mock profile save failure', data: null })
        })
        return
      }
      const body = request.postDataBuffer()?.toString('latin1') || ''
      currentAvatar = body.includes('removeAvatar') ? null : '/uploads/avatar/new-avatar.jpg'
      data = userData(currentAvatar)
    } else if (pathname === '/api/user/me') {
      data = userData(currentAvatar)
    }

    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ code: 200, data })
    })
  })

  await seedSession(page, {
    token: 'image-feature-token',
    user: userData(null)
  })

  await page.goto('/publish')
  await page.locator('.van-field', { hasText: '标题' }).locator('input').fill(activity.title)
  await page.locator('.van-field', { hasText: '地址' }).locator('input').fill(activity.address)
  await page.locator('.van-field', { hasText: '经度' }).locator('input').fill('116.4')
  await page.locator('.van-field', { hasText: '纬度' }).locator('input').fill('39.9')
  await page.locator('.van-field', { hasText: '说明' }).locator('textarea').fill(activity.description)
  await page.locator('input[type="file"]').setInputFiles({
    name: 'unsupported.gif',
    mimeType: 'image/gif',
    buffer: Buffer.from('GIF89a')
  })
  await expect(page.locator('.van-toast')).toContainText('仅支持 JPG、PNG、WebP')
  await page.locator('input[type="file"]').setInputFiles({
    name: 'too-large.png',
    mimeType: 'image/png',
    buffer: Buffer.alloc(10 * 1024 * 1024 + 1)
  })
  await expect(page.locator('.van-toast')).toContainText('原图不能超过 10MB')
  await page.locator('input[type="file"]').setInputFiles(testPngFile('cover.png'))
  await expect(page.locator('cropper-selection')).toBeVisible()
  await page.locator('.van-nav-bar__right').getByText('确定', { exact: true }).click()
  await expect(page.locator('.activity-cover-preview')).toHaveAttribute('src', /^blob:/)
  const firstCoverPreview = await page.locator('.activity-cover-preview').getAttribute('src')
  await page.locator('input[type="file"]').setInputFiles(testPngFile('replacement-cover.png'))
  await expect(page.locator('cropper-selection')).toBeVisible()
  await page.locator('.van-nav-bar__left').getByText('取消', { exact: true }).click()
  await expect(page.locator('.activity-cover-preview')).toHaveAttribute('src', firstCoverPreview)
  await page.locator('input[type="file"]').setInputFiles(testPngFile('replacement-cover.png'))
  await expect(page.locator('cropper-selection')).toBeVisible()
  await page.locator('.van-nav-bar__right').getByText('确定', { exact: true }).click()
  await expect.poll(() => page.locator('.activity-cover-preview').getAttribute('src')).not.toBe(firstCoverPreview)
  await page.locator('form .van-button--block').click()
  await expect.poll(() => activityRequests.length).toBe(1)
  const createBody = activityRequests[0].toString('latin1')
  expect(createBody).toContain('activity-cover.jpg')
  expect(createBody).toContain('Image Cropper Activity')

  await page.goto('/publish?editId=501')
  await expect(page.locator('.activity-cover-preview')).toHaveAttribute('src', activity.coverUrl)
  await page.getByRole('button', { name: '删除图片' }).click()
  await page.locator('form .van-button--block').click()
  await expect.poll(() => activityRequests.length).toBe(2)
  expect(activityRequests[1].toString('latin1')).toContain('removeCover')

  await page.goto('/profile/edit')
  await page.locator('input[type="file"]').setInputFiles(testPngFile('avatar.png'))
  await expect(page.locator('cropper-selection')).toBeVisible()
  await page.locator('.van-nav-bar__right').getByText('确定', { exact: true }).click()
  await expect(page.locator('.avatar-editor-preview')).toHaveAttribute('src', /^blob:/)
  await page.locator('form .van-button--block').click()
  await expect.poll(() => profileRequests.length).toBe(1)
  expect(profileRequests[0].toString('latin1')).toContain('avatar.jpg')
  await expect(page.locator('.avatar-editor-preview')).toHaveAttribute('src', /^blob:/)
  await page.locator('form .van-button--block').click()
  await expect.poll(() => profileRequests.length).toBe(2)
  expect(profileRequests[1].toString('latin1')).toContain('avatar.jpg')

  await page.getByRole('button', { name: '删除头像' }).click()
  await page.locator('form .van-button--block').click()
  await expect.poll(() => profileRequests.length).toBe(3)
  expect(profileRequests[2].toString('latin1')).toContain('removeAvatar')
})

function userData(avatarUrl) {
  return {
    id: 1,
    username: 'image_user',
    nickname: 'Image User',
    role: 'USER',
    city: '北京',
    bio: 'Image feature test user',
    interestTags: [],
    avatarUrl
  }
}

function testPngFile(name) {
  return {
    name,
    mimeType: 'image/png',
    buffer: Buffer.from(
      'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=',
      'base64'
    )
  }
}
