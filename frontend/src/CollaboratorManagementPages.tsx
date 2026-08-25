import { FormEvent, useMemo, useState } from 'react'
import { ContextualPageHeading } from './ContextualPageHeading'

export type AuditedReference = {
  id: string
  designation: string
  active: boolean
  createdAt: string
  createdBy: string
  updatedAt: string
  updatedBy: string
}

const initialAuditDate = '2026-08-25T00:00:00.000Z'

function reference(id: string, designation: string): AuditedReference {
  return { id, designation, active: true, createdAt: initialAuditDate, createdBy: 'Sistema', updatedAt: initialAuditDate, updatedBy: 'Sistema' }
}

export const initialAccountProfiles: AuditedReference[] = [
  reference('ADMIN', 'Administrador'),
  reference('OPERATOR', 'Operador'),
  reference('ACCOUNTING', 'Contabilidade'),
  reference('DRIVER', 'Motorista'),
  reference('FRONT_DESK', 'Atendedor de Balcão'),
]

export const initialProfessionalCategories: AuditedReference[] = [
  reference('1', 'Administrativo'),
  reference('2', 'Motoristas Ligeiros'),
  reference('3', 'Motoristas Pesados'),
  reference('4', 'Operadores de Armazém'),
  reference('5', 'Oficina'),
  reference('6', 'Outros'),
]

type ReferenceManagementPageProps = {
  actor: string
  createTitle: string
  description: string
  editTitle: string
  idHint: string
  items: AuditedReference[]
  onBack: () => void
  onChange: (items: AuditedReference[]) => void
  title: string
  trail: string[]
}

export function ReferenceManagementPage({ actor, createTitle, description, editTitle, idHint, items, onBack, onChange, title, trail }: ReferenceManagementPageProps) {
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

  function save(event: FormEvent) {
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
    const now = new Date().toISOString()
    if (editingId) {
      onChange(items.map(item => item.id === editingId ? { ...item, designation: normalizedDesignation, updatedAt: now, updatedBy: actor } : item))
    } else {
      onChange([...items, { id: normalizedId, designation: normalizedDesignation, active: true, createdAt: now, createdBy: actor, updatedAt: now, updatedBy: actor }])
    }
    closeForm()
  }

  function toggle(item: AuditedReference) {
    const now = new Date().toISOString()
    onChange(items.map(current => current.id === item.id ? { ...current, active: !current.active, updatedAt: now, updatedBy: actor } : current))
  }

  return <main className="workspace-page entities-page reference-management-page">
    <ContextualPageHeading trail={trail} title={title}/>
    <section className="panel list-panel">
      <div className="reference-toolbar">
        <div className="toolbar-actions"><button onClick={create}>+ Novo</button><button className="secondary" onClick={onBack}>Voltar ao colaborador</button><label>ID ou designação<input value={search} onChange={event => setSearch(event.target.value)} placeholder="Pesquisar…"/></label></div>
        <span className="results-summary">{visibleItems.length} de {items.length} registos</span>
      </div>
      <p className="reference-description">{description}</p>
      <div className="table-wrap reference-table"><table><thead><tr><th>ID</th><th>Designação</th><th>Estado</th><th>Criado</th><th>Última alteração</th><th>Ações</th></tr></thead><tbody>
        {visibleItems.map(item => <tr key={item.id}><td><strong className="customer-code">{item.id}</strong></td><td><strong>{item.designation}</strong></td><td><span className={item.active ? 'badge active' : 'badge'}>{item.active ? 'Ativo' : 'Inativo'}</span></td><td>{formatAuditDate(item.createdAt)}<small>{item.createdBy}</small></td><td>{formatAuditDate(item.updatedAt)}<small>{item.updatedBy}</small></td><td><div className="row-actions"><button className="secondary" onClick={() => edit(item)}>Editar</button><button className={item.active ? 'danger-link' : 'secondary'} onClick={() => toggle(item)}>{item.active ? 'Inativar' : 'Ativar'}</button></div></td></tr>)}
      </tbody></table></div>
    </section>
    {formOpen && <div className="overlay" onMouseDown={event => { if (event.target === event.currentTarget) closeForm() }}><aside className="partial reference-partial" role="dialog" aria-modal="true" aria-labelledby="reference-form-title"><div className="partial-head"><div><h2 id="reference-form-title">{editingId ? editTitle : createTitle}</h2><p>Os campos assinalados com * são obrigatórios.</p></div><button className="close" onClick={closeForm} aria-label="Fechar">×</button></div><form className="reference-form" onSubmit={save}><label>ID *<input required disabled={Boolean(editingId)} value={id} onChange={event => setId(event.target.value)} placeholder={idHint}/><small>O ID é imutável depois da criação.</small></label><label>Designação *<input required maxLength={120} value={designation} onChange={event => setDesignation(event.target.value)}/></label>{error && <p className="error">{error}</p>}<div className="actions"><button type="button" className="secondary" onClick={closeForm}>Cancelar</button><button type="submit">Guardar</button></div></form></aside></div>}
  </main>
}

function formatAuditDate(value: string) {
  return new Intl.DateTimeFormat('pt-PT', { dateStyle: 'short', timeStyle: 'short' }).format(new Date(value))
}
