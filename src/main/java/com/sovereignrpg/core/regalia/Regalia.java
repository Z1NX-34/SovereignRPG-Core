package com.sovereignrpg.core.regalia;

import java.util.ArrayList;
import java.util.List;

/** The regalia table: name, color and the 3 jobs (one per kind of item) with the value at each tier. */
public final class Regalia {
    public record Job(Cat cat, Eff eff, double[] vals) {
        public double value(int tier) { return vals[Math.max(1, Math.min(3, tier)) - 1]; }

        public String text(int tier) {
            double v = value(tier);
            String n = Math.abs(v - Math.rint(v)) < 0.001 ? String.valueOf((long) Math.rint(v)) : String.valueOf(v);
            return "+" + n + (eff.flat ? " " : "% ") + eff.label;
        }
    }

    public final String id, name;
    public final int color;
    public final Job[] jobs;

    private Regalia(String id, String name, int color, Job... jobs) {
        this.id = id; this.name = name; this.color = color; this.jobs = jobs;
    }

    private static Job j(Cat c, Eff e, double a, double b, double d) { return new Job(c, e, new double[]{a, b, d}); }

    public static final List<Regalia> ALL = new ArrayList<>();
    private static Regalia add(Regalia r) { ALL.add(r); return r; }

    static {
        add(new Regalia("ferocity", "Ferocity", 0xFF7D28,
            j(Cat.LIGHT_MELEE, Eff.CRIT_CHANCE, 8, 12, 18), j(Cat.HEAVY_MELEE, Eff.CRIT_DAMAGE, 15, 20, 30), j(Cat.RANGED, Eff.RANGED_DAMAGE, 15, 20, 28)));
        add(new Regalia("enhancement", "Enhancement", 0x3CCDBE,
            j(Cat.HELMET, Eff.DR, 4, 6, 9), j(Cat.CHEST, Eff.DR, 10, 14, 20), j(Cat.LOWER, Eff.MOVE_SPEED, 8, 11, 15)));
        add(new Regalia("constitution", "Constitution", 0xE13740,
            j(Cat.MELEE, Eff.LIFESTEAL, 3, 4, 6), j(Cat.ARMOR, Eff.MAX_HEALTH, 4, 6, 10), j(Cat.CURIOS, Eff.HEAL_RECEIVED, 1, 2, 3)));
        add(new Regalia("precision", "Precision", 0xEBF0F8,
            j(Cat.MELEE, Eff.ARMOR_PIERCE, 10, 15, 22), j(Cat.LIGHT_RANGED, Eff.ARMOR_PIERCE, 20, 26, 35), j(Cat.HEAVY_RANGED, Eff.ARMOR_PIERCE, 25, 32, 42)));
        add(new Regalia("executioner", "Executioner", 0xBE1E30,
            j(Cat.WEAPON, Eff.LOW_HP_DAMAGE, 20, 28, 40), j(Cat.ARMOR, Eff.LOW_HP_DR, 15, 20, 28), j(Cat.CURIOS, Eff.LOW_HP_DAMAGE_CURIOS, 2, 3, 4)));
        add(new Regalia("sanctuary", "Sanctuary", 0x96CDFF,
            j(Cat.WAND, Eff.SPELL_POWER, 10, 15, 22), j(Cat.CHEST, Eff.MAGIC_DR, 18, 24, 32), j(Cat.LOWER, Eff.STAMINA, 15, 20, 28)));
        add(new Regalia("alchemist", "Alchemist", 0x5FDC5A,
            j(Cat.SPELLBOOK, Eff.SPELL_POWER, 25, 32, 42), j(Cat.CURIOS, Eff.POTION_DURATION, 15, 22, 32), j(Cat.ARMOR, Eff.POTION_DURATION, 22, 30, 42)));
        add(new Regalia("echoes", "Echoes", 0xB262F2,
            j(Cat.RING, Eff.SPELL_CDR, 3, 4, 5), j(Cat.NECKLACE, Eff.SPELL_CDR, 7, 9, 12), j(Cat.CHARM, Eff.XP_GAIN, 35, 45, 60)));
        add(new Regalia("wards", "Wards", 0xBEC4D4,
            j(Cat.RING, Eff.XP_GAIN, 25, 32, 42), j(Cat.CHARM, Eff.MAGIC_DR, 18, 24, 32), j(Cat.SPELLSTONE, Eff.SPELL_CDR, 5, 7, 10)));
    }

    public static Regalia byId(String id) {
        for (Regalia r : ALL) if (r.id.equals(id)) return r;
        return null;
    }

    /** The job of this regalia that applies to an item with these categories, within the given group, or null. */
    public Job jobFor(java.util.Set<Cat> cats, int group) {
        for (Job jb : jobs) if (jb.cat().group == group && cats.contains(jb.cat())) return jb;
        return null;
    }

    public Job jobAny(java.util.Set<Cat> cats) {
        for (Job jb : jobs) if (cats.contains(jb.cat())) return jb;
        return null;
    }
}
