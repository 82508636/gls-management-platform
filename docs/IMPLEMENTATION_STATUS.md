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

## Validação de 3 de setembro de 2026

- Sete bases PostgreSQL acessíveis, incluindo a base do backend legado.
- Histórico Flyway sem migrations falhadas em qualquer base.
- Seis microserviços com `/actuator/health` em estado `UP`.
- Login real no Keycloak concluído e sessão OIDC recuperada pelo frontend.
- Chamadas autenticadas com perfil `ADMIN` a Customer, Pickup, Workforce, Identity, Catalog e Pricing devolveram `200`.
- A página de Clientes e a gestão JML carregaram dados reais depois da autenticação.

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
