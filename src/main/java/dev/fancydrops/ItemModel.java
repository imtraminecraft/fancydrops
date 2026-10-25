package dev.fancydrops;

/** What an animation draws for a dropped item: a 3D skull / block, or a flat sprite puffed into a thin 3D slab. */
final class ItemModel {
    final Render3D.Obj[] parts; final double natural, pitchK; final PuffyItem puffy; final int[] sprite;   // sprite: flat 16x16 art of a non-skull item
    ItemModel(Render3D.Obj[] parts, double natural, double pitchK) { this.parts = parts; this.natural = natural; this.pitchK = pitchK; puffy = null; sprite = null; }
    ItemModel(PuffyItem puffy, int[] sprite) { this.puffy = puffy; this.sprite = sprite; parts = null; natural = 16; pitchK = 1; }   // sprite == null: a skull we only have a picture of, spun as a thick 3D slab
}
