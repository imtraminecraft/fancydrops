package dev.fancydrops;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;

import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public class FancyDropsMod implements ClientModInitializer, ModInitializer {
    private static int tabTick;
    /** The one version number: gradle.properties -> fabric.mod.json -> here. */
    public static final String VERSION = net.fabricmc.loader.api.FabricLoader.getInstance().getModContainer("fancydrops")
            .map(m -> m.getMetadata().getVersion().getFriendlyString()).orElse("dev");
    /** "Dev" or "Main": which build this jar is (set by the build: GitHub Actions on the main branch = Main, everything else = Dev). */
    public static final String CHANNEL = net.fabricmc.loader.api.FabricLoader.getInstance().getModContainer("fancydrops")
            .map(m -> { try { return m.getMetadata().getCustomValue("fancydrops:channel").getAsString(); } catch (Throwable t) { return "dev"; } })
            .map(v -> v.equalsIgnoreCase("main") ? "Main" : "Dev").orElse("Dev");
    private static volatile boolean openMenu;
    public static volatile boolean reopenMenu;   // set by the Preview buttons: menu closes, reopens when the scene ends

    @Override
    public void onInitialize() {
        // Common entrypoint runs while registries are still open; register our custom sound events here.
        SoundFx.register();
    }

    @Override
    public void onInitializeClient() {
        FancyConfig.load();
        Cosmetics.checkIcons();
        DataFiles.reloadAll();   // slayer drop tables

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> registerCommands(dispatcher));

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (openMenu) { openMenu = false; mc.setScreenAndShow(new FancyScreen()); }   // 26.2: setScreenAndShow
            if (reopenMenu && !AnimationManager.isPlaying()) { reopenMenu = false; mc.setScreenAndShow(new FancyScreen()); }
            DropListener.tick(mc);
            tabTick++;                                                                        // who wears what changes rarely: look every quarter second, every tick only while a name is moving
            if (Cosmetics.tabMoving || tabTick % 5 == 0) Cosmetics.tickTab(mc);
            if (tabTick % 5 == 0) Cosmetics.tickAura(mc);
            Cosmetics.tickChat(mc);
        });

        ClientReceiveMessageEvents.MODIFY_GAME.register((message, overlay) -> overlay ? message : Cosmetics.apply(message));

        Identifier early = Identifier.fromNamespaceAndPath("fancydrops", "early");            // our listener goes first: another mod that hides a chat line (SBO's Diana Message Hider) can no longer stop us from seeing it
        ClientReceiveMessageEvents.ALLOW_GAME.addPhaseOrdering(early, Event.DEFAULT_PHASE);
        ClientReceiveMessageEvents.ALLOW_GAME.register(early, (message, overlay) -> {
            if (overlay) return true;
            return !DropListener.onChat(message);   // the RARE DROP line is replaced by the animation
        });

        HudElementRegistry.attachElementAfter(VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath("fancydrops", "drop_animation"),
                (graphics, deltaTracker) -> AnimationManager.render(graphics));

        HudElementRegistry.attachElementAfter(VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath("fancydrops", "auras"),
                (graphics, deltaTracker) -> AuraFx.render(graphics));

        try {                                                                                  // moving name effects: refresh the tab list every frame, just before it is drawn
            HudElementRegistry.attachElementBefore(Identifier.withDefaultNamespace("player_list"), Identifier.fromNamespaceAndPath("fancydrops", "tab_fx"),
                    (graphics, deltaTracker) -> Cosmetics.tickTabFrame(Minecraft.getInstance()));
        } catch (Throwable t) { System.err.println("[FancyDrops] smooth tab effects unavailable: " + t); }

        // While a scene plays, the bottom of the screen slides away: the hearts and bars with the hotbar. When it ends the hearts slide back first, then the hotbar.
        for (String el : new String[]{"hotbar", "health_bar", "armor_bar", "food_bar", "air_bar", "mount_health", "info_bar", "experience_level", "selected_item_name", "overlay_message", "jump_meter"}) {
            final boolean hotbar = el.equals("hotbar");
            final float mult = el.equals("overlay_message") || el.equals("selected_item_name") ? 1.7f : 1f;       // the words above the hearts have further to go
            try {
                HudElementRegistry.replaceElement(Identifier.withDefaultNamespace(el), original -> (graphics, deltaTracker) -> {
                    float off = slideOffset(hotbar) * mult;
                    if (off >= 63f * mult) return;                                                    // fully away: nothing to draw
                    if (off > 0.05f) { Gfx.push(graphics); Gfx.translate(graphics, 0, off); }
                    callHud(original, graphics, deltaTracker);
                    if (off > 0.05f) Gfx.pop(graphics);
                });
            } catch (Throwable t) { System.err.println("[FancyDrops] could not wrap " + el + ": " + t); }
        }

        // Warm the price cache when joining Hypixel so the first drop already has a price.
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            var server = Minecraft.getInstance().getCurrentServer();
            if (server != null && server.ip.toLowerCase().contains("hypixel")) PriceService.warm();
        });
    }

    private static java.lang.reflect.Method hudCall;
    /** Runs a vanilla HUD element through its one method. The method is found on the HudElement interface by shape, so its exact name does not matter. */
    private static void callHud(HudElement element, Object graphics, Object deltaTracker) {
        try {
            if (hudCall == null) for (java.lang.reflect.Method m : HudElement.class.getMethods())
                if (java.lang.reflect.Modifier.isAbstract(m.getModifiers()) && m.getParameterCount() == 2) { hudCall = m; break; }
            if (hudCall != null) hudCall.invoke(element, graphics, deltaTracker);
        } catch (Throwable ignored) { }
    }

    private static float slideStatus, slideHotbar, velHotbar, velStatus;        // 0 = in place, 1 = slid away
    private static long slideAt = 0, slideFlip = 0; private static boolean slideAway;
    /** How far (in GUI pixels) the hotbar, or the hearts and everything else, are pushed down this frame. Both chase their target with a critically damped spring (no hard start or stop at any frame rate); the hotbar always moves first, the rest follows a beat later. */
    private static float slideOffset(boolean hotbar) {
        long now = System.nanoTime();
        if (now != slideAt) {
            float dt = slideAt == 0 ? 0f : Math.min(0.05f, (now - slideAt) / 1_000_000_000f); slideAt = now;
            boolean away = AnimationManager.hidesHud();
            if (away != slideAway) { slideAway = away; slideFlip = now; }
            float tgt = away ? 1f : 0f, beat = (now - slideFlip) / 1_000_000_000f;
            float[] h = damp(slideHotbar, velHotbar, tgt, 0.30f, dt);
            slideHotbar = h[0]; velHotbar = h[1];
            float[] r = damp(slideStatus, velStatus, beat > 0.16f ? tgt : slideStatus, 0.30f, dt);          // the rest waits a beat after the hotbar sets off
            slideStatus = r[0]; velStatus = r[1];
            if (!away && slideHotbar < 0.001f) { slideHotbar = 0f; velHotbar = 0f; }
            if (!away && slideStatus < 0.001f) { slideStatus = 0f; velStatus = 0f; }
            if (away && slideHotbar > 0.999f) slideHotbar = 1f;
            if (away && slideStatus > 0.999f) slideStatus = 1f;
        }
        return (hotbar ? slideHotbar : slideStatus) * 64f;
    }

    private static float[] damp(float cur, float vel, float target, float smooth, float dt) {
        if (dt <= 0f) return new float[]{cur, vel};
        float om = 2f / smooth, x = om * dt, e = 1f / (1f + x + 0.48f * x * x + 0.235f * x * x * x);
        float change = cur - target, temp = (vel + om * change) * dt;
        vel = (vel - om * temp) * e;
        return new float[]{target + (change + temp) * e, vel};
    }

    // ------------------------------------------------------------------ commands


    private static void registerCommands(com.mojang.brigadier.CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(buildRoot("fancydrops"));
        dispatcher.register(buildRoot("fd"));      // short alias, identical to /fancydrops
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> buildRoot(String name) {
        return ClientCommands.literal(name)
            .executes(ctx -> { openMenu = true; return Command.SINGLE_SUCCESS; })          // opened next tick so the chat screen doesn't close it again
            .then(ClientCommands.literal("help").executes(ctx -> help(ctx.getSource())))
            .then(ClientCommands.literal("mute").executes(ctx -> {                        // every sound on or off
                FancyConfig c = FancyConfig.get(); c.sound = !c.sound; FancyConfig.save();
                say(ctx.getSource(), c.sound ? "Sounds on (" + c.volume + "%)" : "Sounds muted");
                return Command.SINGLE_SUCCESS;
            }))
            .then(ClientCommands.literal("toggle").executes(ctx -> {                      // the master switch
                FancyConfig c = FancyConfig.get(); boolean on = !(c.masterAnimate || c.masterAnnounce); c.setMaster(on, on); FancyConfig.save();
                say(ctx.getSource(), "FancyDrops " + (on ? "enabled" : "disabled") + " (animations and chat announcements)");
                return Command.SINGLE_SUCCESS;
            }))
            .then(ClientCommands.literal("simulate")
                .executes(ctx -> { say(ctx.getSource(), "Use " + SIM_USAGE); return Command.SINGLE_SUCCESS; })
                .then(ClientCommands.literal("Mythological")
                    .executes(ctx -> { say(ctx.getSource(), "Use " + SIM_USAGE); return Command.SINGLE_SUCCESS; })
                    .then(ClientCommands.argument("args", StringArgumentType.greedyString())
                        .suggests((ctx, b) -> suggestSim(b, false))
                        .executes(ctx -> simulate(ctx.getSource(), StringArgumentType.getString(ctx, "args"), false))))
                .then(ClientCommands.literal("Slayer")
                    .executes(ctx -> { say(ctx.getSource(), "Use " + SIM_SLAYER); return Command.SINGLE_SUCCESS; })
                    .then(ClientCommands.argument("args", StringArgumentType.greedyString())
                        .suggests((ctx, b) -> suggestSim(b, true))
                        .executes(ctx -> simulate(ctx.getSource(), StringArgumentType.getString(ctx, "args"), true)))));
    }

    static final String DISCORD = "https://discord.gg/bum8VPbRPR";
    static final String GITHUB = "https://github.com/imtraminecraft/fancydrops";

    private static void say(FabricClientCommandSource source, String msg) { source.sendFeedback(Chat.msg(msg)); }

    private static CompletableFuture<Suggestions> suggest(SuggestionsBuilder b, String... options) {
        for (String o : options) if (o.toLowerCase(Locale.ROOT).startsWith(b.getRemaining().toLowerCase(Locale.ROOT))) b.suggest(o);
        return b.buildFuture();
    }
    private static CompletableFuture<Suggestions> suggest(SuggestionsBuilder b, Iterable<String> options) {
        for (String o : options) if (o.toLowerCase(Locale.ROOT).startsWith(b.getRemaining().toLowerCase(Locale.ROOT))) b.suggest(o);
        return b.buildFuture();
    }

    private static int help(FabricClientCommandSource s) {
        s.sendFeedback(waveTitle("FancyDrops").copy().append(Chat.color("  \u00B7  Quick guide", Chat.DIM)));
        helpLine(s, "/fd", "Open the menu");
        helpLine(s, "/fd mute", "Turn every sound on or off");
        helpLine(s, "/fd toggle", "Turn the whole mod on or off");
        helpLine(s, "/fd simulate Mythological <Mob|Treasure> <Drop> <mf>", "Play a pretend drop through the real animation");
        helpLine(s, "/fd simulate Slayer <Boss> <Drop> <mf>", "The same for a slayer boss (Nothing for no drop)");
        s.sendFeedback(Component.literal(" "));
        s.sendFeedback(Chat.color("Example  ", Chat.DIM).append(Chat.color("/fd simulate Mythological Minos Hunter Daedalus Stick 250", Chat.COMMAND)));
        s.sendFeedback(Chat.color("Tab completes names. Use Nothing as the drop for a ritual that gave nothing. The magic find goes last and defaults to 250.", Chat.DIM));
        return Command.SINGLE_SUCCESS;
    }

    private static void helpLine(FabricClientCommandSource s, String command, String description) {
        s.sendFeedback(Chat.color("   " + command, Chat.COMMAND));
        s.sendFeedback(Chat.color("      " + description, Chat.TEXT));
    }

    /** Same cyan-to-violet-to-pink wave palette as the animated /fancydrops menu title. */
    private static Component waveTitle(String text) {
        MutableComponent out = Component.empty();
        for (int i = 0; i < text.length(); i++) {
            float u = i / (float) Math.max(1, text.length() - 1) * (Chat.GRADIENT.length - 1);
            int a = Chat.GRADIENT[(int) u], b = Chat.GRADIENT[Math.min(Chat.GRADIENT.length - 1, (int) u + 1)], color = Chat.mix(a, b, u - (int) u);
            out.append(Component.literal(String.valueOf(text.charAt(i))).withStyle(st -> st.withColor(color).withBold(true)));
        }
        return out;
    }

    private static final String SIM_USAGE = "/fd simulate Mythological <Mob|Treasure> <Drop> <mf>";
    private static final String SIM_SLAYER = "/fd simulate Slayer <Boss> <Drop> <mf>";
    private static final String[] MF_TIPS = {"0", "100", "250", "400"};

    /** The text after a leading source name ("Minos Hunter Hilt of ..." -> "Hilt of ..."), or null when the text doesn't start with one. */
    private static String afterName(String t, String name) {
        int n = name.length();
        return t.regionMatches(true, 0, name, 0, n) && (t.length() == n || Character.isWhitespace(t.charAt(n))) ? t.substring(n).trim() : null;
    }

    /** /fd simulate Mythological <Mob|Treasure> <Drop> <mf>: the mob (or Treasure), one drop (Nothing for none), then the magic find, 250 by default. */
    private static int simulate(FabricClientCommandSource source, String args, boolean slayer) {
        String t = args.trim(); int mf = 250;
        String[] w = t.split("\\s+");
        if (w.length > 1 && w[w.length - 1].matches("\\d{1,5}")) { mf = Math.min(10000, Integer.parseInt(w[w.length - 1])); t = t.substring(0, t.length() - w[w.length - 1].length()).trim(); }
        if (slayer) {
            DropData.Source bs = null;
            for (DropData.Source sl : SlayerData.ALL) if (afterName(t, sl.boss) != null && (bs == null || sl.boss.length() > bs.boss.length())) bs = sl;
            if (bs == null) { say(source, "Pick a boss: " + String.join(", ", SlayerData.ALL.stream().map(sl -> sl.boss).toList())); return 0; }
            DropListener.simulateSlayer(mf, t, m -> source.sendFeedback(Chat.msg(m)));
            return Command.SINGLE_SUCCESS;
        }
        String rest = afterName(t, "Treasure");
        if (rest != null) { DropListener.simulateTreasure(rest, mf, m -> source.sendFeedback(Chat.msg(m))); return Command.SINGLE_SUCCESS; }
        DropData.Source best = null;
        for (DropData.Source sl : MythData.ALL) if (afterName(t, sl.boss) != null && (best == null || sl.boss.length() > best.boss.length())) best = sl;
        if (best == null) { say(source, "Pick a mob or Treasure: " + String.join(", ", MythData.ALL.stream().map(sl -> sl.boss).toList()) + ", Treasure"); return 0; }
        DropListener.simulateMyth(mf, t, m -> source.sendFeedback(Chat.msg(m)));
        return Command.SINGLE_SUCCESS;
    }

    /** Tab completion: the mob (or Treasure), then its drops (and Nothing), then the magic find. */
    private static CompletableFuture<Suggestions> suggestSim(SuggestionsBuilder builder, boolean slayer) {
        java.util.List<DropData.Source> lib = slayer ? SlayerData.ALL : MythData.ALL;
        String rem = builder.getRemaining(); int lead = rem.length() - rem.stripLeading().length(); String t = rem.stripLeading();
        java.util.List<String> names = new java.util.ArrayList<>(); for (DropData.Source sl : lib) names.add(sl.boss); if (!slayer) names.add("Treasure");
        String owner = null; for (String nm : names) { String r = afterName(t, nm); if (r != null && t.length() > nm.length() && (owner == null || nm.length() > owner.length())) owner = nm; }
        if (owner == null) {
            SuggestionsBuilder b0 = builder.createOffset(builder.getStart() + lead);
            for (String nm : names) if (nm.toLowerCase(Locale.ROOT).startsWith(t.toLowerCase(Locale.ROOT))) b0.suggest(nm);
            return b0.buildFuture();
        }
        java.util.List<String> pool = new java.util.ArrayList<>();
        if (owner.equals("Treasure")) { java.util.Set<String> seen = new java.util.LinkedHashSet<>(); for (int sp = 0; sp < 3; sp++) for (DropData.Drop d : TreasureData.drops(sp)) seen.add(d.name); pool.addAll(seen); }
        else { for (DropData.Source sl : lib) if (sl.boss.equals(owner)) for (DropData.Drop d : sl.drops) pool.add(d.name); pool.add("Nothing"); }
        String after = t.substring(owner.length()).stripLeading();
        int offset = rem.length() - after.length();
        String dropDone = null; for (String nm : pool) if (after.length() > nm.length() && after.regionMatches(true, 0, nm, 0, nm.length()) && Character.isWhitespace(after.charAt(nm.length())) && (dropDone == null || nm.length() > dropDone.length())) dropDone = nm;
        if (dropDone != null) {
            String typed = after.substring(dropDone.length()).stripLeading();
            SuggestionsBuilder b2 = builder.createOffset(builder.getStart() + rem.length() - typed.length());
            for (String m : MF_TIPS) if (m.startsWith(typed)) b2.suggest(m);
            return b2.buildFuture();
        }
        SuggestionsBuilder b1 = builder.createOffset(builder.getStart() + offset);
        for (String nm : pool) if (nm.toLowerCase(Locale.ROOT).contains(after.toLowerCase(Locale.ROOT))) b1.suggest(nm);
        return b1.buildFuture();
    }
}
