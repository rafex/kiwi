const DEFAULT_BASE_URL = process.env.KIWI_BASE_URL || "http://localhost:8080";

function toBase64(value) {
  return Buffer.from(value, "utf8").toString("base64");
}

export class KiwiClient {
  constructor({ baseUrl = DEFAULT_BASE_URL, token = process.env.KIWI_TOKEN || "" } = {}) {
    this.baseUrl = baseUrl.replace(/\/$/, "");
    this.token = token;
  }

  setToken(token) {
    this.token = token;
  }

  async request(path, { method = "GET", headers = {}, body, token } = {}) {
    const requestHeaders = {
      Accept: "application/json",
      ...headers
    };

    const bearer = token ?? this.token;
    if (bearer) {
      requestHeaders.Authorization = `Bearer ${bearer}`;
    }

    const init = { method, headers: requestHeaders };
    if (body !== undefined) {
      init.body = JSON.stringify(body);
      requestHeaders["Content-Type"] = "application/json";
    }

    const response = await fetch(`${this.baseUrl}${path}`, init);
    const text = await response.text();

    let data = null;
    if (text) {
      try {
        data = JSON.parse(text);
      } catch {
        data = text;
      }
    }

    if (!response.ok) {
      const error = new Error(`HTTP ${response.status} ${response.statusText}`);
      error.status = response.status;
      error.data = data;
      throw error;
    }

    return { status: response.status, data };
  }

  hello() {
    return this.request("/hello");
  }

  health() {
    return this.request("/health");
  }

  async login({ username = process.env.KIWI_USERNAME, password = process.env.KIWI_PASSWORD, useBasic = false } = {}) {
    if (!username || !password) {
      throw new Error("Debes enviar username y password (args o env KIWI_USERNAME/KIWI_PASSWORD)");
    }

    if (useBasic) {
      const auth = toBase64(`${username}:${password}`);
      return this.request("/auth/login", {
        method: "POST",
        headers: { Authorization: `Basic ${auth}` },
        token: ""
      });
    }

    return this.request("/auth/login", {
      method: "POST",
      body: { username, password },
      token: ""
    });
  }

  createLocation(payload) {
    return this.request("/locations", { method: "POST", body: payload });
  }

  createObject(payload) {
    return this.request("/objects", { method: "POST", body: payload });
  }

  getObject(objectId) {
    return this.request(`/objects/${objectId}`);
  }

  search({ q, tags, locationId, limit }) {
    const params = new URLSearchParams();
    if (q) params.set("q", q);
    if (tags) params.set("tags", Array.isArray(tags) ? tags.join(",") : tags);
    if (locationId) params.set("locationId", locationId);
    if (limit !== undefined) params.set("limit", String(limit));
    return this.request(`/objects/search?${params.toString()}`);
  }

  fuzzy({ name, limit }) {
    const params = new URLSearchParams();
    if (name) params.set("name", name);
    if (limit !== undefined) params.set("limit", String(limit));
    return this.request(`/objects/fuzzy?${params.toString()}`);
  }

  moveObject(objectId, newLocationId) {
    return this.request(`/objects/${objectId}/move`, {
      method: "PATCH",
      body: { newLocationId }
    });
  }

  updateTags(objectId, tags) {
    return this.request(`/objects/${objectId}/tags`, {
      method: "PATCH",
      body: { tags }
    });
  }

  updateText(objectId, { name, description }) {
    return this.request(`/objects/${objectId}/text`, {
      method: "PATCH",
      body: { name, description }
    });
  }
}
