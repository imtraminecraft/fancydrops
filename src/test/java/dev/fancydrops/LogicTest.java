package dev.fancydrops;

import java.util.ArrayList;
import java.util.List;

/**
 * Plain-Java checks for the chat parsing and slayer-kill tracking (no Minecraft needed).
 * Run:  javac -d out src/main/java/dev/fancydrops/{ChatParser,KillTracker}.java src/test/java/dev/fancydrops/LogicTest.java && java -cp out dev.fancydrops.LogicTest
 */
public final class LogicTest {
    static int fails, passes;
    static void check(String what, boolean ok) { if (ok) passes++; else { fails++; System.out.println("FAIL: " + what); } }

    public static void main(String[] a) {
        // ---- kill line
        check("kill: plain", ChatParser.isKill("Your Slayer Kill gave you +1,500 Slayer XP"));
        check("kill: boss wording", ChatParser.isKill("  Your Slayer Boss Kill gave you 500 XP"));
        check("kill: case-insensitive", ChatParser.isKill("YOUR SLAYER KILL GAVE YOU stuff"));
        check("kill: not a drop line", !ChatParser.isKill("RARE DROP! Scythe Blade"));
        check("kill: unrelated chat", !ChatParser.isKill("[MVP+] Bob: your slayer kill was nice"));
        check("quest complete", ChatParser.isQuestComplete("  SLAYER QUEST COMPLETE!"));

        // ---- drop lines
        var d = ChatParser.parseDrop("RARE DROP! Scythe Blade (+120% ✯ Magic Find)");
        check("drop: name", d != null && d.name().equals("Scythe Blade"));
        check("drop: kind", d != null && d.kind().equals("RARE") && d.rank() == 0);
        check("drop: magic find", d != null && "+120%".equals(d.mf()));
        d = ChatParser.parseDrop("CRAZY RARE DROP! Warden Heart (+250% ✯ Magic Find)");
        check("crazy rare rank", d != null && d.rank() == 2 && d.name().equals("Warden Heart"));
        d = ChatParser.parseDrop("RARE DROP! 2x Revenant Viscera");
        check("amount prefix", d != null && d.amount() == 2 && d.name().equals("Revenant Viscera") && d.mf() == null);
        d = ChatParser.parseDrop("VERY RARE DROP! Foul Flesh (3x) (+10% ✯ Magic Find)");
        check("amount parens", d != null && d.amount() == 3 && d.name().equals("Foul Flesh"));
        check("not a drop", ChatParser.parseDrop("Your Slayer Kill gave you +5 XP") == null);
        check("drop with colour stripped only", ChatParser.parseDrop("  PET DROP! Golden Dragon  ") != null);

        // ---- tracker: kill THEN drop
        List<KillTracker.Session<String>> started = new ArrayList<>();
        KillTracker<String> t = new KillTracker<>(started::add);
        t.kill(1000); t.tick(1010);
        check("no scene before the join delay", started.isEmpty());
        t.drop("A", 1050); t.tick(1160);
        check("scene starts after join delay", started.size() == 1);
        check("drop before the scene started joins it", started.get(0).drops.contains("A"));
        t.drop("B", 1700);
        check("late drop (inside window) joins the open scene", started.get(0).drops.contains("B") && started.size() == 1);
        check("scene closes at kill + window", started.get(0).closeAt == 1000 + KillTracker.WINDOW_MS);
        started.get(0).close();
        t.drop("C", 2600); t.tick(2700);
        check("drop after reveal waits, no new scene yet", started.size() == 1);

        // ---- tracker: drop THEN kill
        started.clear(); t = new KillTracker<>(started::add);
        t.drop("X", 5000); t.drop("Y", 5010); t.kill(5400); t.tick(5560);
        check("drops before the kill join its scene", started.size() == 1 && started.get(0).drops.size() == 2 && started.get(0).fromKill);

        // ---- tracker: nothing dropped
        started.clear(); t = new KillTracker<>(started::add);
        t.kill(100); t.tick(300);
        check("kill with no drops still starts a scene", started.size() == 1 && started.get(0).drops.isEmpty());

        // ---- tracker: drop with no kill line at all
        started.clear(); t = new KillTracker<>(started::add);
        t.drop("Z", 9000); t.tick(9500);
        check("lone drop does not start instantly", started.isEmpty());
        t.tick(10600);
        check("lone drop gets its own scene after the wait", started.size() == 1 && !started.get(0).fromKill && started.get(0).drops.contains("Z"));

        // ---- tracker: duplicate kill messages (e.g. quest-complete + kill line)
        started.clear(); t = new KillTracker<>(started::add);
        t.kill(20000); t.kill(20300); t.tick(20400); t.tick(21000);
        check("duplicate kill lines make one scene", started.size() == 1 && t.kills == 1);

        // ---- tracker: two separate kills
        t.kill(30000); t.tick(30200);
        check("a later kill starts a new scene", started.size() == 2 && t.kills == 2);

        // ---- stale pending drops are not glued to a much later kill
        started.clear(); t = new KillTracker<>(started::add);
        t.drop("OLD", 40000); t.tick(41600);       // becomes its own scene
        started.get(0).close();
        t.kill(50000); t.tick(50200);
        check("old drop not re-used by a later kill", started.size() == 2 && started.get(1).drops.isEmpty());

        // ---- chat message filter
        for (String ok : new String[]{"RARE DROP! {item} ({price}) from {slayer}", "!!!!! | {item} | {mf}", "Class assassin passes the assessment", "GG {item} worth {price}", "Scunthorpe classic"})
            check("filter allows: " + ok, ChatFilter.ok(ok));
        for (String bad : new String[]{"what the fuck {item}", "F U C K {item}", "sh1t drop", "{item} you b!tch", "fuuuuck", "n1gg3r", "/kick everyone", "join discord.gg/abc", "visit www.x.com", "nice {unknown}", "", "a".repeat(121)})
            check("filter blocks: " + bad, !ChatFilter.ok(bad));

        System.out.println(passes + " passed, " + fails + " failed");
        if (fails > 0) System.exit(1);
    }
    private LogicTest() {}
}
