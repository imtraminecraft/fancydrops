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
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
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
        DataFiles.reloadAll();   // slayer drop tables
        ProfitTracker.load();    // overall profit, kept between sessions

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> registerCommands(dispatcher));

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (openMenu) { openMenu = false; mc.setScreenAndShow(new FancyScreen()); }   // 26.2: setScreenAndShow
            if (reopenMenu && !AnimationManager.isPlaying()) { reopenMenu = false; mc.setScreenAndShow(new FancyScreen()); }
            DropListener.tick(mc);
            if (++tabTick >= 20) { tabTick = 0; Cosmetics.tickTab(mc); }
        });

        ClientReceiveMessageEvents.MODIFY_GAME.register((message, overlay) -> overlay ? message : Cosmetics.apply(message));

        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> {
            if (overlay) return true;
            return !DropListener.onChat(message);   // the RARE DROP line is replaced by the animation
        });

        HudElementRegistry.attachElementAfter(VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath("fancydrops", "drop_animation"),
                (graphics, deltaTracker) -> AnimationManager.render(graphics));

        // Warm the price cache when joining Hypixel so the first drop already has a price.
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            var server = Minecraft.getInstance().getCurrentServer();
            if (server != null && server.ip.toLowerCase().contains("hypixel")) PriceService.warm();
        });
    }

    // ------------------------------------------------------------------ commands

    private static final String[] ON_OFF = {"on", "off"};

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
            .then(ClientCommands.literal("discord").executes(ctx -> { ctx.getSource().sendFeedback(Chat.msg("Join the Discord: ").copy().append(net.minecraft.network.chat.Component.literal(DISCORD).withStyle(st -> st.withColor(0x7A8BFF).withUnderlined(true).withClickEvent(new net.minecraft.network.chat.ClickEvent.OpenUrl(java.net.URI.create(DISCORD)))))); return Command.SINGLE_SUCCESS; }))
            .then(ClientCommands.literal("patreon").executes(ctx -> { say(ctx.getSource(), "Patreon link coming soon"); return Command.SINGLE_SUCCESS; }))
            .then(ClientCommands.literal("github").executes(ctx -> { say(ctx.getSource(), "GitHub link coming soon"); return Command.SINGLE_SUCCESS; }))
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
            // /fd sim|simulation <mf> <Dye | Boss> <Drop 1> [Drop 2]  - pretends you got a drop (a boss kill can drop two items)
            .then(simNode("simulation"))
            .then(simNode("simulate"))
            .then(simNode("sim"))
            // /fd <anything else>  - opens the menu and searches for it
            .then(ClientCommands.argument("search", StringArgumentType.greedyString()).executes(ctx -> {
                FancyScreen.pendingSearch = StringArgumentType.getString(ctx, "search").trim(); openMenu = true;
                return Command.SINGLE_SUCCESS;
            }))
            // /fd mf <magic find> [drop name | chance]  - what your magic find does to a drop chance
            .then(ClientCommands.literal("mf")
                .executes(ctx -> { say(ctx.getSource(), "Use /fd mf <magic find> [drop name | 1-in-N chance]   e.g. /fd mf 250 Warden Heart"); return Command.SINGLE_SUCCESS; })
                .then(ClientCommands.argument("magicfind", DoubleArgumentType.doubleArg(0, 100000))
                    .suggests((ctx, builder) -> suggestMagicFind(builder))
                    .executes(ctx -> mfCalc(ctx.getSource(), DoubleArgumentType.getDouble(ctx, "magicfind"), ""))
                    .then(ClientCommands.argument("drop", StringArgumentType.greedyString())
                        .suggests((ctx, builder) -> suggestDropNames(builder))
                        .executes(ctx -> mfCalc(ctx.getSource(), DoubleArgumentType.getDouble(ctx, "magicfind"), StringArgumentType.getString(ctx, "drop"))))));
    }

    /** /fancydrops <name> [on|off]  - no argument flips the setting. */
    private static LiteralArgumentBuilder<FabricClientCommandSource> switchCmd(String name, String label,
            java.util.function.Function<FancyConfig, Boolean> get, java.util.function.BiConsumer<FancyConfig, Boolean> set) {
        return ClientCommands.literal(name)
            .executes(ctx -> {
                FancyConfig c = FancyConfig.get(); set.accept(c, !get.apply(c)); FancyConfig.save();
                say(ctx.getSource(), label + ": " + (get.apply(c) ? "on" : "off"));
                return Command.SINGLE_SUCCESS;
            })
            .then(ClientCommands.argument("state", StringArgumentType.word())
                .suggests((ctx, b) -> suggest(b, ON_OFF))
                .executes(ctx -> {
                    FancyConfig c = FancyConfig.get();
                    String v = StringArgumentType.getString(ctx, "state").toLowerCase(Locale.ROOT);
                    if (!v.equals("on") && !v.equals("off")) { say(ctx.getSource(), "Use: /fancydrops " + name + " <on|off>"); return 0; }
                    set.accept(c, v.equals("on")); FancyConfig.save();
                    say(ctx.getSource(), label + ": " + v);
                    return Command.SINGLE_SUCCESS;
                }));
    }

    static final String DISCORD = "https://discord.gg/bum8VPbRPR";

    private static void say(FabricClientCommandSource source, String msg) { source.sendFeedback(Chat.msg(msg)); }

    private static CompletableFuture<Suggestions> suggest(SuggestionsBuilder b, String... options) {
        for (String o : options) if (o.startsWith(b.getRemaining().toLowerCase(Locale.ROOT))) b.suggest(o);
        return b.buildFuture();
    }
    private static CompletableFuture<Suggestions> suggest(SuggestionsBuilder b, Iterable<String> options) {
        for (String o : options) if (o.startsWith(b.getRemaining().toLowerCase(Locale.ROOT))) b.suggest(o);
        return b.buildFuture();
    }

    private static int help(FabricClientCommandSource s) {
        s.sendFeedback(waveTitle("FancyDrops").copy().append(Chat.color("  ·  Quick guide", Chat.DIM)));
        helpHead(s, "Menu");
        helpLine(s, "/fd", "Open the menu");
        helpLine(s, "/fd stats", "Open the menu on the Stats page");
        helpLine(s, "/fd <search>", "Open the menu and search for what you typed");
        helpHead(s, "Try a drop  (never counts toward profit)");
        helpLine(s, "/fd sim <Boss> <Drop 1> [Drop 2] [mf]", "A boss kill with one or two drops. Use \"nothing\" for an empty kill");
        helpLine(s, "/fd sim Mytho <Creature> <Drop 1> [Drop 2] [mf]", "A Mythological Ritual with one or two drops");
        helpLine(s, "/fd sim Exp <Drop 1> [Drop 2] [mf]", "Experimentation Table rewards, one or two items");
        helpLine(s, "/fd sim Dye <Dye> [mf]", "One dye (random if you leave it out)");
        helpLine(s, "/fd sim Farm <Drop> [mf]", "One farming drop (random if you leave it out)");
        helpHead(s, "Handy");
        helpLine(s, "/fd mf <magic find> [drop]", "What your Magic Find does to a drop chance");
        helpLine(s, "/fd mute", "Turn sounds on or off");
        helpLine(s, "/fd toggle", "Turn the whole mod on or off");
        s.sendFeedback(Component.literal(" "));
        s.sendFeedback(Chat.color("Example  ", Chat.DIM).append(Chat.color("/fd sim Exp Power VII Protection VI 250", Chat.COMMAND)));
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
        say(s, "Animations " + (c.masterAnimate ? "on" : "OFF") + ", announcements " + (c.masterAnnounce ? "on" : "OFF") + " | v" + VERSION + " | 3D " + (c.show3d ? "on" : "off")
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
        if (shown == 0) say(s, "No drop matches \"" + what + "\". You can also type a chance: /fd mf " + trimNum(mf) + " 735");
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

    private static LiteralArgumentBuilder<FabricClientCommandSource> simNode(String name) {
        return ClientCommands.literal(name)
            .executes(ctx -> { say(ctx.getSource(), "Use /fd sim <Boss | Mytho | Exp | Dye | Farm> <Drop 1> [Drop 2] [magic find]   e.g. /fd sim Exp Power VII Protection VI 250  (Tab lists them; /fd help has the guide)"); return Command.SINGLE_SUCCESS; })
            .then(ClientCommands.argument("items", StringArgumentType.greedyString())
                .suggests((ctx, builder) -> suggestDrops(builder))
                .executes(ctx -> simulate(ctx.getSource(), StringArgumentType.getString(ctx, "items"))));
    }

    /** "<what> [mf]": the magic find is a trailing number (a leading one still works, like the old syntax). */
    private static int simulate(FabricClientCommandSource source, String args) {
        String t = args.trim(); int mf = 250;
        String[] w = t.split("\\s+");
        if (w.length > 1 && w[w.length - 1].matches("\\d{1,5}")) { mf = Integer.parseInt(w[w.length - 1]); t = t.substring(0, t.length() - w[w.length - 1].length()).trim(); }
        else if (w.length > 1 && w[0].matches("\\d{1,5}")) { mf = Integer.parseInt(w[0]); t = t.substring(w[0].length()).trim(); }
        else if (w.length == 1 && w[0].matches("\\d{1,5}")) { say(source, "Name what to simulate first: /fd sim <Boss | Mytho | Exp | Dye | Farm> ..."); return 0; }
        return simulate(source, Math.min(10000, mf), t);
    }

    private static int simulate(FabricClientCommandSource source, int mf, String items) {
        DropListener.simulateKill(mf, items, msg -> source.sendFeedback(Chat.msg(msg)));
        return Command.SINGLE_SUCCESS;
    }

    private static CompletableFuture<Suggestions> suggestMagicFind(SuggestionsBuilder builder) {
        String typed = builder.getRemaining();
        for (String value : new String[]{"0", "100", "200", "250", "300", "400"})
            if (value.startsWith(typed)) builder.suggest(value);
        return builder.buildFuture();
    }

    /** /fd sim <mf> <Boss | Dye | Farm | Mytho> <Drop 1> [Drop 2]: first the boss (or "Dye", "Farm", "Mytho"), then its drops one after the other (names may contain spaces). */
    /** Tab completion for /fd sim: the category first, then one or two drops, then a magic find. */
    private static CompletableFuture<Suggestions> suggestDrops(SuggestionsBuilder builder) {
        String rem = builder.getRemaining(), low = rem.toLowerCase(Locale.ROOT);
        java.util.List<String> pool = null; int max = 1, pos = 0; boolean nothing = false, creatures = false;
        String[] kinds = {"dye ", "farm ", "exp ", "mytho "};
        for (String k : kinds) if (low.startsWith(k)) {
            pos = k.length();
            switch (k) {
                case "dye " -> { pool = new java.util.ArrayList<>(); for (Object[] o : DropListener.allDyes()) pool.add(((SlayerData.Drop) o[1]).name); }
                case "farm " -> { pool = new java.util.ArrayList<>(); for (Object[] o : DropListener.allFarmDrops()) pool.add(((SlayerData.Drop) o[1]).name); }
                case "exp " -> { pool = new java.util.ArrayList<>(); for (String[] e : ExperimentData.ALL) pool.add(e[0]); max = 2; }
                default -> {
                    max = 2; creatures = true; pool = new java.util.ArrayList<>();
                    String after = rem.substring(pos).stripLeading();
                    SlayerData.Slayer own = null;
                    for (SlayerData.Slayer ms : MythData.ALL) if (after.regionMatches(true, 0, ms.boss + " ", 0, ms.boss.length() + 1)) own = ms;
                    if (own != null) { pos = rem.length() - after.length() + own.boss.length() + 1; creatures = false; for (SlayerData.Drop md : own.drops) pool.add(md.name); }
                    else { java.util.Set<String> seen = new java.util.LinkedHashSet<>(); for (SlayerData.Slayer ms : MythData.ALL) seen.add(ms.boss); for (SlayerData.Slayer ms : MythData.ALL) for (SlayerData.Drop md : ms.drops) seen.add(md.name); pool.addAll(seen); }
                }
            }
            break;
        }
        if (pool == null) {
            SlayerData.Slayer[] hit = new SlayerData.Slayer[1];
            int n = DropListener.bossPrefix(rem, hit);
            if (hit[0] == null || n >= rem.length()) {                            // still choosing the category
                for (SlayerData.Slayer sl : SlayerData.ALL) if (sl.boss.toLowerCase(Locale.ROOT).startsWith(low)) builder.suggest(sl.boss);
                for (String k : new String[]{"Dye", "Farm", "Mytho", "Exp"}) if (k.toLowerCase(Locale.ROOT).startsWith(low)) builder.suggest(k);
                return builder.buildFuture();
            }
            pool = new java.util.ArrayList<>(DropListener.dropNames(hit[0])); max = 2; nothing = true; pos = n;
        }
        java.util.List<String> chosen = new java.util.ArrayList<>();
        while (true) {                                                           // names already typed in full
            while (pos < rem.length() && Character.isWhitespace(rem.charAt(pos))) pos++;
            String rest = rem.substring(pos); int best = 0;
            for (String nm : pool) if (nm.length() > best && rest.regionMatches(true, 0, nm, 0, nm.length()) && rest.length() > nm.length() && Character.isWhitespace(rest.charAt(nm.length()))) best = nm.length();
            if (best == 0) break;
            chosen.add(rest.substring(0, best).toLowerCase(Locale.ROOT)); pos += best;
            if (creatures) break;
            if (chosen.size() >= max) break;
        }
        String typed = rem.substring(Math.min(pos, rem.length())).stripLeading().toLowerCase(Locale.ROOT);
        SuggestionsBuilder b = builder.createOffset(builder.getStart() + rem.length() - typed.length());
        if (chosen.size() >= max) { for (String m : new String[]{"100", "250", "400"}) if (m.startsWith(typed)) b.suggest(m); return b.buildFuture(); }
        if (nothing && chosen.isEmpty() && "nothing".startsWith(typed)) b.suggest("nothing");
        for (String nm : pool) if (nm.toLowerCase(Locale.ROOT).contains(typed) && !chosen.contains(nm.toLowerCase(Locale.ROOT))) b.suggest(nm);
        if (!chosen.isEmpty() && !creatures) for (String m : new String[]{"100", "250", "400"}) if (m.startsWith(typed)) b.suggest(m);
        return b.buildFuture();
    }
}
