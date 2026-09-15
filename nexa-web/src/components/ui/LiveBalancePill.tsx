"use client";

import { useState } from "react";
import { ArrowUpRight, ShieldCheck } from "lucide-react";

export default function LiveBalancePill() {
  const [balance, setBalance] = useState(24850.4);
  const [isCopied, setIsCopied] = useState(false);

  const handleCopyLedgerHash = () => {
    navigator.clipboard?.writeText("0x7f8a3c9e2b1049ad5f");
    setIsCopied(true);
    setTimeout(() => setIsCopied(false), 2000);
  };

  return (
    <div
      onClick={handleCopyLedgerHash}
      className="group relative cursor-pointer inline-flex items-center gap-4 px-5 py-3 rounded-2xl bg-black/60 border border-white/10 backdrop-blur-2xl shadow-2xl hover:border-white/30 transition-all duration-300"
      data-cursor="pointer"
    >
      <div className="w-10 h-10 rounded-xl bg-white/[0.06] border border-white/10 flex items-center justify-center text-white">
        <ShieldCheck className="w-5 h-5 text-emerald-400" />
      </div>

      <div className="flex flex-col text-left">
        <div className="flex items-center gap-2">
          <span className="text-[10px] font-mono tracking-widest text-white/40 uppercase">
            Saldo Consolidado (Ledger)
          </span>
          <span className="inline-flex items-center text-[10px] font-mono text-emerald-400 bg-emerald-500/10 px-1.5 py-0.5 rounded">
            +12.8%
            <ArrowUpRight className="w-2.5 h-2.5 ml-0.5" />
          </span>
        </div>
        <div className="text-xl md:text-2xl font-bold font-mono tracking-tight text-white">
          R$ {balance.toLocaleString("pt-BR", { minimumFractionDigits: 2 })}
        </div>
      </div>

      <div className="hidden sm:flex flex-col items-end pl-3 border-l border-white/10 text-[10px] font-mono text-white/40">
        <span>HASH: #7F8A..9B</span>
        <span className="text-emerald-400/80">
          {isCopied ? "COPIADO!" : "AUDITADO ACID"}
        </span>
      </div>
    </div>
  );
}
