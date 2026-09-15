"use client";

import React from "react";
import dynamic from "next/dynamic";
import { Lock, ShieldAlert, Cpu, CheckCircle } from "lucide-react";
import { TRANSACTION_FLOW_STEPS } from "@/data/content";

const Scene = dynamic(() => import("@/components/three/Scene"), { ssr: false });
const SecurityParticles = dynamic(() => import("@/components/three/SecurityParticles"), { ssr: false });

export default function Security() {
  return (
    <section
      id="security"
      className="relative py-32 px-6 md:px-12 bg-[#050507] border-t border-white/[0.06] overflow-hidden"
    >
      <div className="max-w-7xl mx-auto relative z-10">
        <div className="text-center max-w-3xl mx-auto mb-20">
          <span className="text-xs font-mono tracking-widest text-emerald-400 uppercase block mb-3">
            04 / SEGURANÇA INSTITUCIONAL
          </span>
          <h2 className="text-4xl md:text-6xl font-bold tracking-tight text-white mb-6">
            Blindagem Criptográfica. <br />
            <span className="text-gradient-silver">Risco Zero de Inconsistência.</span>
          </h2>
          <p className="text-sm md:text-base text-white/50 font-light leading-relaxed">
            Cada transação atravessa quatro camadas de validação determinística antes de qualquer débito ser efetivado.
          </p>
        </div>

        {/* 4 Steps Pipeline Cards */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
          {TRANSACTION_FLOW_STEPS.map((step, idx) => (
            <div
              key={step.step}
              className="p-6 rounded-2xl bg-[#0a0a0f] border border-white/[0.08] flex flex-col justify-between min-h-[220px] hover:border-emerald-500/30 transition-all"
            >
              <div>
                <div className="flex items-center justify-between mb-4">
                  <span className="text-xs font-mono text-emerald-400 font-bold">
                    FASE {step.step}
                  </span>
                  <span className="text-[10px] font-mono text-white/40 bg-white/[0.03] px-2 py-0.5 rounded">
                    {step.time}
                  </span>
                </div>
                <h4 className="text-base font-semibold text-white mb-2">{step.label}</h4>
                <p className="text-xs text-white/50 font-mono leading-relaxed">{step.status}</p>
              </div>

              <div className="pt-4 border-t border-white/[0.04] flex items-center justify-between text-[11px] font-mono text-white/30">
                <span>STATUS: VERIFICADO</span>
                <CheckCircle className="w-3.5 h-3.5 text-emerald-400" />
              </div>
            </div>
          ))}
        </div>

        {/* WebGL Ambient Background Nodes */}
        <div className="h-[280px] w-full mt-12 relative rounded-3xl overflow-hidden border border-white/[0.06] bg-black/40">
          <div className="absolute top-4 left-6 z-10 font-mono text-[11px] text-white/40 flex items-center gap-2">
            <span className="w-2 h-2 rounded-full bg-emerald-400 animate-ping" />
            <span>NEURAL RISK MESH • 350 NÓS ATIVOS</span>
          </div>
          <Scene cameraPosition={[0, 0, 5]}>
            <SecurityParticles count={300} />
          </Scene>
        </div>
      </div>
    </section>
  );
}
