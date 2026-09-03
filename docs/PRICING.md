# Tabelas de preços

## Âmbito

As rotas comerciais são configurações autónomas. Não existe uma entidade ou categoria superior de serviço como Business Parcel ou Express Parcel. Cada rota é criada e identificada diretamente pela sua designação e código.

A solução separa a configuração comercial do futuro processo de expedição. Ainda não existe ligação à API da GLS e a simulação não cria envios nem documentos de faturação.

O domínio corre autonomamente no `pricing-service`, porta `8086`, e é o único proprietário da base PostgreSQL
`pricing`. O frontend usa `VITE_PRICING_API_URL` e o backend de Envios usa `PRICING_SERVICE_URL`; nenhum consumidor
consulta diretamente as tabelas desta base.

## Modelo

Uma tabela de preços (`pricing_plans`) contém:

- código, designação e versão;
- período de validade e moeda;
- percentagens de combustível e IVA;
- estado `DRAFT`, `ACTIVE` ou `ARCHIVED`;
- informação de auditoria de criação e alteração.

Cada tabela contém rotas autónomas (`pricing_routes`). A rota define o destino, prazo, fator volumétrico, peso máximo por volume e preço por quilograma adicional. Os escalões (`pricing_brackets`) associam um peso máximo ao respetivo preço base.

## Cálculo da simulação

1. Calcula-se o peso volumétrico, quando são indicadas as três dimensões: `comprimento × largura × altura × fator / 1 000 000 × volumes`.
2. O peso taxável é o maior entre o peso real e o peso volumétrico.
3. É selecionado o primeiro escalão cujo limite cubra o peso taxável.
4. Acima do último escalão, são somados os passos de quilograma adicional, arredondados para cima.
5. Ao preço base é acrescentada a taxa de combustível.
6. O IVA é aplicado ao subtotal.

O simulador apresenta separadamente peso real, volumétrico e taxável, preço base, combustível, IVA e total.

## Regras de edição e segurança

- `ADMIN` pode criar, alterar, eliminar e ativar tabelas e rotas.
- `ADMIN` e `ACCOUNTING` podem consultar tabelas e usar o simulador.
- Uma tabela ativa deixa de poder ser editada.
- A ativação exige pelo menos uma rota válida e ativa.
- A rota exige escalões crescentes, valores não negativos, peso máximo positivo e configuração válida do quilograma adicional.
- Os erros devolvidos ao frontend são genéricos; o detalhe técnico fica apenas nos registos do backend.

## Endpoints

- `GET /api/pricing/plans`
- `GET /api/pricing/plans/{id}`
- `POST /api/pricing/plans`
- `PUT /api/pricing/plans/{id}`
- `POST /api/pricing/plans/{id}/routes`
- `PUT /api/pricing/plans/{planId}/routes/{routeId}`
- `DELETE /api/pricing/plans/{planId}/routes/{routeId}`
- `PATCH /api/pricing/plans/{id}/status`
- `POST /api/pricing/simulations`
- `POST /internal/v1/pricing/quotes`

O último endpoint é o contrato autenticado usado por Envios. Recebe a tabela, a rota, o peso, os volumes e dimensões
e devolve a decomposição completa da cotação. O JWT do utilizador é propagado, pelo que a revogação imediata e as roles
continuam a ser verificadas pelo serviço de Identidade.

## Integração com envios

Ao criar um envio, o backend pede a cotação ao `pricing-service` antes de persistir. Uma indisponibilidade, falha de
autenticação entre serviços ou resposta vazia devolve `503` sem criar um envio parcialmente calculado. Dados inválidos
para a tabela ou rota devolvem o erro funcional de validação.

O envio guarda os UUIDs da tabela e da rota e um snapshot com código, versão, designação, pesos, tarifa base,
combustível, subtotal, IVA, total e moeda. Alterações posteriores à tabela não alteram o histórico comercial do envio.

## Migração e corte

`tools/pricing-data-migrator` copia tabelas, rotas e escalões do backend legado numa única transação. O utilitário:

- bloqueia a execução concorrente no destino;
- recusa uma base de destino que já contenha dados;
- preserva todos os UUIDs e campos de auditoria;
- compara contagens e conjuntos de IDs antes do `commit`;
- executa `rollback` integral se alguma verificação falhar.

Na migração local foram reconciliadas 1 tabela, 16 rotas e 56 escalões. As tabelas legadas permanecem temporariamente
para preservar as chaves e os envios existentes durante a transição; o frontend e as novas cotações já usam a porta
`8086`. A remoção física só deve ocorrer depois da extração de Envios e de uma janela de observação sem divergências.

## Configuração inicial

A migração `V15__flatten_and_reset_pricing_routes.sql` remove a classificação por serviço e limpa as rotas inicialmente importadas. A tabela fica em `DRAFT`, sem rotas, para que a configuração seja introduzida manualmente. Os envios históricos conservam os respetivos snapshots de rota e preço.
