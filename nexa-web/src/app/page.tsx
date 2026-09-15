"use client";

import { useState } from "react";
import Preloader from "@/components/layout/Preloader";
import CustomCursor from "@/components/layout/CustomCursor";
import Navbar from "@/components/layout/Navbar";
import Footer from "@/components/layout/Footer";
import Hero from "@/components/sections/Hero";
import FinancialExperience from "@/components/sections/FinancialExperience";
import PaymentExperience from "@/components/sections/PaymentExperience";
import Security from "@/components/sections/Security";
import GlobalMoney from "@/components/sections/GlobalMoney";
import Technology from "@/components/sections/Technology";
import FinalCTA from "@/components/sections/FinalCTA";

export default function Home() {
  const [preloaderDone, setPreloaderDone] = useState(false);

  return (
    <main className="relative min-h-screen bg-[#08080a] text-[#f4f4f6]">
      {/* Preloader Curtain */}
      <Preloader onComplete={() => setPreloaderDone(true)} />

      {/* Custom Spring/Lerp Cursor */}
      <CustomCursor />

      {/* Global Navigation Bar */}
      <Navbar />

      {/* Narrative Chapters */}
      <Hero />
      <FinancialExperience />
      <PaymentExperience />
      <Security />
      <GlobalMoney />
      <Technology />
      <FinalCTA />

      {/* Editorial Tech Footer */}
      <Footer />
    </main>
  );
}
