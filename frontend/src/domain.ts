export type Customer = {
  id: string; customerCode: string; abbreviation: string | null; shippingName: string; agency: 'LTFT01' | 'LTFT02'
  address: string | null; postalCode: string | null; locality: string | null; country: string | null
  contactEmail: string | null; mobile: string | null; phone: string | null
  billingCountry: string | null; vatNumber: string; billingLegalName: string | null; billingAddress: string | null
  billingPostalCode: string | null; billingLocality: string | null; accountCode: string | null; billingReference: string | null
  customerType: 'COMPANY' | 'PRIVATE'; responsibleName: string | null; billingEmail: string | null
  defaultDocument: string | null; exchangeRate: number | null; currency: string | null
  invoiceByPost: boolean; documentsByEmail: boolean; active: boolean
}

export type CustomerService = {
  id: string
  reference: string
  description: string
  serviceDate: string
  dueDate: string
  amount: number
  status: 'PENDING' | 'PAID'
  paidAt: string | null
}
