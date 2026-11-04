package dev.fancydrops;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import org.joml.Matrix3x2f;

import java.util.*;

/**
 * The Collection: your achievements and your cosmetics in one leather-bound book that pops out of the menu.
 *
 * The button (bottom right of the menu) throws the closed book up into the middle of the screen; the cover swings open on its spine and the
 * pages settle. Bookmarks stick out of both edges: on the right the ledger (Contents and the seven achievement chapters), on the left the
 * wardrobe (Sets, Name Icons, Name Effects, Auras). Pages turn like paper: the sheet lifts off the spine, leans and lands on the other side,
 * with its front on one side and the next page printed on its back. Esc, a click outside, or the seal in the corner closes it again.
 */
final class Codex {
    private Codex() {}

    /** What the menu lends the book: its font, the chat / tab preview, the aura stage, toasts and the hover card. */
    interface Host {
        Font font();
        default int width(String s) { return font().width(s); }
        void chatPreview(GuiGraphicsExtractor g, int x, int y, int w, int h);
        void auraStage(GuiGraphicsExtractor g, int x, int y, int w, int h, String aura);
        void toast(String text, int col);
        void tip(String lines, int col);
    }

    // ---- colours: leather, paper, ink, gold
    private static final int LEATHER = 0xFF4A2418, LEATHER_D = 0xFF2E140E, LEATHER_L = 0xFF6A3424, PAPER = 0xFFF3E8D0, PAPER_D = 0xFFE4D4B2,
            INK = 0xFF3A2A1C, INK_SOFT = 0xFF7A6448, INK_FAINT = 0x557A6448, GOLD = 0xFFE8B84A, GOLD_D = 0xFF9A6A1E, GOLD_L = 0xFFFFE3A0;

    // ---- chapters: the ledger on the right edge, the wardrobe on the left
    // the ledger's chapters are the menu's own pages, in the same order
    private static final String[] ACH = {"ledger", "fortune", "myth", "rare", "farm", "fish", "dye"};
    private static final String[] ACH_NAME = {"The Ledger", "Fortune", "Mythological", "Rare Drops", "Farming", "Fishing", "Dyes"};
    private static final int[] ACH_COL = {0xFFC9A23A, 0xFF8A4FD8, 0xFF3AA8D8, 0xFFC0392B, 0xFF4FA83A, 0xFF2E9AB0, 0xFFE8489A};
    private static final String[] ACH_ICON = {"stats", "coin", "mytho", "raredrops", "farm", "fishing", "dye"};
    private static final String[] QUOTE = {"Every find, written down.", "Fortune favours the patient.", "Dig, and the earth answers.", "They drop what they guard.", "The harvest remembers.", "Patience, then a splash.", "Every colour has a story."};
    // the wardrobe: Rewards first (every cosmetic the ledger gives, next to the achievement that gives it), then each kind, then the sets
    private static final String[] WARD = {"rewards", "icons", "effects", "auras", "sets"};
    private static final String[] WARD_NAME = {"Rewards", "Name Icons", "Name Effects", "Auras", "Sets"};
    private static final int[] WARD_COL = {0xFFE8B84A, 0xFFFFC857, 0xFFFF9AC8, 0xFF8FD0FF, 0xFFC77DFF};
    private static final String[] WARD_ICON = {"trophy", "cos_suffix", "cos_name", "cos_aura", "cosmetics"};
    /** Chapter numbers: 0 contents, 1..7 the ledger, 8..12 the wardrobe. */
    private static final int CONTENTS = 0, FIRST_ACH = 1, FIRST_WARD = 8, CHAPTERS = 8;          // achievements only: cosmetics have their own page again

    static boolean open;
    private static long openAt, closeAt;
    private static int[] from = {0, 0, 0, 0};
    private static int ch, sp;                                  // the chapter and its spread on show
    private static int aCh, aSp, bCh, bSp, flipDir; private static long flipAt = -1;
    private static final long FLY = 380, COVER = 560, CLOSE_COVER = 380, CLOSE_FLY = 300, FLIP = 520;
    private static final List<Object[]> HITS = new ArrayList<>();
    private static String hoverAura; private static final Map<String, Long> SHAKE = new HashMap<>();
    private static boolean sOpen, sLand;

    // ------------------------------------------------------------------ open / close
    /** Opens the book from the button at `btn` (x0, y0, x1, y1), on a chapter (-1 = where it was left). */
    static void open(int[] btn, int chapter) {
        open = true; openAt = System.currentTimeMillis(); closeAt = 0; from = btn.clone(); flipAt = -1; sOpen = sLand = false;
        if (chapter >= 0) { ch = chapter; sp = 0; }
        Achievements.refresh(true);
        sound("book_whoosh", 1f, 0.8f);
    }
    static void openWardrobe(int[] btn) { open(btn, FIRST_WARD + 1); }
    static final int ICONS_CHAPTER = FIRST_WARD + 1;
    static void close() { if (open && closeAt == 0) { closeAt = System.currentTimeMillis(); sound("book_close", 1f, 0.9f); } }
    static boolean busy() { return open; }

    private static void sound(String name, float pitch, float gain) { try { SoundFx.Fx fx = SoundFx.byName(name); if (fx == null) fx = SoundFx.byName("exp_flip"); if (fx != null) SoundFx.play(fx, pitch, gain); } catch (Throwable ignored) { } }
    private static float cl(float v) { return v < 0 ? 0 : v > 1 ? 1 : v; }
    private static float ease(float v) { v = cl(v); return v * v * v * (v * (v * 6 - 15) + 10); }
    private static float back(float v) { v = cl(v); float c1 = 1.5f, c3 = c1 + 1, q = v - 1; return 1 + c3 * q * q * q + c1 * q * q; }
    private static int mix(int a, int b, float f) { return Canvas.mix(a, b, cl(f)); }
    private static int argb(float a, int rgb) { return Gfx.argb(Math.round(cl(a) * 255), rgb & 0xFFFFFF); }

    // ------------------------------------------------------------------ the pages in order
    private static int spreads(int c) {
        if (c == CONTENTS) return 1;
        if (c < FIRST_WARD) { int n = achOf(ACH[c - FIRST_ACH]).size(); return Math.max(1, (n + perAch - 1) / perAch); }
        int n = wardItems(c - FIRST_WARD).size(), per = c == FIRST_WARD ? perReward : perWard; return Math.max(1, (n + per - 1) / per);
    }
    private static int perAch = 9, perWard = 9, perReward = 5;
    private static List<Achievements.Ach> achOf(String id) {
        Achievements.Profile p = Achievements.profile(); List<Achievements.Ach> l = new ArrayList<>();
        if (p != null) for (Achievements.Ach a : p.list()) if (a.ch().equals(id)) l.add(a);
        return l;
    }
    private static List<Object> wardItems(int w) {
        List<Object> l = new ArrayList<>();
        if (w == 4) { for (int r = 4; r >= 0; r--) for (CosmeticData.SetDef s : CosmeticData.SETS) if (s.rarity() == r) l.add(s); return l; }
        if (w == 0) {                                                                      // Rewards: in the ledger's order
            Achievements.Profile p = Achievements.profile();
            if (p != null) for (Achievements.Ach a : p.list()) if (!a.reward().isEmpty() && CosmeticData.byId(a.reward()) != null) l.add(a);
            return l;
        }
        CosmeticData.Kind k = w == 1 ? CosmeticData.Kind.SUFFIX : w == 2 ? CosmeticData.Kind.NAME : CosmeticData.Kind.AURA;
        List<Object> got = new ArrayList<>(), rest = new ArrayList<>();
        for (int r = 4; r >= 0; r--) {                                                     // what you own first, then the rest, rarest first in each
            for (CosmeticData.SetDef s : CosmeticData.SETS) if (s.rarity() == r) for (CosmeticData.Def d : CosmeticData.members(s)) if (d.kind() == k) (CosmeticData.unlocked(d) ? got : rest).add(d);
            for (CosmeticData.Def d : CosmeticData.ALL) if (d.kind() == k && d.set() == null && CosmeticData.rarityOf(d) == r) (CosmeticData.unlocked(d) ? got : rest).add(d);
        }
        l.addAll(got); l.addAll(rest); return l;
    }
    /** Turns to the next (+1) or previous (-1) spread, crossing into the next chapter at the end of one. */
    static void turn(int dir) {
        if (!ready()) return;
        int c = ch, s = sp + dir;
        if (s >= spreads(c)) { c++; s = 0; } else if (s < 0) { c--; s = c >= 0 ? spreads(c) - 1 : 0; }
        if (c < 0 || c >= CHAPTERS) return;
        go(c, s, dir);
    }
    private static void go(int c, int s, int dir) {
        if (c == ch && s == sp) return;
        aCh = ch; aSp = sp; bCh = c; bSp = s; flipDir = dir; flipAt = System.currentTimeMillis(); ch = c; sp = s;
        sound("book_page", 0.92f + (float) Math.random() * 0.16f, 0.85f);
    }
    private static void jump(int c) { if (c != ch) { go(c, 0, c > ch ? 1 : -1); sound("book_tab", 1f, 0.5f); } }
    private static boolean ready() { long now = System.currentTimeMillis(); return open && closeAt == 0 && now - openAt > FLY + COVER && (flipAt < 0 || now - flipAt > FLIP * 0.7f); }

    // ------------------------------------------------------------------ drawing
    private static int W, H, pw, bh, spine, top;
    private static Font font; private static Host host; private static int mx, my; private static long now;

    static void draw(GuiGraphicsExtractor g, Host h, int width, int height, int mx0, int my0) {
        if (!open) return;
        now = System.currentTimeMillis(); host = h; font = h.font(); W = width; H = height; mx = mx0; my = my0; HITS.clear(); hoverAura = null;
        tryIcon = nTryIcon; tryFx = nTryFx; tryAura = nTryAura; nTryIcon = nTryFx = nTryAura = null;
        bh = Math.min(H - 34, 340); pw = Math.max(160, Math.min((W - 110) / 2, Math.round(bh * 0.86f))); top = (H - bh) / 2 + 4;
        perAch = 3 * Math.max(1, (bh - 84) / 52); perWard = 3 * Math.max(1, (bh - 80) / 56); perReward = Math.max(2, (bh - 64) / 40);
        long t = now - openAt;
        float closeT = closeAt == 0 ? -1f : (now - closeAt);
        if (closeT >= CLOSE_COVER + CLOSE_FLY) { open = false; return; }

        // how far each part has come: fly (0..1), cover (0 closed .. 1 open)
        float fly, cover;
        if (closeT >= 0) { cover = 1f - ease(closeT / CLOSE_COVER); fly = 1f - ease((closeT - CLOSE_COVER) / CLOSE_FLY); }
        else { fly = back(t / (float) FLY); cover = ease((t - FLY) / (float) COVER); }
        if (closeT < 0 && !sOpen && t > FLY) { sOpen = true; sound("book_open", 1f, 0.9f); }
        if (closeT < 0 && !sLand && t > FLY + COVER) { sLand = true; sound("book_land", 1f, 0.7f); }

        float dim = closeT >= 0 ? 1f - cl((closeT - CLOSE_COVER * 0.4f) / (CLOSE_COVER * 0.6f + CLOSE_FLY)) : cl(t / 260f);
        g.fill(0, 0, W, H, argb(0.62f * dim, 0x06040A));
        for (int i = 0; i < 6; i++) { int in = Math.round(Math.min(W, H) * 0.06f * i); g.fill(0, 0, W, in, argb(0.05f * dim, 0)); g.fill(0, H - in, W, H, argb(0.05f * dim, 0)); }
        dust(g, dim);

        spine = Math.round(W / 2f - pw / 2f * (1f - cover));                      // closed, the cover sits where the right page will be
        float s = 0.18f + 0.82f * fly, cx = W / 2f - pw / 2f * (1f - cover) + pw / 2f, cy = top + bh / 2f;
        float bx = (from[0] + from[2]) / 2f, byy = (from[1] + from[3]) / 2f;
        float ox = (bx - cx) * (1f - fly), oy = (byy - cy) * (1f - fly) - (float) Math.sin(Math.PI * cl(fly)) * 26f;
        Gfx.push(g);
        Gfx.translate(g, cx + ox, cy + oy); Gfx.rotate(g, -0.45f * (1f - fly)); Gfx.scale(g, s); Gfx.translate(g, -cx, -cy);
        int fade = Math.round(255 * cl(fly * 3f));
        if (fade > 4) {
            drawBook(g, cover, closeT < 0 && cover >= 1f);
        }
        Gfx.pop(g);
        if (closeT < 0 && cover >= 1f && t < FLY + COVER + 700) burst(g, (t - FLY - COVER) / 700f);
    }

    /** Gold dust drifting over the dimmed menu. */
    private static void dust(GuiGraphicsExtractor g, float a) {
        for (int i = 0; i < 28; i++) {
            float u = ((now / 9000f) + i * 0.0357f) % 1f, x = (Ritual.rnd(i * 13) * W + (float) Math.sin(now / 1700.0 + i) * 20) % W, y = H - u * H * 1.1f;
            int al = Math.round(120 * a * (float) Math.sin(u * Math.PI));
            g.fill(Math.round(x), Math.round(y), Math.round(x) + 1, Math.round(y) + 1, Gfx.argb(al, i % 3 == 0 ? 0xFFF4C8 : 0xE8B84A));
        }
    }
    /** Sparks thrown from the spine as the book lands open. */
    private static void burst(GuiGraphicsExtractor g, float u) {
        if (u >= 1f) return;
        for (int i = 0; i < 26; i++) {
            float an = Ritual.rnd(i * 7) * 6.2832f, sp2 = 40 + Ritual.rnd(i * 3) * 120, x = spine + (float) Math.cos(an) * sp2 * u, y = top + bh / 2f + (float) Math.sin(an) * sp2 * u * 0.6f + 30 * u * u;
            int al = Math.round(230 * (1f - u)); g.fill(Math.round(x), Math.round(y), Math.round(x) + 2, Math.round(y) + 1, Gfx.argb(al, i % 2 == 0 ? 0xFFE3A0 : 0xFFFFFF)); g.fill(Math.round(x), Math.round(y) - 1, Math.round(x) + 1, Math.round(y) + 2, Gfx.argb(al / 2, 0xFFE3A0));
        }
    }

    private static void drawBook(GuiGraphicsExtractor g, float cover, boolean live) {
        int x0 = spine - pw, x1 = spine + pw, y0 = top, y1 = top + bh;
        boolean opened = cover > 0.5f;
        // ---- the back board (and, as the cover opens, the left board)
        float k = cl((cover - 0.5f) * 2f);
        int lb = Math.round(spine - (pw + 9) * k);
        g.fill(Math.min(lb, spine - 3) + 6, y0 + 4, x1 + 15, y1 + 15, 0x40000000);                // the shadow on the table
        g.fill(Math.min(lb, spine - 3), y0 - 7, x1 + 9, y1 + 9, LEATHER_D);
        g.fill(Math.min(lb + 2, spine - 1), y0 - 5, x1 + 7, y1 + 7, LEATHER);
        for (int i = 3; i >= 1; i--) {                                                            // the edges of the paper block
            g.fill(spine, y0 + i, x1 + i * 2, y1 + i, mix(PAPER_D, 0xFF000000, 0.08f * i));
            if (k > 0) g.fill(Math.round(spine - pw * k) - i * 2, y0 + i, spine, y1 + i, mix(PAPER_D, 0xFF000000, 0.08f * i));
        }
        // ---- the bookmarks (behind the pages, sticking out of the edges)
        if (live) tabs(g);
        // ---- the pages under the cover: the right page always, the left one as the cover lifts
        paper(g, spine, y0, pw, bh, false);
        if (opened) withSheet(g, -k, 0f, () -> paper(g, spine - pw, y0, pw, bh, true));
        boolean flipping = live && flipAt >= 0 && now - flipAt < FLIP;
        if (cover >= 1f) {
            if (!flipping) { page(g, ch, sp, 0, live); page(g, ch, sp, 1, live); }
            else flip(g);
        } else page(g, ch, sp, 1, false);
        gutter(g);
        // ---- the front cover swinging on the spine
        if (cover < 1f) {
            float th = cover * (float) Math.PI, c = (float) Math.cos(th);
            float shade = 0.45f * (float) Math.sin(th);
            if (c > 0f) withSheet(g, c, 0.10f * (float) Math.sin(th), () -> coverFront(g, spine, y0 - 7, pw + 9, bh + 16, shade));
            else withSheet(g, c, -0.10f * (float) Math.sin(th), () -> coverInside(g, spine - pw - 9, y0 - 7, pw + 9, bh + 16, shade));
        }
        if (live) seal(g, x1 + 4, y0 - 12);
    }

    /** Draws `r` squashed round the spine by s (positive: on the right side, negative: mirrored to the left side), the outer edge leaning by `lean`. */
    private static void withSheet(GuiGraphicsExtractor g, float s, float lean, Runnable r) {
        float a = Math.abs(s); if (a < 0.01f) return;
        Gfx.push(g);
        Matrix3x2f m = new Matrix3x2f(a, -lean * a, 0, 1, spine - a * spine, lean * a * spine);
        g.pose().mul(m);
        r.run();
        Gfx.pop(g);
    }

    private static void paper(GuiGraphicsExtractor g, int x, int y, int w, int h, boolean left) {
        g.fill(x, y, x + w, y + h, PAPER);
        int steps = 10; for (int i = 0; i < steps; i++) {                                        // darker towards the spine
            int bw = w / 5 / steps + 1, xx = left ? x + w - (i + 1) * bw : x + i * bw;
            g.fill(xx, y, xx + bw, y + h, argb(0.11f * (1f - i / (float) steps), 0x6A4A20));
        }
        for (int i = 0; i < 18; i++) { int fx = x + Math.round(Ritual.rnd(i * 31 + (left ? 7 : 0)) * w), fy = y + Math.round(Ritual.rnd(i * 17 + 3) * h); g.fill(fx, fy, fx + 1, fy + 1, 0x18604830); }     // flecks in the paper
    }
    private static void gutter(GuiGraphicsExtractor g) {
        for (int i = 0; i < 7; i++) { int a = Math.round(42 * (1f - i / 7f)); g.fill(spine - 1 - i, top, spine - i, top + bh, Gfx.argb(a, 0x3A2410)); g.fill(spine + i, top, spine + i + 1, top + bh, Gfx.argb(a, 0x3A2410)); }
    }
    private static void coverFront(GuiGraphicsExtractor g, int x, int y, int w, int h, float shade) {
        g.fill(x, y, x + w, y + h, LEATHER); g.fill(x, y, x + 3, y + h, LEATHER_D); g.fill(x + w - 2, y, x + w, y + h, LEATHER_L);
        int m = 8; frame(g, x + m, y + m, w - 2 * m, h - 2 * m, GOLD_D); frame(g, x + m + 3, y + m + 3, w - 2 * m - 6, h - 2 * m - 6, 0x80E8B84A);
        int cx = x + w / 2, cy = y + h / 2 - 10;
        for (int r = 22; r > 0; r -= 2) { int hw = r; g.fill(cx - hw, cy - r / 4, cx + hw, cy + r / 4, argb(0.05f, 0xE8B84A)); }
        Gfx.push(g); Gfx.translate(g, cx - 12, cy - 18); Gfx.scale(g, 1.5f); Icons.draw(g, ":trophy:", 0, 0, 2400L); Gfx.pop(g);
        String tt = "ACHIEVEMENTS"; Gfx.push(g); Gfx.translate(g, cx - fw(tt) * 0.6f, cy + 14); Gfx.scale(g, 1.2f); g.text(font, tt, 1, 1, 0xFF2A140C, false); g.text(font, tt, 0, 0, GOLD, false); Gfx.pop(g);
        if (shade > 0.01f) g.fill(x, y, x + w, y + h, argb(shade, 0));
    }
    private static void coverInside(GuiGraphicsExtractor g, int x, int y, int w, int h, float shade) {
        g.fill(x, y, x + w, y + h, LEATHER); g.fill(x + 6, y + 6, x + w - 3, y + h - 6, 0xFF7A3A2A);                   // the endpaper, marbled
        for (int i = 0; i < 40; i++) { int px = x + 8 + Math.round(Ritual.rnd(i * 5) * (w - 14)), py = y + 8 + Math.round(Ritual.rnd(i * 9) * (h - 16)); g.fill(px, py, px + 4, py + 1, 0x30E8B84A); }
        if (shade > 0.01f) g.fill(x, y, x + w, y + h, argb(shade, 0));
    }
    private static void frame(GuiGraphicsExtractor g, int x, int y, int w, int h, int c) { g.fill(x, y, x + w, y + 1, c); g.fill(x, y + h - 1, x + w, y + h, c); g.fill(x, y, x + 1, y + h, c); g.fill(x + w - 1, y, x + w, y + h, c); }

    /** The close seal: a red wax disc with a gold cross, top right. */
    private static void seal(GuiGraphicsExtractor g, int x, int y) {
        boolean hv = mx >= x - 9 && mx < x + 9 && my >= y - 9 && my < y + 9;
        disc(g, x, y + 1, 9, 0x50000000); disc(g, x, y, 9, hv ? 0xFFC8402F : 0xFF9E2A22); disc(g, x, y, 7, hv ? 0xFFD84A36 : 0xFFB8352B);
        g.fill(x - 3, y - 3, x - 2, y - 2, GOLD_L); for (int i = -3; i <= 3; i++) { g.fill(x + i, y + i, x + i + 1, y + i + 1, GOLD_L); g.fill(x + i, y - i, x + i + 1, y - i + 1, GOLD_L); }
        HITS.add(new Object[]{x - 9, y - 9, x + 9, y + 9, (Runnable) Codex::close});
        if (hv) host.tip("Close", 0xFFC8402F);
    }
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

    // ---- the bookmarks
    private static void tabs(GuiGraphicsExtractor g) {
        int n = FIRST_WARD, th = Math.min(24, (bh - 16) / n - 3), y = top + 10;
        for (int i = 0; i < n; i++) {                                                          // right edge: Contents and the ledger
            int col = i == 0 ? 0xFFB8862A : ACH_COL[i - 1]; String ic = i == 0 ? "home" : ACH_ICON[i - 1]; String nm = i == 0 ? "Contents" : ACH_NAME[i - 1];
            tab(g, spine + pw + 9, y + i * (th + 3), th, col, ic, nm, i, true);
        }
        if (CHAPTERS > FIRST_WARD) for (int i = 0, wt = Math.min(30, (bh - 16) / WARD.length - 4); i < WARD.length; i++) tab(g, spine - pw - 9, top + 14 + i * (wt + 4), wt, WARD_COL[i], WARD_ICON[i], WARD_NAME[i], FIRST_WARD + i, false);
    }
    private static void tab(GuiGraphicsExtractor g, int edge, int y, int h, int col, String icon, String name, int c, boolean right) {
        boolean act = ch == c, hv = right ? mx >= edge && mx < edge + 24 && my >= y && my < y + h : mx >= edge - 24 && mx < edge && my >= y && my < y + h;
        int out = act ? 20 : hv ? 17 : 13;
        int x0 = right ? edge - 4 : edge - out, x1 = right ? edge + out : edge + 4;
        g.fill(x0, y + 1, x1, y + h + 1, 0x50000000);
        g.fill(x0, y, x1, y + h, act ? mix(col, 0xFFFFFFFF, 0.1f) : mix(col, 0xFF000000, hv ? 0.05f : 0.22f));
        g.fill(x0, y, x1, y + 1, mix(col, 0xFFFFFFFF, 0.35f)); g.fill(right ? x1 - 1 : x0, y, right ? x1 : x0 + 1, y + h, mix(col, 0xFF000000, 0.35f));
        float k = Math.min(0.75f, (h - 4) / 16f);
        Gfx.push(g); Gfx.translate(g, right ? x1 - 3 - 16 * k : x0 + 3, y + (h - 16 * k) / 2f); Gfx.scale(g, k); Icons.draw(g, ":" + icon + ":", 0, 0, hv || act ? now : 2400L); Gfx.pop(g);
        HITS.add(new Object[]{Math.min(x0, edge - (right ? 0 : 24)), y, Math.max(x1, edge + (right ? 24 : 0)), y + h, (Runnable) () -> jump(c)});
        if (hv) host.tip(name, col);
    }

    // ---- turning a page
    private static void flip(GuiGraphicsExtractor g) {
        float e = ease((now - flipAt) / (float) FLIP), th = e * (float) Math.PI, c = (float) Math.cos(th), lift = (float) Math.sin(th);
        boolean fwd = flipDir > 0;
        // the pages lying flat: the one being uncovered, and the one not moving
        if (fwd) { page(g, aCh, aSp, 0, false); page(g, bCh, bSp, 1, false); }
        else { page(g, bCh, bSp, 0, false); page(g, aCh, aSp, 1, false); }
        // the shadow the lifted sheet throws on them
        int reach = Math.round(pw * Math.abs(c));
        for (int i = 0; i < 12; i++) {
            int a = Math.round(70 * lift * (1f - i / 12f)), w = Math.max(1, pw / 24);
            boolean onRight = fwd ? c > 0 : c < 0;
            int x = onRight ? spine + reach + i * w : spine - reach - (i + 1) * w;
            if ((onRight && x < spine + pw) || (!onRight && x > spine - pw)) g.fill(Math.max(spine - pw, x), top, Math.min(spine + pw, x + w), top + bh, Gfx.argb(a, 0x2A1A08));
        }
        // the sheet itself: its front, then (past the spine's straight up) its back
        float lean = 0.09f * lift;
        if (fwd) {
            if (c > 0) withSheet(g, c, lean, () -> { paper(g, spine, top, pw, bh, false); page(g, aCh, aSp, 1, false); curl(g, spine, pw, true, lift); g.fill(spine, top, spine + pw, top + bh, argb(0.22f * (1 - c), 0x2A1A08)); g.fill(spine + pw - 2, top, spine + pw, top + bh, argb(0.5f * lift, 0xFFFFFF)); });
            else withSheet(g, c, -lean, () -> { paper(g, spine - pw, top, pw, bh, true); page(g, bCh, bSp, 0, false); curl(g, spine - pw, pw, false, lift); g.fill(spine - pw, top, spine, top + bh, argb(0.18f * (1 + c), 0xFFFFFF)); });
        } else {
            if (c > 0) withSheet(g, -c, -lean, () -> { paper(g, spine - pw, top, pw, bh, true); page(g, aCh, aSp, 0, false); curl(g, spine - pw, pw, false, lift); g.fill(spine - pw, top, spine, top + bh, argb(0.22f * (1 - c), 0x2A1A08)); g.fill(spine - pw, top, spine - pw + 2, top + bh, argb(0.5f * lift, 0xFFFFFF)); });
            else withSheet(g, -c, lean, () -> { paper(g, spine, top, pw, bh, false); page(g, bCh, bSp, 1, false); curl(g, spine, pw, true, lift); g.fill(spine, top, spine + pw, top + bh, argb(0.18f * (1 + c), 0xFFFFFF)); });
        }
    }

    /** The bend of a lifted sheet: light along the rising middle, shadow towards the spine and the free edge (x0..x0+w, spine side first when fromLeft). */
    private static void curl(GuiGraphicsExtractor g, int x0, int w, boolean spineLeft, float lift) {
        if (lift < 0.02f) return;
        int n = 14, bw = Math.max(1, w / n);
        for (int i = 0; i < n; i++) {
            float u = (i + 0.5f) / n, bend = (float) Math.sin(u * Math.PI);                  // 0 at the spine and the edge, 1 in the middle
            int xx = spineLeft ? x0 + i * bw : x0 + w - (i + 1) * bw;
            float shadow = (1f - u) * (1f - u) * 0.35f * lift, light = bend * 0.18f * lift;
            g.fill(xx, top, xx + bw + 1, top + bh, argb(shadow, 0x2A1A08));
            g.fill(xx, top, xx + bw + 1, top + bh, argb(light, 0xFFFFFF));
        }
    }

    // ------------------------------------------------------------------ what is printed on a page
    /** Page `side` (0 left, 1 right) of chapter c, spread s. live = it takes the mouse (only when nothing moves). */
    private static void page(GuiGraphicsExtractor g, int c, int s, int side, boolean live) {
        int x = side == 0 ? spine - pw + 14 : spine + 12, y = top + 12, w = pw - 26, h = bh - 24;
        if (c == CONTENTS) { if (side == 0) contentsLeft(g, x, y, w, h); else contentsRight(g, x, y, w, h, live); }
        else if (c < FIRST_WARD) { if (side == 0) achLeft(g, c - FIRST_ACH, x, y, w, h); else achRight(g, c - FIRST_ACH, s, x, y, w, h, live); }
        else { if (side == 0) wardLeft(g, c - FIRST_WARD, x, y, w, h, live); else wardRight(g, c - FIRST_WARD, s, x, y, w, h, live); }
        String num = String.valueOf(pageNumber(c, s) * 2 + side + 1);                     // the page number at the foot
        small(g, num, side == 0 ? spine - pw + 14 : spine + pw - 14 - Math.round(fw(num) * 0.75f), top + bh - 11, INK_SOFT);
        if (live && side == 1) {                                                             // the corner you can lift to turn
            boolean more = !(c == CHAPTERS - 1 && s == spreads(c) - 1);
            if (more) corner(g, spine + pw, top + bh, true);
        }
        if (live && side == 0 && (c > 0 || s > 0)) corner(g, spine - pw, top + bh, false);
    }
    private static int pageNumber(int c, int s) { int n = 0; for (int i = 0; i < c; i++) n += spreads(i); return n + s; }
    private static void corner(GuiGraphicsExtractor g, int x, int y, boolean right) {
        boolean hv = right ? mx >= x - 20 && mx < x && my >= y - 20 && my < y : mx >= x && mx < x + 20 && my >= y - 20 && my < y;
        int k = hv ? 12 : 7;
        for (int i = 0; i < k; i++) { int len = k - i; if (right) g.fill(x - len, y - i - 1, x, y - i, i == 0 ? 0x30000000 : mix(PAPER_D, 0xFFFFFFFF, 0.2f)); else g.fill(x, y - i - 1, x + len, y - i, mix(PAPER_D, 0xFFFFFFFF, 0.2f)); }
        if (hv) { String a = right ? "›" : "‹"; g.text(font, a, right ? x - 9 : x + 4, y - 11, INK, false); }
        HITS.add(new Object[]{right ? x - 22 : x, y - 22, right ? x : x + 22, y, (Runnable) () -> turn(right ? 1 : -1)});
    }

    private static int fw(String s) { return host.width(s); }
    private static void ink(GuiGraphicsExtractor g, String s, int x, int y, int c) { g.text(font, s, x, y, c, false); }
    private static void small(GuiGraphicsExtractor g, String s, int x, int y, int c) { Gfx.push(g); Gfx.translate(g, x, y); Gfx.scale(g, 0.75f); g.text(font, s, 0, 0, c, false); Gfx.pop(g); }
    private static void title(GuiGraphicsExtractor g, String s, int x, int y, int col) { Gfx.push(g); Gfx.translate(g, x, y); Gfx.scale(g, 1.5f); g.text(font, s, 0, 0, INK, false); Gfx.pop(g); g.fill(x, y + 15, x + 34, y + 16, col); }
    private static List<String> wrap(String s, int max, float k) {
        List<String> o = new ArrayList<>(); String cur = "";
        for (String wd : s.split(" ")) { String t = cur.isEmpty() ? wd : cur + " " + wd; if (fw(t) * k > max && !cur.isEmpty()) { o.add(cur); cur = wd; } else cur = t; }
        if (!cur.isEmpty()) o.add(cur); return o;
    }
    private static void leader(GuiGraphicsExtractor g, String l, String r, int x, int y, int w) {
        ink(g, l, x, y, INK); int vw = fw(r); ink(g, r, x + w - vw, y, INK);
        for (int dx = x + fw(l) + 3; dx < x + w - vw - 3; dx += 3) g.fill(dx, y + 6, dx + 1, y + 7, INK_FAINT);
    }
    private static void bar(GuiGraphicsExtractor g, int x, int y, int w, float fr, int col) {
        g.fill(x, y, x + w, y + 5, 0x30604830); g.fill(x, y, x + Math.round(w * cl(fr)), y + 5, col); g.fill(x, y, x + Math.round(w * cl(fr)), y + 1, mix(col, 0xFFFFFFFF, 0.5f));
    }

    // ---- contents
    private static int achDone() { Achievements.Profile p = Achievements.profile(); int n = 0; if (p != null) for (Achievements.Ach a : p.list()) if (a.done()) n++; return n; }
    private static int achAll() { Achievements.Profile p = Achievements.profile(); return p == null ? 0 : p.list().size(); }
    private static int cosOwned() { int n = 0; for (CosmeticData.Def d : CosmeticData.ALL) if (CosmeticData.unlocked(d)) n++; return n; }
    private static void contentsLeft(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        title(g, "Achievements", x, y, GOLD_D);
        String who; try { who = net.minecraft.client.Minecraft.getInstance().getUser().getName(); } catch (Throwable e) { who = "you"; }
        small(g, "kept for " + who, x, y + 21, INK_SOFT);
        int r = Math.min(30, w / 5), cy = y + 46 + r;
        dial(g, x + w / 4, cy, r, achAll() == 0 ? 0f : achDone() / (float) achAll(), GOLD, achDone() + "/" + achAll(), "achievements");
        int chDone = 0; for (String id : ACH) { List<Achievements.Ach> l = achOf(id); boolean all = !l.isEmpty(); for (Achievements.Ach a : l) all &= a.done(); if (all) chDone++; }
        dial(g, x + w * 3 / 4, cy, r, chDone / (float) ACH.length, 0xFFC77DFF, chDone + "/" + ACH.length, "chapters done");
        int yy = cy + r + 22;
        Achievements.Profile p = Achievements.profile();
        if (p == null) { for (String l : wrap(Achievements.status.isEmpty() ? "Opening the ledger..." : Achievements.status, w, 1f)) { ink(g, l, x, yy, INK_SOFT); yy += 10; } return; }
        ink(g, "Latest", x, yy, INK); g.fill(x, yy + 10, x + w, yy + 11, INK_FAINT); yy += 15;
        List<Achievements.Ach> got = new ArrayList<>(); for (Achievements.Ach a : p.list()) if (a.done()) got.add(a);
        got.sort((a, b) -> Long.compare(b.at(), a.at()));
        if (got.isEmpty()) { for (String l : wrap("Nothing yet. Your first rare drop opens the first page. Click a chapter on the right to see what you can earn.", w, 0.75f)) { small(g, l, x, yy, INK_SOFT); yy += 8; } }
        for (int i = 0; i < Math.min(3, got.size()) && yy < y + h - 12; i++) {
            Achievements.Ach a = got.get(i);
            disc(g, x + 5, yy + 3, 4, GOLD); ink(g, a.name(), x + 14, yy, INK);
            String d = new java.text.SimpleDateFormat("d MMM").format(new Date(a.at())); small(g, d, x + w - Math.round(fw(d) * 0.75f), yy + 1, INK_SOFT); yy += 12;
        }
    }
    private static void dial(GuiGraphicsExtractor g, int cx, int cy, int r, float fr, int col, String big, String label) {
        ring(g, cx, cy, r, INK_FAINT, 0, 1f); float shown = fr * ease((now - openAt - FLY - COVER) / 1200f);
        for (int k = 0; k < 3; k++) ring(g, cx, cy, r - 1 - k * 0.6f, col, 0, shown);
        ink(g, big, cx - fw(big) / 2, cy - 7, INK);
        small(g, label, cx - Math.round(fw(label) * 0.375f), cy + 4, INK_SOFT);
    }
    private static void contentsRight(GuiGraphicsExtractor g, int x, int y, int w, int h, boolean live) {
        ink(g, "CHAPTERS", x, y + 2, GOLD_D); g.fill(x, y + 12, x + w, y + 13, INK_FAINT);
        int yy = y + 20, rowH = Math.max(11, Math.min(16, (h - 40) / 13));
        Achievements.Profile p = Achievements.profile();
        for (int c = FIRST_ACH; c < CHAPTERS; c++) {
            if (c == FIRST_WARD) { yy += 4; small(g, "THE WARDROBE", x, yy, INK_SOFT); yy += 9; }
            String nm = c < FIRST_WARD ? ACH_NAME[c - FIRST_ACH] : WARD_NAME[c - FIRST_WARD];
            int col = c < FIRST_WARD ? ACH_COL[c - FIRST_ACH] : WARD_COL[c - FIRST_WARD];
            String right;
            if (c < FIRST_WARD) { List<Achievements.Ach> l = achOf(ACH[c - FIRST_ACH]); int d = 0; for (Achievements.Ach a : l) if (a.done()) d++; right = p == null ? "·" : d + " of " + l.size() + " done"; }
            else { List<Object> l = wardItems(c - FIRST_WARD); int d = 0; for (Object o : l) if (o instanceof CosmeticData.Def df ? CosmeticData.unlocked(df) : o instanceof Achievements.Ach aa ? CosmeticData.unlockedId(aa.reward()) : CosmeticData.owned((CosmeticData.SetDef) o) > 0) d++; right = d + "/" + l.size(); }
            boolean hv = live && mx >= x && mx < x + w && my >= yy - 2 && my < yy + rowH - 2;
            g.fill(x, yy + 1, x + 3, yy + 7, col);
            leader(g, nm, right, x + 7, yy, w - 7);          // how many of the chapter's achievements you have (click the row to open it)
            if (hv) g.fill(x + 7, yy + 9, x + 7 + fw(nm), yy + 10, col);
            final int cc = c; if (live) HITS.add(new Object[]{x, yy - 2, x + w, yy + rowH - 2, (Runnable) () -> jump(cc)});
            yy += rowH;
        }
    }

    // ---- a ledger chapter
    private static void achLeft(GuiGraphicsExtractor g, int i, int x, int y, int w, int h) {
        Achievements.Profile p = Achievements.profile();
        title(g, ACH_NAME[i], x, y, ACH_COL[i]);
        if (p == null) { int yy = y + 26; for (String l : wrap(Achievements.status.isEmpty() ? "Opening the ledger..." : Achievements.status, w, 1f)) { ink(g, l, x, yy, INK_SOFT); yy += 10; } small(g, "Your achievements are kept on the FancyDrops server, not on this PC.", x, yy + 6, INK_SOFT); return; }
        List<Achievements.Ach> l = achOf(ACH[i]); int d = 0; for (Achievements.Ach a : l) if (a.done()) d++;
        ink(g, d + " / " + l.size() + " achievements", x, y + 21, INK_SOFT);
        int sy = y + 38;
        for (String[] row : Ledger.statRows(ACH[i], p)) { if (sy > y + h - 52) break; leader(g, row[0], row[1], x, sy, w); sy += 11; }
        int by = y + h - 36; float fr = l.isEmpty() ? 0f : d / (float) l.size();
        bar(g, x, by, w - 30, fr, GOLD); small(g, Math.round(fr * 100) + "% of this chapter", x, by + 8, INK_SOFT);
        small(g, QUOTE[i], x, y + h - 10, INK_SOFT);
        int sx = x + w - 12, scy = by + 1;                                                        // the wax seal: stamped when the chapter is complete
        if (fr >= 1f) { disc(g, sx, scy, 11, 0xFF9E2A22); disc(g, sx, scy, 8, 0xFFB8352B); ring(g, sx, scy, 9.5f, 0xFF7A1E18, 0, 1f); ink(g, "✦", sx - 3, scy - 4, 0xFFF2C0B0); }
        else ring(g, sx, scy, 10, INK_FAINT, 2, 1f);
    }
    private static void achRight(GuiGraphicsExtractor g, int i, int s, int x, int y, int w, int h, boolean live) {
        Achievements.Profile p = Achievements.profile();
        ink(g, ACH_NAME[i].toUpperCase(Locale.ROOT), x, y + 2, ACH_COL[i]); g.fill(x, y + 12, x + w, y + 13, INK_FAINT);
        if (p == null) return;
        List<Achievements.Ach> l = achOf(ACH[i]); int cols = 3, cw = w / cols;
        for (int k = 0; k < perAch; k++) {
            int idx = s * perAch + k; if (idx >= l.size()) break;
            Achievements.Ach a = l.get(idx);
            int bx = x + (k % cols) * cw + cw / 2, byy = y + 34 + (k / cols) * 50;
            boolean hv = live && Math.abs(mx - bx) <= cw / 2 - 2 && my >= byy - 15 && my <= byy + 30;
            float prog = (float) Math.min(1.0, a.goal() <= 0 ? 0 : a.progress() / a.goal());
            int bob = hv ? 1 : 0;
            if (a.done()) {
                disc(g, bx, byy + 2, 13, 0x40000000);
                disc(g, bx, byy - bob, 13, GOLD_D); disc(g, bx, byy - bob, 11.5f, GOLD); disc(g, bx, byy - bob, 9.5f, 0xFFF4C860);
                ring(g, bx, byy - bob, 12.3f, 0xFFB8862A, 1, 1f);
                float sh = ((now / 2600f) + idx * 0.21f) % 1f; if (sh < 0.25f) { int sx = bx - 10 + Math.round(sh * 80); g.fill(sx, byy - 9 - bob, sx + 2, byy + 9 - bob, 0x50FFFFFF); }     // a glint crosses the coin now and then
                Icons.draw(g, Ledger.iconFor(a.icon()), bx - 8, byy - 8 - bob, hv ? now : 2400L);
            } else {
                ring(g, bx, byy, 12, hv ? INK_SOFT : INK_FAINT, 2, 1f);
                if (prog > 0f) { ring(g, bx, byy, 13.5f, GOLD, 0, prog); ring(g, bx, byy, 14f, GOLD, 0, prog); }
                ink(g, "?", bx - 2, byy - 4, hv ? INK_SOFT : INK_FAINT);
            }
            CosmeticData.Def rw = a.reward().isEmpty() ? null : CosmeticData.byId(a.reward());
            if (rw != null) {                                                                // the reward hangs off the coin as a small medal
                int rx = bx + 11, ry = byy + 9;
                disc(g, rx, ry, 7, 0x50000000); disc(g, rx, ry - 1, 7, a.done() ? mix(rw.color() | 0xFF000000, 0xFF000000, 0.3f) : 0xFFD8CCB0); disc(g, rx, ry - 1, 5.5f, a.done() ? 0xFF1A1622 : 0xFFE8DCC0);
                Gfx.push(g); Gfx.translate(g, rx - 5, ry - 6); Gfx.scale(g, 0.625f);
                if (rw.kind() == CosmeticData.Kind.NAME) { var cp = Cosmetics.nameComp("A", 0x55FFFF, rw.id(), true, now); g.text(font, cp, 5, 4, 0xFFFFFFFF, false); }
                else Icons.draw(g, rw.kind() == CosmeticData.Kind.SUFFIX ? Cosmetics.token(rw.id()) : Icons.tok("cx_" + rw.id()), 0, 0, 2400L);
                Gfx.pop(g);
                if (!a.done()) disc(g, rx, ry - 1, 5.5f, 0x80E8DCC0);
            }
            String nm = a.secret() && !a.done() ? "???" : a.name();
            List<String> lines = wrap(nm, cw - 4, 0.75f);
            for (int li = 0; li < Math.min(2, lines.size()); li++) small(g, lines.get(li), bx - Math.round(fw(lines.get(li)) * 0.375f), byy + 17 + li * 8, a.done() ? INK : INK_SOFT);
            if (hv) {
                String rew = a.reward().isEmpty() ? "" : Ledger.rewardName(a.reward());
                String how = (a.secret() && !a.done() ? "A secret. Keep finding drops." : a.desc()) + (rew.isEmpty() ? "" : "  Unlocks the " + rew + ".");
                String extra = a.done() ? "Unlocked " + new java.text.SimpleDateFormat("d MMM yyyy").format(new Date(a.at())) : Ledger.fmt(a.progress()) + " / " + Ledger.fmt(a.goal());
                host.tip(nm + "\n" + ACH_NAME[i] + "\n" + (a.done() ? 3 : 0) + "\n" + (a.done() ? "1" : "0") + "\n" + extra + "\n" + how, a.done() ? GOLD : 0xFF807891);
            }
        }
        int n = spreads(FIRST_ACH + i); if (n > 1) small(g, (s + 1) + " of " + n, x + w / 2 - 8, y + h - 10, INK_SOFT);
    }

    // ---- a wardrobe chapter
    // what the mouse rests on (read by the left page next frame, so you can try a cosmetic on before you own or wear it)
    private static String tryIcon, tryFx, tryAura, nTryIcon, nTryFx, nTryAura;
    private static void wardLeft(GuiGraphicsExtractor g, int i, int x, int y, int w, int h, boolean live) {
        title(g, WARD_NAME[i], x, y, WARD_COL[i]);
        List<Object> l = wardItems(i); int d = 0;
        for (Object o : l) if (o instanceof CosmeticData.Def df ? CosmeticData.unlocked(df) : o instanceof Achievements.Ach a ? CosmeticData.unlockedId(a.reward()) : CosmeticData.owned((CosmeticData.SetDef) o) > 0) d++;
        ink(g, d + " / " + l.size() + (i == 0 ? " earned" : " unlocked"), x, y + 21, INK_SOFT);
        int yy = y + 34;
        if (i == 0) { for (String ln : wrap("Every cosmetic the ledger gives, next to the achievement that earns it. Click one you have to wear it.", w, 0.85f)) { med(g, ln, x, yy, INK_SOFT); yy += 9; } yy += 4; }
        String[] kinds = {"Name icon", "Name effect", "Aura"};
        String[] worn = {nameOf(FancyConfig.get().suffix), nameOf(FancyConfig.get().nameFx), nameOf(FancyConfig.get().aura)};
        for (int k = 0; k < 3; k++) { leader(g, kinds[k], worn[k].isEmpty() ? "-" : worn[k], x, yy, w); yy += 11; }
        yy += 4;
        // the mirror: how you look right now, or with the thing under the mouse tried on
        boolean auraPage = i == 3 || i == 4 || i == 0;
        int fx = x - 2, fw = w + 4, fy = yy + 2, fh = y + h - 14 - fy;
        if (fh < 40) return;
        int mh = Math.min(fh, auraPage ? 52 : 70);
        frameCard(g, fx, fy, fw, auraPage ? fh : mh);
        if (!live) { g.fill(fx, fy, fx + fw, fy + (auraPage ? fh : mh), 0xFF14161E); return; }
        g.enableScissor(fx, fy, fx + fw, fy + (auraPage ? fh : mh));
        mirror(g, fx, fy, fw, mh);
        if (auraPage && fh - mh > 30) {
            String au = tryAura != null ? tryAura : CosmeticData.unlockedId(FancyConfig.get().aura) ? FancyConfig.get().aura : null;
            host.auraStage(g, fx, fy + mh, fw, fh - mh, au);
        }
        g.disableScissor();
        small(g, tryIcon != null || tryFx != null || tryAura != null ? "Trying it on" : i == 3 ? "Hover an aura to try it on." : "Hover anything to try it on.", x, y + h - 10, INK_SOFT);
    }
    /** A dark card with photo corners, pasted on the page. */
    private static void frameCard(GuiGraphicsExtractor g, int fx, int fy, int fw, int fh) {
        g.fill(fx - 3, fy - 3, fx + fw + 3, fy + fh + 3, 0xFFFBF6EA); g.fill(fx - 3, fy + fh + 3, fx + fw + 3, fy + fh + 4, 0x30000000);
        g.fill(fx, fy, fx + fw, fy + fh, 0xFF16141C);
        for (int q = 0; q < 4; q++) { int cx = q % 2 == 0 ? fx - 3 : fx + fw + 3, cy = q < 2 ? fy - 3 : fy + fh + 3, dx = q % 2 == 0 ? 1 : -1, dy = q < 2 ? 1 : -1; for (int k = 0; k < 7; k++) g.fill(Math.min(cx, cx + dx * (7 - k)), cy + dy * k, Math.max(cx, cx + dx * (7 - k)), cy + dy * k + 1, 0xFF5A3A24); }
    }
    /** Your name in chat and on your name tag, wearing what you wear (or what is being tried on). */
    private static void mirror(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        String who; try { who = net.minecraft.client.Minecraft.getInstance().getUser().getName(); } catch (Throwable e) { who = "You"; }
        String fx = tryFx != null ? tryFx : CosmeticData.unlockedId(FancyConfig.get().nameFx) ? FancyConfig.get().nameFx : null;
        String ic = tryIcon != null ? tryIcon : CosmeticData.unlockedId(FancyConfig.get().suffix) ? FancyConfig.get().suffix : null;
        Cosmetics.Suffix suf = ic == null ? null : Cosmetics.byId(ic);
        net.minecraft.network.chat.Style plain = net.minecraft.network.chat.Style.EMPTY;
        net.minecraft.network.chat.MutableComponent name = Component.empty().append(Component.literal("[MVP+] ").withStyle(plain.withColor(0x55FFFF))).append(Cosmetics.nameComp(who, 0x55FFFF, fx, true, now));
        if (suf != null) name.append(Component.literal(" ")).append(Cosmetics.chatGlyph(suf).copy().withStyle(plain.withColor(0xFFFFFF)));
        net.minecraft.network.chat.MutableComponent chat = name.copy().append(Component.literal(": gg!").withStyle(plain.withColor(0xFFFFFF)));
        small(g, "IN CHAT", x + 5, y + 4, 0xFF8A8296);
        fitText(g, chat, x + 5, y + 13, w - 10);
        small(g, "NAME TAG", x + 5, y + 28, 0xFF8A8296);
        int tw = Math.min(w - 10, host.width(name.getString()) + 8), tx = x + (w - tw) / 2, ty = y + 37;
        g.fill(tx, ty, tx + tw, ty + 11, 0x66000000);
        fitText(g, name, tx + 4, ty + 2, tw - 8);
    }
    /** Draws a line, squeezed to fit `max` when it is wider. */
    private static void fitText(GuiGraphicsExtractor g, Component c, int x, int y, int max) {
        int wd = host.width(c.getString()); float k = wd > max ? max / (float) wd : 1f;
        Gfx.push(g); Gfx.translate(g, x, y + (1 - k) * 4); Gfx.scale(g, k); g.text(font, c, 0, 0, 0xFFFFFFFF, true); Gfx.pop(g);
    }
    private static void med(GuiGraphicsExtractor g, String s, int x, int y, int c) { Gfx.push(g); Gfx.translate(g, x, y); Gfx.scale(g, 0.85f); g.text(font, s, 0, 0, c, false); Gfx.pop(g); }
    private static String nameOf(String id) { CosmeticData.Def d = id == null ? null : CosmeticData.byId(id); return d != null && CosmeticData.unlocked(d) ? d.name() : ""; }

    private static void wardRight(GuiGraphicsExtractor g, int i, int s, int x, int y, int w, int h, boolean live) {
        ink(g, WARD_NAME[i].toUpperCase(Locale.ROOT), x, y + 2, mix(WARD_COL[i], 0xFF000000, 0.35f)); g.fill(x, y + 12, x + w, y + 13, INK_FAINT);
        List<Object> l = wardItems(i);
        if (i == 0) {                                                                       // Rewards: one row each, the cosmetic, the achievement that gives it and how far along you are
            for (int k = 0; k < perReward; k++) {
                int idx = s * perReward + k; if (idx >= l.size()) break;
                rewardRow(g, (Achievements.Ach) l.get(idx), x, y + 20 + k * 40, w, live);
            }
            if (Achievements.profile() == null) for (String ln : wrap(Achievements.status.isEmpty() ? "Opening the ledger..." : Achievements.status, w, 1f)) { ink(g, ln, x, y + 24, INK_SOFT); y += 10; }
        } else {
            int cols = 3, cw = w / cols;
            for (int k = 0; k < perWard; k++) {
                int idx = s * perWard + k; if (idx >= l.size()) break;
                Object o = l.get(idx);
                int bx = x + (k % cols) * cw + cw / 2, byy = y + 36 + (k / cols) * 56;
                boolean hv = live && Math.abs(mx - bx) <= cw / 2 - 2 && my >= byy - 17 && my <= byy + 32;
                if (o instanceof CosmeticData.SetDef sd) setTile(g, sd, bx, byy, cw, hv, live);
                else defTile(g, (CosmeticData.Def) o, bx, byy, cw, hv, live, i);
            }
        }
        int n = spreads(FIRST_WARD + i); if (n > 1) small(g, (s + 1) + " of " + n, x + w / 2 - 8, y + h - 10, INK_SOFT);
    }
    /** One reward: its medallion, its name and kind, the achievement (and chapter) that earns it, and a progress bar. */
    private static void rewardRow(GuiGraphicsExtractor g, Achievements.Ach a, int x, int y, int w, boolean live) {
        CosmeticData.Def d = CosmeticData.byId(a.reward()); if (d == null) return;
        boolean owned = CosmeticData.unlocked(d), worn = owned && CosmeticData.isEquipped(d);
        boolean hv = live && mx >= x && mx < x + w && my >= y - 2 && my < y + 36;
        if (hv) g.fill(x - 3, y - 2, x + w + 3, y + 36, 0x14604830);
        int bx = x + 15, byy = y + 15;
        medallion(g, bx, byy, d.color() | 0xFF000000, owned, worn, hv);
        drawCos(g, d, bx, byy, owned, hv || worn);
        ink(g, d.name(), x + 36, y + 2, owned ? INK : INK_SOFT);
        String kind = CosmeticData.kindLabel(d.kind()); int kx = x + 36 + fw(d.name()) + 5;
        if (kx + Math.round(fw(kind) * 0.75f) < x + w) small(g, kind, kx, y + 3, mix(d.color() | 0xFF000000, 0xFF000000, 0.35f));
        String chap = ""; for (int c = 0; c < ACH.length; c++) if (ACH[c].equals(a.ch())) chap = ACH_NAME[c];
        small(g, (a.done() ? "Earned with " : "Earn ") + "\"" + (a.secret() && !a.done() ? "???" : a.name()) + "\"  ·  " + chap, x + 36, y + 13, INK_SOFT);
        float fr = (float) Math.min(1.0, a.goal() <= 0 ? 0 : a.progress() / a.goal());
        String st = worn ? "worn" : owned ? "click to wear" : Ledger.fmt(a.progress()) + " / " + Ledger.fmt(a.goal());
        int sw = Math.round(fw(st) * 0.75f);
        bar(g, x + 36, y + 24, w - 36 - sw - 6, fr, a.done() ? GOLD : mix(GOLD, PAPER_D, 0.35f));
        small(g, st, x + w - sw, y + 23, worn ? GOLD_D : INK_SOFT);
        if (!live) return;
        HITS.add(new Object[]{x - 3, y - 2, x + w + 3, y + 36, (Runnable) () -> clickDef(d)});
        if (hv) { tryOn(d); host.tip(d.name() + "\n" + kind + "\n" + CosmeticData.rarityOf(d) + "\n" + (owned ? "1" : "0") + "\n" + (worn ? "Worn now. Click to take it off." : owned ? "Click to wear it." : "Earn \"" + a.name() + "\" first.") + "\n" + a.desc(), d.color() | 0xFF000000); }
    }
    private static void tryOn(CosmeticData.Def d) { if (d.kind() == CosmeticData.Kind.SUFFIX) nTryIcon = d.id(); else if (d.kind() == CosmeticData.Kind.NAME) nTryFx = d.id(); else nTryAura = d.id(); }
    /** The cosmetic's own picture on its medallion: the icon, "Aa" in the name effect, or the aura's picture. */
    private static void drawCos(GuiGraphicsExtractor g, CosmeticData.Def d, int bx, int byy, boolean owned, boolean moving) {
        if (d.kind() == CosmeticData.Kind.NAME) { var comp = Cosmetics.nameComp("Aa", 0x55FFFF, d.id(), true, now); g.text(font, comp, bx - fw("Aa") / 2, byy - 4, 0xFFFFFFFF, true); }
        else Icons.draw(g, d.kind() == CosmeticData.Kind.SUFFIX ? Cosmetics.token(d.id()) : Icons.tok("cx_" + d.id()), bx - 8, byy - 8, moving ? now : 2400L);
        if (!owned) disc(g, bx, byy, 11.5f, argb(0.45f, 0xD8CCB0));
    }
    private static float shake(String id) { Long t = SHAKE.get(id); if (t == null) return 0f; float u = (now - t) / 400f; if (u >= 1f) { SHAKE.remove(id); return 0f; } return (float) Math.sin(u * 30) * 3f * (1f - u); }
    private static void medallion(GuiGraphicsExtractor g, int bx, int byy, int col, boolean owned, boolean worn, boolean hv) {
        int bob = hv ? 1 : 0;
        disc(g, bx, byy + 2, 15, 0x40000000);
        if (owned) {
            disc(g, bx, byy - bob, 15, mix(col, 0xFF000000, 0.45f)); disc(g, bx, byy - bob, 13.5f, mix(col, 0xFF000000, 0.15f)); disc(g, bx, byy - bob, 11.5f, 0xFF1A1622);
            if (worn) { for (int k = 0; k < 2; k++) ring(g, bx, byy - bob, 16.5f + k, GOLD, 0, 1f); float sp2 = (now / 900f) % 1f; double an = sp2 * Math.PI * 2; int sx = bx + (int) Math.round(Math.cos(an) * 17), sy = byy - bob + (int) Math.round(Math.sin(an) * 17); g.fill(sx - 1, sy, sx + 2, sy + 1, 0xFFFFFFFF); g.fill(sx, sy - 1, sx + 1, sy + 2, 0xFFFFFFFF); }
        } else {
            disc(g, bx, byy, 15, 0xFFD8CCB0); ring(g, bx, byy, 14, INK_FAINT, 2, 1f);
        }
    }
    private static void lockGlyph(GuiGraphicsExtractor g, int bx, int byy) {
        g.fill(bx - 4, byy - 1, bx + 4, byy + 5, INK_SOFT); g.fill(bx - 3, byy - 5, bx - 2, byy - 1, INK_SOFT); g.fill(bx + 2, byy - 5, bx + 3, byy - 1, INK_SOFT); g.fill(bx - 2, byy - 6, bx + 2, byy - 5, INK_SOFT); g.fill(bx, byy + 1, bx + 1, byy + 3, PAPER);
    }
    private static void defTile(GuiGraphicsExtractor g, CosmeticData.Def d, int bx0, int byy, int cw, boolean hv, boolean live, int chap) {
        boolean owned = CosmeticData.unlocked(d), worn = owned && CosmeticData.isEquipped(d); int bx = bx0 + Math.round(shake(d.id()));
        CosmeticData.SetDef sd = d.set() == null ? null : CosmeticData.setById(d.set());
        int col = sd != null ? sd.color() : d.color();
        medallion(g, bx, byy, col, owned, worn, hv);
        int bob = hv ? 1 : 0;
        if (owned || hv) drawCos(g, d, bx, byy - bob, owned, hv || worn);
        else lockGlyph(g, bx, byy);
        String nm = !owned && d.id().equals("secret") ? "???" : d.name();
        List<String> lines = wrap(nm, cw - 4, 0.75f);
        for (int li = 0; li < Math.min(2, lines.size()); li++) small(g, lines.get(li), bx0 - Math.round(fw(lines.get(li)) * 0.375f), byy + 19 + li * 8, owned ? INK : INK_SOFT);
        Achievements.Ach via0 = Achievements.unlocking(d.id());
        String sub = worn ? "worn" : !owned && via0 != null ? "\u2192 " + via0.name() : "";
        if (!sub.isEmpty()) { List<String> sl = wrap(sub, cw - 2, 0.75f); small(g, sl.get(0), bx0 - Math.round(fw(sl.get(0)) * 0.375f), byy + 19 + Math.min(2, lines.size()) * 8, worn ? GOLD_D : mix(GOLD_D, INK_SOFT, 0.5f)); }
        if (!live) return;
        HITS.add(new Object[]{bx0 - cw / 2 + 2, byy - 17, bx0 + cw / 2 - 2, byy + 32, (Runnable) () -> clickDef(d)});
        if (hv) {
            tryOn(d); if (d.kind() == CosmeticData.Kind.AURA) hoverAura = d.id();
            Achievements.Ach via = Achievements.unlocking(d.id());
            String how = owned ? d.how() : via != null ? "Earn \"" + via.name() + "\" in the ledger: " + via.desc() + (via.goal() > 1 ? "  (" + Ledger.fmt(via.progress()) + " / " + Ledger.fmt(via.goal()) + ")" : "") : CosmeticData.lockText(d);
            host.tip(nm + "\n" + CosmeticData.kindLabel(d.kind()) + "\n" + CosmeticData.rarityOf(d) + "\n" + (owned ? "1" : "0") + "\n" + (sd != null ? sd.name() + " set" : worn ? "Worn now. Click to take it off." : owned ? "Click to wear it." : "") + "\n" + how, col);
        }
    }
    private static void setTile(GuiGraphicsExtractor g, CosmeticData.SetDef sd, int bx0, int byy, int cw, boolean hv, boolean live) {
        List<CosmeticData.Def> ms = CosmeticData.members(sd); boolean owned = !ms.isEmpty() && CosmeticData.unlocked(ms.get(0)), worn = owned && CosmeticData.wearingSet(sd);
        int bx = bx0 + Math.round(shake("set:" + sd.id()));
        medallion(g, bx, byy, sd.color(), owned, worn, hv);
        int bob = hv ? 1 : 0;
        if (owned || hv) Icons.draw(g, (sd.suffix() != null ? ":emblem_special:#" : ":emblem:#") + String.format("%06X", sd.color() & 0xFFFFFF), bx - 8, byy - 8 - bob, owned ? now : 2400L);
        else lockGlyph(g, bx, byy);
        small(g, sd.name(), bx0 - Math.round(fw(sd.name()) * 0.375f), byy + 19, owned ? INK : INK_SOFT);
        String cnt = CosmeticData.owned(sd) + "/" + ms.size(); small(g, cnt, bx0 - Math.round(fw(cnt) * 0.375f), byy + 27, worn ? GOLD_D : INK_SOFT);
        if (!live) return;
        HITS.add(new Object[]{bx0 - cw / 2 + 2, byy - 17, bx0 + cw / 2 - 2, byy + 32, (Runnable) () -> clickSet(sd)});
        if (hv) { if (sd.aura() != null) { hoverAura = sd.aura(); nTryAura = sd.aura(); } if (sd.fx() != null) nTryFx = sd.fx(); if (sd.suffix() != null) nTryIcon = sd.suffix(); host.tip(sd.name() + "\nSet\n" + sd.rarity() + "\n" + (owned ? "1" : "0") + "\nItems " + CosmeticData.owned(sd) + " / " + ms.size() + "\n" + sd.how(), sd.color()); }
    }
    private static void clickDef(CosmeticData.Def d) {
        if (!CosmeticData.unlocked(d)) { SHAKE.put(d.id(), now); sound("book_locked", 1f, 0.8f); Achievements.Ach via = Achievements.unlocking(d.id()); host.toast("LOCKED!\n" + (via != null ? "Earn \"" + via.name() + "\" to unlock it." : CosmeticData.lockText(d)), 0xFFFF5A5A); return; }
        boolean was = CosmeticData.isEquipped(d); CosmeticData.toggle(d); sound(was ? "book_unequip" : "book_equip", 1f, 0.8f);
    }
    private static void clickSet(CosmeticData.SetDef sd) {
        List<CosmeticData.Def> ms = CosmeticData.members(sd);
        if (ms.isEmpty() || !CosmeticData.unlocked(ms.get(0))) { SHAKE.put("set:" + sd.id(), now); sound("book_locked", 1f, 0.8f); host.toast("LOCKED!\n" + sd.how(), 0xFFFF5A5A); return; }
        boolean was = CosmeticData.wearingSet(sd); CosmeticData.toggleSet(sd); sound(was ? "book_unequip" : "book_equip", 1f, 0.8f);
    }

    // ------------------------------------------------------------------ input
    /** A click while the book is open. Always true: nothing behind the book takes clicks. */
    static boolean click(double x, double y) {
        if (!open) return false;
        if (closeAt != 0) return true;
        for (int i = HITS.size() - 1; i >= 0; i--) { Object[] h = HITS.get(i); if (x >= (int) h[0] && x < (int) h[2] && y >= (int) h[1] && y < (int) h[3]) { ((Runnable) h[4]).run(); return true; } }
        boolean onBook = x >= spine - pw - 32 && x < spine + pw + 32 && y >= top - 16 && y < top + bh + 16;
        if (!onBook) close();
        return true;
    }
    static boolean scroll(double dy) { if (!open) return false; if (dy != 0) turn(dy < 0 ? 1 : -1); return true; }
    /** Keys while the book is open: Esc closes it, the arrows turn the pages. */
    static boolean key(int key) {
        if (!open) return false;
        if (key == 256) close();
        else if (key == 262 || key == 32) turn(1);
        else if (key == 263) turn(-1);
        return true;
    }
}
