package com.sovereignrpg.core.server;

import com.sovereignrpg.core.CoreConfig;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Souls Like Bosses: renames the Nightlord's bar per phase and gives Melkor two full bars (one per phase) with a
 * phase 2 name. Works on the mod through reflection only, so nothing here breaks if the mod is missing or changes.
 */
@Mod.EventBusSubscriber(modid = "sovereignrpgcore")
public final class BossBars {
    private BossBars() {}

    private static final String NIGHTLORD = "com.slb.cinder.nl.NightlordEntity";
    private static final String MELKOR = "com.slb.cinder.te.TimeEraserEntity";
    private static final String NL_ORIG_1 = "The Shape of Night";
    private static final String NL_ORIG_2 = "The Nightlord";

    private static final List<Entity> TRACKED = new ArrayList<>();
    private static final Map<Class<?>, Field> BAR = new HashMap<>();
    private static final Map<Class<?>, Method> SCRIPT = new HashMap<>();
    private static final Map<Class<?>, Field> BAR_ACTIVE = new HashMap<>();
    private static final Map<Class<?>, Method> GET = new HashMap<>();

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent e) {
        if (e.getLevel().isClientSide()) return;
        String n = e.getEntity().getClass().getName();
        if ((n.equals(NIGHTLORD) || n.equals(MELKOR)) && !TRACKED.contains(e.getEntity())) TRACKED.add(e.getEntity());
        if (n.equals(MELKOR) && CoreConfig.BOSS_BARS.get()) {
            try {
                bar(e.getEntity()).setName(title(CoreConfig.MELKOR_1.get()));
            } catch (Throwable t) {
                // ignore
            }
        }
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || TRACKED.isEmpty() || !CoreConfig.BOSS_BARS.get()) return;
        TRACKED.removeIf(Entity::isRemoved);
        for (Entity boss : TRACKED) {
            try {
                if (boss.getClass().getName().equals(NIGHTLORD)) nightlord(boss);
                else melkor(boss);
            } catch (Throwable t) {
                // never crash the server over a boss bar
            }
        }
    }

    private static ServerBossEvent bar(Entity boss) throws Exception {
        Field f = BAR.get(boss.getClass());
        if (f == null) {
            f = boss.getClass().getDeclaredField("bar");
            f.setAccessible(true);
            BAR.put(boss.getClass(), f);
        }
        return (ServerBossEvent) f.get(boss);
    }

    private static int score(Entity boss, String objective) throws Exception {
        Class<?> c = boss.getClass();
        Method script = SCRIPT.get(c);
        if (script == null) {
            script = c.getMethod("script");
            SCRIPT.put(c, script);
        }
        Object s = script.invoke(boss);
        Method get = GET.get(s.getClass());
        if (get == null) {
            get = s.getClass().getMethod("get", String.class);
            get.setAccessible(true);
            GET.put(s.getClass(), get);
        }
        return (Integer) get.invoke(s, objective);
    }

    private static boolean barActive(Entity boss) throws Exception {
        Field f = BAR_ACTIVE.get(boss.getClass());
        if (f == null) {
            f = boss.getClass().getDeclaredField("barActive");
            f.setAccessible(true);
            BAR_ACTIVE.put(boss.getClass(), f);
        }
        return f.getBoolean(boss);
    }

    private static Component title(String s) {
        return Component.literal(s).withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD);
    }

    private static Component title(String s, int rgb) {
        return Component.literal(s).withStyle(st -> st.withColor(rgb).withBold(true));
    }

    /** The mod writes its own names (phase 1, then phase 2 mid-transition); swap each for the configured one. */
    private static void nightlord(Entity boss) throws Exception {
        ServerBossEvent bar = bar(boss);
        String cur = bar.getName().getString();
        if (cur.equals(NL_ORIG_1)) bar.setName(title(CoreConfig.NIGHTLORD_1.get(), 0xFFD27A));
        else if (cur.equals(NL_ORIG_2)) bar.setName(title(CoreConfig.NIGHTLORD_2.get(), 0xB8C4E0));
    }

    /**
     * Melkor's script already has a real phase change at half health (tag boss_phase_2). Her bar just ignores it:
     * one bar over the whole pool. Phase 1 drains the first half, phase 2 refills and drains the second half.
     */
    private static void melkor(Entity boss) throws Exception {
        ServerBossEvent bar = bar(boss);
        boolean two = boss.getTags().contains("boss_phase_2");
        String name = two ? CoreConfig.MELKOR_2.get() : CoreConfig.MELKOR_1.get();
        if (!bar.getName().getString().equals(name)) bar.setName(title(name));
        if (!barActive(boss) || score(boss, "boss_active") != 1) return;
        int health = score(boss, "boss_health");
        int floor = score(boss, "te_health");
        int split = score(boss, "final_boss_attack_health");
        int span = two ? split - floor : 500000 - split;
        int value = two ? health - floor : health - split;
        float p = span <= 0 ? 0.0F : Math.max(0.0F, Math.min(1.0F, (float) value / (float) span));
        bar.setProgress(p);
    }
}
