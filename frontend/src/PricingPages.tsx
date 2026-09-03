import { FormEvent, useEffect, useMemo, useState } from 'react'
import type { AuthSession } from './auth'
import { ContextualPageHeading } from './ContextualPageHeading'

const apiUrl = import.meta.env.VITE_PRICING_API_URL ?? 'http://localhost:8086/api'
type PlanStatus = 'DRAFT' | 'ACTIVE' | 'ARCHIVED'
type EditorMode = 'idle' | 'create' | 'edit'
type Bracket = { upToWeightKg: number; price: number }
type PricingRoute = { id:string; code:string; designation:string; destinationCountry:string; deliveryCommitment:string; volumetricFactor:number; maxPieceWeightKg:number; maxCombinedDimensionsCm:number|null; additionalStepKg:number; additionalStepPrice:number|null; enabled:boolean; sortOrder:number; brackets:Bracket[] }
type PlanSummary = { id:string; code:string; designation:string; version:number; validFrom:string; validTo:string|null; currency:string; fuelSurchargePercent:number; vatPercent:number; status:PlanStatus; routeCount:number; updatedAt:string; updatedBy:string }
type PlanDetail = PlanSummary & { createdAt:string; createdBy:string; routes:PricingRoute[] }
type Simulation = { routeDesignation:string; actualWeightKg:number; volumetricWeightKg:number; chargeableWeightKg:number; bracketWeightKg:number; additionalSteps:number; basePrice:number; fuelSurcharge:number; subtotal:number; vat:number; total:number; currency:string }

export function PricingPlansPage({ auth }: { auth: AuthSession }) {
  const [plans, setPlans] = useState<PlanSummary[]>([])
  const [selected, setSelected] = useState<PlanDetail | null>(null)
  const [routeId, setRouteId] = useState('')
  const [mode, setMode] = useState<EditorMode>('idle')
  const [routeDraft, setRouteDraft] = useState<PricingRoute | null>(null)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(true)
  const [weight, setWeight] = useState('1')
  const [parcels, setParcels] = useState('1')
  const [length, setLength] = useState('')
  const [width, setWidth] = useState('')
  const [height, setHeight] = useState('')
  const [simulation, setSimulation] = useState<Simulation | null>(null)
  const canManage = auth.roles.includes('ADMIN')

  async function loadPlans(preferredPlanId?: string, preferredRouteId?: string) {
    setLoading(true)
    try {
      const response = await auth.fetch(`${apiUrl}/pricing/plans`)
      if (!response.ok) throw new Error('load')
      const values = await response.json() as PlanSummary[]
      setPlans(values)
      const id = preferredPlanId ?? selected?.id ?? values[0]?.id
      if (id) await loadDetail(id, preferredRouteId)
      else setSelected(null)
      setError('')
    } catch { setError('Não foi possível carregar as tabelas de preços.'); setSelected(null) }
    finally { setLoading(false) }
  }

  async function loadDetail(id: string, preferredRouteId?: string) {
    const response = await auth.fetch(`${apiUrl}/pricing/plans/${encodeURIComponent(id)}`)
    if (!response.ok) throw new Error('detail')
    const detail = await response.json() as PlanDetail
    setSelected(detail)
    const route = detail.routes.find(value => value.id === preferredRouteId) ?? detail.routes[0]
    setRouteId(route?.id ?? '')
    setMode('idle'); setRouteDraft(null); setSimulation(null)
  }

  useEffect(() => { void loadPlans() }, [])
  const routes = useMemo(() => [...(selected?.routes ?? [])].sort((left, right) => left.sortOrder - right.sortOrder), [selected])
  const currentRoute = routes.find(route => route.id === routeId) ?? null

  function chooseRoute(route: PricingRoute) {
    setRouteId(route.id); setMode('idle'); setRouteDraft(null); setSimulation(null); setError('')
  }
  function beginCreate() {
    const nextOrder = (routes.at(-1)?.sortOrder ?? 0) + 10
    setRouteId('')
    setRouteDraft({ id:'', code:'', designation:'', destinationCountry:'PT', deliveryCommitment:'', volumetricFactor:167,
      maxPieceWeightKg:40, maxCombinedDimensionsCm:null, additionalStepKg:1, additionalStepPrice:null,
      enabled:true, sortOrder:nextOrder, brackets:[{upToWeightKg:1,price:0}] })
    setMode('create'); setSimulation(null); setError('')
  }
  function beginEdit() {
    if (currentRoute) { setRouteDraft(structuredClone(currentRoute)); setMode('edit'); setError('') }
  }
  function cancelEdit() {
    setMode('idle'); setRouteDraft(null); setRouteId(currentRoute?.id ?? routes[0]?.id ?? '')
  }
  function updateBracket(index: number, field: keyof Bracket, value: string) {
    setRouteDraft(current => current ? { ...current, brackets: current.brackets.map((item, position) => position === index ? { ...item, [field]: Number(value) } : item) } : current)
  }
  function addBracket() {
    setRouteDraft(current => {
      if (!current) return current
      const lastWeight = current.brackets.at(-1)?.upToWeightKg ?? 0
      return { ...current, brackets: [...current.brackets, { upToWeightKg: lastWeight + 1, price: 0 }] }
    })
  }
  function removeBracket(index: number) {
    setRouteDraft(current => current && current.brackets.length > 1
      ? { ...current, brackets: current.brackets.filter((_, position) => position !== index) }
      : current)
  }

  async function saveRoute(event: FormEvent) {
    event.preventDefault()
    if (!selected || !routeDraft || mode === 'idle') return
    const values = {
      code:routeDraft.code, designation:routeDraft.designation, destinationCountry:routeDraft.destinationCountry,
      deliveryCommitment:routeDraft.deliveryCommitment, volumetricFactor:routeDraft.volumetricFactor,
      maxPieceWeightKg:routeDraft.maxPieceWeightKg, maxCombinedDimensionsCm:routeDraft.maxCombinedDimensionsCm,
      additionalStepKg:routeDraft.additionalStepKg, additionalStepPrice:routeDraft.additionalStepPrice,
      enabled:routeDraft.enabled, sortOrder:routeDraft.sortOrder, brackets:routeDraft.brackets,
    }
    try {
      const creating = mode === 'create'
      const url = creating ? `${apiUrl}/pricing/plans/${selected.id}/routes` : `${apiUrl}/pricing/plans/${selected.id}/routes/${routeDraft.id}`
      const response = await auth.fetch(url, {
        method:creating ? 'POST' : 'PUT', headers:{'Content-Type':'application/json'},
        body:JSON.stringify(values),
      })
      if (!response.ok) throw new Error('save')
      const saved = await response.json() as PricingRoute
      await loadPlans(selected.id, saved.id)
      setError('')
    } catch { setError('Não foi possível guardar a rota. Verifique os dados e os escalões.') }
  }

  async function deleteRoute() {
    if (!selected || !currentRoute || !window.confirm(`Eliminar a rota "${currentRoute.designation}"?`)) return
    try {
      const response = await auth.fetch(`${apiUrl}/pricing/plans/${selected.id}/routes/${currentRoute.id}`, {method:'DELETE'})
      if (!response.ok) throw new Error('delete')
      await loadPlans(selected.id)
    } catch { setError('Não foi possível eliminar a rota.') }
  }

  async function activate() {
    if (!selected || !window.confirm('Ativar esta versão da tabela de preços? Depois de ativa deixa de ser editável.')) return
    try {
      const response = await auth.fetch(`${apiUrl}/pricing/plans/${selected.id}/status`, { method:'PATCH', headers:{'Content-Type':'application/json'}, body:JSON.stringify({status:'ACTIVE'}) })
      if (!response.ok) throw new Error('activate')
      await loadPlans(selected.id)
    } catch { setError('Não foi possível ativar. Confirme se existe pelo menos uma rota ativa com preços válidos.') }
  }

  async function simulate(event: FormEvent) {
    event.preventDefault(); setSimulation(null)
    if (!selected || !currentRoute) return
    const dimensions = [length, width, height]
    if (dimensions.some(Boolean) && !dimensions.every(Boolean)) { setError('Preencha as três dimensões ou deixe todas vazias.'); return }
    try {
      const response = await auth.fetch(`${apiUrl}/pricing/simulations`, { method:'POST', headers:{'Content-Type':'application/json'}, body:JSON.stringify({
        planId:selected.id, routeCode:currentRoute.code, actualWeightKg:Number(weight), parcelCount:Number(parcels),
        lengthCm:length ? Number(length) : null, widthCm:width ? Number(width) : null, heightCm:height ? Number(height) : null,
      }) })
      if (!response.ok) throw new Error('simulation')
      setSimulation(await response.json() as Simulation); setError('')
    } catch { setError('Não foi possível calcular. Confirme o peso, os volumes e os limites da rota.') }
  }

  return <main className="workspace-page pricing-page">
    <ContextualPageHeading title="Tabelas de preços" trail={['Configuração comercial','Rotas e preços']}/>
    {error && <p className="error">{error}</p>}
    <section className="pricing-layout">
      <aside className="panel pricing-plan-list">
        <div className="pricing-list-head"><h2>Tabelas</h2><span>{plans.length}</span></div>
        {loading ? <p>A carregar…</p> : plans.map(plan => <button key={plan.id} className={selected?.id === plan.id ? 'is-selected' : ''} onClick={() => void loadDetail(plan.id)}><strong>{plan.designation}</strong><span>{plan.code} · v{plan.version}</span><small className={`pricing-status ${plan.status.toLowerCase()}`}>{statusLabel(plan.status)}</small></button>)}
      </aside>
      {selected && <div className="pricing-main">
        <section className="panel pricing-summary">
          <div><p className="eyebrow">{selected.code} · Versão {selected.version}</p><h1>{selected.designation}</h1><p>Válida de {formatDate(selected.validFrom)} a {selected.validTo ? formatDate(selected.validTo) : 'sem data final'} · Combustível {selected.fuelSurchargePercent}% · IVA {selected.vatPercent}%</p></div>
          <div className="pricing-summary-actions"><span className={`badge ${selected.status === 'ACTIVE' ? 'active' : ''}`}>{statusLabel(selected.status)}</span>{canManage && selected.status === 'DRAFT' && routes.length > 0 && <button onClick={() => void activate()}>Ativar tabela</button>}</div>
        </section>
        <section className="panel pricing-editor">
          <div className="pricing-accordion-heading"><div><h2>Rotas</h2><p>Cada rota tem a sua própria configuração e tabela de preços.</p></div>{canManage && selected.status === 'DRAFT' && mode === 'idle' && <button type="button" onClick={beginCreate}>+ Adicionar rota</button>}</div>
          {routes.length > 0 ? <div className="pricing-route-tabs" role="tablist" aria-label="Rotas da tabela de preços">
            {routes.map(route => <button type="button" role="tab" id={`pricing-route-tab-${route.id}`} aria-controls={`pricing-route-panel-${route.id}`} aria-expanded={currentRoute?.id === route.id} aria-selected={currentRoute?.id === route.id} className={currentRoute?.id === route.id ? 'active' : ''} key={route.id} onClick={() => chooseRoute(route)}>
              <strong>{route.designation}</strong><small>{route.destinationCountry} · {route.deliveryCommitment}</small>
            </button>)}
          </div> : mode !== 'create' && <div className="pricing-empty"><strong>Ainda não existem rotas.</strong><span>Adicione a primeira rota para introduzir os respetivos escalões de preço.</span></div>}

          {mode !== 'idle' && routeDraft ? <div className="pricing-route-panel"><div className="pricing-route-toolbar"><div><small>{mode === 'create' ? 'Nova rota' : 'Editar rota'}</small><strong>{routeDraft.designation || 'Sem designação'}</strong><span>{routeDraft.code || 'Código por definir'}</span></div></div>
            <form className="pricing-route-form" onSubmit={event => void saveRoute(event)}>
              <RouteFields route={routeDraft} onChange={setRouteDraft}/>
              <div className="pricing-rate-heading"><div><h3>Escalões de peso</h3><p>Introduza os preços sem combustível e sem IVA.</p></div><button type="button" className="secondary" onClick={addBracket}>+ Adicionar escalão</button></div>
              <RateTable brackets={routeDraft.brackets} currency={selected.currency} editing onUpdate={updateBracket} onRemove={removeBracket}/>
              <div className="actions"><button type="button" className="secondary" onClick={cancelEdit}>Cancelar</button><button type="submit">Guardar rota</button></div>
            </form>
          </div> : currentRoute && <div id={`pricing-route-panel-${currentRoute.id}`} role="tabpanel" aria-labelledby={`pricing-route-tab-${currentRoute.id}`}><div className="pricing-route-toolbar"><div><small>Rota</small><strong>{currentRoute.designation}</strong><span>{currentRoute.code}</span></div>{canManage && selected.status === 'DRAFT' && <div className="pricing-route-actions"><button className="secondary" onClick={beginEdit}>Editar rota</button><button className="danger-link" onClick={() => void deleteRoute()}>Eliminar</button></div>}</div>
            <div className="route-facts"><span><small>Destino</small>{currentRoute.destinationCountry}</span><span><small>Prazo</small>{currentRoute.deliveryCommitment}</span><span><small>Fator volumétrico</small>{currentRoute.volumetricFactor} kg/m³</span><span><small>Máximo por volume</small>{currentRoute.maxPieceWeightKg} kg</span><span><small>Kg adicional</small>{currentRoute.additionalStepPrice == null ? 'Não disponível' : formatMoney(currentRoute.additionalStepPrice,selected.currency)}</span></div><RateTable brackets={currentRoute.brackets} currency={selected.currency}/>
          </div>}
        </section>
        {currentRoute && mode === 'idle' && <section className="panel pricing-simulator"><div className="panel-title"><div><h2>Simular preço</h2><p>O cálculo usa a rota selecionada e não cria qualquer envio ou fatura.</p></div></div><form onSubmit={event => void simulate(event)}><label>Peso real total (kg)<input required type="number" min="0.001" step="0.001" value={weight} onChange={event => setWeight(event.target.value)}/></label><label>N.º de volumes<input required type="number" min="1" step="1" value={parcels} onChange={event => setParcels(event.target.value)}/></label><label>Comprimento (cm)<input type="number" min="0.01" step="0.01" value={length} onChange={event => setLength(event.target.value)}/></label><label>Largura (cm)<input type="number" min="0.01" step="0.01" value={width} onChange={event => setWidth(event.target.value)}/></label><label>Altura (cm)<input type="number" min="0.01" step="0.01" value={height} onChange={event => setHeight(event.target.value)}/></label><button type="submit">Calcular</button></form>{simulation && <div className="simulation-result"><div><span>Peso real</span><strong>{simulation.actualWeightKg} kg</strong></div><div><span>Peso volumétrico</span><strong>{simulation.volumetricWeightKg} kg</strong></div><div><span>Peso faturável</span><strong>{simulation.chargeableWeightKg} kg</strong></div><div><span>Tarifa base</span><strong>{formatMoney(simulation.basePrice,simulation.currency)}</strong></div><div><span>Combustível</span><strong>{formatMoney(simulation.fuelSurcharge,simulation.currency)}</strong></div><div><span>IVA</span><strong>{formatMoney(simulation.vat,simulation.currency)}</strong></div><div className="total"><span>Total</span><strong>{formatMoney(simulation.total,simulation.currency)}</strong></div></div>}</section>}
      </div>}
    </section>
  </main>
}

function RouteFields({route,onChange}:{route:PricingRoute;onChange:(route:PricingRoute)=>void}) {
  return <div className="pricing-route-fields">
    <label>Código<input required maxLength={60} value={route.code} onChange={event=>onChange({...route,code:event.target.value.toUpperCase()})}/></label>
    <label>Designação<input required maxLength={160} value={route.designation} onChange={event=>onChange({...route,designation:event.target.value})}/></label>
    <label>País<input required maxLength={2} value={route.destinationCountry} onChange={event=>onChange({...route,destinationCountry:event.target.value.toUpperCase()})}/></label>
    <label>Prazo<input required maxLength={40} value={route.deliveryCommitment} onChange={event=>onChange({...route,deliveryCommitment:event.target.value})}/></label>
    <label>Fator volumétrico<input required type="number" min="0.001" step="0.001" value={route.volumetricFactor} onChange={event=>onChange({...route,volumetricFactor:Number(event.target.value)})}/></label>
    <label>Peso máx./volume<input required type="number" min="0.001" step="0.001" value={route.maxPieceWeightKg} onChange={event=>onChange({...route,maxPieceWeightKg:Number(event.target.value)})}/></label>
    <label>Dimensões máx. (cm)<input type="number" min="0.01" step="0.01" value={route.maxCombinedDimensionsCm ?? ''} onChange={event=>onChange({...route,maxCombinedDimensionsCm:event.target.value===''?null:Number(event.target.value)})}/></label>
    <label>Passo kg adicional<input required type="number" min="0.001" step="0.001" value={route.additionalStepKg} onChange={event=>onChange({...route,additionalStepKg:Number(event.target.value)})}/></label>
    <label>Preço kg adicional<input type="number" min="0" step="0.01" value={route.additionalStepPrice ?? ''} onChange={event=>onChange({...route,additionalStepPrice:event.target.value===''?null:Number(event.target.value)})}/></label>
    <label>Ordem<input required type="number" min="0" step="1" value={route.sortOrder} onChange={event=>onChange({...route,sortOrder:Number(event.target.value)})}/></label>
    <label className="pricing-enabled"><input type="checkbox" checked={route.enabled} onChange={event=>onChange({...route,enabled:event.target.checked})}/> Rota ativa</label>
  </div>
}

function RateTable({brackets,currency,editing=false,onUpdate,onRemove}:{brackets:Bracket[];currency:string;editing?:boolean;onUpdate?:(index:number,field:keyof Bracket,value:string)=>void;onRemove?:(index:number)=>void}) {
  return <div className="table-wrap pricing-rate-table"><table><thead><tr><th>Até (kg)</th><th>Preço</th>{editing && <th className="pricing-rate-action">Ação</th>}</tr></thead><tbody>{brackets.map((bracket,index)=><tr key={`${bracket.upToWeightKg}-${index}`}><td>{editing?<input aria-label={`Peso máximo do escalão ${index + 1}`} required type="number" min="0.001" step="0.001" value={bracket.upToWeightKg} onChange={event=>onUpdate?.(index,'upToWeightKg',event.target.value)}/>:bracket.upToWeightKg}</td><td>{editing?<input aria-label={`Preço do escalão ${index + 1}`} required type="number" min="0" step="0.01" value={bracket.price} onChange={event=>onUpdate?.(index,'price',event.target.value)}/>:formatMoney(bracket.price,currency)}</td>{editing && <td className="pricing-rate-action"><button type="button" className="danger-link" disabled={brackets.length === 1} onClick={() => onRemove?.(index)}>Remover</button></td>}</tr>)}</tbody></table></div>
}
function formatMoney(value:number,currency:string){return new Intl.NumberFormat('pt-PT',{style:'currency',currency}).format(value)}
function formatDate(value:string){return new Intl.DateTimeFormat('pt-PT').format(new Date(`${value}T12:00:00`))}
function statusLabel(value:PlanStatus){return value==='DRAFT'?'Rascunho':value==='ACTIVE'?'Ativa':'Arquivada'}
