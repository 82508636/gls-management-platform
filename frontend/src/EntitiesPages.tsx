import { FormEvent, useEffect, useMemo, useState } from 'react'
import type { AuthSession } from './auth'

const apiUrl = import.meta.env.VITE_API_URL ?? 'http://localhost:8080/api'

type PickupPoint = {
  id: string; code: string; designation: string; supplier: string
  morningOpen: string | null; morningClose: string | null; afternoonOpen: string | null; afternoonClose: string | null
  address: string; postalCode: string; locality: string; country: string
  email: string | null; phone: string | null; mobile: string | null
  openSaturday: boolean; openSunday: boolean; active: boolean
}
type PickupForm = Omit<PickupPoint, 'id'>
type Page<T> = { content: T[]; page: number; totalElements: number; totalPages: number; first: boolean; last: boolean }
type Recipient = { id: string; name: string; contactName: string | null; address: string; postalCode: string; locality: string; country: string; email: string | null; phone: string | null; mobile: string | null; lastUsedAt: string }

const emptyPickup: PickupForm = {
  code: '', designation: '', supplier: '', morningOpen: '', morningClose: '', afternoonOpen: '', afternoonClose: '',
  address: '', postalCode: '', locality: '', country: 'PT', email: '', phone: '', mobile: '',
  openSaturday: false, openSunday: false, active: true,
}
const countries = [['PT', 'Portugal'], ['ES', 'Espanha'], ['FR', 'França'], ['DE', 'Alemanha'], ['IT', 'Itália'], ['BE', 'Bélgica'], ['NL', 'Países Baixos']]

export function PickupPointsPage({ auth }: { auth: AuthSession }) {
  const [points, setPoints] = useState<PickupPoint[]>([])
  const [page, setPage] = useState(0)
  const [pageInfo, setPageInfo] = useState({ totalElements: 0, totalPages: 1, first: true, last: true })
  const [query, setQuery] = useState('')
  const [form, setForm] = useState<PickupForm>(emptyPickup)
  const [editingId, setEditingId] = useState<string | null>(null)
  const [open, setOpen] = useState(false)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const canManage = auth.roles.some(role => role === 'ADMIN' || role === 'OPERATOR')
  const canAdminister = auth.roles.includes('ADMIN')
  const visible = useMemo(() => {
    const normalized = query.trim().toLocaleLowerCase('pt-PT')
    return points.filter(point => !normalized || [point.code, point.designation, point.supplier, point.locality, point.postalCode]
      .some(value => value.toLocaleLowerCase('pt-PT').includes(normalized)))
  }, [points, query])

  async function load(targetPage = 0) {
    setLoading(true)
    try {
      const response = await auth.fetch(`${apiUrl}/pickup-points?page=${targetPage}&size=50`)
      if (!response.ok) throw new Error('load failed')
      const result = await response.json() as Page<PickupPoint>
      setPoints(result.content); setPage(result.page)
      setPageInfo({ totalElements: result.totalElements, totalPages: result.totalPages || 1, first: result.first, last: result.last }); setError('')
    } catch { setError('Não foi possível carregar os Pontos Pickup. Tente novamente.') }
    finally { setLoading(false) }
  }
  useEffect(() => { void load(0) }, [])

  function update<K extends keyof PickupForm>(field: K, value: PickupForm[K]) { setForm(current => ({ ...current, [field]: value })) }
  function create() { setEditingId(null); setForm(emptyPickup); setError(''); setOpen(true) }
  function edit(point: PickupPoint) { const { id, ...values } = point; setEditingId(id); setForm(values); setError(''); setOpen(true) }
  function close() { setOpen(false); setEditingId(null); setForm(emptyPickup); setError('') }

  async function save(event: FormEvent) {
    event.preventDefault(); setError('')
    try {
      const response = await auth.fetch(`${apiUrl}/pickup-points${editingId ? `/${editingId}` : ''}`, {
        method: editingId ? 'PUT' : 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(form),
      })
      if (!response.ok) throw new Error('save failed')
      close(); await load(editingId ? page : 0)
    } catch { setError('Não foi possível guardar o Ponto Pickup. Confirme os dados e tente novamente.') }
  }

  async function changeStatus(point: PickupPoint) {
    try {
      const response = await auth.fetch(`${apiUrl}/pickup-points/${point.id}/status`, { method: 'PATCH', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ active: !point.active }) })
      if (!response.ok) throw new Error('status failed')
      await load(page)
    } catch { setError('Não foi possível alterar o estado do Ponto Pickup.') }
  }

  return <main className="entities-page">
    <div className="page-heading"><div><p className="eyebrow">Entidades</p><h1>Pontos Pickup</h1><p>Locais disponíveis para entrega e recolha de envios.</p></div>{canManage && <button onClick={create}>+ Novo ponto</button>}</div>
    <section className="panel list-panel">
      <div className="customer-toolbar"><label>Pesquisar<input value={query} onChange={event => setQuery(event.target.value)} placeholder="Código, nome ou localidade…" /></label><span className="results-summary">{visible.length} de {points.length} pontos</span></div>
      {error && !open && <p className="error">{error}</p>}
      {loading && points.length === 0 ? <p>A carregar…</p> : visible.length === 0 ? <p className="empty compact-empty">Ainda não existem Pontos Pickup.</p> : <div className="table-wrap customer-table"><table><thead><tr><th>Código</th><th>Designação</th><th>Fornecedor</th><th>Localidade</th><th>Horário</th><th>Fim de semana</th><th>Estado</th><th>Ações</th></tr></thead><tbody>{visible.map(point => <tr key={point.id}>
        <td><strong className="customer-code">{point.code}</strong></td><td><strong>{point.designation}</strong><small>{point.address}</small></td><td>{point.supplier}</td><td>{point.locality}<small>{point.postalCode} · {point.country}</small></td><td>{schedule(point)}</td><td>{point.openSaturday ? 'Sáb.' : '—'} {point.openSunday ? 'Dom.' : ''}</td><td><span className={point.active ? 'status-dot active' : 'status-dot'} aria-label={point.active ? 'Ativo' : 'Inativo'} /></td><td><div className="row-actions"><button className="compact-action" disabled={!canManage} onClick={() => edit(point)}>Editar</button>{canAdminister && <button className="compact-action secondary" onClick={() => void changeStatus(point)}>{point.active ? 'Inativar' : 'Ativar'}</button>}</div></td>
      </tr>)}</tbody></table></div>}
      <nav className="pagination"><span>{pageInfo.totalElements} pontos · Página {page + 1} de {pageInfo.totalPages}</span><div><button className="secondary" disabled={pageInfo.first} onClick={() => void load(page - 1)}>Anterior</button><button className="secondary" disabled={pageInfo.last} onClick={() => void load(page + 1)}>Seguinte</button></div></nav>
    </section>
    {open && <div className="overlay" onMouseDown={event => { if (event.target === event.currentTarget) close() }}><aside className="partial pickup-partial" role="dialog" aria-modal="true" aria-labelledby="pickup-title"><div className="partial-head"><div><h2 id="pickup-title">{editingId ? 'Editar Ponto Pickup' : 'Adicionar Ponto Pickup'}</h2><p>Campos obrigatórios demarcados com *</p></div><button className="close" onClick={close} aria-label="Fechar">×</button></div>
      <form className="pickup-form form-grid" onSubmit={save}>
        <label className="col-3">Código *<input required maxLength={30} value={form.code} onChange={e => update('code', e.target.value)} /></label>
        <label className="col-6">Designação *<input required maxLength={200} value={form.designation} onChange={e => update('designation', e.target.value)} /></label>
        <label className="col-3">Fornecedor *<input required maxLength={100} list="pickup-suppliers" value={form.supplier} onChange={e => update('supplier', e.target.value)} /><datalist id="pickup-suppliers"><option value="GLS-FAFE"/><option value="GLS-TAIPAS"/></datalist></label>
        <fieldset className="schedule-field col-6"><legend>Horário manhã</legend><label>De<input type="time" value={form.morningOpen ?? ''} onChange={e => update('morningOpen', e.target.value)} /></label><label>Até<input type="time" value={form.morningClose ?? ''} onChange={e => update('morningClose', e.target.value)} /></label></fieldset>
        <fieldset className="schedule-field col-6"><legend>Horário tarde</legend><label>De<input type="time" value={form.afternoonOpen ?? ''} onChange={e => update('afternoonOpen', e.target.value)} /></label><label>Até<input type="time" value={form.afternoonClose ?? ''} onChange={e => update('afternoonClose', e.target.value)} /></label></fieldset>
        <label className="col-12">Morada *<input required maxLength={500} value={form.address} onChange={e => update('address', e.target.value)} /></label>
        <label className="col-3">Código Postal *<input required maxLength={20} value={form.postalCode} onChange={e => update('postalCode', e.target.value)} /></label>
        <label className="col-6">Localidade *<input required maxLength={120} value={form.locality} onChange={e => update('locality', e.target.value)} /></label>
        <label className="col-3">País *<select required value={form.country} onChange={e => update('country', e.target.value)}>{countries.map(([code, name]) => <option key={code} value={code}>{name}</option>)}</select></label>
        <label className="col-6">E-mail<input type="email" value={form.email ?? ''} onChange={e => update('email', e.target.value)} /></label>
        <label className="col-3">Telefone<input value={form.phone ?? ''} onChange={e => update('phone', e.target.value)} /></label>
        <label className="col-3">Telemóvel<input value={form.mobile ?? ''} onChange={e => update('mobile', e.target.value)} /></label>
        <div className="check-row col-12"><label className="checkbox"><input type="checkbox" checked={form.openSaturday} onChange={e => update('openSaturday', e.target.checked)} /> Aberto sábado</label><label className="checkbox"><input type="checkbox" checked={form.openSunday} onChange={e => update('openSunday', e.target.checked)} /> Aberto domingo</label><label className="checkbox"><input type="checkbox" checked={form.active} onChange={e => update('active', e.target.checked)} /> Ativo</label></div>
        {error && <p className="error col-12">{error}</p>}<div className="actions col-12"><button type="button" className="secondary" onClick={close}>Cancelar</button><button type="submit">Guardar</button></div>
      </form></aside></div>}
  </main>
}

export function RecipientsPage({ auth }: { auth: AuthSession }) {
  const [recipients, setRecipients] = useState<Recipient[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  useEffect(() => { void (async () => { try { const response = await auth.fetch(`${apiUrl}/recipients?page=0&size=50`); if (!response.ok) throw new Error(); const page = await response.json() as Page<Recipient>; setRecipients(page.content) } catch { setError('Não foi possível carregar os destinatários.') } finally { setLoading(false) } })() }, [])
  return <main className="entities-page"><div className="page-heading"><div><p className="eyebrow">Entidades</p><h1>Destinatários</h1><p>Registos criados automaticamente a partir das recolhas e envios.</p></div><span className="info-chip">Sem criação manual</span></div><section className="panel list-panel">{error && <p className="error">{error}</p>}{loading ? <p>A carregar…</p> : recipients.length === 0 ? <div className="empty compact-empty"><strong>Ainda não existem destinatários.</strong><p>Quando for registada uma recolha ou um envio, o destinatário será guardado automaticamente e ficará disponível aqui.</p></div> : <div className="table-wrap customer-table"><table><thead><tr><th>Destinatário</th><th>Contacto</th><th>Morada</th><th>Última utilização</th></tr></thead><tbody>{recipients.map(recipient => <tr key={recipient.id}><td><strong>{recipient.name}</strong><small>{recipient.contactName || 'Sem pessoa de contacto'}</small></td><td>{recipient.mobile || recipient.phone || '—'}<small>{recipient.email || 'Sem email'}</small></td><td>{recipient.address}<small>{recipient.postalCode} {recipient.locality} · {recipient.country}</small></td><td>{new Intl.DateTimeFormat('pt-PT').format(new Date(recipient.lastUsedAt))}</td></tr>)}</tbody></table></div>}</section></main>
}

export function CollaboratorsPage({ onCreate }: { onCreate: () => void }) {
  return <main className="entities-page collaborator-list-page">
    <div className="page-heading"><div><p className="eyebrow">Entidades</p><h1>Colaboradores</h1></div><button onClick={onCreate}>+ Novo colaborador</button></div>
    <section className="panel list-panel">
      <div className="customer-toolbar"><div className="toolbar-actions"><label>Código ou colaborador<input placeholder="Pesquisar…" /></label><label>Estado<select defaultValue="ALL"><option value="ALL">Todos</option><option value="ACTIVE">Ativos</option><option value="INACTIVE">Inativos</option></select></label></div><span className="results-summary">0 colaboradores</span></div>
      <div className="table-wrap customer-table"><table><thead><tr><th>Código</th><th>Nome completo</th><th>Perfil</th><th>Delegação</th><th>Contactos</th><th>Estado</th><th>Ações</th></tr></thead><tbody><tr><td className="collaborator-empty-cell" colSpan={7}><strong>Ainda não existem colaboradores.</strong><small>Utiliza “+ Novo colaborador” para preparar um novo registo.</small></td></tr></tbody></table></div>
    </section>
  </main>
}

export function CollaboratorCreatePage({ onBack }: { onBack: () => void }) {
  return <main className="entities-page collaborator-page"><button className="back-link" onClick={onBack}>← Voltar aos colaboradores</button><div className="page-heading"><div><p className="eyebrow">Entidades · Colaboradores</p><h1>Adicionar colaborador</h1></div></div>
    <section className="panel collaborator-form">
      <section className="employee-identification-layout">
        <div className="employee-identification-main"><h2>Identificação</h2><div className="employee-identification-grid">
          <Field label="Código" span="ident-code" disabled placeholder="A atribuir"/>
          <Field label="Nome completo *" span="ident-name"/>
          <SelectField label="Perfil da conta *" className="ident-profile" options={['Selecionar…','Administrador','Operador','Contabilidade','Cliente']}/>
          <label className="checkbox compact-check ident-active"><input type="checkbox" defaultChecked/> Ativo</label>
          <Field label="Cód. Abrev." span="ident-abbreviation"/>
          <SelectField label="Género" className="ident-gender" options={['Selecionar…','Feminino','Masculino','Outro','Prefiro não indicar']}/>
          <Field label="Data de nascimento" span="ident-birthdate" type="date"/>
          <SelectField label="Estado civil" className="ident-civil-status" options={['Selecionar…','Solteiro/a','Casado/a','União de facto','Divorciado/a','Viúvo/a']}/>
          <Field label="Nacionalidade" span="ident-nationality" defaultValue="Portugal"/>
        </div></div>
        <label className="employee-photo">Fotografia<div className="photo-placeholder">Sem fotografia</div><input type="file" accept="image/*" /></label>
      </section>
      <EmployeeSection title="Informação profissional"><Field label="Data de admissão" type="date"/><Field label="Data de demissão" type="date"/><SelectField label="Delegação principal *" options={['Selecionar…','LTFT01 · Fafe','LTFT02 · Taipas']}/><Field label="Cargo ou função"/><Field label="Categoria profissional"/><Field label="Departamento"/><Field label="Grupos de trabalho" span="span-2"/><Field label="E-mail da empresa" type="email" span="span-2"/><Field label="Telemóvel da empresa"/></EmployeeSection>
      <EmployeeSection title="Contactos pessoais e residência"><Field label="Morada de residência" span="span-4"/><Field label="Código Postal"/><Field label="Localidade" span="span-2"/><Field label="País" defaultValue="Portugal"/><Field label="E-mail pessoal" type="email" span="span-2"/><Field label="Telemóvel pessoal"/><Field label="Telefone"/><Field label="Pessoa de contacto de emergência" span="span-2"/><Field label="Telemóvel SOS"/><Field label="Telefone SOS"/></EmployeeSection>
      <EmployeeSection title="Dados fiscais e familiares"><Field label="Documento de identificação" span="span-2"/><Field label="Contribuinte"/><Field label="N.º Segurança Social"/><SelectField label="Dependentes" options={['0','1','2','3','4','5+']}/><SelectField label="Rendimentos" options={['Não indicado','Categoria A','Categoria B','Outros']}/><SelectField label="Deficiência" options={['Não','Sim','Não indicado']}/></EmployeeSection>
      <EmployeeSection title="Formação académica"><SelectField label="Grau académico" options={['Selecionar…','Ensino básico','Ensino secundário','Licenciatura','Mestrado','Doutoramento','Outro']}/><Field label="Instituição de ensino"/><Field label="Curso / Formação" span="span-2"/><Field label="Avaliação"/></EmployeeSection>
      <EmployeeSection title="Informação bancária"><Field label="Nome do banco"/><Field label="IBAN" span="span-2"/><Field label="BIC / SWIFT"/></EmployeeSection>
      <EmployeeSection title="Redes sociais e observações"><Field label="LinkedIn" span="span-2"/><Field label="Facebook"/><Field label="X / Twitter"/><label className="span-4">Notas e observações<textarea rows={4}/></label></EmployeeSection>
    </section>
  </main>
}

export function SuppliersStandbyPage() { return <main className="entities-page"><div className="page-heading"><div><p className="eyebrow">Entidades</p><h1>Fornecedores</h1></div><span className="info-chip">Stand by</span></div><section className="panel empty compact-empty"><strong>Funcionalidade em preparação.</strong><p>Não foram criados dados, páginas de manutenção ou endpoints para fornecedores.</p></section></main> }

function EmployeeSection({ title, children, className = '' }: { title: string; children: React.ReactNode; className?: string }) { return <section className={`employee-section ${className}`}><h2>{title}</h2><div className="employee-grid">{children}</div></section> }
function Field({ label, span = '', ...props }: { label: string; span?: string } & React.InputHTMLAttributes<HTMLInputElement>) { return <label className={span}>{label}<input {...props}/></label> }
function SelectField({ label, options, className = '' }: { label: string; options: string[]; className?: string }) { return <label className={className}>{label}<select>{options.map(option => <option key={option}>{option}</option>)}</select></label> }
function schedule(point: PickupPoint) { const morning = point.morningOpen && point.morningClose ? `${point.morningOpen.slice(0,5)}–${point.morningClose.slice(0,5)}` : ''; const afternoon = point.afternoonOpen && point.afternoonClose ? `${point.afternoonOpen.slice(0,5)}–${point.afternoonClose.slice(0,5)}` : ''; return [morning, afternoon].filter(Boolean).join(' / ') || '—' }
