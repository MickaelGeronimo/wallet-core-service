"use client";

import { useEffect, useState } from "react";

const REVOLUT_NAV = [
  { label: "Conta pessoal", href: "#" },
  { label: "Business", href: "#" },
  { label: "Kids & Teens", href: "#" },
  { label: "Ajuda", href: "#" },
  { label: "Sobre nós", href: "#" },
];

export default function Navbar() {
  const [scrolled, setScrolled] = useState(false);

  useEffect(() => {
    const onScroll = () => {
      setScrolled(window.scrollY > 30);
    };
    window.addEventListener("scroll", onScroll, { passive: true });
    return () => window.removeEventListener("scroll", onScroll);
  }, []);

  return (
    <header
      className={`fixed top-0 left-0 right-0 z-40 transition-all duration-300 ${
        scrolled
          ? "py-4 bg-[#1b6ff4]/85 backdrop-blur-md shadow-sm"
          : "py-6 bg-transparent"
      }`}
    >
      <div className="max-w-7xl mx-auto px-6 md:px-12 flex items-center justify-between">
        {/* Revolut / Nexa Brand Logo */}
        <a
          href="#"
          className="flex items-center gap-2 text-white focus:outline-none"
          data-cursor="pointer"
        >
          <span className="font-sans text-2xl font-black tracking-tight text-white select-none">
            Revolut
          </span>
        </a>

        {/* Center Nav Links */}
        <nav className="hidden lg:flex items-center gap-7">
          {REVOLUT_NAV.map((link) => (
            <a
              key={link.label}
              href={link.href}
              className="text-sm font-medium text-white/90 hover:text-white transition-colors"
              data-cursor="pointer"
            >
              {link.label}
            </a>
          ))}
        </nav>

        {/* Right Actions */}
        <div className="flex items-center gap-6">
          <a
            href="http://localhost:8080/swagger-ui/index.html"
            target="_blank"
            rel="noreferrer"
            className="text-sm font-medium text-white hover:text-white/80 transition-colors"
            data-cursor="pointer"
          >
            Entrar
          </a>

          <a
            href="#experience"
            className="px-6 py-2.5 rounded-full text-sm font-semibold bg-[#191c1f] text-white hover:bg-black hover:scale-[1.02] active:scale-[0.98] transition-all shadow-md"
            data-cursor="pointer"
          >
            Inscrever-se
          </a>
        </div>
      </div>
    </header>
  );
}
