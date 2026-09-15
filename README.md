# Digital Wallet Engine — Core Ledger & Payment Processing Service

> Serviço de carteira digital e processamento de transferências em tempo real com livro-razão contábil (double-entry ledger), controle estrito de concorrência, pipeline de avaliação de risco e degradação graciosa sob alta carga.

[![Java 17](https://img.shields.io/badge/Java-17%20LTS-orange.svg)](https://openjdk.org/)
[![Spring Boot 3.3.4](https://img.shields.io/badge/Spring%20Boot-3.3.4-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Build Status](https://img.shields.io/badge/Tests-11%2F11%20Passed%20(100%25)-success.svg)]()
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Docker](https://img.shields.io/badge/Docker-PostgreSQL%20%7C%20Redis%20%7C%20Kafka-2496ED.svg)](docker-compose.yml)

---

## Sumário

- [Visão Geral](#visão-geral)
- [Arquitetura](#arquitetura)
- [Características Técnicas](#características-técnicas)
- [Degradação Graciosa & Resiliência](#degradação-graciosa--resiliência)
- [Stack Tecnológica](#stack-tecnológica)
- [Como Executar](#como-executar)
- [Endpoints da API](#endpoints-da-api)
- [Testes Automatizados](#testes-automatizados)
- [Licença](#licença)

---

## Visão Geral

Este projeto implementa o núcleo transacional de uma carteira digital (Digital Wallet) focada em consistência de dados, prevenção contra gasto duplo e resiliência operacional durante picos de tráfego.

Diferente de implementações simples baseadas apenas em atualização de saldo em tabela única, este serviço adota:
- **Partidas Dobradas (Double-Entry Bookkeeping):** Toda movimentação gera lançamentos imutáveis e auditáveis de débito e crédito balanceados a zero.
- **Isolamento de Concorrência:** Locks pessimistas (`PESSIMISTIC_WRITE`) com ordenação determinística de identificadores para evitar deadlocks e condições de corrida (*race conditions*).
- **Idempotência Distribuída:** Garantia de que requisições repetidas com a mesma chave (`Idempotency-Key`) retornem o resultado original sem duplicar débitos.
- **Transactional Outbox Pattern:** Publicação atômica de eventos no mesmo commit do banco, eliminando o problema de escrita dupla (*dual-write*).
- **Degradação Graciosa (Graceful Degradation):** Sobrecarga de banco de dados e exaustão de pool não geram erro 500 para o cliente; as ordens são retidas em buffer assíncrono retornando `HTTP 202 Accepted` e liquidadas automaticamente assim que a saúde do sistema é restabelecida.

---

## Arquitetura

```
                           Fluxo de Processamento de Transferência
┌──────────────────┐
│  Cliente / API   │
└────────┬─────────┘
         │ POST /api/transfer/pix (Header: Idempotency-Key)
         ▼
┌──────────────────┐      Sim (Já existe)      ┌─────────────────────────────┐
│ 1. Idempotência  ├──────────────────────────▶│ Retorna transação original  │
└────────┬─────────┘                           └─────────────────────────────┘
         │ Não
         ▼
┌──────────────────┐      Reprovado             ┌─────────────────────────────┐
│ 2. Motor Risco   ├──────────────────────────▶│ HTTP 422 (RiskRejected)     │
└────────┬─────────┘                           └─────────────────────────────┘
         │ Aprovado
         ▼
┌──────────────────┐
│ 3. Lock Ordenado │  Lock exclusivo (SELECT ... FOR UPDATE) na ordem ascendente de ID
└────────┬─────────┘  (Evita Deadlocks entre contas simultâneas)
         │
         ▼
┌──────────────────┐
│ 4. Débito/Crédito│  Valida saldo, subtrai de A, soma em B
│    & Ledger      │  Grava LedgerEntry (Débito A, Crédito B)
└────────┬─────────┘
         │
         ▼
┌──────────────────┐
│ 5. Outbox Event  │  Persiste OutboxEvent ("PAYMENT_SETTLED") na mesma transação ACID
└────────┬─────────┘
         │
         ▼
┌──────────────────┐
│ 6. Relay Worker  │  Worker assíncrono despacha eventos para mensageria / DLQ
└──────────────────┘
```

---

## Características Técnicas

### 1. Livro-Razão Contábil (Double-Entry Ledger)
Cada transferência atômica registra dois lançamentos complementares na tabela de auditoria contábil:
- **Débito:** Decremento no saldo do pagador.
- **Crédito:** Incremento no saldo do recebedor.
A soma de todos os débitos e créditos de uma transação é sempre rigorosamente nula.

### 2. Controle de Concorrência Anti-Deadlock
Para evitar bloqueios mútuos (*deadlocks*) quando dois usuários transferem valores um para o outro simultaneamente:
- O sistema calcula a ordem dos IDs: `firstId = Math.min(idA, idB)` e `secondId = Math.max(idA, idB)`.
- Os locks pessimistas (`SELECT ... FOR UPDATE`) são sempre adquiridos nessa sequência.

### 3. Pipeline de Avaliação de Risco (Antifraude / AML)
Executado antes de qualquer alteração de estado no banco de dados:
- **SanctionsCheckRule:** Bloqueia chaves presentes em listas restritivas e de monitoramento.
- **VelocityLimitRule:** Limita rajadas de transações repetitivas (máximo de 5 transações por minuto por conta).
- **HighValueThresholdRule:** Aplica teto regulatório de transações instantâneas (rejeição de valores acima de R$ 100.000,00).

### 4. Transactional Outbox Pattern
Ao invés de tentar publicar mensagens diretamente para o broker durante a transação relacional (o que causa inconsistência em caso de falha de rede):
1. O evento é salvo na tabela `outbox_events` no mesmo commit do saldo.
2. Um worker em background (`OutboxRelayWorker`) lê os eventos pendentes e os despacha de forma assíncrona, com suporte a retentativas e Dead Letter Queue (DLQ).

---

## Degradação Graciosa & Resiliência

Durante picos extremos de acessos (ex: datas comemorativas, congestionamento de rede ou pool de conexões saturado):

1. **Leituras Instantâneas de Saldo (< 0.2ms):**
   - O `BalanceCacheService` mantém cópia em memória do saldo das contas sincronizada a cada commit.
   - Caso o banco relacional sofra degradação de I/O, as consultas de saldo continuam respondendo instantaneamente com o cabeçalho `X-Degraded-Mode: true`.

2. **Escritas Retidas em Buffer (`HTTP 202 Accepted`):**
   - Sob falhas transitórias de conexão com o banco de dados, o Circuit Breaker (`Resilience4j`) redireciona a requisição para o `BufferedTransferQueueService`.
   - O cliente recebe confirmação de recebimento com status `QUEUED_FOR_SETTLEMENT` (HTTP 202).
   - O `QueueDrainWorker` liquida as operações pendentes em lotes atômicos assim que o banco restabelece operação normal.

3. **Controlador de Caos para Testes:**
   - Endpoints `/api/chaos/**` permitem injetar latência de banco e simular indisponibilidade sob demanda.

---

## Stack Tecnológica

| Componente | Tecnologia |
| :--- | :--- |
| **Linguagem** | Java 17 LTS (Records, Pattern Matching, Sealed Types) |
| **Framework** | Spring Boot 3.3.4 (Spring Data JPA, Spring Security, Validation) |
| **Tolerância a Falhas** | Resilience4j 2.2 (CircuitBreaker, RateLimiter) |
| **Documentação API** | SpringDoc OpenAPI 3.0 / Swagger UI |
| **Segurança** | Autenticação Stateless via JWT (JJWT 0.12.6) |
| **Banco de Dados** | H2 (Memória para desenvolvimento/testes) / PostgreSQL 16 (Produção) |
| **Mensageria & Cache** | Apache Kafka (KRaft mode) e Redis 7 |

---

## Como Executar

### Pré-requisitos
- **JDK 17 LTS**
- **Maven 3.9+**
- **Docker & Docker Compose** (opcional, para ambiente de containers)

### 1. Executando o Serviço
```bash
mvn spring-boot:run
```
- API Base: `http://localhost:8080`
- Documentação Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- Console H2: `http://localhost:8080/h2-console`
- Actuator Health: `http://localhost:8080/actuator/health`

### 2. Executando via Docker Compose
```bash
docker compose up -d
```

---

## Endpoints da API

| Método | Rota | Descrição |
| :--- | :--- | :--- |
| `POST` | `/api/auth/login` | Autenticação JWT e emissão de token |
| `GET` | `/api/auth/personas` | Lista de contas pré-carregadas para teste |
| `GET` | `/api/wallet/me` | Dados da conta autenticada (com fallback de cache) |
| `GET` | `/api/wallet/cached-balance` | Leitura direta do saldo em cache de alta velocidade |
| `GET` | `/api/wallet/pix-lookup?key={pixKey}` | Consulta de conta por chave Pix |
| `POST` | `/api/wallet/deposit` | Depósito / recarga de saldo |
| `POST` | `/api/transfer/pix` | Transferência instantânea idempotente com ledger |
| `GET` | `/api/audit/balance` | Auditoria matemática da integridade contábil |
| `GET` | `/api/audit/ledger-entries` | Extrato detalhado de partidas dobradas |
| `GET` | `/api/chaos/status` | Monitoramento do estado operacional e filas |
| `POST` | `/api/chaos/db-latency?delayMs=1000` | Injeção de latência artificial no banco |
| `POST` | `/api/chaos/force-failure?fail=true` | Simulação de indisponibilidade de banco |
| `POST` | `/api/chaos/reset` | Restauração dos parâmetros normais |
| `POST` | `/api/chaos/drain-queue` | Drenagem manual das transações pendentes |

---

## Testes Automatizados

A suíte de testes valida a integridade contábil, concorrência, regras de risco e degradação graciosa:

```bash
mvn test
```

### Resultados da Execução:
```text
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0 -- in com.apex.wallet.ApexWalletApplicationTests
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0 -- in com.apex.wallet.GracefulDegradationStressTest
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] Tests run: 11, Failures: 0, Errors: 0, Skipped: 0
[INFO] ------------------------------------------------------------------------
```

### Casos Cobertos:
1. `contextLoads`: Valida inicialização do contexto Spring e bootstrap de dados.
2. `testIdempotencyProtection`: Valida que reenvio da mesma chave não duplica débitos.
3. `testConcurrentTransfersNoDoubleSpending`: Valida consistência exata sob concorrência paralela.
4. `testAntifraudSanctionsBlock`: Bloqueia chaves sancionadas na esteira de risco.
5. `testAntifraudHighValueThreshold`: Rejeita transferências acima do teto regulatório.
6. `testTransactionalOutboxEventCreated`: Verifica persistência atômica do evento de outbox.
7. `testHealthyTransferExecution`: Execução atômica direta em condições normais (`COMPLETED`).
8. `testGracefulDegradationUnderDatabaseOutage`: Absorção de transferências para buffer durante falha de banco (`QUEUED_FOR_SETTLEMENT`).
9. `testEventualConsistencyAndAutoHealing`: Auto-cura via `QueueDrainWorker` ao restabelecer o banco.
10. `testHighSpeedBalanceCacheLookups`: Leitura de saldo via cache em memória com banco offline.
11. `testExtremeConcurrencySpikeWithGracefulDegradation`: Rajada concorrente com 100% de atendimento e liquidação final íntegra.

---

## Licença

Distribuído sob a licença [MIT](LICENSE).
