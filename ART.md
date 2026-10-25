# Where the art is

Everything that is drawn, by kind. All files are in `src/main/java/dev/fancydrops/` (Java needs them in the package folder).

| Folder | What | Main file |
|---|---|---|
| icons | the 16x16 pixel icons (`:name:`), 16 frames each | `IconArt.java` (one `case` per icon), `Icons.java` (drawing, caching, PNG overrides) |
| animations | the Mythological and Treasure scenes, creatures, digger, timing | `Ritual.java` (pictures), `AnimationManager.java` (timing and cues), `SceneTexture.java` |
| heads | 3D heads and item pictures | `SkullSpin.java` (the turning block), `ItemIcons.java` (loading and the PNG folder), `ItemModel.java` |

## Replacing art without touching code
In the game's config folder: `config/fancydrops/art/`
- `icons/<name>.png`: 16 px wide, 256 px tall (16 frames stacked). A 16x16 file is a still icon. Saved files reload within a second.
- `heads/<ITEM_ID>_head.png`: a 64x64 or 64x32 skin, shown as the 3D head.
- `items/<ITEM_ID>.png`: a flat item picture (any size, scaled to 16x16).
- `animations/`: a note only. The scenes are code.
