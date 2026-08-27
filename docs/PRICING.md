# Tabelas de preços

## Âmbito da primeira versão

Esta versão cobre apenas os serviços **Business Parcel** e **Express Parcel** da tabela 4 Winners. Os restantes serviços do documento de origem ficam fora do âmbito até validação funcional desta base.

A solução separa a configuração comercial do futuro processo de expedição. Ainda não existe ligação à API da GLS e a simulação não cria envios nem documentos de faturação.

## Modelo

Uma tabela de preços (`pricing_plans`) contém:

- código, designação e versão;
- período de validade e moeda;
- percentagens de combustível e IVA;
- estado `DRAFT`, `ACTIVE` ou `ARCHIVED`;
- informação de auditoria de criação e alteração.

Cada tabela contém rotas (`pricing_routes`) de Business Parcel ou Express Parcel. A rota define o destino, prazo, fator volumétrico, peso máximo por volume e preço por quilograma adicional. Os escalões (`pricing_brackets`) associam um peso máximo ao respetivo preço base.

## Cálculo da simulação

1. Calcula-se o peso volumétrico, quando são indicadas as três dimensões: `comprimento × largura × altura × fator / 1 000 000 × volumes`.
2. O peso taxável é o maior entre o peso real e o peso volumétrico.
3. É selecionado o primeiro escalão cujo limite cubra o peso taxável.
4. Acima do último escalão, são somados os passos de quilograma adicional, arredondados para cima.
5. Ao preço base é acrescentada a taxa de combustível.
6. O IVA é aplicado ao subtotal.

O simulador apresenta separadamente peso real, volumétrico e taxável, preço base, combustível, IVA e total.

## Regras de edição e segurança

- `ADMIN` pode criar, alterar e ativar tabelas e rotas.
- `ADMIN` e `ACCOUNTING` podem consultar tabelas e usar o simulador.
- Uma tabela ativa deixa de poder ser editada.
- A ativação exige pelo menos uma rota válida e ativa de cada um dos dois serviços.
- A rota exige escalões crescentes, valores não negativos, peso máximo positivo e configuração válida do quilograma adicional.
- Os erros devolvidos ao frontend são genéricos; o detalhe técnico fica apenas nos registos do backend.

## Endpoints

- `GET /api/pricing/plans`
- `GET /api/pricing/plans/{id}`
- `POST /api/pricing/plans`
- `PUT /api/pricing/plans/{id}`
- `POST /api/pricing/plans/{id}/routes`
- `PUT /api/pricing/plans/{planId}/routes/{routeId}`
- `PATCH /api/pricing/plans/{id}/status`
- `POST /api/pricing/simulations`

## Dados iniciais

A migração `V12__create_business_and_express_pricing.sql` cria uma tabela em rascunho com as rotas, escalões, fatores volumétricos e valores de Business Parcel e Express Parcel transcritos do documento 4 Winners. A tabela começa em `DRAFT` para exigir revisão humana antes da ativação.

