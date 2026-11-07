package dev.fancydrops;

import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;

/**
 * Playtime: how long you've played with FancyDrops on (counted while you're in a world and the game isn't paused), kept in your config.
 * It feeds the Playtime constellation (The Hourglass) in the achievements sky: each milestone is a star. Counted on this computer only.
 */
final class Playtime {
    private Playtime() {}

    record Goal(String id, String name, String desc, double minutes) {}
    static final Goal[] GOALS = {
        new Goal("time_1h", "First Hour", "Play for an hour with FancyDrops on.", 60),
        new Goal("time_5h", "Settling In", "Play for 5 hours with FancyDrops on.", 300),
        new Goal("time_10h", "Regular", "Play for 10 hours with FancyDrops on.", 600),
        new Goal("time_24h", "A Whole Day", "Play for 24 hours with FancyDrops on.", 1440),
        new Goal("time_50h", "Dedicated", "Play for 50 hours with FancyDrops on.", 3000),
        new Goal("time_100h", "Centurion", "Play for 100 hours with FancyDrops on.", 6000),
        new Goal("time_250h", "Lost Track of Time", "Play for 250 hours with FancyDrops on.", 15000),
        new Goal("time_500h", "Timeless", "Play for 500 hours with FancyDrops on.", 30000),
    };
    private static long last, savedAt;

    /** Every client tick. */
    static void tick(Minecraft mc) {
        long now = System.currentTimeMillis();
        long dt = last == 0 ? 0 : Math.min(1000, now - last); last = now;
        if (mc.level == null || mc.player == null || mc.isPaused()) return;
        FancyConfig c = FancyConfig.get();
        double before = c.playMinutes; c.playMinutes += dt / 60000.0;
        for (Goal g : GOALS) if (before < g.minutes() && c.playMinutes >= g.minutes()) c.playDone.put(g.id(), now);   // when each star was earned
        if (now - savedAt > 120_000) { savedAt = now; try { FancyConfig.save(); } catch (Throwable ignored) { } }
    }

    /** The milestones as achievements (chapter "time"). */
    static List<Achievements.Ach> list() {
        FancyConfig c = FancyConfig.get(); List<Achievements.Ach> l = new ArrayList<>();
        for (Goal g : GOALS) {
            boolean done = c.playMinutes >= g.minutes();
            l.add(new Achievements.Ach(g.id(), "time", "Playtime", g.name(), g.desc(), "clock", g.minutes() / 60.0, Math.min(g.minutes(), c.playMinutes) / 60.0, done, c.playDone.getOrDefault(g.id(), 0L), "", false));
        }
        return l;
    }
    static String hours() { double m = FancyConfig.get().playMinutes; return m < 60 ? Math.round(m) + " min" : String.format(java.util.Locale.ROOT, "%.1f h", m / 60); }
}
