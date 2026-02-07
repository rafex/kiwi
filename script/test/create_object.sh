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
    "locationId": "11111111-1111-1111-1111-111111111111"
  }'
