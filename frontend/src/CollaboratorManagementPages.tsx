import { FormEvent, useMemo, useState } from 'react'
import { ContextualPageHeading } from './ContextualPageHeading'
import type { AuthSession } from './auth'

const workforceApiUrl = import.meta.env.VITE_WORKFORCE_API_URL ?? 'http://localhost:8083/api'

export type AuditedReference = {
  id: string
  designation: string
  active: boolean
  createdAt: string
  createdBy: string
  updatedAt: string
  updatedBy: string
}

type ReferenceManagementPageProps = {
  auth: AuthSession
  createTitle: string
  description: string
  editTitle: string
  endpoint: 'account-profiles' | 'professional-categories'
  idHint: string
  items: AuditedReference[]
  onBack: () => void
  onChange: (items: AuditedReference[]) => void
  title: string
  trail: string[]
}

export function ReferenceManagementPage({ auth, createTitle, description, editTitle, endpoint, idHint, items, onBack, onChange, title, trail }: ReferenceManagementPageProps) {
  const [search, setSearch] = useState('')
  const [editingId, setEditingId] = useState<string | null>(null)
  const [formOpen, setFormOpen] = useState(false)
  const [id, setId] = useState('')
  const [designation, setDesignation] = useState('')
  const [error, setError] = useState('')
  const visibleItems = useMemo(() => {
    const query = search.trim().toLocaleLowerCase('pt-PT')
    return items.filter(item => !query || item.id.toLocaleLowerCase('pt-PT').includes(query) || item.designation.toLocaleLowerCase('pt-PT').includes(query))
  }, [items, search])

  function closeForm() {
    setFormOpen(false)
    setEditingId(null)
    setId('')
    setDesignation('')
    setError('')
  }

  function create() {
    setEditingId(null)
    setId('')
    setDesignation('')
    setError('')
    setFormOpen(true)
  }

  function edit(item: AuditedReference) {
    setEditingId(item.id)
    setId(item.id)
    setDesignation(item.designation)
    setError('')
    setFormOpen(true)
  }

  async function save(event: FormEvent) {
    event.preventDefault()
    const normalizedId = id.trim().toUpperCase()
    const normalizedDesignation = designation.trim()
    if (!normalizedId || !normalizedDesignation) {
      setError('Preencha o ID e a designação.')
      return
    }
    if (!editingId && items.some(item => item.id.toLocaleUpperCase('pt-PT') === normalizedId)) {
      setError('Já existe um registo com este ID.')
      return
    }
    try {
      const response = await auth.fetch(`${workforceApiUrl}/reference-data/${endpoint}${editingId ? `/${encodeURIComponent(editingId)}` : ''}`, {
        method: editingId ? 'PUT' : 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(editingId ? { designation: normalizedDesignation } : { id: normalizedId, designation: normalizedDesignation }),
      })
      if (!response.ok) throw new Error('save failed')
      const saved = await response.json() as AuditedReference
      const next = editingId ? items.map(item => item.id === editingId ? saved : item) : [...items, saved]
      onChange(next.sort((left, right) => left.designation.localeCompare(right.designation, 'pt-PT')))
      closeForm()
    } catch {
      setError('Não foi possível guardar. Confirme se o ID ou a designação já existem.')
    }
  }

  async function toggle(item: AuditedReference) {
    setError('')
    try {
      const response = await auth.fetch(`${workforceApiUrl}/reference-data/${endpoint}/${encodeURIComponent(item.id)}/status`, {
        method: 'PATCH', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ active: !item.active }),
      })
      if (!response.ok) throw new Error('status failed')
      const saved = await response.json() as AuditedReference
      onChange(items.map(current => current.id === item.id ? saved : current))
    } catch { setError('Não foi possível alterar o estado do registo.') }
  }

  return <main className="workspace-page entities-page reference-management-page">
    <ContextualPageHeading trail={trail} title={title}/>
    <section className="panel list-panel">
      <div className="reference-toolbar">
        <div className="toolbar-actions"><button onClick={create}>+ Novo</button><button className="secondary" onClick={onBack}>Voltar ao colaborador</button><label>ID ou designação<input value={search} onChange={event => setSearch(event.target.value)} placeholder="Pesquisar…"/></label></div>
        <span className="results-summary">{visibleItems.length} de {items.length} registos</span>
      </div>
      <p className="reference-description">{description}</p>
      {error && !formOpen && <p className="error">{error}</p>}
      <div className="table-wrap reference-table"><table><thead><tr><th>ID</th><th>Designação</th><th>Estado</th><th>Criado</th><th>Última alteração</th><th>Ações</th></tr></thead><tbody>
        {visibleItems.map(item => <tr key={item.id}><td><strong className="customer-code">{item.id}</strong></td><td><strong>{item.designation}</strong></td><td><span className={item.active ? 'badge active' : 'badge'}>{item.active ? 'Ativo' : 'Inativo'}</span></td><td>{formatAuditDate(item.createdAt)}<small>{item.createdBy}</small></td><td>{formatAuditDate(item.updatedAt)}<small>{item.updatedBy}</small></td><td><div className="row-actions"><button className="secondary" onClick={() => edit(item)}>Editar</button><button className={item.active ? 'danger-link' : 'secondary'} onClick={() => void toggle(item)}>{item.active ? 'Inativar' : 'Ativar'}</button></div></td></tr>)}
      </tbody></table></div>
    </section>
    {formOpen && <div className="overlay" onMouseDown={event => { if (event.target === event.currentTarget) closeForm() }}><aside className="partial reference-partial" role="dialog" aria-modal="true" aria-labelledby="reference-form-title"><div className="partial-head"><div><h2 id="reference-form-title">{editingId ? editTitle : createTitle}</h2><p>Os campos assinalados com * são obrigatórios.</p></div><button className="close" onClick={closeForm} aria-label="Fechar">×</button></div><form className="reference-form" onSubmit={event => void save(event)}><label>ID *<input required disabled={Boolean(editingId)} value={id} onChange={event => setId(event.target.value)} placeholder={idHint}/><small>O ID é imutável depois da criação.</small></label><label>Designação *<input required maxLength={120} value={designation} onChange={event => setDesignation(event.target.value)}/></label>{error && <p className="error">{error}</p>}<div className="actions"><button type="button" className="secondary" onClick={closeForm}>Cancelar</button><button type="submit">Guardar</button></div></form></aside></div>}
  </main>
}

function formatAuditDate(value: string) {
  return new Intl.DateTimeFormat('pt-PT', { dateStyle: 'short', timeStyle: 'short' }).format(new Date(value))
}
