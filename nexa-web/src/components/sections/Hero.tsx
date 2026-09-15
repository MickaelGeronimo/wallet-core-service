"use client";

import React, { useState } from "react";
import Image from "next/image";
import { Coins, Check, ArrowRight } from "lucide-react";
import { motion, AnimatePresence } from "framer-motion";

const CURRENCY_OPTIONS = [
  { label: "BRL", symbol: "R$", amount: "42.084", salary: "+R$ 17.850" },
  { label: "USD", symbol: "$", amount: "8.416", salary: "+$ 3.570" },
  { label: "EUR", symbol: "€", amount: "7.650", salary: "+€ 3.245" },
];

export default function Hero() {
  const [currIndex, setCurrIndex] = useState(0);
  const [showToast, setShowToast] = useState(false);

  const active = CURRENCY_OPTIONS[currIndex];

  const handleToggleCurrency = () => {
    setCurrIndex((prev) => (prev + 1) % CURRENCY_OPTIONS.length);
  };

  const handleSalaryClick = () => {
    setShowToast(true);
    setTimeout(() => setShowToast(false), 2400);
  };

  return (
    <section className="relative min-h-screen w-full bg-[#2075f5] bg-revolut-sky pt-28 pb-16 px-6 md:px-12 flex flex-col justify-center overflow-hidden">
      {/* Background Soft Sunlight Flare */}
      <div className="absolute top-0 right-1/4 w-[600px] h-[500px] bg-white/10 rounded-full blur-3xl pointer-events-none" />

      <div className="max-w-7xl mx-auto w-full grid grid-cols-1 lg:grid-cols-12 gap-12 items-center my-auto z-10">
        
        {/* Left Editorial Content */}
        <div className="lg:col-span-6 flex flex-col items-start text-left z-20">
          <h1 className="text-5xl sm:text-6xl lg:text-[4.75rem] font-bold tracking-tight text-white leading-[1.05] select-none">
            Mais que uma conta <br />
            global
          </h1>

          <p className="mt-6 text-base sm:text-lg text-white/95 font-normal max-w-md leading-relaxed">
            Dentro ou fora do país, local ou globalmente, mova-se com liberdade entre fronteiras e moedas. Registre-se grátis com um toque.
          </p>

          <div className="mt-8 flex items-center gap-4">
            <a
              href="#experience"
              className="px-7 py-3 rounded-full bg-[#191c1f] text-white font-semibold text-sm hover:bg-black transition-all shadow-xl active:scale-95 flex items-center gap-2"
              data-cursor="pointer"
            >
              <span>Baixar app</span>
            </a>

            <button
              onClick={handleToggleCurrency}
              className="px-5 py-3 rounded-full bg-white/10 border border-white/25 text-white text-xs font-semibold hover:bg-white/20 transition-all backdrop-blur-md"
              data-cursor="pointer"
            >
              Alternar Moeda ({active.label})
            </button>
          </div>
        </div>

        {/* Center / Right Editorial Subject & Phone Silhouette */}
        <div className="lg:col-span-6 relative flex items-center justify-center min-h-[520px] sm:min-h-[600px]">
          
          {/* Editorial Photograph Layer */}
          <div className="relative w-[340px] sm:w-[420px] md:w-[460px] h-[460px] sm:h-[540px] md:h-[580px] rounded-[44px] overflow-hidden shadow-2xl">
            <Image
              src="/revolut_hero_subject.jpg"
              alt="Modelo editorial em casaco de linho bege contra o céu azul aberto"
              fill
              priority
              className="object-cover object-center transform scale-105"
            />
            {/* Soft Ambient Blend Overlay */}
            <div className="absolute inset-0 bg-gradient-to-t from-[#2075f5]/30 via-transparent to-transparent pointer-events-none" />
          </div>

          {/* Translucent Smartphone Glass Silhouette Frame */}
          <div className="absolute w-[290px] sm:w-[350px] md:w-[380px] h-[480px] sm:h-[540px] md:h-[580px] rounded-[46px] border-2 border-white/60 phone-glass-frame backdrop-blur-[1px] pointer-events-auto flex flex-col justify-between p-6 sm:p-8 z-30">
            
            {/* Top Empty Space */}
            <div className="w-full flex justify-center pt-2">
              <div className="w-16 h-1 rounded-full bg-white/30" />
            </div>

            {/* Center Balance Display */}
            <div className="flex flex-col items-center justify-center text-center my-auto -mt-6">
              <span className="text-white text-sm sm:text-base font-medium drop-shadow-sm">
                Conta pessoal
              </span>

              <AnimatePresence mode="wait">
                <motion.div
                  key={active.label}
                  initial={{ opacity: 0, y: 10, scale: 0.96 }}
                  animate={{ opacity: 1, y: 0, scale: 1 }}
                  exit={{ opacity: 0, y: -10, scale: 0.96 }}
                  transition={{ duration: 0.25 }}
                  className="text-4xl sm:text-5xl md:text-6xl font-extrabold text-white tracking-tight my-2 drop-shadow-md select-none"
                >
                  {active.symbol} {active.amount}
                </motion.div>
              </AnimatePresence>

              <button
                onClick={handleToggleCurrency}
                className="mt-2 px-6 py-2 rounded-full bg-white text-[#191c1f] font-semibold text-xs sm:text-sm shadow-lg hover:scale-105 active:scale-95 transition-all"
                data-cursor="pointer"
              >
                Contas
              </button>
            </div>

            {/* Bottom Floating Transaction Pill */}
            <div
              onClick={handleSalaryClick}
              className="w-full bg-white rounded-3xl p-3.5 sm:p-4 shadow-2xl flex items-center justify-between cursor-pointer hover:scale-[1.02] active:scale-[0.98] transition-all border border-black/5"
              data-cursor="pointer"
            >
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 sm:w-11 sm:h-11 rounded-2xl bg-[#4965f6] flex items-center justify-center text-white shadow-sm shrink-0">
                  <Coins className="w-5 h-5" />
                </div>
                <div className="flex flex-col text-left">
                  <span className="text-sm font-bold text-[#191c1f] leading-tight">
                    Salário
                  </span>
                  <span className="text-[11px] sm:text-xs text-[#73767c]">
                    Hoje às 11:28
                  </span>
                </div>
              </div>

              <div className="text-sm sm:text-base font-bold text-[#191c1f]">
                {active.salary}
              </div>
            </div>

          </div>

          {/* Toast Notification upon clicking transaction */}
          <AnimatePresence>
            {showToast && (
              <motion.div
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0, y: -20 }}
                className="absolute -bottom-6 z-40 px-4 py-2 rounded-full bg-[#191c1f] text-white text-xs font-medium shadow-2xl flex items-center gap-2"
              >
                <Check className="w-3.5 h-3.5 text-emerald-400" />
                <span>Transação confirmada no Ledger ACID</span>
              </motion.div>
            )}
          </AnimatePresence>

        </div>

      </div>
    </section>
  );
}
