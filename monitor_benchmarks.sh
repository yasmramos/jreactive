#!/bin/bash

# Script de monitoreo de benchmarks JMH
# Monitorea el progreso y notifica cuando completa

RESULTS_FILE="/workspace/jreactive/benchmark_results_full.json"
LOG_FILE="/workspace/jreactive/benchmark_monitor.log"
CHECK_INTERVAL=300  # 5 minutos

echo "==================================================================" | tee -a "$LOG_FILE"
echo "Monitor de Benchmarks Iniciado: $(date)" | tee -a "$LOG_FILE"
echo "==================================================================" | tee -a "$LOG_FILE"
echo "" | tee -a "$LOG_FILE"

while true; do
    TIMESTAMP=$(date '+%Y-%m-%d %H:%M:%S')
    
    # Verificar si el proceso JMH está corriendo
    if ! pgrep -f "benchmarks.jar" > /dev/null; then
        echo "[$TIMESTAMP] ⚠️  Proceso JMH no encontrado" | tee -a "$LOG_FILE"
        
        # Verificar si el archivo de resultados existe
        if [ -f "$RESULTS_FILE" ]; then
            RESULT_COUNT=$(jq '. | length' "$RESULTS_FILE" 2>/dev/null || echo "0")
            echo "[$TIMESTAMP] ✅ Archivo de resultados encontrado con $RESULT_COUNT benchmarks" | tee -a "$LOG_FILE"
            echo "[$TIMESTAMP] 🎉 BENCHMARKS COMPLETADOS!" | tee -a "$LOG_FILE"
            echo "" | tee -a "$LOG_FILE"
            echo "Ejecutando análisis automático..." | tee -a "$LOG_FILE"
            
            # Ejecutar análisis
            cd /workspace/jreactive
            python3 analyze_benchmarks.py "$RESULTS_FILE" | tee benchmark_analysis_report.txt
            
            echo "" | tee -a "$LOG_FILE"
            echo "Análisis guardado en: benchmark_analysis_report.txt" | tee -a "$LOG_FILE"
            break
        else
            echo "[$TIMESTAMP] ⚠️  Archivo de resultados no encontrado. Esperando..." | tee -a "$LOG_FILE"
        fi
    else
        # Proceso está corriendo
        PID=$(pgrep -f "benchmarks.jar")
        CPU_USAGE=$(ps -p "$PID" -o %cpu= 2>/dev/null || echo "N/A")
        MEM_USAGE=$(ps -p "$PID" -o %mem= 2>/dev/null || echo "N/A")
        
        echo "[$TIMESTAMP] 🔄 Benchmarks ejecutándose (PID: $PID)" | tee -a "$LOG_FILE"
        echo "[$TIMESTAMP]    CPU: ${CPU_USAGE}% | MEM: ${MEM_USAGE}%" | tee -a "$LOG_FILE"
        
        # Verificar progreso si el archivo JSON existe
        if [ -f "$RESULTS_FILE" ]; then
            RESULT_COUNT=$(jq '. | length' "$RESULTS_FILE" 2>/dev/null || echo "0")
            echo "[$TIMESTAMP]    Resultados parciales: $RESULT_COUNT benchmarks completados" | tee -a "$LOG_FILE"
        fi
    fi
    
    echo "" | tee -a "$LOG_FILE"
    sleep "$CHECK_INTERVAL"
done

echo "==================================================================" | tee -a "$LOG_FILE"
echo "Monitor finalizado: $(date)" | tee -a "$LOG_FILE"
echo "==================================================================" | tee -a "$LOG_FILE"
