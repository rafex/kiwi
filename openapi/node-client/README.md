# kiwi-node-client

Proyecto Node.js para consumir la API Kiwi en dos modos:

- CLI (`src/index.js`)
- Web UI visual tipo Postman/Swagger (`src/server.js` + `public/`)

La especificación fuente está en `../kiwi-openapi.yaml`.

## Requisitos

- Node.js 18+

## Instalación

```bash
cd /Users/rafex/repository/github/rafex/kiwi/openapi/node-client
npm install
```

## Modo visual (recomendado)

```bash
npm run web
```

Abre: `http://localhost:3030`

Características de la UI:

- lista y filtro de endpoints desde OAS
- visualización de método, path y resumen
- edición de path params, query params y headers
- autenticación `Bearer` o `Basic`
- edición de body JSON
- response con status, tiempo, headers y body formateado

Puedes cambiar el puerto con:

```bash
export KIWI_CLIENT_PORT=3030
npm run web
```

## Modo CLI

```bash
npm run health
npm run hello
npm run login -- --username USER --password PASS [--basic]
npm run start -- get-object --id UUID
npm run start -- search --q macbook --tags laptop,apple --limit 20
npm run start -- fuzzy --name mac
npm run start -- create-location --name "Closet"
npm run start -- create-object --name "MacBook Pro" --location-id UUID --tags laptop,apple --metadata '{"ram_gb":32}'
npm run start -- move-object --id UUID --new-location-id UUID
npm run start -- update-tags --id UUID --tags a,b,c
npm run start -- update-text --id UUID --name "Nuevo nombre" --description "Nueva desc"
```

Variables útiles:

```bash
export KIWI_BASE_URL="https://kiwi.v1.rafex.cloud"
export KIWI_TOKEN="<jwt>"
export KIWI_USERNAME="rafex"
export KIWI_PASSWORD="secret"
```
