# Decisiones Arquitectónicas

Este directorio contiene el registro de decisiones arquitectónicas (ADRs) del proyecto Kiwi.

## ADRs Disponibles

- [001-hexagonal-architecture.md](001-hexagonal-architecture.md): Decisión de adoptar arquitectura hexagonal.
- [002-jetty-vs-springboot.md](002-jetty-vs-springboot.md): Decisión de usar Jetty en lugar de Spring Boot.
- [003-rsql-implementation.md](003-rsql-implementation.md): Decisión de implementar RSQL para filtrado.

## Formato de ADR

Cada ADR sigue el formato estándar:

```markdown
# <Título de la Decisión>

## Contexto

Descripción del problema o situación que requiere una decisión.

## Decisión

Descripción de la decisión tomada.

## Consecuencias

Impacto de la decisión, incluyendo beneficios y posibles desventajas.

## Alternativas Consideradas

Otras opciones que se evaluaron y por qué no se eligieron.

## Estado

- Propuesto
- Aceptado
- Obsoleto

## Fecha

Fecha de la decisión.
```

## Crear un Nuevo ADR

Para crear un nuevo ADR:

1. Crear un nuevo archivo con el formato `NNN-titulo.md`, donde `NNN` es el siguiente número disponible.
2. Seguir el formato estándar de ADR.
3. Actualizar el índice en este archivo `README.md`.

## Lista de ADRs

| Número | Título | Estado | Fecha |
|--------|--------|--------|-------|
| 001 | Arquitectura Hexagonal | Aceptado | 2026-03-24 |
| 002 | Jetty vs Spring Boot | Aceptado | 2026-03-24 |
| 003 | Implementación de RSQL | Aceptado | 2026-03-24 |
