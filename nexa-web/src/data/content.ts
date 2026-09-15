export const NAV_LINKS = [
  { label: "Experiência", href: "#experience" },
  { label: "Pagamentos", href: "#payments" },
  { label: "Segurança", href: "#security" },
  { label: "Global", href: "#global" },
  { label: "Tecnologia", href: "#technology" },
];

export const FINANCIAL_PILLARS = [
  {
    tag: "01 / CAPACIDADE",
    title: "PAY",
    subtitle: "Liquidação instantânea em sub-segundos",
    description:
      "Transações com validação atômica dupla. Sem intermediários opacos, com visibilidade e rastreabilidade criptográfica direta.",
    metric: "0.014s",
    metricLabel: "Latência média de liquidação",
    badge: "MOTOR ATÔMICO",
  },
  {
    tag: "02 / PRESERVAÇÃO",
    title: "SAVE",
    subtitle: "Rendimento institucional automatizado",
    description:
      "Seu capital alocado com proteção soberana e liquidez diária. Ganhos contínuos creditados a cada ciclo de bloco contábil.",
    metric: "108.4%",
    metricLabel: "Do CDI com liquidez imediata",
    badge: "PROTEÇÃO PATRIMONIAL",
  },
  {
    tag: "03 / MULTIPLICAÇÃO",
    title: "INVEST",
    subtitle: "Acesso a classes de ativos globais",
    description:
      "Títulos soberanos, moedas fortes e infraestrutura privada em um único portfólio inteligente com rebalanceamento algorítmico.",
    metric: "$ 4.2B+",
    metricLabel: "Volume transitado e verificado",
    badge: "PORTFÓLIO INTELIGENTE",
  },
];

export const TRANSACTION_FLOW_STEPS = [
  { step: "01", label: "Iniciação do Pagamento", status: "Autenticado via Biometria HSM", time: "0.001s" },
  { step: "02", label: "Verificação de Risco ML", status: "Score de Anomalia 0.0002% (Aprovado)", time: "0.004s" },
  { step: "03", label: "Lock Pessimista no Ledger", status: "Transação ACID Double-Entry", time: "0.008s" },
  { step: "04", label: "Liquidação & Notificação", status: "Crédito Concluído Irreversível", time: "0.012s" },
];

export const CURRENCIES = [
  { code: "USD", name: "Dólar Americano", symbol: "$", rate: "1.0000", change: "+0.14%" },
  { code: "EUR", name: "Euro", symbol: "€", rate: "0.9180", change: "-0.05%" },
  { code: "GBP", name: "Libra Esterlina", symbol: "£", rate: "0.7845", change: "+0.22%" },
  { code: "BRL", name: "Real Brasileiro", symbol: "R$", rate: "5.4210", change: "+0.45%" },
  { code: "JPY", name: "Iene Japonês", symbol: "¥", rate: "148.90", change: "-0.31%" },
  { code: "CHF", name: "Franco Suíço", symbol: "CHF", rate: "0.8520", change: "+0.08%" },
];

export const TECH_SPECS = [
  {
    category: "Ledger Core",
    title: "Double-Entry Imutável",
    detail: "Toda movimentação gera lançamentos de Débito e Crédito balanceados a zero absoluto, garantindo integridade matemática auditável.",
  },
  {
    category: "Concorrência",
    title: "Pessimistic Locking & ACID",
    detail: "Eliminação total de race conditions e double-spending sob cargas extremas de concorrência com bloqueio a nível de linha no banco de dados.",
  },
  {
    category: "Infraestrutura",
    title: "Java 17 LTS / Spring Boot 3",
    detail: "Alta taxa de transferência (throughput) e baixa latência rodando em runtime JVM otimizada para o setor financeiro global.",
  },
  {
    category: "Interface",
    title: "Next.js + WebGL Criativo",
    detail: "Renderização 3D de precisão com framerate desacoplado e amortecimento inercial, unindo estética de galeria e fluidez de produto.",
  },
];
