# P0 — Autenticação, autorização e ciclo de vida de perfis

Este documento define o âmbito inicial de identidade e acesso do LTFT Comand Center. A implementação deve seguir OAuth 2.0/OpenID Connect com JWT, preferencialmente através de Keycloak, sem acoplar a lógica de negócio ao fornecedor de identidade.

## Funcionalidades P0

1. Autenticação e autorização
   - Login e logout seguros.
   - Proteção das páginas e dos endpoints da API.
   - Perfis base: `ADMIN`, `OPERATOR`, `ACCOUNTING` e `CUSTOMER`.
   - Associação controlada entre utilizadores e clientes quando aplicável.
   - Matriz explícita de permissões por perfil.
   - Auditoria das alterações de acesso.
   - Processo Joiner–Mover–Leaver para o tratamento do ciclo de vida dos perfis:
     - **Joiner:** criar/ativar a identidade, atribuir o perfil inicial e, quando aplicável, associar o cliente.
     - **Mover:** alterar perfis, permissões e associações, removendo os acessos que deixaram de ser necessários.
     - **Leaver:** desativar o acesso, terminar sessões/revogar acessos ativos e preservar o histórico de auditoria.

## Decisões que devem preceder a automatização JML

- Origem dos eventos Joiner–Mover–Leaver: operação manual, sistema de RH ou outro fornecedor de identidade.
- Aprovações exigidas para atribuir e alterar cada perfil.
- Matriz final de permissões e segregação de funções.
- Relação permitida entre utilizadores e clientes.
- Política de retenção do histórico e evidências de auditoria.

Até estas decisões serem fechadas, o fluxo JML será desenhado por interfaces e estados internos, sem assumir integrações externas nem regras organizacionais não fornecidas.

## Matriz inicial de proteção

| Recurso | Público | ADMIN | OPERATOR | ACCOUNTING | CUSTOMER |
| --- | --- | --- | --- | --- | --- |
| Health e info (`/actuator/health`, `/actuator/info`) | Sim | Sim | Sim | Sim | Sim |
| OpenAPI/Swagger (`/v3/api-docs/**`, `/swagger-ui/**`) | Sim, apenas nesta fase de desenvolvimento | Sim | Sim | Sim | Sim |
| Listar/consultar clientes (`GET /api/customers/**`) | Não | Sim | Sim | Sim | Não |
| Criar/editar clientes (`POST`/`PUT /api/customers/**`) | Não | Sim | Sim | Não | Não |
| Validar NIF/VIES (`POST /api/vat-validations`) | Não | Sim | Sim | Não | Não |
| Consultar a conta de cliente (`/clientes/{código}/conta`) | Não | Sim | Não | Sim | Não |
| Ativar/inativar clientes (`PATCH /api/customers/{id}/status`) | Não | Sim | Não | Não | Não |
| Eliminar clientes (`DELETE /api/customers/{id}`) | Não | Sim | Não | Não | Não |
| Restantes endpoints de negócio (`/api/**`) | Não | Autenticado | Autenticado | Autenticado | Autenticado, sujeito a regras futuras |

O perfil `CUSTOMER` não recebe acesso à API geral de clientes enquanto não existir uma associação verificável entre o `sub` do token e o cliente autorizado. Em produção, a exposição pública de Swagger deverá ser reavaliada.

## JML manual do P0

O processo é iniciado por um utilizador `ADMIN` em `/admin/utilizadores`:

- **Joiner:** cria uma identidade ativa no Keycloak, define uma password temporária e atribui exatamente um perfil da plataforma.
- **Mover:** substitui os perfis da plataforma pelo novo perfil selecionado. Roles internas do Keycloak não são alteradas.
- **Leaver:** desativa a identidade e termina as sessões ativas. O utilizador não é eliminado.

O backend usa um cliente técnico confidencial com permissões mínimas de gestão de utilizadores. O segredo nunca é enviado ao frontend. Cada operação grava ator, alvo, instante, ação e alteração de perfis na tabela `identity_audit_events`; passwords e tokens não são auditados. A alteração de perfil e a desativação do próprio administrador são recusadas. Contas técnicas do Keycloak não aparecem na lista JML.
