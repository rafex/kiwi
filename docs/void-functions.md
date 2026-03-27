# Funciones VOID en PostgreSQL

## Introducción

En PostgreSQL, es común definir funciones que no retornan valor (o retornan `VOID`). Estas funciones se utilizan para realizar operaciones de escritura (INSERT, UPDATE, DELETE) o lógica compleja que no requiere devolver datos al cliente.

## Patrón de Implementación en Java

Cuando se invoca una función PostgreSQL que retorna `VOID` desde Java usando el cliente de base de datos, es importante manejar correctamente el `ResultSet` generado por la consulta.

### Error Común

El error típico al usar `db.execute()` con una consulta que ejecuta una función `VOID` es:

```
org.postgresql.util.PSQLException: No results were returned by the query.
```

Esto ocurre porque `db.execute()` espera un resultado (filas devueltas), pero las funciones `VOID` no devuelven ninguna fila.

### Solución Correcta

Se debe utilizar `db.query(..., rs -> null)` para ejecutar la consulta y descartar el `ResultSet` resultante.

**Ejemplo de implementación incorrecta:**

```java
// INCORRECTO: Usar execute con funciones VOID
db.execute(new SqlQuery("SELECT api_create_object(...)", params));
```

**Ejemplo de implementación correcta:**

```java
// CORRECTO: Usar query con un mapper que devuelve null
db.query(new SqlQuery("SELECT api_create_object(...)", params), rs -> null);
```

## Funciones VOID en el Proyecto Kiwi

A continuación se listan las funciones `VOID` definidas en el proyecto y sus respectivas implementaciones en Java:

| Función PostgreSQL | Ubicación Java | Método |
|--------------------|----------------|--------|
| `api_create_object` | `ObjectRepositoryImpl` | `createObject` |
| `api_move_object` | `ObjectRepositoryImpl` | `moveObject` |
| `api_update_tags` | `ObjectRepositoryImpl` | `updateTags` |
| `api_update_text` | `ObjectRepositoryImpl` | `updateText` |
| `api_update_metadata` | `ObjectRepositoryImpl` | `updateMetadata` |
| `api_create_location` | `LocationRepositoryImpl` | `createLocation` |
| `api_assign_role_to_user` | `RoleRepositoryImpl` | `assignRoleToUser` |

## Documentación de Javadoc

Cada método que invoca una función `VOID` debe incluir Javadoc explicando el patrón utilizado:

```java
/**
 * Descripción de la operación.
 * Esta operación ejecuta una función PostgreSQL que devuelve VOID.
 * Se utiliza {@code db.query(..., rs -> null)} para descartar el {@link java.sql.ResultSet}.
 */
public void metodoEjemplo(...) {
    db.query(new SqlQuery("SELECT api_funcion_void(...)", params), rs -> null);
}
```

## Referencias

- [PostgreSQL Documentation: Functions Returning Void](https://www.postgresql.org/docs/current/sql-createfunction.html#SQL-CREATEFUNCTION-RETURN-VOID)
- [V2__api_functions.sql](../db/sql/V2__api_functions.sql) - Definición de funciones API en el proyecto
