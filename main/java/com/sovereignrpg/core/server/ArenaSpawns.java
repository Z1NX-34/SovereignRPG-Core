package com.sovereignrpg.core.server;

import com.sovereignrpg.core.CoreConfig;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * The arena dimension is meant to be empty until a boss is summoned. Cancels the spawn types that create mobs on their
 * own (natural, chunk generation, patrols, reinforcements) in that dimension only. Eggs, commands, spawners, structures
 * and mods that call addFreshEntity directly are untouched.
 */
@Mod.EventBusSubscriber(modid = "sovereignrpgcore")
public final class ArenaSpawns {
    private ArenaSpawns() {}

    @SubscribeEvent
    public static void onSpawn(MobSpawnEvent.FinalizeSpawn e) {
        if (!CoreConfig.ARENA_NO_SPAWNS.get()) return;
        MobSpawnType t = e.getSpawnType();
        if (t != MobSpawnType.NATURAL && t != MobSpawnType.CHUNK_GENERATION && t != MobSpawnType.PATROL && t != MobSpawnType.REINFORCEMENT) return;
        if (e.getLevel().getLevel().dimension().location().toString().equals(CoreConfig.ARENA_DIMENSION.get())) {
            e.setSpawnCancelled(true);
        }
    }
}
