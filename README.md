# CreatorPay MCP

Servidor **MCP (Model Context Protocol)** em **Java 21 + Spring Boot 4 + Spring AI** que dá a agentes de IA acesso aos dados de uma plataforma de pagamentos para creators: vendas, status de pagamento, carrinhos abandonados, pagamentos vencidos e sugestões de recuperação de vendas.

> **Projeto demonstrativo.** A CreatorPay não existe, e os produtores, compradores e pedidos são fictícios, gerados na inicialização. O código é real e testado.

## Por que fiz este projeto

No meu estágio desenvolvi um servidor MCP que conecta o ERP da empresa a agentes de IA. Esse código é privado, então criei este projeto do zero, com outro domínio, para mostrar publicamente como penso esse tipo de integração. Escolhi o domínio de pagamentos para creators porque é onde recuperação de vendas, PIX, boleto e webhooks fazem parte do dia a dia.

## Exemplo de uso

Com o servidor conectado a um cliente MCP (Claude, Cursor, MCP Inspector...), dá para perguntar em linguagem natural:

> **Usuário:** Quanto o Código do Zero deixou de ganhar com carrinhos abandonados esse mês? O que dá pra fazer?

O agente chama, por conta própria:

1. `listarProdutores` → descobre que "Código do Zero" tem o ID 2
2. `resumoDoProdutor(produtorId: 2)` → R$ 10.792 em 16 carrinhos abandonados nos últimos 30 dias
3. `carrinhosAbandonados(produtorId: 2)` → os pedidos, do maior para o menor valor
4. `sugerirRecuperacao(codigoPedido: "CP-10166")` → canal, ação e um rascunho de mensagem

## Ferramentas

| Ferramenta | O que faz |
|---|---|
| `listarProdutores` | Lista produtores com ID, nome, nicho e quantidade de produtos |
| `resumoDoProdutor` | Faturamento, vendas pagas, ticket médio, taxa de conversão e valores em carrinhos abandonados e pagamentos vencidos |
| `vendasPorProduto` | Ranking de produtos por faturamento (agregação feita no banco, com JPQL) |
| `statusPagamento` | Detalhes de um pedido pelo código (`CP-10001`) |
| `carrinhosAbandonados` | Até 20 carrinhos abandonados no período, do maior para o menor valor |
| `pagamentosVencidos` | Até 20 pedidos com PIX ou boleto vencido |
| `sugerirRecuperacao` | Sugere canal, ação e mensagem para recuperar a venda (só sugere, não envia nada) |

Todas são **somente leitura** e declaram isso ao cliente (`readOnlyHint`). Os parâmetros `dias` são opcionais (padrão de 30, máximo de 90).

## Arquitetura

```mermaid
flowchart LR
    A[Agente de IA<br/>Claude, Cursor...] -- "MCP · Streamable HTTP<br/>POST /mcp" --> B[CreatorPayMcpTools<br/>@McpTool]
    B --> C[VendasService<br/>RecuperacaoService]
    C --> D[(H2 · dados fictícios)]
    G[Gateway de pagamento<br/>simulado] -- "POST /webhooks/pagamentos<br/>assinado com HMAC" --> W[WebhookController]
    W --> S[WebhookService]
    S --> D
```

```
src/main/java/io/github/johnsampa/creatorpay
├── mcp/         ferramentas MCP (camada fina, como um controller)
├── service/     regras de negócio e consultas
├── webhook/     recebimento de eventos de pagamento (HMAC + idempotência)
├── domain/      entidades JPA
├── repository/  Spring Data JPA
├── dto/         o que o agente recebe (records)
└── config/      relógio injetável e gerador de dados fictícios
```

## Como rodar

Requisitos: Java 21 (o Maven Wrapper já vem no projeto).

```bash
./mvnw spring-boot:run
```

O servidor MCP sobe em `http://localhost:8080/mcp`.

Com Docker:

```bash
docker build -t creatorpay-mcp .
docker run -p 8080:8080 -e CREATORPAY_WEBHOOK_SECRET=meu-segredo creatorpay-mcp
```

### Conectar a um cliente MCP

**Claude Code:**

```bash
claude mcp add --transport http creatorpay http://localhost:8080/mcp
```

**MCP Inspector** (interface para testar as ferramentas na mão):

```bash
npx @modelcontextprotocol/inspector
```

No Inspector, escolha o transporte *Streamable HTTP* e a URL `http://localhost:8080/mcp`.

### Simular um webhook de pagamento

```bash
BODY='{"idEvento":"evt_123","tipo":"pagamento.aprovado","codigoPedido":"CP-10005"}'
SIG=$(printf '%s' "$BODY" | openssl dgst -sha256 -hmac "dev-secret-troque-em-producao" | sed 's/^.* //')

curl -X POST localhost:8080/webhooks/pagamentos \
  -H "Content-Type: application/json" \
  -H "X-CreatorPay-Signature: sha256=$SIG" \
  -d "$BODY"
# {"resultado":"PROCESSADO"}. Enviando de novo: {"resultado":"DUPLICADO"}
```

Tipos aceitos: `pagamento.aprovado`, `pagamento.vencido` e `pagamento.reembolsado`. Assinatura inválida retorna `401`.

## Decisões técnicas

- **Descrições escritas para o modelo.** Cada `@McpTool` diz quando usar a ferramenta, o formato dos parâmetros e qual chamar antes ou depois. As `instructions` do servidor reforçam o fluxo (ex.: "se citarem o produtor pelo nome, chame `listarProdutores` primeiro").
- **Erros que orientam o agente.** Em vez de um erro genérico, a mensagem diz o que fazer: *"Produtor 99 não encontrado. Use listarProdutores para ver os IDs válidos."*
- **A IA não decide a regra de negócio.** `sugerirRecuperacao` segue regras fixas e testadas (boleto vencido → oferecer PIX; carrinho com menos de 24h → lembrete sem desconto; de 24h a 72h → cupom de 10%; mais de 72h → cupom de 15%). O agente só redige ou adapta a mensagem.
- **Mínimo de dados pessoais.** O e-mail do comprador sai mascarado (`an***@email.test`), porque o agente não precisa dele completo.
- **Webhook seguro:**
  - HMAC-SHA256 calculado sobre o corpo cru (bytes), com comparação em tempo constante (`MessageDigest.isEqual`).
  - Idempotência pelo ID do evento, já que gateways reenviam eventos.
- **Camadas separadas.** A classe de ferramentas MCP é fina, como um controller; a regra fica nos services. Dá para expor as mesmas regras por REST sem duplicar código.
- **Testabilidade.** O `Clock` é injetado (as regras de tempo são testadas com um relógio fixo), e a regra de recuperação é uma função pura, testada sem subir o Spring.

## Testes

```bash
./mvnw test
```

São 31 testes:

- **Regras de recuperação:** testes de unidade, sem Spring.
- **Assinatura HMAC e máscara de e-mail:** testes de unidade.
- **Ferramentas:** chamadas como o agente chamaria, com os dados fictícios.
- **Webhook:** status atualizado, evento duplicado, `401`, `404` e `400`.
- **Protocolo MCP de ponta a ponta:** sobe o servidor numa porta real, faz `initialize`, `tools/list` e `tools/call` em JSON-RPC.

## Como usei IA neste projeto

Desenvolvi este projeto com o Claude Code como par de programação. Eu defini o domínio, as ferramentas, as regras de recuperação e as decisões de segurança. A IA acelerou a escrita do código e dos testes, e eu revisei, rodei e ajustei cada parte. Uso IA como ferramenta de trabalho, mas o que está aqui eu sei explicar e modificar.

## Próximos passos

- Ferramenta de clientes recorrentes (LTV por comprador)
- PostgreSQL com Docker Compose no lugar do H2
- Autenticação no endpoint `/mcp` (OAuth, como prevê a especificação do MCP)

## Licença

MIT
