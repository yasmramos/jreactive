#!/usr/bin/env python3
"""
Monitor de progreso de benchmarks JMH
Mantiene la sesión activa y reporta el avance
"""

import time
import subprocess
import re
import os
from datetime import datetime

def count_completed_benchmarks(log_output):
    """Cuenta cuántos benchmarks se han completado"""
    # Buscar patrones como "# Run complete" o benchmarks completados
    completed = len(re.findall(r'Benchmark\s+Mode\s+Cnt\s+Score', log_output))
    return completed

def get_current_benchmark(log_output):
    """Extrae el benchmark que se está ejecutando actualmente"""
    # Buscar el último "# Warmup" o "# Run"
    warmup_match = re.findall(r'# Warmup Iteration\s+\d+:', log_output)
    run_match = re.findall(r'# Measurement:\s+\d+', log_output)
    
    # Buscar el nombre del benchmark actual
    bench_match = re.findall(r'# Benchmark:\s+(.+)', log_output)
    
    if bench_match:
        current = bench_match[-1]
        if warmup_match:
            return f"{current} (Warmup)"
        elif run_match:
            return f"{current} (Medición)"
        else:
            return current
    return "Inicializando..."

def estimate_progress(completed_count, total_benchmarks=90):
    """Estima el porcentaje de progreso"""
    return min(100, int((completed_count / total_benchmarks) * 100))

def main():
    """Monitorea el proceso de benchmarks y reporta progreso"""
    
    print("=== Monitor de Benchmarks JMH ===")
    print(f"Inicio: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    print("-" * 60)
    
    iteration = 0
    last_completed = 0
    
    while True:
        iteration += 1
        
        try:
            # Verificar si el proceso sigue corriendo
            result = subprocess.run(
                ['pgrep', '-f', 'jmh'],
                capture_output=True,
                text=True
            )
            
            if not result.stdout.strip():
                print("\n⚠️  Proceso JMH no encontrado")
                break
            
            # Leer el log parcial si existe
            log_file = "/workspace/benchmark_results_full.json"
            
            # Intentar leer salida del proceso
            try:
                proc_output = subprocess.run(
                    ['ps', 'aux'],
                    capture_output=True,
                    text=True,
                    timeout=5
                )
                
                # Buscar archivos de log temporal de Maven
                maven_log = ""
                if os.path.exists("/workspace/jreactive/target/jmh-result.json"):
                    with open("/workspace/jreactive/target/jmh-result.json", 'r') as f:
                        maven_log = f.read()
                
            except Exception as e:
                maven_log = ""
            
            # Reportar cada 3 iteraciones (~ cada 3 minutos)
            if iteration % 3 == 0:
                print(f"\n[{datetime.now().strftime('%H:%M:%S')}] Iteración {iteration}")
                print(f"  ✓ Proceso JMH activo (PID: {result.stdout.strip()})")
                
                if os.path.exists(log_file) and os.path.getsize(log_file) > 100:
                    size_kb = os.path.getsize(log_file) / 1024
                    print(f"  ✓ Archivo de resultados: {size_kb:.1f} KB")
                else:
                    print(f"  ⏳ Compilando y ejecutando benchmarks...")
                
                # Mantener sesión activa
                print(f"  🔄 Sesión activa - Monitoreando continuamente...")
            
            # Esperar 1 minuto antes de la siguiente verificación
            time.sleep(60)
            
        except KeyboardInterrupt:
            print("\n\n❌ Monitoreo detenido por el usuario")
            break
        except Exception as e:
            print(f"\n⚠️  Error en monitoreo: {e}")
            time.sleep(60)
    
    print("\n" + "=" * 60)
    print(f"Fin del monitoreo: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")

if __name__ == "__main__":
    main()
