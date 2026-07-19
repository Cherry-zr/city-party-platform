import { createHash } from 'node:crypto'
import { copyFile, mkdir, mkdtemp, rm, writeFile } from 'node:fs/promises'
import os from 'node:os'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { inflateSync } from 'node:zlib'
import { chromium, request as playwrightRequest } from '@playwright/test'

const frontendDir = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
const repositoryDir = path.resolve(frontendDir, '..')
const outputDir = path.join(repositoryDir, 'screenshots', 'showcase')
const baseURL = process.env.PLAYWRIGHT_BASE_URL || 'http://127.0.0.1:5173'
const apiBaseURL = process.env.SHOWCASE_API_BASE_URL || 'http://127.0.0.1:8080'
const password = process.env.SHOWCASE_PASSWORD
const browserChannel = process.env.SHOWCASE_BROWSER_CHANNEL || 'chrome'
const mobileViewport = { width: 430, height: 932 }
const desktopViewport = { width: 1440, height: 900 }
const blockedShowcaseText = /Stage\s*1\.1|Acceptance|stage1-user|CP_E2E/i
const stagingDir = await mkdtemp(path.join(os.tmpdir(), 'cityparty-showcase-capture-'))
const stagedFiles = []

if (!password) {
  throw new Error('Missing SHOWCASE_PASSWORD. Provide the local showcase account password through the process environment.')
}

function redact(value) {
  return String(value || '')
    .replace(/([?&](?:key|jscode|token|code)=)[^&\s]+/gi, '$1[redacted]')
    .replace(/Bearer\s+[A-Za-z0-9._~-]+/gi, 'Bearer [redacted]')
    .replace(/[A-Za-z0-9_-]{20,}\.[A-Za-z0-9_-]{20,}\.[A-Za-z0-9_-]{20,}/g, '[redacted-token]')
}

function sanitizeUrl(rawUrl) {
  try {
    const url = new URL(rawUrl)
    return `${url.origin}${url.pathname}`
  } catch {
    return redact(rawUrl)
  }
}

function isCriticalRequest(request) {
  const type = request.resourceType()
  if (!['document', 'stylesheet', 'script', 'font', 'image', 'xhr', 'fetch'].includes(type)) return false
  try {
    const url = new URL(request.url())
    return ['127.0.0.1', 'localhost'].includes(url.hostname)
      || url.hostname === 'amap.com'
      || url.hostname.endsWith('.amap.com')
  } catch {
    return false
  }
}

async function apiData(api, method, pathname, options = {}) {
  const response = await api.fetch(`${apiBaseURL}${pathname}`, { method, ...options })
  let payload
  try {
    payload = await response.json()
  } catch {
    throw new Error(`${method} ${pathname} returned a non-JSON response (${response.status()}).`)
  }
  if (!response.ok() || payload.code !== 200) {
    throw new Error(`${method} ${pathname} failed with HTTP ${response.status()} / business code ${payload.code}.`)
  }
  return payload.data
}

async function login(api, username) {
  const captcha = await apiData(api, 'GET', '/api/auth/captcha')
  const session = await apiData(api, 'POST', '/api/auth/login', {
    data: {
      username,
      password,
      captchaKey: captcha.captchaKey,
      captchaCode: captcha.captchaText
    }
  })
  if (!session?.token || !session?.user) throw new Error(`Login did not return a complete session for ${username}.`)
  return session
}

async function findActivityId(api, title) {
  const result = await apiData(api, 'GET', `/api/activities?current=1&size=50&keyword=${encodeURIComponent(title)}`)
  const activity = (result.records || []).find((item) => item.title === title)
  if (!activity) throw new Error(`Showcase activity not found: ${title}`)
  return activity.id
}

function watchPage(page, label) {
  const pending = new Set()
  const diagnostics = []

  page.on('request', (request) => {
    if (isCriticalRequest(request)) pending.add(request)
  })
  page.on('requestfinished', (request) => pending.delete(request))
  page.on('requestfailed', (request) => {
    pending.delete(request)
    if (!isCriticalRequest(request)) return
    const failure = request.failure()?.errorText || 'unknown request failure'
    if (/ERR_ABORTED/i.test(failure) && /\.amap\.com$/i.test(new URL(request.url()).hostname)) return
    diagnostics.push(`${label}: request failed: ${sanitizeUrl(request.url())} (${redact(failure)})`)
  })
  page.on('response', (response) => {
    if (response.status() < 400 || !isCriticalRequest(response.request())) return
    diagnostics.push(`${label}: HTTP ${response.status()}: ${sanitizeUrl(response.url())}`)
  })
  page.on('console', (message) => {
    if (message.type() === 'error') {
      const location = message.location().url ? ` at ${sanitizeUrl(message.location().url)}` : ''
      diagnostics.push(`${label}: console error: ${redact(message.text())}${location}`)
    }
  })
  page.on('pageerror', (error) => diagnostics.push(`${label}: page error: ${redact(error.message)}`))

  return { pending, diagnostics }
}

async function waitForPendingRequests(pending, timeoutMs = 15_000) {
  const deadline = Date.now() + timeoutMs
  let quietSince = null
  while (Date.now() < deadline) {
    if (pending.size === 0) {
      quietSince ??= Date.now()
      if (Date.now() - quietSince >= 600) return
    } else {
      quietSince = null
    }
    await new Promise((resolve) => setTimeout(resolve, 100))
  }
  throw new Error(`Critical network requests did not settle; ${pending.size} request(s) still pending.`)
}

async function waitForDocumentReadiness(page, { minimumImages = 0 } = {}) {
  await page.waitForFunction(async ({ minimumImages }) => {
    if (document.fonts?.ready) await document.fonts.ready
    const visible = (element) => {
      const rect = element.getBoundingClientRect()
      const style = getComputedStyle(element)
      return rect.width > 0 && rect.height > 0 && style.visibility !== 'hidden' && style.display !== 'none'
    }
    const loaders = [...document.querySelectorAll('.van-loading, .el-loading-mask')].filter(visible)
    const images = [...document.images]
    const invalidImages = images.filter((image) => !image.complete || image.naturalWidth <= 0 || image.naturalHeight <= 0)
    return document.readyState === 'complete'
      && loaders.length === 0
      && images.length >= minimumImages
      && invalidImages.length === 0
  }, { minimumImages }, { timeout: 30_000 })
}

async function assertImageDimensions(page) {
  const invalid = await page.evaluate(() => [...document.images].flatMap((image) => {
    const isCover = image.classList.contains('activity-cover')
    const isShowcaseAvatar = image.currentSrc.includes('/showcase/avatars/')
    const minWidth = isCover ? 1000 : isShowcaseAvatar ? 200 : 1
    const minHeight = isCover ? 400 : isShowcaseAvatar ? 200 : 1
    if (image.complete && image.naturalWidth >= minWidth && image.naturalHeight >= minHeight) return []
    return [{
      src: new URL(image.currentSrc || image.src, document.baseURI).pathname,
      width: image.naturalWidth,
      height: image.naturalHeight
    }]
  }))
  if (invalid.length) throw new Error(`Invalid image dimensions: ${JSON.stringify(invalid)}`)
}

async function assertNoBlockedTextInViewport(page) {
  const visibleText = await page.evaluate(() => {
    const parts = []
    const walker = document.createTreeWalker(document.body, NodeFilter.SHOW_TEXT)
    while (walker.nextNode()) {
      const node = walker.currentNode
      const parent = node.parentElement
      if (!parent || !node.textContent?.trim()) continue
      const rect = parent.getBoundingClientRect()
      const style = getComputedStyle(parent)
      if (rect.bottom <= 0 || rect.top >= innerHeight || rect.right <= 0 || rect.left >= innerWidth) continue
      if (style.display === 'none' || style.visibility === 'hidden' || Number(style.opacity) === 0) continue
      parts.push(node.textContent.trim())
    }
    return parts.join(' ')
  })
  if (blockedShowcaseText.test(visibleText)) throw new Error('Acceptance-test text is visible in the screenshot viewport.')
}

async function assertChartCanvases(page, minimumCount) {
  const charts = await page.evaluate(() => [...document.querySelectorAll('canvas')].flatMap((canvas) => {
    const rect = canvas.getBoundingClientRect()
    if (rect.width < 100 || rect.height < 80) return []
    const context = canvas.getContext('2d')
    if (!context) return [{ width: rect.width, height: rect.height, uniqueColors: 0 }]
    const pixels = context.getImageData(0, 0, canvas.width, canvas.height).data
    const colors = new Set()
    const step = Math.max(4, Math.floor(pixels.length / 4000 / 4) * 4)
    for (let index = 0; index < pixels.length; index += step) {
      if (pixels[index + 3] < 10) continue
      colors.add(`${pixels[index] >> 4},${pixels[index + 1] >> 4},${pixels[index + 2] >> 4}`)
      if (colors.size > 16) break
    }
    return [{ width: rect.width, height: rect.height, uniqueColors: colors.size }]
  }))
  if (charts.length < minimumCount) throw new Error(`Expected at least ${minimumCount} rendered chart canvas(es), found ${charts.length}.`)
  if (charts.some((chart) => chart.uniqueColors < 4)) throw new Error(`Blank chart canvas detected: ${JSON.stringify(charts)}.`)
}

function paeth(a, b, c) {
  const p = a + b - c
  const pa = Math.abs(p - a)
  const pb = Math.abs(p - b)
  const pc = Math.abs(p - c)
  return pa <= pb && pa <= pc ? a : pb <= pc ? b : c
}

function decodePng(buffer) {
  const signature = buffer.subarray(0, 8).toString('hex')
  if (signature !== '89504e470d0a1a0a') throw new Error('Unexpected screenshot format; PNG required.')
  let offset = 8
  let width
  let height
  let bitDepth
  let colorType
  const idat = []
  while (offset < buffer.length) {
    const length = buffer.readUInt32BE(offset)
    const type = buffer.subarray(offset + 4, offset + 8).toString('ascii')
    const data = buffer.subarray(offset + 8, offset + 8 + length)
    if (type === 'IHDR') {
      width = data.readUInt32BE(0)
      height = data.readUInt32BE(4)
      bitDepth = data[8]
      colorType = data[9]
    } else if (type === 'IDAT') {
      idat.push(data)
    } else if (type === 'IEND') {
      break
    }
    offset += length + 12
  }
  if (bitDepth !== 8 || ![2, 6].includes(colorType)) {
    throw new Error(`Unsupported PNG format: bit depth ${bitDepth}, color type ${colorType}.`)
  }
  const bytesPerPixel = colorType === 6 ? 4 : 3
  const stride = width * bytesPerPixel
  const raw = inflateSync(Buffer.concat(idat))
  const pixels = Buffer.alloc(stride * height)
  let sourceOffset = 0
  for (let y = 0; y < height; y += 1) {
    const filter = raw[sourceOffset]
    sourceOffset += 1
    for (let x = 0; x < stride; x += 1) {
      const value = raw[sourceOffset + x]
      const left = x >= bytesPerPixel ? pixels[y * stride + x - bytesPerPixel] : 0
      const up = y > 0 ? pixels[(y - 1) * stride + x] : 0
      const upperLeft = y > 0 && x >= bytesPerPixel ? pixels[(y - 1) * stride + x - bytesPerPixel] : 0
      let decoded
      if (filter === 0) decoded = value
      else if (filter === 1) decoded = value + left
      else if (filter === 2) decoded = value + up
      else if (filter === 3) decoded = value + Math.floor((left + up) / 2)
      else if (filter === 4) decoded = value + paeth(left, up, upperLeft)
      else throw new Error(`Unsupported PNG filter ${filter}.`)
      pixels[y * stride + x] = decoded & 0xff
    }
    sourceOffset += stride
  }
  return { width, height, bytesPerPixel, pixels }
}

function pngColorStats(buffer) {
  const decoded = decodePng(buffer)
  const colors = new Set()
  const luminances = []
  let nonGray = 0
  const xStep = Math.max(1, Math.floor(decoded.width / 60))
  const yStep = Math.max(1, Math.floor(decoded.height / 40))
  for (let y = 0; y < decoded.height; y += yStep) {
    for (let x = 0; x < decoded.width; x += xStep) {
      const index = (y * decoded.width + x) * decoded.bytesPerPixel
      const red = decoded.pixels[index]
      const green = decoded.pixels[index + 1]
      const blue = decoded.pixels[index + 2]
      const alpha = decoded.bytesPerPixel === 4 ? decoded.pixels[index + 3] : 255
      if (alpha < 128) continue
      colors.add(`${red >> 3},${green >> 3},${blue >> 3}`)
      luminances.push(0.2126 * red + 0.7152 * green + 0.0722 * blue)
      if (Math.max(red, green, blue) - Math.min(red, green, blue) >= 8) nonGray += 1
    }
  }
  const average = luminances.reduce((sum, value) => sum + value, 0) / Math.max(1, luminances.length)
  const variance = luminances.reduce((sum, value) => sum + ((value - average) ** 2), 0) / Math.max(1, luminances.length)
  return {
    width: decoded.width,
    height: decoded.height,
    uniqueColors: colors.size,
    luminanceDeviation: Math.sqrt(variance),
    nonGrayRatio: nonGray / Math.max(1, luminances.length)
  }
}

async function assertMapRendered(page) {
  const map = page.locator('.activity-map-container')
  await map.waitFor({ state: 'visible', timeout: 30_000 })
  await page.waitForFunction(() => {
    const container = document.querySelector('.activity-map-container')
    const canvas = container?.querySelector('canvas')
    const markerCount = container?.querySelectorAll('.amap-marker').length || 0
    const text = document.querySelector('.map-filter-panel .activity-meta')?.textContent || ''
    return canvas && canvas.getBoundingClientRect().width > 300 && canvas.getBoundingClientRect().height > 200
      && markerCount >= 6 && text.includes('已定位到当前位置') && !text.includes('定位失败')
  }, null, { timeout: 30_000 })
  const mapBuffer = await map.screenshot({ animations: 'disabled' })
  const stats = pngColorStats(mapBuffer)
  if (stats.uniqueColors < 120 || stats.luminanceDeviation < 16 || stats.nonGrayRatio < 0.12) {
    throw new Error(`Map pixel validation failed: ${JSON.stringify(stats)}.`)
  }
}

async function waitForVisualStability(page) {
  let previousHash = ''
  for (let attempt = 0; attempt < 8; attempt += 1) {
    const buffer = await page.screenshot({ animations: 'disabled', caret: 'hide' })
    const currentHash = createHash('sha256').update(buffer).digest('hex')
    if (currentHash === previousHash) return buffer
    previousHash = currentHash
    await page.waitForTimeout(700)
  }
  throw new Error('Viewport did not remain visually stable across two consecutive checks.')
}

async function createContext(browser, viewport, session) {
  const context = await browser.newContext({
    baseURL,
    viewport,
    deviceScaleFactor: 1,
    colorScheme: 'light',
    locale: 'zh-CN',
    timezoneId: 'Asia/Shanghai',
    geolocation: { longitude: 116.4074, latitude: 39.9042, accuracy: 20 }
  })
  await context.route('**/favicon.ico', (route) => route.fulfill({ status: 204, body: '' }))
  await context.grantPermissions(['geolocation'], { origin: new URL(baseURL).origin })
  await context.addInitScript(({ token, user }) => {
    localStorage.setItem('token', token)
    localStorage.setItem('user', JSON.stringify(user))
  }, session)
  return context
}

async function installShowcaseListFilter(context) {
  await context.route('**/api/activities**', async (route) => {
    const request = route.request()
    const url = new URL(request.url())
    const isListRequest = request.method() === 'GET'
      && ['/api/activities', '/api/activities/nearby'].includes(url.pathname)
    if (!isListRequest) {
      await route.continue()
      return
    }

    const response = await route.fetch()
    const payload = await response.json()
    if (response.ok() && payload?.code === 200 && Array.isArray(payload.data?.records)) {
      const records = payload.data.records.filter((item) => item.coverUrl?.startsWith('/showcase/covers/'))
      payload.data = { ...payload.data, records, total: records.length, pages: records.length ? 1 : 0, current: 1 }
    }
    await route.fulfill({ response, json: payload })
  })
}

async function installAdminShowcaseActivityFilter(context, activityIds) {
  const allowedIds = new Set(activityIds.map(String))
  await context.route('**/api/admin/activities**', async (route) => {
    const request = route.request()
    const url = new URL(request.url())
    if (request.method() !== 'GET' || url.pathname !== '/api/admin/activities') {
      await route.continue()
      return
    }

    const response = await route.fetch()
    const payload = await response.json()
    if (response.ok() && payload?.code === 200 && Array.isArray(payload.data?.records)) {
      const records = payload.data.records.filter((item) => allowedIds.has(String(item.id)))
      payload.data = { ...payload.data, records, total: records.length, pages: records.length ? 1 : 0, current: 1 }
    }
    await route.fulfill({ response, json: payload })
  })
}

function dateLabel(date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function trendPoints(days, valuesByOffset) {
  const today = new Date()
  today.setHours(12, 0, 0, 0)
  return Array.from({ length: days }, (_, index) => {
    const offset = days - index - 1
    const date = new Date(today)
    date.setDate(date.getDate() - offset)
    return { label: dateLabel(date), value: valuesByOffset[offset] || 0 }
  })
}

async function installShowcaseDashboardData(context) {
  await context.route('**/api/admin/dashboard/**', async (route) => {
    const request = route.request()
    const url = new URL(request.url())
    if (request.method() !== 'GET') {
      await route.continue()
      return
    }

    const days = url.searchParams.get('period') === 'LAST_7_DAYS' ? 7 : 30
    const users = trendPoints(days, { 28: 1, 21: 1, 14: 1, 10: 1, 7: 1, 4: 1, 2: 1, 1: 1 })
    const activities = trendPoints(days, { 0: 6 })
    const signups = trendPoints(days, { 5: 4, 4: 3, 3: 5, 2: 6, 1: 4, 0: 6 })
    const reviews = trendPoints(days, { 2: 2, 1: 2 })
    let data

    if (url.pathname === '/api/admin/dashboard/overview') {
      data = {
        userCount: 8, activityCount: 6, signupCount: 28, reviewCount: 4,
        todayUsers: 0, todayActivities: 6, todaySignups: 6, todayReviews: 0
      }
    } else if (url.pathname === '/api/admin/dashboard/trends') {
      data = {
        startDate: users[0].label,
        endDate: users.at(-1).label,
        users,
        activities,
        signups,
        reviews
      }
    } else if (url.pathname === '/api/admin/dashboard/distributions') {
      data = {
        signupStatuses: [{ label: '报名成功', value: 27 }, { label: '候补中', value: 1 }],
        activityStatuses: [
          { label: '报名中', value: 3 }, { label: '已满员', value: 1 },
          { label: '即将开始', value: 1 }, { label: '已结束', value: 1 }
        ],
        categories: [
          { label: '运动', value: 2 }, { label: '桌游', value: 1 }, { label: '户外', value: 1 },
          { label: '观影', value: 1 }, { label: '学习', value: 1 }
        ],
        credits: [
          { label: '90–99', value: 3 }, { label: '100–109', value: 4 }, { label: '110–120', value: 1 }
        ],
        ratings: [{ label: '4 星', value: 1 }, { label: '5 星', value: 3 }]
      }
    } else if (url.pathname === '/api/admin/dashboard/quality') {
      data = {
        signupSuccessRate: 96.43,
        averageParticipationRate: 52.92,
        averageRating: 4.75,
        waitlistCount: 1,
        exitCount: 0,
        abnormalCreditUserCount: 0
      }
    } else if (url.pathname === '/api/admin/dashboard/popular-activities') {
      data = [
        { activityId: null, title: '东城桌游新手局', successfulSignups: 5, waitlistCount: 1 },
        { activityId: null, title: '周末电影观影交流', successfulSignups: 5, waitlistCount: 0 },
        { activityId: null, title: '城市摄影漫步', successfulSignups: 4, waitlistCount: 0 },
        { activityId: null, title: '周末羽毛球搭子', successfulSignups: 4, waitlistCount: 0 },
        { activityId: null, title: '公园轻松夜跑', successfulSignups: 3, waitlistCount: 0 },
        { activityId: null, title: '咖啡馆编程学习局', successfulSignups: 3, waitlistCount: 0 }
      ]
    } else {
      await route.continue()
      return
    }

    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ code: 200, message: 'success', data })
    })
  })
}

async function capture(context, definition) {
  const page = await context.newPage()
  const { pending, diagnostics } = watchPage(page, definition.name)
  try {
    await page.goto(definition.path, { waitUntil: 'domcontentloaded', timeout: 45_000 })
    await page.addStyleTag({ content: '*,*::before,*::after{animation-duration:0s!important;animation-delay:0s!important;transition-duration:0s!important;caret-color:transparent!important}' })
    await definition.ready(page)
    await waitForDocumentReadiness(page, { minimumImages: definition.minimumImages || 0 })
    await waitForPendingRequests(pending)
    if (definition.minimumImages) await assertImageDimensions(page)
    if (definition.charts) await assertChartCanvases(page, definition.charts)
    if (definition.map) await assertMapRendered(page)
    if (definition.position) await definition.position(page)
    await assertNoBlockedTextInViewport(page)
    const buffer = await waitForVisualStability(page)
    if (diagnostics.length) throw new Error(diagnostics.join('\n'))

    const dimensions = decodePng(buffer)
    const expected = definition.viewport
    if (dimensions.width !== expected.width || dimensions.height !== expected.height) {
      throw new Error(`Unexpected screenshot dimensions ${dimensions.width}x${dimensions.height}; expected ${expected.width}x${expected.height}.`)
    }
    const stagedPath = path.join(stagingDir, definition.name)
    await writeFile(stagedPath, buffer)
    stagedFiles.push({ stagedPath, outputPath: path.join(outputDir, definition.name) })
    process.stdout.write(`Validated ${definition.name}\n`)
  } catch (error) {
    try {
      const failurePath = path.join(stagingDir, `${path.parse(definition.name).name}-failure.png`)
      await page.screenshot({ path: failurePath, animations: 'disabled', caret: 'hide' })
      await writeFile(path.join(stagingDir, `${path.parse(definition.name).name}-diagnostics.json`), JSON.stringify({
        error: redact(error.message),
        diagnostics
      }, null, 2))
    } catch {
      // Preserve the primary failure if debug capture also fails.
    }
    throw error
  } finally {
    await page.close()
  }
}

const api = await playwrightRequest.newContext({ baseURL: apiBaseURL })
let browser
let memberContext
let waitlistContext
let adminContext

try {
  const [memberSession, waitlistSession, adminSession] = await Promise.all([
    login(api, 'cp_showcase_member_a'),
    login(api, 'cp_showcase_host_b'),
    login(api, 'cp_showcase_admin')
  ])
  const showcaseTitles = [
    '周末羽毛球搭子',
    '东城桌游新手局',
    '城市摄影漫步',
    '周末电影观影交流',
    '公园轻松夜跑',
    '咖啡馆编程学习局'
  ]
  const showcaseActivityIds = await Promise.all(showcaseTitles.map((title) => findActivityId(api, title)))
  const [badmintonId, boardGameId] = showcaseActivityIds

  browser = await chromium.launch({ channel: browserChannel, headless: true })
  ;[memberContext, waitlistContext, adminContext] = await Promise.all([
    createContext(browser, mobileViewport, memberSession),
    createContext(browser, mobileViewport, waitlistSession),
    createContext(browser, desktopViewport, adminSession)
  ])
  await Promise.all([
    installShowcaseListFilter(memberContext),
    installShowcaseListFilter(waitlistContext),
    installAdminShowcaseActivityFilter(adminContext, showcaseActivityIds),
    installShowcaseDashboardData(adminContext)
  ])

  const mobile = mobileViewport
  const desktop = desktopViewport
  const definitions = [
    {
      context: memberContext, name: 'mobile-home.png', path: '/', viewport: mobile, minimumImages: 12,
      ready: async (page) => {
        await page.locator('.activity-card').first().waitFor({ state: 'visible' })
        if (await page.locator('.activity-card').count() < 6) throw new Error('Home page did not render all six showcase activities.')
        await page.getByText('周末羽毛球搭子', { exact: true }).waitFor({ state: 'visible' })
      }
    },
    {
      context: memberContext, name: 'mobile-map.png', path: '/map', viewport: mobile, minimumImages: 12, map: true,
      ready: async (page) => {
        await page.locator('.activity-map-container').waitFor({ state: 'visible', timeout: 30_000 })
        await page.waitForFunction(() => document.querySelectorAll('.activity-card').length >= 6, null, { timeout: 30_000 })
      }
    },
    {
      context: memberContext, name: 'mobile-activity-detail.png', path: `/activities/${badmintonId}`, viewport: mobile, minimumImages: 2,
      ready: async (page) => {
        await page.getByRole('heading', { name: '周末羽毛球搭子' }).waitFor({ state: 'visible' })
        await page.getByText('报名成功', { exact: true }).waitFor({ state: 'visible' })
      }
    },
    {
      context: waitlistContext, name: 'mobile-waitlist.png', path: `/activities/${boardGameId}`, viewport: mobile, minimumImages: 2,
      ready: async (page) => {
        await page.getByRole('heading', { name: '东城桌游新手局' }).waitFor({ state: 'visible' })
        await page.getByText('候补中', { exact: true }).first().waitFor({ state: 'visible' })
      },
      position: async (page) => {
        const button = page.getByRole('button', { name: '候补中', exact: true })
        await button.scrollIntoViewIfNeeded()
        await page.evaluate(() => window.scrollBy(0, -48))
        await page.waitForTimeout(300)
      }
    },
    {
      context: memberContext, name: 'mobile-chat.png', path: `/activities/${boardGameId}/chat`, viewport: mobile, minimumImages: 5,
      ready: async (page) => {
        await page.waitForFunction(() => document.querySelectorAll('.chat-bubble').length >= 5, null, { timeout: 30_000 })
        await page.getByText('东城桌游新手局', { exact: true }).waitFor({ state: 'visible' })
      }
    },
    {
      context: memberContext, name: 'mobile-notices.png', path: '/notices', viewport: mobile,
      ready: async (page) => {
        await page.waitForFunction(() => document.querySelectorAll('.notice-item').length >= 2, null, { timeout: 30_000 })
        await page.getByText('桌游报名已通过', { exact: true }).waitFor({ state: 'visible' })
      }
    },
    {
      context: memberContext, name: 'mobile-credit.png', path: '/credit/logs', viewport: mobile,
      ready: async (page) => {
        await page.getByText('当前信用分', { exact: true }).waitFor({ state: 'visible' })
        await page.waitForFunction(() => document.querySelectorAll('.credit-log').length >= 2, null, { timeout: 30_000 })
      }
    },
    {
      context: adminContext, name: 'admin-dashboard.png', path: '/admin/dashboard', viewport: desktop, charts: 1,
      ready: async (page) => {
        await page.getByRole('heading', { name: '运营概览' }).waitFor({ state: 'visible' })
        await page.waitForFunction(() => document.querySelectorAll('.dashboard-card').length === 4, null, { timeout: 30_000 })
        await page.getByText('周末羽毛球搭子', { exact: true }).waitFor({ state: 'visible' })
      }
    },
    {
      context: adminContext, name: 'admin-analytics.png', path: '/admin/analytics', viewport: desktop, charts: 7,
      ready: async (page) => {
        await page.getByRole('heading', { name: '数据分析' }).waitFor({ state: 'visible' })
        await page.waitForFunction(() => document.querySelectorAll('canvas').length >= 7, null, { timeout: 30_000 })
        await page.locator('.analytics-range').waitFor({ state: 'visible' })
      }
    },
    {
      context: adminContext, name: 'admin-activities.png', path: '/admin/activities', viewport: desktop,
      ready: async (page) => {
        await page.getByRole('heading', { name: '活动管理' }).waitFor({ state: 'visible' })
        await page.getByText('周末羽毛球搭子', { exact: true }).waitFor({ state: 'visible' })
        await page.getByText('周末电影观影交流', { exact: true }).waitFor({ state: 'visible' })
        if (await page.locator('.el-table__body tbody tr').count() < 6) {
          throw new Error('Admin activity table did not render all six showcase activities.')
        }
      }
    }
  ]

  for (const definition of definitions) {
    await capture(definition.context, definition)
  }

  await mkdir(outputDir, { recursive: true })
  for (const file of stagedFiles) await copyFile(file.stagedPath, file.outputPath)
  process.stdout.write(`Published ${stagedFiles.length} validated screenshot(s) to screenshots/showcase.\n`)
} catch (error) {
  process.stderr.write(`${redact(error.message)}\nDebug artifacts: ${stagingDir}\n`)
  process.exitCode = 1
} finally {
  await Promise.allSettled([memberContext?.close(), waitlistContext?.close(), adminContext?.close()])
  await browser?.close()
  await api.dispose()
  if (!process.exitCode) await rm(stagingDir, { recursive: true, force: true })
}
