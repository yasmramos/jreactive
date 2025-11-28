#!/bin/bash

echo "=== Compilando Reactive Java ==="

# Crear directorio de salida
mkdir -p build/classes

# Compilar todos los archivos fuente
echo "Compilando código principal..."
find src/main/java -name "*.java" > sources.txt
javac -d build/classes @sources.txt

if [ $? -eq 0 ]; then
    echo "✓ Compilación exitosa!"
    
    # Compilar ejemplos
    echo "Compilando ejemplos..."
    find src/examples/java -name "*.java" > examples.txt
    javac -cp build/classes -d build/classes @examples.txt
    
    if [ $? -eq 0 ]; then
        echo "✓ Ejemplos compilados!"
        
        # Ejecutar ejemplos básicos
        echo ""
        echo "=== Ejecutando Ejemplos Básicos ==="
        java -cp build/classes com.reactive.examples.BasicExamples
        
    else
        echo "✗ Error compilando ejemplos"
        exit 1
    fi
else
    echo "✗ Error en compilación"
    exit 1
fi

# Limpiar archivos temporales
rm -f sources.txt examples.txt
