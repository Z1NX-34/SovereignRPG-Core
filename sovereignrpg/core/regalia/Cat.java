package com.sovereignrpg.core.regalia;

/** What kind of item a regalia job applies to. group: 0 weapon (main hand), 1 armor, 2 curios. */
public enum Cat {
    LIGHT_MELEE(0, "Light weapons"), HEAVY_MELEE(0, "Heavy weapons"), MELEE(0, "Melee weapons"),
    LIGHT_RANGED(0, "Light ranged"), HEAVY_RANGED(0, "Heavy ranged"), RANGED(0, "Ranged weapons"),
    WEAPON(0, "All weapons"), WAND(0, "Wands"),
    HELMET(1, "Helmets"), CHEST(1, "Chestplates"), LOWER(1, "Leggings and boots"), ARMOR(1, "Armor"),
    CURIOS(2, "Curios"), RING(2, "Rings"), NECKLACE(2, "Necklaces"), CHARM(2, "Charms"),
    SPELLSTONE(2, "Spellstones"), SPELLBOOK(2, "Spellbooks");

    public final int group;
    public final String label;

    Cat(int group, String label) { this.group = group; this.label = label; }

    public boolean ranged() { return this == LIGHT_RANGED || this == HEAVY_RANGED || this == RANGED; }
}
