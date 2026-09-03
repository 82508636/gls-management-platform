# Catálogo de serviços operacionais

## Objetivo

O catálogo descreve as capacidades e regras operacionais de um serviço de transporte. É administrado em `/configuracao/servicos` e não cria uma relação pai–filho com as rotas de preços.

As rotas continuam a pertencer exclusivamente às tabelas de preços e a ser editadas de forma autónoma. As opções de cálculo guardadas num serviço são instruções para um futuro motor de cálculo, não referências a rotas.

## Modelo

- `service_groups`: catálogo administrável de grupos, com ID, designação, estado e auditoria.
- `billing_zones`: zonas pesquisáveis com código único, designação, país, grupo e padrões de código postal.
- `operational_services`: identidade, transporte, trânsito, cálculo, horários, capacidades, definições, estado e auditoria.
- `operational_service_zones`: zonas permitidas e prazos mínimo/máximo próprios.
- `operational_service_package_limits`: limites para caixa, documento e palete.
- coleções de dias de recolha/entrega, clientes exclusivos e agências bloqueadas.

As associações de recolha ligam apenas serviços operacionais entre si. Não aceitam autorreferências e apenas podem apontar para serviços ativos.

## Regras de integridade

- código de serviço, código de zona e ID de grupo são normalizados em maiúsculas e são únicos;
- urgência limitada a `1..5`;
- mínimos nunca podem exceder os máximos;
- a hora final de recolha tem de ser posterior à inicial e ambas são indicadas em conjunto;
- `Sem recolha` não pode coexistir com horário, dias ou serviços de recolha associados;
- uma zona ou tipo de volume não pode ser repetido no mesmo serviço;
- zonas selecionadas têm de existir e estar ativas;
- clientes exclusivos têm de existir;
- as únicas agências reconhecidas são `LTFT01` e `LTFT02`;
- valores físicos e pesos máximos têm de ser positivos.

As edições reutilizam os registos de zona e volume já associados sempre que a chave funcional é a mesma. Isto evita conflitos de unicidade e conserva uma atualização transacional.

## Segurança

- `ADMIN`, `OPERATOR`, `ACCOUNTING` e `FRONT_DESK` podem consultar serviços, grupos e zonas;
- apenas `ADMIN` pode criar, alterar, ativar ou inativar estes catálogos;
- `HEAD` possui a mesma autorização de leitura de `GET`;
- respostas de erro apresentadas ao frontend são genéricas.

O catálogo corre no `catalog-service` (`http://localhost:8085`) e possui a base PostgreSQL `catalog`. Para validar
clientes exclusivos, envia o JWT do administrador para `POST /internal/v1/customers/existence` no
`customer-service`. A tabela do catálogo conserva apenas os UUIDs: não há chave estrangeira nem acesso direto à base
`customer`. Uma falha desta integração devolve `503` e impede a gravação.

## Endpoints

### Serviços

- `GET /api/operational-services`
- `GET /api/operational-services/{id}`
- `POST /api/operational-services`
- `PUT /api/operational-services/{id}`
- `PATCH /api/operational-services/{id}/status`

### Grupos

- `GET /api/service-groups`
- `POST /api/service-groups`
- `PUT /api/service-groups/{id}`
- `PATCH /api/service-groups/{id}/status`

### Zonas de faturação

- `GET /api/billing/zones`
- `POST /api/billing/zones`
- `PUT /api/billing/zones/{id}`
- `PATCH /api/billing/zones/{id}/status`

Os três endpoints de listagem aceitam `active=true|false` para filtragem por estado.

## Interface

- `/configuracao/servicos`: lista compacta, pesquisa local e filtro por estado;
- `/configuracao/servicos/create`: formulário integral dos dez blocos;
- `/configuracao/servicos/{id}/edit`: consulta e edição;
- `/configuracao/grupos-servicos`: manutenção de grupos;
- `/billing/zones`: manutenção e pesquisa de zonas de faturação.

As transportadoras apresentadas no seletor são, nesta fase, opções controladas da interface. Quando existir um catálogo próprio de transportadoras, o campo deverá passar a referenciar essa entidade sem alterar a independência das rotas.

## Migração e testes

A migração própria `services/catalog-service/.../V1__create_catalog_domain.sql` cria todas as tabelas, restrições e
índices na base `catalog`. Os testes do domínio cobrem criação válida, horários incoerentes, duplicação de zonas/tipos
de volume, zonas inativas, clientes inexistentes e indisponibilidade do serviço de Clientes. Os testes HTTP cobrem
leitura, `HEAD`, anonimato e escrita exclusiva de `ADMIN`. A bateria real confirma ainda duplicados `409`,
configurações inválidas `422`, CORS e revogação imediata do mesmo token (`200 → 401 → 200`).
