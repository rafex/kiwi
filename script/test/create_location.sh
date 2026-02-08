#!/bin/sh

curl -X POST http://localhost:8080/locations \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Bodega principal"
  }'
