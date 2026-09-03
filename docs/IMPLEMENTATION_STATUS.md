# Estado consolidado da implementação

Atualizado em 3 de setembro de 2026.

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
- Não existe ainda API Gateway nem contrato de erro único entre todos os serviços.

## Estado P0, P1 e P2

### P0

- **Concluído — P0.1 Segurança e permissões:** helper JWT, conversor de roles, autorização de `HEAD` e matriz backend cobertos pelo reactor Maven verde.
- **Parcial — P0.2 JML/Keycloak:** Joiner, Mover, Leaver, auditoria e revogação imediata estão implementados; falta compensar a criação parcial no Keycloak quando a password ou atribuição de role falha e definir timeouts explícitos no cliente administrativo.
- **Concluído — P0.3 Estado inicial do cliente:** qualquer criação resulta num cliente inativo; só `ADMIN` altera o estado pelo endpoint dedicado.
- **Concluído — P0.4 Paginação:** conteúdo, metadados, pesquisa e filtro usam a página pedida ao backend.
- **Concluído — P0.5 Sessão OIDC:** recuperação silenciosa, deep-link, refresh partilhado e logout estão implementados sem persistir tokens em `localStorage`.

### P1

- **Pendente — P1.1 PDF:** ainda recebe a coleção completa, não os registos filtrados; falta também validar os acentos portugueses.
- **Pendente — P1.2 Modal JML:** Cancelar, fechar e clicar no overlay ainda não limpam todos os dados, incluindo a password temporária.
- **Pendente — P1.3 Operações em curso:** faltam estados `submitting`/`pendingActionId` para impedir pedidos duplicados.
- **Parcial — P1.4 Conta e pesquisa:** pesquisa e filtros globais no backend estão concluídos; um deep-link da conta ainda depende do cliente já estar na página carregada.
- **Parcial — P1.5 Países:** o backend aceita países fora do VIES como `NOT_APPLICABLE`, mas o seletor do frontend ainda apresenta apenas o conjunto VIES.

### P2 — automação

- **Concluído:** testes Java de todo o reactor executam em Maven 3.9.11/Java 21 e estão ligados à CI.
- **Parcial:** existe cenário Playwright de segurança/JML na CI, mas falta voltar a executá-lo sobre este estado consolidado.
- **Pendente:** testes de componentes frontend com Vitest/React Testing Library, paginação com mais de 50 clientes, integração PostgreSQL/Testcontainers e smoke VIES automatizado opt-in.

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
