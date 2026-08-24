import type { Customer, CustomerService } from './domain'
import ltftLogoDataUrl from './assets/ltft-logo.jpg?inline'

const PAGE_WIDTH = 842
const PAGE_HEIGHT = 595
const ROWS_PER_PAGE = 13
const LTFT_LOGO = dataUrlBytes(ltftLogoDataUrl)
const LTFT_LOGO_WIDTH = 1004
const LTFT_LOGO_HEIGHT = 251

export function downloadPendingServicesPdf(customer: Customer, services: CustomerService[]) {
  const pendingServices = services.filter(service => service.status === 'PENDING')
  if (pendingServices.length === 0) return
  const pdf = createPendingServicesPdf(customer, pendingServices)
  const blob = new Blob([pdf], { type: 'application/pdf' })
  const url = URL.createObjectURL(blob)
  const anchor = document.createElement('a')
  anchor.href = url
  anchor.download = `resumo-servicos-${safeFileName(customer.id)}.pdf`
  document.body.appendChild(anchor)
  anchor.click()
  anchor.remove()
  window.setTimeout(() => URL.revokeObjectURL(url), 1000)
}

export function createPendingServicesPdf(customer: Customer, services: CustomerService[]) {
  const pendingServices = services.filter(service => service.status === 'PENDING')
  const pages = chunk(pendingServices, ROWS_PER_PAGE)
  const streams = pages.map((pageServices, index) => buildPage(customer, pageServices, index + 1, pages.length, pendingServices))
  return buildPdf(streams)
}

function buildPage(customer: Customer, services: CustomerService[], page: number, totalPages: number, allServices: CustomerService[]) {
  const minDate = allServices.map(service => service.serviceDate).sort()[0]
  const maxDate = allServices.map(service => service.serviceDate).sort().at(-1)!
  const total = allServices.reduce((sum, service) => sum + service.amount, 0)
  const commands: string[] = []
  const text = (x: number, y: number, size: number, value: string, bold = false) => {
    commands.push(`BT /${bold ? 'F2' : 'F1'} ${size} Tf ${x} ${y} Td (${pdfText(value)}) Tj ET`)
  }
  const line = (x1: number, y1: number, x2: number, y2: number, width = 0.5) => commands.push(`${width} w ${x1} ${y1} m ${x2} ${y2} l S`)
  const fillRect = (x: number, y: number, width: number, height: number, gray: number) => commands.push(`${gray} g ${x} ${y} ${width} ${height} re f 0 g`)

  // Cabecalho inspirado no resumo LTFT de referencia: fundo branco, faixa azul e logotipo oficial.
  commands.push('0.17 0.31 0.45 rg 32 574 778 7 re f 0 g')
  commands.push('q 208 0 0 52 36 516 cm /Logo Do Q')
  text(462, 548, 17, `RESUMO DE SERVICOS - ${monthYear(maxDate)}`, true)
  text(624, 530, 10, `${customer.customerCode} - ${truncate(customer.shippingName, 27).toUpperCase()}`, true)
  text(610, 514, 10, `PERIODO DE ${formatDate(minDate)} A ${formatDate(maxDate)}`)
  line(32, 502, 810, 502, 0.7)

  text(36, 486, 9, 'CLIENTE', true)
  text(36, 470, 12, truncate(customer.shippingName, 60), true)
  text(36, 454, 9, `NIF ${customer.vatNumber}  |  Referencia ${customer.billingReference ?? customer.accountCode ?? '-'}`)
  text(704, 470, 8, `Pagina ${page} de ${totalPages}`)

  const tableTop = 424
  fillRect(32, tableTop, 778, 22, 0.88)
  text(40, tableTop + 7, 8, 'REFERENCIA', true)
  text(190, tableTop + 7, 8, 'DATA', true)
  text(270, tableTop + 7, 8, 'SERVICO', true)
  text(620, tableTop + 7, 8, 'VENCIMENTO', true)
  text(735, tableTop + 7, 8, 'VALOR', true)
  line(32, tableTop, 810, tableTop, 0.8)

  services.forEach((_, index) => {
    const y = tableTop - 29 - index * 23
    if (index % 2 === 1) fillRect(32, y - 6, 778, 23, 0.96)
  })

  services.forEach((service, index) => {
    const y = tableTop - 29 - index * 23
    text(40, y, 8, service.reference, true)
    text(190, y, 8, formatDate(service.serviceDate))
    text(270, y, 8, truncate(service.description, 52))
    text(620, y, 8, formatDate(service.dueDate))
    text(742, y, 8, formatCurrency(service.amount), true)
    line(32, y - 7, 810, y - 7, 0.25)
  })

  if (page === totalPages) {
    // O topo do total fica pelo menos 12 pontos abaixo da linha inferior da ultima linha.
    const lastRowLineY = tableTop - 36 - (services.length - 1) * 23
    const totalY = Math.max(62, lastRowLineY - 47)
    fillRect(600, totalY, 210, 35, 0.91)
    text(614, totalY + 21, 9, 'TOTAL POR PAGAR', true)
    text(735, totalY + 19, 12, formatCurrency(total), true)
  }
  text(36, 24, 7, 'Documento gerado com dados simulados. Nao constitui fatura nem documento fiscal.')
  text(748, 24, 7, `LTFT | ${page}/${totalPages}`)
  return commands.join('\n')
}

function buildPdf(streams: string[]) {
  const objects: Array<string | Uint8Array> = []
  objects[1] = '<< /Type /Catalog /Pages 2 0 R >>'
  const pageIds = streams.map((_, index) => 4 + index * 2)
  objects[2] = `<< /Type /Pages /Kids [${pageIds.map(id => `${id} 0 R`).join(' ')}] /Count ${streams.length} >>`
  objects[3] = '<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>'
  const boldFontId = 4 + streams.length * 2
  const logoId = boldFontId + 1
  streams.forEach((stream, index) => {
    const pageId = 4 + index * 2
    const contentId = pageId + 1
    objects[pageId] = `<< /Type /Page /Parent 2 0 R /MediaBox [0 0 ${PAGE_WIDTH} ${PAGE_HEIGHT}] /Resources << /Font << /F1 3 0 R /F2 ${boldFontId} 0 R >> /XObject << /Logo ${logoId} 0 R >> >> /Contents ${contentId} 0 R >>`
    objects[contentId] = `<< /Length ${latin1Bytes(stream).length} >>\nstream\n${stream}\nendstream`
  })
  objects[boldFontId] = '<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold >>'
  objects[logoId] = binaryPdfObject(
    `<< /Type /XObject /Subtype /Image /Width ${LTFT_LOGO_WIDTH} /Height ${LTFT_LOGO_HEIGHT} /ColorSpace /DeviceRGB /BitsPerComponent 8 /Filter /DCTDecode /Length ${LTFT_LOGO.length} >>\nstream\n`,
    LTFT_LOGO,
    '\nendstream',
  )

  const parts: Uint8Array[] = [latin1Bytes('%PDF-1.4\n%LTFT\n')]
  const offsets: number[] = [0]
  let byteOffset = parts[0].length
  for (let id = 1; id < objects.length; id++) {
    const object = objects[id]
    const body = typeof object === 'string' ? latin1Bytes(object) : object
    const part = concatBytes(latin1Bytes(`${id} 0 obj\n`), body, latin1Bytes('\nendobj\n'))
    offsets[id] = byteOffset
    parts.push(part)
    byteOffset += part.length
  }
  const xrefOffset = byteOffset
  const xref = [`xref`, `0 ${objects.length}`, '0000000000 65535 f ']
  for (let id = 1; id < objects.length; id++) xref.push(`${String(offsets[id]).padStart(10, '0')} 00000 n `)
  xref.push(`trailer\n<< /Size ${objects.length} /Root 1 0 R >>\nstartxref\n${xrefOffset}\n%%EOF`)
  parts.push(latin1Bytes(xref.join('\n')))
  const size = parts.reduce((sum, part) => sum + part.length, 0)
  const output = new Uint8Array(size)
  let cursor = 0
  parts.forEach(part => { output.set(part, cursor); cursor += part.length })
  return output
}

function latin1Bytes(value: string) { return Uint8Array.from(value, character => character.charCodeAt(0) & 0xff) }
function dataUrlBytes(value: string) {
  const encoded = value.slice(value.indexOf(',') + 1)
  return Uint8Array.from(atob(encoded), character => character.charCodeAt(0))
}
function binaryPdfObject(prefix: string, bytes: Uint8Array, suffix: string) {
  return concatBytes(latin1Bytes(prefix), bytes, latin1Bytes(suffix))
}
function concatBytes(...parts: Uint8Array[]) {
  const output = new Uint8Array(parts.reduce((sum, part) => sum + part.length, 0))
  let cursor = 0
  parts.forEach(part => { output.set(part, cursor); cursor += part.length })
  return output
}
function pdfText(value: string) { return value.replace(/[^\x20-\xFF]/g, '-').replace(/([\\()])/g, '\\$1') }
function safeFileName(value: string) { return value.toLowerCase().replace(/[^a-z0-9-]/g, '-') }
function truncate(value: string, length: number) { return value.length > length ? `${value.slice(0, length - 3)}...` : value }
function chunk<T>(items: T[], size: number) { return Array.from({ length: Math.ceil(items.length / size) }, (_, index) => items.slice(index * size, (index + 1) * size)) }
function formatDate(value: string) { const [year, month, day] = value.split('-'); return `${day}/${month}/${year}` }
function formatCurrency(value: number) { return `${value.toFixed(2).replace('.', ',')} EUR` }
function monthYear(value: string) {
  const [year, month] = value.split('-')
  const months = ['JANEIRO', 'FEVEREIRO', 'MARCO', 'ABRIL', 'MAIO', 'JUNHO', 'JULHO', 'AGOSTO', 'SETEMBRO', 'OUTUBRO', 'NOVEMBRO', 'DEZEMBRO']
  return `${months[Number(month) - 1]} ${year}`
}
