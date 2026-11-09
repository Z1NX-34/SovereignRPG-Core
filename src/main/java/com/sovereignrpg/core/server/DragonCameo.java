package com.sovereignrpg.core.server;

import com.sovereignrpg.core.CoreConfig;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Devourer of Gods tie-in for dragons that are not the Ender Dragon: plays DoG's devour cutscene on the first kill of
 * each listed dragon (once per world) and drops a Draconic Soul Fragment on every kill. Everything is reached through
 * reflection, so Core still loads without DoG.
 */
@Mod.EventBusSubscriber(modid = "sovereignrpgcore")
public final class DragonCameo {
    private DragonCameo() {}

    private static final ResourceLocation FRAGMENT_ENTITY = new ResourceLocation("dog", "draconic_soul_fragment");
    private static final ResourceLocation FRAGMENT_ITEM = new ResourceLocation("dog", "draconic_soul_fragment");
    private static final int AFTER_CUTSCENE = 200;
    private static final int NO_CUTSCENE = 20;

    private static final List<Pending> PENDING = new ArrayList<>();
    private static final Set<UUID> HANDLED = new HashSet<>();

    private static final class Pending {
        final ServerLevel level;
        final Vec3 pos;
        int ticks;

        Pending(ServerLevel level, Vec3 pos, int ticks) {
            this.level = level;
            this.pos = pos;
            this.ticks = ticks;
        }
    }

    /** Which dragons already had their cutscene in this world. */
    public static final class Data extends SavedData {
        private final Set<String> done = new HashSet<>();

        static Data load(CompoundTag tag) {
            Data d = new Data();
            for (Tag t : tag.getList("done", Tag.TAG_STRING)) d.done.add(t.getAsString());
            return d;
        }

        @Override
        public CompoundTag save(CompoundTag tag) {
            ListTag list = new ListTag();
            for (String s : done) list.add(StringTag.valueOf(s));
            tag.put("done", list);
            return tag;
        }

        boolean first(String id) {
            if (!done.add(id)) return false;
            setDirty();
            return true;
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent e) {
        LivingEntity boss = e.getEntity();
        if (boss.level().isClientSide || boss instanceof EnderDragon) return;
        if (!CoreConfig.DRAGON_CAMEO.get() || !ModList.get().isLoaded("dog")) return;
        ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(boss.getType());
        if (key == null || !CoreConfig.CAMEO_DRAGONS.get().contains(key.toString())) return;
        if (!(boss.level() instanceof ServerLevel level) || !HANDLED.add(boss.getUUID())) return;
        // In the End DoG already treats a dragon-named boss as the Ender Dragon; leave that to it.
        if (level.dimension() == Level.END && key.getPath().contains("dragon")) return;

        boolean cutscene = false;
        try {
            Data data = level.getServer().overworld().getDataStorage().computeIfAbsent(Data::load, Data::new, "sovereignrpgcore_cameo");
            if (data.first(key.toString())) {
                Vec3 center = boss.position().add(0.0, boss.getBbHeight() * 0.5, 0.0);
                boss.setInvulnerable(true);
                Class<?> handler = Class.forName("it.yuruni.dog.event.DragonDevourHandler");
                Method play = handler.getMethod("playDevourCutscene", ServerLevel.class, Vec3.class, boolean.class, UUID.class);
                play.invoke(null, level, center, true, boss.getUUID());
                cutscene = true;
            }
        } catch (Throwable t) {
            boss.setInvulnerable(false);
        }
        PENDING.add(new Pending(level, boss.position(), cutscene ? AFTER_CUTSCENE : NO_CUTSCENE));
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || PENDING.isEmpty()) return;
        Iterator<Pending> it = PENDING.iterator();
        while (it.hasNext()) {
            Pending p = it.next();
            if (--p.ticks > 0) continue;
            it.remove();
            try {
                dropFragment(p.level, p.pos);
            } catch (Throwable t) {
                // no fragment if DoG changes its classes
            }
        }
        if (HANDLED.size() > 64) HANDLED.clear();
    }

    /** Same floating fragment DoG gives over the End fountain, put down where the dragon fell. */
    private static void dropFragment(ServerLevel level, Vec3 at) throws Exception {
        EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(FRAGMENT_ENTITY);
        Item item = ForgeRegistries.ITEMS.getValue(FRAGMENT_ITEM);
        if (type == null || item == null) return;
        Entity fragment = type.create(level);
        if (fragment == null) return;
        int x = (int) Math.floor(at.x);
        int z = (int) Math.floor(at.z);
        double top = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
        if (top <= level.getMinBuildHeight() + 1) top = at.y;
        fragment.setPos(at.x, top + 14.0, at.z);
        fragment.getClass().getMethod("setHoverY", double.class).invoke(fragment, top + 2.0);
        fragment.getClass().getMethod("setReward", ItemStack.class).invoke(fragment, new ItemStack(item));
        level.addFreshEntity(fragment);
    }
}
