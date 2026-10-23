# FancyDrops

Fabric mod (Minecraft 26.2) that plays a short animation for Hypixel SkyBlock slayer drops and can announce them in party / co-op chat.

* `/fancydrops` or `/fd` - settings menu (Drop Library, Animations, Sounds, Chat, General, Stats)
* `/fd test [nothing|<drop name>]` - simulates "Your Slayer Kill gave you ..." (and a RARE DROP line) through the real detection
* `/fd help` - every command

Three scenes: Default, Rare Drop (crazy-rare class drops) and RNG Drop (drops worth 20M+, configurable). A kill that drops nothing you ticked plays the empty "nothing" scene.
Chat announcements only go out on Hypixel, and only for drops worth at least the RNG Drop price. The message format is filtered (no swearing, links or commands).
Stats tracks profit per slayer and combined: overall (saved), this session, and per hour of actual slaying.

## Version

The version lives in one place: `version=` in `gradle.properties`. It flows into `fabric.mod.json` and from there into the menu and `/fancydrops status`.

## Tests

`LogicTest` covers the chat parsing and the kill/drop pairing (no Minecraft needed):

    javac -d out src/main/java/dev/fancydrops/ChatParser.java src/main/java/dev/fancydrops/KillTracker.java src/main/java/dev/fancydrops/ChatFilter.java src/test/java/dev/fancydrops/LogicTest.java
    java -cp out dev.fancydrops.LogicTest

## License

CC0.
