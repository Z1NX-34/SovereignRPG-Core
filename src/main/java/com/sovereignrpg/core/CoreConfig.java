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
    public static final ForgeConfigSpec.BooleanValue BOSS_BARS;
    public static final ForgeConfigSpec.ConfigValue<String> NIGHTLORD_1, NIGHTLORD_2, MELKOR_1, MELKOR_2;
    public static final ForgeConfigSpec.BooleanValue SKRYTHE_BAR;
    public static final ForgeConfigSpec.BooleanValue COMPASS_BRIDGE, COMPASS_DEATHS, WIPE_NEW_WORLD;
    public static final ForgeConfigSpec.IntValue COMPASS_MAX_DISTANCE;
    public static final ForgeConfigSpec.BooleanValue ARENA_NO_SPAWNS;
    public static final ForgeConfigSpec.ConfigValue<String> ARENA_DIMENSION;
    public static final ForgeConfigSpec.BooleanValue DRAGON_CAMEO;
    public static final ForgeConfigSpec.ConfigValue<java.util.List<? extends String>> CAMEO_DRAGONS;
    public static final ForgeConfigSpec.BooleanValue HIDE_DEATH_TITLES;
    public static final ForgeConfigSpec.ConfigValue<java.util.List<? extends String>> DEATH_TITLES;

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

        b.comment("Boss bar names and phases for Souls Like Bosses. Melkor gets two full bars like the other bosses.").push("boss_bars");
        BOSS_BARS = b.comment("Turn the whole feature on or off.").define("enabled", true);
        NIGHTLORD_1 = b.define("nightlord_phase_1", "God Of 7 Realms, The Knightmare");
        NIGHTLORD_2 = b.define("nightlord_phase_2", "The Forgotten Name, Ez'Aethelgrim");
        MELKOR_1 = b.define("melkor_phase_1", "Melkor");
        MELKOR_2 = b.define("melkor_phase_2", "Goddess of Eternity");
        SKRYTHE_BAR = b.comment("Give Skrythe (Monster Expansion) a boss bar. The mod has none, so the HUD and Boss Intros never see him as a boss.").define("skrythe_bar", true);
        HIDE_DEATH_TITLES = b.comment("Stop the big screen titles Souls Like Bosses shows when a boss dies (Boss Intros already shows a victory card).").define("hide_death_titles", true);
        DEATH_TITLES = b.comment("Title texts to drop, compared ignoring case.").defineList("death_titles",
                java.util.List.of("NIGHTLORD SLAIN", "LORD OF CINDER FALLEN", "LORD OF TIME FALLEN"), o -> o instanceof String);
        b.pop();

        b.comment("Devourer of Gods tie-in. Each dragon below gets DoG's devour cutscene on its first kill (once per dragon per world) and drops a Draconic Soul Fragment on every kill. The Ender Dragon is already handled by DoG itself.").push("dragon_cameo");
        DRAGON_CAMEO = b.define("enabled", true);
        CAMEO_DRAGONS = b.defineList("dragons",
                java.util.List.of("aether_dragon:aetherdragon", "block_factorys_bosses:infernal_dragon", "monsterexpansion:skrythe"), o -> o instanceof String);
        b.pop();

        b.comment("Arena dimension (P1nero's Battle Field). Stops natural mob spawning there; spawn eggs, commands, spawners and boss summons still work.").push("arena");
        ARENA_NO_SPAWNS = b.define("block_natural_spawns", true);
        ARENA_DIMENSION = b.define("dimension", "pbf1:sanctum_of_the_battle1");
        b.pop();
        b.comment("Client UI tie-ins: Advanced Compass and Remnants Minimap.").push("ui");
        COMPASS_BRIDGE = b.comment("Show Remnants Minimap markers and beacons on the Advanced Compass (only the current dimension).").define("compass_remnant_markers", true);
        COMPASS_DEATHS = b.comment("Also show bloodstain (death) markers on the compass.").define("compass_show_bloodstains", true);
        COMPASS_MAX_DISTANCE = b.comment("Hide markers farther than this many blocks from the compass. 0 = no limit.").defineInRange("compass_marker_max_distance", 0, 0, 1000000);
        WIPE_NEW_WORLD = b.comment("When a brand-new single-player world is created, delete any Remnants Minimap map and markers left over from an older world with the same folder name.").define("wipe_remnant_data_for_new_worlds", true);
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
