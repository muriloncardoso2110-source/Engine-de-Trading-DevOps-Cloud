# Aurora Exchange

**Matching e liquidação de ordens em Java 21**, desenvolvido como laboratório de engenharia de baixa latência. O objetivo é experimentar prioridades de preço/tempo, consistência de saldos e observabilidade de uma engine de negociação — sem depender de infraestrutura paga.

> **Escopo:** simulação financeira. Não conecta a bolsa, não movimenta dinheiro real e não foi projetado para receber tráfego público.

## Começar por aqui

**Requisitos:** JDK 21 e Maven 3.9+ disponíveis no `PATH`. VS Code é opcional.

No PowerShell, a partir da raiz do projeto:

```powershell
java -version
mvn -version
mvn clean verify
java -jar target/aurora-exchange-1.0.0.jar
```

Abra <http://localhost:8080/health>. Em outro terminal:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\smoke-test.ps1
```

Se preferir, rode `scripts\start-windows.bat`. Não precisa de Docker para desenvolver ou testar o núcleo da aplicação.

## O que o sistema faz

- Recebe ordens limitadas `BUY` e `SELL` por API HTTP (`application/x-www-form-urlencoded`).
- Usa **ordenação por preço e FIFO** para casar ofertas do mesmo ativo fictício.
- Separa saldo disponível e reservado, liquida negócios e permite cancelamento de ordens em aberto.
- Evita execução contra a própria conta cancelando a ordem em repouso antes do casamento.
- Mantém histórico recente de 50 negócios e exporta métricas no formato Prometheus.
- Executa a API com **Virtual Threads** e serializa mutações via **LMAX Disruptor**, em um único consumidor.

As contas 1 a 100 começam com saldos artificiais. Tudo fica em memória: reiniciar a aplicação reinicia as contas e o livro.

## API

| Método | Rota | Descrição |
| --- | --- | --- |
| GET | `/health` | Health check |
| GET | `/book` | Melhor preço de compra/venda e quantidade de níveis |
| GET | `/accounts?id=1` | Saldo disponível e reservado da conta |
| GET | `/trades` | Últimos negócios |
| GET | `/metrics` | Métricas Prometheus |
| POST | `/orders` | `account`, `side`, `price`, `quantity` |
| POST | `/cancel` | `account`, `orderId` |

Exemplo (PowerShell, duas transações):

```powershell
Invoke-RestMethod -Method Post http://localhost:8080/orders -Body @{ account=2; side='SELL'; price=100; quantity=5 }
Invoke-RestMethod -Method Post http://localhost:8080/orders -Body @{ account=1; side='BUY'; price=110; quantity=5 }
Invoke-RestMethod http://localhost:8080/trades
```

Preços e quantidades são **inteiros**. O preço de execução é o preço da ordem já presente no livro. Não há taxas, múltiplos instrumentos, casas decimais ou precificação de ativos reais.

## Benchmark e evidências

```powershell
scripts\benchmark-windows.bat
# ou
java -jar target/aurora-exchange-1.0.0.jar benchmark 10000 5
```

O benchmark tem uma carga **alvo** de 100.000 submissões/segundo por 10 s (configurável), e informa throughput realizado, p50/p95/p99/p99.9 e falhas. PASS exige >=95% do alvo, p99 <10 ms e zero exceções. **Esses números são uma meta, não um resultado medido ou garantido.** A carga é **in-process**, não requisições HTTP reais. Rode em máquina estável, salve a saída e especifique CPU, memória, Java, SO e flags. Mais detalhes em [`docs/PERFORMANCE.md`](docs/PERFORMANCE.md).

## DevOps / Cloud sem cobrança

Para rodar a stack opcional de observabilidade (requer Docker Engine com Compose ou uma alternativa compatível):

```bash
docker compose up --build
```

- Serviço: <http://localhost:8080/health>
- Prometheus: <http://localhost:9090>
- Grafana: <http://localhost:3000> (usuário `admin`; configure `GRAFANA_PASSWORD` em `.env`)

A configuração do datasource do Grafana é provisionada em `infra/monitoring/grafana/`. Para Kubernetes local, veja [`docs/RUNBOOK.md`](docs/RUNBOOK.md). Terraform configura um *namespace* em um cluster Kubernetes já existente, sem contratar provedores. A pipeline do GitHub Actions executa testes e build. Limites de gratuidade de plataformas externas variam.

## Arquitetura e trade-offs

```text
HTTP / Virtual Threads
       |
       v
   Disruptor (ring buffer, produtor múltiplo)
       |
       v
 Consumidor único do OrderBook
       |-- ordens e reservas
       |-- matching preço/tempo
       '-- liquidação em memória
       |
       +--> CompletableFuture --> resposta HTTP
```

O consumidor único é uma decisão deliberada: evita mutações concorrentes do livro, mas também define o teto de vazão. O ring buffer reaproveita *slots*, não todos os objetos; existem alocações de futures, ordens e mapas. Consulte [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) para decisões técnicas e limitações.

## Estrutura

```text
src/main/java/com/aurora/    engine, livro, API e benchmark
src/test/java/com/aurora/    testes de matching e saldo
infra/k8s/                 deployment local
infra/terraform/           IaC de laboratório
infra/monitoring/          Prometheus + Grafana
.github/workflows/         CI
scripts/                   comandos Windows e Linux
docs/                      decisões, performance e operação
```

## Limitações importantes

Não há autenticação, TLS, banco de dados, log persistente, replay, idempotência, alta disponibilidade ou conformidade regulatória. Não exponha portas fora da sua máquina/rede privada. Não aumente réplicas no Kubernetes: cada réplica teria um livro de ofertas independente. Para um produto real, a próxima etapa é um log de eventos persistente, recuperação determinística, controle de acesso e testes sob falha.

**Licença:** MIT (ver `LICENSE`).
