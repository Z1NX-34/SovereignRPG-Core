package com.sovereignrpg.core.forge;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** The Ruined Forge: a block with a two-tab screen (Charm / Discharm). */
public final class ModForge {
    private ModForge() {}

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, "sovereignrpgcore");
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, "sovereignrpgcore");

    public static final RegistryObject<Block> BLOCK = BLOCKS.register("ruined_forge", RuinedForgeBlock::new);
    public static final RegistryObject<MenuType<ForgeMenu>> MENU =
        MENUS.register("ruined_forge", () -> new MenuType<>(ForgeMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        MENUS.register(bus);
    }
}
