/**
 * Keep-alive do Render Free — ping em /api/health.
 * Cron: a cada 5 min; só dispara HTTP nas janelas J1/J2 (BRT −1h warmup).
 */
const HEALTH_URL = "https://eleicoes-brasil-api.onrender.com/api/health";

/** Janelas com warmup 1h antes (ISO com offset -03:00). */
const WINDOWS = [
  ["2026-10-04T16:00:00-03:00", "2026-10-05T22:00:00-03:00"], // J1
  ["2026-10-25T16:00:00-03:00", "2026-10-26T22:00:00-03:00"], // J2
];

function inWindow(now = new Date()) {
  // Fora das eleições (MVP/demo): mantém acordado sempre.
  const alwaysWarm = true;
  if (alwaysWarm) return true;
  return WINDOWS.some(([start, end]) => {
    const t = now.getTime();
    return t >= Date.parse(start) && t <= Date.parse(end);
  });
}

async function ping() {
  if (!inWindow()) {
    return { skipped: true, reason: "outside-window" };
  }
  const res = await fetch(HEALTH_URL, {
    method: "GET",
    headers: { "User-Agent": "eleicoes-brasil-keepalive/1.0" },
  });
  const text = await res.text();
  return { status: res.status, body: text.slice(0, 200) };
}

export default {
  async fetch() {
    const result = await ping();
    return Response.json({ ok: true, ...result, at: new Date().toISOString() });
  },
  async scheduled(event, env, ctx) {
    ctx.waitUntil(
      ping().then((r) => console.log("keepalive", JSON.stringify(r))).catch((e) => console.error(e)),
    );
  },
};
