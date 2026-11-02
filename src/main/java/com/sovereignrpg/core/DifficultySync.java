package com.sovereignrpg.core;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModList;
import org.apache.logging.log4j.LogManager;

/** Pushes Core's difficulty values into True Difficulty Scale's own config (found by reflection; no compile dependency). */
final class DifficultySync {
    private DifficultySync() {}

    static void apply() {
        if (!ModList.get().isLoaded("truedifficultyscale")) return;
        try {
            Class<?> c = Class.forName("net.somekoifee.truedifficultyscale.configuration.DifficulyScaleConfigConfiguration");
            push(c, "CASUAL", CoreConfig.NOVICE);
            push(c, "EXPERT", CoreConfig.CHAMPION);
            push(c, "MALICIOUS", CoreConfig.SOVEREIGN);
        } catch (Throwable t) {
            LogManager.getLogger("sovereignrpgcore").warn("Could not sync difficulty values into True Difficulty Scale", t);
        }
    }

    private static void push(Class<?> c, String p, CoreConfig.Mode m) throws Exception {
        set(c, p + "_MODE_DAMAGETAKEN_SCALE", m.taken.get());
        set(c, p + "_MODE_DAMAGEDEALT_SCALE", m.dealt.get());
        set(c, p + "_MOB_HEALTH", m.mobHealth.get());
        set(c, p + "_MOB_SPEED", m.mobSpeed.get());
        set(c, p + "_MOB_ARMOR", m.mobArmor.get());
        set(c, p + "_MOB_DAMAGE", m.mobDamage.get());
        set(c, p + "_MOB_EXP", m.mobExp.get());
    }

    @SuppressWarnings("unchecked")
    private static void set(Class<?> c, String field, double v) throws Exception {
        ((ForgeConfigSpec.ConfigValue<Double>) c.getField(field).get(null)).set(v);
    }
}
