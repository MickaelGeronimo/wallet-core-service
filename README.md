# ⚡ NEXA — Core Banking Ledger & Real-Time Payment Platform

<p align="center">
  <img src="https://img.shields.io/badge/Java-17%20LTS-orange?style=for-the-badge&logo=openjdk" />
  <img src="https://img.shields.io/badge/Spring%20Boot-3.3.4-brightgreen?style=for-the-badge&logo=springboot" />
  <img src="https://img.shields.io/badge/Next.js-14.2-black?style=for-the-badge&logo=next.js" />
  <img src="https://img.shields.io/badge/Tests-6%2F6%20Passing%20(100%25)-success?style=for-the-badge" />
  <img src="https://img.shields.io/badge/Resilience4j-Circuit%20Breaker-blue?style=for-the-badge" />
  <img src="https://img.shields.io/badge/Docker-Postgres%20%7C%20Redis%20%7C%20Kafka-2496ED?style=for-the-badge&logo=docker" />
</p>

---

## 🔷 Visão Geral do Projeto

O **NEXA** é uma plataforma financeira corporativa de alta precisão que combina a engenharia contábil de missão crítica dos maiores bancos digitais e fintechs do mundo (**Itaú, Stripe, Nubank e Revolut**) com uma experiência web cinematográfica de padrão internacional (**Awwwards / Revolut-level**).

Diferente de sistemas acadêmicos ou CRUDs genéricos (que apenas subtraem colunas em banco), o NEXA implementa:
- **Livro-Razão Contábil Imutável (Double-Entry General Ledger)** balanceado a zero absoluto;
- **Controle Estrito de Concorrência & Idempotência** via Lock Pessimista em nível de linha (`PESSIMISTIC_WRITE`);
- **Transactional Outbox Pattern** para desacoplamento assíncrono seguro, eliminando o problema do *Dual-Write*;
- **Pipeline Antifraude (AML / Risk Screening)** em *Chain of Responsibility*;
- **Resiliência Bancária com Resilience4j** (Circuit Breaker e Rate Limiting);
- **Frontend Revolut-Style** em Next.js 14, React 18, Three.js, GSAP e rolagem suave Lenis.

---

## 📊 Comparativo Técnico: Por que o NEXA supera backends legados (ex: `fab-backend`)

| Dimensão | O `fab-backend` (Legado) | **NEXA Core Platform (Moderno)** |
| :--- | :--- | :--- |
| **Java & Runtime** | Java 8 (legado) + Spring Boot 1/2 | **Java 17 LTS (Records, Pattern Matching) + Spring Boot 3.3.4** |
| **Modelo Contábil** | `balance = balance - X` (vulnerável a inconsistências e saldo negativo) | **Double-Entry Bookkeeping**: partidas dobradas imutáveis com débito e crédito balanceados a zero absoluto |
| **Concorrência** | Sem controle (permite gastar o mesmo saldo várias vezes) | **Locking Pessimista (`PESSIMISTIC_WRITE`) & Idempotência Distribuída**: proteção contra race conditions e replay |
| **Arquitetura** | Monolito síncrono acoplado | **Arquitetura Limpa / DDD** + Fases de Saga com isolamento de transação |
| **Mensageria & Eventos** | Nenhuma (tudo síncrono no banco) | **Transactional Outbox Pattern**: publicação atômica com worker assíncrono e DLQ (Dead Letter Queue) |
| **Resiliência** | Nenhuma (falhas quebram o fluxo) | **Resilience4j**: Circuit Breaker, Rate Limiter e Retries exponenciais |
| **Motor de Risco** | Nenhum | **Risk Screening Chain (Antifraude)**: Sanções, Velocidade de rajada e Limite Regulatório |
| **Interface / Frontend** | Angular legado básico | **Next.js 14 + Three.js + GSAP + Lenis**: estética idêntica ao app do Revolut |
| **Infra & DevOps** | Nenhuma | **Docker Compose corporativo** (Postgres 16, Redis 7, Kafka KRaft, PgAdmin) |

---

## 🏛 Arquitetura do Pipeline de Pagamentos

```
                  ┌────────────────────────────────────────────────────────────────────────┐
                  │                    NEXA Real-Time Payment Pipeline                     │
                  └────────────────────────────────────────────────────────────────────────┘
    1. Idempotência          2. Antifraude / AML       3. Deadlock-Free Lock       4. Double-Entry Hold      5. Transactional Outbox
    Key Verification         Chain of Responsibility   (Ascending Account IDs)     (Debits == Credits)       (Async Event Dispatch)
   ┌────────────────┐       ┌──────────────────────┐   ┌───────────────────────┐   ┌─────────────────────┐   ┌───────────────────────┐
   │  Verifica se   │──────▶│  • Sanctions Rule    │──▶│  Lock Pessimista no   │──▶│  • Débito Conta A   │──▶│  Persiste evento em   │
   │  chave já foi  │       │  • Velocity Limit    │   │  DB na ordem crescente│   │  • Crédito Conta B  │   │  outbox_events na     │
   │  processada    │       │  • High-Value Teto   │   │  de ID (Zero Deadlock)│   │  • Invariante = 0   │   │  mesma transação ACID │
   └────────────────┘       └──────────────────────┘   └───────────────────────┘   └─────────────────────┘   └───────────────────────┘
                                                                                                                         │
                                                                                                                         ▼
                                                                                                             ┌───────────────────────┐
                                                                                                             │  OutboxRelayWorker    │
                                                                                                             │  (Polling assíncrono  │
                                                                                                             │   com retry e DLQ)    │
                                                                                                             └───────────────────────┘
```

---

## ⚙️ Os 6 Pilares de Engenharia do Backend

### 1. Livro-Razão Contábil Imutável (Double-Entry General Ledger)
O saldo de uma conta não é atualizado por um comando ingênuo de subtração. Cada movimentação gera obrigatoriamente um par de lançamentos atômicos no [`LedgerEntry`](nexa-core-backend/src/main/java/com/apex/wallet/domain/model/LedgerEntry.java):
$$\sum \text{Débitos} = \sum \text{Créditos}$$
- **Débito:** decrementa a conta devedora;
- **Crédito:** incrementa a conta credora;
- A soma matemática das partidas dobradas fecha sempre em zero absoluto.

### 2. Lock Pessimista & Prevenção de Deadlocks
Para anular race conditions em ambientes com dezenas de transações por segundo competindo pelo mesmo saldo:
- A busca pelas contas adquire lock exclusivo no banco de dados via `PESSIMISTIC_WRITE`:
  ```java
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT a FROM Account a WHERE a.id = :id")
  Optional<Account> findByIdWithLock(@Param("id") Long id);
  ```
- **Estratégia Anti-Deadlock:** As contas são sempre bloqueadas na ordem crescente de ID (`Math.min(idA, idB)` depois `Math.max(idA, idB)`), impossibilitando deadlocks cruzados entre duas pessoas transferindo uma para a outra simultaneamente.

### 3. Idempotência Distribuída
Toda requisição carrega uma chave única de idempotência (`Idempotency-Key`). Se a rede oscilar e o app móvel reenviar a mesma requisição 10 vezes, a transação original já comutada é retornada imediatamente sem debitar o saldo uma segunda vez.

### 4. Transactional Outbox Pattern (Zero Dual-Write)
Chamar um broker de mensageria (Kafka, RabbitMQ) diretamente no meio da transação JDBC é uma má prática que gera inconsistência caso o broker falhe e o banco reverta (ou vice-versa).
- O NEXA grava o [`OutboxEvent`](nexa-core-backend/src/main/java/com/apex/wallet/domain/outbox/OutboxEvent.java) na mesma transação ACID do débito contábil.
- O [`OutboxRelayWorker`](nexa-core-backend/src/main/java/com/apex/wallet/infrastructure/outbox/OutboxRelayWorker.java) processa a fila assincronamente e envia os eventos para o barramento, isolando o tempo de resposta do cliente da latência da rede externa.

### 5. Motor de Avaliação de Risco & Antifraude (AML)
Antes de tocar no livro-razão, a transação atravessa uma esteira orientada pelo padrão *Chain of Responsibility*:
- **`SanctionsCheckRule`:** Bloqueia destinatários catalogados em listas restritivas internacionais e de fraude;
- **`VelocityLimitRule`:** Impede rajadas anômalas (limite de 5 operações por minuto);
- **`HighValueThresholdRule`:** Rejeita operações instantâneas acima de R$ 100.000,00 e audita quantias elevadas.

### 6. Resiliência com Resilience4j
Configurado via Spring Cloud / Spring Boot 3:
- **Circuit Breaker:** Protege o serviço caso dependências externas fiquem instáveis;
- **Rate Limiter:** Garante vazão estável e proteção contra ataques DoS;
- **Ignore Exceptions:** Calibrado para que rejeições de negócio esperadas (`RiskRejectedException`) não abram o circuito indevidamente.

---

## 🧪 Suíte de Testes Automatizados (100% Aprovados)

O projeto conta com validação automatizada de concorrência e integridade contábil:

```bash
mvn test
```

```text
[INFO] Running com.apex.wallet.ApexWalletApplicationTests
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 20.87 s
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```

### Casos de Teste Cobertos:
1. **`testIdempotencyProtection`:** Comprova que chamadas duplicadas com a mesma chave não duplicam débitos;
2. **`testConcurrentTransfersNoDoubleSpending`:** 5 threads paralelas disparadas via `CountDownLatch` contra a mesma conta terminam com saldo final matematicamente perfeito;
3. **`testAntifraudSanctionsBlock`:** Valida que chaves em sanção são imediatamente bloqueadas com `RiskRejectedException`;
4. **`testAntifraudHighValueThreshold`:** Rejeita transações acima do limite regulatório;
5. **`testTransactionalOutboxEventCreated`:** Garante que o evento de mensageria foi gerado na mesma transação;
6. **`contextLoads`:** Valida o bootstrap das entidades e personas de demonstração.

---

## 🌐 Endpoints da API & Documentação

Ao iniciar a aplicação, os seguintes serviços ficam disponíveis:

| Método | Rota | Descrição |
| :--- | :--- | :--- |
| **GET** | `/` | Metadados do sistema, status UP e lista de rotas |
| **GET** | `/swagger-ui/index.html` | Interface interativa OpenAPI 3 / Swagger |
| **GET** | `/h2-console` | Console do banco de dados em memória |
| **GET** | `/actuator/health` | Status de saúde dos componentes e Circuit Breaker |
| **POST** | `/api/auth/login` | Autenticação JWT para contas demo |
| **GET** | `/api/auth/personas` | Lista de contas pré-carregadas para teste |
| **GET** | `/api/audit/balance` | Auditoria matemática da invariante contábil em tempo real |
| **GET** | `/api/audit/ledger-entries`| Extrato completo e imutável de todas as partidas dobradas |
| **POST** | `/api/transfer/pix` | Executa transferência instantânea protegida pelo Ledger |
| **GET** | `/api/wallet/pix-lookup` | Consulta pública de chaves PIX |

---

## 🚀 Como Executar

### 1. Pré-requisitos
- **Java 17 LTS+** e **Maven 3.9+**
- **Node.js 18+** e **npm**
- **Docker & Docker Compose** (opcional, para ambiente corporativo)

---

### 2. Executando o Backend (`nexa-core-backend`)
```bash
cd nexa-core-backend
mvn spring-boot:run
```
> O backend estará acessível em **http://localhost:8080**  
> Documentação Swagger: **http://localhost:8080/swagger-ui/index.html**

---

### 3. Executando o Frontend (`nexa-web`)
```bash
cd nexa-web
npm install
npm run dev
```
> O frontend estará acessível em **http://localhost:3000**

---

### 4. Executando Toda a Infraestrutura com Docker Compose
Para subir a infraestrutura completa de produção (Postgres 16, Redis 7, Kafka KRaft e PgAdmin):
```bash
docker compose up -d
```

| Serviço | Porta | Credenciais |
| :--- | :--- | :--- |
| **PostgreSQL 16** | `5432` | User: `nexa_admin` / Pass: `nexa_secure_password_2026` / DB: `nexawallet` |
| **Redis 7** | `6379` | Sem senha |
| **Apache Kafka (KRaft)** | `9092` | Modo KRaft nativo |
| **PgAdmin 4** | `5050` | User: `admin@nexa.com` / Pass: `admin` |

---

## 📄 Licença
Distribuído sob a licença **MIT**. Consulte `LICENSE` para mais detalhes.
