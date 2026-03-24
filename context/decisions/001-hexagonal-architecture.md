# 001. Arquitectura Hexagonal

## Contexto

El proyecto Kiwi requiere una arquitectura que permita:
- Separación clara entre la lógica de negocio y los detalles técnicos.
- Facilidad para probar componentes de forma aislada.
- Flexibilidad para cambiar tecnologías sin afectar la lógica de negocio.
- Escalabilidad y mantenimiento a largo plazo.

## Decisión

Adoptar la arquitectura hexagonal (también conocida como puertos y adaptadores) para el proyecto Kiwi.

## Consecuencias

### Beneficios
- **Separación de responsabilidades**: La lógica de negocio está aislada de los detalles técnicos.
- **Facilidad de prueba**: Los componentes pueden probarse de forma aislada sin depender de infraestructura.
- **Flexibilidad**: Permite cambiar tecnologías (ej. Jetty a Spring Boot) sin afectar la lógica de negocio.
- **Mantenibilidad**: El código es más fácil de entender y mantener.

### Desventajas
- **Complejidad inicial**: Requiere más esfuerzo inicial para definir las interfaces y adaptadores.
- **Curva de aprendizaje**: Los desarrolladores nuevos deben entender la arquitectura hexagonal.

## Alternativas Consideradas

1. **Arquitectura en capas tradicional**: Más simple pero menos flexible.
2. **Arquitectura limpia**: Similar a la hexagonal pero con más capas.
3. **Microservicios**: Demasiado complejo para el alcance actual del proyecto.

## Estado

Aceptado

## Fecha

2026-03-24
