"use client";

import React, { useRef } from "react";
import { useFrame } from "@react-three/fiber";
import { RoundedBox } from "@react-three/drei";
import * as THREE from "three";

interface FinancialCard3DProps {
  interactive?: boolean;
}

export default function FinancialCard3D({ interactive = true }: FinancialCard3DProps) {
  const groupRef = useRef<THREE.Group>(null);
  const targetRotation = useRef({ x: 0.15, y: -0.3 });

  useFrame((state) => {
    if (!groupRef.current) return;

    if (interactive) {
      // Lerp towards mouse pointer in normalized device coordinates
      const mouseX = state.pointer.x * 0.45;
      const mouseY = state.pointer.y * 0.35;

      targetRotation.current.y = -0.35 + mouseX;
      targetRotation.current.x = 0.15 - mouseY;
    }

    // Smooth lerp for organic floating feeling
    groupRef.current.rotation.x = THREE.MathUtils.lerp(
      groupRef.current.rotation.x,
      targetRotation.current.x + Math.sin(state.clock.elapsedTime * 1.2) * 0.05,
      0.06
    );
    groupRef.current.rotation.y = THREE.MathUtils.lerp(
      groupRef.current.rotation.y,
      targetRotation.current.y + Math.cos(state.clock.elapsedTime * 0.8) * 0.06,
      0.06
    );

    // Subtle breathing float in Z and Y
    groupRef.current.position.y = Math.sin(state.clock.elapsedTime * 1.5) * 0.08;
  });

  return (
    <group ref={groupRef} scale={[1.25, 1.25, 1.25]}>
      {/* Main Obsidian Card Body */}
      <RoundedBox
        args={[3.37, 2.125, 0.04]} // Standard ISO/IEC 7810 ID-1 card ratio (85.60 x 53.98 mm)
        radius={0.08}
        smoothness={4}
      >
        <meshPhysicalMaterial
          color="#0d0d12"
          metalness={0.92}
          roughness={0.18}
          clearcoat={0.9}
          clearcoatRoughness={0.1}
          reflectivity={0.8}
        />
      </RoundedBox>

      {/* Titanium Card Bevel Edge (outer border accent) */}
      <RoundedBox
        args={[3.39, 2.145, 0.038]}
        radius={0.082}
        smoothness={4}
      >
        <meshStandardMaterial
          color="#222228"
          metalness={0.95}
          roughness={0.3}
          wireframe={false}
        />
      </RoundedBox>

      {/* Gold EMV Microchip */}
      <group position={[-0.85, 0.15, 0.025]}>
        <mesh>
          <planeGeometry args={[0.55, 0.44]} />
          <meshStandardMaterial
            color="#e6b450"
            metalness={0.98}
            roughness={0.25}
          />
        </mesh>
        {/* Chip Circuit Divider Lines */}
        <mesh position={[0, 0, 0.001]}>
          <planeGeometry args={[0.015, 0.44]} />
          <meshBasicMaterial color="#785310" />
        </mesh>
        <mesh position={[0, 0, 0.001]}>
          <planeGeometry args={[0.55, 0.015]} />
          <meshBasicMaterial color="#785310" />
        </mesh>
      </group>

      {/* Contactless Waves Icon */}
      <group position={[-0.2, 0.15, 0.025]}>
        <mesh position={[0, 0, 0]}>
          <ringGeometry args={[0.08, 0.1, 16, 1, 0, Math.PI / 2]} />
          <meshBasicMaterial color="#ffffff" transparent opacity={0.6} />
        </mesh>
        <mesh position={[-0.04, -0.04, 0]}>
          <ringGeometry args={[0.14, 0.16, 16, 1, 0, Math.PI / 2]} />
          <meshBasicMaterial color="#ffffff" transparent opacity={0.6} />
        </mesh>
      </group>

      {/* Hologram / Specular Accent Band */}
      <mesh position={[0.9, 0, 0.022]}>
        <planeGeometry args={[0.7, 2.1]} />
        <meshPhysicalMaterial
          color="#161822"
          metalness={0.8}
          roughness={0.2}
          clearcoat={1.0}
          transparent
          opacity={0.7}
        />
      </mesh>

      {/* NEXA Brand Accent Bar */}
      <mesh position={[0.9, 0.65, 0.025]}>
        <planeGeometry args={[0.5, 0.1]} />
        <meshStandardMaterial color="#ffffff" metalness={0.9} roughness={0.1} />
      </mesh>
    </group>
  );
}
