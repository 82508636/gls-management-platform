import { useEffect, useRef, useState } from 'react'
import type { Customer } from './domain'

type Form = Omit<Customer, 'id'>
export type VatValidationRequest = {
  countryCode: string
  vatNumber: string
  subjectType: Form['customerType']
}
export type VatValidationResult = {
  countryCode: string
  vatNumber: string
  formatValid: boolean
  viesStatus: 'VALID' | 'NOT_VALID' | 'NOT_APPLICABLE' | 'NOT_CHECKED' | 'UNAVAILABLE'
  checkedAt: string
}
type Props = {
  form: Form
  editing: boolean
  update: <K extends keyof Form>(field: K, value: Form[K]) => void
  validateVat: (request: VatValidationRequest) => Promise<VatValidationResult>
}

const countries = [
  ['PT', 'Portugal'], ['DE', 'Alemanha'], ['AT', 'Áustria'], ['BE', 'Bélgica'],
  ['BG', 'Bulgária'], ['CY', 'Chipre'], ['HR', 'Croácia'], ['DK', 'Dinamarca'],
  ['SK', 'Eslováquia'], ['SI', 'Eslovénia'], ['ES', 'Espanha'], ['EE', 'Estónia'],
  ['FI', 'Finlândia'], ['FR', 'França'], ['GR', 'Grécia'], ['HU', 'Hungria'],
  ['IE', 'Irlanda'], ['XI', 'Irlanda do Norte'], ['IT', 'Itália'], ['LV', 'Letónia'],
  ['LT', 'Lituânia'], ['LU', 'Luxemburgo'], ['MT', 'Malta'], ['NL', 'Países Baixos'],
  ['PL', 'Polónia'], ['CZ', 'Chéquia'], ['RO', 'Roménia'], ['SE', 'Suécia'],
]

export function CustomerFormFields({ form, editing, update, validateVat }: Props) {
  const [validation, setValidation] = useState<VatValidationResult | null>(null)
  const [validating, setValidating] = useState(false)
  const [validationError, setValidationError] = useState('')
  const validationSequence = useRef(0)

  useEffect(() => {
    validationSequence.current += 1
    setValidating(false)
    setValidation(null)
    setValidationError('')
  }, [form.billingCountry, form.vatNumber, form.customerType])

  async function handleVatValidation() {
    if (!form.billingCountry || !form.vatNumber.trim() || validating) return
    const sequence = ++validationSequence.current
    setValidating(true)
    setValidation(null)
    setValidationError('')
    try {
      const result = await validateVat({
        countryCode: form.billingCountry,
        vatNumber: form.vatNumber,
        subjectType: form.customerType,
      })
      if (validationSequence.current === sequence) setValidation(result)
    } catch {
      if (validationSequence.current === sequence) setValidationError('Não foi possível validar o NIF. Tente novamente.')
    } finally {
      if (validationSequence.current === sequence) setValidating(false)
    }
  }

  return <>
    <section className="form-section wide">
      <h3>Dados do cliente</h3>
      <div className="form-grid customer-top">
        <label className="col-2">Código<input disabled value={editing ? form.customerCode : ''} placeholder={editing ? '' : 'Após criação'} /><small>{editing ? 'Código definitivo' : `Será atribuído pela agência ${form.agency}`}</small></label>
        <label className="col-2">Abrev.<input maxLength={30} value={form.abbreviation ?? ''} onChange={event => update('abbreviation', event.target.value)} /></label>
        <label className="col-5">Designação para Expedição *<input autoFocus required maxLength={200} value={form.shippingName} onChange={event => update('shippingName', event.target.value)} /></label>
        <label className="col-3">Agência *<select required disabled={editing} value={form.agency} onChange={event => update('agency', event.target.value as Form['agency'])}><option value="LTFT01">[LTFT01] Fafe</option><option value="LTFT02">[LTFT02] Taipas</option></select><small>{editing ? 'Não alterável após criação' : form.agency === 'LTFT01' ? 'Código iniciado por 1' : 'Código iniciado por 2'}</small></label>
        <label className="col-12">Morada<input value={form.address ?? ''} onChange={event => update('address', event.target.value)} /></label>
        <label className="col-2">Código Postal<input maxLength={20} value={form.postalCode ?? ''} onChange={event => update('postalCode', event.target.value)} /></label>
        <label className="col-7">Localidade / Município<input maxLength={120} value={form.locality ?? ''} onChange={event => update('locality', event.target.value)} /></label>
        <label className="col-3">País<select value={form.country ?? 'PT'} onChange={event => update('country', event.target.value)}>{countries.map(([value, name]) => <option key={value} value={value}>{name}</option>)}</select></label>
        <label className="col-6">Email de contacto<input type="email" value={form.contactEmail ?? ''} onChange={event => update('contactEmail', event.target.value)} /></label>
        <label className="col-3">Telemóvel<input value={form.mobile ?? ''} onChange={event => update('mobile', event.target.value)} /></label>
        <label className="col-3">Telefone<input value={form.phone ?? ''} onChange={event => update('phone', event.target.value)} /></label>
      </div>
    </section>

    <section className="form-section wide">
      <h3>Dados de faturação</h3>
      <div className="form-grid">
        <label className="col-2">País<select value={form.billingCountry ?? 'PT'} onChange={event => update('billingCountry', event.target.value)}>{countries.map(([value, name]) => <option key={value} value={value}>{name}</option>)}</select></label>
        <label className="col-3">NIF *
          <div className="inline-field"><input required maxLength={32} value={form.vatNumber} onChange={event => update('vatNumber', event.target.value)} aria-describedby="vat-validation-result" /><button type="button" className="secondary" disabled={validating || !form.billingCountry || !form.vatNumber.trim()} onClick={() => void handleVatValidation()}>{validating ? 'A validar…' : 'Validar'}</button></div>
          <small>Validação independente dos restantes dados de faturação</small>
        </label>
        <label className="col-7">Designação social<input maxLength={200} value={form.billingLegalName ?? ''} onChange={event => update('billingLegalName', event.target.value)} /></label>
        {(validation || validationError) && <div id="vat-validation-result" className={`vat-validation col-12 ${validationClass(validation)}`} role="status" aria-live="polite">{validationError || validationMessage(validation!)}</div>}
        <label className="col-12">Morada<input value={form.billingAddress ?? ''} onChange={event => update('billingAddress', event.target.value)} /></label>
        <label className="col-2">Código Postal<input value={form.billingPostalCode ?? ''} onChange={event => update('billingPostalCode', event.target.value)} /></label>
        <label className="col-4">Localidade / Município<input value={form.billingLocality ?? ''} onChange={event => update('billingLocality', event.target.value)} /></label>
        <label className="col-3">Cód. Conta <span className="field-note">Em avaliação</span><input value={form.accountCode ?? ''} onChange={event => update('accountCode', event.target.value)} /></label>
        <label className="col-3">Ref. Faturação <span className="field-note">Em avaliação</span><input value={form.billingReference ?? ''} onChange={event => update('billingReference', event.target.value)} /></label>
        <label className="col-2">Tipo<select value={form.customerType} onChange={event => update('customerType', event.target.value as Form['customerType'])}><option value="COMPANY">Empresa</option><option value="PRIVATE">Particular</option></select></label>
        <label className="col-3">Responsável<input value={form.responsibleName ?? ''} onChange={event => update('responsibleName', event.target.value)} /></label>
        <label className="col-4">Email de faturação<input type="email" value={form.billingEmail ?? ''} onChange={event => update('billingEmail', event.target.value)} /></label>
        <label className="col-3">Doc. por defeito<select value={form.defaultDocument ?? 'INVOICE'} onChange={event => update('defaultDocument', event.target.value)}><option value="INVOICE">Fatura</option><option value="INVOICE_RECEIPT">Fatura-recibo</option><option value="CREDIT_NOTE">Nota de crédito</option></select></label>
        <label className="col-2">Câmbio <span className="field-note">Em avaliação</span><input min="0" step="0.000001" type="number" value={form.exchangeRate ?? ''} onChange={event => update('exchangeRate', event.target.value === '' ? null : Number(event.target.value))} /></label>
        <label className="col-2">Moeda<select value={form.currency ?? 'EUR'} onChange={event => update('currency', event.target.value)}><option>EUR</option><option>USD</option><option>GBP</option><option>CHF</option></select></label>
        <div className="check-row col-8"><label className="checkbox"><input type="checkbox" checked={form.invoiceByPost} onChange={event => update('invoiceByPost', event.target.checked)} /> Recebe fatura por correio</label><label className="checkbox"><input type="checkbox" checked={form.documentsByEmail} onChange={event => update('documentsByEmail', event.target.checked)} /> Recebe documentação por email</label><label className="checkbox"><input type="checkbox" checked={form.active} onChange={event => update('active', event.target.checked)} /> Cliente ativo</label></div>
      </div>
    </section>
  </>
}

function validationClass(validation: VatValidationResult | null) {
  if (!validation || !validation.formatValid || validation.viesStatus === 'NOT_CHECKED') return 'invalid'
  if (validation.viesStatus === 'VALID') return 'valid'
  if (validation.viesStatus === 'NOT_APPLICABLE') return 'structural'
  return 'warning'
}

function validationMessage(validation: VatValidationResult) {
  const isPortuguese = validation.countryCode === 'PT'
  if (!validation.formatValid || validation.viesStatus === 'NOT_CHECKED') return isPortuguese ? 'NIF com formato ou dígito de controlo inválido. O VIES não foi consultado.' : 'Número fiscal com formato inválido. O VIES não foi consultado.'
  if (validation.viesStatus === 'VALID') return 'Empresa confirmada no VIES para operações intracomunitárias.'
  if (validation.viesStatus === 'NOT_VALID') return 'Número fiscal com formato aceite, mas não confirmado no VIES. Isto não prova que o número ou a empresa não existem.'
  if (validation.viesStatus === 'NOT_APPLICABLE') return isPortuguese ? 'NIF com formato e dígito de controlo válidos. O VIES não é aplicável a este tipo de cliente.' : 'Número fiscal com formato aceite. O VIES não é aplicável a este tipo de cliente ou país.'
  return isPortuguese ? 'NIF com formato e dígito de controlo válidos. Não foi possível consultar o VIES neste momento.' : 'Número fiscal com formato aceite. Não foi possível consultar o VIES neste momento.'
}
