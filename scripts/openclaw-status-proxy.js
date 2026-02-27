#!/usr/bin/env node
const http = require("http");
const { URL } = require("url");

const PORT = Number(process.env.OPENCLAW_PROXY_PORT || "18795");
const HOST = process.env.OPENCLAW_PROXY_HOST || "0.0.0.0";
const GATEWAY_HOST = process.env.OPENCLAW_GATEWAY_HOST || "127.0.0.1";
const GATEWAY_PORT = Number(process.env.OPENCLAW_GATEWAY_PORT || "18789");
const TIMEOUT_MS = Number(process.env.OPENCLAW_PROXY_TIMEOUT_MS || "5000");
const GATEWAY_CANDIDATE_PATHS = [
  "/status",
  "/",
  "/api/status",
  "/api/health",
  "/health",
  "/api/v1/status",
];
const DEFAULT_SYNTHETIC_HOSTNAME =
  process.env.OPENCLAW_SYNTHETIC_HOSTNAME || "AAI";

function sendJson(res, statusCode, bodyObj) {
  const body = JSON.stringify(bodyObj);
  res.writeHead(statusCode, {
    "Content-Type": "application/json; charset=utf-8",
    "Access-Control-Allow-Origin": "*",
    "Cache-Control": "no-store",
    "Content-Length": Buffer.byteLength(body),
  });
  res.end(body);
}

function formatNow() {
  const now = new Date();
  const iso = now.toISOString();
  const updatedAtJST = new Intl.DateTimeFormat("sv-SE", {
    timeZone: "Asia/Tokyo",
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit",
  })
    .format(now)
    .replace("T", " ");
  return { iso, updatedAtJST: `${updatedAtJST} JST` };
}

function buildSyntheticStatus() {
  const host = process.env.OPENCLAW_GATEWAY_HOST || DEFAULT_SYNTHETIC_HOSTNAME;
  const { iso, updatedAtJST } = formatNow();
  return {
    summary: "Gateway reachable (dashboard mode)",
    gateway: "🟢",
    gatewayStatus: "reachable (HTML dashboard)",
    gatewayLatencyMs: null,
    telegram: "❓",
    telegramStatus: "unknown",
    agents: 0,
    sessions: 0,
    memoryChunks: 0,
    updateAvailable: false,
    agentList: [],
    heartbeatTasks: 0,
    updatedAt: iso,
    updatedAtJST,
    updatedTime: iso,
    host,
    version: "proxy-synthetic",
    _note:
      "Gateway returns HTML dashboard. JSON API not available. Proxy built synthetic response.",
  };
}

function looksLikeHtml(text, contentType) {
  return (
    (contentType && contentType.includes("text/html")) ||
    text.startsWith("<!DOCTYPE") ||
    text.startsWith("<html") ||
    text.startsWith("<HTML")
  );
}

function requestGatewayStatus(path) {
  return new Promise((resolve, reject) => {
    const options = {
      host: GATEWAY_HOST,
      port: GATEWAY_PORT,
      path,
      method: "GET",
      headers: {
        Accept: "application/json,text/html,*/*;q=0.8",
      },
      timeout: TIMEOUT_MS,
    };

    const req = http.request(options, (res) => {
      const chunks = [];
      const contentType = (res.headers["content-type"] || "").toLowerCase();

      res.on("data", (chunk) => chunks.push(chunk));
      res.on("end", () => {
        const text = Buffer.concat(chunks).toString("utf8").trim();

        if (res.statusCode < 200 || res.statusCode >= 300) {
          reject(
            new Error(
              `gateway path ${path} returned status ${res.statusCode}: ${text}`
            )
          );
          return;
        }

        if (!text) {
          reject(new Error(`gateway path ${path}: 空レスポンス`));
          return;
        }

        if (
          contentType.includes("application/json") ||
          text.startsWith("{") ||
          text.startsWith("[")
        ) {
          try {
            JSON.parse(text);
            resolve(text);
            return;
          } catch (parseError) {
            reject(
              new Error(
                `gateway path ${path}: JSON parse error: ${
                  parseError.message || parseError
                }`
              )
            );
            return;
          }
        }

        if (looksLikeHtml(text, contentType)) {
          resolve(JSON.stringify(buildSyntheticStatus()));
          return;
        }

        reject(
          new Error(
            `gateway path ${path}: unsupported content-type=${contentType || "unknown"}`
          )
        );
      });
    });

    req.on("timeout", () => {
      req.destroy(new Error(`gateway request timeout: ${TIMEOUT_MS}ms`));
    });

    req.on("error", (error) => {
      reject(error);
    });

    req.end();
  });
}

async function fetchGatewayStatus() {
  const errors = [];

  for (const path of GATEWAY_CANDIDATE_PATHS) {
    try {
      return await requestGatewayStatus(path);
    } catch (error) {
      errors.push(`${path}: ${error.message}`);
    }
  }

  const combinedMessage = `gateway fetch failed: ${errors.join("; ")}`;
  const combinedError = new Error(combinedMessage);
  combinedError.details = errors;
  throw combinedError;
}

const server = http.createServer(async (req, res) => {
  if (!req.url) {
    sendJson(res, 404, { error: "not found", path: "" });
    return;
  }

  const parsedUrl = new URL(req.url, `http://localhost:${PORT}`);
  let path = parsedUrl.pathname;
  if (path.length > 1) {
    path = path.replace(/\/+$/, "");
  }

  if (path !== "/" && path !== "/status") {
    sendJson(res, 404, { error: "not found", path: parsedUrl.pathname || "" });
    return;
  }

  if (req.method !== "GET") {
    sendJson(res, 405, { error: "method not allowed" });
    return;
  }

  try {
    const jsonText = await fetchGatewayStatus();
    res.writeHead(200, {
      "Content-Type": "application/json; charset=utf-8",
      "Access-Control-Allow-Origin": "*",
      "Cache-Control": "no-store",
      "Content-Length": Buffer.byteLength(jsonText),
    });
    res.end(jsonText);
  } catch (err) {
    sendJson(res, 502, {
      error: "failed to call OpenClaw gateway",
      detail: err.message,
      details: err.details || [],
    });
  }
});

server.listen(PORT, HOST, () => {
  console.log(
    `openclaw-status-proxy listening on http://${HOST}:${PORT}/status`
  );
  console.log(`gateway target: http://${GATEWAY_HOST}:${GATEWAY_PORT}`);
  console.log(`candidate paths: ${GATEWAY_CANDIDATE_PATHS.join(", ")}`);
});

server.on("error", (error) => {
  console.error(`proxy error: ${error.message}`);
  process.exit(1);
});
