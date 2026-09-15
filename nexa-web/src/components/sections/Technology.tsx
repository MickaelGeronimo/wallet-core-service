"use client";

import React from "react";
import { TECH_SPECS } from "@/data/content";
import { Terminal, Database, Server, Cpu } from "lucide-react";

export default function Technology() {
  return (
    <section
      id="technology"
      className="relative py-32 px-6 md:px-12 bg-[#050507] border-t border-white/[0.06] overflow-hidden"
    >
      <div className="max-w-7xl mx-auto">
        <div className="flex flex-col md:flex-row md:items-end justify-between mb-20 gap-8">
          <div>
            <span className="text-xs font-mono tracking-widest text-white/40 uppercase block mb-3">
              06 / NÚCLEO DISTRIBUÍDO
            </span>
            <h2 className="text-4xl md:text-6xl font-bold tracking-tight text-white leading-tight">
              Engenharia Bancária. <br />
              <span className="text-gradient-silver">Java 17 LTS & Spring Core.</span>
            </h2>
          </div>
          <p className="text-sm md:text-base text-white/50 max-w-md font-light leading-relaxed">
            Não usamos bancos de dados de brinquedo. Nosso motor utiliza isolamento estrito e locks transacionais pessimistas garantindo consistência matemática irrefutável.
          </p>
        </div>

        {/* 4 Tech Specs Grid */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-12">
          {TECH_SPECS.map((tech, idx) => (
            <div
              key={tech.title}
              className="p-8 rounded-3xl bg-[#0b0b10] border border-white/[0.06] flex flex-col justify-between hover:border-white/20 transition-all"
            >
              <div>
                <span className="text-[10px] font-mono tracking-widest text-emerald-400 uppercase block mb-4">
                  {tech.category}
                </span>
                <h3 className="text-lg font-bold text-white mb-3">{tech.title}</h3>
                <p className="text-xs md:text-sm text-white/50 font-light leading-relaxed">
                  {tech.detail}
                </p>
              </div>

              <div className="pt-6 mt-6 border-t border-white/[0.04] text-[10px] font-mono text-white/30">
                PRODUÇÃO VALIDADA
              </div>
            </div>
          ))}
        </div>

        {/* Java Spring Boot Code Showcase Console */}
        <div className="rounded-3xl bg-[#09090d] border border-white/10 p-6 md:p-8 font-mono text-xs overflow-x-auto shadow-2xl">
          <div className="flex items-center justify-between pb-4 mb-4 border-b border-white/[0.06] text-white/40">
            <div className="flex items-center gap-2">
              <Terminal className="w-4 h-4 text-emerald-400" />
              <span>NexaLedgerService.java (Double-Entry Engine)</span>
            </div>
            <div className="text-[10px]">SPRING BOOT 3.3.4 • JAVA 17</div>
          </div>

          <pre className="text-white/80 leading-relaxed overflow-x-auto">
            <code>
{`@Service
@Transactional(isolation = Isolation.SERIALIZABLE)
public class NexaLedgerService {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    public TransactionReceipt executeTransfer(TransferRequest req) {
        Account source = accountRepo.findByIdWithLock(req.getSourceAccountId());
        Account target = accountRepo.findByIdWithLock(req.getTargetAccountId());

        // Validação de saldo e integridade estrita
        source.validateSufficientFunds(req.getAmount());

        // Lançamento de Dupla Entrada: Débito e Crédito balanceados
        LedgerEntry debit = new LedgerEntry(source, req.getAmount().negate(), EntryType.DEBIT);
        LedgerEntry credit = new LedgerEntry(target, req.getAmount(), EntryType.CREDIT);

        ledgerRepo.saveAll(List.of(debit, credit));
        return new TransactionReceipt(UUID.randomUUID(), req.getAmount(), Status.COMMITTED);
    }
}`}
            </code>
          </pre>
        </div>
      </div>
    </section>
  );
}
