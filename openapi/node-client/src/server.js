import express from "express";
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import yaml from "js-yaml";

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const projectRoot = path.resolve(__dirname, "..");
const specPath = path.resolve(projectRoot, "../kiwi-openapi.yaml");
const publicDir = path.resolve(projectRoot, "public");

const app = express();
app.use(express.json({ limit: "2mb" }));
app.use(express.static(publicDir));

function parseSpec() {
  const raw = fs.readFileSync(specPath, "utf8");
  return yaml.load(raw);
}

app.get("/api/spec", (_req, res) => {
  try {
    const spec = parseSpec();
    res.json(spec);
  } catch (error) {
    res.status(500).json({ error: "cannot_load_spec", message: String(error?.message || error) });
  }
});

app.post("/api/request", async (req, res) => {
  const { baseUrl, path: reqPath, method, headers, body } = req.body || {};

  if (!baseUrl || !reqPath || !method) {
    return res.status(400).json({ error: "missing_fields", message: "baseUrl, path y method son requeridos" });
  }

  const target = `${String(baseUrl).replace(/\/$/, "")}${reqPath}`;
  const cleanHeaders = { ...(headers || {}) };

  const init = {
    method: String(method).toUpperCase(),
    headers: cleanHeaders
  };

  if (body !== undefined && body !== null && body !== "") {
    init.body = typeof body === "string" ? body : JSON.stringify(body);
    if (!Object.keys(cleanHeaders).some((k) => k.toLowerCase() === "content-type")) {
      cleanHeaders["Content-Type"] = "application/json";
    }
  }

  const startedAt = Date.now();

  try {
    const response = await fetch(target, init);
    const elapsedMs = Date.now() - startedAt;
    const text = await response.text();

    let parsedBody = text;
    if (text) {
      try {
        parsedBody = JSON.parse(text);
      } catch {
        parsedBody = text;
      }
    }

    const responseHeaders = {};
    response.headers.forEach((value, key) => {
      responseHeaders[key] = value;
    });

    return res.json({
      ok: response.ok,
      status: response.status,
      statusText: response.statusText,
      elapsedMs,
      target,
      responseHeaders,
      responseBody: parsedBody
    });
  } catch (error) {
    return res.status(502).json({
      ok: false,
      error: "upstream_request_failed",
      message: String(error?.message || error),
      target
    });
  }
});

app.get("*", (_req, res) => {
  res.sendFile(path.join(publicDir, "index.html"));
});

const PORT = Number(process.env.KIWI_CLIENT_PORT || 3030);
app.listen(PORT, () => {
  console.log(`kiwi-node-client web UI: http://localhost:${PORT}`);
});
