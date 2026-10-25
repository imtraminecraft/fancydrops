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
        ProfitTracker.load();    // overall profit, kept between sessions

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
            .executes(ctx -> {
                openMenu = true;          // opened next tick so the chat screen doesn't close it again
                return Command.SINGLE_SUCCESS;
            })
            .then(ClientCommands.literal("help").executes(ctx -> help(ctx.getSource())))
            .then(ClientCommands.literal("stats").executes(ctx -> stats(ctx.getSource())))
            .then(ClientCommands.literal("editor")
                .executes(ctx -> { FancyScreen.pendingTab = "animations"; openMenu = true; return Command.SINGLE_SUCCESS; })
                .then(ClientCommands.literal("animations").executes(ctx -> { FancyScreen.pendingTab = "animations"; openMenu = true; return Command.SINGLE_SUCCESS; }))
                .then(ClientCommands.literal("sounds").executes(ctx -> { FancyScreen.pendingTab = "sounds"; openMenu = true; return Command.SINGLE_SUCCESS; })))
            .then(ClientCommands.literal("cosmetics").executes(ctx -> { FancyScreen.pendingCos = true; openMenu = true; return Command.SINGLE_SUCCESS; }))
            .then(ClientCommands.literal("discord").executes(ctx -> { ctx.getSource().sendFeedback(Chat.msg("Join the Discord: ").copy().append(net.minecraft.network.chat.Component.literal(DISCORD).withStyle(st -> st.withColor(0x7A8BFF).withUnderlined(true).withClickEvent(new net.minecraft.network.chat.ClickEvent.OpenUrl(java.net.URI.create(DISCORD)))))); return Command.SINGLE_SUCCESS; }))
            .then(ClientCommands.literal("patreon").executes(ctx -> { say(ctx.getSource(), "Patreon link coming soon"); return Command.SINGLE_SUCCESS; }))
            .then(ClientCommands.literal("github").executes(ctx -> { ctx.getSource().sendFeedback(Chat.msg("Source and releases: ").copy().append(net.minecraft.network.chat.Component.literal(GITHUB).withStyle(st -> st.withColor(0x7A8BFF).withUnderlined(true).withClickEvent(new net.minecraft.network.chat.ClickEvent.OpenUrl(java.net.URI.create(GITHUB)))))); return Command.SINGLE_SUCCESS; }))
            // /fancydrops mute  - quick on/off for every sound
            .then(ClientCommands.literal("mute").executes(ctx -> {
                FancyConfig c = FancyConfig.get(); c.sound = !c.sound; FancyConfig.save();
                say(ctx.getSource(), c.sound ? "Sounds on (" + c.volume + "%)" : "Sounds muted");
                return Command.SINGLE_SUCCESS;
            }))
            // /fancydrops toggle  - master switch
            .then(ClientCommands.literal("toggle").executes(ctx -> {
                FancyConfig c = FancyConfig.get(); boolean on = !(c.masterAnimate || c.masterAnnounce); c.setMaster(on, on); FancyConfig.save();
                say(ctx.getSource(), "FancyDrops " + (on ? "enabled" : "disabled") + " (animations and chat announcements)");
                return Command.SINGLE_SUCCESS;
            }))
            // /fd simulate <Slayer | Mythological | Dye | Farming | Experiments> ...  - pretends you got a drop
            .then(simNode("simulate"))
            .then(ClientCommands.literal("cosmetics").executes(ctx -> { FancyScreen.pendingCos = true; openMenu = true; return Command.SINGLE_SUCCESS; }))
            // /fd <anything else>  - opens the menu and searches for it
            .then(ClientCommands.argument("search", StringArgumentType.greedyString()).executes(ctx -> {
                FancyScreen.pendingSearch = StringArgumentType.getString(ctx, "search").trim(); openMenu = true;
                return Command.SINGLE_SUCCESS;
            }))
            // /fd magicfind <magic find> [drop name | chance]  - what your magic find does to a drop chance
            .then(ClientCommands.literal("magicfind")
                .executes(ctx -> { say(ctx.getSource(), "Use /fd magicfind <magic find> [drop name | 1-in-N chance]   e.g. /fd magicfind 250 Warden Heart"); return Command.SINGLE_SUCCESS; })
                .then(ClientCommands.argument("magicfind", DoubleArgumentType.doubleArg(0, 100000))
                    .suggests((ctx, builder) -> suggestMagicFind(builder))
                    .executes(ctx -> mfCalc(ctx.getSource(), DoubleArgumentType.getDouble(ctx, "magicfind"), ""))
                    .then(ClientCommands.argument("drop", StringArgumentType.greedyString())
                        .suggests((ctx, builder) -> suggestDropNames(builder))
                        .executes(ctx -> mfCalc(ctx.getSource(), DoubleArgumentType.getDouble(ctx, "magicfind"), StringArgumentType.getString(ctx, "drop"))))));
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
        s.sendFeedback(waveTitle("FancyDrops").copy().append(Chat.color("  ·  Quick guide", Chat.DIM)));
        helpHead(s, "Menu");
        helpLine(s, "/fd", "Open the menu");
        helpLine(s, "/fd stats", "Open the menu on the Stats page");
        helpLine(s, "/fd cosmetics", "Open the cosmetics menu");
        helpLine(s, "/fd <search>", "Open the menu and search for what you typed");
        helpHead(s, "Try a drop  (never counts toward profit)");
        helpLine(s, "/fd simulate Mythological <Mob> <Drop 1|Nothing> [Drop 2] [mf]", "A Mythological Ritual creature kill");
        helpLine(s, "/fd simulate Treasure <Drop>", "One Griffin burrow treasure (blank = a random one)");
        helpHead(s, "Handy");
        helpLine(s, "/fd editor [animations|sounds]", "Open the Animations or Sounds editor");
        helpLine(s, "/fd magicfind <magic find> [drop]", "What your Magic Find does to a drop chance");
        helpLine(s, "/fd mute", "Turn sounds on or off");
        helpLine(s, "/fd toggle", "Turn the whole mod on or off");
        s.sendFeedback(Component.literal(" "));
        s.sendFeedback(Chat.color("Example  ", Chat.DIM).append(Chat.color("/fd simulate Mythological Minos Hunter Daedalus Stick", Chat.COMMAND)));
        s.sendFeedback(Chat.color("Tab completes names. The magic find goes last and defaults to 250.", Chat.DIM));
        return Command.SINGLE_SUCCESS;
    }

    private static void helpHead(FabricClientCommandSource s, String text) {
        s.sendFeedback(Component.literal(" "));
        s.sendFeedback(Chat.color("\u25B8 " + text, Chat.NUMBER).copy().withStyle(net.minecraft.ChatFormatting.BOLD));
    }

    private static void helpLine(FabricClientCommandSource s, String command, String description) {
        s.sendFeedback(Chat.color("   " + command, Chat.COMMAND));
        s.sendFeedback(Chat.color("      " + description, Chat.TEXT));
    }

    private static int status(FabricClientCommandSource s) {
        FancyConfig c = FancyConfig.get();
        say(s, "Animations " + (c.masterAnimate ? "on" : "OFF") + ", announcements " + (c.masterAnnounce ? "on" : "OFF") + " | v" + VERSION + " (" + CHANNEL + " build)" + " | 3D " + (c.show3d ? "on" : "off")
                + " | RNG Drop " + (c.epicEffects ? "from " + PriceService.fmt(c.epicThreshold) : "off"));
        say(s, "Sound " + (c.sound ? c.volume + "%" : "muted") + " (" + (c.customSounds ? "custom" : "vanilla") + ") | size " + Math.round(c.size * 100) + "% text " + Math.round(c.textScale * 100) + "% | slices " + c.chopsDefault + "/" + c.chopsCool + "/" + c.chopsEpic);
        say(s, "Chat channel " + c.channel + " | prices: " + PriceService.status);
        return Command.SINGLE_SUCCESS;
    }

    /** /fd stats: opens the menu on the Stats page. */
    private static int stats(FabricClientCommandSource s) {
        FancyScreen.pendingStats = true; openMenu = true;
        return Command.SINGLE_SUCCESS;
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

    /** Magic find multiplies the drop chance by (1 + MF/100): at +250% MF a 1 in 735 drop becomes 1 in 210. */
    private static int mfCalc(FabricClientCommandSource s, double mf, String what) {
        double mult = 1 + mf / 100.0;
        String m = "+" + trimNum(mf) + "% MF";
        what = what.trim();
        if (what.isEmpty()) { say(s, m + " multiplies every affected drop chance by " + trimNum(mult) + "x  (1 in N becomes 1 in N / " + trimNum(mult) + ")"); return Command.SINGLE_SUCCESS; }
        String num = what.toLowerCase(Locale.ROOT).replace("1 in", "").replace(",", "").replace("1/", "").trim();
        if (num.matches("[0-9]+(\\.[0-9]+)?")) {
            double n = Double.parseDouble(num);
            say(s, "1 in " + PriceService.fmt(n) + " with " + m + " becomes 1 in " + oneInText(n / mult) + "  (" + pct(100 * mult / n) + ")");
            return Command.SINGLE_SUCCESS;
        }
        int shown = 0;
        for (SlayerData.Slayer sl : SlayerData.ALL) for (SlayerData.Drop d : sl.drops) {
            if (!d.name.toLowerCase(Locale.ROOT).contains(what.toLowerCase(Locale.ROOT))) continue;
            java.util.regex.Matcher mm = java.util.regex.Pattern.compile("1 in ([0-9][0-9,.]*)").matcher(d.chance == null ? "" : d.chance);
            if (!mm.find()) { say(s, d.name + " (" + sl.boss + "): chance unknown"); shown++; continue; }
            double n = Double.parseDouble(mm.group(1).replace(",", ""));
            if (++shown > 8) { say(s, "...narrow it down with more of the name"); return Command.SINGLE_SUCCESS; }
            say(s, d.name + " (" + sl.boss + "): " + d.chance + "  ->  1 in " + oneInText(n / mult) + " with " + m + "  (" + pct(100 * mult / n) + ")");
        }
        if (shown == 0) say(s, "No drop matches \"" + what + "\". You can also type a chance: /fd magicfind " + trimNum(mf) + " 735");
        return Command.SINGLE_SUCCESS;
    }
    private static String trimNum(double v) { return v == Math.floor(v) ? String.valueOf((long) v) : String.format(Locale.ROOT, "%.2f", v); }
    private static String oneInText(double v) { return v >= 100 ? PriceService.fmt(Math.round(v)) : String.format(Locale.ROOT, "%.1f", v); }
    private static String pct(double p) { return (p >= 1 ? String.format(Locale.ROOT, "%.2f", p) : String.format(Locale.ROOT, "%.4f", p)) + "%"; }

    private static CompletableFuture<Suggestions> suggestDropNames(SuggestionsBuilder builder) {
        String typed = builder.getRemaining().toLowerCase(Locale.ROOT);
        for (SlayerData.Slayer sl : SlayerData.ALL) for (SlayerData.Drop d : sl.drops) if (d.name.toLowerCase(Locale.ROOT).startsWith(typed)) builder.suggest(d.name);
        return builder.buildFuture();
    }

    private static final String[] SIM_KINDS = {"Mythological", "Treasure"};           // only the finished pages are listed (Slayer, Dye, Farming and Experiments still answer, but wait for their rebuild)
    private static final String SIM_USAGE = "/fd simulate Mythological <Mob> <Drop 1|Nothing> [Drop 2] [mf]   ·   Treasure <Drop>";

    private static LiteralArgumentBuilder<FabricClientCommandSource> simNode(String name) {
        return ClientCommands.literal(name)
            .executes(ctx -> { say(ctx.getSource(), "Use " + SIM_USAGE); return Command.SINGLE_SUCCESS; })
            .then(ClientCommands.argument("kind", StringArgumentType.word())
                .suggests((ctx, b) -> suggest(b, SIM_KINDS))
                .executes(ctx -> simulate(ctx.getSource(), StringArgumentType.getString(ctx, "kind"), ""))
                .then(ClientCommands.argument("items", StringArgumentType.greedyString())
                    .suggests((ctx, b) -> suggestSim(b, StringArgumentType.getString(ctx, "kind")))
                    .executes(ctx -> simulate(ctx.getSource(), StringArgumentType.getString(ctx, "kind"), StringArgumentType.getString(ctx, "items")))));
    }

    /** The text after a leading source name ("Minos Hunter Hilt of ..." -> "Hilt of ..."), or null when the text doesn't start with one. */
    private static String afterSource(java.util.List<SlayerData.Slayer> srcs, String t) {
        SlayerData.Slayer best = null;
        for (SlayerData.Slayer sl : srcs) { int n = sl.boss.length(); if (t.regionMatches(true, 0, sl.boss, 0, n) && (t.length() == n || Character.isWhitespace(t.charAt(n))) && (best == null || n > best.boss.length())) best = sl; }
        return best == null ? null : t.substring(best.boss.length()).trim();
    }
    private static SlayerData.Slayer sourceOf(java.util.List<SlayerData.Slayer> srcs, String t) {
        SlayerData.Slayer best = null;
        for (SlayerData.Slayer sl : srcs) { int n = sl.boss.length(); if (t.regionMatches(true, 0, sl.boss, 0, n) && (t.length() == n || Character.isWhitespace(t.charAt(n))) && (best == null || n > best.boss.length())) best = sl; }
        return best;
    }

    private static int simulate(FabricClientCommandSource source, String kind, String args) {
        String t = args.trim(); String k = kind.toLowerCase(Locale.ROOT);
        Integer num = null; String[] w = t.split("\\s+");
        if (w.length > 0 && w[w.length - 1].matches("\\d{1,5}") && !k.equals("dye") && !k.equals("experiments") && !k.equals("treasure")) { num = Math.min(10000, Integer.parseInt(w[w.length - 1])); t = t.substring(0, t.length() - w[w.length - 1].length()).trim(); }
        switch (k) {
            case "slayer" -> {
                SlayerData.Slayer[] hit = new SlayerData.Slayer[1]; int n = DropListener.bossPrefix(t, hit);
                if (hit[0] == null) { say(source, "Pick a boss: " + String.join(", ", SlayerData.ALL.stream().map(sl -> sl.boss).toList()) + "  (Tab lists them)"); return 0; }
                if (t.substring(n).isBlank()) { say(source, "Use /fd simulate Slayer <Boss> <Drop 1|Nothing> [Drop 2] [mf]"); return 0; }
                DropListener.simulateKill(num == null ? 250 : num, t, m -> source.sendFeedback(Chat.msg(m)));
            }
            case "mythological" -> {
                String rest = afterSource(MythData.ALL, t);
                if (rest == null) { say(source, "Pick a mob: " + String.join(", ", MythData.ALL.stream().map(sl -> sl.boss).toList()) + "  (Tab lists them)"); return 0; }
                if (rest.isEmpty()) { say(source, "Use /fd simulate Mythological <Mob> <Drop 1|Nothing> [Drop 2] [mf]"); return 0; }
                DropListener.simulateMyth(num == null ? 250 : num, t, m -> source.sendFeedback(Chat.msg(m)));
            }
            case "treasure" -> DropListener.simulateTreasure(t, m -> source.sendFeedback(Chat.msg(m)));
            case "dye" -> {
                if (t.isEmpty()) { say(source, "Use /fd simulate Dye <Dye>"); return 0; }
                DropListener.simulateDye(250, t, m -> source.sendFeedback(Chat.msg(m)));
            }
            case "farming" -> {
                String rest = afterSource(FarmData.ALL, t);
                if (rest == null) { say(source, "Pick a crop or pest: " + String.join(", ", FarmData.ALL.stream().map(sl -> sl.boss).toList()) + "  (Tab lists them)"); return 0; }
                if (rest.isEmpty()) { say(source, "Use /fd simulate Farming <Crop|Pest> <Drop 1> [ob]"); return 0; }
                DropListener.simulateFarm(num == null ? 25 : num, t, m -> source.sendFeedback(Chat.msg(m)));
            }
            case "experiments" -> {
                if (t.isEmpty()) { say(source, "Use /fd simulate Experiments <Drop 1> [Drop 2]"); return 0; }
                DropListener.simulateExp(250, t, m -> source.sendFeedback(Chat.msg(m)));
            }
            default -> { say(source, "Use " + SIM_USAGE); return 0; }
        }
        return Command.SINGLE_SUCCESS;
    }

    private static CompletableFuture<Suggestions> suggestMagicFind(SuggestionsBuilder builder) {
        String typed = builder.getRemaining();
        for (String value : new String[]{"0", "100", "200", "250", "300", "400"})
            if (value.startsWith(typed)) builder.suggest(value);
        return builder.buildFuture();
    }

    /** Tab completion for /fd simulate <kind> ...: the source (boss, mob, crop), then one or two drops, then the optional number. */
    private static CompletableFuture<Suggestions> suggestSim(SuggestionsBuilder builder, String kind) {
        String k = kind.toLowerCase(Locale.ROOT), rem = builder.getRemaining();
        java.util.List<SlayerData.Slayer> srcs = null; java.util.List<String> pool; int max = 1; boolean nothing = false; String[] tail = {};
        switch (k) {
            case "slayer" -> { srcs = SlayerData.ALL; max = 2; nothing = true; tail = new String[]{"100", "250", "400"}; }
            case "mythological" -> { srcs = MythData.ALL; max = 2; nothing = true; tail = new String[]{"100", "250", "400"}; }
            case "farming" -> { srcs = FarmData.ALL; tail = new String[]{"25", "50", "100"}; }
            case "treasure" -> { }
            case "dye" -> { }
            case "experiments" -> max = 2;
            default -> { return builder.buildFuture(); }
        }
        int pos = 0; pool = new java.util.ArrayList<>();
        if (srcs != null) {
            SlayerData.Slayer own = sourceOf(srcs, rem.stripLeading());
            int lead = rem.length() - rem.stripLeading().length();
            boolean full = own != null && rem.stripLeading().length() > own.boss.length();            // the name and a space were typed
            if (!full) {
                String typed = rem.stripLeading().toLowerCase(Locale.ROOT);
                SuggestionsBuilder b0 = builder.createOffset(builder.getStart() + lead);
                for (SlayerData.Slayer sl : srcs) if (sl.boss.toLowerCase(Locale.ROOT).startsWith(typed)) b0.suggest(sl.boss);
                return b0.buildFuture();
            }
            pos = lead + own.boss.length();
            for (SlayerData.Drop d : own.drops) pool.add(d.name);
        } else if (k.equals("treasure")) { java.util.Set<String> seen = new java.util.LinkedHashSet<>(); for (int sp = 0; sp < 3; sp++) for (SlayerData.Drop d : TreasureData.drops(sp)) seen.add(d.name); pool.addAll(seen);
        } else if (k.equals("dye")) { for (Object[] o : DropListener.allDyes()) pool.add(((SlayerData.Drop) o[1]).name); }
        else { for (String[] e : ExperimentData.ALL) pool.add(e[0]); }
        if (nothing) pool.add("Nothing");
        java.util.List<String> chosen = new java.util.ArrayList<>();
        while (true) {                                                           // names already typed in full
            while (pos < rem.length() && Character.isWhitespace(rem.charAt(pos))) pos++;
            String rest = rem.substring(pos); int best = 0;
            for (String nm : pool) if (nm.length() > best && rest.regionMatches(true, 0, nm, 0, nm.length()) && rest.length() > nm.length() && Character.isWhitespace(rest.charAt(nm.length()))) best = nm.length();
            if (best == 0) break;
            chosen.add(rest.substring(0, best).toLowerCase(Locale.ROOT)); pos += best;
            if (chosen.size() >= max || chosen.contains("nothing")) break;
        }
        String typed = rem.substring(Math.min(pos, rem.length())).stripLeading().toLowerCase(Locale.ROOT);
        SuggestionsBuilder b = builder.createOffset(builder.getStart() + rem.length() - typed.length());
        boolean done = chosen.size() >= max || chosen.contains("nothing");
        if (!done) for (String nm : pool) if (nm.toLowerCase(Locale.ROOT).contains(typed) && !chosen.contains(nm.toLowerCase(Locale.ROOT)) && !(nm.equals("Nothing") && !chosen.isEmpty())) b.suggest(nm);
        if (!chosen.isEmpty() || srcs != null && done) for (String m : tail) if (m.startsWith(typed)) b.suggest(m);
        return b.buildFuture();
    }
}
