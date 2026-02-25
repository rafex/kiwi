const state = {
  spec: null,
  operations: [],
  selected: null
};

const el = {
  baseUrl: document.getElementById("baseUrl"),
  endpointFilter: document.getElementById("endpointFilter"),
  endpoints: document.getElementById("endpoints"),
  methodBadge: document.getElementById("methodBadge"),
  pathValue: document.getElementById("pathValue"),
  summary: document.getElementById("summary"),
  securityHint: document.getElementById("securityHint"),
  pathParams: document.getElementById("pathParams"),
  queryParams: document.getElementById("queryParams"),
  authType: document.getElementById("authType"),
  bearerToken: document.getElementById("bearerToken"),
  basicUser: document.getElementById("basicUser"),
  basicPass: document.getElementById("basicPass"),
  bearerField: document.getElementById("bearerField"),
  basicUserField: document.getElementById("basicUserField"),
  basicPassField: document.getElementById("basicPassField"),
  headersInput: document.getElementById("headersInput"),
  bodyInput: document.getElementById("bodyInput"),
  sendBtn: document.getElementById("sendBtn"),
  statusValue: document.getElementById("statusValue"),
  timeValue: document.getElementById("timeValue"),
  urlValue: document.getElementById("urlValue"),
  responseHeaders: document.getElementById("responseHeaders"),
  responseBody: document.getElementById("responseBody")
};

function pretty(value) {
  if (typeof value === "string") {
    try {
      return JSON.stringify(JSON.parse(value), null, 2);
    } catch {
      return value;
    }
  }
  return JSON.stringify(value ?? null, null, 2);
}

function methodColor(method) {
  const m = method.toUpperCase();
  if (m === "GET") return "#2f9ef7";
  if (m === "POST") return "#31c48d";
  if (m === "PATCH") return "#ffb020";
  if (m === "DELETE") return "#ff6b6b";
  return "#9db0d3";
}

function parseHeaders(raw) {
  const out = {};
  raw
    .split("\n")
    .map((line) => line.trim())
    .filter(Boolean)
    .forEach((line) => {
      const idx = line.indexOf(":");
      if (idx > 0) {
        const key = line.slice(0, idx).trim();
        const value = line.slice(idx + 1).trim();
        out[key] = value;
      }
    });
  return out;
}

function parseBody(raw) {
  const trimmed = raw.trim();
  if (!trimmed) return undefined;
  return JSON.parse(trimmed);
}

function resolveSchema(schema) {
  if (!schema) return null;
  if (schema.$ref) {
    const parts = schema.$ref.split("/");
    const key = parts[parts.length - 1];
    return state.spec?.components?.schemas?.[key] || null;
  }
  return schema;
}

function buildFields(container, title, params, keyPrefix) {
  container.innerHTML = "";
  if (!params || params.length === 0) return;

  const wrapper = document.createElement("div");
  wrapper.className = "section";
  const h = document.createElement("h3");
  h.textContent = title;
  wrapper.appendChild(h);

  params.forEach((param) => {
    const field = document.createElement("div");
    field.className = "field";

    const label = document.createElement("label");
    label.setAttribute("for", `${keyPrefix}-${param.name}`);
    label.textContent = `${param.name}${param.required ? " *" : ""}`;

    const input = document.createElement("input");
    input.id = `${keyPrefix}-${param.name}`;
    input.dataset.paramName = param.name;
    input.placeholder = param.schema?.format === "uuid" ? "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx" : "";

    field.appendChild(label);
    field.appendChild(input);
    wrapper.appendChild(field);
  });

  container.appendChild(wrapper);
}

function renderEndpoints() {
  const query = el.endpointFilter.value.trim().toLowerCase();
  el.endpoints.innerHTML = "";

  state.operations
    .filter((op) => {
      if (!query) return true;
      return `${op.method} ${op.path} ${op.summary || ""}`.toLowerCase().includes(query);
    })
    .forEach((op, idx) => {
      const item = document.createElement("button");
      item.type = "button";
      item.className = `endpoint-item${state.selected === op ? " active" : ""}`;
      item.innerHTML = `<strong style="color:${methodColor(op.method)}">${op.method}</strong><code>${op.path}</code><small>${op.summary || "sin resumen"}</small>`;
      item.addEventListener("click", () => selectOperation(op));
      el.endpoints.appendChild(item);
      if (idx === 0 && !state.selected) {
        selectOperation(op);
      }
    });
}

function authHint(operation) {
  if (!operation.security || operation.security.length === 0) return "Endpoint público (sin auth)";

  const hasBearer = operation.security.some((sec) => Object.keys(sec).includes("bearerAuth"));
  const hasBasic = operation.security.some((sec) => Object.keys(sec).includes("basicAuth"));

  if (hasBearer && hasBasic) return "Acepta Bearer o Basic";
  if (hasBearer) return "Requiere Bearer token";
  if (hasBasic) return "Requiere Basic auth";
  return "Requiere autenticación";
}

function defaultBody(operation) {
  const schema = operation.requestBody?.content?.["application/json"]?.schema;
  const examples = operation.requestBody?.content?.["application/json"]?.examples;

  if (examples && Object.keys(examples).length > 0) {
    const first = examples[Object.keys(examples)[0]];
    if (first?.value) {
      return JSON.stringify(first.value, null, 2);
    }
  }

  const resolved = resolveSchema(schema);
  if (!resolved?.properties) return "";

  const skeleton = {};
  Object.entries(resolved.properties).forEach(([key, prop]) => {
    if (prop.type === "string") skeleton[key] = "";
    else if (prop.type === "array") skeleton[key] = [];
    else if (prop.type === "object") skeleton[key] = {};
    else if (prop.type === "integer" || prop.type === "number") skeleton[key] = 0;
    else skeleton[key] = null;
  });
  return JSON.stringify(skeleton, null, 2);
}

function selectOperation(operation) {
  state.selected = operation;
  renderEndpoints();

  el.methodBadge.textContent = operation.method;
  el.methodBadge.style.background = methodColor(operation.method);
  el.pathValue.textContent = operation.path;
  el.summary.textContent = operation.summary || "Sin resumen";
  el.securityHint.textContent = authHint(operation);

  buildFields(el.pathParams, "Path Params", operation.parameters.filter((p) => p.in === "path"), "path");
  buildFields(el.queryParams, "Query Params", operation.parameters.filter((p) => p.in === "query"), "query");

  el.headersInput.value = "Accept: application/json";
  el.bodyInput.value = defaultBody(operation);
  toggleAuthFields();
}

function toggleAuthFields() {
  const type = el.authType.value;
  el.bearerField.classList.toggle("hidden", type !== "bearer");
  el.basicUserField.classList.toggle("hidden", type !== "basic");
  el.basicPassField.classList.toggle("hidden", type !== "basic");
}

function collectParamValues(keyPrefix) {
  const values = {};
  document.querySelectorAll(`input[id^="${keyPrefix}-"]`).forEach((input) => {
    if (input.value.trim()) {
      values[input.dataset.paramName] = input.value.trim();
    }
  });
  return values;
}

async function sendRequest() {
  if (!state.selected) return;

  const op = state.selected;
  const pathValues = collectParamValues("path");
  const queryValues = collectParamValues("query");

  let finalPath = op.path;
  Object.entries(pathValues).forEach(([key, value]) => {
    finalPath = finalPath.replace(`{${key}}`, encodeURIComponent(value));
  });

  const query = new URLSearchParams(queryValues).toString();
  if (query) finalPath += `?${query}`;

  const headers = parseHeaders(el.headersInput.value);
  if (!Object.keys(headers).some((k) => k.toLowerCase() === "accept")) {
    headers.Accept = "application/json";
  }

  if (el.authType.value === "bearer" && el.bearerToken.value.trim()) {
    headers.Authorization = `Bearer ${el.bearerToken.value.trim()}`;
  }

  if (el.authType.value === "basic" && el.basicUser.value.trim()) {
    const token = btoa(`${el.basicUser.value.trim()}:${el.basicPass.value}`);
    headers.Authorization = `Basic ${token}`;
  }

  let parsedBody;
  try {
    const hasBody = Boolean(op.requestBody?.content?.["application/json"]);
    parsedBody = hasBody ? parseBody(el.bodyInput.value) : undefined;
  } catch (error) {
    el.statusValue.textContent = "Body JSON inválido";
    el.statusValue.className = "status-bad";
    el.responseBody.textContent = String(error.message || error);
    return;
  }

  el.sendBtn.disabled = true;
  el.sendBtn.textContent = "Enviando...";

  try {
    const res = await fetch("/api/request", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        baseUrl: el.baseUrl.value.trim(),
        path: finalPath,
        method: op.method,
        headers,
        body: parsedBody
      })
    });

    const data = await res.json();
    const statusClass = data.status >= 200 && data.status < 300 ? "status-ok" : data.status >= 400 ? "status-bad" : "status-warn";

    el.statusValue.textContent = `${data.status} ${data.statusText || ""}`.trim();
    el.statusValue.className = statusClass;
    el.timeValue.textContent = `${data.elapsedMs ?? "-"} ms`;
    el.urlValue.textContent = data.target || "-";
    el.responseHeaders.textContent = pretty(data.responseHeaders || {});
    el.responseBody.textContent = pretty(data.responseBody ?? data);
  } catch (error) {
    el.statusValue.textContent = "Error local";
    el.statusValue.className = "status-bad";
    el.timeValue.textContent = "-";
    el.urlValue.textContent = "-";
    el.responseHeaders.textContent = "{}";
    el.responseBody.textContent = pretty({ error: String(error.message || error) });
  } finally {
    el.sendBtn.disabled = false;
    el.sendBtn.textContent = "Enviar Request";
  }
}

async function bootstrap() {
  const res = await fetch("/api/spec");
  state.spec = await res.json();

  const serverUrl = state.spec?.servers?.[0]?.url || "http://localhost:8080";
  el.baseUrl.value = localStorage.getItem("kiwi.baseUrl") || serverUrl;
  el.bearerToken.value = localStorage.getItem("kiwi.bearer") || "";

  const ops = [];
  Object.entries(state.spec.paths || {}).forEach(([path, methods]) => {
    Object.entries(methods).forEach(([method, operation]) => {
      const params = [...(operation.parameters || [])].map((param) => {
        if (param.$ref) {
          const key = param.$ref.split("/").pop();
          return state.spec.components?.parameters?.[key] || param;
        }
        return param;
      });

      ops.push({
        path,
        method: method.toUpperCase(),
        summary: operation.summary || "",
        parameters: params,
        requestBody: operation.requestBody,
        security: operation.security ?? state.spec.security ?? []
      });
    });
  });

  state.operations = ops.sort((a, b) => `${a.path}${a.method}`.localeCompare(`${b.path}${b.method}`));
  renderEndpoints();
}

el.sendBtn.addEventListener("click", sendRequest);
el.authType.addEventListener("change", toggleAuthFields);
el.endpointFilter.addEventListener("input", renderEndpoints);
el.baseUrl.addEventListener("change", () => localStorage.setItem("kiwi.baseUrl", el.baseUrl.value.trim()));
el.bearerToken.addEventListener("change", () => localStorage.setItem("kiwi.bearer", el.bearerToken.value.trim()));

bootstrap().catch((error) => {
  el.responseBody.textContent = pretty({ error: "No se pudo cargar el OAS", message: String(error.message || error) });
});
