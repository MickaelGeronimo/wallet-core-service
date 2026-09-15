"use client";

import React, { useRef, useEffect } from "react";
import TiltCard from "@/components/ui/TiltCard";
import { FINANCIAL_PILLARS } from "@/data/content";
import { ArrowUpRight, Zap, TrendingUp, Compass } from "lucide-react";
import { gsap } from "@/lib/gsap";

const ICONS = [Zap, TrendingUp, Compass];

export default function FinancialExperience() {
  const containerRef = useRef<HTMLDivElement>(null);
  const cardsRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const ctx = gsap.context(() => {
      gsap.from(cardsRef.current?.children || [], {
        scrollTrigger: {
          trigger: containerRef.current,
          start: "top 75%",
          toggleActions: "play none none none",
        },
        y: 80,
        opacity: 0,
        duration: 1.0,
        stagger: 0.18,
        ease: "power3.out",
      });
    }, containerRef);

    return () => ctx.revert();
  }, []);

  return (
    <section
      id="experience"
      ref={containerRef}
      className="relative py-32 px-6 md:px-12 bg-gradient-to-b from-[#2075f5] via-[#0b172a] to-[#050507] border-t border-white/[0.1] overflow-hidden"
    >
      <div className="max-w-7xl mx-auto">
        {/* Editorial Section Header */}
        <div className="flex flex-col md:flex-row md:items-end justify-between mb-20 gap-8">
          <div>
            <span className="text-xs font-mono tracking-widest text-white/40 uppercase block mb-3">
              02 / EXPERIÊNCIA DE CAPITAL
            </span>
            <h2 className="text-4xl md:text-6xl font-bold tracking-tight text-white max-w-2xl leading-[1.05]">
              Três dimensões. <br />
              <span className="text-gradient-silver">Um ecossistema unificado.</span>
            </h2>
          </div>
          <p className="text-sm md:text-base text-white/50 max-w-md font-light leading-relaxed">
            Eliminamos camadas de latência e burocracia bancária tradicional. O controle do seu patrimônio opera na velocidade da luz.
          </p>
        </div>

        {/* 3 Pillar Cards */}
        <div ref={cardsRef} className="grid grid-cols-1 lg:grid-cols-3 gap-6 md:gap-8">
          {FINANCIAL_PILLARS.map((pillar, idx) => {
            const Icon = ICONS[idx];
            return (
              <TiltCard
                key={pillar.title}
                maxTilt={8}
                className="rounded-3xl bg-[#0c0c11] border border-white/[0.08] p-8 md:p-10 flex flex-col justify-between min-h-[460px] hover:border-white/20"
              >
                <div>
                  <div className="flex items-center justify-between mb-8">
                    <span className="text-xs font-mono tracking-widest text-white/40 uppercase">
                      {pillar.tag}
                    </span>
                    <span className="text-[10px] font-mono tracking-wider px-2.5 py-1 rounded-full bg-white/[0.05] border border-white/10 text-white/70">
                      {pillar.badge}
                    </span>
                  </div>

                  <div className="w-12 h-12 rounded-2xl bg-white/[0.04] border border-white/10 flex items-center justify-center text-white mb-6">
                    <Icon className="w-5 h-5 text-white/90" />
                  </div>

                  <h3 className="text-3xl md:text-4xl font-bold tracking-tight text-white mb-2">
                    {pillar.title}
                  </h3>
                  <p className="text-sm text-white/70 font-medium mb-4">
                    {pillar.subtitle}
                  </p>
                  <p className="text-xs md:text-sm text-white/40 font-light leading-relaxed">
                    {pillar.description}
                  </p>
                </div>

                <div className="pt-8 mt-8 border-t border-white/[0.06] flex items-end justify-between">
                  <div>
                    <div className="text-2xl md:text-3xl font-mono font-bold tracking-tight text-white">
                      {pillar.metric}
                    </div>
                    <div className="text-[11px] font-mono text-white/40 mt-1">
                      {pillar.metricLabel}
                    </div>
                  </div>

                  <div className="w-9 h-9 rounded-full bg-white/5 border border-white/10 flex items-center justify-center text-white/40 group-hover:text-white group-hover:bg-white/10 transition-all">
                    <ArrowUpRight className="w-4 h-4" />
                  </div>
                </div>
              </TiltCard>
            );
          })}
        </div>
      </div>
    </section>
  );
}
