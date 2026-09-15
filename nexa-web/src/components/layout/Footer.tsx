"use client";

import { TECH_SPECS } from "@/data/content";

export default function Footer() {
  return (
    <footer className="relative bg-[#050507] border-t border-white/[0.08] pt-24 pb-16 overflow-hidden">
      {/* Background Watermark */}
      <div className="absolute -bottom-10 left-1/2 -translate-x-1/2 select-none pointer-events-none opacity-[0.03] text-[28vw] font-black tracking-tighter leading-none text-white whitespace-nowrap">
        NEXA
      </div>

      <div className="max-w-7xl mx-auto px-6 md:px-12 relative z-10">
        <div className="grid grid-cols-1 md:grid-cols-4 gap-12 pb-16 border-b border-white/[0.06]">
          {/* Col 1: Brand info */}
          <div className="md:col-span-1 space-y-4">
            <div className="flex items-center gap-3">
              <div className="w-8 h-8 rounded-lg bg-white/10 border border-white/20 flex items-center justify-center font-mono font-bold text-sm text-white">
                N
              </div>
              <span className="font-mono text-sm tracking-widest font-semibold uppercase text-white">
                NEXA
              </span>
            </div>
            <p className="text-xs text-white/50 leading-relaxed max-w-xs">
              Infraestrutura de ponta para movimentação financeira global. Double-entry ledger auditável em tempo real.
            </p>
            <div className="pt-2 text-[11px] font-mono text-white/30">
              © {new Date().getFullYear()} NEXA Core Technologies.
            </div>
          </div>

          {/* Col 2: Arquitetura */}
          <div className="space-y-3">
            <h4 className="text-xs font-mono tracking-widest uppercase text-white/40">Arquitetura</h4>
            <ul className="space-y-2 text-xs text-white/60">
              <li>Double-Entry Ledger</li>
              <li>Pessimistic Concurrency</li>
              <li>Spring Boot Core Engine</li>
              <li>Distributed Kafka Bus</li>
              <li>HSM Key Orchestration</li>
            </ul>
          </div>

          {/* Col 3: Soluções */}
          <div className="space-y-3">
            <h4 className="text-xs font-mono tracking-widest uppercase text-white/40">Soluções</h4>
            <ul className="space-y-2 text-xs text-white/60">
              <li>NEXA Obsidian Card</li>
              <li>Instant Foreign Exchange</li>
              <li>Yield Automático</li>
              <li>API de Liquidação Direta</li>
              <li>Risk & Fraud Detection</li>
            </ul>
          </div>

          {/* Col 4: Status do Sistema */}
          <div className="space-y-3">
            <h4 className="text-xs font-mono tracking-widest uppercase text-white/40">System Heartbeat</h4>
            <div className="p-4 rounded-xl bg-white/[0.02] border border-white/[0.06] space-y-2 font-mono text-[11px]">
              <div className="flex justify-between items-center text-white/70">
                <span>Ledger Service:</span>
                <span className="text-emerald-400 flex items-center gap-1.5">
                  <span className="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-pulse" />
                  ONLINE
                </span>
              </div>
              <div className="flex justify-between items-center text-white/70">
                <span>Latency P99:</span>
                <span className="text-white">12.4ms</span>
              </div>
              <div className="flex justify-between items-center text-white/70">
                <span>Consensus:</span>
                <span className="text-white">SYNCHRONIZED</span>
              </div>
            </div>
          </div>
        </div>

        <div className="pt-8 flex flex-col md:flex-row items-center justify-between gap-4 text-xs font-mono text-white/30">
          <div>
            Regulado conforme os padrões de infraestrutura financeira internacional e compliance PCI-DSS Tier 1.
          </div>
          <div className="flex gap-6">
            <a href="#" className="hover:text-white transition-colors">Privacidade</a>
            <a href="#" className="hover:text-white transition-colors">Termos</a>
            <a href="#" className="hover:text-white transition-colors">Segurança</a>
          </div>
        </div>
      </div>
    </footer>
  );
}
