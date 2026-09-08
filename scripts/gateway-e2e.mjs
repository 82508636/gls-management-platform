import { createHash, randomBytes } from 'node:crypto'

const keycloakUrl = process.env.KEYCLOAK_BASE_URL ?? 'http://localhost:8180'
const gatewayUrl = process.env.GATEWAY_BASE_URL ?? 'http://localhost:8090'
const catalogUrl = process.env.CATALOG_BASE_URL ?? 'http://localhost:8085'
const realm = process.env.KEYCLOAK_REALM ?? 'ltft'
const clientId = process.env.OIDC_CLIENT_ID ?? 'ltft-web'
const redirectUri = process.env.OIDC_REDIRECT_URI ?? 'http://localhost:5173/'
const username = process.env.E2E_USERNAME
const password = process.env.E2E_PASSWORD

if (!username || !password) {
  throw new Error('Defina E2E_USERNAME e E2E_PASSWORD para executar o E2E do gateway.')
}

const cookies = new Map()

function captureCookies(headers) {
  const values = typeof headers.getSetCookie === 'function'
    ? headers.getSetCookie()
    : [headers.get('set-cookie')].filter(Boolean)
  for (const value of values) {
    const pair = value.split(';', 1)[0]
    const separator = pair.indexOf('=')
    if (separator > 0) cookies.set(pair.slice(0, separator), pair.slice(separator + 1))
  }
}

async function sessionFetch(url, options = {}) {
  const headers = new Headers(options.headers)
  if (cookies.size) headers.set('Cookie', [...cookies].map(([key, value]) => `${key}=${value}`).join('; '))
  const response = await fetch(url, { ...options, headers, redirect: 'manual' })
  captureCookies(response.headers)
  return response
}

function decodeHtml(value) {
  return value.replaceAll('&amp;', '&').replaceAll('&#x3D;', '=').replaceAll('&#61;', '=')
}

async function authorizationCode() {
  const verifier = randomBytes(48).toString('base64url')
  const challenge = createHash('sha256').update(verifier).digest('base64url')
  const state = randomBytes(24).toString('base64url')
  const authorize = new URL(`${keycloakUrl}/realms/${realm}/protocol/openid-connect/auth`)
  authorize.search = new URLSearchParams({
    client_id: clientId,
    redirect_uri: redirectUri,
    response_type: 'code',
    scope: 'openid profile email',
    state,
    code_challenge: challenge,
    code_challenge_method: 'S256',
  })

  const loginPage = await sessionFetch(authorize)
  if (loginPage.status !== 200) throw new Error(`A página de login devolveu ${loginPage.status}.`)
  const html = await loginPage.text()
  const action = html.match(/<form[^>]+(?:id="kc-form-login"[^>]+action|action)="([^"]+)"/i)?.[1]
  if (!action) throw new Error('Não foi possível localizar o formulário de login do Keycloak.')

  let response = await sessionFetch(decodeHtml(action), {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: new URLSearchParams({ username, password, credentialId: '' }),
  })

  for (let redirects = 0; redirects < 10; redirects += 1) {
    const location = response.headers.get('location')
    if (!location) {
      const body = await response.text()
      throw new Error(`O login não redirecionou (${response.status}). ${body.includes('Invalid') ? 'Credenciais rejeitadas.' : ''}`)
    }
    const destination = new URL(location, keycloakUrl)
    if (destination.origin === new URL(redirectUri).origin) {
      if (destination.searchParams.get('state') !== state) throw new Error('O state OIDC devolvido é inválido.')
      const code = destination.searchParams.get('code')
      if (!code) throw new Error(`O Keycloak devolveu ${destination.searchParams.get('error') ?? 'um callback sem code'}.`)
      return { code, verifier }
    }
    response = await sessionFetch(destination)
  }
  throw new Error('Foram excedidos os redirecionamentos do login OIDC.')
}

async function accessToken() {
  const { code, verifier } = await authorizationCode()
  const response = await fetch(`${keycloakUrl}/realms/${realm}/protocol/openid-connect/token`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: new URLSearchParams({
      grant_type: 'authorization_code',
      client_id: clientId,
      code,
      code_verifier: verifier,
      redirect_uri: redirectUri,
    }),
  })
  if (!response.ok) throw new Error(`A troca do authorization code devolveu ${response.status}.`)
  return (await response.json()).access_token
}

async function expectStatus(path, expected, token, options = {}) {
  const headers = new Headers(options.headers)
  if (token) headers.set('Authorization', `Bearer ${token}`)
  let response
  try {
    response = await fetch(`${gatewayUrl}${path}`, {
      ...options,
      headers,
      signal: AbortSignal.timeout(20_000),
    })
  } catch (error) {
    throw new Error(`${options.method ?? 'GET'} ${path}: falha de ligação ao gateway`, { cause: error })
  }
  if (response.status !== expected) {
    throw new Error(`${options.method ?? 'GET'} ${path}: esperado ${expected}, recebido ${response.status}: ${await response.text()}`)
  }
  return response
}

const anonymous = await expectStatus('/api/customers?page=0&size=1', 401)
const anonymousError = await anonymous.json()
if (!anonymousError.traceId) throw new Error('A resposta 401 do gateway não contém traceId.')

const token = await accessToken()
const tokenClaims = JSON.parse(Buffer.from(token.split('.')[1], 'base64url').toString('utf8'))
console.log(`TOKEN issuer=${tokenClaims.iss} roles=${(tokenClaims.realm_access?.roles ?? []).join(',')}`)
const allRoutes = [
  '/api/customers?page=0&size=1',
  '/api/recipients?page=0&size=1',
  '/api/pickup-points?page=0&size=1',
  '/api/reference-data/account-profiles',
  '/api/admin/users',
  '/api/operational-services',
  '/api/service-groups',
  '/api/billing/zones',
  '/api/pricing/plans',
  '/api/shipments?page=0&size=1',
]
const routeFilter = process.env.E2E_ROUTES?.split(',').map(value => value.trim()).filter(Boolean)
const routes = routeFilter?.length ? allRoutes.filter(route => routeFilter.includes(route.split('?')[0])) : allRoutes

for (const route of routes) {
  console.log(`TEST GET ${route}`)
  try {
    await expectStatus(route, 200, token)
  } catch (error) {
    if (route.startsWith('/api/operational-services')) {
      const direct = await fetch(`${catalogUrl}${route}`, {
        headers: { Authorization: `Bearer ${token}` },
        signal: AbortSignal.timeout(20_000),
      })
      console.error(`DIAGNOSTIC direct catalog status=${direct.status} body=${await direct.text()}`)
    }
    throw error
  }
  console.log(`PASS GET ${route}`)
}

const correlationId = `e2e-${randomBytes(12).toString('hex')}`
const correlated = await expectStatus('/api/customers?page=0&size=1', 200, token, {
  headers: { 'X-Correlation-ID': correlationId },
})
if (correlated.headers.get('x-correlation-id') !== correlationId) {
  throw new Error('O gateway não propagou o X-Correlation-ID.')
}

await expectStatus('/api/customers?page=0&size=1', 200, token, { method: 'HEAD' })
console.log('PASS OIDC Authorization Code + PKCE')
console.log('PASS 401 comum com traceId')
console.log(`PASS ${routes.length} rotas autenticadas do gateway`)
console.log('PASS X-Correlation-ID')
console.log('PASS HEAD autenticado')
