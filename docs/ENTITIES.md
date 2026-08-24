# Entidades

## Destinatários

Os destinatários não têm criação manual. O serviço interno
`RecipientRegistrationService.registerFromShipment(...)` deve ser chamado pela futura transação de
criação de uma recolha ou envio.

O registo é idempotente: nome, morada, código postal, localidade e país são normalizados e usados para
calcular uma chave SHA-256. Um destinatário já conhecido é atualizado e recebe uma nova data de última
utilização, em vez de ser duplicado.

Endpoint disponível nesta fase:

- `GET /api/recipients?page=0&size=50` — ADMIN, OPERATOR e ACCOUNTING.

Não existe `POST /api/recipients`; essa operação está deliberadamente reservada ao fluxo interno de
recolhas/envios.

## Pontos Pickup

Dados persistidos:

- código, designação e fornecedor;
- horário de manhã e de tarde;
- morada, código postal, localidade e país;
- email, telefone e telemóvel;
- abertura ao sábado e domingo;
- estado ativo/inativo.

Endpoints:

- `GET /api/pickup-points` — ADMIN, OPERATOR e ACCOUNTING;
- `POST /api/pickup-points` — ADMIN e OPERATOR;
- `PUT /api/pickup-points/{id}` — ADMIN e OPERATOR;
- `PATCH /api/pickup-points/{id}/status` — ADMIN;
- `DELETE /api/pickup-points/{id}` — ADMIN.

As páginas têm limite máximo de 50 registos. O fornecedor é, por enquanto, um código textual controlado;
não foi criada uma entidade Fornecedor.

## Colaboradores

A página `/entidades/colaboradores` é um protótipo acessível apenas a ADMIN. Não envia dados ao backend,
não persiste informação e mostra essa limitação explicitamente ao utilizador.

Os campos estão agrupados em identificação, informação profissional, contactos pessoais, dados fiscais e
familiares, formação académica, informação bancária, redes sociais e observações. O modelo e as regras de
negócio serão definidos depois da validação funcional do formulário.

## Fornecedores

Em stand by. Existe apenas uma página informativa, sem API, tabela ou formulário.
