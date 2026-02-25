import { KiwiClient } from "./client.js";

function argValue(flag, fallback = "") {
  const idx = process.argv.indexOf(flag);
  if (idx === -1 || idx + 1 >= process.argv.length) return fallback;
  return process.argv[idx + 1];
}

function hasFlag(flag) {
  return process.argv.includes(flag);
}

function printJson(value) {
  console.log(JSON.stringify(value, null, 2));
}

function usage() {
  console.log(`
Uso:
  node src/index.js <comando> [opciones]

Comandos:
  health
  hello
  login --username USER --password PASS [--basic]
  get-object --id UUID
  search --q TEXTO [--tags a,b] [--location-id UUID] [--limit N]
  fuzzy --name TEXTO [--limit N]
  create-location --name NOMBRE [--parent-id UUID]
  create-object --name NOMBRE --location-id UUID [--description TXT] [--type TIPO] [--tags a,b] [--metadata '{"k":"v"}']
  move-object --id UUID --new-location-id UUID
  update-tags --id UUID --tags a,b,c
  update-text --id UUID [--name NOMBRE] [--description DESC]
  demo

Variables de entorno:
  KIWI_BASE_URL   (default: http://localhost:8080)
  KIWI_TOKEN      token Bearer para rutas protegidas
  KIWI_USERNAME   usuario default para login
  KIWI_PASSWORD   password default para login
`);
}

async function main() {
  const command = process.argv[2];

  if (!command || command === "-h" || command === "--help") {
    usage();
    process.exit(0);
  }

  const client = new KiwiClient({
    baseUrl: process.env.KIWI_BASE_URL,
    token: process.env.KIWI_TOKEN
  });

  try {
    let res;

    switch (command) {
      case "health":
        res = await client.health();
        break;

      case "hello":
        res = await client.hello();
        break;

      case "login": {
        const username = argValue("--username", process.env.KIWI_USERNAME || "");
        const password = argValue("--password", process.env.KIWI_PASSWORD || "");
        const useBasic = hasFlag("--basic");
        res = await client.login({ username, password, useBasic });
        if (res?.data?.access_token) {
          console.log("Token recibido. Exporta para siguientes llamadas:");
          console.log(`export KIWI_TOKEN=${res.data.access_token}`);
        }
        break;
      }

      case "get-object":
        res = await client.getObject(argValue("--id"));
        break;

      case "search":
        res = await client.search({
          q: argValue("--q"),
          tags: argValue("--tags"),
          locationId: argValue("--location-id"),
          limit: argValue("--limit") ? Number(argValue("--limit")) : undefined
        });
        break;

      case "fuzzy":
        res = await client.fuzzy({
          name: argValue("--name"),
          limit: argValue("--limit") ? Number(argValue("--limit")) : undefined
        });
        break;

      case "create-location":
        res = await client.createLocation({
          name: argValue("--name"),
          parentLocationId: argValue("--parent-id") || undefined
        });
        break;

      case "create-object": {
        const metadataRaw = argValue("--metadata");
        let metadata;
        if (metadataRaw) {
          metadata = JSON.parse(metadataRaw);
        }

        res = await client.createObject({
          name: argValue("--name"),
          description: argValue("--description") || undefined,
          type: argValue("--type") || undefined,
          tags: argValue("--tags") ? argValue("--tags").split(",") : undefined,
          metadata,
          locationId: argValue("--location-id")
        });
        break;
      }

      case "move-object":
        res = await client.moveObject(argValue("--id"), argValue("--new-location-id"));
        break;

      case "update-tags":
        res = await client.updateTags(argValue("--id"), argValue("--tags").split(","));
        break;

      case "update-text":
        res = await client.updateText(argValue("--id"), {
          name: argValue("--name") || undefined,
          description: argValue("--description") || undefined
        });
        break;

      case "demo": {
        const hello = await client.hello();
        const health = await client.health();
        printJson({ hello: hello.data, health: health.data, baseUrl: client.baseUrl });
        return;
      }

      default:
        usage();
        process.exit(1);
    }

    printJson({ status: res.status, data: res.data });
  } catch (error) {
    printJson({
      error: error.message,
      status: error.status,
      data: error.data
    });
    process.exit(1);
  }
}

main();
