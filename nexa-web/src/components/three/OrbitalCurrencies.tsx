"use client";

import React, { useRef } from "react";
import { useFrame } from "@react-three/fiber";
import * as THREE from "three";

export default function OrbitalCurrencies() {
  const ring1 = useRef<THREE.Group>(null);
  const ring2 = useRef<THREE.Group>(null);
  const ring3 = useRef<THREE.Group>(null);

  useFrame((state) => {
    const t = state.clock.elapsedTime;
    if (ring1.current) ring1.current.rotation.z = t * 0.15;
    if (ring2.current) ring2.current.rotation.z = -t * 0.12;
    if (ring3.current) ring3.current.rotation.x = t * 0.18;
  });

  return (
    <group scale={[0.9, 0.9, 0.9]}>
      {/* Central Metallic Core */}
      <mesh>
        <sphereGeometry args={[0.75, 32, 32]} />
        <meshStandardMaterial
          color="#121218"
          metalness={0.95}
          roughness={0.2}
          wireframe={false}
        />
      </mesh>

      {/* Orbit 1 */}
      <group ref={ring1} rotation={[Math.PI / 4, 0, 0]}>
        <mesh>
          <torusGeometry args={[2.0, 0.012, 16, 100]} />
          <meshBasicMaterial color="#ffffff" transparent opacity={0.2} />
        </mesh>
        {/* Node USD */}
        <mesh position={[2.0, 0, 0]}>
          <sphereGeometry args={[0.16, 16, 16]} />
          <meshStandardMaterial color="#38bdf8" metalness={0.8} roughness={0.2} />
        </mesh>
      </group>

      {/* Orbit 2 */}
      <group ref={ring2} rotation={[-Math.PI / 3, Math.PI / 6, 0]}>
        <mesh>
          <torusGeometry args={[2.7, 0.012, 16, 100]} />
          <meshBasicMaterial color="#ffffff" transparent opacity={0.2} />
        </mesh>
        {/* Node EUR */}
        <mesh position={[-2.7, 0, 0]}>
          <sphereGeometry args={[0.18, 16, 16]} />
          <meshStandardMaterial color="#e6b450" metalness={0.9} roughness={0.2} />
        </mesh>
        {/* Node BRL */}
        <mesh position={[0, 2.7, 0]}>
          <sphereGeometry args={[0.15, 16, 16]} />
          <meshStandardMaterial color="#10b981" metalness={0.8} roughness={0.2} />
        </mesh>
      </group>

      {/* Orbit 3 */}
      <group ref={ring3} rotation={[0, Math.PI / 3, Math.PI / 4]}>
        <mesh>
          <torusGeometry args={[3.4, 0.012, 16, 100]} />
          <meshBasicMaterial color="#ffffff" transparent opacity={0.15} />
        </mesh>
        {/* Node GBP */}
        <mesh position={[0, -3.4, 0]}>
          <sphereGeometry args={[0.17, 16, 16]} />
          <meshStandardMaterial color="#a78bfa" metalness={0.8} roughness={0.2} />
        </mesh>
      </group>
    </group>
  );
}
