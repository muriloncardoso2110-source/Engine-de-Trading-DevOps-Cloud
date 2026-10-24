# Decisões de arquitetura

## 001 — Um escritor por livro

**Contexto:** sincronizar bids, asks e reservas com locks por operação torna o comportamento mais difícil de reproduzir e medir.

**Decisão:** operações entram em um Disruptor com múltiplos produtores e são aplicadas sequencialmente por um consumidor. A resposta é comunicada pelo future associado ao evento.

**Consequências:** operações são ordenadas pelo sequenciamento de publicação; consultas também passam pelo consumidor para evitar leituras durante uma mutação. Não há escalabilidade horizontal transparente: para múltiplos ativos, será necessário particionar por instrumento com regras explícitas de liquidação.

## 002 — Inteiros para preço e quantidade

Não usamos `double` para valores financeiros. Este protótipo usa inteiros sem fração de unidade e valida a multiplicação de preço por quantidade. Em uma versão com instrumentos reais, cada ativo precisaria declarar escala e *tick size*.

## 003 — API desacoplada do hot path

Virtual Threads resolvem a concorrência de conexões e aguardam `CompletableFuture` sem exigir uma thread de plataforma por requisição. Elas não aceleram o consumidor único. O benchmark interno mede submissão até confirmação, sem HTTP, serialização ou rede.

## 004 — Estado volátil (limitação aceita no laboratório)

Não há escrita durável. Um processo interrompido perde saldos e ordens. A estratégia de recuperação apropriada seria um registro append-only de eventos aceitos, snapshots verificáveis, idempotência e replay sob testes de falha. Não aplicar réplicas paralelas apontando para o mesmo instrumento.

## Riscos conhecidos

- O processo não possui proteção contra tráfego não confiável ou abuso de contas.
- Não há limites por conta para número de ordens em aberto nem proteção robusta contra saturação.
- Estruturas de árvore, futures e respostas provocam pressão de GC; ring buffer não significa 'zero GC'.
- Métricas oferecem visão operacional básica e não substituem tracing ou métricas de cauda da API.
- Prioridades de desempenho devem partir de profiler e medição reproduzível, e não de flags aleatórias da JVM.
