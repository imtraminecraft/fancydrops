// FancyDrops "worn cosmetics" server: a Cloudflare Worker with one KV namespace bound as WORN.
// GET  /worn  -> { "<player code>": { "s": suffix, "f": name effect, "a": aura, "t": time }, ... }
// POST /worn  <- { name, serverId, suffix, fx, aura }: the mod first calls Mojang's joinServer with serverId,
//                 so hasJoined below proves the request really comes from that player.
// The player code is the SHA-256 of the lowercase dashed UUID, the same as Cosmetics.code() in the mod.
// The mod only ever shows what the player was given (cosmetics.json), so this list can only choose among those.

const CORS = { "Access-Control-Allow-Origin": "*" };

export default {
  async fetch(req, env) {
    const url = new URL(req.url);
    if (url.pathname !== "/worn") return new Response("FancyDrops worn cosmetics", { status: 404 });

    if (req.method === "GET") {
      const all = (await env.WORN.get("all", "json")) || {};
      return new Response(JSON.stringify(all), { headers: { "content-type": "application/json", "cache-control": "max-age=30", ...CORS } });
    }

    if (req.method === "POST") {
      let b;
      try { b = await req.json(); } catch { return new Response("bad json", { status: 400 }); }
      const name = String(b.name || ""), serverId = String(b.serverId || "");
      if (!/^[A-Za-z0-9_]{1,16}$/.test(name) || !/^[0-9a-f]{8,64}$/.test(serverId)) return new Response("bad request", { status: 400 });

      const r = await fetch(`https://sessionserver.mojang.com/session/minecraft/hasJoined?username=${name}&serverId=${serverId}`);
      if (r.status !== 200) return new Response("not verified", { status: 403 });
      const id = (await r.json()).id;
      const uuid = `${id.slice(0, 8)}-${id.slice(8, 12)}-${id.slice(12, 16)}-${id.slice(16, 20)}-${id.slice(20)}`.toLowerCase();
      const digest = await crypto.subtle.digest("SHA-256", new TextEncoder().encode(uuid));
      const code = [...new Uint8Array(digest)].map((x) => x.toString(16).padStart(2, "0")).join("");

      const ok = (v) => (typeof v === "string" && /^[a-z0-9_]{0,32}$/.test(v) ? v : "");
      const entry = { s: ok(b.suffix), f: ok(b.fx), a: ok(b.aura), t: Date.now() };
      const all = (await env.WORN.get("all", "json")) || {};
      if (!entry.s && !entry.f && !entry.a) delete all[code]; else all[code] = entry;
      await env.WORN.put("all", JSON.stringify(all));
      return new Response("ok", { headers: CORS });
    }
    return new Response("method not allowed", { status: 405 });
  },
};
