import { createRequire } from 'node:module'
import { randomBytes } from 'node:crypto'

const requireFromFrontend = createRequire(new URL('../frontend/package.json', import.meta.url))
let chromium
try {
  ;({ chromium } = requireFromFrontend('playwright'))
} catch {
  try {
    ;({ chromium } = createRequire(import.meta.url)('playwright'))
  } catch {
    throw new Error('Playwright não está disponível. Instale-o no frontend ou configure NODE_PATH para um runtime que o disponibilize.')
  }
}

const keycloakUrl = process.env.KEYCLOAK_BASE_URL ?? 'http://localhost:8180'
const appUrl = process.env.E2E_APP_URL ?? 'http://localhost:5173'
const realm = process.env.KEYCLOAK_REALM ?? 'ltft'
const adminUsername = process.env.KEYCLOAK_ADMIN
const adminPassword = process.env.KEYCLOAK_ADMIN_PASSWORD
const executablePath = process.env.PLAYWRIGHT_EXECUTABLE_PATH

if (!adminUsername || !adminPassword) {
  throw new Error('Defina KEYCLOAK_ADMIN e KEYCLOAK_ADMIN_PASSWORD para executar os testes E2E.')
}

const runId = `${Date.now()}-${randomBytes(3).toString('hex')}`
const password = `E2E-${randomBytes(18).toString('base64url')}!aA1`
const roles = ['ADMIN', 'OPERATOR', 'ACCOUNTING', 'CUSTOMER']
const createdUsers = []

async function request(url, options = {}) {
  const response = await fetch(url, options)
  if (!response.ok) throw new Error(`${options.method ?? 'GET'} ${url} devolveu ${response.status}`)
  return response
}

async function adminToken() {
  const body = new URLSearchParams({
    client_id: 'admin-cli',
    username: adminUsername,
    password: adminPassword,
    grant_type: 'password',
  })
  const response = await request(`${keycloakUrl}/realms/master/protocol/openid-connect/token`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body,
  })
  return (await response.json()).access_token
}

async function provisionUsers(token) {
  const headers = { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' }
  for (const role of roles) {
    const username = `e2e-${role.toLowerCase()}-${runId}`
    const response = await request(`${keycloakUrl}/admin/realms/${realm}/users`, {
      method: 'POST',
      headers,
      body: JSON.stringify({
        username,
        enabled: true,
        emailVerified: true,
        firstName: 'E2E',
        lastName: role,
        email: `${username}@example.test`,
        credentials: [{ type: 'password', value: password, temporary: false }],
      }),
    })
    const location = response.headers.get('location')
    const id = location?.split('/').pop()
    if (!id) throw new Error(`O Keycloak não devolveu o id do utilizador ${username}.`)
    createdUsers.push(id)
    const roleResponse = await request(`${keycloakUrl}/admin/realms/${realm}/roles/${role}`, { headers })
    await request(`${keycloakUrl}/admin/realms/${realm}/users/${id}/role-mappings/realm`, {
      method: 'POST',
      headers,
      body: JSON.stringify([await roleResponse.json()]),
    })
  }
}

async function cleanupUsers(token) {
  const headers = { Authorization: `Bearer ${token}` }
  const response = await fetch(`${keycloakUrl}/admin/realms/${realm}/users?first=0&max=200`, { headers })
  const generatedUsers = response.ok ? await response.json() : []
  const ids = new Set([...createdUsers, ...generatedUsers.filter(user => user.username?.includes(runId)).map(user => user.id)])
  await Promise.allSettled([...ids].map(id => fetch(`${keycloakUrl}/admin/realms/${realm}/users/${id}`, { method: 'DELETE', headers })))
}

function assert(condition, message) {
  if (!condition) throw new Error(message)
}

async function login(page, role) {
  await page.goto(`${appUrl}/clientes`)
  const login = page.getByRole('button', { name: 'Login' })
  await login.waitFor({ state: 'visible' })
  await login.click()
  await page.getByLabel(/Username|Utilizador|Email/i).fill(`e2e-${role.toLowerCase()}-${runId}`)
  await page.locator('input[name="password"]').fill(password)
  await page.getByRole('button', { name: /Sign In|Entrar|Iniciar sessão/i }).click()
  await page.waitForURL(url => url.origin === new URL(appUrl).origin, { timeout: 20_000 })
}

async function testRole(browser, role) {
  const context = await browser.newContext()
  const page = await context.newPage()
  try {
    await login(page, role)
    if (role === 'CUSTOMER') {
      await page.getByRole('heading', { name: 'Acesso não autorizado' }).waitFor()
      return
    }

    await page.locator('main .customers-heading').filter({ hasText: 'Clientes' }).waitFor()
    const newCustomerCount = await page.getByRole('button', { name: '+ Novo', exact: true }).count()
    assert(newCustomerCount === (role === 'ADMIN' || role === 'OPERATOR' ? 1 : 0), `${role}: permissão de criação de clientes incorreta.`)

    await page.locator('header details.user-menu summary').click()
    const manageUsersCount = await page.getByRole('button', { name: 'Gerir utilizadores' }).count()
    assert(manageUsersCount === (role === 'ADMIN' ? 1 : 0), `${role}: visibilidade da gestão JML incorreta.`)

    if (role === 'ADMIN') {
      await page.getByRole('button', { name: 'Gerir utilizadores' }).click()
      await page.getByRole('heading', { name: 'Utilizadores' }).waitFor()
      await page.reload()
      await page.getByRole('heading', { name: 'Utilizadores' }).waitFor({ timeout: 20_000 })
      await testJml(page)
      await page.locator('header details.user-menu summary').click()
      await page.getByRole('button', { name: 'Terminar sessão' }).click()
      await page.getByRole('button', { name: 'Login' }).waitFor({ timeout: 20_000 })
    }
  } finally {
    await context.close()
  }
}

async function testJml(page) {
  const username = `e2e-jml-${runId}`
  await page.getByRole('button', { name: '+ Novo utilizador' }).click()
  const dialog = page.getByRole('dialog', { name: 'Novo utilizador' })
  await dialog.getByLabel('Nome', { exact: true }).fill('E2E')
  await dialog.getByLabel('Apelido', { exact: true }).fill('Lifecycle')
  await dialog.getByLabel('Username', { exact: true }).fill(username)
  await dialog.getByLabel('Email', { exact: true }).fill(`${username}@example.test`)
  await dialog.locator('label').filter({ hasText: 'Perfil' }).locator('select').selectOption('OPERATOR')
  await dialog.getByLabel('Password temporária', { exact: true }).fill(password)
  await dialog.getByRole('button', { name: 'Criar utilizador' }).click()

  let row = page.locator('tbody tr').filter({ hasText: username })
  await row.waitFor({ timeout: 20_000 })
  const roleSelect = row.getByLabel(`Perfil de ${username}`)
  assert(await roleSelect.inputValue() === 'OPERATOR', 'JOINER não atribuiu o perfil OPERATOR.')

  await roleSelect.selectOption('ACCOUNTING')
  row = page.locator('tbody tr').filter({ hasText: username })
  await waitUntil(async () => await row.getByLabel(`Perfil de ${username}`).inputValue() === 'ACCOUNTING', 'MOVER não atribuiu o perfil ACCOUNTING.')

  page.once('dialog', dialog => dialog.accept())
  await row.getByRole('button', { name: 'Desativar' }).click()
  await waitUntil(async () => (await page.locator('tbody tr').filter({ hasText: username }).textContent())?.includes('Inativo') === true,
    'LEAVER não desativou o utilizador.')
  console.log('PASS JML Joiner–Mover–Leaver')
}

async function waitUntil(predicate, message, timeout = 20_000) {
  const deadline = Date.now() + timeout
  while (Date.now() < deadline) {
    if (await predicate()) return
    await new Promise(resolve => setTimeout(resolve, 200))
  }
  throw new Error(message)
}

let token
let browser
try {
  token = await adminToken()
  await provisionUsers(token)
  browser = await chromium.launch({ headless: true, ...(executablePath ? { executablePath } : { channel: 'chrome' }) })
  for (const role of roles) {
    await testRole(browser, role)
    console.log(`PASS ${role}`)
  }
  console.log('PASS autenticação, autorização, refresh/deep-link e logout')
} finally {
  if (browser) await browser.close()
  if (token) await cleanupUsers(token)
}
