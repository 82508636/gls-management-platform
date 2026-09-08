# LTFT Comand Center

Plataforma web de gestão comercial e financeira de envios GLS. A GLS continua a ser o *source of truth* logístico; esta aplicação mantém a visão comercial e financeira.

> A migração incremental para microserviços está documentada em
> [`docs/MICROSERVICES.md`](docs/MICROSERVICES.md). Pickup, Clientes, Workforce,
> Identidade, Catálogo e Preços já são serviços autónomos, cada um com a sua base de dados. O acesso do frontend
> converge no API Gateway documentado em [`docs/API_GATEWAY.md`](docs/API_GATEWAY.md).

## Estrutura

- `backend/`: backend legado Java 21 + Spring Boot, mantido durante a migração
- `services/`: microserviços extraídos, cada um com processo e base próprios
- `services/api-gateway`: entrada HTTP única, autenticação inicial e políticas transversais da API
- `tools/customer-data-migrator`: migração transacional e não destrutiva de Clientes/Destinatários para a base `customer`
- `tools/pricing-data-migrator`: migração transacional e não destrutiva de tabelas, rotas e escalões para a base `pricing`
- `frontend/`: React + TypeScript + Vite
- `compose.yaml`: PostgreSQL para desenvolvimento local

## Pré-requisitos

- Java 21
- Maven 3.9+
- Node.js 22+
- Docker com Docker Compose

## Arranque local

1. Copiar `.env.example` para `.env` se for necessário alterar a configuração.
2. Iniciar as bases, Keycloak e serviços extraídos: `docker compose up --detach`.
3. Iniciar o backend legado para os domínios ainda não extraídos: `cd backend && mvn spring-boot:run`.
4. Iniciar o frontend: `cd frontend && npm install && npm run dev`.

O backend legado fica disponível em `http://localhost:8080`; os serviços extraídos usam as portas `8081` a `8086`,
respetivamente Pickup, Clientes, Colaboradores, Identidade, Catálogo e Preços. O API Gateway fica na porta `8090`, o
frontend em `http://localhost:5173` e o Keycloak em `http://localhost:8180`. Cada serviço expõe o seu próprio
`/actuator/health`. O frontend usa exclusivamente `VITE_GATEWAY_API_URL`, cujo valor local é
`http://localhost:8090/api`.

O Compose aguarda pelos `healthchecks` do PostgreSQL, Keycloak e serviços dos quais outros componentes dependem.
Um serviço Spring só fica saudável depois de aplicar as migrations Flyway e validar o esquema JPA. Para confirmar o
ambiente, verifique `http://localhost:8081/actuator/health` até `http://localhost:8086/actuator/health` e
`http://localhost:8090/actuator/health`; todos devem responder `{"status":"UP"}`. O backend legado da porta `8080` é iniciado separadamente e continua necessário apenas
para os domínios ainda não extraídos, nomeadamente Envios.

O gateway valida a presença e assinatura do JWT antes do encaminhamento, limita pedidos e cabeçalhos, aplica timeouts,
CORS e cabeçalhos seguros, e propaga um `X-Correlation-ID`. Os microserviços continuam a validar o token, a revogação
JML e as roles: o gateway não substitui a autorização do dono de cada domínio. O rate limiting usa Redis e uma chave
por identidade autenticada. A cache continua desativada até existir uma política que não contorne a revogação imediata.

O realm local `ltft` é importado automaticamente com o cliente público `ltft-web` e os perfis `ADMIN`, `OPERATOR`, `ACCOUNTING`, `CUSTOMER`, `DRIVER` e `FRONT_DESK`. As credenciais administrativas locais vêm de `.env`; os valores de exemplo não devem ser usados em produção.

O frontend autentica através de OIDC Authorization Code com PKCE. Ao abrir `http://localhost:5173`, a aplicação tenta recuperar silenciosamente uma sessão SSO existente (`prompt=none`); sem sessão, apresenta o botão `Login` sem entrar num ciclo de redirects. A password é introduzida exclusivamente no Keycloak. O cabeçalho apresenta o utilizador autenticado e permite terminar a sessão através do endpoint OIDC com `id_token_hint`. Access, refresh e ID tokens são mantidos apenas em memória; não são guardados em `localStorage`. O refresh token é renovado através de uma única operação partilhada quando existem pedidos concorrentes.

Se o volume local do Keycloak já tiver sido criado antes de uma alteração ao ficheiro do realm, a importação não substitui automaticamente o realm existente. Durante desenvolvimento, ajuste o cliente `ltft-web` na consola ou recrie deliberadamente o volume do Keycloak depois de confirmar que não contém utilizadores necessários.

## Dados locais de clientes

O frontend não contém clientes ou serviços mock. Para preencher a base local com 200 clientes fake reproduzíveis, execute a partir da raiz do repositório:

```powershell
Get-Content -LiteralPath .\scripts\seed-local-fake-customers.sql -Raw |
  docker compose exec -T postgres psql -U gls -d gls_management
```

O seed cria 100 clientes por agência, com códigos sequenciais, NIFs numéricos com checksum português válido e uma combinação de estados ativos/inativos. Reexecuções substituem exclusivamente os registos identificados por `FAKE-SEED-*`; clientes reais são preservados. Este script é apenas para ambientes locais e não faz parte das migrations Flyway de produção.

Para criar novamente os 200 clientes e acrescentar envios locais reproduzíveis para vários destinos nacionais e espanhóis, execute:

```powershell
.\scripts\seed-local-demo-shipments.ps1
```

O script cria envios apenas quando já existem rotas ativas compatíveis com os países dos destinatários. Reexecuções substituem apenas os clientes e envios marcados como dados locais.

### Gestão JML

Administradores acedem a `/admin/utilizadores` através da opção `Gerir utilizadores` no menu do cabeçalho. A página permite criar utilizadores com password temporária, substituir o perfil da plataforma e desativar contas com revogação de sessões.

O backend usa o cliente confidencial `ltft-jml-service`. Num realm já existente, crie esse cliente com `Service accounts roles` ativo, configure o segredo definido em `KEYCLOAK_JML_CLIENT_SECRET` e atribua à conta técnica as client roles `manage-users`, `view-users`, `query-users` e `view-realm` do cliente `realm-management`. Não exponha esse segredo no frontend. As chamadas administrativas usam timeouts configuráveis por `KEYCLOAK_CONNECT_TIMEOUT` e `KEYCLOAK_READ_TIMEOUT`. Se um Joiner falhar depois de criar a identidade, a operação elimina compensatoriamente o utilizador parcial e tenta conservar uma decisão local desativada. Mover e Leaver persistem primeiro a nova decisão local numa transação independente: uma falha posterior no Keycloak mantém o acesso fechado e exige repetição administrativa, em vez de voltar a aceitar o token antigo. Falhas de compensação ficam associadas à exceção original para diagnóstico interno sem serem expostas ao frontend.

O primeiro módulo funcional permite criar, listar e editar clientes em `http://localhost:5173/clientes`. A API paginada está disponível em `/api/customers`, com um máximo de 50 clientes por pedido. A pesquisa é executada no backend através de `query` e pode ser combinada com o filtro booleano `active`; código, designação, NIF, contactos, localidade e agência são pesquisáveis.

A ficha de cliente separa dados gerais/contactos dos dados de faturação. São obrigatórios a designação para expedição, a agência (`LTFT01` Fafe ou `LTFT02` Taipas) e o NIF. O código interno de seis dígitos fica bloqueado no formulário e é atribuído apenas quando a criação é aprovada: começa por `1` em Fafe e por `2` nas Taipas, seguindo um contador transacional independente por agência. Código e agência tornam-se imutáveis após a criação. Cód. Conta, Ref. Faturação e câmbio permanecem opcionais e em avaliação; não existe integração com o Enovo.

### Validação de NIF e VIES

O botão `Validar` na ficha do cliente chama o endpoint protegido `POST /api/vat-validations`. Para NIF portugueses, o backend valida primeiro formato e dígito de controlo. Empresas de países abrangidos são depois consultadas no serviço REST oficial VIES. Para particulares, o VIES não é consultado; fora de Portugal só é verificada a sintaxe genérica do número fiscal.

O resultado é informativo: `não confirmado no VIES` não significa que o NIF ou a empresa não existem. Quando a resposta
é `VALID` e o fornecedor disponibiliza os dados, o frontend preenche em tempo real a designação social, morada fiscal,
código postal e localidade. Valores vazios ou `---` não substituem o que o utilizador já escreveu e os campos continuam
editáveis; o operador deve confirmar a informação antes de criar o cliente. Uma indisponibilidade do VIES não bloqueia
a criação do cliente. Não é necessária API key. URL, timeouts, tentativas e limite de concorrência podem ser configurados
através de `VIES_BASE_URL`, `VIES_CONNECT_TIMEOUT`, `VIES_READ_TIMEOUT`, `VIES_MAX_ATTEMPTS`, `VIES_RETRY_DELAY` e
`VIES_MAX_CONCURRENT_REQUESTS`.

As decisões, contrato e limitações estão documentados em [`docs/vies-integration.md`](docs/vies-integration.md).

A conta do cliente está disponível em `/clientes/{id}/conta`. Os serviços são lidos dos envios persistidos através de `GET /api/customers/{id}/services`; os filtros por estado e data e a geração do resumo PDF usam esses dados reais da base local.

### Tabelas de preços

O módulo de configuração comercial usa rotas autónomas, sem categorias Business Parcel ou Express Parcel. Administradores podem criar, editar e eliminar rotas enquanto a tabela está em rascunho; administradores e contabilidade podem consultar preços e simular o custo de um envio em `/configuracao/tabelas-precos`. O cálculo considera peso real e volumétrico, escalões, quilograma adicional, combustível e IVA. O serviço autónomo corre na porta `8086`, é o único proprietário da base `pricing` e é consumido pelo frontend e pelo backend de Envios. Não existe ainda ligação à GLS nem geração de faturas.

### Serviços operacionais e zonas

O catálogo de serviços está disponível em `/configuracao/servicos`; grupos são geridos em `/configuracao/grupos-servicos` e zonas de faturação em `/billing/zones`. O serviço autónomo corre na porta `8085` e é dono da base `catalog`. Reúne identidade, trânsito por zona, regras futuras de cálculo, limites físicos, horários, recolhas associadas, capacidades e restrições por cliente/agência. Apenas administradores alteram estes dados.

Serviços operacionais e rotas de preços permanecem independentes. A arquitetura, regras e endpoints estão documentados em [`docs/OPERATIONAL_SERVICES.md`](docs/OPERATIONAL_SERVICES.md).

### Envios locais

`POST /api/shipments` cria um envio transacional: valida o cliente, calcula o preço, confirma que o país do destinatário corresponde à rota e cria ou reutiliza o destinatário antes de persistir o envio. ADMIN, OPERATOR e FRONT_DESK podem criar; ADMIN, OPERATOR, ACCOUNTING e FRONT_DESK podem consultar `GET /api/shipments`. Cada registo conserva fotografias dos dados comerciais usados no momento da criação, estados logístico e de pagamento e auditoria do utilizador responsável.

O modelo, as regras e os endpoints estão documentados em [`docs/PRICING.md`](docs/PRICING.md).

## Validação

- Reactor completo (recomendado em Windows): `.\scripts\mvn-docker.cmd clean test`
- Backend legado apenas: `.\scripts\mvn-docker.cmd -f backend/pom.xml clean test`
- Frontend: `cd frontend && npm run build`

O runner Maven usa Maven 3.9.11 e Java 21 dentro de Docker, reutiliza o volume
`ltft-maven-cache` e aceita os mesmos goals e opções do comando `mvn`. Desta forma,
o build não depende de uma instalação Maven no Windows e evita falhas do `ZipFS`
do JDK observadas em ambientes Windows isolados. É necessário ter o Docker Desktop
ativo. O goal `clean` é recomendado para não reutilizar classes `target` produzidas
no Windows quando o build corre no contentor Linux. Por exemplo, para produzir os
artefactos sem executar testes:

```powershell
.\scripts\mvn-docker.cmd clean -DskipTests package
```

### Testes E2E de segurança

O script `scripts/security-e2e.mjs` valida login, logout, recuperação da sessão após refresh/deep-link, as fronteiras dos perfis `ADMIN`, `OPERATOR`, `ACCOUNTING`, `CUSTOMER`, `DRIVER` e `FRONT_DESK` e o ciclo Joiner–Mover–Leaver através da plataforma. A CI confirma adicionalmente os três eventos `JOINER`, `MOVER` e `LEAVER` na tabela de auditoria PostgreSQL. O próprio teste cria utilizadores temporários no Keycloak e remove-os no fim; passwords e tokens não são guardados no repositório.

Com PostgreSQL, Keycloak, backend e frontend ativos, instale `playwright` como dependência de desenvolvimento do frontend e execute a partir da raiz:

```powershell
$env:KEYCLOAK_ADMIN = '<administrador-local>'
$env:KEYCLOAK_ADMIN_PASSWORD = '<password-local>'
node .\scripts\security-e2e.mjs
```

Opcionalmente, `PLAYWRIGHT_EXECUTABLE_PATH` pode indicar um Chromium/Chrome já instalado. Nunca use credenciais de produção nestes testes locais.

## Integração GLS

A integração está isolada pela interface `CarrierProvider`. Nesta fase só existe `MockGlsCarrierProvider`; não foram assumidos endpoints, autenticação, payloads ou códigos da GLS.

## Identidade e acesso

O âmbito de autenticação, autorização e gestão Joiner–Mover–Leaver está definido em [`docs/identity-and-access.md`](docs/identity-and-access.md).
O estado consolidado do produto e as validações operacionais encontram-se em [`docs/IMPLEMENTATION_STATUS.md`](docs/IMPLEMENTATION_STATUS.md).
