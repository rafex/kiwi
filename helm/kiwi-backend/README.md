# kiwi-backend Helm Chart

Chart Helm para desplegar **directo a producción** la imagen publicada del backend en GHCR.

## Imagen por defecto

- `ghcr.io/rafex/kiwi-jetty-backend:latest`

Para producción se recomienda fijar un tag inmutable (por ejemplo SHA).

## Instalación recomendada (producción)

1) Crear secret con credenciales y secretos:

```bash
kubectl -n kiwi create secret generic kiwi-backend-secrets \
  --from-literal=DB_URL='jdbc:postgresql://postgres:5432/kiwi' \
  --from-literal=DB_USER='kiwi_app' \
  --from-literal=DB_PASSWORD='changeme' \
  --from-literal=JWT_SECRET='CHANGE_ME'
```

2) Instalar/actualizar release con tag fijo:

```bash
helm upgrade --install kiwi-backend ./helm/kiwi-backend \
  --namespace kiwi --create-namespace \
  --set image.tag="<sha-o-tag-version>" \
  --set existingSecret="kiwi-backend-secrets"
```

## Notas de operación

- El chart está endurecido para producción por defecto:
  - `replicaCount=2`
  - `autoscaling.enabled=true`
  - `service.exposeGlowroot=false` (no expone 4000 en Service)
  - `securityContext` restrictivo
- Si necesitas Glowroot en cluster, habilítalo con:

```bash
--set service.exposeGlowroot=true
```

## Usar otro secret existente

```bash
kubectl -n kiwi create secret generic kiwi-backend-secrets \
  --from-literal=DB_URL='jdbc:postgresql://postgres:5432/kiwi' \
  --from-literal=DB_USER='kiwi_app' \
  --from-literal=DB_PASSWORD='changeme' \
  --from-literal=JWT_SECRET='CHANGE_ME'

helm upgrade --install kiwi-backend ./helm/kiwi-backend \
  --namespace kiwi --create-namespace \
  --set existingSecret=kiwi-backend-secrets
```

## Valores importantes

- `image.repository`, `image.tag`
- `service.port` (HTTP 8080)
- `service.glowrootPort` (4000)
- `service.exposeGlowroot` (true/false)
- `env.*` (JWT_ISS, JWT_AUD, JWT_TTL_SECONDS, ENVIRONMENT)
- `secretEnv.*` o `existingSecret`
