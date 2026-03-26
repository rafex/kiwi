# Tablero de Tareas - Kiwi Project

> _Archivo de seguimiento de tareas entre sesiones_

## Activo

### ✅ Completadas

#### Integración de Testcontainers (2026-03-25)
- [x] Crear worktree para integración de Testcontainers
- [x] Agregar dependencias de Testcontainers 1.19.7 al pom.xml
- [x] Crear clase base `BasePostgresTest.java` para tests con PostgreSQL
- [x] Refactorizar `DatabaseConfigTest.java` para usar Testcontainers
- [x] Ejecutar tests y verificar que pasen (51/51 tests)
- [x] Formatear código con Spotless
- [x] Hacer commit de los cambios
- [x] Actualizar documentación (AGENTS.md y docs/testing.md)
- [x] Hacer push de los cambios al repositorio

### 🔄 En Progreso

### 📋 Pendientes

#### Extensión de Testcontainers a otros módulos
- [ ] Aplicar estrategia de Testcontainers a `kiwi-infra-postgres`
- [ ] Crear tests de integración para repositorios existentes
- [ ] Configurar CI/CD para ejecutar tests con Testcontainers

#### Mejoras de BasePostgresTest
- [ ] Agregar método `getConnection()` para acceso directo a la base de datos
- [ ] Agregar método `executeSql()` para ejecutar scripts SQL
- [ ] Implementar limpieza automática de datos entre tests

#### Documentación adicional
- [ ] Actualizar `context/architecture.md` para enlazar a la documentación de testing
- [ ] Crear ejemplos de tests de integración para otros módulos
- [ ] Documentar configuración de CI/CD para Testcontainers

## Referencias

- **Commit actual**: `fedde9e` - docs: actualizar documentación con Testcontainers
- **Rama principal**: `main`
- **Última integración**: Testcontainers para PostgreSQL (2026-03-25)