import { FormEvent, StrictMode, useEffect, useMemo, useRef, useState } from 'react'
import { createRoot } from 'react-dom/client'
import type { Customer, CustomerService } from './domain'
import { downloadPendingServicesPdf } from './pdfReport'
import { AuthSession, initializeAuth } from './auth'
import { AdminUsersPage } from './AdminUsersPage'
import { CustomerFormFields, type VatValidationRequest, type VatValidationResult } from './CustomerFormFields'
import { CollaboratorsPrototypePage, PickupPointsPage, RecipientsPage, SuppliersStandbyPage } from './EntitiesPages'
import ltftLogoUrl from './assets/ltft-logo.jpg'
import './styles.css'

type CustomerForm = Omit<Customer, 'id'>
type CustomerPage = { content: Customer[]; page: number; size: number; totalElements: number; totalPages: number; first: boolean; last: boolean }
type ServiceStatus = 'PENDING' | 'PAID' | 'ALL'

const apiUrl = import.meta.env.VITE_API_URL ?? 'http://localhost:8080/api'
const GENERIC_LOAD_ERROR = 'Não foi possível carregar a informação. Tente novamente.'
const GENERIC_SAVE_ERROR = 'Não foi possível guardar. Tente novamente.'
const emptyForm: CustomerForm = { customerCode:'', abbreviation:'', shippingName:'', agency:'LTFT01', address:'', postalCode:'', locality:'', country:'PT', contactEmail:'', mobile:'', phone:'', billingCountry:'PT', vatNumber:'', billingLegalName:'', billingAddress:'', billingPostalCode:'', billingLocality:'', accountCode:'', billingReference:'', customerType:'COMPANY', responsibleName:'', billingEmail:'', defaultDocument:'INVOICE', exchangeRate:null, currency:'EUR', invoiceByPost:false, documentsByEmail:true, active:false }

function navigate(path: string) {
  window.history.pushState({}, '', path)
  window.dispatchEvent(new PopStateEvent('popstate'))
}

function Header({ auth, path }: { auth: AuthSession; path: string }) {
  const canViewEntities = auth.roles.some(role => role === 'ADMIN' || role === 'OPERATOR' || role === 'ACCOUNTING')
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
    return targetPath === '/clientes' ? path === '/clientes' || path.startsWith('/clientes/') : path === targetPath
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
            {auth.roles.includes('ADMIN') && <><p className="drawer-category-label">Administração</p><button className={`drawer-page-link${isCurrentPage('/admin/utilizadores') ? ' is-active' : ''}`} aria-current={isCurrentPage('/admin/utilizadores') ? 'page' : undefined} onClick={() => goToPage('/admin/utilizadores')}>Utilizadores</button></>}
          </div>
        </nav>
      </div> : <button className="brand-button" onClick={() => navigate('/clientes')}>{brandContent()}</button>}
    </div>
    <details className="user-menu"><summary><strong>{auth.displayName}</strong><span className="menu-arrow" aria-hidden="true" /></summary><div className="user-dropdown"><div className="user-roles"><small>Perfis</small><span>{auth.roles.join(' · ')}</span></div>{auth.roles.includes('ADMIN') && <button className="admin-menu-link" onClick={() => navigate('/admin/utilizadores')}>Gerir utilizadores</button>}<button onClick={() => void auth.logout()}>Terminar sessão</button></div></details>
  </header>
}

function AccountPage({ customer, onBack }: { customer: Customer; onBack: () => void }) {
  const [status, setStatus] = useState<ServiceStatus>('PENDING')
  const [dateFrom, setDateFrom] = useState('')
  const [dateTo, setDateTo] = useState('')
  const services: CustomerService[] = []
  const filtered = useMemo(() => services.filter(service =>
    (status === 'ALL' || service.status === status) &&
    (!dateFrom || service.serviceDate >= dateFrom) &&
    (!dateTo || service.serviceDate <= dateTo),
  ), [services, status, dateFrom, dateTo])
  const pendingTotal = services.filter(service => service.status === 'PENDING').reduce((total, service) => total + service.amount, 0)

  return <main>
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
      <p className="results-count">{filtered.length} {filtered.length === 1 ? 'serviço encontrado' : 'serviços encontrados'}</p>
      {filtered.length === 0 ? <p className="empty">Não existem serviços para os filtros selecionados.</p> : <div className="table-wrap"><table><thead><tr><th>Serviço</th><th>Data</th><th>Vencimento</th><th>Valor</th><th>Estado</th></tr></thead><tbody>
        {filtered.map(service => <ServiceRow key={service.id} service={service} />)}
      </tbody></table></div>}
    </section>
  </main>
}

function ServiceRow({ service }: { service: CustomerService }) {
  return <tr><td><strong>{service.reference}</strong><small>{service.description}</small></td><td>{formatDate(service.serviceDate)}</td><td>{formatDate(service.dueDate)}</td><td className="money">{formatCurrency(service.amount)}</td><td><span className={service.status === 'PAID' ? 'badge active' : 'badge pending'}>{service.status === 'PAID' ? 'Pago' : 'Por pagar'}</span>{service.paidAt && <small>em {formatDate(service.paidAt)}</small>}</td></tr>
}

function CustomersPage({ customers, loadCustomers, auth }: { customers: Customer[]; loadCustomers: (page: number) => Promise<CustomerPage>; auth: AuthSession }) {
  const [page, setPage] = useState(0)
  const [pageInfo, setPageInfo] = useState({ totalElements: 0, totalPages: 1, first: true, last: true })
  const [form, setForm] = useState<CustomerForm>(emptyForm)
  const [editingId, setEditingId] = useState<string | null>(null)
  const [formOpen, setFormOpen] = useState(false)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const [search, setSearch] = useState('')
  const [statusFilter, setStatusFilter] = useState<'ALL' | 'ACTIVE' | 'INACTIVE'>('ALL')
  const canManage = auth.roles.some(role => role === 'ADMIN' || role === 'OPERATOR')
  const canAdminister = auth.roles.includes('ADMIN')
  const canViewAccount = auth.roles.some(role => role === 'ADMIN' || role === 'ACCOUNTING')
  const visibleCustomers = useMemo(() => {
    const query = search.trim().toLocaleLowerCase('pt-PT')
    return customers.filter(customer =>
      (statusFilter === 'ALL' || (statusFilter === 'ACTIVE' ? customer.active : !customer.active)) &&
      (!query || [customer.customerCode, customer.shippingName, customer.vatNumber, customer.contactEmail, customer.locality, customer.agency]
        .some(value => value?.toLocaleLowerCase('pt-PT').includes(query)))
    )
  }, [customers, search, statusFilter])

  async function loadPage(targetPage: number) {
    setLoading(true)
    try {
      let result = await loadCustomers(targetPage)
      if (targetPage > 0 && result.content.length === 0 && result.totalElements > 0) {
        result = await loadCustomers(targetPage - 1)
      }
      setPage(result.page)
      setPageInfo({ totalElements: result.totalElements, totalPages: result.totalPages || 1, first: result.first, last: result.last })
      setError('')
    } catch {
      setPageInfo({ totalElements: 0, totalPages: 1, first: true, last: true })
      setError(GENERIC_LOAD_ERROR)
    }
    finally { setLoading(false) }
  }
  useEffect(() => { void loadPage(0) }, [])

  function updateField<K extends keyof CustomerForm>(field: K, value: CustomerForm[K]) { setForm(current => ({ ...current, [field]: value })) }
  function closeForm() { setFormOpen(false); setEditingId(null); setForm(emptyForm); setError('') }
  function create() { setEditingId(null); setForm(emptyForm); setError(''); setFormOpen(true) }
  function edit(customer: Customer) { const { id, ...values } = customer; setEditingId(id); setForm(values); setError(''); setFormOpen(true) }

  async function save(event: FormEvent) {
    event.preventDefault(); setError('')
    try {
      const response = await auth.fetch(`${apiUrl}/customers${editingId ? `/${editingId}` : ''}`, { method: editingId ? 'PUT' : 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(form) })
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
      const response = await auth.fetch(`${apiUrl}/customers/${encodeURIComponent(customer.id)}/status`, { method: 'PATCH', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ active: !customer.active }) })
      if (!response.ok) throw new Error('Customer status update failed')
      await loadPage(page)
    } catch { setError('Não foi possível alterar o estado do cliente. Tente novamente.') }
  }

  async function deleteCustomer(customer: Customer) {
    if (!window.confirm(`Pretende eliminar definitivamente o cliente ${customer.shippingName}?`)) return
    setError('')
    try {
      const response = await auth.fetch(`${apiUrl}/customers/${encodeURIComponent(customer.id)}`, { method: 'DELETE' })
      if (!response.ok) throw new Error('Customer deletion failed')
      await loadPage(page)
    } catch { setError('Não foi possível eliminar o cliente. Tente novamente.') }
  }

  async function validateVat(request: VatValidationRequest): Promise<VatValidationResult> {
    const response = await auth.fetch(`${apiUrl}/vat-validations`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(request),
    })
    if (!response.ok) throw new Error('VAT validation request failed')
    return response.json() as Promise<VatValidationResult>
  }

  return <main>
    <p className="eyebrow customers-heading">Clientes</p>
    <section className="panel list-panel">
      <div className="customer-toolbar">
        <div className="toolbar-actions">{canManage && <button onClick={create}>+ Novo</button>}<label>Código ou cliente<input value={search} onChange={event => setSearch(event.target.value)} placeholder="Pesquisar…" /></label><label>Estado<select value={statusFilter} onChange={event => setStatusFilter(event.target.value as typeof statusFilter)}><option value="ALL">Todos</option><option value="ACTIVE">Ativos</option><option value="INACTIVE">Inativos</option></select></label></div>
        <span className="results-summary">{visibleCustomers.length} de {customers.length} clientes</span>
      </div>
      {error && !formOpen && <p className="error">{error}</p>}
      {loading && customers.length === 0 ? <p>A carregar…</p> : visibleCustomers.length === 0 ? <p className="empty compact-empty">Não foram encontrados clientes.</p> : <div className="table-wrap customer-table"><table><thead><tr><th>Código</th><th>Designação social</th><th>Contactos</th><th>Localidade</th><th>Agência</th><th>Estado</th><th>Ações</th></tr></thead><tbody>
        {visibleCustomers.map(customer => { const canChange = canManage; const canUseAdminActions = canAdminister; return <tr key={customer.id}><td><strong className="customer-code">{customer.customerCode}</strong><small>{customer.agency}</small></td><td><strong>{customer.shippingName}</strong><small>NIF: {customer.vatNumber}</small></td><td><span>{customer.mobile || customer.phone || '—'}</span><small>{customer.contactEmail || 'Sem email'}</small></td><td>{customer.locality || '—'}<small>{customer.country || '—'}</small></td><td>{customer.agency === 'LTFT01' ? 'Fafe' : 'Taipas'}</td><td><span className={customer.active ? 'status-dot active' : 'status-dot'} title={customer.active ? 'Ativo' : 'Inativo'} aria-label={customer.active ? 'Ativo' : 'Inativo'} /></td><td><div className="split-action"><button className="edit-main" disabled={!canChange} title={!canManage ? 'Sem permissão para editar' : undefined} onClick={() => edit(customer)}>Editar</button><details><summary aria-label={`Mais ações para ${customer.shippingName}`}><span className="action-arrow" aria-hidden="true" /></summary><div className="action-dropdown">{canViewAccount && <button onClick={() => navigate(`/clientes/${customer.id}/conta`)}>Conta</button>}<button disabled={!canUseAdminActions} onClick={() => void toggleActive(customer)}>{customer.active ? 'Inativar cliente' : 'Ativar cliente'}</button><button disabled>Converter em prospect <small>Em preparação</small></button><button disabled>Histórico de edições <small>Em preparação</small></button><hr/><button disabled>Autorização de débito direto <small>Em preparação</small></button><hr/><button className="delete-action" disabled={!canUseAdminActions} onClick={() => void deleteCustomer(customer)}>Eliminar</button></div></details></div></td></tr> })}
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
  useEffect(() => { const update = () => setPath(window.location.pathname); window.addEventListener('popstate', update); return () => window.removeEventListener('popstate', update) }, [])
  async function loadCustomers(page: number): Promise<CustomerPage> {
    try {
      const response = await auth.fetch(`${apiUrl}/customers?page=${page}&size=50`)
      if (!response.ok) throw new Error(`Customer request failed with status ${response.status}`)
      const result = await response.json() as CustomerPage
      setApiCustomers(result.content)
      return result
    } catch (error) {
      setApiCustomers([])
      throw error
    }
  }
  const customers = apiCustomers
  const match = path.match(/^\/clientes\/([^/]+)\/conta$/)
  const customer = match ? customers.find(item => item.id === decodeURIComponent(match[1])) : null
  const canUseCustomerArea = auth.roles.some(role => role === 'ADMIN' || role === 'OPERATOR' || role === 'ACCOUNTING')
  const canUseCustomerAccount = auth.roles.some(role => role === 'ADMIN' || role === 'ACCOUNTING')
  const isAdminUsers = path === '/admin/utilizadores'
  const isRecipients = path === '/entidades/destinatarios'
  const isPickupPoints = path === '/entidades/pontos-pickup'
  const isSuppliers = path === '/entidades/fornecedores'
  const isCollaborators = path === '/entidades/colaboradores'
  const isEntityPage = isRecipients || isPickupPoints || isSuppliers || isCollaborators
  const entityContent = isRecipients ? <RecipientsPage auth={auth} /> : isPickupPoints ? <PickupPointsPage auth={auth} /> : isSuppliers ? <SuppliersStandbyPage /> : isCollaborators && auth.roles.includes('ADMIN') ? <CollaboratorsPrototypePage /> : null
  return <div className="app-shell"><Header auth={auth} path={path} />{isAdminUsers && auth.roles.includes('ADMIN') ? <AdminUsersPage auth={auth} onBack={() => navigate('/clientes')} /> : isAdminUsers || !canUseCustomerArea || (match && !canUseCustomerAccount) || (isCollaborators && !auth.roles.includes('ADMIN')) ? <main><section className="panel access-denied"><h1>Acesso não autorizado</h1><p>Não tem permissões para consultar esta área.</p></section></main> : isEntityPage ? entityContent : match && customer ? <AccountPage customer={customer} onBack={() => navigate('/clientes')} /> : match ? <main><button className="back-link" onClick={() => navigate('/clientes')}>← Voltar aos clientes</button><p className="empty">Cliente não encontrado.</p></main> : <CustomersPage customers={apiCustomers} loadCustomers={loadCustomers} auth={auth} />}</div>
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
