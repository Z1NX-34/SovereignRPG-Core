package com.sovereignrpg.core;

import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;

/** Modpack-specific additions. Classes and races ship as data; integrations with other mods live in client/ and server/. */
@Mod(SovereignRPGCore.MODID)
public class SovereignRPGCore {
    public static final String MODID = "sovereignrpgcore";

    public SovereignRPGCore() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, CoreConfig.SPEC);
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        com.sovereignrpg.core.regalia.ModItems.register(bus);
        if (ModList.get().isLoaded("curios")) com.sovereignrpg.core.regalia.RegaliaEvents.registerCurios();
        bus.addListener((FMLCommonSetupEvent e) -> e.enqueueWork(DifficultySync::apply));
        bus.addListener((ModConfigEvent.Reloading e) -> {
            if (e.getConfig().getSpec() == CoreConfig.SPEC) DifficultySync.apply();
        });
    }
}
