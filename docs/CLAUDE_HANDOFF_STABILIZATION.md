# LTFT Comand Center — Handoff de estabilização para Claude

> **Documento histórico.** Este handoff descreve o estado e os problemas observados antes da extração dos
> microserviços e das correções posteriores. Não deve ser usado como estado atual nem como especificação do VIES.
> Consulte [`IMPLEMENTATION_STATUS.md`](IMPLEMENTATION_STATUS.md), [`MICROSERVICES.md`](MICROSERVICES.md) e
> [`vies-integration.md`](vies-integration.md) para a implementação vigente.

## 1. Objetivo deste documento

Este documento descreve o estado atual do **LTFT Comand Center**, os testes já executados, os defeitos encontrados e o trabalho recomendado antes de iniciar novas funcionalidades.

O objetivo imediato não é redesenhar a plataforma. É estabilizar o que já existe, garantir que as regras de autorização são cumpridas e deixar as suites de testes verdes e reproduzíveis.

### Resultado esperado

No final deste trabalho deverão estar garantidos:

1. Backend Maven totalmente verde.
2. Frontend com testes automáticos mínimos.
3. Paginação correta na lista de clientes.
4. Sessão OIDC recuperada após refresh/deep-link.
5. Apenas ADMIN pode ativar clientes.
6. JML funcional com configuração Keycloak declarativa e reproduzível.
7. PDF coerente com os filtros apresentados ao utilizador.
8. Password temporária JML removida do estado ao fechar a modal.
9. Erros apresentados no frontend continuam genéricos.

---

## 2. Contexto técnico

### Componentes

- Backend: Java 21, Spring Boot 3.5.4, Spring Security Resource Server, Spring Data JPA.
- Base de dados: PostgreSQL 16.
- Migrations: Flyway V1–V5.
- Frontend: React, TypeScript, Vite.
- Identidade: Keycloak 26.3.3, OIDC Authorization Code + PKCE.
- Integração fiscal: API REST oficial VIES.
- Infraestrutura local: `compose.yaml` para PostgreSQL e Keycloak.

### Endpoints e portas locais

- Frontend: `http://localhost:5173`
- Backend: `http://localhost:8080`
- Backend health: `http://localhost:8080/actuator/health`
- Keycloak: `http://localhost:8180`
- Realm: `ltft`

### Regras funcionais de autorização

| Operação | ADMIN | OPERATOR | ACCOUNTING | CUSTOMER |
|---|---:|---:|---:|---:|
| Listar/consultar clientes | Sim | Sim | Sim | Não |
| Criar clientes | Sim | Sim | Não | Não |
| Editar clientes | Sim | Sim | Não | Não |
| Ativar/inativar clientes | Sim | Não | Não | Não |
| Eliminar clientes | Sim | Não | Não | Não |
| Consultar conta do cliente | Sim | Não | Sim | Não |
| Validar NIF no VIES | Sim | Sim | Não | Não |
| Administrar utilizadores/JML | Sim | Não | Não | Não |

Estas regras devem ser aplicadas no backend. O frontend pode esconder ou desativar ações, mas nunca pode ser a única camada de proteção.

---

## 3. Estado validado

### Infraestrutura e runtime

Foi confirmado:

- PostgreSQL disponível.
- Keycloak disponível.
- Frontend disponível.
- Backend com health `200`.
- Hibernate validou o schema.
- Flyway validou cinco migrations.
- Uma base que estava em V2 foi atualizada com sucesso para V5.

Ainda não existe teste automático de criação de uma base vazia V1→V5.

### Build frontend

O comando abaixo passou:

```powershell
cd frontend
npm.cmd run build
```

Resultado observado:

- TypeScript compilou.
- Vite transformou 22 módulos.
- Bundle de produção criado com sucesso.

Não existem atualmente testes Vitest/React Testing Library ou Playwright no projeto.

### Suite backend

Foram executados 34 testes:

- 29 passaram.
- 5 falharam.
- 0 erros de execução.
- 0 ignorados.

As cinco falhas estão nos testes de segurança e não correspondem ao comportamento observado com tokens reais do Keycloak. O harness de teste não aplica o conversor customizado de roles.

### Testes E2E reais executados

Foram usados utilizadores temporários separados, cada um com uma única role. Os utilizadores e o cliente temporário foram removidos no final.

Passaram:

- Login OIDC com PKCE.
- Logout Keycloak.
- `401` para pedidos anónimos.
- `403` para roles sem permissão.
- Matriz real de autorização dos endpoints.
- Joiner, Mover e Leaver.
- Bloqueio de self-move e self-leave.
- Persistência de três eventos de auditoria JML.
- Criação, consulta, edição, ativação e eliminação de clientes.
- Códigos `1xxxxx` para LTFT01.
- Códigos `2xxxxx` para LTFT02.
- Incremento sequencial dentro da agência.
- Rejeição de NIF duplicado com `409` genérico.
- Rejeição da alteração de agência.
- Limite de 50 clientes por pedido no backend.
- CORS para PATCH a partir de `http://localhost:5173`.
- Validação VIES local/determinística.
- Pesquisa e filtros visíveis na lista de clientes.
- Filtros por estado e data na conta do cliente.
- Geração do PDF.

Os dados temporários de clientes, utilizadores e auditoria foram limpos e os contadores de clientes foram repostos aos valores anteriores ao teste.

---

## 4. Bloqueadores P0

## P0.1 — Suite de segurança com cinco falsos `403`

### Sintoma

Os testes esperam acesso permitido, mas recebem `403`:

1. ACCOUNTING a listar clientes: esperado `200`, obtido `403`.
2. ADMIN a alterar estado: esperado `200`, obtido `403`.
3. ADMIN a eliminar cliente: esperado `204`, obtido `403`.
4. ADMIN/OPERATOR a chamar VIES: esperado `200`, obtido `403`.
5. ADMIN com payload VIES inválido: esperado `400`, obtido `403`.

### Ficheiros

- `backend/src/test/java/pt/glsmanagement/platform/customer/CustomerSecurityTest.java`
- `backend/src/test/java/pt/glsmanagement/platform/customer/VatValidationSecurityTest.java`
- `backend/src/main/java/pt/glsmanagement/platform/security/SecurityConfig.java`

### Causa provável

Os testes usam:

```java
.with(jwt().jwt(token -> token.claim("realm_access", realmRole("ADMIN"))))
```

O request post-processor `jwt()` cria uma autenticação de teste com authorities predefinidas. Ele não aplica automaticamente o `JwtAuthenticationConverter` da aplicação apenas porque a claim `realm_access` foi adicionada.

No runtime real, o conversor lê `realm_access.roles`, converte cada role para maiúsculas e adiciona o prefixo `ROLE_`. Por isso os testes E2E reais passaram e os MockMvc falharam.

### Alteração recomendada

Criar um helper de teste que forneça explicitamente as authorities esperadas:

```java
private static RequestPostProcessor jwtWithRole(String role) {
    return jwt()
            .jwt(token -> token.claim(
                    "realm_access",
                    Map.of("roles", List.of(role))))
            .authorities(new SimpleGrantedAuthority("ROLE_" + role));
}
```

Alternativa: testar diretamente o `KeycloakRealmRoleConverter` e usar as authorities resultantes no MockMvc.

Não enfraquecer `SecurityConfig` para fazer os testes passar.

### Critérios de aceitação

- `mvn test` termina com `BUILD SUCCESS`.
- ACCOUNTING continua sem permissão de escrita.
- OPERATOR continua sem permissão para status/delete.
- CUSTOMER continua sem acesso à lista geral.
- Testes adicionais cobrem claims ausentes e roles em minúsculas.

---

## P0.2 — Configuração Keycloak JML incompleta

### Sintoma

Com as permissões declaradas no realm, o backend consegue:

1. Criar o utilizador.
2. Definir a password temporária.
3. Falha ao obter a representação da role do realm.
4. Devolve `502`.
5. Deixa um utilizador parcialmente criado no Keycloak.

### Ficheiros

- `keycloak/realm/ltft-realm.json`
- `backend/src/main/java/pt/glsmanagement/platform/identity/KeycloakAdminClient.java`
- `backend/src/main/java/pt/glsmanagement/platform/identity/IdentityLifecycleService.java`
- `README.md`

### Causa

O service account declara:

```json
"realm-management": ["manage-users", "view-users", "query-users"]
```

Mas o backend executa:

```text
GET /admin/realms/{realm}/roles/{role}
```

Esse endpoint exigiu também `view-realm` no Keycloak 26.3.3 usado no teste.

### Correção mínima

Adicionar `view-realm` à configuração declarativa e à documentação:

```json
"realm-management": [
  "manage-users",
  "view-users",
  "query-users",
  "view-realm"
]
```

### Decisão recomendada de segurança

Antes de aceitar a permissão adicional, verificar se o backend pode obter a role através de um endpoint mais restrito, como as roles disponíveis do próprio utilizador. Se não for viável, documentar claramente a necessidade de `view-realm`.

### Problema adicional: operação não atómica

`@Transactional` protege apenas a base de dados local. Não reverte alterações já feitas no Keycloak.

O Joiner atual realiza:

1. Create user.
2. Reset password.
3. Replace roles.
4. Persist audit event.

Uma falha no passo 3 ou 4 deixa efeitos externos.

### Alteração recomendada

Implementar compensação no Joiner:

- Se a configuração da password ou role falhar depois da criação, eliminar/desativar o utilizador recém-criado.
- Registar o erro de forma segura, sem password, token ou dados internos.
- Considerar uma estratégia de estado `PENDING/FAILED` se a compensação também falhar.

### Critérios de aceitação

- Realm criado de raiz permite executar Joiner sem configuração manual.
- Joiner, Mover e Leaver passam em ambiente limpo.
- Falha forçada na atribuição de role não deixa utilizador ativo sem role.
- O frontend recebe apenas erro genérico.

---

## P0.3 — OPERATOR consegue criar clientes ativos

### Regra funcional

Somente ADMIN pode ativar ou inativar clientes.

### Sintoma confirmado

- O formulário inicia com `active: true`.
- O checkbox `Cliente ativo` está visível e editável para OPERATOR.
- O backend aceita `active: true` no POST de criação feito por OPERATOR.
- Um OPERATOR criou efetivamente um cliente ativo durante o teste.

### Ficheiros

- `frontend/src/main.tsx`
- `frontend/src/CustomerFormFields.tsx`
- `backend/src/main/java/pt/glsmanagement/platform/customer/CustomerService.java`
- `backend/src/main/java/pt/glsmanagement/platform/customer/Customer.java`

### Correção recomendada

Aplicar a regra no backend:

- Novos clientes devem ser sempre criados inativos, independentemente de `request.active()`.
- Ativação deve acontecer apenas pelo endpoint protegido `PATCH /api/customers/{id}/status`.

No frontend:

- Remover o checkbox `Cliente ativo` do formulário de criação/edição.
- Mostrar uma indicação como `O cliente ficará pendente de aprovação`.
- Manter ativação/inativação apenas no dropdown ADMIN.

### Critérios de aceitação

- POST por ADMIN ou OPERATOR cria sempre `active=false`.
- OPERATOR não vê nem consegue escolher o estado.
- Apenas PATCH por ADMIN consegue mudar o estado.
- Testes backend e frontend cobrem esta regra.

---

## P0.4 — Paginação frontend mostrava sempre a página zero — RESOLVIDO

### Ficheiro

- `frontend/src/main.tsx`, função `loadPage`.

### Problema original

O frontend pedia a página correta e atualizava os metadados:

```typescript
const result: CustomerPage = await response.json()
setPage(result.page)
setPageInfo(...)
```

Mas não guardava `result.content`. Em seguida executava:

```typescript
await refresh()
```

`refresh()` fazia sempre:

```text
GET /api/customers?page=0&size=50
```

O indicador podia mostrar página 2 enquanto a tabela continuava com dados da página 1.

### Correção implementada

Foi centralizada a obtenção de clientes numa função que recebe a página, guarda diretamente `result.content` no estado partilhado e devolve os metadados à página:

```typescript
async function loadCustomers(targetPage: number) {
  const response = await auth.fetch(
    `${apiUrl}/customers?page=${targetPage}&size=50`
  )
  const result: CustomerPage = await response.json()
  setApiCustomers(result.content)
  setPage(result.page)
  setPageInfo(...)
}
```

Foi removida a segunda chamada que regressava implicitamente à página zero. Criar recarrega a página zero; editar, ativar e eliminar recarregam a página atual. Se a eliminação deixar uma página vazia, a interface recua uma página.

### Dados persistidos

Os clientes mock foram removidos do frontend. A base local recebeu um seed explícito com 200 clientes fake persistidos, 100 por agência, sem alterar as migrations de produção.

### Validação executada

- `npm.cmd run build`: passou.
- TypeScript: passou.
- Vite: bundle de produção criado com sucesso.
- Não existe ainda uma suite frontend automatizada; o cenário com 51+ clientes deve ser adicionado a Vitest/Playwright no P2.

### Critérios de aceitação

- Com pelo menos 51 clientes, `Seguinte` mostra um conjunto diferente.
- `Anterior` regressa corretamente.
- O contador e a tabela pertencem à mesma página.
- Pesquisa e filtro não atuam apenas nos 50 registos locais sem indicação.

---

## P0.5 — Sessão desaparecia após refresh/deep-link — RESOLVIDO

### Ficheiro

- `frontend/src/auth.ts`

### Sintoma original

- Login funciona.
- Após F5, a aplicação mostra novamente o botão `Login`.
- A sessão SSO do Keycloak continua ativa.
- Clicar novamente em Login recupera a sessão sem pedir credenciais.

### Causa original

`accessToken`, `refreshToken` e `claims` existem apenas em variáveis do módulo:

```typescript
let accessToken = ''
let refreshToken = ''
let claims: TokenClaims = {}
```

`initializeAuth()` só recupera sessão quando a URL contém `code`.

### Correção implementada

Foi mantido Authorization Code + PKCE sem persistir tokens em `localStorage`:

- No arranque sem token, a aplicação executa autorização silenciosa com `prompt=none` uma vez por separador.
- O caminho completo, incluindo query e hash, é preservado em `sessionStorage` e validado como caminho local antes de ser restaurado.
- `login_required`, `interaction_required`, `consent_required` e `account_selection_required` regressam ao ecrã Login sem loop e sem texto técnico.
- Callback valida `state`, `code_verifier` e o modo da autorização.
- O logout usa `id_token_hint`, limpa tokens em memória e regressa à raiz permitida pelo cliente.
- Access, refresh e ID tokens continuam apenas em memória.

Alternativa futura mais robusta:

- BFF com cookie `HttpOnly`, `Secure` e `SameSite` adequado.

### Concorrência no refresh token

`refreshIfNeeded()` passou a usar uma promise partilhada. Pedidos simultâneos aguardam a mesma renovação e uma falha limpa os tokens em memória.

Implementação aplicada:

```typescript
let refreshPromise: Promise<void> | null = null
```

Todos os pedidos devem aguardar a mesma renovação.

### Validação executada

- Build TypeScript/Vite: passou.
- Sem sessão SSO: `prompt=none` regressou ao Login sem loop.
- F5 sem sessão: permaneceu estável no Login.
- Login manual: abriu o realm `ltft`, cliente `ltft-web`, com `code_challenge_method=S256`.
- Login autenticado Authorization Code + PKCE: passou.
- Recuperação silenciosa com sessão SSO ativa: passou.
- Logout com `id_token_hint` e redirect para a plataforma: passou.
- Após logout, uma nova tentativa `prompt=none` devolveu `login_required`, confirmando o fim da sessão SSO.
- O utilizador temporário criado para o teste foi eliminado no final.

### Critérios de aceitação

- F5 mantém o utilizador autenticado sem interação visível.
- Abrir `/clientes/{id}/conta` num novo separador recupera a sessão e o destino.
- Dois pedidos simultâneos próximos da expiração executam apenas um refresh.
- Tokens não são guardados em `localStorage`.

---

## 5. Correções P1

## P1.1 — PDF ignora os filtros visíveis

### Ficheiros

- `frontend/src/main.tsx`
- `frontend/src/pdfReport.ts`

### Sintoma confirmado

Com filtro de agosto aplicado, o PDF incluiu também um serviço de junho.

A tabela usa `filtered`, mas o botão chama:

```typescript
downloadPendingServicesPdf(customer, services)
```

### Decisão funcional necessária

Escolher e tornar explícito:

1. Gerar todos os pendentes, independentemente dos filtros; ou
2. Gerar os pendentes dentro do intervalo de datas selecionado.

Recomendação: o estado deve continuar fixo em pendentes, mas o intervalo de datas deve ser respeitado. O botão pode chamar-se `Gerar resumo dos pendentes filtrados`.

### Qualidade visual confirmada

- Logo LTFT correto.
- Cabeçalho correto.
- Três linhas com separadores.
- Total separado, sem sobreposição.
- Uma página A4 horizontal.
- Total de `51,85 EUR`.

### Problema de acentos

Alguns textos aparecem sem acentos: `PAGINA`, `REFERENCIA`, `SERVICO`, `Nao`.

Usar uma fonte Unicode incorporada ou configurar corretamente WinAnsi/encoding. Adicionar um teste visual ou extração que confirme `Página`, `Referência`, `Serviço` e `Não`.

### Critérios de aceitação

- PDF respeita a decisão funcional documentada.
- Nenhum serviço fora do intervalo selecionado quando o filtro for aplicável.
- Acentos portugueses renderizam corretamente.
- Total nunca sobrepõe a última linha.

---

## P1.2 — Password JML permanece após Cancelar/Fechar

### Ficheiro

- `frontend/src/AdminUsersPage.tsx`

### Sintoma confirmado

1. Abrir Novo utilizador.
2. Preencher username e password temporária.
3. Cancelar.
4. Reabrir.
5. Username e password continuam preenchidos.

### Correção recomendada

Centralizar o fecho:

```typescript
function closeJoiner() {
  setFormOpen(false)
  setJoiner(emptyJoiner)
  setError('')
}
```

Usar a mesma função em:

- Cancelar.
- Botão X.
- Clique no overlay.
- Fecho depois de sucesso.

### Critérios de aceitação

- Password fica vazia após qualquer forma de fecho.
- Username, email, nome e role regressam ao estado inicial.
- Teste React Testing Library cobre Cancelar, X e overlay.

---

## P1.3 — Falta de estados `saving/pending`

### Risco

Duplo clique pode repetir:

- POST de cliente.
- PUT de cliente.
- PATCH de estado.
- DELETE.
- Joiner/Mover/Leaver.

### Correção recomendada

- Estado `submitting` por formulário.
- Estado `pendingActionId` para ações de linha.
- Desativar botões durante o pedido.
- Não apresentar erro de uma segunda chamada depois de uma primeira chamada bem-sucedida.

---

## P1.4 — Conta e pesquisa dependem apenas da página carregada

### Problemas

- Pesquisa atua apenas na página atual carregada do backend.
- Filtro de estado atua apenas na página atual.
- A conta procura primeiro o cliente no array local.
- Um cliente fora da página zero pode surgir como não encontrado.

### Backend existente

Já existe:

```text
GET /api/customers/{uuid}
```

### Correção recomendada

- A conta deve carregar o cliente pelo UUID quando não existir no estado local.
- Adicionar estado de loading antes de mostrar `não encontrado`.
- Se pesquisa global for requisito, enviar `search` e `status` ao backend e paginar o resultado.
- Confirmar se o URL público deve usar UUID ou o código de seis dígitos.

---

## P1.5 — Países de clientes limitados aos países VIES

VIES aplica-se ao VAT intracomunitário, mas a ficha de cliente pode necessitar de qualquer país.

Separar:

- Lista de países válida para moradas/faturação.
- Elegibilidade para VIES.

Um país fora do VIES não deve impedir a criação do cliente; deve apenas produzir `NOT_APPLICABLE` na validação VIES.

---

## 6. Integração VIES — estado e requisitos

### Contrato implementado

Endpoint oficial:

```text
POST https://ec.europa.eu/taxation_customs/vies/rest-api/check-vat-number
```

Não necessita API key.

Contrato oficial:

```text
https://ec.europa.eu/assets/taxud/vow-information/swagger_publicVAT.yaml
```

### Endpoint interno

```text
POST /api/vat-validations
```

Roles: ADMIN e OPERATOR.

### Casos que passaram

#### Formato PT inválido

```json
{
  "countryCode": "PT",
  "vatNumber": "123",
  "subjectType": "COMPANY"
}
```

Resultado:

```json
{
  "formatValid": false,
  "viesStatus": "NOT_CHECKED"
}
```

#### Particular

Resultado esperado: `NOT_APPLICABLE`, sem chamada ao VIES.

#### País fora do VIES

Resultado esperado: `NOT_APPLICABLE`.

#### Serviço externo indisponível

Resultado esperado: HTTP `200` com `viesStatus: "UNAVAILABLE"`.

O frontend mostrou uma mensagem genérica e não expôs resposta, stack trace ou código interno do fornecedor.

### Limitação do teste atual

O ambiente Codex não conseguiu estabelecer ligação externa direta ao servidor da Comissão Europeia. Por isso ficou validado o comportamento de indisponibilidade, mas não uma resposta live `VALID/NOT_VALID`.

### Smoke recomendado

Criar um teste manual/opt-in, nunca obrigatório no CI:

- Desativado por defeito.
- Timeout curto.
- Sem NIF real em logs.
- Executado apenas num ambiente com saída HTTPS.
- Aceitar indisponibilidade temporária sem falhar a suite normal.

### Não alterar a semântica

VIES não prova a existência universal de um NIF. Valida se um VAT está registado/ativo para operações intracomunitárias.

Não preencher automaticamente todos os dados de faturação a partir do VIES. Nome e morada devolvidos podem faltar e não devem ser tratados como fonte autoritativa.

---

## 7. Política de erros

Requisito: não expor detalhes internos no frontend.

Foi confirmado que os seguintes erros usam mensagens genéricas:

- `400` validação/operação inválida.
- `404` recurso não encontrado.
- `409` NIF duplicado.
- `502` erro na operação de utilizador/Keycloak.
- VIES indisponível.

### Manter estas regras

- Não devolver stack traces.
- Não devolver mensagens do Keycloak/VIES.
- Não devolver nomes de constraints ou SQL.
- Não incluir passwords, tokens ou secrets em logs.
- Logs internos podem conter um correlation ID e categoria técnica, mas não dados sensíveis desnecessários.

Adicionar testes que garantam que respostas não contêm:

```text
Exception
constraint
Keycloak
stack
password
token
```

---

## 8. Cobertura automática em falta

### Backend

Não existem atualmente testes suficientes para:

- Identity/JML.
- KeycloakAdminClient.
- Compensação de Joiner parcial.
- PostgreSQL/Flyway real.
- Base vazia V1→V5.
- Upgrade V2→V5 com dados representativos.
- CustomerCodeGenerator.
- Concorrência dos contadores.
- Esgotamento do intervalo `99999`.
- `get`, `update`, not-found e agência imutável de CustomerService.
- Concorrência de NIF duplicado.
- CORS completo.
- Claims JWT ausentes/malformadas.

### Frontend

Não existe framework de testes configurado.

Adicionar no mínimo:

#### Vitest + React Testing Library

- Matriz de visibilidade por role.
- OPERATOR não vê controlo de ativação.
- Paginação usa o conteúdo da página pedida.
- Pesquisa/filtro.
- Formulário cria/edita e apresenta erros genéricos.
- VIES invalida resultado antigo quando NIF/país/tipo muda.
- Modal JML limpa password ao fechar.
- Botões ficam disabled durante submit.

#### Playwright

- Login/logout.
- Refresh com SSO.
- Deep-link para conta.
- ADMIN/OPERATOR/ACCOUNTING.
- Criação e aprovação de cliente.
- Conta e filtros.
- Download e validação básica do PDF.

---

## 9. Comandos de validação

### Iniciar infraestrutura

A partir da raiz do repositório:

```powershell
docker compose up -d
```

Não executar `docker compose down -v` sem confirmar que os volumes não contêm dados necessários.

### Backend

Se Maven e Java estiverem no PATH:

```powershell
mvn -f backend/pom.xml test
mvn -f backend/pom.xml spring-boot:run
```

Se não estiverem no PATH, usar os binários existentes em `work/tools` ou configurar `JAVA_HOME` e `Path` explicitamente.

### Frontend

```powershell
cd frontend
npm.cmd install
npm.cmd run build
npm.cmd run dev
```

Em PowerShell, usar `npm.cmd` caso a política bloqueie `npm.ps1`.

### Health

```powershell
Invoke-RestMethod http://localhost:8080/actuator/health
```

Esperado:

```json
{"status":"UP"}
```

### Keycloak discovery

```powershell
Invoke-RestMethod http://localhost:8180/realms/ltft/.well-known/openid-configuration
```

Não colocar credenciais administrativas ou client secrets em documentação, commits ou output de testes.

---

## 10. Ordem de implementação recomendada

### Fase 1 — Tornar a validação confiável

1. Corrigir os cinco testes de segurança.
2. Executar `mvn test` até `BUILD SUCCESS`.
3. Adicionar testes do conversor JWT.

### Fase 2 — Corrigir regras funcionais P0

1. Forçar novos clientes a inativos no backend.
2. Remover o checkbox de estado do formulário.
3. Corrigir paginação.
4. Corrigir sessão silenciosa e mutex do refresh token.

### Fase 3 — Estabilizar JML

1. Resolver `view-realm` declarativamente ou mudar o endpoint usado.
2. Implementar compensação de Joiner parcial.
3. Adicionar timeouts no RestClient Keycloak.
4. Adicionar testes de Joiner/Mover/Leaver.
5. Limpar password no frontend.

### Fase 4 — Corrigir conta e PDF

1. Definir semântica dos filtros do PDF.
2. Respeitar datas selecionadas.
3. Corrigir acentos/fontes.
4. Carregar cliente da conta pelo endpoint individual.

### Fase 5 — Automação E2E

1. Vitest/RTL.
2. Playwright.
3. PostgreSQL/Testcontainers.
4. Smoke VIES opt-in.

---

## 11. Definition of Done

Não considerar a estabilização concluída até todos os pontos abaixo passarem:

- [ ] `mvn test` verde.
- [ ] `npm.cmd run build` verde.
- [ ] Testes frontend configurados e verdes.
- [ ] Base PostgreSQL vazia migra até V5.
- [ ] Login/logout e F5 funcionam.
- [ ] Deep-link autenticado funciona.
- [ ] Matriz de roles testada automaticamente.
- [ ] OPERATOR nunca cria cliente ativo.
- [ ] Página 2 apresenta dados diferentes da página 1.
- [ ] Joiner não deixa utilizadores parciais após falha.
- [ ] Password temporária é limpa ao fechar a modal.
- [ ] PDF respeita a regra documentada de filtros.
- [ ] PDF mostra acentos corretamente.
- [ ] Erros frontend continuam genéricos.
- [ ] Nenhum secret foi introduzido no repositório.

---

## 12. Restrições para a alteração

- Não remover segurança para fazer testes passar.
- Não guardar tokens OIDC em `localStorage` como solução rápida.
- Não expor mensagens internas no frontend.
- Não usar o VIES como prova de existência universal do NIF.
- Não preencher automaticamente dados fiscais não autoritativos.
- Não alterar migrations já aplicadas; criar nova migration se o schema precisar de mudar.
- Não apagar volumes Docker existentes sem validação explícita.
- Não misturar a correção destes bloqueios com uma funcionalidade grande nova.

O trabalho deve ser entregue em alterações pequenas, testáveis e fáceis de rever.
