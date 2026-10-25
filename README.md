# FancyDrops

Fabric mod (Minecraft 26.2) that plays a short animation when you drop something on Hypixel SkyBlock, and can announce it in chat. Mythological Rituals (creatures and Griffin burrow treasure) are live. Slayers is next, then Dyes, Farming, Experiments, Mining and Fishing.

* `/fancydrops` or `/fd` - settings menu (Home, Drop Library, Animations, Sounds, Chat, Stats, Settings)
* `/fd simulate Mythological <Mob|Treasure> <Drop> <mf>` - plays a pretend drop through the real animation
* `/fd mute` and `/fd toggle` - sounds on or off, the whole mod on or off
* `/fd help` - every command

Every category has three animations: No Drops, Normal and RNG. Each has its own length, position, size and sounds. Only RNG has the cinematic bars and blur. Chat announcements only go out on Hypixel, and the message format is filtered (no swearing, links or commands).

## Version

The version lives in one place: `version=` in `gradle.properties`. It flows into `fabric.mod.json` and from there into the menu and `/fancydrops status`.

## License

CC0.

## Editing art
Icons: `IconArt.java` (16x16, one `case` per `:name:`). Scene animations: `Ritual.java`. Timing: `AnimationManager.java`. Item art: `ItemIcons.java`, `SkullSpin.java`. Defaults: `FancyConfig.java`.

## Replacing item textures
Open `config/fancydrops/art/items` and `art/heads`. Each item has `<ID>.png` (flat) or `<ID>_head.png` (a 64x64 skin for heads). Replace a file and re-open the game or the item.

## Editing icons
`config/fancydrops/icons/<name>.png`: 16 px wide, 128 tall (8 frames stacked). Save the file and the game reloads it within a second.

## Animation frames
Open `config/fancydrops/art/animations`. Every scene is there as PNG frames (320x180, 8 per second). Edit a frame, save, and the mod plays it. Items, bars and words are drawn on top.
