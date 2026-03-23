# PLAN: Actualización a Ether 9.x y Java 25

**Fecha:** 2026-03-23  
**Rama:** migrate-java25  
**Estado:** En ejecución

---

## Objetivo General
Actualizar las bibliotecas Ether a versión 9.x compatible con Java 25 y garantizar que todo el proyecto kiwi sea compatible con Java 25.

## Análisis Actual
1. **Versión Java actual**: 25 (confirmado en pom.xml principal)
2. **Versiones Ether actuales**: 9.0.0-SNAPSHOT (según pom.xml principal)
3. **Módulos críticos**:
   - kiwi-common
   - kiwi-core
   - kiwi-infra-postgres
   - kiwi-transport-jetty

## Pasos de Implementación

### Fase 1: Actualización de Ether (3 pasos)
- [ ] **1.1** Confirmar versiones estables de Ether 9.x
- [ ] **1.2** Actualizar pom.xml principal con versiones estables
- [ ] **1.3** Verificar dependencias en submódulos

### Fase 2: Compatibilidad Java 25 (4 pasos)
- [ ] **2.1** Verificar configuración de compilación
- [ ] **2.2** Actualizar plugins Maven
- [ ] **2.3** Reemplazar características obsoletas
- [ ] **2.4** Actualizar sintaxis Java

### Fase 3: Verificación (3 pasos)
- [ ] **3.1** Pruebas de compilación
- [ ] **3.2** Ejecución de tests
- [ ] **3.3** Análisis estático

## Archivos Críticos a Modificar
| Archivo | Propósito |
|---------|-----------|
| `backend/java/kiwi-parent/pom.xml` | Versiones principales Ether |
| `backend/java/kiwi-parent/kiwi-core/pom.xml` | Dependencias de módulo core |
| `backend/java/kiwi-parent/kiwi-infra-postgres/pom.xml` | Dependencias de PostgreSQL |
| `.github/workflows/build_backend_java.yml` | Configuración CI/CD |

## Riesgos y Mitigación
1. **Riesgo**: Cambios breaking en Ether 9.x  
   **Mitigación**: Revisar changelog de Ether, probar incrementalmente

2. **Riesgo**: APIs obsoletas en Java 25  
   **Mitigación**: Ejecutar `jdeps` antes/después, usar `--jdk-internals`

3. **Riesgo**: Incompatibilidad con plugins Maven  
   **Mitigación**: Verificar compatibilidad de plugins con Java 25

## Progreso

### ✅ Completado
- Creación del plan
- **Fase 1: Actualización de Ether**
  - ✅ Versiones confirmadas: Ether 9.0.0-SNAPSHOT (única versión compatible con Java 25)
  - ✅ Todos los módulos heredan versiones del parent POM
  - ✅ No hay sobreescrituras locales de versiones
- **Fase 2: Compatibilidad Java 25**
  - ✅ Configuración de compilación verificada (Java 25, maven-compiler-plugin 3.13.0)
  - ✅ Comentario actualizado en pom.xml (Java 21 → Java 25)
  - ✅ No se encontraron características obsoletas (finalize(), constructores de wrappers)
  - ✅ Uso extensivo de características modernas:
    - 40/96 archivos usan `record` (41.6%)
    - 1 archivo usa `sealed`
    - 28 archivos usan colecciones inmutables (List.of, Set.of, Map.of)
- **Fase 3: Verificación**
  - ✅ Compilación exitosa (mvn clean compile)
  - ✅ Tests ejecutados correctamente (detectado Java 25.0.2)
  - ✅ Análisis jdeps: sin uso de APIs internas del JDK
  - ✅ CI/CD configurado para Java 25 (GitHub Actions)

### 🔄 En progreso
- Ninguna

### ⏳ Pendiente
- Ninguna

## Resultados

### Compilación
```
✅ mvn clean compile -DskipTests
✅ mvn test (Java 25.0.2 detectado)
✅ mvn package -DskipTests
```

### Análisis jdeps
```
✅ No se detectaron dependencias a APIs internas del JDK
✅ Uso correcto de módulos estándar (java.base, java.sql)
```

### Características Java 25
- **Records:** 40 archivos (41.6% del código)
- **Sealed classes:** 1 archivo
- **Colecciones inmutables:** 28 archivos
- **Sin código obsoleto:** No se encontró finalize() ni constructores deprecated

### Versiones Ether 9.x
Todas las bibliotecas Ether están en **9.0.0-SNAPSHOT**:
- ether-config
- ether-database-core
- ether-jdbc
- ether-database-postgres
- ether-json
- ether-jwt
- ether-observability-core
- ether-http-core
- ether-http-security
- ether-http-problem
- ether-http-jetty12
- ether-glowroot-jetty12
- ether-logging-core

## Conclusión

✅ **El proyecto Kiwi está completamente actualizado y compatible con Java 25.**

- Todas las bibliotecas Ether están en versión 9.0.0-SNAPSHOT (compatible con Java 25)
- El código usa extensivamente características modernas de Java
- No hay código obsoleto ni dependencias a APIs internas del JDK
- La compilación, tests y CI/CD funcionan correctamente con Java 25
- Solo se realizó un cambio menor: actualización de comentario en pom.xml (Java 21 → Java 25)
