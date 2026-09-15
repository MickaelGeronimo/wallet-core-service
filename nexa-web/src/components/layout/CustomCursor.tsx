"use client";

import { useEffect, useRef, useState } from "react";
import { gsap } from "@/lib/gsap";

export default function CustomCursor() {
  const cursorRef = useRef<HTMLDivElement>(null);
  const dotRef = useRef<HTMLDivElement>(null);
  const [hasPointer, setHasPointer] = useState(false);

  useEffect(() => {
    // Disable cursor on touch devices
    if (window.matchMedia("(pointer: coarse)").matches) return;
    setHasPointer(true);

    const cursor = cursorRef.current;
    const dot = dotRef.current;
    if (!cursor || !dot) return;

    let mouseX = window.innerWidth / 2;
    let mouseY = window.innerHeight / 2;

    const onMouseMove = (e: MouseEvent) => {
      mouseX = e.clientX;
      mouseY = e.clientY;

      gsap.to(dot, {
        x: mouseX,
        y: mouseY,
        duration: 0.08,
        ease: "power2.out",
      });

      gsap.to(cursor, {
        x: mouseX,
        y: mouseY,
        duration: 0.35,
        ease: "power3.out",
      });

      // Target hover state
      const target = e.target as HTMLElement | null;
      const hoverEl = target?.closest("[data-cursor]");
      const isInteractive = target?.closest("button, a, input, [role='button']");

      if (hoverEl) {
        const type = hoverEl.getAttribute("data-cursor");
        if (type === "card") {
          gsap.to(cursor, {
            scale: 2.2,
            borderColor: "rgba(255, 255, 255, 0.4)",
            backgroundColor: "rgba(255, 255, 255, 0.05)",
            duration: 0.25,
          });
        } else {
          gsap.to(cursor, {
            scale: 1.8,
            borderColor: "rgba(255, 255, 255, 0.6)",
            backgroundColor: "rgba(255, 255, 255, 0.08)",
            duration: 0.25,
          });
        }
      } else if (isInteractive) {
        gsap.to(cursor, {
          scale: 1.5,
          borderColor: "rgba(255, 255, 255, 0.5)",
          duration: 0.2,
        });
      } else {
        gsap.to(cursor, {
          scale: 1,
          borderColor: "rgba(255, 255, 255, 0.25)",
          backgroundColor: "transparent",
          duration: 0.25,
        });
      }
    };

    window.addEventListener("mousemove", onMouseMove, { passive: true });

    return () => {
      window.removeEventListener("mousemove", onMouseMove);
    };
  }, []);

  if (!hasPointer) return null;

  return (
    <div className="pointer-events-none fixed inset-0 z-50 overflow-hidden mix-blend-difference">
      {/* Inner Dot */}
      <div
        ref={dotRef}
        className="fixed top-0 left-0 w-1.5 h-1.5 -ml-[3px] -mt-[3px] rounded-full bg-white transition-opacity duration-300"
      />
      {/* Outer Follower Ring */}
      <div
        ref={cursorRef}
        className="fixed top-0 left-0 w-8 h-8 -ml-4 -mt-4 rounded-full border border-white/30 backdrop-blur-[1px] transition-all duration-200 ease-out"
      />
    </div>
  );
}
