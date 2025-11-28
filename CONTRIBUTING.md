# Contributing to JReactive

¡Gracias por tu interés en contribuir a JReactive! 🎉

## Cómo Contribuir

### Reportar Bugs

Si encuentras un bug, por favor abre un issue con:
- Descripción clara del problema
- Pasos para reproducirlo
- Comportamiento esperado vs actual
- Versión de Java y JReactive
- Stack trace si es aplicable

### Sugerir Nuevas Características

Para sugerir nuevas características:
1. Abre un issue describiendo la característica
2. Explica el caso de uso
3. Proporciona ejemplos de código si es posible

### Pull Requests

1. **Fork el repositorio**
   ```bash
   git clone https://github.com/yasmramos/jreactive.git
   cd jreactive
   ```

2. **Crea una rama para tu feature**
   ```bash
   git checkout -b feature/mi-nueva-caracteristica
   ```

3. **Haz tus cambios**
   - Escribe código limpio y bien documentado
   - Añade tests para nuevas funcionalidades
   - Asegúrate de que todos los tests pasan: `mvn test`
   - Sigue el estilo de código existente

4. **Commit tus cambios**
   ```bash
   git commit -m "Add: descripción clara del cambio"
   ```
   
   Prefijos de commit sugeridos:
   - `Add:` para nuevas características
   - `Fix:` para correcciones de bugs
   - `Docs:` para cambios en documentación
   - `Test:` para añadir o modificar tests
   - `Refactor:` para refactorización de código

5. **Push a tu fork**
   ```bash
   git push origin feature/mi-nueva-caracteristica
   ```

6. **Abre un Pull Request**
   - Describe los cambios realizados
   - Referencia issues relacionados
   - Asegúrate de que el CI pasa

## Estándares de Código

### Java Code Style
- Usa Java 17+ features cuando sea apropiado
- Sigue las convenciones de nombres de Java
- Mantén métodos cortos y enfocados
- Escribe código auto-documentado

### JavaDoc
- Todos los métodos públicos deben tener JavaDoc
- Incluye ejemplos de uso cuando sea útil
- Documenta parámetros, retornos y excepciones

### Tests
- Escribe tests unitarios para nuevas funcionalidades
- Mantén cobertura de tests alta
- Usa nombres descriptivos para tests
- Sigue el patrón Arrange-Act-Assert

## Estructura del Proyecto

```
jreactive/
├── src/
│   ├── main/java/com/reactive/
│   │   ├── core/              # Tipos reactivos principales
│   │   ├── operators/         # Implementaciones de operadores
│   │   ├── schedulers/        # Sistema de schedulers
│   │   └── testing/           # Utilidades de testing
│   └── test/java/com/reactive/
│       ├── core/              # Tests de tipos reactivos
│       ├── operators/         # Tests de operadores
│       └── schedulers/        # Tests de schedulers
├── docs/                      # Documentación de usuario
└── pom.xml
```

## Proceso de Review

1. Un mantenedor revisará tu PR
2. Puede solicitar cambios o aclaraciones
3. Una vez aprobado, se mergeará a main
4. Tu contribución aparecerá en la próxima release

## Código de Conducta

- Sé respetuoso y profesional
- Acepta críticas constructivas
- Enfócate en lo mejor para el proyecto
- Ayuda a otros contribuidores

## Preguntas

Si tienes preguntas, puedes:
- Abrir un issue con la etiqueta "question"
- Contactar a los mantenedores

## Licencia

Al contribuir, aceptas que tus contribuciones estarán bajo la licencia MIT del proyecto.

---

¡Gracias por hacer JReactive mejor! 🚀
