"use client";

import React, { useState } from "react";
import dynamic from "next/dynamic";
import { CURRENCIES } from "@/data/content";
import { Globe, ArrowRightLeft } from "lucide-react";

const Scene = dynamic(() => import("@/components/three/Scene"), { ssr: false });
const OrbitalCurrencies = dynamic(() => import("@/components/three/OrbitalCurrencies"), { ssr: false });

export default function GlobalMoney() {
  const [amount, setAmount] = useState<number>(1000);
  const [selectedCurrency, setSelectedCurrency] = useState<string>("USD");

  const activeCurrency = CURRENCIES.find((c) => c.code === selectedCurrency) || CURRENCIES[0];
  const converted = (amount / parseFloat(activeCurrency.rate)).toFixed(2);

  return (
    <section
      id="global"
      className="relative py-32 px-6 md:px-12 bg-[#08080a] bg-grid-tech border-t border-white/[0.06] overflow-hidden"
    >
      <div className="max-w-7xl mx-auto">
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-12 items-center">
          {/* Left: 3D Orbital Scene */}
          <div className="lg:col-span-5 h-[380px] md:h-[480px] rounded-3xl bg-black/40 border border-white/[0.08] relative overflow-hidden flex items-center justify-center">
            <div className="absolute top-4 left-6 z-10 font-mono text-[11px] text-white/40 flex items-center gap-2">
              <Globe className="w-3.5 h-3.5 text-cyan-400" />
              <span>ORBITAL MULTI-CURRENCY ROUTING</span>
            </div>
            <Scene cameraPosition={[0, 0, 5.5]}>
              <OrbitalCurrencies />
            </Scene>
          </div>

          {/* Right: Currency Exchange Info & Converter */}
          <div className="lg:col-span-7 space-y-8">
            <div>
              <span className="text-xs font-mono tracking-widest text-cyan-400 uppercase block mb-3">
                05 / LIQUIDEZ SEM FRONTEIRAS
              </span>
              <h2 className="text-4xl md:text-5xl font-bold tracking-tight text-white mb-4">
                Mova capital pelo planeta. <br />
                <span className="text-gradient-silver">Ao câmbio comercial real.</span>
              </h2>
              <p className="text-sm md:text-base text-white/50 font-light leading-relaxed max-w-xl">
                Contas multimoeda integradas com liquidação FX direta. Sem spread oculto, sem taxas abusivas de conversão.
              </p>
            </div>

            {/* Currency Rates Strip */}
            <div className="grid grid-cols-2 sm:grid-cols-3 gap-3">
              {CURRENCIES.map((curr) => (
                <button
                  key={curr.code}
                  onClick={() => setSelectedCurrency(curr.code)}
                  className={`p-3.5 rounded-xl text-left border transition-all ${
                    selectedCurrency === curr.code
                      ? "bg-white/10 border-white/40"
                      : "bg-white/[0.02] border-white/[0.06] hover:bg-white/[0.05]"
                  }`}
                >
                  <div className="flex justify-between items-center text-xs font-mono mb-1">
                    <span className="font-bold text-white">{curr.code}</span>
                    <span className={curr.change.startsWith("+") ? "text-emerald-400" : "text-rose-400"}>
                      {curr.change}
                    </span>
                  </div>
                  <div className="text-[11px] text-white/40 font-mono">
                    1 USD = {curr.rate} {curr.symbol}
                  </div>
                </button>
              ))}
            </div>

            {/* Instant Convert Mock */}
            <div className="p-5 rounded-2xl bg-[#0c0c14] border border-white/10 flex flex-col sm:flex-row items-center justify-between gap-4">
              <div className="w-full sm:w-auto">
                <label className="text-[10px] font-mono tracking-widest text-white/40 uppercase block mb-1">
                  Você envia (BRL)
                </label>
                <input
                  type="number"
                  value={amount}
                  onChange={(e) => setAmount(Number(e.target.value))}
                  className="w-full sm:w-40 bg-white/[0.05] border border-white/10 rounded-lg px-3 py-1.5 font-mono text-white text-lg focus:outline-none focus:border-white/30"
                />
              </div>

              <div className="w-8 h-8 rounded-full bg-white/5 flex items-center justify-center text-white/40">
                <ArrowRightLeft className="w-4 h-4" />
              </div>

              <div className="w-full sm:w-auto text-right">
                <label className="text-[10px] font-mono tracking-widest text-white/40 uppercase block mb-1">
                  Destino ({activeCurrency.code})
                </label>
                <div className="text-xl md:text-2xl font-mono font-bold text-emerald-400">
                  {activeCurrency.symbol} {converted}
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}
