# kiwi-node-client

Cliente visual tipo Postman para la API Kiwi, generado automáticamente desde el OAS (`../kiwi-openapi.yaml`).

## Requisitos

- Node.js 18+

## Instalación

```bash
cd openapi/node-client
npm install
```

## Uso

```bash
npm run web
```

Abre: `http://localhost:3030`

## Funcionalidades

- Lista de endpoints leída directamente del OAS — cualquier endpoint nuevo aparece automáticamente
- Filtro por método, path o resumen
- Path params y query params con campos generados desde el spec
- Autenticación Bearer o Basic (persiste en `localStorage`)
- Body JSON editable con skeleton autogenerado desde el schema
- Response con status HTTP, tiempo en ms, headers y body formateado
- Paneles redimensionables

## Puerto

```bash
KIWI_CLIENT_PORT=3030 npm run web
```
