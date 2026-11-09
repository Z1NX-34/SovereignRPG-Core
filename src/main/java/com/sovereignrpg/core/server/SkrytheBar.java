package com.sovereignrpg.core.server;

import com.sovereignrpg.core.CoreConfig;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

/** Monster Expansion's Skrythe has no boss bar. This gives him one for every player within range. */
@Mod.EventBusSubscriber(modid = "sovereignrpgcore")
public final class SkrytheBar {
    private SkrytheBar() {}

    private static final ResourceLocation SKRYTHE = new ResourceLocation("monsterexpansion", "skrythe");
    private static final double RANGE_SQ = 64.0 * 64.0;
    private static final Map<LivingEntity, ServerBossEvent> BARS = new HashMap<>();

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent e) {
        Entity en = e.getEntity();
        if (e.getLevel().isClientSide() || !(en instanceof LivingEntity living) || BARS.containsKey(living)) return;
        if (!SKRYTHE.equals(ForgeRegistries.ENTITY_TYPES.getKey(en.getType()))) return;
        ServerBossEvent bar = new ServerBossEvent(
                Component.translatable("entity.monsterexpansion.skrythe").withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD),
                BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.PROGRESS);
        BARS.put(living, bar);
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || BARS.isEmpty()) return;
        var it = BARS.entrySet().iterator();
        while (it.hasNext()) {
            var entry = it.next();
            LivingEntity boss = entry.getKey();
            ServerBossEvent bar = entry.getValue();
            if (boss.isRemoved() || !CoreConfig.SKRYTHE_BAR.get()) {
                bar.removeAllPlayers();
                it.remove();
                continue;
            }
            if (!(boss.level() instanceof ServerLevel level)) continue;
            bar.setProgress(Math.max(0.0F, Math.min(1.0F, boss.getHealth() / boss.getMaxHealth())));
            for (ServerPlayer p : level.players()) {
                boolean near = p.distanceToSqr(boss) <= RANGE_SQ;
                if (near) bar.addPlayer(p);
                else bar.removePlayer(p);
            }
        }
    }
}
