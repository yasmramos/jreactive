# ConnectableObservable - Limitaciones Conocidas

## Estado Actual
- **Tests Pasando**: 12/16 (75%)
- **Tests Fallando**: 4/16 (25%)

## Funcionalidad Operativa ✅
- ✅ Multicasting con `publish()` - funciona correctamente para subscriptores concurrentes
- ✅ Buffering con `replay()` - funciona correctamente
- ✅ Auto-conexión con `autoConnect()` - funciona perfectamente
- ✅ Manejo de errores - funciona correctamente
- ✅ Disposal y limpieza de recursos - funciona correctamente
- ✅ Múltiples observers concurrentes - funciona perfectamente

## Limitaciones Conocidas  ⚠️

### 1. Reconexión Secuencial con Sources Síncronos
**Afecta a**: `refCount()`, `share()`

**Problema**: Cuando un Observable completa y luego un nuevo observer se suscribe (reconexión secuencial), el nuevo observer no recibe valores si el source es síncrono (ej: `Observable.just()`).

**Causa Raíz**: Race condition entre la creación del Subject fresco y la suscripción del observer. Con sources síncronos, los valores se emiten instantáneamente al conectar, antes de que el observer esté completamente configurado.

**Tests Afectados**:
- `testRefCountBasic` (línea 214) - segundo observer no recibe valores
- `testShareBasic` (línea 349) - mismo comportamiento

**Workaround**: Funciona correctamente con:
- Sources asíncronos (con delays)
- Subscriptores concurrentes (múltiples observers antes de complete)
- Uso directo de `publish().connect()` sin `refCount()`

### 2. Reconexión con Múltiples Observers Concurrentes
**Problema**: En el test `testRefCountMultipleObservers`, se conecta 2 veces en lugar de 1.

**Causa**: Interacción compleja entre el conteo de subscribers y el estado de conexión durante reconexión.

**Test Afectado**:
- `testRefCountMultipleObservers` (línea 257)

### 3. Disposal de Upstream en RefCount
**Problema**: El upstream no se dispone correctamente en ciertos escenarios de reconexión.

**Test Afectado**:
- `testRefCountDispose` (línea 322)

## Solución Requerida
Estos problemas requieren refactorización arquitectural mayor:
- Implementar patrón Subject Factory para garantizar subjects frescos
- Usar mecanismos de coordinación más sofisticados (latches, barriers)
- Posiblemente rediseñar el ciclo de vida de ConnectableObservable

## Recomendación
Para casos de uso de producción:
1. Usar `publish().autoConnect()` para escenarios donde no se necesita auto-disconnect
2. Usar `replay()` con buffering para garantizar que los late subscribers reciban valores
3. Para sources síncronos con `refCount()`, convertirlos a asíncronos con `observeOn()`

## Archivos Afectados
- `ObservablePublish.java`
- `ObservableReplay.java`
- `ObservableRefCount.java`
- `ConnectableObservableTest.java`

---
*Documento creado: 2025-11-26*
*Implementación: 671 LOC | Tests: 480 LOC | Total: 1,151 LOC*
