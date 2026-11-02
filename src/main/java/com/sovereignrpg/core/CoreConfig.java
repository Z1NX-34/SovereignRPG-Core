package com.sovereignrpg.core;

import net.minecraftforge.common.ForgeConfigSpec;

/** config/sovereignrpgcore-common.toml - everything Core lets you tune lives here. */
public final class CoreConfig {
    private CoreConfig() {}

    public static final class Mode {
        public final ForgeConfigSpec.DoubleValue taken, dealt, mobHealth, mobSpeed, mobArmor, mobDamage, mobExp;

        Mode(ForgeConfigSpec.Builder b, String name, String comment, double taken, double dealt) {
            b.comment(comment).push(name);
            this.taken = b.comment("Multiplier on damage the player takes. 1.5 = 50% more, 0.75 = 25% less.")
                .defineInRange("damage_taken", taken, 0.0, 100.0);
            this.dealt = b.comment("Multiplier on damage the player deals. 0.8 = 20% less.")
                .defineInRange("damage_dealt", dealt, 0.0, 100.0);
            this.mobHealth = b.comment("Extra enemy max health. 0 = unchanged, 1 = +100%.").defineInRange("mob_health", 0.0, -0.95, 100.0);
            this.mobSpeed = b.comment("Extra enemy speed. 0 = unchanged, 1 = +100%.").defineInRange("mob_speed", 0.0, -0.95, 100.0);
            this.mobArmor = b.comment("Extra enemy armor points added. 0 = unchanged.").defineInRange("mob_armor", 0.0, -100.0, 100.0);
            this.mobDamage = b.comment("Extra enemy damage. 0 = unchanged, 1 = +100%.").defineInRange("mob_damage", 0.0, -0.95, 100.0);
            this.mobExp = b.comment("Multiplier on XP enemies drop. 1 = unchanged.").defineInRange("mob_exp", 1.0, 0.0, 100.0);
            b.pop();
        }
    }

    public static final ForgeConfigSpec SPEC;
    public static final Mode NOVICE, CHAMPION, SOVEREIGN;
    public static final ForgeConfigSpec.BooleanValue RING_RESTRICT;
    public static final ForgeConfigSpec.ConfigValue<String> RING_CLASS;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        b.comment("Difficulty values. Core writes these into True Difficulty Scale on startup, so this is the only place to edit them.").push("difficulty");
        NOVICE = new Mode(b, "novice", "Novice (TDS Casual)", 0.75, 1.0);
        // Adept (TDS Normal) is plain vanilla and has no values
        CHAMPION = new Mode(b, "champion", "Champion (TDS Expert)", 1.5, 0.8);
        SOVEREIGN = new Mode(b, "sovereign", "Sovereign (TDS Malicious)", 3.0, 0.7);
        b.pop();

        b.comment("Ring of the Seven Curses rule. Enigmatic Legacy hands the ring to everyone at spawn; Core removes it from every class except the one below.").push("cursed_ring");
        RING_RESTRICT = b.comment("Turn the rule on or off.").define("restrict", true);
        RING_CLASS = b.comment("The only class that keeps (and always wears) the ring. Class id, with or without namespace.").define("class", "cursed_god");
        b.pop();
        SPEC = b.build();
    }

    public static Mode mode(String tdsKey) {
        return switch (tdsKey) {
            case "casual" -> NOVICE;
            case "expert" -> CHAMPION;
            case "malice" -> SOVEREIGN;
            default -> null;
        };
    }
}
