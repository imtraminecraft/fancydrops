package dev.fancydrops;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.*;

/**
 * The Achievements page: an open leather ledger. Ribbons along the top are its chapters; the left page holds the chapter's
 * numbers and a wax seal, the right page its achievements as stamped gold coins (locked ones are inked outlines with a ring
 * that fills as you get closer). Turning to another chapter or page turns a page.
 * Everything shown comes from the server (Achievements.profile()).
 */
final class Ledger {
    private Ledger() {}

    private static final int INK = 0xFF3A2A1C, INK_SOFT = 0xFF7A6448, INK_FAINT = 0x5A7A6448, PAPER_L = 0xFFF2E7CF, PAPER_R = 0xFFEDE0C3, LEATHER = 0xFF3B1F18, LEATHER_HI = 0xFF5A3024, GOLD = 0xFFE8B84A, GOLD_D = 0xFF9A6A1E, GOLD_L = 0xFFFFE3A0;
    private static final String[] ORDER = {"ledger", "fortune", "myth", "bosses", "farm", "table", "wild"};
    private static final int[] RIBBON = {0xFFC9A23A, 0xFF8A4FD8, 0xFFE07A2A, 0xFFC0392B, 0xFF4FA83A, 0xFF5A7AD8, 0xFF2E9AB0};
    private static final String[] QUOTE = {"Every find, written down.", "Fortune favours the patient.", "Dig, and the earth answers.", "They drop what they guard.", "The harvest remembers.", "Read twice, find once.", "Out past the map's edge."};
    private static int chapter, page; private static long flipAt; private static int flipDir = 1;
    private static final List<int[]> HITS = new ArrayList<>();          // {x0, y0, x1, y1, action}: action 0..5 chapter, 10 prev page, 11 next page
    static String tip; static int tipRar; static boolean tipDone;

    private static void fill(GuiGraphicsExtractor g, int x0, int y0, int x1, int y1, int c) { g.fill(x0, y0, x1, y1, c); }
    private static void ink(GuiGraphicsExtractor g, Font f, String s, int x, int y, int c) { g.text(f, s, x, y, c, false); }
    private static void inkC(GuiGraphicsExtractor g, Font f, String s, int cx, int y, int c) { g.text(f, s, cx - f.width(s) / 2, y, c, false); }
    private static void inkS(GuiGraphicsExtractor g, Font f, String s, float x, float y, float k, int c) { Gfx.push(g); Gfx.translate(g, x, y); Gfx.scale(g, k); g.text(f, s, 0, 0, c, false); Gfx.pop(g); }
    private static void disc(GuiGraphicsExtractor g, int cx, int cy, float r, int c) {
        for (int dy = -(int) Math.ceil(r); dy <= Math.ceil(r); dy++) { int hw = (int) Math.round(Math.sqrt(Math.max(0, r * r - dy * dy))); if (hw > 0) g.fill(cx - hw, cy + dy, cx + hw, cy + dy + 1, c); }
    }
    private static void ring(GuiGraphicsExtractor g, int cx, int cy, float r, int c, int dash, float upto) {
        int n = Math.max(24, Math.round(r * 6));
        for (int i = 0; i < n; i++) {
            float u = i / (float) n; if (u > upto) break;
            if (dash > 0 && (i / dash) % 2 == 1) continue;
            double an = -Math.PI / 2 + u * Math.PI * 2;
            int x = cx + (int) Math.round(Math.cos(an) * r), y = cy + (int) Math.round(Math.sin(an) * r);
            g.fill(x, y, x + 1, y + 1, c);
        }
    }

    static String fmt(double v) { return CosmeticData.fmtN(v); }

    /** Draws the ledger into x,y,w,h. */
    static void draw(GuiGraphicsExtractor g, Font f, int x, int y, int w, int h, int mx, int my) {
        HITS.clear(); tip = null;
        long now = System.currentTimeMillis();
        Achievements.Profile p = Achievements.profile();
        // ---- the cover and the pages
        fill(g, x, y + 10, x + w, y + h, LEATHER);
        fill(g, x + 1, y + 11, x + w - 1, y + 12, LEATHER_HI);
        for (int[] cn : new int[][]{{x + 2, y + 12}, {x + w - 8, y + 12}, {x + 2, y + h - 8}, {x + w - 8, y + h - 8}}) { fill(g, cn[0], cn[1], cn[0] + 6, cn[1] + 1, GOLD_D); fill(g, cn[0], cn[1], cn[0] + 1, cn[1] + 6, GOLD_D); fill(g, cn[0] + 5, cn[1] + 5, cn[0] + 6, cn[1] + 6, GOLD); }
        int px0 = x + 7, px1 = x + w - 7, py0 = y + 15, py1 = y + h - 6, mid = (px0 + px1) / 2;
        fill(g, px0, py0, mid, py1, PAPER_L); fill(g, mid, py0, px1, py1, PAPER_R);
        for (int i = 0; i < 6; i++) { fill(g, mid - 6 + i, py0, mid - 5 + i, py1, (0x08 * (i + 1)) << 24); fill(g, mid + 5 - i, py0, mid + 6 - i, py1, (0x08 * (i + 1)) << 24); }       // the shadow in the gutter
        for (int ly = py0 + 49; ly < py1 - 44; ly += 11) { fill(g, px0 + 8, ly, mid - 10, ly + 1, 0x14604830); }          // ruled lines under the numbers
        fill(g, px0, py1, px1, py1 + 2, 0xFFD8C8A4); fill(g, px0 + 1, py1 + 2, px1 - 1, py1 + 3, 0xFFC4B28C);          // page edges

        // ---- the chapter ribbons hanging over the top edge
        int rw = 13, rx0 = px1 - 8 - ORDER.length * (rw + 3);
        for (int i = 0; i < ORDER.length; i++) {
            int rx = rx0 + i * (rw + 3); boolean act = i == chapter, hv = mx >= rx && mx < rx + rw && my >= y && my < py0 + 26;
            int len = act ? 30 : hv ? 22 : 16, col = RIBBON[i];
            fill(g, rx, y + 2, rx + rw, py0 + len - 4, col); fill(g, rx, y + 2, rx + 1, py0 + len - 4, 0x40000000);
            int tipY = py0 + len - 4; for (int k = 0; k < rw; k++) { int extra = Math.round(Math.abs(k - (rw - 1) / 2f) * 4f / ((rw - 1) / 2f)); fill(g, rx + k, tipY, rx + k + 1, tipY + extra, col); }      // a swallowtail end
            fill(g, rx + 2, y + 2, rx + 3, py0 + len - 5, 0x30FFFFFF);
            String lt = String.valueOf((p != null && p.chapters().containsKey(ORDER[i]) ? p.chapters().get(ORDER[i]) : ORDER[i]).charAt(0)).toUpperCase(Locale.ROOT);
            inkS(g, f, lt, rx + (rw - f.width(lt) * 0.75f) / 2f, py0 + len - 14, 0.75f, 0xFFFFFFFF);
            HITS.add(new int[]{rx, y, rx + rw, py0 + len, i});
        }

        if (p == null) {
            inkC(g, f, "The Ledger", (px0 + mid) / 2, py0 + 30, INK);
            inkC(g, f, Achievements.status.isEmpty() ? "Opening..." : Achievements.status, (px0 + mid) / 2, py0 + 50, INK_SOFT);
            inkC(g, f, "Your achievements are kept on the", (mid + px1) / 2, py0 + 40, INK_SOFT);
            inkC(g, f, "FancyDrops server, not on this PC.", (mid + px1) / 2, py0 + 52, INK_SOFT);
            return;
        }
        String chId = ORDER[Math.max(0, Math.min(ORDER.length - 1, chapter))];
        List<Achievements.Ach> list = new ArrayList<>(); for (Achievements.Ach a : p.list()) if (a.ch().equals(chId)) list.add(a);
        int done = 0; for (Achievements.Ach a : list) if (a.done()) done++;

        // ---- left page: the chapter's numbers
        int lx = px0 + 10, lw = mid - 10 - lx;
        inkS(g, f, p.chapters().getOrDefault(chId, chId), lx, py0 + 6, 1.5f, INK);
        fill(g, lx, py0 + 20, lx + 30, py0 + 21, RIBBON[chapter]);
        ink(g, f, done + " / " + list.size() + " achievements", lx, py0 + 25, INK_SOFT);
        int sy = py0 + 40;
        for (String[] row : statRows(chId, p)) {
            if (sy > py1 - 52) break;
            ink(g, f, row[0], lx, sy, INK);
            int vw = f.width(row[1]); ink(g, f, row[1], lx + lw - vw, sy, INK);
            for (int dx = lx + f.width(row[0]) + 3; dx < lx + lw - vw - 3; dx += 3) fill(g, dx, sy + 6, dx + 1, sy + 7, INK_FAINT);
            sy += 11;
        }
        int by = py1 - 40; float fr = list.isEmpty() ? 0f : done / (float) list.size();
        fill(g, lx, by, lx + lw, by + 5, 0x30604830); fill(g, lx, by, lx + Math.round(lw * fr), by + 5, GOLD); fill(g, lx, by, lx + Math.round(lw * fr), by + 1, GOLD_L);
        ink(g, f, Math.round(fr * 100) + "% of this chapter", lx, by + 8, INK_SOFT);
        inkS(g, f, QUOTE[chapter], lx, py1 - 14, 0.75f, INK_SOFT);
        int sx = mid - 26, scy = by - 4;                                                             // the wax seal: stamped when the chapter is complete
        if (fr >= 1f) { disc(g, sx, scy, 11, 0xFF9E2A22); disc(g, sx, scy, 8, 0xFFB8352B); ring(g, sx, scy, 9.5f, 0xFF7A1E18, 0, 1f); inkC(g, f, "✦", sx, scy - 4, 0xFFF2C0B0); }
        else ring(g, sx, scy, 10, INK_FAINT, 2, 1f);

        // ---- right page: the achievements
        int rx = mid + 10, rwid = px1 - 10 - rx, cols = 3, rows = Math.max(1, (py1 - py0 - 70) / 46), per = cols * rows, pages = Math.max(1, (list.size() + per - 1) / per);
        page = Math.max(0, Math.min(pages - 1, page));
        String hd = (p.chapters().getOrDefault(chId, chId)).toUpperCase(Locale.ROOT);
        ink(g, f, hd, rx, py0 + 8, RIBBON[chapter]); fill(g, rx, py0 + 18, rx + Math.min(f.width(hd), rx0 - rx - 8), py0 + 19, INK_FAINT);          // left of the ribbons
        int cw = rwid / cols;
        for (int k = 0; k < per; k++) {
            int idx = page * per + k; if (idx >= list.size()) break;
            Achievements.Ach a = list.get(idx);
            int bx = rx + (k % cols) * cw + cw / 2, byy = py0 + 62 + (k / cols) * 46;
            boolean hv = Math.abs(mx - bx) <= 14 && my >= byy - 14 && my <= byy + 26;
            float prog = (float) Math.min(1.0, a.goal() <= 0 ? 0 : a.progress() / a.goal());
            if (a.done()) {
                float bob = hv ? 1f : 0f;
                disc(g, bx, byy + 1, 12.5f, 0x40000000);
                disc(g, bx, byy - (int) bob, 12, GOLD_D); disc(g, bx, byy - (int) bob, 10.5f, GOLD); disc(g, bx, byy - (int) bob, 8.5f, 0xFFF4C860);
                ring(g, bx, byy - (int) bob, 11.3f, 0xFFB8862A, 1, 1f);
                fill(g, bx - 5, byy - 7 - (int) bob, bx - 2, byy - 6 - (int) bob, 0xC0FFFFFF);
                Icons.draw(g, iconFor(a.icon()), bx - 8, byy - 8 - (int) bob, hv ? now : 2400L);
            } else {
                ring(g, bx, byy, 11, hv ? INK_SOFT : INK_FAINT, 2, 1f);
                if (prog > 0f) ring(g, bx, byy, 12.5f, GOLD, 0, prog);
                inkC(g, f, "?", bx, byy - 4, hv ? INK_SOFT : INK_FAINT);
            }
            String nm = a.secret() && !a.done() ? "???" : a.name();
            List<String> lines = wrap(f, nm, cw - 4);
            for (int li = 0; li < Math.min(2, lines.size()); li++) inkS(g, f, lines.get(li), bx - f.width(lines.get(li)) * 0.75f / 2f, byy + 15 + li * 7, 0.75f, a.done() ? INK : INK_SOFT);
            if (hv) {
                String rew = a.reward().isEmpty() ? "" : rewardName(a.reward());
                String how = (a.secret() && !a.done() ? "A secret. Keep finding drops." : a.desc()) + (rew.isEmpty() ? "" : "  Unlocks the " + rew + ".");
                String extra = a.done() ? "Unlocked " + new java.text.SimpleDateFormat("d MMM yyyy").format(new Date(a.at())) : fmt(a.progress()) + " / " + fmt(a.goal());
                tip = nm + "\n" + p.chapters().getOrDefault(chId, chId) + "\n" + (a.done() ? 3 : 0) + "\n" + (a.done() ? "1" : "0") + "\n" + extra + "\n" + how; tipDone = a.done();
            }
        }
        // page arrows and number
        String pg = "page " + (page + 1) + " of " + pages; inkS(g, f, pg, rx + rwid / 2f - f.width(pg) * 0.75f / 2f, py1 - 12, 0.75f, INK_SOFT);
        if (page > 0) { boolean hv = mx >= rx && mx < rx + 14 && my >= py1 - 16 && my < py1 - 2; ink(g, f, "‹", rx + 2, py1 - 14, hv ? INK : INK_SOFT); HITS.add(new int[]{rx, py1 - 16, rx + 14, py1 - 2, 10}); }
        if (page < pages - 1) { boolean hv = mx >= px1 - 24 && mx < px1 - 10 && my >= py1 - 16 && my < py1 - 2; ink(g, f, "›", px1 - 20, py1 - 14, hv ? INK : INK_SOFT); HITS.add(new int[]{px1 - 24, py1 - 16, px1 - 10, py1 - 2, 11}); }

        // ---- a page turning (0.32 s): a sheet sweeps over the gutter
        float ft = (now - flipAt) / 320f;
        if (ft < 1f) {
            float e = ft * ft * (3 - 2 * ft); int pw = mid - px0;
            int sheetW = Math.round(pw * (float) Math.abs(Math.cos(e * Math.PI)));
            boolean onRight = flipDir > 0 ? e < 0.5f : e >= 0.5f;
            int x0 = onRight ? mid : mid - sheetW, x1 = onRight ? mid + sheetW : mid;
            fill(g, x0, py0, x1, py1, onRight ? PAPER_R : PAPER_L);
            int shade = Math.round(90 * (float) Math.sin(e * Math.PI)); fill(g, onRight ? x1 - 3 : x0, py0, onRight ? x1 : x0 + 3, py1, shade << 24);
        }
    }

    static boolean click(double mx, double my) {
        for (int[] h : HITS) if (mx >= h[0] && mx < h[2] && my >= h[1] && my < h[3]) {
            if (h[4] < ORDER.length) { if (h[4] != chapter) { flipDir = h[4] > chapter ? 1 : -1; chapter = h[4]; page = 0; flipAt = System.currentTimeMillis(); turnSound(); } }
            else if (h[4] == 10) { page--; flipDir = -1; flipAt = System.currentTimeMillis(); turnSound(); }
            else if (h[4] == 11) { page++; flipDir = 1; flipAt = System.currentTimeMillis(); turnSound(); }
            return true;
        }
        return false;
    }
    private static void turnSound() { SoundFx.Fx fx = SoundFx.byName("exp_flip"); if (fx != null) SoundFx.play(fx, 1.1f, 0.6f); }

    private static List<String[]> statRows(String ch, Achievements.Profile p) {
        Map<String, Double> s = p.stats(); List<String[]> o = new ArrayList<>();
        java.util.function.Function<String, Double> st = k -> s.getOrDefault(k, 0.0);
        java.util.function.BiFunction<String, Integer, Double> gr = (k, i) -> p.groups().containsKey(k) ? p.groups().get(k)[i] : 0.0;
        switch (ch) {
            case "fortune" -> { o.add(new String[]{"RNG drops", fmt(st.apply("rng"))}); o.add(new String[]{"RNG worth 1M+", fmt(st.apply("bigRng"))}); o.add(new String[]{"Pray RNGesus+", fmt(st.apply("r3"))}); o.add(new String[]{"Coins found", fmt(st.apply("value"))}); o.add(new String[]{"Best drop", fmt(st.apply("best"))}); }
            case "myth" -> { o.add(new String[]{"Treasure finds", fmt(gr.apply("treasure", 0))}); o.add(new String[]{"Creature drops", fmt(gr.apply("myth", 0))}); o.add(new String[]{"Creature RNG", fmt(gr.apply("myth", 1))}); }
            case "bosses" -> { o.add(new String[]{"Slayer drops", fmt(gr.apply("slayer", 0))}); o.add(new String[]{"Slayer RNG", fmt(gr.apply("slayer", 1))}); o.add(new String[]{"Bosses", fmt(st.apply("bosses"))}); }
            case "farm" -> { o.add(new String[]{"Rare Crops", fmt(gr.apply("crops", 0))}); o.add(new String[]{"Harvest Feast", fmt(gr.apply("feast", 0))}); o.add(new String[]{"Harvest Bounty", fmt(gr.apply("bounty", 0))}); o.add(new String[]{"Pest drops", fmt(gr.apply("pests", 0))}); o.add(new String[]{"Vinyls", fmt(st.apply("vinyls"))}); }
            case "table" -> { o.add(new String[]{"Table rewards", fmt(gr.apply("exp", 0))}); o.add(new String[]{"Table RNG", fmt(gr.apply("exp", 1))}); }
            case "wild" -> { double fd = 0, fr = 0; for (String k : new String[]{"fish_water", "fish_lava", "fish_spooky", "fish_winter"}) { fd += gr.apply(k, 0); fr += gr.apply(k, 1); }
                o.add(new String[]{"Sea creature drops", fmt(fd)}); o.add(new String[]{"Sea creature RNG", fmt(fr)}); o.add(new String[]{"Crimson Isle drops", fmt(gr.apply("crimson", 0))}); o.add(new String[]{"Rare mob drops", fmt(gr.apply("mobs", 0))}); o.add(new String[]{"Dyes", fmt(st.apply("dyes"))}); }
            default -> { o.add(new String[]{"Rare drops", fmt(st.apply("drops"))}); o.add(new String[]{"Different items", fmt(st.apply("unique"))}); o.add(new String[]{"RNG drops", fmt(st.apply("rng"))}); o.add(new String[]{"Coins found", fmt(st.apply("value"))}); o.add(new String[]{"Days with drops", fmt(st.apply("days"))});
                if (p.since() > 0) o.add(new String[]{"Ledger since", new java.text.SimpleDateFormat("d MMM yyyy").format(new Date(p.since()))}); }
        }
        return o;
    }
    private static String rewardName(String id) { CosmeticData.Def d = CosmeticData.byId(id); return d == null ? id : d.name() + " " + CosmeticData.kindLabel(d.kind()).toLowerCase(Locale.ROOT); }
    /** The picture on a badge: the mod's own icons. */
    private static String iconFor(String icon) {
        String k = switch (icon) {
            case "bell" -> "bell"; case "clock" -> "clock"; case "dice" -> "dice"; case "chest" -> "treasure"; case "coin" -> "coin"; case "shovel" -> "shovel";
            case "horn" -> "mytho"; case "sword", "skull" -> "slayers"; case "anvil" -> "anvil"; case "wheat", "leaf" -> "grp_crops"; case "bug" -> "farm"; case "disc" -> "disc"; case "fish" -> "fishing"; case "flame" -> "grp_crimson"; case "dye" -> "dye";
            case "book" -> "raredrops"; case "shelf", "sack" -> "dropped"; case "crown" -> "cosmetics"; case "gem", "star" -> "animations"; default -> "stats";
        };
        return ":" + k + ":";
    }
    private static List<String> wrap(Font f, String s, int max) {
        List<String> o = new ArrayList<>(); String cur = "";
        for (String wd : s.split(" ")) { String t = cur.isEmpty() ? wd : cur + " " + wd; if (f.width(t) * 0.75f > max && !cur.isEmpty()) { o.add(cur); cur = wd; } else cur = t; }
        if (!cur.isEmpty()) o.add(cur); return o;
    }
}
