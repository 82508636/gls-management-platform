import { FormEvent, useEffect, useState } from 'react'
import type { AuthSession, Role } from './auth'

type IdentityUser = {
  id: string
  username: string
  email: string
  firstName: string
  lastName: string
  enabled: boolean
  roles: Role[]
}

const roles: Role[] = ['ADMIN', 'OPERATOR', 'ACCOUNTING', 'CUSTOMER']
const apiUrl = import.meta.env.VITE_API_URL ?? 'http://localhost:8080/api'
const emptyJoiner = { username: '', email: '', firstName: '', lastName: '', role: 'OPERATOR' as Role, temporaryPassword: '' }

export function AdminUsersPage({ auth, onBack }: { auth: AuthSession; onBack: () => void }) {
  const [users, setUsers] = useState<IdentityUser[]>([])
  const [joiner, setJoiner] = useState(emptyJoiner)
  const [formOpen, setFormOpen] = useState(false)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  async function load() {
    setLoading(true); setError('')
    try {
      const response = await auth.fetch(`${apiUrl}/admin/users`)
      if (!response.ok) throw new Error('users request failed')
      setUsers(await response.json())
    } catch { setError('Não foi possível carregar os utilizadores. Tente novamente.') }
    finally { setLoading(false) }
  }
  useEffect(() => { void load() }, [])

  async function create(event: FormEvent) {
    event.preventDefault(); setError('')
    try {
      const response = await auth.fetch(`${apiUrl}/admin/users`, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(joiner) })
      if (!response.ok) throw new Error('joiner request failed')
      setFormOpen(false); setJoiner(emptyJoiner); await load()
    } catch { setError('Não foi possível criar o utilizador. Tente novamente.') }
  }

  async function move(user: IdentityUser, role: Role) {
    setError('')
    try {
      const response = await auth.fetch(`${apiUrl}/admin/users/${encodeURIComponent(user.id)}/role`, { method: 'PUT', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ role }) })
      if (!response.ok) throw new Error('mover request failed')
      await load()
    } catch { setError('Não foi possível alterar o perfil. Tente novamente.') }
  }

  async function leave(user: IdentityUser) {
    if (!window.confirm(`Desativar o utilizador ${user.username} e terminar as suas sessões?`)) return
    setError('')
    try {
      const response = await auth.fetch(`${apiUrl}/admin/users/${encodeURIComponent(user.id)}/leave`, { method: 'POST' })
      if (!response.ok) throw new Error('leaver request failed')
      await load()
    } catch { setError('Não foi possível desativar o utilizador. Tente novamente.') }
  }

  return <main>
    <button className="back-link" onClick={onBack}>← Voltar aos clientes</button>
    <section className="hero"><div><p className="eyebrow">Administração</p><h1>Utilizadores</h1><p>Gestão Joiner–Mover–Leaver dos acessos à plataforma.</p></div><button onClick={() => setFormOpen(true)}>+ Novo utilizador</button></section>
    <section className="panel">
      <div className="panel-title"><h2>Perfis e acessos</h2><p>As alterações são aplicadas no Keycloak e registadas para auditoria.</p></div>
      {error && <p className="error">{error}</p>}
      {loading ? <p>A carregar…</p> : <div className="table-wrap"><table><thead><tr><th>Utilizador</th><th>Email</th><th>Perfil</th><th>Estado</th><th>Ações</th></tr></thead><tbody>
        {users.map(user => <tr key={user.id}><td><strong>{[user.firstName, user.lastName].filter(Boolean).join(' ') || user.username}</strong><small>{user.username}</small></td><td>{user.email || '—'}</td><td><select aria-label={`Perfil de ${user.username}`} value={user.roles[0] ?? ''} disabled={!user.enabled} onChange={event => void move(user, event.target.value as Role)}><option value="" disabled>Sem perfil</option>{roles.map(role => <option key={role}>{role}</option>)}</select></td><td><span className={user.enabled ? 'badge active' : 'badge'}>{user.enabled ? 'Ativo' : 'Inativo'}</span></td><td>{user.enabled ? <button className="danger-link" onClick={() => void leave(user)}>Desativar</button> : <span>—</span>}</td></tr>)}
      </tbody></table></div>}
    </section>
    {formOpen && <div className="overlay" onMouseDown={event => { if (event.target === event.currentTarget) setFormOpen(false) }}><aside className="partial" role="dialog" aria-modal="true" aria-labelledby="joiner-title"><div className="partial-head"><div><h2 id="joiner-title">Novo utilizador</h2><p>Todos os campos são obrigatórios.</p></div><button className="close" onClick={() => setFormOpen(false)} aria-label="Fechar">×</button></div><form onSubmit={create}>
      <label>Nome<input required maxLength={100} value={joiner.firstName} onChange={event => setJoiner(current => ({ ...current, firstName: event.target.value }))} /></label>
      <label>Apelido<input required maxLength={100} value={joiner.lastName} onChange={event => setJoiner(current => ({ ...current, lastName: event.target.value }))} /></label>
      <label>Username<input required maxLength={100} autoComplete="off" value={joiner.username} onChange={event => setJoiner(current => ({ ...current, username: event.target.value }))} /></label>
      <label>Email<input required type="email" maxLength={254} value={joiner.email} onChange={event => setJoiner(current => ({ ...current, email: event.target.value }))} /></label>
      <label>Perfil<select value={joiner.role} onChange={event => setJoiner(current => ({ ...current, role: event.target.value as Role }))}>{roles.map(role => <option key={role}>{role}</option>)}</select></label>
      <label>Password temporária<input required minLength={12} maxLength={128} type="password" autoComplete="new-password" value={joiner.temporaryPassword} onChange={event => setJoiner(current => ({ ...current, temporaryPassword: event.target.value }))} /></label>
      <div className="actions wide"><button type="button" className="secondary" onClick={() => setFormOpen(false)}>Cancelar</button><button type="submit">Criar utilizador</button></div>
    </form></aside></div>}
  </main>
}
