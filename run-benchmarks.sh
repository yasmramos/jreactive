#!/bin/bash
# Script helper para ejecutar benchmarks de Reactive Java

set -e

JAR_FILE="target/benchmarks.jar"
DEFAULT_OPTIONS="-wi 3 -i 5 -f 1"

# Colores para output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}  Reactive Java Benchmarks Runner${NC}"
echo -e "${BLUE}========================================${NC}"
echo

# Verificar si el JAR existe
if [ ! -f "$JAR_FILE" ]; then
    echo -e "${YELLOW}JAR de benchmarks no encontrado. Compilando...${NC}"
    mvn clean package -Pbenchmarks -DskipTests
    echo
fi

# Función para mostrar el menú
show_menu() {
    echo -e "${GREEN}Selecciona qué benchmarks ejecutar:${NC}"
    echo "1) Todos los benchmarks (puede tomar horas)"
    echo "2) BasicOperatorsBenchmark - Operadores básicos"
    echo "3) CreationBenchmark - Creación de observables"
    echo "4) SubjectsBenchmark - Subjects (Hot Observables)"
    echo "5) AdvancedOperatorsBenchmark - Operadores avanzados"
    echo "6) CombinationOperatorsBenchmark - Operadores de combinación"
    echo "7) ErrorHandlingBenchmark - Manejo de errores"
    echo "8) Test rápido (1 warmup, 2 iterations, size=100)"
    echo "9) Personalizado (introducir patrón)"
    echo "0) Salir"
    echo
}

# Función para ejecutar benchmark
run_benchmark() {
    local pattern=$1
    local options=${2:-$DEFAULT_OPTIONS}
    
    echo -e "${BLUE}Ejecutando: java -jar $JAR_FILE \"$pattern\" $options${NC}"
    echo
    java -jar "$JAR_FILE" "$pattern" $options
}

# Loop principal
while true; do
    show_menu
    read -p "Opción: " choice
    echo
    
    case $choice in
        1)
            echo -e "${YELLOW}⚠️  ADVERTENCIA: Esto puede tomar varias horas${NC}"
            read -p "¿Continuar? (s/n): " confirm
            if [ "$confirm" = "s" ] || [ "$confirm" = "S" ]; then
                run_benchmark ".*"
            fi
            ;;
        2)
            run_benchmark "BasicOperatorsBenchmark"
            ;;
        3)
            run_benchmark "CreationBenchmark"
            ;;
        4)
            run_benchmark "SubjectsBenchmark"
            ;;
        5)
            run_benchmark "AdvancedOperatorsBenchmark"
            ;;
        6)
            run_benchmark "CombinationOperatorsBenchmark"
            ;;
        7)
            run_benchmark "ErrorHandlingBenchmark"
            ;;
        8)
            echo -e "${GREEN}Test rápido para verificar que todo funciona${NC}"
            run_benchmark "CreationBenchmark.(ourJust|rxJavaJust)" "-wi 1 -i 2 -f 1 -p size=100"
            ;;
        9)
            read -p "Introduce el patrón de benchmark (ej: BasicOperatorsBenchmark.ourMap): " pattern
            read -p "Opciones JMH (Enter para default: $DEFAULT_OPTIONS): " custom_options
            run_benchmark "$pattern" "${custom_options:-$DEFAULT_OPTIONS}"
            ;;
        0)
            echo -e "${GREEN}¡Hasta luego!${NC}"
            exit 0
            ;;
        *)
            echo -e "${YELLOW}Opción inválida${NC}"
            ;;
    esac
    
    echo
    echo -e "${GREEN}Benchmark completado. Presiona Enter para continuar...${NC}"
    read
    echo
done
