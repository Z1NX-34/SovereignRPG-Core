package com.sovereignrpg.core.regalia;

/** What a regalia job does. flat = shown and applied as a plain number instead of a percentage. */
public enum Eff {
    CRIT_CHANCE("Crit Chance", false), CRIT_DAMAGE("Crit Damage", false), RANGED_DAMAGE("Ranged Damage", false),
    DR("Damage Reduction", false), MOVE_SPEED("Movement Speed", false), LIFESTEAL("Lifesteal", false),
    MAX_HEALTH("Max Health", true), HEAL_RECEIVED("Healing Received", false), ARMOR_PIERCE("Armor Pierce", false),
    LOW_HP_DAMAGE("Damage to enemies at 15% health or less", false),
    LOW_HP_DR("Damage reduction from enemies at 15% health or less", false),
    LOW_HP_DAMAGE_CURIOS("Damage to enemies at 10% health or less", false),
    SPELL_POWER("Spell Power", false), MAGIC_DR("Magic Damage Reduction", false), STAMINA("Max Stamina", false),
    POTION_DURATION("Potion Duration", false), SPELL_CDR("Spell Cooldown Reduction", false), XP_GAIN("XP Gain", false);

    public final String label;
    public final boolean flat;

    Eff(String label, boolean flat) { this.label = label; this.flat = flat; }
}
