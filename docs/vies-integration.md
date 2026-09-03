# Integração VIES

## Objetivo

Validar, através do serviço oficial VIES da Comissão Europeia, se um número de IVA de uma empresa está registado para
operações intracomunitárias e, quando disponíveis, usar os dados devolvidos para auxiliar o preenchimento da faturação.

## Decisões

- O frontend comunica apenas com o backend da LTFT.
- O backend isola o contrato externo no módulo `integration.vies`.
- Não é necessária API key.
- Antes do pedido externo, números portugueses são validados localmente quanto ao formato e dígito de controlo.
- Para particulares, o VIES não é consultado. Em Portugal é validado o formato e dígito de controlo; noutros países é verificada apenas a sintaxe genérica.
- Um resultado negativo do VIES significa `não confirmado no VIES`; não prova que o NIF ou a empresa não existem.
- Indisponibilidade, timeout ou resposta inesperada do VIES não bloqueiam a criação ou edição do cliente.
- Numa resposta `VALID`, nome e morada devolvidos são expostos ao frontend como dados de registo e podem preencher a
  designação social, morada fiscal, código postal e localidade.
- O preenchimento é auxiliar: ignora valores vazios ou `---`, não bloqueia a edição manual e não transforma o VIES numa
  fonte autoritativa de moradas.
- Nesta fase não é guardado histórico das consultas. A retenção de NIF, resultado e ator exige uma decisão de privacidade e prazo de conservação.

## Contrato interno

`POST /api/vat-validations`

Pedido:

```json
{
  "countryCode": "PT",
  "vatNumber": "509321765",
  "subjectType": "COMPANY"
}
```

Resposta:

```json
{
  "countryCode": "PT",
  "vatNumber": "509321765",
  "formatValid": true,
  "viesStatus": "VALID",
  "registeredName": "EMPRESA EXEMPLO, S.A.",
  "registeredAddress": "RUA EXEMPLO 1",
  "registeredPostalCode": "1000-001",
  "registeredLocality": "LISBOA",
  "checkedAt": "2026-08-14T10:00:00Z"
}
```

Os quatro campos `registered*` podem ser nulos quando o Estado-Membro não os devolve. Para Portugal, o adaptador aceita
tanto os campos estruturados do VIES como a morada textual em várias linhas ou em linha única, separando código postal e
localidade quando o formato o permite.

Estados VIES:

- `VALID`: confirmado no VIES.
- `NOT_VALID`: não confirmado no VIES; pode existir sem ativação intracomunitária.
- `NOT_APPLICABLE`: particular ou país fora do âmbito do VIES.
- `NOT_CHECKED`: formato inválido; o VIES não foi consultado.
- `UNAVAILABLE`: timeout, indisponibilidade ou resposta externa inválida.

Quando `formatValid` é `false`, não é efetuado pedido externo.

## Segurança

O endpoint requer `ADMIN` ou `OPERATOR`, os mesmos perfis autorizados a criar ou editar clientes. `ACCOUNTING` e `CUSTOMER` não podem iniciar validações.

## Configuração

- `VIES_BASE_URL`, por omissão `https://ec.europa.eu/taxation_customs/vies/rest-api`
- `VIES_CONNECT_TIMEOUT`, por omissão `3s`
- `VIES_READ_TIMEOUT`, por omissão `5s`
- `VIES_MAX_CONCURRENT_REQUESTS`, por omissão `4`
- `VIES_MAX_ATTEMPTS`, por omissão `3`
- `VIES_RETRY_DELAY`, por omissão `250ms`

O limite de concorrência funciona como um *bulkhead*: quando os pedidos externos em curso atingem o limite, a validação devolve temporariamente `UNAVAILABLE` sem ocupar mais ligações nem colocar NIF em filas. Não existe cache de NIF nesta fase.

## Limitações

- O VIES é um motor de pesquisa sobre bases nacionais de IVA, não um registo geral de NIF portugueses.
- A disponibilidade dos serviços nacionais varia.
- A validação estrutural portuguesa não confirma que um NIF tenha sido atribuído.
- Uma eventual exigência de validação obrigatória antes da aprovação de clientes permanece uma decisão funcional futura.
- Os dados preenchidos automaticamente devem ser confirmados pelo utilizador antes da gravação do cliente.

## Referências oficiais

- Informação técnica: <https://ec.europa.eu/taxation_customs/vies/#/technical-information>
- Contrato REST: <https://ec.europa.eu/assets/taxud/vow-information/swagger_publicVAT.yaml>
