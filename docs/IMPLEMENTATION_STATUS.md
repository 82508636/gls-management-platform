# Estado consolidado da implementação

Atualizado em 8 de setembro de 2026.

## Operacional

- OIDC Authorization Code com PKCE, login, logout, renovação de token e revogação JML imediata.
- `identity-service`, `customer-service`, `pickup-service`, `workforce-service`, `catalog-service` e `pricing-service`
  isolados por processo e base PostgreSQL.
- Clientes, pesquisa, estados, código por agência, NIF único normalizado e criação sincronizada de Destinatários.
- Validação VIES de empresas e preenchimento auxiliar dos dados fiscais devolvidos.
- CRUD e validação de horários de Pontos Pickup.
- Perfis e categorias profissionais auditáveis; interface de Colaboradores ainda sem persistência do colaborador.
- Serviços operacionais, grupos e zonas de faturação no Catálogo.
- Tabelas, rotas independentes, escalões e simulação no serviço de Preços.

## Parcial ou deliberadamente adiado

- Envios continuam no backend legado da porta `8080`; a extração para `shipment-service` ainda não foi executada.
- A associação de Pontos Pickup a entregas e levantamentos ainda não está integrada no workflow de Envios.
- Colaboradores têm interface e catálogos, mas não persistência da ficha completa.
- Fornecedores permanecem em *stand by*.
- Não existe integração real com a API GLS; o fornecedor logístico continua isolado por uma interface mock.
- O API Gateway está operacional e o frontend já aponta para ele. O arranque integrado, as rotas autenticadas e o
  encaminhamento para os seis microserviços e para Envios no backend legado foram confirmados localmente.
- O API Gateway normaliza os seus erros de autenticação e autorização; os erros funcionais devolvidos pelos serviços
  ainda não partilham um contrato único.

## Estado P0, P1 e P2

### P0

- **Concluído — P0.1 Segurança e permissões:** helper JWT, conversor de roles, autorização de `HEAD` e matriz backend cobertos pelo reactor Maven verde.
- **Implementado, validação final pendente — P0.2 JML/Keycloak:** Joiner, Mover, Leaver, auditoria, revogação imediata,
  compensação de criação parcial, timeouts e persistência *fail closed* da decisão local estão implementados. Mover e
  Leaver bloqueiam primeiro o token antigo numa transação independente e só depois alteram o Keycloak; falta repetir o
  `clean test` Docker dos módulos afetados.
- **Concluído — P0.3 Estado inicial do cliente:** qualquer criação resulta num cliente inativo; só `ADMIN` altera o estado pelo endpoint dedicado.
- **Concluído — P0.4 Paginação:** conteúdo, metadados, pesquisa e filtro usam a página pedida ao backend.
- **Concluído — P0.5 Sessão OIDC:** recuperação silenciosa, deep-link, refresh partilhado e logout estão implementados sem persistir tokens em `localStorage`.

### P1

- **Em curso — P1.1 API Gateway**, dividido nas seguintes tarefas:
  - **P1.1.1 — Concluído:** módulo Spring Cloud Gateway 4.3.5, porta `8090`, health check e oito rotas explícitas para
    os seis serviços e o backend legado, com arranque Docker integrado confirmado.
  - **P1.1.2 — Implementado:** autenticação JWT no gateway e defesa em profundidade, mantendo validação JML e roles
    nos microserviços.
  - **P1.1.3 — Implementado:** CORS central, cabeçalhos seguros, limites de 2 MB/16 KB e timeouts configuráveis.
  - **P1.1.4 — Implementado:** `X-Correlation-ID` validado, gerado e propagado em pedidos e respostas.
  - **P1.1.5 — Implementado:** frontend migrado para o endpoint único `VITE_GATEWAY_API_URL`; build TypeScript verde.
  - **P1.1.6 — Parcial:** respostas `401/403`, falhas de ligação e timeouts do gateway seguem JSON comum com `traceId`;
    falta normalizar os erros funcionais devolvidos por todos os serviços.
  - **P1.1.7 — Implementado:** rate limiting por identidade autenticada, com token bucket partilhado em Redis e limites
    configuráveis; falta o teste E2E de saturação dos limites.
  - **P1.1.8 — Pendente por segurança:** cache apenas para dados de referência, depois de garantir validação JML nos
    cache hits e invalidação nas escritas.
  - **P1.1.9 — Parcial:** o E2E real pela porta `8090` valida Authorization Code + PKCE, erro anónimo `401` com
    `traceId`, dez endpoints autenticados que cobrem todas as rotas configuradas, `X-Correlation-ID` e `HEAD`.
    Faltam `403`, saturação do rate limiting, limites de payload/header, upstream indisponível, circuit breakers e
    integração deste cenário na CI.
- **Pendente — P1.2 Modal JML:** Cancelar, fechar e clicar no overlay ainda não limpam todos os dados, incluindo a password temporária.
- **Pendente — P1.3 Operações em curso:** faltam estados `submitting`/`pendingActionId` para impedir pedidos duplicados.
- **Parcial — P1.4 Conta e pesquisa:** pesquisa e filtros globais no backend estão concluídos; um deep-link da conta ainda depende do cliente já estar na página carregada.
- **Parcial — P1.5 Países:** o backend aceita países fora do VIES como `NOT_APPLICABLE`, mas o seletor do frontend ainda apresenta apenas o conjunto VIES.

O relatório PDF foi retirado do caminho crítico e movido para o backlog P3.

### P2 — automação

- **Concluído:** testes Java de todo o reactor executam em Maven 3.9.11/Java 21 e estão ligados à CI.
- **Parcial:** existe cenário Playwright de segurança/JML na CI, mas falta voltar a executá-lo sobre este estado consolidado.
- **Pendente:** testes de componentes frontend com Vitest/React Testing Library, paginação com mais de 50 clientes,
  integração PostgreSQL/Testcontainers, smoke VIES automatizado opt-in e E2E completo através do API Gateway.

Os seis microserviços publicados neste estado usam a versão de desenvolvimento `0.0.1-SNAPSHOT`. A numeração de release deve ser definida antes de produzir a primeira versão imutável.

## Validação de 3 de setembro de 2026

- Sete bases PostgreSQL acessíveis, incluindo a base do backend legado.
- Histórico Flyway sem migrations falhadas em qualquer base.
- Seis microserviços com `/actuator/health` em estado `UP`.
- Login real no Keycloak concluído e sessão OIDC recuperada pelo frontend.
- Chamadas autenticadas com perfil `ADMIN` a Customer, Pickup, Workforce, Identity, Catalog e Pricing devolveram `200`.
- A página de Clientes e a gestão JML carregaram dados reais depois da autenticação.
- Validação VIES e preenchimento dos dados fiscais confirmados no browser.
- Build Maven reproduzível em Java 21 através de `scripts/mvn-docker.cmd`, sem dependência de Maven instalado no Windows.
- `clean test` do reactor Maven confirmado com `BUILD SUCCESS` nos 11 módulos em 3 de setembro de 2026.

## Validação de 8 de setembro de 2026

- Backend legado, seis microserviços, API Gateway, Keycloak e frontend confirmados em execução.
- Corrigida a seleção do construtor de produção do `KeycloakAdminClient` no backend e no `identity-service`.
- O E2E do gateway passou com OIDC Authorization Code + PKCE, `401` comum com `traceId`, dez endpoints autenticados,
  propagação de `X-Correlation-ID` e autorização de `HEAD`.
- Uma primeira execução encontrou um `504` transitório em Catálogo; a rota passou isoladamente e na repetição integral.
  Este sinal deve orientar métricas e circuit breaker, sem ser considerado uma falha persistente.

## Organização antes de versionar

As alterações devem ser revistas e versionadas por blocos coerentes, sem misturar temporários de execução:

1. infraestrutura e bibliotecas comuns;
2. extração dos microserviços e migrations;
3. segurança OIDC/JML e autorização;
4. Clientes, Destinatários e VIES;
5. Pickup e Workforce;
6. Catálogo e zonas de faturação;
7. Preços e integração transitória com Envios;
8. frontend e documentação.

Diretórios de execução como `work/runtime`, dependências explodidas e artefactos `target` não devem entrar nos commits.
