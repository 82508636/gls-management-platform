export type Role = 'ADMIN' | 'OPERATOR' | 'ACCOUNTING' | 'CUSTOMER' | 'DRIVER' | 'FRONT_DESK'

type TokenClaims = {
  exp?: number
  name?: string
  preferred_username?: string
  realm_access?: { roles?: string[] }
}

type AuthorizationMode = 'login' | 'silent'

type TokenResponse = {
  access_token: string
  refresh_token?: string
  id_token?: string
}

export type AuthSession = {
  authenticated: boolean
  displayName: string
  roles: Role[]
  login: () => Promise<void>
  logout: () => Promise<void>
  fetch: typeof fetch
}

const authority = import.meta.env.VITE_OIDC_AUTHORITY ?? 'http://localhost:8180/realms/ltft'
const clientId = import.meta.env.VITE_OIDC_CLIENT_ID ?? 'ltft-web'
const callbackUri = `${window.location.origin}/`
const STATE_KEY = 'oidc_state'
const VERIFIER_KEY = 'oidc_verifier'
const RETURN_PATH_KEY = 'oidc_return_path'
const MODE_KEY = 'oidc_mode'
const SKIP_SILENT_ONCE_KEY = 'oidc_skip_silent_once'
const SILENT_ERRORS = new Set(['login_required', 'interaction_required', 'consent_required', 'account_selection_required'])

let accessToken = ''
let refreshToken = ''
let idToken = ''
let claims: TokenClaims = {}
let refreshPromise: Promise<void> | null = null

export async function initializeAuth(): Promise<AuthSession> {
  const url = new URL(window.location.href)
  const code = url.searchParams.get('code')
  const oidcError = url.searchParams.get('error')

  if (code || oidcError) {
    await handleAuthorizationCallback(url, code, oidcError)
  } else if (!accessToken) {
    if (sessionStorage.getItem(SKIP_SILENT_ONCE_KEY) === 'true') {
      sessionStorage.removeItem(SKIP_SILENT_ONCE_KEY)
    } else {
      await startAuthorization('silent')
    }
  }

  return session()
}

function session(): AuthSession {
  const realmRoles = claims.realm_access?.roles ?? []
  const roles = realmRoles.filter((role): role is Role => ['ADMIN', 'OPERATOR', 'ACCOUNTING', 'CUSTOMER', 'DRIVER', 'FRONT_DESK'].includes(role))
  return {
    authenticated: Boolean(accessToken),
    displayName: claims.name ?? claims.preferred_username ?? '',
    roles,
    login,
    logout,
    fetch: authenticatedFetch,
  }
}

async function login() {
  await startAuthorization('login')
}

async function startAuthorization(mode: AuthorizationMode) {
  const state = randomValue()
  const verifier = randomValue(64)
  const challenge = await sha256(verifier)
  sessionStorage.setItem(STATE_KEY, state)
  sessionStorage.setItem(VERIFIER_KEY, verifier)
  sessionStorage.setItem(RETURN_PATH_KEY, currentReturnPath())
  sessionStorage.setItem(MODE_KEY, mode)

  const params = new URLSearchParams({
    client_id: clientId,
    redirect_uri: callbackUri,
    response_type: 'code',
    scope: 'openid profile email',
    state,
    code_challenge: challenge,
    code_challenge_method: 'S256',
  })
  if (mode === 'silent') params.set('prompt', 'none')

  const destination = `${authority}/protocol/openid-connect/auth?${params}`
  if (mode === 'silent') window.location.replace(destination)
  else window.location.assign(destination)
}

async function handleAuthorizationCallback(url: URL, code: string | null, oidcError: string | null) {
  const returnedState = url.searchParams.get('state')
  const expectedState = sessionStorage.getItem(STATE_KEY)
  const verifier = sessionStorage.getItem(VERIFIER_KEY)
  const mode = sessionStorage.getItem(MODE_KEY) as AuthorizationMode | null
  const returnPath = safeReturnPath(sessionStorage.getItem(RETURN_PATH_KEY))

  if (!expectedState || returnedState !== expectedState || !verifier || !mode) {
    clearAuthorizationRequest()
    throw new Error('Invalid OIDC callback')
  }

  clearAuthorizationRequest()
  window.history.replaceState({}, '', returnPath)

  if (oidcError) {
    if (mode === 'silent' && SILENT_ERRORS.has(oidcError)) return
    throw new Error('OIDC authorization failed')
  }
  if (!code) throw new Error('OIDC authorization code missing')

  await exchangeCode(code, verifier)
}

async function logout() {
  const logoutHint = idToken
  clearTokens()
  clearAuthorizationRequest()
  sessionStorage.setItem(SKIP_SILENT_ONCE_KEY, 'true')

  const params = new URLSearchParams({
    client_id: clientId,
    post_logout_redirect_uri: callbackUri,
  })
  if (logoutHint) params.set('id_token_hint', logoutHint)
  window.location.assign(`${authority}/protocol/openid-connect/logout?${params}`)
}

async function authenticatedFetch(input: RequestInfo | URL, init: RequestInit = {}) {
  await refreshIfNeeded()
  let response = await fetchWithAccessToken(input, init)
  if (response.status !== 401 || !refreshToken) return response

  try {
    await refreshAccessToken()
  } catch {
    return response
  }
  response.body?.cancel().catch(() => undefined)
  response = await fetchWithAccessToken(input, init)
  return response
}

function fetchWithAccessToken(input: RequestInfo | URL, init: RequestInit) {
  const headers = new Headers(init.headers)
  if (accessToken) headers.set('Authorization', `Bearer ${accessToken}`)
  const requestInput = input instanceof Request ? input.clone() : input
  return fetch(requestInput, { ...init, headers })
}

async function exchangeCode(code: string, verifier: string) {
  const body = new URLSearchParams({
    grant_type: 'authorization_code',
    client_id: clientId,
    code,
    code_verifier: verifier,
    redirect_uri: callbackUri,
  })
  await requestTokens(body)
}

async function refreshIfNeeded() {
  if (!accessToken || !claims.exp || claims.exp * 1000 - Date.now() > 30_000) return
  if (!refreshToken) {
    clearTokens()
    throw new Error('OIDC session expired')
  }
  await refreshAccessToken()
}

async function refreshAccessToken() {
  if (!refreshPromise) {
    refreshPromise = requestTokens(new URLSearchParams({
      grant_type: 'refresh_token',
      client_id: clientId,
      refresh_token: refreshToken,
    })).catch(error => {
      clearTokens()
      throw error
    }).finally(() => {
      refreshPromise = null
    })
  }
  await refreshPromise
}

async function requestTokens(body: URLSearchParams) {
  const response = await fetch(`${authority}/protocol/openid-connect/token`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body,
  })
  if (!response.ok) throw new Error(`OIDC token request failed with status ${response.status}`)
  const tokens = await response.json() as TokenResponse
  accessToken = tokens.access_token
  refreshToken = tokens.refresh_token ?? refreshToken
  idToken = tokens.id_token ?? idToken
  claims = decodeJwt(accessToken)
}

function clearTokens() {
  accessToken = ''
  refreshToken = ''
  idToken = ''
  claims = {}
}

function clearAuthorizationRequest() {
  sessionStorage.removeItem(STATE_KEY)
  sessionStorage.removeItem(VERIFIER_KEY)
  sessionStorage.removeItem(RETURN_PATH_KEY)
  sessionStorage.removeItem(MODE_KEY)
}

function currentReturnPath() {
  return `${window.location.pathname}${window.location.search}${window.location.hash}`
}

function safeReturnPath(value: string | null) {
  return value?.startsWith('/') && !value.startsWith('//') ? value : '/clientes'
}

function decodeJwt(token: string): TokenClaims {
  const parts = token.split('.')
  if (parts.length < 2) throw new Error('Invalid JWT')
  const encoded = parts[1].replace(/-/g, '+').replace(/_/g, '/')
  return JSON.parse(decodeURIComponent(Array.from(atob(encoded), character => `%${character.charCodeAt(0).toString(16).padStart(2, '0')}`).join('')))
}

function randomValue(length = 32) {
  const bytes = crypto.getRandomValues(new Uint8Array(length))
  return base64Url(bytes)
}

async function sha256(value: string) {
  return base64Url(new Uint8Array(await crypto.subtle.digest('SHA-256', new TextEncoder().encode(value))))
}

function base64Url(bytes: Uint8Array) {
  return btoa(String.fromCharCode(...bytes)).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '')
}
