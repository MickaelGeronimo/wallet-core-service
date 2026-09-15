"use client";

import React, { useState } from "react";
import { CheckCircle2, ArrowRight, Loader2, Sparkles, CreditCard, ShieldCheck } from "lucide-react";
import MagneticButton from "@/components/ui/MagneticButton";

export default function PaymentExperience() {
  const [isProcessing, setIsProcessing] = useState(false);
  const [isCompleted, setIsCompleted] = useState(false);
  const [transactionLog, setTransactionLog] = useState<string[]>([
    "ID: #TX-9842A1 • STATUS: INITIALIZED",
    "DEBIT: ACC-772910 (-R$ 1.280,00)",
    "CREDIT: MERCHANT-4412 (+R$ 1.280,00)",
    "SUM BALANCED = R$ 0,00 (ZERO DRIFT)",
  ]);

  const handleExecuteTransaction = () => {
    if (isProcessing) return;
    setIsProcessing(true);
    setIsCompleted(false);

    setTimeout(() => {
      setTransactionLog((prev) => [
        `ID: #TX-${Math.floor(100000 + Math.random() * 900000)} • TIME: ${new Date().toLocaleTimeString()}`,
        "DOUBLE-ENTRY LOCK ACQUIRED (PESSIMISTIC_WRITE)",
        "DEBIT: ACC-772910 (-R$ 450,00)",
        "CREDIT: MERCHANT-8831 (+R$ 450,00)",
        "STATUS: COMMITTED ACID • 0.011s",
      ]);
      setIsProcessing(false);
      setIsCompleted(true);
    }, 900);
  };

  return (
    <section
      id="payments"
      className="relative py-32 px-6 md:px-12 bg-[#08080a] bg-grid-tech border-t border-white/[0.06] overflow-hidden"
    >
      <div className="max-w-7xl mx-auto">
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-12 items-center">
          {/* Left Text Column */}
          <div className="lg:col-span-6 space-y-6">
            <span className="text-xs font-mono tracking-widest text-white/40 uppercase block">
              03 / FLUXO DE LIQUIDAÇÃO
            </span>
            <h2 className="text-4xl md:text-5xl lg:text-6xl font-bold tracking-tight text-white leading-tight">
              Cada centavo. <br />
              <span className="text-gradient-silver">Auditado em milissegundos.</span>
            </h2>
            <p className="text-sm md:text-base text-white/60 font-light leading-relaxed max-w-lg">
              Nosso motor Double-Entry assegura que toda operação gere débitos e créditos estritamente balanceados.
              Sem reconciliações tardias de fim de dia. O estado contábil é verdadeiro agora.
            </p>

            <div className="pt-4 flex items-center gap-4">
              <MagneticButton
                onClick={handleExecuteTransaction}
                disabled={isProcessing}
                className="px-6 py-3.5 rounded-full bg-white text-black font-mono text-xs uppercase tracking-wider font-semibold hover:bg-white/90 transition-all disabled:opacity-50"
              >
                {isProcessing ? (
                  <>
                    <Loader2 className="w-4 h-4 animate-spin mr-2" />
                    Validando Ledger...
                  </>
                ) : (
                  <>
                    <Sparkles className="w-4 h-4 mr-2 text-amber-600" />
                    Simular Transação Real
                  </>
                )}
              </MagneticButton>
            </div>
          </div>

          {/* Right Interface Console */}
          <div className="lg:col-span-6">
            <div className="rounded-3xl bg-[#0c0c12]/90 border border-white/10 p-6 md:p-8 backdrop-blur-2xl shadow-2xl space-y-6">
              {/* Payment Flow Stepper Header */}
              <div className="flex items-center justify-between border-b border-white/[0.08] pb-5">
                <div className="flex items-center gap-3">
                  <div className="w-9 h-9 rounded-xl bg-white/[0.06] border border-white/10 flex items-center justify-center">
                    <CreditCard className="w-4 h-4 text-white" />
                  </div>
                  <div>
                    <h4 className="text-sm font-semibold text-white">Wallet Pay Direct</h4>
                    <span className="text-[11px] font-mono text-white/40">Protocolo v2.4</span>
                  </div>
                </div>

                <div className="flex items-center gap-2">
                  <ShieldCheck className="w-4 h-4 text-emerald-400" />
                  <span className="text-xs font-mono text-emerald-400">Zero Drift</span>
                </div>
              </div>

              {/* Transaction Amount Box */}
              <div className="p-5 rounded-2xl bg-white/[0.02] border border-white/[0.06] flex items-center justify-between">
                <div>
                  <span className="text-[10px] font-mono tracking-widest text-white/40 uppercase block mb-1">
                    Valor da Operação
                  </span>
                  <div className="text-2xl font-mono font-bold text-white">
                    R$ 1.280,00
                  </div>
                </div>
                <div className="flex items-center gap-2 text-xs font-mono text-white/50">
                  <span>CLIENTE</span>
                  <ArrowRight className="w-3.5 h-3.5 text-white/30" />
                  <span className="text-white">LOJA / SERVIÇO</span>
                </div>
              </div>

              {/* Terminal / Live Ledger Stream */}
              <div className="rounded-xl bg-black/70 border border-white/[0.06] p-4 font-mono text-[11px] space-y-1.5">
                <div className="text-white/30 text-[10px] tracking-widest pb-1 border-b border-white/[0.04] flex justify-between">
                  <span>LEDGER ENGINE TRACE</span>
                  <span className="text-emerald-400">P99: 11ms</span>
                </div>
                {transactionLog.map((log, i) => (
                  <div key={i} className="text-white/70 flex items-center gap-2">
                    <span className="text-white/30">›</span>
                    <span className={i === 0 ? "text-emerald-400 font-medium" : "text-white/60"}>
                      {log}
                    </span>
                  </div>
                ))}
              </div>

              {isCompleted && (
                <div className="flex items-center gap-2 text-xs font-mono text-emerald-400 bg-emerald-500/10 border border-emerald-500/20 p-3 rounded-xl">
                  <CheckCircle2 className="w-4 h-4 shrink-0" />
                  <span>Transação comitada com sucesso no banco de dados!</span>
                </div>
              )}
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}
