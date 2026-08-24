# LTFT Comand Center

Plataforma web de gestão comercial e financeira de envios GLS. A GLS continua a ser o *source of truth* logístico; esta aplicação mantém a visão comercial e financeira.

## Estrutura

- `backend/`: monólito modular Java 21 + Spring Boot
- `frontend/`: React + TypeScript + Vite
- `compose.yaml`: PostgreSQL para desenvolvimento local

## Pré-requisitos

- Java 21
- Maven 3.9+
- Node.js 22+
- Docker com Docker Compose

## Arranque local

1. Copiar `.env.example` para `.env` se for necessário alterar a configuração.
2. Iniciar PostgreSQL e Keycloak: `docker compose up -d postgres keycloak`.
3. Iniciar o backend: `cd backend && mvn spring-boot:run`.
4. Iniciar o frontend: `cd frontend && npm install && npm run dev`.

O backend fica disponível em `http://localhost:8080`, o frontend em `http://localhost:5173`, o Keycloak em `http://localhost:8180` e o health check em `http://localhost:8080/actuator/health`.

O realm local `ltft` é importado automaticamente com o cliente público `ltft-web` e os perfis `ADMIN`, `OPERATOR`, `ACCOUNTING` e `CUSTOMER`. As credenciais administrativas locais vêm de `.env`; os valores de exemplo não devem ser usados em produção.

O frontend autentica através de OIDC Authorization Code com PKCE. Ao abrir `http://localhost:5173`, a aplicação tenta recuperar silenciosamente uma sessão SSO existente (`prompt=none`); sem sessão, apresenta o botão `Login` sem entrar num ciclo de redirects. A password é introduzida exclusivamente no Keycloak. O cabeçalho apresenta o utilizador autenticado e permite terminar a sessão através do endpoint OIDC com `id_token_hint`. Access, refresh e ID tokens são mantidos apenas em memória; não são guardados em `localStorage`. O refresh token é renovado através de uma única operação partilhada quando existem pedidos concorrentes.

Se o volume local do Keycloak já tiver sido criado antes de uma alteração ao ficheiro do realm, a importação não substitui automaticamente o realm existente. Durante desenvolvimento, ajuste o cliente `ltft-web` na consola ou recrie deliberadamente o volume do Keycloak depois de confirmar que não contém utilizadores necessários.

## Dados locais de clientes

O frontend não contém clientes ou serviços mock. Para preencher a base local com 200 clientes fake reproduzíveis, execute a partir da raiz do repositório:

```powershell
Get-Content -LiteralPath .\scripts\seed-local-fake-customers.sql -Raw |
  docker compose exec -T postgres psql -U gls -d gls_management
```

O seed cria 100 clientes por agência, com códigos sequenciais, NIFs numéricos com checksum português válido e uma combinação de estados ativos/inativos. Reexecuções substituem exclusivamente os registos identificados por `FAKE-SEED-*`; clientes reais são preservados. Este script é apenas para ambientes locais e não faz parte das migrations Flyway de produção.

### Gestão JML

Administradores acedem a `/admin/utilizadores` através da opção `Gerir utilizadores` no menu do cabeçalho. A página permite criar utilizadores com password temporária, substituir o perfil da plataforma e desativar contas com revogação de sessões.

O backend usa o cliente confidencial `ltft-jml-service`. Num realm já existente, crie esse cliente com `Service accounts roles` ativo, configure o segredo definido em `KEYCLOAK_JML_CLIENT_SECRET` e atribua à conta técnica as client roles `manage-users`, `view-users`, `query-users` e `view-realm` do cliente `realm-management`. Não exponha esse segredo no frontend.

O primeiro módulo funcional permite criar, listar e editar clientes em `http://localhost:5173/clientes`. A API paginada está disponível em `/api/customers`, com um máximo de 50 clientes por pedido.

A ficha de cliente separa dados gerais/contactos dos dados de faturação. São obrigatórios a designação para expedição, a agência (`LTFT01` Fafe ou `LTFT02` Taipas) e o NIF. O código interno de seis dígitos fica bloqueado no formulário e é atribuído apenas quando a criação é aprovada: começa por `1` em Fafe e por `2` nas Taipas, seguindo um contador transacional independente por agência. Código e agência tornam-se imutáveis após a criação. Cód. Conta, Ref. Faturação e câmbio permanecem opcionais e em avaliação; não existe integração com o Enovo.

### Validação de NIF e VIES

O botão `Validar` na ficha do cliente chama o endpoint protegido `POST /api/vat-validations`. Para NIF portugueses, o backend valida primeiro formato e dígito de controlo. Empresas de países abrangidos são depois consultadas no serviço REST oficial VIES. Para particulares, o VIES não é consultado; fora de Portugal só é verificada a sintaxe genérica do número fiscal.

O resultado é informativo: `não confirmado no VIES` não significa que o NIF ou a empresa não existem. A integração não copia nome, morada ou outros dados de faturação devolvidos pelo serviço externo e uma indisponibilidade do VIES não bloqueia a criação do cliente. Não é necessária API key. URL, timeouts e limite de concorrência podem ser configurados através de `VIES_BASE_URL`, `VIES_CONNECT_TIMEOUT`, `VIES_READ_TIMEOUT` e `VIES_MAX_CONCURRENT_REQUESTS`.

As decisões, contrato e limitações estão documentados em [`docs/vies-integration.md`](docs/vies-integration.md).

A conta do cliente está disponível em `/clientes/{id}/conta`. Nesta fase, os serviços e respetivos estados de pagamento são dados de demonstração isolados no frontend para validar navegação e filtros por estado e data.

O frontend já contém a apresentação da conta e a geração de PDF, mas não injeta serviços mock. Enquanto não existir uma fonte real de serviços, a conta aparece vazia e o botão `Gerar resumo PDF` permanece desativado. O PDF não aplica IVA nem regras fiscais ainda não especificadas.

## Validação

- Backend: `cd backend && mvn test`
- Frontend: `cd frontend && npm run build`

### Testes E2E de segurança

O script `scripts/security-e2e.mjs` valida login, logout, recuperação da sessão após refresh/deep-link, as fronteiras dos perfis `ADMIN`, `OPERATOR`, `ACCOUNTING` e `CUSTOMER` e o ciclo Joiner–Mover–Leaver através da plataforma. A CI confirma adicionalmente os três eventos `JOINER`, `MOVER` e `LEAVER` na tabela de auditoria PostgreSQL. O próprio teste cria utilizadores temporários no Keycloak e remove-os no fim; passwords e tokens não são guardados no repositório.

Com PostgreSQL, Keycloak, backend e frontend ativos, instale `playwright` como dependência de desenvolvimento do frontend e execute a partir da raiz:

```powershell
$env:KEYCLOAK_ADMIN = '<administrador-local>'
$env:KEYCLOAK_ADMIN_PASSWORD = '<password-local>'
node .\scripts\security-e2e.mjs
```

Opcionalmente, `PLAYWRIGHT_EXECUTABLE_PATH` pode indicar um Chromium/Chrome já instalado. Nunca use credenciais de produção nestes testes locais.

## Integração GLS

A integração está isolada pela interface `CarrierProvider`. Nesta fase só existe `MockGlsCarrierProvider`; não foram assumidos endpoints, autenticação, payloads ou códigos da GLS.

## Próximo P0: identidade e acesso

O âmbito de autenticação, autorização e gestão Joiner–Mover–Leaver está definido em [`docs/identity-and-access.md`](docs/identity-and-access.md).
