"use client";

import React, { Suspense } from "react";
import { Canvas } from "@react-three/fiber";

interface SceneProps {
  children: React.ReactNode;
  className?: string;
  cameraPosition?: [number, number, number];
  fov?: number;
}

export default function Scene({
  children,
  className = "",
  cameraPosition = [0, 0, 5.5],
  fov = 45,
}: SceneProps) {
  return (
    <div className={`relative w-full h-full ${className}`}>
      <Canvas
        camera={{ position: cameraPosition, fov }}
        dpr={[1, 1.75]} // Cap DPR to prevent GPU bottlenecking
        gl={{
          antialias: true,
          powerPreference: "high-performance",
          alpha: true,
        }}
      >
        {/* Soft Studio Lighting */}
        <ambientLight intensity={0.7} />
        
        {/* Key Light */}
        <directionalLight position={[5, 6, 5]} intensity={1.8} color="#ffffff" />
        
        {/* Fill Light (Cool bluish tone) */}
        <directionalLight position={[-5, -2, 2]} intensity={0.6} color="#38bdf8" />
        
        {/* Rim Light (Gold warmth on card edges) */}
        <directionalLight position={[0, -5, -4]} intensity={0.9} color="#e6b450" />
        
        {/* Top Specular */}
        <pointLight position={[0, 4, 2]} intensity={0.8} />

        <Suspense fallback={null}>{children}</Suspense>
      </Canvas>
    </div>
  );
}
