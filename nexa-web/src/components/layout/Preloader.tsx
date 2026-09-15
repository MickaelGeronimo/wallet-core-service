"use client";

import { useEffect, useRef, useState } from "react";
import { gsap } from "@/lib/gsap";

interface PreloaderProps {
  onComplete?: () => void;
}

export default function Preloader({ onComplete }: PreloaderProps) {
  const containerRef = useRef<HTMLDivElement>(null);
  const numberRef = useRef<HTMLSpanElement>(null);
  const textRef = useRef<HTMLDivElement>(null);
  const [isFinished, setIsFinished] = useState(false);

  useEffect(() => {
    const counter = { val: 0 };
    const ctx = gsap.context(() => {
      const tl = gsap.timeline({
        onComplete: () => {
          setIsFinished(true);
          onComplete?.();
        },
      });

      tl.to(counter, {
        val: 100,
        duration: 1.6,
        ease: "power2.inOut",
        onUpdate: () => {
          if (numberRef.current) {
            const rounded = Math.floor(counter.val);
            numberRef.current.textContent = rounded < 10 ? `0${rounded}` : `${rounded}`;
          }
        },
      })
        .to(
          textRef.current,
          {
            opacity: 0,
            y: -20,
            duration: 0.4,
            ease: "power2.in",
          },
          "-=0.2"
        )
        .to(containerRef.current, {
          yPercent: -100,
          duration: 0.9,
          ease: "power3.inOut",
        });
    });

    return () => ctx.revert();
  }, [onComplete]);

  if (isFinished) return null;

  return (
    <aside
      ref={containerRef}
      aria-label="Carregando aplicação"
      className="fixed inset-0 z-50 flex flex-col justify-between bg-[#050507] p-8 md:p-14 text-[#f4f4f6] select-none"
    >
      <header className="flex justify-between items-center text-xs tracking-widest uppercase text-white/40">
        <span>WALLET CORE</span>
        <span className="flex items-center gap-2">
          <span className="inline-block w-1.5 h-1.5 rounded-full bg-emerald-400 animate-pulse" />
          LEDGER INITIALIZING
        </span>
      </header>

      <div ref={textRef} className="flex flex-col items-center justify-center text-center my-auto">
        <h1 className="text-[14vw] md:text-[11vw] font-black tracking-tighter leading-none text-white select-none">
          <span ref={numberRef}>00</span>
          <span className="text-3xl md:text-5xl font-light text-white/30 ml-2">%</span>
        </h1>
        <p className="mt-4 text-xs md:text-sm uppercase tracking-widest text-white/50">
          Carregando Arquitetura Financeira
        </p>
      </div>

      <footer className="flex justify-between items-end text-xs tracking-wider text-white/30">
        <span>DOUBLE-ENTRY ENGINE</span>
        <span>ACID COMPLIANT</span>
      </footer>
    </aside>
  );
}
