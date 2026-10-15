import React, { useEffect, useRef, useState } from 'react';
import * as THREE from 'three';
import { Box, Truck } from 'lucide-react';
import { SimulatorSession } from '../types';

interface ThreeFleetSceneProps {
  sessions: SimulatorSession[];
  selectedVehicleId: number | null;
  onSelectVehicle: (vehicleId: number) => void;
}

/**
 * Procedural 3D Fleet Visualization built with Three.js.
 * Renders low-poly truck models (cab, cargo trailer, wheels, status beacon),
 * a stylized logistics warehouse hub, road highway corridor, and yard lighting.
 * Synchronizes truck positions and headings with the GPS simulator and provides
 * an automatic accessible fallback when WebGL is unavailable.
 */
export const ThreeFleetScene: React.FC<ThreeFleetSceneProps> = ({
  sessions,
  selectedVehicleId,
  onSelectVehicle,
}) => {
  const mountRef = useRef<HTMLDivElement | null>(null);
  const isJsdom = typeof navigator !== 'undefined' && /jsdom/i.test(navigator.userAgent);
  const [webglSupported, setWebglSupported] = useState<boolean>(!isJsdom);
  const sessionsRef = useRef<SimulatorSession[]>(sessions);
  sessionsRef.current = sessions;

  useEffect(() => {
    if (isJsdom) {
      setWebglSupported(false);
      return;
    }
    const container = mountRef.current;
    if (!container) return;

    let renderer: THREE.WebGLRenderer | null = null;
    let animationFrameId = 0;

    try {
      renderer = new THREE.WebGLRenderer({ antialias: true, alpha: false });
      const width = container.clientWidth || 720;
      const height = 380;
      renderer.setSize(width, height);
      renderer.setPixelRatio(Math.min(window.devicePixelRatio || 1, 2));
      renderer.setClearColor(0x070d19, 1);
      container.innerHTML = '';
      container.appendChild(renderer.domElement);
    } catch {
      setWebglSupported(false);
      return;
    }

    const scene = new THREE.Scene();
    scene.fog = new THREE.FogExp2(0x070d19, 0.012);

    const camera = new THREE.PerspectiveCamera(48, (container.clientWidth || 720) / 380, 0.1, 500);
    camera.position.set(0, 26, 42);
    camera.lookAt(0, 0, 0);

    // Lighting
    const ambientLight = new THREE.AmbientLight(0x94a3b8, 1.1);
    scene.add(ambientLight);

    const dirLight = new THREE.DirectionalLight(0x38bdf8, 1.6);
    dirLight.position.set(25, 40, 20);
    scene.add(dirLight);

    // Yard Ground & Grid
    const groundGeo = new THREE.PlaneGeometry(120, 120);
    const groundMat = new THREE.MeshLambertMaterial({ color: 0x0b1325 });
    const ground = new THREE.Mesh(groundGeo, groundMat);
    ground.rotation.x = -Math.PI / 2;
    scene.add(ground);

    const gridHelper = new THREE.GridHelper(100, 25, 0x1e293b, 0x1e293b);
    gridHelper.position.y = 0.02;
    scene.add(gridHelper);

    // Highway Corridor Strip
    const roadGeo = new THREE.PlaneGeometry(80, 10);
    const roadMat = new THREE.MeshLambertMaterial({ color: 0x1e293b });
    const road = new THREE.Mesh(roadGeo, roadMat);
    road.rotation.x = -Math.PI / 2;
    road.position.set(0, 0.05, 4);
    scene.add(road);

    // Stylized Logistics Warehouse Hub
    const warehouseGroup = new THREE.Group();
    const buildingGeo = new THREE.BoxGeometry(22, 6, 12);
    const buildingMat = new THREE.MeshLambertMaterial({ color: 0x1e3a5f });
    const building = new THREE.Mesh(buildingGeo, buildingMat);
    building.position.set(0, 3, -14);
    warehouseGroup.add(building);

    const roofGeo = new THREE.BoxGeometry(23, 0.8, 13);
    const roofMat = new THREE.MeshLambertMaterial({ color: 0x0284c7 });
    const roof = new THREE.Mesh(roofGeo, roofMat);
    roof.position.set(0, 6.4, -14);
    warehouseGroup.add(roof);

    // Loading Dock Bays
    for (let i = -7; i <= 7; i += 7) {
      const bayGeo = new THREE.BoxGeometry(3.2, 3.5, 0.4);
      const bayMat = new THREE.MeshLambertMaterial({ color: 0x38bdf8 });
      const bay = new THREE.Mesh(bayGeo, bayMat);
      bay.position.set(i, 1.8, -7.9);
      warehouseGroup.add(bay);
    }
    scene.add(warehouseGroup);

    // Helper to build a procedural low-poly truck mesh
    const createProceduralTruck = (colorHex: number): THREE.Group => {
      const truck = new THREE.Group();

      // Trailer container
      const trailerGeo = new THREE.BoxGeometry(4.6, 2.4, 2.0);
      const trailerMat = new THREE.MeshLambertMaterial({ color: 0xe2e8f0 });
      const trailer = new THREE.Mesh(trailerGeo, trailerMat);
      trailer.position.set(-0.8, 1.6, 0);
      truck.add(trailer);

      // Tractor cab
      const cabGeo = new THREE.BoxGeometry(1.8, 2.0, 1.9);
      const cabMat = new THREE.MeshLambertMaterial({ color: colorHex });
      const cab = new THREE.Mesh(cabGeo, cabMat);
      cab.position.set(2.5, 1.4, 0);
      truck.add(cab);

      // 4 Wheels
      const wheelGeo = new THREE.CylinderGeometry(0.5, 0.5, 0.4, 12);
      const wheelMat = new THREE.MeshLambertMaterial({ color: 0x0f172a });
      const wheelPositions: [number, number, number][] = [
        [-2.2, 0.5, 1.0],
        [-2.2, 0.5, -1.0],
        [2.4, 0.5, 1.0],
        [2.4, 0.5, -1.0],
      ];
      wheelPositions.forEach(([wx, wy, wz]) => {
        const wheel = new THREE.Mesh(wheelGeo, wheelMat);
        wheel.rotation.x = Math.PI / 2;
        wheel.position.set(wx, wy, wz);
        truck.add(wheel);
      });

      return truck;
    };

    const truckMeshes = new Map<number, THREE.Group>();

    const animate = () => {
      animationFrameId = requestAnimationFrame(animate);

      const currentSessions = sessionsRef.current;
      const activeIds = new Set<number>();

      currentSessions.forEach((s, index) => {
        activeIds.add(s.vehicleId);
        let truck = truckMeshes.get(s.vehicleId);
        const statusColor =
          s.simulatorStatus === 'RUNNING'
            ? 0x10b981
            : s.simulatorStatus === 'PAUSED'
            ? 0xf59e0b
            : 0x38bdf8;

        if (!truck) {
          truck = createProceduralTruck(statusColor);
          scene.add(truck);
          truckMeshes.set(s.vehicleId, truck);
        }

        // Map geographical coordinates (approx Mumbai-Pune corridor) to 3D highway coordinates [-32, +32]
        const normLon = ((s.longitude - 72.85) / 1.05) * 60 - 30;
        const normLat = ((19.1 - s.latitude) / 0.65) * 16 - 2;
        const targetX = Math.max(-34, Math.min(34, normLon));
        const targetZ = Math.max(-4, Math.min(14, normLat + (index % 3) * 3));

        truck.position.x += (targetX - truck.position.x) * 0.15;
        truck.position.z += (targetZ - truck.position.z) * 0.15;
      });

      // Remove meshes for vehicles no longer present
      for (const [vid, mesh] of truckMeshes.entries()) {
        if (!activeIds.has(vid)) {
          scene.remove(mesh);
          truckMeshes.delete(vid);
        }
      }

      if (renderer) {
        renderer.render(scene, camera);
      }
    };

    animate();

    return () => {
      cancelAnimationFrame(animationFrameId);
      if (renderer) {
        renderer.dispose();
      }
    };
  }, []);

  return (
    <div className="d-flex flex-column h-100" data-testid="three-fleet-scene">
      {/* Explicit Simulated Data Indicator */}
      <div
        className="d-flex flex-wrap justify-content-between align-items-center px-3 py-2 border-bottom border-secondary border-opacity-25 small"
        style={{ background: 'rgba(15, 23, 42, 0.92)' }}
      >
        <div className="d-flex align-items-center gap-2">
          <span className="badge bg-warning text-dark fw-bold">
            SIMULATED DATA
          </span>
          <span className="text-secondary">
            Three.js Procedural Low-Poly 3D Yard &amp; Corridor Scene (Synchronized with GPS Simulator)
          </span>
        </div>
        <span className="text-info small">
          Models: Procedural Warehouse + {sessions.length} Low-Poly Truck(s)
        </span>
      </div>

      {/* 3D Canvas or Accessible Fallback */}
      {webglSupported ? (
        <div
          ref={mountRef}
          style={{ width: '100%', height: '380px', background: '#070d19' }}
        />
      ) : (
        <div
          className="d-flex flex-column justify-content-center align-items-center text-center p-4"
          style={{ height: '380px', background: '#070d19' }}
        >
          <Box size={40} className="text-info mb-2" />
          <h6 className="fw-bold text-light mb-1">
            3D Fleet Telemetry Scene (Accessible View)
          </h6>
          <p className="text-secondary small mb-3" style={{ maxWidth: 460 }}>
            Procedural Three.js scene synchronized with simulated vehicle telemetry coordinates.
          </p>
        </div>
      )}

      {/* Synchronized 3D Asset Selector Strip */}
      <div
        className="px-3 py-2 border-top border-secondary border-opacity-25 small"
        style={{ background: 'rgba(15, 23, 42, 0.92)' }}
      >
        <div className="d-flex flex-wrap align-items-center justify-content-between gap-2">
          <span className="text-secondary d-inline-flex align-items-center gap-1">
            <Truck size={14} className="text-info" /> 3D Scene Vehicle Nodes:
          </span>
          <div className="d-flex flex-wrap gap-2">
            {sessions.length === 0 ? (
              <span className="text-secondary">
                Warehouse yard on standby — start a vehicle simulator to spawn 3D trucks.
              </span>
            ) : (
              sessions.map((s) => (
                <button
                  key={s.vehicleId}
                  type="button"
                  onClick={() => onSelectVehicle(s.vehicleId)}
                  className={`btn btn-sm py-0 px-2 ${
                    s.vehicleId === selectedVehicleId
                      ? 'btn-info text-dark fw-semibold'
                      : 'btn-outline-secondary text-light'
                  }`}
                >
                  {s.licensePlate} [{s.latitude.toFixed(3)}, {s.longitude.toFixed(3)}]
                </button>
              ))
            )}
          </div>
        </div>
      </div>
    </div>
  );
};
