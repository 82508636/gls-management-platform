import { FormEvent, StrictMode, useEffect, useMemo, useRef, useState } from 'react'
import { createRoot } from 'react-dom/client'
import { apiUrl } from './api'
import type { Customer, CustomerService } from './domain'
import { downloadPendingServicesPdf } from './pdfReport'
import { AuthSession, initializeAuth } from './auth'
import { AdminUsersPage } from './AdminUsersPage'
import { CustomerFormFields, type VatValidationRequest, type VatValidationResult } from './CustomerFormFields'
import { CollaboratorCreatePage, CollaboratorsPage, PickupPointsPage, RecipientsPage, SuppliersStandbyPage } from './EntitiesPages'
import { type AuditedReference, ReferenceManagementPage } from './CollaboratorManagementPages'
import { PricingPlansPage } from './PricingPages'
import { BillingZonesPage, OperationalServiceEditorPage, OperationalServicesPage, ServiceGroupsPage } from './OperationalServicesPages'
import ltftLogoUrl from './assets/ltft-logo.jpg'
import './styles.css'

type CustomerForm = Omit<Customer, 'id'>
type CustomerPage = { content: Customer[]; page: number; size: number; totalElements: number; totalPages: number; first: boolean; last: boolean }
type ServiceStatus = 'PENDING' | 'PAID' | 'ALL'

const customerApiUrl = apiUrl
const workforceApiUrl = apiUrl
const GENERIC_LOAD_ERROR = 'Não foi possível carregar a informação. Tente novamente.'
const GENERIC_SAVE_ERROR = 'Não foi possível guardar. Tente novamente.'
const emptyForm: CustomerForm = { customerCode:'', abbreviation:'', shippingName:'', agency:'LTFT01', address:'', postalCode:'', locality:'', country:'PT', contactEmail:'', mobile:'', phone:'', billingCountry:'PT', vatNumber:'', billingLegalName:'', billingAddress:'', billingPostalCode:'', billingLocality:'', accountCode:'', billingReference:'', customerType:'COMPANY', responsibleName:'', billingEmail:'', defaultDocument:'INVOICE', exchangeRate:null, currency:'EUR', invoiceByPost:false, documentsByEmail:true, active:false }

function navigate(path: string) {
  window.history.pushState({}, '', path)
  window.dispatchEvent(new PopStateEvent('popstate'))
}

function Header({ auth, path }: { auth: AuthSession; path: string }) {
  const canViewEntities = auth.roles.some(role => role === 'ADMIN' || role === 'OPERATOR' || role === 'ACCOUNTING' || role === 'FRONT_DESK')
  const canViewPricing = auth.roles.some(role => role === 'ADMIN' || role === 'ACCOUNTING')
  const entitiesMenuRef = useRef<HTMLDivElement>(null)
  const entitiesMenuCloseTimer = useRef<number | undefined>(undefined)
  const [entitiesMenuOpen, setEntitiesMenuOpen] = useState(false)
  const [entitiesExpanded, setEntitiesExpanded] = useState(true)

  useEffect(() => () => window.clearTimeout(entitiesMenuCloseTimer.current), [])

  function openEntitiesMenu() {
    window.clearTimeout(entitiesMenuCloseTimer.current)
    setEntitiesMenuOpen(true)
  }

  function closeEntitiesMenu(delay = 0) {
    window.clearTimeout(entitiesMenuCloseTimer.current)
    entitiesMenuCloseTimer.current = window.setTimeout(() => setEntitiesMenuOpen(false), delay)
  }

  function goToPage(targetPath: string) {
    const menu = entitiesMenuRef.current
    closeEntitiesMenu()
    menu?.querySelector<HTMLElement>('.brand-trigger')?.focus()
    navigate(targetPath)
  }

  function isCurrentPage(targetPath: string) {
    return path === targetPath || path.startsWith(`${targetPath}/`)
  }

  function brandContent() {
    return <><span className="brand-mark">LTFT</span><span className="brand-copy"><strong>LTFT Comand Center</strong><small>Gestão comercial e financeira</small></span></>
  }

  return <header>
    <div className="header-left">
      {canViewEntities ? <div ref={entitiesMenuRef} className={`brand-entities-menu${entitiesMenuOpen ? ' is-open' : ''}`} onMouseEnter={openEntitiesMenu} onMouseLeave={() => closeEntitiesMenu(180)} onKeyDown={event => {
        if (event.key === 'Escape') {
          closeEntitiesMenu()
          entitiesMenuRef.current?.querySelector<HTMLElement>('.brand-trigger')?.focus()
        }
      }}>
        <button type="button" className="brand-trigger" aria-label="Abrir menu de navegação" aria-haspopup="menu" aria-expanded={entitiesMenuOpen} onClick={() => setEntitiesMenuOpen(open => !open)}>{brandContent()}<span className="brand-menu-arrow" aria-hidden="true" /></button>
        <nav className="entity-drawer" aria-label="Navegação principal" aria-hidden={!entitiesMenuOpen}>
          <div className="entity-drawer-head">
            <div><small>LTFT Comand Center</small><strong>Menu principal</strong></div>
            <button type="button" className="entity-drawer-close" aria-label="Fechar menu de navegação" onClick={() => closeEntitiesMenu()}>×</button>
          </div>
          <div className="entity-drawer-links">
            <button type="button" className={`drawer-section-toggle${entitiesExpanded ? ' is-expanded' : ''}`} aria-expanded={entitiesExpanded} aria-controls="entities-submenu" onClick={() => setEntitiesExpanded(expanded => !expanded)}><span>Entidades</span><span className="drawer-section-arrow" aria-hidden="true" /></button>
            <div id="entities-submenu" className="drawer-submenu" hidden={!entitiesExpanded}>
              <button className={isCurrentPage('/clientes') ? 'is-active' : ''} aria-current={isCurrentPage('/clientes') ? 'page' : undefined} onClick={() => goToPage('/clientes')}>Clientes</button>
              <button className={isCurrentPage('/entidades/destinatarios') ? 'is-active' : ''} aria-current={isCurrentPage('/entidades/destinatarios') ? 'page' : undefined} onClick={() => goToPage('/entidades/destinatarios')}>Destinatários</button>
              <button className={isCurrentPage('/entidades/pontos-pickup') ? 'is-active' : ''} aria-current={isCurrentPage('/entidades/pontos-pickup') ? 'page' : undefined} onClick={() => goToPage('/entidades/pontos-pickup')}>Pontos Pickup</button>
              <button className={isCurrentPage('/entidades/fornecedores') ? 'is-active' : ''} aria-current={isCurrentPage('/entidades/fornecedores') ? 'page' : undefined} onClick={() => goToPage('/entidades/fornecedores')}>Fornecedores <small>Stand by</small></button>
              {auth.roles.includes('ADMIN') && <button className={isCurrentPage('/entidades/colaboradores') ? 'is-active' : ''} aria-current={isCurrentPage('/entidades/colaboradores') ? 'page' : undefined} onClick={() => goToPage('/entidades/colaboradores')}>Colaboradores</button>}
            </div>
            {canViewEntities && <><p className="drawer-category-label">Configuração comercial</p><button className={`drawer-page-link${isCurrentPage('/configuracao/servicos') ? ' is-active' : ''}`} aria-current={isCurrentPage('/configuracao/servicos') ? 'page' : undefined} onClick={() => goToPage('/configuracao/servicos')}>Serviços</button><button className={`drawer-page-link${isCurrentPage('/billing/zones') ? ' is-active' : ''}`} aria-current={isCurrentPage('/billing/zones') ? 'page' : undefined} onClick={() => goToPage('/billing/zones')}>Zonas de faturação</button>{canViewPricing && <button className={`drawer-page-link${isCurrentPage('/configuracao/tabelas-precos') ? ' is-active' : ''}`} aria-current={isCurrentPage('/configuracao/tabelas-precos') ? 'page' : undefined} onClick={() => goToPage('/configuracao/tabelas-precos')}>Tabelas de preços</button>}</>}
            {auth.roles.includes('ADMIN') && <><p className="drawer-category-label">Administração</p><button className={`drawer-page-link${isCurrentPage('/admin/utilizadores') ? ' is-active' : ''}`} aria-current={isCurrentPage('/admin/utilizadores') ? 'page' : undefined} onClick={() => goToPage('/admin/utilizadores')}>Utilizadores</button></>}
          </div>
        </nav>
      </div> : <button className="brand-button" onClick={() => navigate('/clientes')}>{brandContent()}</button>}
    </div>
    <details className="user-menu"><summary><strong>{auth.displayName}</strong><span className="menu-arrow" aria-hidden="true" /></summary><div className="user-dropdown"><div className="user-roles"><small>Perfis</small><span>{auth.roles.join(' · ')}</span></div>{auth.roles.includes('ADMIN') && <button className="admin-menu-link" onClick={() => navigate('/admin/utilizadores')}>Gerir utilizadores</button>}<button onClick={() => void auth.logout()}>Terminar sessão</button></div></details>
  </header>
}

function AccountPage({ customer, auth, onBack }: { customer: Customer; auth: AuthSession; onBack: () => void }) {
  const [status, setStatus] = useState<ServiceStatus>('PENDING')
  const [dateFrom, setDateFrom] = useState('')
  const [dateTo, setDateTo] = useState('')
  const [services, setServices] = useState<CustomerService[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  useEffect(() => {
    const controller = new AbortController()
    setLoading(true)
    setError('')
    void auth.fetch(`${apiUrl}/customers/${customer.id}/services`, { signal: controller.signal })
      .then(response => {
        if (!response.ok) throw new Error('load failed')
        return response.json() as Promise<CustomerService[]>
      })
      .then(result => setServices(result))
      .catch(exception => {
        if (exception instanceof DOMException && exception.name === 'AbortError') return
        setServices([])
        setError(GENERIC_LOAD_ERROR)
      })
      .finally(() => { if (!controller.signal.aborted) setLoading(false) })
    return () => controller.abort()
  }, [customer.id, auth])
  const filtered = useMemo(() => services.filter(service =>
    (status === 'ALL' || service.status === status) &&
    (!dateFrom || service.serviceDate >= dateFrom) &&
    (!dateTo || service.serviceDate <= dateTo),
  ), [services, status, dateFrom, dateTo])
  const pendingTotal = services.filter(service => service.status === 'PENDING').reduce((total, service) => total + service.amount, 0)

  return <main className="workspace-page account-page">
    <button className="back-link" onClick={onBack}>← Voltar aos clientes</button>
    <section className="account-hero">
      <div><p className="eyebrow">Conta do cliente</p><h1>{customer.shippingName}</h1><p>Cliente {customer.customerCode} · NIF {customer.vatNumber}</p></div>
      <div className="account-tools"><button className="pdf-button" disabled={pendingTotal === 0} onClick={() => downloadPendingServicesPdf(customer, services)}>↓ Gerar resumo PDF</button><div className="balance"><span>Total por pagar</span><strong>{formatCurrency(pendingTotal)}</strong></div></div>
    </section>
    <section className="panel">
      <div className="panel-title account-title"><div><h2>Serviços</h2><p>Consulte os serviços por pagar e o histórico de pagamentos.</p></div></div>
      <div className="filters">
        <label>Estado<select value={status} onChange={event => setStatus(event.target.value as ServiceStatus)}><option value="PENDING">Por pagar</option><option value="PAID">Pagos</option><option value="ALL">Todos</option></select></label>
        <label>Data inicial<input type="date" value={dateFrom} onInput={event => setDateFrom(event.currentTarget.value)} /></label>
        <label>Data final<input type="date" value={dateTo} onInput={event => setDateTo(event.currentTarget.value)} /></label>
        <button className="secondary clear-filter" onClick={() => { setStatus('PENDING'); setDateFrom(''); setDateTo('') }}>Limpar filtros</button>
      </div>
      {error && <p className="form-error" role="alert">{error}</p>}
      <p className="results-count">{loading ? 'A carregar serviços…' : `${filtered.length} ${filtered.length === 1 ? 'serviço encontrado' : 'serviços encontrados'}`}</p>
      {!loading && filtered.length === 0 ? <p className="empty">Não existem serviços para os filtros selecionados.</p> : !loading && <div className="table-wrap"><table><thead><tr><th>Serviço</th><th>Data</th><th>Vencimento</th><th>Valor</th><th>Estado</th></tr></thead><tbody>
        {filtered.map(service => <ServiceRow key={service.id} service={service} />)}
      </tbody></table></div>}
    </section>
  </main>
}

function ServiceRow({ service }: { service: CustomerService }) {
  return <tr><td><strong>{service.reference}</strong><small>{service.description}</small></td><td>{formatDate(service.serviceDate)}</td><td>{formatDate(service.dueDate)}</td><td className="money">{formatCurrency(service.amount)}</td><td><span className={service.status === 'PAID' ? 'badge active' : 'badge pending'}>{service.status === 'PAID' ? 'Pago' : 'Por pagar'}</span>{service.paidAt && <small>em {formatDate(service.paidAt)}</small>}</td></tr>
}

function CustomersPage({ customers, loadCustomers, auth }: { customers: Customer[]; loadCustomers: (page: number, query: string, active: boolean | null) => Promise<CustomerPage>; auth: AuthSession }) {
  const [page, setPage] = useState(0)
  const [pageInfo, setPageInfo] = useState({ totalElements: 0, totalPages: 1, first: true, last: true })
  const [form, setForm] = useState<CustomerForm>(emptyForm)
  const [editingId, setEditingId] = useState<string | null>(null)
  const [formOpen, setFormOpen] = useState(false)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const [search, setSearch] = useState('')
  const [statusFilter, setStatusFilter] = useState<'ALL' | 'ACTIVE' | 'INACTIVE'>('ALL')
  const loadSequence = useRef(0)
  const canManage = auth.roles.some(role => role === 'ADMIN' || role === 'OPERATOR' || role === 'FRONT_DESK')
  const canAdminister = auth.roles.includes('ADMIN')
  const canViewAccount = auth.roles.some(role => role === 'ADMIN' || role === 'ACCOUNTING')
  async function loadPage(targetPage: number) {
    const sequence = ++loadSequence.current
    setLoading(true)
    try {
      const active = statusFilter === 'ALL' ? null : statusFilter === 'ACTIVE'
      let result = await loadCustomers(targetPage, search, active)
      if (sequence !== loadSequence.current) return
      if (targetPage > 0 && result.content.length === 0 && result.totalElements > 0) {
        result = await loadCustomers(targetPage - 1, search, active)
        if (sequence !== loadSequence.current) return
      }
      setPage(result.page)
      setPageInfo({ totalElements: result.totalElements, totalPages: result.totalPages || 1, first: result.first, last: result.last })
      setError('')
    } catch {
      if (sequence !== loadSequence.current) return
      setPageInfo({ totalElements: 0, totalPages: 1, first: true, last: true })
      setError(GENERIC_LOAD_ERROR)
    }
    finally { if (sequence === loadSequence.current) setLoading(false) }
  }
  useEffect(() => { const timer = window.setTimeout(() => void loadPage(0), 300); return () => window.clearTimeout(timer) }, [search, statusFilter])

  function updateField<K extends keyof CustomerForm>(field: K, value: CustomerForm[K]) { setForm(current => ({ ...current, [field]: value })) }
  function closeForm() { setFormOpen(false); setEditingId(null); setForm(emptyForm); setError('') }
  function create() { setEditingId(null); setForm(emptyForm); setError(''); setFormOpen(true) }
  function edit(customer: Customer) { const { id, ...values } = customer; setEditingId(id); setForm(values); setError(''); setFormOpen(true) }

  async function save(event: FormEvent) {
    event.preventDefault(); setError('')
    try {
      const response = await auth.fetch(`${customerApiUrl}/customers${editingId ? `/${editingId}` : ''}`, { method: editingId ? 'PUT' : 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(form) })
      if (!response.ok) { setError(GENERIC_SAVE_ERROR); return }
      closeForm(); await loadPage(editingId ? page : 0)
    } catch {
      setError(GENERIC_SAVE_ERROR)
    }
  }

  async function toggleActive(customer: Customer) {
    const action = customer.active ? 'inativar' : 'ativar'
    if (!window.confirm(`Pretende ${action} o cliente ${customer.shippingName}?`)) return
    setError('')
    try {
      const response = await auth.fetch(`${customerApiUrl}/customers/${encodeURIComponent(customer.id)}/status`, { method: 'PATCH', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ active: !customer.active }) })
      if (!response.ok) throw new Error('Customer status update failed')
      await loadPage(page)
    } catch { setError('Não foi possível alterar o estado do cliente. Tente novamente.') }
  }

  async function deleteCustomer(customer: Customer) {
    if (!window.confirm(`Pretende eliminar definitivamente o cliente ${customer.shippingName}?`)) return
    setError('')
    try {
      const response = await auth.fetch(`${customerApiUrl}/customers/${encodeURIComponent(customer.id)}`, { method: 'DELETE' })
      if (!response.ok) throw new Error('Customer deletion failed')
      await loadPage(page)
    } catch { setError('Não foi possível eliminar o cliente. Tente novamente.') }
  }

  async function validateVat(request: VatValidationRequest): Promise<VatValidationResult> {
    let response: Response
    try {
      response = await auth.fetch(`${customerApiUrl}/vat-validations`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(request),
        signal: AbortSignal.timeout(20_000),
      })
    } catch (exception) {
      if (exception instanceof DOMException && exception.name === 'TimeoutError') {
        throw new Error('O VIES demorou demasiado a responder. Tente novamente dentro de alguns instantes.')
      }
      throw new Error('Não foi possível contactar o serviço de validação de NIF.')
    }
    if (response.status === 401) throw new Error('A sessão deixou de ser válida. Termine a sessão e volte a entrar.')
    if (response.status === 403) throw new Error('O seu perfil não tem permissão para validar NIFs.')
    if (response.status >= 500) throw new Error('O serviço de validação está temporariamente indisponível. Tente novamente.')
    if (!response.ok) throw new Error(`Não foi possível validar o NIF (HTTP ${response.status}).`)
    return response.json() as Promise<VatValidationResult>
  }

  return <main className="workspace-page customers-page">
    <p className="eyebrow customers-heading">Clientes</p>
    <section className="panel list-panel">
      <div className="customer-toolbar">
        <div className="toolbar-actions">{canManage && <button onClick={create}>+ Novo</button>}<label>Código ou cliente<input value={search} onChange={event => setSearch(event.target.value)} placeholder="Pesquisar…" /></label><label>Estado<select value={statusFilter} onChange={event => setStatusFilter(event.target.value as typeof statusFilter)}><option value="ALL">Todos</option><option value="ACTIVE">Ativos</option><option value="INACTIVE">Inativos</option></select></label></div>
        <span className="results-summary">{customers.length} de {pageInfo.totalElements} clientes</span>
      </div>
      {error && !formOpen && <p className="error">{error}</p>}
      {loading && customers.length === 0 ? <p>A carregar…</p> : customers.length === 0 ? <p className="empty compact-empty">Não foram encontrados clientes.</p> : <div className="table-wrap customer-table"><table><thead><tr><th>Código</th><th>Designação social</th><th>Contactos</th><th>Localidade</th><th>Agência</th><th>Estado</th><th>Ações</th></tr></thead><tbody>
        {customers.map(customer => { const canChange = canManage; const canUseAdminActions = canAdminister; return <tr key={customer.id}><td><strong className="customer-code">{customer.customerCode}</strong><small>{customer.agency}</small></td><td><strong>{customer.shippingName}</strong><small>NIF: {customer.vatNumber}</small></td><td><span>{customer.mobile || customer.phone || '—'}</span><small>{customer.contactEmail || 'Sem email'}</small></td><td>{customer.locality || '—'}<small>{customer.country || '—'}</small></td><td>{customer.agency === 'LTFT01' ? 'Fafe' : 'Taipas'}</td><td><span className={customer.active ? 'status-dot active' : 'status-dot'} title={customer.active ? 'Ativo' : 'Inativo'} aria-label={customer.active ? 'Ativo' : 'Inativo'} /></td><td><div className="split-action"><button className="edit-main" disabled={!canChange} title={!canManage ? 'Sem permissão para editar' : undefined} onClick={() => edit(customer)}>Editar</button><details><summary aria-label={`Mais ações para ${customer.shippingName}`}><span className="action-arrow" aria-hidden="true" /></summary><div className="action-dropdown">{canViewAccount && <button onClick={() => navigate(`/clientes/${customer.id}/conta`)}>Conta</button>}<button disabled={!canUseAdminActions} onClick={() => void toggleActive(customer)}>{customer.active ? 'Inativar cliente' : 'Ativar cliente'}</button><button disabled>Converter em prospect <small>Em preparação</small></button><button disabled>Histórico de edições <small>Em preparação</small></button><hr/><button disabled>Autorização de débito direto <small>Em preparação</small></button><hr/><button className="delete-action" disabled={!canUseAdminActions} onClick={() => void deleteCustomer(customer)}>Eliminar</button></div></details></div></td></tr> })}
      </tbody></table></div>}
      <nav className="pagination" aria-label="Paginação de clientes"><span>{pageInfo.totalElements} clientes · Página {page + 1} de {Math.max(pageInfo.totalPages, 1)}</span><div><button className="secondary" disabled={pageInfo.first} onClick={() => void loadPage(page - 1)}>Anterior</button><button className="secondary" disabled={pageInfo.last} onClick={() => void loadPage(page + 1)}>Seguinte</button></div></nav>
    </section>
    {formOpen && <div className="overlay" onMouseDown={event => { if (event.target === event.currentTarget) closeForm() }}><aside className="partial" role="dialog" aria-modal="true" aria-labelledby="customer-form-title"><div className="partial-head"><div><h2 id="customer-form-title">{editingId ? 'Editar cliente' : 'Novo cliente'}</h2><p>Campos obrigatórios demarcados com *</p></div><button className="close" onClick={closeForm} aria-label="Fechar">×</button></div>
      <form className="customer-form" onSubmit={save}><CustomerFormFields form={form} editing={Boolean(editingId)} update={updateField} validateVat={validateVat}/>{error && <p className="error wide">{error}</p>}<div className="actions wide"><button type="button" className="secondary" onClick={closeForm}>Cancelar</button><button type="submit">{editingId ? 'Guardar alterações' : 'Aprovar e criar cliente'}</button></div></form>
    </aside></div>}
  </main>
}

function App({ auth }: { auth: AuthSession }) {
  const [path, setPath] = useState(window.location.pathname)
  const [apiCustomers, setApiCustomers] = useState<Customer[]>([])
  const [accountProfiles, setAccountProfiles] = useState<AuditedReference[]>([])
  const [professionalCategories, setProfessionalCategories] = useState<AuditedReference[]>([])
  useEffect(() => { const update = () => setPath(window.location.pathname); window.addEventListener('popstate', update); return () => window.removeEventListener('popstate', update) }, [])
  useEffect(() => {
    if (!auth.roles.includes('ADMIN')) return
    void (async () => {
      try {
        const [profilesResponse, categoriesResponse] = await Promise.all([
          auth.fetch(`${workforceApiUrl}/reference-data/account-profiles`),
          auth.fetch(`${workforceApiUrl}/reference-data/professional-categories`),
        ])
        if (!profilesResponse.ok || !categoriesResponse.ok) throw new Error('reference load failed')
        setAccountProfiles(await profilesResponse.json() as AuditedReference[])
        setProfessionalCategories(await categoriesResponse.json() as AuditedReference[])
      } catch {
        setAccountProfiles([])
        setProfessionalCategories([])
      }
    })()
  }, [])
  const customerRequest = useRef<AbortController | null>(null)
  async function loadCustomers(page: number, query: string, active: boolean | null): Promise<CustomerPage> {
    customerRequest.current?.abort()
    const controller = new AbortController()
    customerRequest.current = controller
    try {
      const parameters = new URLSearchParams({ page: String(page), size: '50' })
      if (query.trim()) parameters.set('query', query.trim())
      if (active !== null) parameters.set('active', String(active))
      const response = await auth.fetch(`${customerApiUrl}/customers?${parameters}`, { signal: controller.signal })
      if (!response.ok) throw new Error(`Customer request failed with status ${response.status}`)
      const result = await response.json() as CustomerPage
      if (customerRequest.current === controller) setApiCustomers(result.content)
      return result
    } catch (error) {
      if (customerRequest.current === controller) setApiCustomers([])
      throw error
    }
  }
  const customers = apiCustomers
  const match = path.match(/^\/clientes\/([^/]+)\/conta$/)
  const customer = match ? customers.find(item => item.id === decodeURIComponent(match[1])) : null
  const canUseCustomerArea = auth.roles.some(role => role === 'ADMIN' || role === 'OPERATOR' || role === 'ACCOUNTING' || role === 'FRONT_DESK')
  const canUseCustomerAccount = auth.roles.some(role => role === 'ADMIN' || role === 'ACCOUNTING')
  const isAdminUsers = path === '/admin/utilizadores'
  const isRecipients = path === '/entidades/destinatarios'
  const isPickupPoints = path === '/entidades/pontos-pickup'
  const isSuppliers = path === '/entidades/fornecedores'
  const isCollaborators = path === '/entidades/colaboradores'
  const isCollaboratorCreate = path === '/entidades/colaboradores/create'
  const isAccountProfiles = path === '/admin/perfis'
  const isProfessionalCategories = path === '/entidades/colaboradores/categorias-profissionais'
  const isPricingPlans = path === '/configuracao/tabelas-precos'
  const isOperationalServices = path === '/configuracao/servicos'
  const operationalServiceMatch = path.match(/^\/configuracao\/servicos\/([^/]+)\/edit$/)
  const isOperationalServiceCreate = path === '/configuracao/servicos/create'
  const isServiceGroups = path === '/configuracao/grupos-servicos'
  const isBillingZones = path === '/billing/zones'
  const isCollaboratorArea = isCollaborators || isCollaboratorCreate || isAccountProfiles || isProfessionalCategories
  const isEntityPage = isRecipients || isPickupPoints || isSuppliers || isCollaboratorArea
  const entityContent = isRecipients ? <RecipientsPage auth={auth} /> : isPickupPoints ? <PickupPointsPage auth={auth} /> : isSuppliers ? <SuppliersStandbyPage /> : isAccountProfiles && auth.roles.includes('ADMIN') ? <ReferenceManagementPage auth={auth} endpoint="account-profiles" title="Gerir perfis de conta" trail={['Administração','Perfis de conta']} description="Catálogo de perfis disponível no registo de colaboradores. As permissões efetivas continuam a ser geridas no Keycloak." createTitle="Novo perfil" editTitle="Editar perfil" idHint="Ex.: DRIVER" items={accountProfiles} onChange={setAccountProfiles} onBack={() => navigate('/entidades/colaboradores/create')}/> : isProfessionalCategories && auth.roles.includes('ADMIN') ? <ReferenceManagementPage auth={auth} endpoint="professional-categories" title="Gerir categorias profissionais" trail={['Entidades','Colaboradores','Categorias profissionais']} description="Mantém as categorias profissionais sem eliminar o respetivo histórico de auditoria." createTitle="Nova categoria profissional" editTitle="Editar categoria profissional" idHint="Ex.: 7" items={professionalCategories} onChange={setProfessionalCategories} onBack={() => navigate('/entidades/colaboradores/create')}/> : isCollaboratorCreate && auth.roles.includes('ADMIN') ? <CollaboratorCreatePage accountProfiles={accountProfiles} professionalCategories={professionalCategories} onManageProfiles={() => navigate('/admin/perfis')} onManageCategories={() => navigate('/entidades/colaboradores/categorias-profissionais')}/> : isCollaborators && auth.roles.includes('ADMIN') ? <CollaboratorsPage onCreate={() => navigate('/entidades/colaboradores/create')} /> : null
  const canViewPricing = auth.roles.some(role => role === 'ADMIN' || role === 'ACCOUNTING')
  const isCommercialCatalog = isOperationalServices || isOperationalServiceCreate || Boolean(operationalServiceMatch) || isServiceGroups || isBillingZones
  const commercialContent = isOperationalServices ? <OperationalServicesPage auth={auth}/> : isOperationalServiceCreate ? <OperationalServiceEditorPage auth={auth}/> : operationalServiceMatch ? <OperationalServiceEditorPage auth={auth} id={decodeURIComponent(operationalServiceMatch[1])}/> : isServiceGroups ? <ServiceGroupsPage auth={auth}/> : isBillingZones ? <BillingZonesPage auth={auth}/> : null
  const commercialWritePage = isOperationalServiceCreate
  return <div className="app-shell"><Header auth={auth} path={path} />{isAdminUsers && auth.roles.includes('ADMIN') ? <AdminUsersPage auth={auth} onBack={() => navigate('/clientes')} /> : isPricingPlans && canViewPricing ? <PricingPlansPage auth={auth}/> : isCommercialCatalog && canUseCustomerArea && (!commercialWritePage || auth.roles.includes('ADMIN')) ? commercialContent : isAdminUsers || !canUseCustomerArea || (match && !canUseCustomerAccount) || (isCollaboratorArea && !auth.roles.includes('ADMIN')) || (isPricingPlans && !canViewPricing) || (isCommercialCatalog && commercialWritePage && !auth.roles.includes('ADMIN')) ? <main><section className="panel access-denied"><h1>Acesso não autorizado</h1><p>Não tem permissões para consultar esta área.</p></section></main> : isEntityPage ? entityContent : match && customer ? <AccountPage customer={customer} auth={auth} onBack={() => navigate('/clientes')} /> : match ? <main><button className="back-link" onClick={() => navigate('/clientes')}>← Voltar aos clientes</button><p className="empty">Cliente não encontrado.</p></main> : <CustomersPage customers={apiCustomers} loadCustomers={loadCustomers} auth={auth} />}</div>
}

function formatCurrency(value: number) { return new Intl.NumberFormat('pt-PT', { style: 'currency', currency: 'EUR' }).format(value) }
function formatDate(value: string) { return new Intl.DateTimeFormat('pt-PT').format(new Date(`${value}T12:00:00`)) }
function LoginPage({ auth }: { auth: AuthSession }) {
  return <div className="login-page"><section className="login-card"><div className="login-logo"><img src={ltftLogoUrl} alt="LTFT — Transporte e Logística" /></div><h1>Comand Center</h1><button onClick={() => void auth.login()}>Login</button></section></div>
}

async function bootstrap() {
  try {
    const auth = await initializeAuth()
    createRoot(document.getElementById('root')!).render(<StrictMode>{auth.authenticated ? <App auth={auth} /> : <LoginPage auth={auth} />}</StrictMode>)
  } catch {
    createRoot(document.getElementById('root')!).render(<div className="login-page"><section className="login-card"><h1>Não foi possível iniciar sessão</h1><p>Tente novamente.</p><button onClick={() => window.location.assign('/')}>Voltar</button></section></div>)
  }
}

void bootstrap()
