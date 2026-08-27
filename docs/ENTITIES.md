# Entidades

## Destinatários

Os destinatários não têm criação manual. O serviço interno
`RecipientRegistrationService.registerFromShipment(...)` deve ser chamado pela futura transação de
criação de uma recolha ou envio.

O registo é idempotente: nome, morada, código postal, localidade e país são normalizados e usados para
calcular uma chave SHA-256. Um destinatário já conhecido é atualizado e recebe uma nova data de última
utilização, em vez de ser duplicado.

Endpoint disponível nesta fase:

- `GET /api/recipients?page=0&size=50` — ADMIN, OPERATOR, ACCOUNTING e FRONT_DESK.

Não existe `POST /api/recipients`; essa operação está deliberadamente reservada ao fluxo interno de
recolhas/envios.

## Pontos Pickup

Os Pontos Pickup são locais operacionais associados aos futuros registos de envio e recolha. Identificam
onde uma encomenda foi entregue ou levantada; não representam fornecedores nem lojas na modelação do sistema.

Dados persistidos:

- código e designação;
- horário de manhã e de tarde;
- morada, código postal, localidade e país;
- email, telefone e telemóvel;
- abertura ao sábado e domingo;
- estado ativo/inativo.

Endpoints:

- `GET /api/pickup-points` — ADMIN, OPERATOR, ACCOUNTING e FRONT_DESK;
- `POST /api/pickup-points` — ADMIN, OPERATOR e FRONT_DESK;
- `PUT /api/pickup-points/{id}` — ADMIN, OPERATOR e FRONT_DESK;
- `PATCH /api/pickup-points/{id}/status` — ADMIN;
- `DELETE /api/pickup-points/{id}` — ADMIN.

As páginas têm limite máximo de 50 registos. A associação de um Ponto Pickup a uma encomenda será feita
no futuro módulo de envios e recolhas, sem dependência da entidade Fornecedor.

Os períodos da manhã e da tarde são opcionais, mas cada período tem de ser indicado por completo e a
hora final tem de ser posterior à inicial. Quando ambos existem, não podem sobrepor-se; períodos
adjacentes são aceites. Os indicadores de sábado e domingo continuam a usar estes horários gerais até
serem definidos horários específicos de fim de semana.

## Colaboradores

A página `/entidades/colaboradores` continua a ser um protótipo acessível apenas a ADMIN e ainda não
persiste colaboradores. Os catálogos usados pelo formulário — perfis de conta e categorias
profissionais — passam a ser persistidos e auditados na base de dados.

Endpoints de catálogo, exclusivos de ADMIN:

- `GET/POST/PUT/PATCH /api/reference-data/account-profiles`;
- `GET/POST/PUT/PATCH /api/reference-data/professional-categories`.

As permissões efetivas continuam a ser administradas no Keycloak. O catálogo de perfis guarda a
designação apresentada no formulário, não substitui os roles nem concede acessos.

Os campos estão agrupados em identificação, informação profissional, contactos pessoais, dados fiscais e
familiares, formação académica, informação bancária, redes sociais e observações. O modelo e as regras de
negócio serão definidos depois da validação funcional do formulário.

## Clientes e destinatários

Ao criar ou atualizar um cliente, o respetivo destinatário é criado ou atualizado na mesma transação.
O destinatário fica associado ao código do cliente e recebe os dados operacionais do cliente. A futura
criação de uma recolha ou envio deve continuar a chamar `registerFromShipment(...)`, que reutiliza o
mesmo mecanismo idempotente.

O NIF é guardado de forma canónica: país normalizado (`GR` é convertido em `EL`), prefixo de país e
separadores removidos do número. A unicidade é aplicada na base de dados pela chave `país:número`, pelo
que variantes como `PT 509 321 765` e `509321765` para Portugal representam o mesmo NIF.

## Fornecedores

Em stand by. Existe apenas uma página informativa, sem API, tabela ou formulário.
