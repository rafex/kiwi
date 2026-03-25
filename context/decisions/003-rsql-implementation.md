# 003. Implementación de RSQL

## Contexto

El proyecto Kiwi requiere una forma flexible de filtrar recursos mediante consultas. Las opciones principales son:
- Implementar un lenguaje de consulta personalizado.
- Utilizar un estándar existente como RSQL.

## Decisión

Implementar RSQL (Restful Query Language) para el filtrado de recursos.

## Consecuencias

### Beneficios
- **Estándar**: RSQL es un estándar conocido y utilizado en otros proyectos.
- **Flexibilidad**: Permite consultas complejas con operadores lógicos (AND, OR) y de comparación (==, !=, =in=, etc.).
- **Extensibilidad**: Fácil de extender con nuevos operadores y características.

### Desventajas
- **Curva de aprendizaje**: Los desarrolladores deben aprender la sintaxis de RSQL.
- **Complejidad**: La implementación de un parser RSQL puede ser compleja.

## Alternativas Consideradas

1. **Lenguaje de consulta personalizado**: Más flexible pero no estándar.
2. **GraphQL**: Demasiado complejo para las necesidades actuales.
3. **OData**: Estándar pero más pesado y complejo.

## Estado

Aceptado

## Fecha

2026-03-24
