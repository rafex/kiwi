# Plantillas de Configuración

Este directorio contiene plantillas de configuración para diferentes entornos de despliegue.

## Plantillas Disponibles

- [helm-values.yaml](helm-values.yaml): Plantilla de valores para Helm.
- [k8s-deployment.yaml](k8s-deployment.yaml): Plantilla de despliegue para Kubernetes.
- [docker-compose.yml](docker-compose.yml): Plantilla de Docker Compose para desarrollo local.

## Uso

1. Copiar la plantilla correspondiente al entorno:

```bash
cp context/config-templates/helm-values.yaml helm/kiwi-backend/values.yaml
```

2. Editar el archivo con los valores específicos del entorno.

3. Usar el archivo para desplegar la aplicación:

```bash
helm upgrade --install kiwi-backend helm/kiwi-backend/
```
