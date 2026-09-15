"use client";

import React, { useState } from "react";
import { ArrowRight, Check, Sparkles } from "lucide-react";
import MagneticButton from "@/components/ui/MagneticButton";

export default function FinalCTA() {
  const [email, setEmail] = useState("");
  const [submitted, setSubmitted] = useState(false);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!email) return;
    setSubmitted(true);
  };

  return (
    <section className="relative py-40 px-6 md:px-12 bg-[#08080a] bg-grid-tech border-t border-white/[0.08] overflow-hidden text-center">
      {/* Dramatic Backlight Glow */}
      <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[800px] h-[500px] bg-gradient-to-t from-white/[0.06] to-transparent rounded-full blur-3xl pointer-events-none" />

      <div className="max-w-5xl mx-auto relative z-10">
        <div className="inline-flex items-center gap-2 px-3.5 py-1.5 rounded-full bg-white/[0.04] border border-white/10 text-xs font-mono text-white/70 uppercase tracking-widest mb-8 backdrop-blur-md">
          <Sparkles className="w-3.5 h-3.5 text-nexa-gold" />
          <span>ACESSO ANTECIPADO VIP</span>
        </div>

        <h2 className="text-5xl sm:text-7xl md:text-8xl font-bold tracking-tighter text-white leading-[0.95] mb-8 select-none">
          Move money. <br />
          <span className="text-gradient-silver">Move forward.</span>
        </h2>

        <p className="text-base md:text-xl text-white/60 font-light max-w-2xl mx-auto mb-12 leading-relaxed">
          Experimente a velocidade de um ledger bancário nativo somada à mais refinada experiência visual já desenhada.
        </p>

        {submitted ? (
          <div className="inline-flex items-center gap-3 px-8 py-5 rounded-full bg-emerald-500/10 border border-emerald-500/30 text-emerald-400 font-mono text-sm">
            <Check className="w-5 h-5" />
            <span>Acesso registrado com prioridade para seu e-mail!</span>
          </div>
        ) : (
          <form onSubmit={handleSubmit} className="flex flex-col sm:flex-row items-center justify-center gap-4 max-w-md mx-auto">
            <input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="Digite seu e-mail corporativo..."
              required
              className="w-full sm:w-80 px-5 py-4 rounded-full bg-white/[0.06] border border-white/10 text-white placeholder-white/40 text-sm focus:outline-none focus:border-white/40 backdrop-blur-md"
            />
            <MagneticButton
              type="submit"
              className="w-full sm:w-auto px-8 py-4 rounded-full bg-white text-black font-semibold text-sm hover:bg-white/90 shadow-2xl transition-all"
            >
              <span>Garantir Vaga</span>
              <ArrowRight className="w-4 h-4 ml-1" />
            </MagneticButton>
          </form>
        )}

        <div className="mt-12 flex items-center justify-center gap-6 text-xs font-mono text-white/40">
          <span>• Abertura sem tarifas de custódia</span>
          <span>• Auditoria Double-Entry em tempo real</span>
          <span>• Cartão Obsidian Físico & Virtual</span>
        </div>
      </div>
    </section>
  );
}
