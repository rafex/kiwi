# 002. Jetty vs Spring Boot

## Contexto

El proyecto Kiwi requiere un servidor HTTP para exponer su API. Las opciones principales son Jetty y Spring Boot.

## Decisión

Utilizar Jetty como servidor HTTP en lugar de Spring Boot.

## Consecuencias

### Beneficios
- **Ligereza**: Jetty es más ligero y tiene menos dependencias que Spring Boot.
- **Control**: Mayor control sobre la configuración y el ciclo de vida del servidor.
- **Rendimiento**: Mejor rendimiento en entornos con recursos limitados.

### Desventajas
- **Menos características**: Spring Boot ofrece más características integradas (ej. Spring Security, Spring Data).
- **Configuración manual**: Requiere más configuración manual para características comunes.

## Alternativas Consideradas

1. **Spring Boot**: Más características integradas pero más pesado.
2. **Undertow**: Buen rendimiento pero menos documentación.
3. **Netty**: Alto rendimiento pero más complejo.

## Estado

Aceptado

## Fecha

2026-03-24
