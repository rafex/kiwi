#!/bin/sh

curl -X POST http://localhost:8080/objects \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Laptop Dell XPS",
    "description": "Equipo asignado a desarrollo backend",
    "type": "EQUIPMENT",
    "tags": ["laptop", "dell", "backend"],
    "metadata": {
      "serial": "ABC123XYZ",
      "ram_gb": 32,
      "os": "linux"
    },
    "locationId": "f1d6c222-2d8b-425f-b77e-5cfefee313b1"
  }'
