"use client";

import { useEffect, useState } from "react";
import Lenis from "lenis";

export function useLenis() {
  const [lenis, setLenis] = useState<Lenis | null>(null);

  useEffect(() => {
    // Check if lenis was placed on window by SmoothScroll provider
    if (typeof window !== "undefined" && (window as unknown as { __lenis?: Lenis }).__lenis) {
      setLenis((window as unknown as { __lenis: Lenis }).__lenis);
    }
  }, []);

  return lenis;
}
