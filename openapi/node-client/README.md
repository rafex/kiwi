# kiwi-node-client

Cliente visual tipo Postman para la API Kiwi, generado automáticamente desde el OAS (`../kiwi-openapi.yaml`).

## Requisitos

- Node.js 18+

## Ejecución recomendada (desde la raíz del repositorio)

El proyecto está configurado con un sistema de Makefiles anidados. Para ejecutar el cliente API desde la raíz del repositorio:

```bash
# Instalar dependencias (solo primera vez)
make client-install

# Ejecutar el servidor web
make client-run
```

## Ejecución directa (desde este directorio)

```bash
npm install
npm run web
```

## Puerto

El servidor por defecto escucha en el puerto 3030:
`http://localhost:3030`

Para usar un puerto personalizado:
```bash
KIWI_CLIENT_PORT=3001 npm run web
```

## Funcionalidades

- Lista de endpoints leída directamente del OAS — cualquier endpoint nuevo aparece automáticamente
- Filtro por método, path o resumen
- Path params y query params con campos generados desde el spec
- Autenticación Bearer o Basic (persiste en `localStorage`)
- Body JSON editable con skeleton autogenerado desde el schema
- Response con status HTTP, tiempo en ms, headers y body formateado
- Paneles redimensionables
