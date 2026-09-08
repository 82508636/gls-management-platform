# API Gateway

## Objetivo

O `api-gateway` é a entrada HTTP única usada pelo frontend. Usa Spring Boot 3.5.15 e Spring Cloud Gateway 4.3.5,
corre na porta `8090`, valida o JWT e encaminha apenas a
superfície `/api` explicitamente publicada. Endpoints internos (`/internal/**`) e endpoints de administração técnica
não são expostos.

O gateway é uma primeira barreira. Cada microserviço continua responsável por validar o JWT, consultar o estado JML e
aplicar as suas roles. Esta defesa em profundidade evita que um acesso direto à porta interna altere as permissões.

## Rotas publicadas

| Rota | Destino |
|---|---|
| `/api/pickup-points/**` | `pickup-service` |
| `/api/customers/**`, `/api/recipients/**`, `/api/vat-validations/**` | `customer-service` |
| `/api/reference-data/**` | `workforce-service` |
| `/api/admin/users/**` | `identity-service` |
| `/api/operational-services/**`, `/api/service-groups/**`, `/api/billing/zones/**` | `catalog-service` |
| `/api/pricing/**` | `pricing-service` |
| `/api/shipments/**`, `/api/customers/{id}/services` | backend legado |

A rota específica `/api/customers/{id}/services` precede a rota geral de Clientes para manter o workflow de Envios no
backend legado durante a migração.

## Políticas transversais implementadas

- autenticação JWT obrigatória e sem sessão de servidor em `/api/**`;
- CORS restrito às origens configuradas;
- limite de 2 MB por pedido e 16 KB para cabeçalhos;
- timeout de ligação de 2 segundos e resposta de 10 segundos por defeito;
- cabeçalhos HTTP seguros;
- erros `401` e `403` em JSON, sem detalhes internos;
- criação ou validação de `X-Correlation-ID`, propagado ao serviço e devolvido ao cliente;
- configuração no namespace nativo `spring.cloud.gateway.server.webflux`, sem depender do migrador de propriedades;
- `Cache-Control: no-store` enquanto não existir uma política de cache compatível com JML;
- rate limiting distribuído em Redis, por identidade autenticada: 20 pedidos/segundo e rajada de 40 por defeito;
- métricas e health check por Actuator.

## Cache e rate limiting

Não existe cache partilhada neste incremento. Uma resposta servida sem contactar o serviço de destino poderia evitar a
validação de revogação imediata. A futura cache ficará limitada a dados de referência explicitamente classificados,
terá invalidação em escritas e só será ativada depois de o gateway validar também a decisão JML.

O rate limiting usa um token bucket em Redis e a identidade autenticada como chave. Assim, várias instâncias do gateway
partilham a mesma decisão e um utilizador não obtém mais capacidade por mudar de instância. Pedidos API sem identidade
não criam um bucket anónimo partilhado e já são recusados pela camada de autenticação. Os cabeçalhos `X-RateLimit-*`
são expostos por CORS para diagnóstico pelo cliente web.

## Configuração local

- `VITE_GATEWAY_API_URL=http://localhost:8090/api`
- `API_GATEWAY_PORT=8090`
- `GATEWAY_CONNECT_TIMEOUT_MS=2000`
- `GATEWAY_RESPONSE_TIMEOUT=10s`
- `GATEWAY_MAX_REQUEST_SIZE=2MB`
- `GATEWAY_MAX_HEADER_SIZE=16KB`
- `GATEWAY_RATE_LIMIT_REPLENISH=20`
- `GATEWAY_RATE_LIMIT_BURST=40`
- `REDIS_TIMEOUT=2s`
- `LEGACY_BACKEND_URL=http://host.docker.internal:8080` dentro do Compose

O backend legado deve estar ativo na porta `8080` para os endpoints de Envios. Os restantes destinos usam os nomes dos
serviços da rede Compose.

No Compose local, as portas dos serviços continuam publicadas para diagnóstico. Em produção, apenas a porta do
gateway deve ficar acessível externamente; as portas dos serviços devem permanecer exclusivas da rede interna.
