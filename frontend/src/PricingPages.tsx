import { FormEvent, useEffect, useMemo, useState } from 'react'
import type { AuthSession } from './auth'
import { ContextualPageHeading } from './ContextualPageHeading'

const apiUrl = import.meta.env.VITE_API_URL ?? 'http://localhost:8080/api'
type ServiceCode = 'BUSINESS_PARCEL' | 'EXPRESS_PARCEL'
type PlanStatus = 'DRAFT' | 'ACTIVE' | 'ARCHIVED'
type Bracket = { upToWeightKg: number; price: number }
type PricingRoute = { id:string; serviceCode:ServiceCode; code:string; designation:string; destinationCountry:string; deliveryCommitment:string; volumetricFactor:number; maxPieceWeightKg:number; maxCombinedDimensionsCm:number|null; additionalStepKg:number; additionalStepPrice:number|null; enabled:boolean; sortOrder:number; brackets:Bracket[] }
type PlanSummary = { id:string; code:string; designation:string; version:number; validFrom:string; validTo:string|null; currency:string; fuelSurchargePercent:number; vatPercent:number; status:PlanStatus; routeCount:number; updatedAt:string; updatedBy:string }
type PlanDetail = PlanSummary & { createdAt:string; createdBy:string; routes:PricingRoute[] }
type Simulation = { routeDesignation:string; actualWeightKg:number; volumetricWeightKg:number; chargeableWeightKg:number; bracketWeightKg:number; additionalSteps:number; basePrice:number; fuelSurcharge:number; subtotal:number; vat:number; total:number; currency:string }

export function PricingPlansPage({ auth }: { auth: AuthSession }) {
  const [plans, setPlans] = useState<PlanSummary[]>([])
  const [selected, setSelected] = useState<PlanDetail | null>(null)
  const [service, setService] = useState<ServiceCode>('BUSINESS_PARCEL')
  const [routeId, setRouteId] = useState('')
  const [editing, setEditing] = useState(false)
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

  async function loadPlans(preferredId?: string) {
    setLoading(true)
    try {
      const response = await auth.fetch(`${apiUrl}/pricing/plans`)
      if (!response.ok) throw new Error('load')
      const values = await response.json() as PlanSummary[]
      setPlans(values)
      const id = preferredId ?? selected?.id ?? values[0]?.id
      if (id) await loadDetail(id)
      else setSelected(null)
      setError('')
    } catch { setError('Não foi possível carregar as tabelas de preços.'); setSelected(null) }
    finally { setLoading(false) }
  }

  async function loadDetail(id: string) {
    const response = await auth.fetch(`${apiUrl}/pricing/plans/${encodeURIComponent(id)}`)
    if (!response.ok) throw new Error('detail')
    const detail = await response.json() as PlanDetail
    setSelected(detail)
    const first = detail.routes.find(route => route.serviceCode === service) ?? detail.routes[0]
    if (first) { setService(first.serviceCode); setRouteId(first.id) }
    setEditing(false); setRouteDraft(null); setSimulation(null)
  }

  useEffect(() => { void loadPlans() }, [])
  const routes = useMemo(() => selected?.routes.filter(route => route.serviceCode === service) ?? [], [selected, service])
  const currentRoute = routes.find(route => route.id === routeId) ?? routes[0] ?? null

  function chooseService(value: ServiceCode) {
    setService(value)
    const first = selected?.routes.find(route => route.serviceCode === value)
    setRouteId(first?.id ?? '')
    setEditing(false); setRouteDraft(null); setSimulation(null)
  }

  function beginEdit() { if (currentRoute) { setRouteDraft(structuredClone(currentRoute)); setEditing(true); setError('') } }
  function updateBracket(index: number, field: keyof Bracket, value: string) {
    setRouteDraft(current => current ? { ...current, brackets: current.brackets.map((item, position) => position === index ? { ...item, [field]: Number(value) } : item) } : current)
  }

  async function saveRoute(event: FormEvent) {
    event.preventDefault()
    if (!selected || !routeDraft) return
    try {
      const response = await auth.fetch(`${apiUrl}/pricing/plans/${selected.id}/routes/${routeDraft.id}`, {
        method:'PUT', headers:{'Content-Type':'application/json'}, body:JSON.stringify({
          designation:routeDraft.designation, destinationCountry:routeDraft.destinationCountry,
          deliveryCommitment:routeDraft.deliveryCommitment, volumetricFactor:routeDraft.volumetricFactor,
          maxPieceWeightKg:routeDraft.maxPieceWeightKg, maxCombinedDimensionsCm:routeDraft.maxCombinedDimensionsCm,
          additionalStepKg:routeDraft.additionalStepKg, additionalStepPrice:routeDraft.additionalStepPrice,
          enabled:routeDraft.enabled, sortOrder:routeDraft.sortOrder, brackets:routeDraft.brackets,
        }),
      })
      if (!response.ok) throw new Error('save')
      await loadPlans(selected.id)
      setError('')
    } catch { setError('Não foi possível guardar a rota. Verifique os escalões e tente novamente.') }
  }

  async function activate() {
    if (!selected || !window.confirm('Ativar esta versão da tabela de preços? Depois de ativa deixa de ser editável.')) return
    try {
      const response = await auth.fetch(`${apiUrl}/pricing/plans/${selected.id}/status`, { method:'PATCH', headers:{'Content-Type':'application/json'}, body:JSON.stringify({status:'ACTIVE'}) })
      if (!response.ok) throw new Error('activate')
      await loadPlans(selected.id)
    } catch { setError('Não foi possível ativar. Confirme se Business Parcel e Express Parcel têm rotas válidas.') }
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
    } catch { setError('Não foi possível calcular. Confirme o peso, os volumes e os limites do serviço.') }
  }

  return <main className="workspace-page pricing-page">
    <ContextualPageHeading title="Tabelas de preços" trail={['Configuração comercial','Business e Express Parcel']}/>
    {error && <p className="error">{error}</p>}
    <section className="pricing-layout">
      <aside className="panel pricing-plan-list">
        <div className="pricing-list-head"><h2>Tabelas</h2><span>{plans.length}</span></div>
        {loading ? <p>A carregar…</p> : plans.map(plan => <button key={plan.id} className={selected?.id === plan.id ? 'is-selected' : ''} onClick={() => void loadDetail(plan.id)}><strong>{plan.designation}</strong><span>{plan.code} · v{plan.version}</span><small className={`pricing-status ${plan.status.toLowerCase()}`}>{statusLabel(plan.status)}</small></button>)}
      </aside>
      {selected && <div className="pricing-main">
        <section className="panel pricing-summary">
          <div><p className="eyebrow">{selected.code} · Versão {selected.version}</p><h1>{selected.designation}</h1><p>Válida de {formatDate(selected.validFrom)} a {selected.validTo ? formatDate(selected.validTo) : 'sem data final'} · Combustível {selected.fuelSurchargePercent}% · IVA {selected.vatPercent}%</p></div>
          <div className="pricing-summary-actions"><span className={`badge ${selected.status === 'ACTIVE' ? 'active' : ''}`}>{statusLabel(selected.status)}</span>{canManage && selected.status === 'DRAFT' && <button onClick={() => void activate()}>Ativar tabela</button>}</div>
        </section>
        <section className="panel pricing-editor">
          <div className="pricing-service-tabs"><button className={service === 'BUSINESS_PARCEL' ? 'active' : ''} onClick={() => chooseService('BUSINESS_PARCEL')}>Business Parcel</button><button className={service === 'EXPRESS_PARCEL' ? 'active' : ''} onClick={() => chooseService('EXPRESS_PARCEL')}>Express Parcel</button></div>
          <div className="pricing-route-toolbar"><label>Rota<select value={currentRoute?.id ?? ''} onChange={event => { setRouteId(event.target.value); setEditing(false); setRouteDraft(null); setSimulation(null) }}>{routes.map(route => <option key={route.id} value={route.id}>{route.designation}</option>)}</select></label>{canManage && selected.status === 'DRAFT' && currentRoute && !editing && <button className="secondary" onClick={beginEdit}>Editar rota e preços</button>}</div>
          {currentRoute && (editing && routeDraft ? <form className="pricing-route-form" onSubmit={event => void saveRoute(event)}>
            <div className="pricing-route-fields"><label>Designação<input value={routeDraft.designation} onChange={event => setRouteDraft({...routeDraft,designation:event.target.value})}/></label><label>País<input maxLength={2} value={routeDraft.destinationCountry} onChange={event => setRouteDraft({...routeDraft,destinationCountry:event.target.value.toUpperCase()})}/></label><label>Prazo<input value={routeDraft.deliveryCommitment} onChange={event => setRouteDraft({...routeDraft,deliveryCommitment:event.target.value})}/></label><label>Fator volumétrico<input type="number" min="1" step="0.001" value={routeDraft.volumetricFactor} onChange={event => setRouteDraft({...routeDraft,volumetricFactor:Number(event.target.value)})}/></label><label>Preço kg adicional<input type="number" min="0" step="0.01" value={routeDraft.additionalStepPrice ?? ''} onChange={event => setRouteDraft({...routeDraft,additionalStepPrice:event.target.value === '' ? null : Number(event.target.value)})}/></label></div>
            <RateTable brackets={routeDraft.brackets} currency={selected.currency} editing onUpdate={updateBracket}/><div className="actions"><button type="button" className="secondary" onClick={() => {setEditing(false);setRouteDraft(null)}}>Cancelar</button><button type="submit">Guardar rota</button></div>
          </form> : <><div className="route-facts"><span><small>Destino</small>{currentRoute.destinationCountry}</span><span><small>Prazo</small>{currentRoute.deliveryCommitment}</span><span><small>Fator volumétrico</small>{currentRoute.volumetricFactor} kg/m³</span><span><small>Máximo por volume</small>{currentRoute.maxPieceWeightKg} kg</span><span><small>Kg adicional</small>{currentRoute.additionalStepPrice == null ? 'Não disponível' : formatMoney(currentRoute.additionalStepPrice,selected.currency)}</span></div><RateTable brackets={currentRoute.brackets} currency={selected.currency}/></>)}
        </section>
        {currentRoute && <section className="panel pricing-simulator"><div className="panel-title"><div><h2>Simular preço</h2><p>O cálculo usa a tabela selecionada e não cria qualquer serviço ou fatura.</p></div></div><form onSubmit={event => void simulate(event)}><label>Peso real total (kg)<input required type="number" min="0.001" step="0.001" value={weight} onChange={event => setWeight(event.target.value)}/></label><label>N.º de volumes<input required type="number" min="1" step="1" value={parcels} onChange={event => setParcels(event.target.value)}/></label><label>Comprimento (cm)<input type="number" min="0.01" step="0.01" value={length} onChange={event => setLength(event.target.value)}/></label><label>Largura (cm)<input type="number" min="0.01" step="0.01" value={width} onChange={event => setWidth(event.target.value)}/></label><label>Altura (cm)<input type="number" min="0.01" step="0.01" value={height} onChange={event => setHeight(event.target.value)}/></label><button type="submit">Calcular</button></form>{simulation && <div className="simulation-result"><div><span>Peso real</span><strong>{simulation.actualWeightKg} kg</strong></div><div><span>Peso volumétrico</span><strong>{simulation.volumetricWeightKg} kg</strong></div><div><span>Peso faturável</span><strong>{simulation.chargeableWeightKg} kg</strong></div><div><span>Tarifa base</span><strong>{formatMoney(simulation.basePrice,simulation.currency)}</strong></div><div><span>Combustível</span><strong>{formatMoney(simulation.fuelSurcharge,simulation.currency)}</strong></div><div><span>IVA</span><strong>{formatMoney(simulation.vat,simulation.currency)}</strong></div><div className="total"><span>Total</span><strong>{formatMoney(simulation.total,simulation.currency)}</strong></div></div>}</section>}
      </div>}
    </section>
  </main>
}

function RateTable({brackets,currency,editing=false,onUpdate}:{brackets:Bracket[];currency:string;editing?:boolean;onUpdate?:(index:number,field:keyof Bracket,value:string)=>void}) {
  return <div className="table-wrap pricing-rate-table"><table><thead><tr><th>Até (kg)</th><th>Preço</th></tr></thead><tbody>{brackets.map((bracket,index)=><tr key={`${bracket.upToWeightKg}-${index}`}><td>{editing?<input type="number" min="0.001" step="0.001" value={bracket.upToWeightKg} onChange={event=>onUpdate?.(index,'upToWeightKg',event.target.value)}/>:bracket.upToWeightKg}</td><td>{editing?<input type="number" min="0" step="0.01" value={bracket.price} onChange={event=>onUpdate?.(index,'price',event.target.value)}/>:formatMoney(bracket.price,currency)}</td></tr>)}</tbody></table></div>
}
function formatMoney(value:number,currency:string){return new Intl.NumberFormat('pt-PT',{style:'currency',currency}).format(value)}
function formatDate(value:string){return new Intl.DateTimeFormat('pt-PT').format(new Date(`${value}T12:00:00`))}
function statusLabel(value:PlanStatus){return value==='DRAFT'?'Rascunho':value==='ACTIVE'?'Ativa':'Arquivada'}
