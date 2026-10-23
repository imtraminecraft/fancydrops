package dev.fancydrops;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Ties "Your Slayer Kill gave you ..." to the RARE DROP lines around it, whichever order Hypixel prints them in.
 * Pure logic with an injected clock, so it is unit-tested without Minecraft (src/test).
 *
 *  - kill, then drops     -> one scene starts JOIN_MS after the kill; drops that arrive until it reveals (WINDOW_MS) join it
 *  - drops, then kill     -> the early drops wait in 'pending' and join that kill's scene
 *  - drops, no kill line  -> after PENDING_MS they get a scene of their own (so the mod still works if the kill text changes)
 */
public final class KillTracker<T> {
    public static final long JOIN_MS = 150, WINDOW_MS = 1400, PENDING_MS = 1500, DEDUPE_MS = 2500, EARLY_MS = 3000, NOTHING_MS = 1600;      // NOTHING_MS: a kill with no drop yet waits this long for one before it counts as "nothing"

    public static final class Session<T> {
        public final List<T> drops = new ArrayList<>();
        public final long closeAt;
        public final boolean fromKill;
        private boolean closed;
        Session(long closeAt, boolean fromKill) { this.closeAt = closeAt; this.fromKill = fromKill; }
        /** Called by the animation when it reveals: later drops start a new scene. */
        public void close() { closed = true; }
        public boolean isOpen() { return !closed; }
    }

    private final Consumer<Session<T>> sink;
    private final List<T> pending = new ArrayList<>();
    private final List<Long> pendingAt = new ArrayList<>();
    private Session<T> live;
    private long killAt = -1, lastKill = Long.MIN_VALUE / 2;
    public int kills;

    public KillTracker(Consumer<Session<T>> sink) { this.sink = sink; }

    public void kill(long now) {
        if (now - lastKill < DEDUPE_MS) return;          // "SLAYER QUEST COMPLETE" and "Your Slayer Kill..." can both appear
        lastKill = now; killAt = now; kills++;
    }

    public void drop(T d, long now) {
        if (killAt < 0 && live != null && live.isOpen() && now <= live.closeAt) { live.drops.add(d); return; }      // a scene that has not revealed yet takes it, but only inside its window
        pending.add(d); pendingAt.add(now);
    }

    public void tick(long now) {
        boolean early = false;
        for (long at : pendingAt) if (killAt - at <= EARLY_MS) early = true;
        if (killAt >= 0 && now - killAt >= JOIN_MS && (early || now - killAt >= NOTHING_MS)) {      // drops can print a little after the kill line: give them a moment before calling it "nothing"
            Session<T> s = new Session<>(Math.max(killAt + WINDOW_MS, now + 800), true);
            for (int i = 0; i < pending.size(); i++) if (killAt - pendingAt.get(i) <= EARLY_MS) s.drops.add(pending.get(i));      // (a drop that came after the kill has a negative gap, so it joins too)
            pending.clear(); pendingAt.clear();
            killAt = -1; live = s; sink.accept(s);
        } else if (killAt < 0 && !pending.isEmpty() && now - pendingAt.get(0) >= PENDING_MS) {
            Session<T> s = new Session<>(now, false);
            s.drops.addAll(pending);
            pending.clear(); pendingAt.clear();
            live = s; sink.accept(s);
        }
    }
}
