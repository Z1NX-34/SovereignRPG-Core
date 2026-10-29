package com.sovereignrpg.core.client;

import java.nio.file.Path;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

/**
 * Registers the jar's "override" folder as an always-on resource pack on top of other mods' assets, so the difficulty
 * screen texture and text changes win over True Difficulty Scale's own files.
 */
@Mod.EventBusSubscriber(modid = "sovereignrpgcore", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class CoreClientPacks {
    private CoreClientPacks() {}

    @SubscribeEvent
    public static void onPackFinders(AddPackFindersEvent e) {
        if (e.getPackType() != PackType.CLIENT_RESOURCES) return;
        Path root = ModList.get().getModFileById("sovereignrpgcore").getFile().findResource("override");
        Pack pack = Pack.readMetaAndCreate("sovereignrpgcore_override", Component.literal("SovereignRPG Core UI"), true,
            id -> new PathPackResources(id, root, true), PackType.CLIENT_RESOURCES, Pack.Position.TOP, PackSource.BUILT_IN);
        if (pack != null) e.addRepositorySource(consumer -> consumer.accept(pack));
    }
}
