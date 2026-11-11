package com.sovereignrpg.core.client;

import com.sovereignrpg.core.CoreConfig;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Advanced Compass only knows Xaero, JourneyMap and FTB Chunks waypoints. This copies the Remnants Minimap markers and
 * beacons of the current dimension onto the compass so they show while you face them. Everything is reflection, so
 * nothing breaks when either mod is missing; after repeated failures the bridge switches itself off.
 */
@Mod.EventBusSubscriber(modid = "sovereignrpgcore", value = Dist.CLIENT)
public final class RemnantCompassBridge {
    private RemnantCompassBridge() {}

    private static final Logger LOG = LogManager.getLogger("SovereignCore/CompassBridge");
    private static final Map<String, String> ICONS = new HashMap<>();
    static {
        // Remnants marker icon -> Advanced Compass waypoint icon (names of the Advanced Compass enum)
        String[][] m = {
            {"GRACE", "STAR"}, {"BEACON", "DIAMOND"}, {"PIN", "PIN"}, {"DEATH", "SKULL"}, {"BED", "HOME"}, {"HOME", "HOME"},
            {"VILLAGE", "HOME"}, {"CASTLE", "HOME"}, {"CHURCH", "SIGN"}, {"RUINS", "CROSS"}, {"TOWER", "SIGN"},
            {"CAVE", "PICKAXE"}, {"CATACOMB", "SKULL"}, {"PORTAL", "ROUND"}, {"BOSS", "SKULL"}, {"DANGER", "SIGN"},
            {"TREASURE", "DOLLAR"}, {"MINE", "PICKAXE"}, {"FORGE", "AXE"}, {"FARM", "HOE"}, {"TREE", "AXE"},
            {"CAMP", "HOME"}, {"SHIP", "ROUND"}, {"STAR", "STAR"}, {"FLAG", "SIGN"}, {"MERCHANT", "DOLLAR"},
            {"GATE", "RHOMBUS"}, {"SPRING", "ROUND"}, {"MOUNTAIN", "DIAMOND"}, {"QUESTION", "SIGN"}, {"SWORD", "SWORD"},
            {"EYE", "ROUND"}
        };
        for (String[] e : m) ICONS.put(e[0], e[1]);
    }

    private static final List<Object> MINE = new ArrayList<>();
    private static int tick;
    private static int failures;
    private static boolean dead;

    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END || dead) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            MINE.clear();
            return;
        }
        if (++tick % 10 != 0) return;
        if (!CoreConfig.COMPASS_BRIDGE.get()) return;
        ModList ml = ModList.get();
        if (!ml.isLoaded("advancedcompass") || !ml.isLoaded("remnantminimaps")) {
            dead = true;
            return;
        }
        try {
            sync(mc);
            failures = 0;
        } catch (Throwable t) {
            if (++failures >= 5) {
                dead = true;
                LOG.warn("Compass bridge switched off after repeated errors: {}", t.toString());
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static void sync(Minecraft mc) throws Exception {
        Object gui = null;
        Object ac = Class.forName("dlovin.advancedcompass.AdvancedCompass").getMethod("getInstance").invoke(null);
        if (ac != null) gui = ac.getClass().getMethod("getCompassGui").invoke(ac);
        if (gui == null) return;
        Field wrField = gui.getClass().getDeclaredField("waypointRenderer");
        wrField.setAccessible(true);
        Object renderer = wrField.get(gui);
        if (renderer == null) return;
        Field wpField = renderer.getClass().getDeclaredField("waypoints");
        wpField.setAccessible(true);
        List<Object> current = (List<Object>) wpField.get(renderer);

        // everything that is not ours
        List<Object> next = new ArrayList<>();
        if (current != null) {
            for (Object o : current) {
                boolean ours = false;
                for (Object m : MINE) if (m == o) { ours = true; break; }
                if (!ours) next.add(o);
            }
        }
        MINE.clear();

        Object session = Class.forName("io.nightbeam.remnantminimaps.client.RemnantClient").getMethod("session").invoke(null);
        if (session != null) {
            Object store = session.getClass().getField("markers").get(session);
            Class<?> sessionCls = Class.forName("io.nightbeam.remnantminimaps.client.session.MapSession");
            String dim = (String) sessionCls.getMethod("dimensionId", ResourceKey.class).invoke(null, mc.level.dimension());
            List<?> markers = new ArrayList<>((List<?>) store.getClass().getMethod("inDimension", String.class).invoke(store, dim));

            Class<?> wpCls = Class.forName("dlovin.advancedcompass.utils.waypoints.ModdedWaypoint");
            Class<?> iconCls = Class.forName("dlovin.advancedcompass.utils.waypoints.WaypointIcon");
            Class<?> colorCls = Class.forName("dlovin.advancedcompass.utils.Color");
            Constructor<?> ctor = wpCls.getConstructor(Vec3.class, iconCls, colorCls, boolean.class);
            Constructor<?> colorCtor = colorCls.getConstructor(int.class);
            Object[] icons = iconCls.getEnumConstants();
            Object fallback = null;
            Map<String, Object> iconByName = new HashMap<>();
            for (Object ic : icons) iconByName.put(((Enum<?>) ic).name(), ic);
            fallback = iconByName.get("PIN");

            boolean deaths = CoreConfig.COMPASS_DEATHS.get();
            int max = CoreConfig.COMPASS_MAX_DISTANCE.get();
            double px = mc.player.getX(), py = mc.player.getY(), pz = mc.player.getZ();

            for (Object mk : markers) {
                Class<?> c = mk.getClass();
                if (c.getField("hidden").getBoolean(mk)) continue;
                Object kind = c.getField("kind").get(mk);
                String kindName = kind == null ? "" : ((Enum<?>) kind).name();
                if (!deaths && kindName.equals("DEATH")) continue;
                int x = c.getField("x").getInt(mk), z = c.getField("z").getInt(mk);
                boolean hasY = c.getField("hasY").getBoolean(mk);
                double y = hasY ? c.getField("y").getInt(mk) : py;
                if (max > 0) {
                    double dx = x + 0.5 - px, dz = z + 0.5 - pz;
                    if (dx * dx + dz * dz > (double) max * max) continue;
                }
                Object icon = c.getField("icon").get(mk);
                String iconName = icon == null ? "PIN" : ((Enum<?>) icon).name();
                Object wpIcon = iconByName.getOrDefault(ICONS.getOrDefault(iconName, "PIN"), fallback);
                int rgb = 0xFFFFFF;
                try {
                    Method tint = c.getMethod("tint");
                    rgb = ((Integer) tint.invoke(mk)) & 0xFFFFFF;
                } catch (Throwable ignored) {
                    // keep white
                }
                Object wp = ctor.newInstance(new Vec3(x + 0.5, y, z + 0.5), wpIcon, colorCtor.newInstance(rgb), hasY);
                next.add(wp);
                MINE.add(wp);
            }
        }
        wpField.set(renderer, next);
    }
}
