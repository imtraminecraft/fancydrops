// FancyDrops server: a Cloudflare Worker with one KV namespace bound as WORN.
//
// Everything a player earns (drops counted, achievements, the cosmetics they unlock) lives HERE, not in the
// player's own files, so editing a file on your computer cannot give you anything.
// Every write is tied to a real Minecraft account: the mod first calls Mojang's joinServer with a random
// serverId, and this server asks Mojang (hasJoined) which account that was.
//
//   GET  /worn                 every player's worn cosmetics      { code: { s, f, a, t } }
//   POST /worn                 { name, serverId, suffix, fx, aura }   (only cosmetics you own are accepted)
//   POST /event                { name, serverId, events: [ { g, src, id, item, r, rng, v, n } ] }   drops you found
//   GET  /profile/<code>       stats, every achievement with progress, and the cosmetics you have unlocked
//
// A player's code is the SHA-256 of their lowercase dashed UUID (Cosmetics.code() in the mod).

const CORS = { "Access-Control-Allow-Origin": "*" };
const COSMETICS_JSON = "https://raw.githubusercontent.com/imtraminecraft/fancydrops/main/cosmetics.json";
const OWNER = "7ad5d2b31b239f2ed148d1de18f8214967bf7d1a411c838d466e9829ab7c2bf8";    // the mod owner (Cosmetics.OWNER): owns every cosmetic; more admins can be listed as "admins" in cosmetics.json

// ---------------------------------------------------------------- achievements (the only place they are defined)
// stat(p) reads the player's stats; goal is the number to reach. reward: the cosmetic it unlocks.
const CH = { ledger: "The Ledger", fortune: "Fortune", myth: "Mythological", rare: "Rare Drops", farm: "Farming", fish: "Fishing", dye: "Dyes" };          // the same pages as the menu
const ACH = [
  // The Ledger: everything together
  { id: "first_find", ch: "ledger", name: "First Find", desc: "Get your first rare drop.", icon: "seal", goal: 1, stat: (p) => p.drops, reward: "sweep" },
  { id: "town_crier", ch: "ledger", name: "Town Crier", desc: "Get 10 rare drops.", icon: "bell", goal: 10, stat: (p) => p.drops, reward: "bell" },
  { id: "hoarder", ch: "ledger", name: "Hoarder", desc: "Get 100 rare drops.", icon: "sack", goal: 100, stat: (p) => p.drops, reward: "ripple" },
  { id: "packrat", ch: "ledger", name: "Pack Rat", desc: "Get 500 rare drops.", icon: "sack", goal: 500, stat: (p) => p.drops },
  { id: "collector", ch: "ledger", name: "Collector", desc: "Find 25 different items.", icon: "shelf", goal: 25, stat: (p) => Object.keys(p.uniq).length },
  { id: "curator", ch: "ledger", name: "Curator", desc: "Find 75 different items.", icon: "shelf", goal: 75, stat: (p) => Object.keys(p.uniq).length },
  { id: "regular", ch: "ledger", name: "Regular", desc: "Find drops on 7 different days.", icon: "clock", goal: 7, stat: (p) => Object.keys(p.days).length, reward: "clock" },
  { id: "devoted", ch: "ledger", name: "Devoted", desc: "Find drops on 30 different days.", icon: "clock", goal: 30, stat: (p) => Object.keys(p.days).length },
  // Fortune: RNG and coins
  { id: "rngesus", ch: "fortune", name: "RNGesus Smiles", desc: "Get your first RNG drop.", icon: "dice", goal: 1, stat: (p) => p.rng },
  { id: "treasure_hunter", ch: "fortune", name: "Treasure Hunter", desc: "Get 10 RNG drops worth 1M coins or more each.", icon: "chest", goal: 10, stat: (p) => p.bigRng, reward: "chest" },
  { id: "blessed", ch: "fortune", name: "Blessed", desc: "Get 50 RNG drops.", icon: "dice", goal: 50, stat: (p) => p.rng, reward: "dice" },
  { id: "chosen", ch: "fortune", name: "The Chosen One", desc: "Get 100 RNG drops.", icon: "crown", goal: 100, stat: (p) => p.rng },
  { id: "millionaire", ch: "fortune", name: "Millionaire", desc: "Find 10M coins worth of drops.", icon: "coin", goal: 10_000_000, stat: (p) => p.value, reward: "coinaura" },
  { id: "tycoon", ch: "fortune", name: "Tycoon", desc: "Find 1B coins worth of drops.", icon: "coin", goal: 1_000_000_000, stat: (p) => p.value },
  { id: "jackpot", ch: "fortune", name: "Jackpot", desc: "One drop worth 100M coins or more.", icon: "gem", goal: 1, stat: (p) => (p.best >= 100_000_000 ? 1 : 0), secret: true },
  { id: "pray", ch: "fortune", name: "Answered Prayers", desc: "Get 5 drops rated Pray RNGesus or rarer.", icon: "star", goal: 5, stat: (p) => p.r3, reward: "neon" },
  // Mythological
  { id: "digger", ch: "myth", name: "Digger", desc: "Get 25 treasure finds from burrows.", icon: "shovel", goal: 25, stat: (p) => g(p, "treasure").d },
  { id: "myth_hunter", ch: "myth", name: "Mythic Hunter", desc: "Get 25 drops from mythological creatures.", icon: "horn", goal: 25, stat: (p) => g(p, "myth").d, reward: "spark" },
  { id: "minos_bane", ch: "myth", name: "Minos' Bane", desc: "Get 5 RNG drops from mythological creatures.", icon: "horn", goal: 5, stat: (p) => g(p, "myth").rng },
  { id: "deep_digger", ch: "myth", name: "Deep Digger", desc: "Get 200 treasure finds.", icon: "shovel", goal: 200, stat: (p) => g(p, "treasure").d },
  // Rare Drops: slayers, the Crimson Isle, rare mobs and the Experimentation Table
  { id: "first_blood", ch: "rare", name: "First Blood", desc: "Get your first slayer rare drop.", icon: "sword", goal: 1, stat: (p) => g(p, "slayer").d },
  { id: "executioner", ch: "rare", name: "Executioner", desc: "Get 25 slayer rare drops.", icon: "anvil", goal: 25, stat: (p) => g(p, "slayer").d, reward: "anvil" },
  { id: "boss_breaker", ch: "rare", name: "Boss Breaker", desc: "Get 10 RNG slayer drops.", icon: "skull", goal: 10, stat: (p) => g(p, "slayer").rng },
  { id: "all_bosses", ch: "rare", name: "Monster Hunter", desc: "Get a rare drop from 4 different slayer bosses.", icon: "sword", goal: 4, stat: (p) => Object.keys(p.bosses).length },
  // Farming
  { id: "green_thumb", ch: "farm", name: "Green Thumb", desc: "Get 100 farming drops.", icon: "wheat", goal: 100, stat: (p) => farm(p), reward: "tractor" },
  { id: "exterminator", ch: "farm", name: "Exterminator", desc: "Get 50 pest drops.", icon: "bug", goal: 50, stat: (p) => g(p, "pests").d, reward: "fireflyaura" },
  { id: "crate_digger", ch: "farm", name: "Crate Digger", desc: "Find 5 different Vinyls.", icon: "disc", goal: 5, stat: (p) => Object.keys(p.vinyl).length, reward: "disc" },
  { id: "greenhouse", ch: "farm", name: "Greenhouse Keeper", desc: "Get 10 Harvest Bounty finds.", icon: "leaf", goal: 10, stat: (p) => g(p, "bounty").d },
  { id: "feast", ch: "farm", name: "Feast", desc: "Get 25 Harvest Feast drops.", icon: "wheat", goal: 25, stat: (p) => g(p, "feast").d },
    { id: "scholar", ch: "rare", name: "Scholar", desc: "Get 10 Experimentation Table rewards.", icon: "book", goal: 10, stat: (p) => g(p, "exp").d },
  { id: "mastermind", ch: "rare", name: "Mastermind", desc: "Get 5 RNG Experimentation Table rewards.", icon: "book", goal: 5, stat: (p) => g(p, "exp").rng, reward: "runeaura" },
  // Fishing and Dyes
  { id: "angler", ch: "fish", name: "Angler", desc: "Get 10 rare drops from sea creatures.", icon: "fish", goal: 10, stat: (p) => fish(p).d, reward: "bubbleaura" },
  { id: "deep_sea", ch: "fish", name: "Deep Sea Legend", desc: "Get 3 RNG drops from sea creatures.", icon: "fish", goal: 3, stat: (p) => fish(p).rng },
  { id: "isle_raider", ch: "rare", name: "Isle Raider", desc: "Get 10 rare drops from Crimson Isle bosses.", icon: "flame", goal: 10, stat: (p) => g(p, "crimson").d },
  { id: "rare_breed", ch: "rare", name: "Rare Breed", desc: "Get 10 rare drops in the Dwarven Mines.", icon: "skull", goal: 10, stat: (p) => g(p, "mobs").d },
  { id: "dragon_slayer", ch: "rare", name: "Dragon Slayer", desc: "Get 5 rare drops from Ender Dragons.", icon: "skull", goal: 5, stat: (p) => g(p, "dragon").d },
  { id: "nucleus", ch: "rare", name: "Heart of the Mountain", desc: "Get 10 rare Crystal Nucleus rewards.", icon: "gem", goal: 10, stat: (p) => g(p, "nucleus").d },
  { id: "living_colour", ch: "dye", name: "Living Colour", desc: "Find a dye.", icon: "dye", goal: 1, stat: (p) => g(p, "dyes").d, secret: true },
  { id: "palette", ch: "dye", name: "Palette", desc: "Find 3 different dyes.", icon: "dye", goal: 3, stat: (p) => Object.keys(p.dyes || {}).length },
  { id: "rainbow", ch: "dye", name: "Every Colour", desc: "Find 10 different dyes.", icon: "dye", goal: 10, stat: (p) => Object.keys(p.dyes || {}).length },
  { id: "sea_hunter", ch: "fish", name: "Sea Hunter", desc: "Get 100 rare drops from sea creatures.", icon: "fish", goal: 100, stat: (p) => fish(p).d },
];
const g = (p, k) => p.g[k] || { d: 0, rng: 0 };
const fish = (p) => ["fish_water", "fish_lava", "fish_spooky", "fish_winter"].reduce((a, k) => ({ d: a.d + g(p, k).d, rng: a.rng + g(p, k).rng }), { d: 0, rng: 0 });
const farm = (p) => ["crops", "feast", "bounty", "pests"].reduce((a, k) => a + g(p, k).d, 0);

// the Special and Beta sets, by grant key in cosmetics.json
const SETS = {
  sparkle: ["sparkle", "sparklefx", "sparkleaura"],
  bugfinder: ["bug", "glitch", "glitchaura"],
  contributors: ["code", "contribfx", "stardust"],
  patreon: ["patron", "royal", "patronaura"],
};
const GRANT_KEY = { sparkle: "sparkle", patreon: "patreon", bughunter: "bugfinder", contributor: "contributors" };
const BETA = ["betamark", "pulse"];

// ---------------------------------------------------------------- helpers
const json = (o, extra = {}) => new Response(JSON.stringify(o), { headers: { "content-type": "application/json", ...CORS, ...extra } });
const blankProfile = () => ({ drops: 0, rng: 0, bigRng: 0, value: 0, best: 0, r3: 0, uniq: {}, days: {}, vinyl: {}, bosses: {}, dyes: {}, g: {}, unlocked: {}, recent: [], first: Date.now() });

async function codeOf(uuid) {
  const d = await crypto.subtle.digest("SHA-256", new TextEncoder().encode(uuid));
  return [...new Uint8Array(d)].map((x) => x.toString(16).padStart(2, "0")).join("");
}
/** Asks Mojang which account just called joinServer with this serverId. Returns its code, or null. */
async function verify(b) {
  const name = String(b.name || ""), serverId = String(b.serverId || "");
  if (!/^[A-Za-z0-9_]{1,16}$/.test(name) || !/^[0-9a-f]{8,64}$/.test(serverId)) { verify.why = "bad request"; return null; }
  // Mojang's session server is shared by every Cloudflare worker and often answers 429 (too many requests) or 204 for a moment:
  // ask again a few times before giving up, and say why in the answer so the game's log shows it.
  for (let i = 0; i < 5; i++) {
    const r = await fetch(`https://sessionserver.mojang.com/session/minecraft/hasJoined?username=${encodeURIComponent(name)}&serverId=${serverId}`, { headers: { "user-agent": "FancyDrops-worker" } });
    if (r.status === 200) {
      const id = (await r.json()).id;
      return codeOf(`${id.slice(0, 8)}-${id.slice(8, 12)}-${id.slice(12, 16)}-${id.slice(16, 20)}-${id.slice(20)}`.toLowerCase());
    }
    verify.why = "mojang " + r.status;
    await new Promise((res) => setTimeout(res, 300 + i * 400));
  }
  return null;
}
/** Cosmetics given by hand (cosmetics.json), cached for 5 minutes. */
async function grantsFor(env, code) {
  let raw = await env.WORN.get("cosmetics_cache", "json");
  if (!raw || Date.now() - raw.at > 300_000) {
    try { const r = await fetch(COSMETICS_JSON); raw = { at: Date.now(), body: await r.json() }; await env.WORN.put("cosmetics_cache", JSON.stringify(raw)); } catch { raw = raw || { at: 0, body: {} }; }
  }
  const out = new Set(BETA), body = raw.body || {};
  for (const [key, codes] of Object.entries(body.special || {})) if (Array.isArray(codes) && codes.map((c) => String(c).toLowerCase()).includes(code)) for (const c of SETS[GRANT_KEY[key]] || []) out.add(c);
  for (const c of (body.extra || {})[code] || []) out.add(c);
  const admins = new Set([OWNER, ...(body.admins || [])].map((x) => String(x).toLowerCase()));
  return { owned: out, admin: admins.has(code) };
}
async function load(env, code) { return (await env.WORN.get("p:" + code, "json")) || blankProfile(); }
function view(p) {
  const list = ACH.map((a) => {
    const v = Math.min(a.stat(p), a.goal), done = !!p.unlocked[a.id];
    return { id: a.id, ch: a.ch, chapter: CH[a.ch], name: a.name, desc: a.desc, icon: a.icon, goal: a.goal, progress: v, done, at: p.unlocked[a.id] || 0, reward: a.reward || "", secret: !!a.secret };
  });
  const cosmetics = ACH.filter((a) => p.unlocked[a.id] && a.reward).map((a) => a.reward);
  const stats = { drops: p.drops, rng: p.rng, bigRng: p.bigRng, value: p.value, best: p.best, r3: p.r3, unique: Object.keys(p.uniq).length, days: Object.keys(p.days).length, bosses: Object.keys(p.bosses).length, vinyls: Object.keys(p.vinyl).length, dyes: Object.keys(p.dyes || {}).length, groups: p.g, since: p.first };
  return { stats, achievements: list, cosmetics, chapters: CH };
}

// ---------------------------------------------------------------- the server
export default {
  async fetch(req, env) {
    const url = new URL(req.url), path = url.pathname;

    if (path === "/worn" && req.method === "GET") return json((await env.WORN.get("all", "json")) || {}, { "cache-control": "max-age=30" });

    if (path.startsWith("/profile/") && req.method === "GET") {
      const code = path.slice(9).toLowerCase();
      if (!/^[0-9a-f]{64}$/.test(code)) return new Response("bad code", { status: 400 });
      return json(view(await load(env, code)), { "cache-control": "max-age=15" });
    }

    if (req.method !== "POST" || (path !== "/worn" && path !== "/event")) return new Response("FancyDrops", { status: 404 });
    let b;
    try { b = await req.json(); } catch { return new Response("bad json", { status: 400 }); }
    const code = await verify(b);
    if (!code) return new Response("not verified (" + (verify.why || "?") + ")", { status: 403 });

    if (path === "/worn") {
      const p = await load(env, code), { owned, admin } = await grantsFor(env, code);
      for (const a of ACH) if (p.unlocked[a.id] && a.reward) owned.add(a.reward);
      const ok = (v) => (typeof v === "string" && /^[a-z0-9_]{0,32}$/.test(v) && (v === "" || admin || owned.has(v)) ? v : "");    // only what you own
      const entry = { s: ok(b.suffix), f: ok(b.fx), a: ok(b.aura), t: Date.now() };
      const all = (await env.WORN.get("all", "json")) || {};
      if (!entry.s && !entry.f && !entry.a) delete all[code]; else all[code] = entry;
      await env.WORN.put("all", JSON.stringify(all));
      return json({ ok: true, worn: entry });
    }

    // /event: drops you found
    const events = Array.isArray(b.events) ? b.events.slice(0, 40) : [];
    const p = await load(env, code), now = Date.now();
    p.recent = (p.recent || []).filter((t) => now - t < 3_600_000);
    const day = new Date(now).toISOString().slice(0, 10);
    let taken = 0;
    for (const e of events) {
      if (p.recent.length >= 400) break;                                                  // more than 400 drops an hour is not real play
      const grp = String(e.g || ""), id = String(e.id || "").slice(0, 64), item = String(e.item || "").slice(0, 64);
      if (!/^[a-z_]{2,16}$/.test(grp) || !id) continue;
      const rng = !!e.rng, v = Math.max(0, Math.min(Number(e.v) || 0, 5_000_000_000)), r = Math.max(0, Math.min(3, Number(e.r) || 0));
      p.drops++; p.recent.push(now); taken++;
      if (rng) p.rng++;
      if (rng && v >= 1_000_000) p.bigRng++;
      if (r >= 3) p.r3++;
      p.value += v; p.best = Math.max(p.best, v);
      p.uniq[id] = 1; p.days[day] = 1;
      if (/vinyl/i.test(item)) p.vinyl[id] = 1;
      if (grp === "dyes") (p.dyes = p.dyes || {})[id] = 1;
      if (grp === "slayer" && e.src) p.bosses[String(e.src).slice(0, 24)] = 1;
      const gg = (p.g[grp] = p.g[grp] || { d: 0, rng: 0 }); gg.d++; if (rng) gg.rng++;
    }
    const fresh = [];
    for (const a of ACH) if (!p.unlocked[a.id] && a.stat(p) >= a.goal) { p.unlocked[a.id] = now; fresh.push(a.id); }
    await env.WORN.put("p:" + code, JSON.stringify(p));
    return json({ ok: true, taken, unlocked: fresh, ...view(p) });
  },
};
