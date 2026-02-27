#!/usr/bin/env node
// MiyabiDash Status Proxy v3 — HTTP direct fetch from Gateway (no child_process)
const http = require('http');

const PORT = parseInt(process.env.PROXY_PORT || '18795', 10);
const GW_HOST = process.env.OPENCLAW_GATEWAY_HOST || '127.0.0.1';
const GW_PORT = parseInt(process.env.OPENCLAW_GATEWAY_PORT || '18789', 10);

process.on('uncaughtException', e => console.error('[FATAL]', e.message));
process.on('unhandledRejection', e => console.error('[REJECT]', e));

let cachedResponse = JSON.stringify({ status: 'starting', message: 'Proxy initializing...' });
let lastOk = 0;

function fetchFromGateway(path) {
  return new Promise((resolve, reject) => {
    const req = http.get({ hostname: GW_HOST, port: GW_PORT, path, timeout: 10000 }, res => {
      let body = '';
      res.on('data', c => body += c);
      res.on('end', () => resolve({ status: res.statusCode, body }));
    });
    req.on('error', reject);
    req.on('timeout', () => { req.destroy(); reject(new Error('timeout')); });
  });
}

async function refresh() {
  try {
    // Try /status first, fallback to /
    let result = await fetchFromGateway('/status');
    let body = result.body;

    // Check if it's JSON
    try {
      JSON.parse(body);
      cachedResponse = body;
      lastOk = Date.now();
      console.log('[refresh] JSON from /status OK');
      return;
    } catch {}

    // /status returned HTML, try /
    result = await fetchFromGateway('/');
    body = result.body;
    try {
      JSON.parse(body);
      cachedResponse = body;
      lastOk = Date.now();
      console.log('[refresh] JSON from / OK');
      return;
    } catch {}

    // Both returned non-JSON (HTML dashboard) — build synthetic status
    const now = new Date();
    const jst = new Date(now.getTime() + 9 * 3600000);
    cachedResponse = JSON.stringify({
      summary: 'Gateway reachable (dashboard mode)',
      gateway: '🟢',
      gatewayStatus: 'reachable (HTML dashboard, no JSON API)',
      gatewayLatencyMs: null,
      telegram: '❓',
      telegramStatus: 'unknown',
      agents: 0, sessions: 0, memoryChunks: 0,
      updateAvailable: false, agentList: [], heartbeatTasks: 0,
      updatedAt: now.toISOString(),
      updatedAtJST: jst.toISOString().replace('T', ' ').slice(0, 19) + ' JST',
      updatedTime: now.toISOString(),
      host: 'AAI', version: 'proxy-synthetic',
      _note: 'Gateway returns HTML dashboard. JSON API not available. Proxy built synthetic response.'
    });
    lastOk = Date.now();
    console.log('[refresh] Gateway reachable but HTML only — synthetic status');
  } catch (e) {
    console.error('[refresh error]', e.message);
    // Keep stale cache
  }
}

// Background refresh every 30s
refresh();
setInterval(refresh, 30000);

// Server
const server = http.createServer((req, res) => {
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type');
  res.setHeader('Connection', 'close');

  if (req.method === 'OPTIONS') { res.writeHead(204); res.end(); return; }

  res.writeHead(200, { 'Content-Type': 'application/json; charset=utf-8' });
  res.end(cachedResponse);
});

server.on('error', e => {
  console.error('[server error]', e.message);
  if (e.code === 'EADDRINUSE') {
    console.error(`Port ${PORT} in use. Kill existing process or use PROXY_PORT env.`);
    process.exit(1);
  }
});

server.listen(PORT, '0.0.0.0', () => {
  console.log(`MiyabiDash Proxy on 0.0.0.0:${PORT}`);
  console.log(`Test: curl http://localhost:${PORT}/status`);
});
