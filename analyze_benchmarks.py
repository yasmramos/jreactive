#!/usr/bin/env python3
"""
Análisis Avanzado de Resultados de Benchmarks JMH
Compara nuestra implementación vs RxJava con análisis de escalabilidad
"""

import json
import sys
from collections import defaultdict
from dataclasses import dataclass
from typing import List, Dict, Tuple
import statistics

@dataclass
class BenchmarkResult:
    benchmark: str
    category: str
    impl_type: str  # 'our' or 'rxJava'
    size: int
    score: float
    error: float
    unit: str

def load_results(filename: str) -> List[BenchmarkResult]:
    """Carga resultados del archivo JSON de JMH"""
    with open(filename, 'r') as f:
        data = json.load(f)
    
    results = []
    for item in data:
        benchmark_full = item['benchmark']
        # Formato: com.reactive.benchmarks.CategoryBenchmark.methodName
        parts = benchmark_full.split('.')
        category = parts[-2].replace('Benchmark', '')
        method_name = parts[-1]
        
        # Determinar tipo de implementación
        if method_name.startswith('our'):
            impl_type = 'our'
        elif method_name.startswith('rxJava'):
            impl_type = 'rxJava'
        else:
            impl_type = 'unknown'
        
        # Extraer size del parámetro
        size = 100  # default
        if 'params' in item and 'size' in item['params']:
            size = int(item['params']['size'])
        
        results.append(BenchmarkResult(
            benchmark=method_name,
            category=category,
            impl_type=impl_type,
            size=size,
            score=item['primaryMetric']['score'],
            error=item['primaryMetric']['scoreError'],
            unit=item['primaryMetric']['scoreUnit']
        ))
    
    return results

def group_by_category(results: List[BenchmarkResult]) -> Dict[str, List[BenchmarkResult]]:
    """Agrupa resultados por categoría"""
    grouped = defaultdict(list)
    for r in results:
        grouped[r.category].append(r)
    return dict(grouped)

def compare_implementations(results: List[BenchmarkResult]) -> Dict[str, Dict]:
    """Compara nuestra implementación vs RxJava para el mismo benchmark y size"""
    comparisons = defaultdict(lambda: defaultdict(dict))
    
    # Agrupar por benchmark base (sin prefijo our/rxJava)
    for r in results:
        base_name = r.benchmark.replace('our', '').replace('rxJava', '')
        key = f"{r.category}.{base_name}"
        comparisons[key][r.size][r.impl_type] = r
    
    return comparisons

def calculate_speedup(our_score: float, rxjava_score: float) -> float:
    """Calcula el speedup (valores más altos = mejor)"""
    if rxjava_score == 0:
        return float('inf')
    return (our_score / rxjava_score - 1) * 100  # Porcentaje de mejora

def analyze_scalability(results: List[BenchmarkResult]) -> Dict[str, Dict]:
    """Analiza cómo escala el rendimiento con diferentes tamaños"""
    scalability = defaultdict(lambda: defaultdict(list))
    
    for r in results:
        key = f"{r.category}.{r.benchmark}"
        scalability[key]['sizes'].append(r.size)
        scalability[key]['scores'].append(r.score)
    
    # Calcular degradación
    for key in scalability:
        sizes = scalability[key]['sizes']
        scores = scalability[key]['scores']
        
        if len(sizes) > 1:
            # Ordenar por size
            sorted_data = sorted(zip(sizes, scores))
            scalability[key]['sorted_sizes'] = [s for s, _ in sorted_data]
            scalability[key]['sorted_scores'] = [score for _, score in sorted_data]
            
            # Calcular factor de degradación entre tamaños consecutivos
            degradations = []
            for i in range(1, len(sorted_data)):
                prev_size, prev_score = sorted_data[i-1]
                curr_size, curr_score = sorted_data[i]
                
                size_ratio = curr_size / prev_size
                score_ratio = prev_score / curr_score if curr_score > 0 else float('inf')
                
                # Degradación normalizada
                normalized_degradation = score_ratio / size_ratio
                degradations.append(normalized_degradation)
            
            scalability[key]['degradations'] = degradations
            scalability[key]['avg_degradation'] = statistics.mean(degradations) if degradations else 0
    
    return scalability

def print_category_summary(category: str, results: List[BenchmarkResult], comparisons: Dict):
    """Imprime resumen de una categoría"""
    print(f"\n{'='*80}")
    print(f"CATEGORÍA: {category.upper()}")
    print(f"{'='*80}\n")
    
    # Agrupar por benchmark base
    benchmarks = defaultdict(list)
    for r in results:
        base_name = r.benchmark.replace('our', '').replace('rxJava', '')
        benchmarks[base_name].append(r)
    
    for bench_name in sorted(benchmarks.keys()):
        bench_results = benchmarks[bench_name]
        
        print(f"\n{bench_name}:")
        print(f"{'-'*80}")
        
        # Agrupar por size
        by_size = defaultdict(lambda: {'our': None, 'rxJava': None})
        for r in bench_results:
            by_size[r.size][r.impl_type] = r
        
        # Header
        print(f"{'Size':<10} {'Nuestra':<20} {'RxJava':<20} {'Speedup':<15} {'Ganador'}")
        print(f"{'-'*80}")
        
        for size in sorted(by_size.keys()):
            our = by_size[size]['our']
            rxjava = by_size[size]['rxJava']
            
            our_str = f"{our.score:,.0f} ops/ms" if our else "N/A"
            rxjava_str = f"{rxjava.score:,.0f} ops/ms" if rxjava else "N/A"
            
            if our and rxjava:
                speedup = calculate_speedup(our.score, rxjava.score)
                speedup_str = f"{speedup:+.1f}%"
                winner = "🚀 NUESTRA" if speedup > 0 else "⚠️  RxJava"
            else:
                speedup_str = "N/A"
                winner = ""
            
            print(f"{size:<10} {our_str:<20} {rxjava_str:<20} {speedup_str:<15} {winner}")

def print_overall_summary(all_comparisons: Dict):
    """Imprime resumen general de todas las comparaciones"""
    print(f"\n{'='*80}")
    print(f"RESUMEN GENERAL DE RENDIMIENTO")
    print(f"{'='*80}\n")
    
    wins = {'our': 0, 'rxJava': 0, 'tie': 0}
    speedups = []
    
    for bench_key, sizes_data in all_comparisons.items():
        for size, impls in sizes_data.items():
            if 'our' in impls and 'rxJava' in impls:
                our_score = impls['our'].score
                rxjava_score = impls['rxJava'].score
                
                speedup = calculate_speedup(our_score, rxjava_score)
                speedups.append(speedup)
                
                if speedup > 5:
                    wins['our'] += 1
                elif speedup < -5:
                    wins['rxJava'] += 1
                else:
                    wins['tie'] += 1
    
    total = sum(wins.values())
    
    print(f"Total de comparaciones: {total}")
    print(f"\nResultados:")
    print(f"  🚀 Nuestra implementación gana:  {wins['our']:3d} ({wins['our']/total*100:.1f}%)")
    print(f"  ⚠️  RxJava gana:                  {wins['rxJava']:3d} ({wins['rxJava']/total*100:.1f}%)")
    print(f"  🤝 Empate (±5%):                 {wins['tie']:3d} ({wins['tie']/total*100:.1f}%)")
    
    if speedups:
        print(f"\nEstadísticas de Speedup:")
        print(f"  Promedio:  {statistics.mean(speedups):+.1f}%")
        print(f"  Mediana:   {statistics.median(speedups):+.1f}%")
        print(f"  Máximo:    {max(speedups):+.1f}%")
        print(f"  Mínimo:    {min(speedups):+.1f}%")

def print_scalability_analysis(scalability: Dict):
    """Analiza y presenta patrones de escalabilidad"""
    print(f"\n{'='*80}")
    print(f"ANÁLISIS DE ESCALABILIDAD")
    print(f"{'='*80}\n")
    
    print("Benchmarks con mejor escalabilidad (menor degradación al aumentar tamaño):\n")
    
    # Filtrar solo benchmarks con datos de escalabilidad
    scalable_benchmarks = {k: v for k, v in scalability.items() 
                          if 'avg_degradation' in v and len(v.get('sizes', [])) > 1}
    
    if not scalable_benchmarks:
        print("No hay suficientes datos de escalabilidad disponibles.")
        return
    
    # Ordenar por degradación promedio (menor = mejor)
    sorted_benchmarks = sorted(scalable_benchmarks.items(), 
                               key=lambda x: x[1]['avg_degradation'])
    
    print(f"{'Benchmark':<50} {'Degradación Promedio':<25}")
    print(f"{'-'*80}")
    
    for bench_name, data in sorted_benchmarks[:20]:  # Top 20
        bench_short = bench_name.split('.')[-1]
        category = bench_name.split('.')[0]
        
        degradation = data['avg_degradation']
        rating = "⭐⭐⭐" if degradation < 1.2 else "⭐⭐" if degradation < 2.0 else "⭐"
        
        print(f"{category}/{bench_short:<45} {degradation:.2f}x {rating}")
    
    # Benchmarks con peor escalabilidad
    print(f"\n\nBenchmarks que necesitan optimización de escalabilidad:\n")
    print(f"{'Benchmark':<50} {'Degradación Promedio':<25}")
    print(f"{'-'*80}")
    
    for bench_name, data in sorted_benchmarks[-10:]:  # Bottom 10
        bench_short = bench_name.split('.')[-1]
        category = bench_name.split('.')[0]
        
        degradation = data['avg_degradation']
        print(f"{category}/{bench_short:<45} {degradation:.2f}x ⚠️")

def identify_optimization_opportunities(all_comparisons: Dict) -> List[Tuple[str, float]]:
    """Identifica los benchmarks donde RxJava es significativamente más rápido"""
    opportunities = []
    
    for bench_key, sizes_data in all_comparisons.items():
        for size, impls in sizes_data.items():
            if 'our' in impls and 'rxJava' in impls:
                our_score = impls['our'].score
                rxjava_score = impls['rxJava'].score
                
                speedup = calculate_speedup(our_score, rxjava_score)
                
                # Si RxJava es >20% más rápido, es oportunidad de optimización
                if speedup < -20:
                    opportunities.append((f"{bench_key} (size={size})", speedup))
    
    return sorted(opportunities, key=lambda x: x[1])

def print_optimization_opportunities(opportunities: List[Tuple[str, float]]):
    """Imprime áreas prioritarias de optimización"""
    print(f"\n{'='*80}")
    print(f"OPORTUNIDADES DE OPTIMIZACIÓN")
    print(f"{'='*80}\n")
    
    if not opportunities:
        print("¡Excelente! No hay áreas donde RxJava sea significativamente más rápido.")
        return
    
    print("Áreas donde RxJava es >20% más rápido (prioridad de optimización):\n")
    print(f"{'Benchmark':<60} {'Gap de Rendimiento'}")
    print(f"{'-'*80}")
    
    for bench, speedup in opportunities:
        priority = "🔴 ALTA" if speedup < -50 else "🟡 MEDIA" if speedup < -35 else "🟢 BAJA"
        print(f"{bench:<60} {speedup:+.1f}% {priority}")

def main():
    if len(sys.argv) < 2:
        print("Uso: python analyze_benchmarks.py <archivo_resultados.json>")
        sys.exit(1)
    
    filename = sys.argv[1]
    
    print("="*80)
    print("ANÁLISIS DE BENCHMARKS - Reactive Java vs RxJava")
    print("="*80)
    
    # Cargar resultados
    print(f"\nCargando resultados de {filename}...")
    results = load_results(filename)
    print(f"✓ Cargados {len(results)} resultados de benchmarks")
    
    # Agrupar por categoría
    by_category = group_by_category(results)
    print(f"✓ Encontradas {len(by_category)} categorías")
    
    # Comparaciones
    all_comparisons = compare_implementations(results)
    
    # Análisis de escalabilidad
    scalability = analyze_scalability(results)
    
    # Imprimir resumen por categoría
    for category in sorted(by_category.keys()):
        print_category_summary(category, by_category[category], all_comparisons)
    
    # Resumen general
    print_overall_summary(all_comparisons)
    
    # Análisis de escalabilidad
    print_scalability_analysis(scalability)
    
    # Oportunidades de optimización
    opportunities = identify_optimization_opportunities(all_comparisons)
    print_optimization_opportunities(opportunities)
    
    print(f"\n{'='*80}")
    print("ANÁLISIS COMPLETADO")
    print(f"{'='*80}\n")

if __name__ == '__main__':
    main()
