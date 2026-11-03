package com.sovereignrpg.core.regalia;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SmithingTemplateItem;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    private ModItems() {}

    private static final String MOD = "sovereignrpgcore";
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MOD);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MOD);
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, MOD);

    /** every orb, in creative-tab order */
    public static final List<RegistryObject<Item>> ORBS = new ArrayList<>();

    private static final String[] TIER_IDS = {"lesser", "greater", "eternal"};

    public static final RegistryObject<Item> PERSEVERANCE = ITEMS.register("regalia_perseverance", () -> new RegaliaItem(RegaliaItem.Kind.PERSEVERANCE));
    public static final RegistryObject<Item> EMPTY = ITEMS.register("regalia_empty", () -> new RegaliaItem(RegaliaItem.Kind.EMPTY));
    public static final RegistryObject<Item> FORGE_ITEM = ITEMS.register("ruined_forge",
        () -> new net.minecraft.world.item.BlockItem(com.sovereignrpg.core.forge.ModForge.BLOCK.get(), new Item.Properties()));
    public static final RegistryObject<Item> ADDITION = ITEMS.register("regalia_addition", () -> new RegaliaItem(RegaliaItem.Kind.ADDITION));

    private static Component gold(String s) {
        return Component.literal(s).withStyle(st -> st.withColor(TextColor.fromRgb(0xC9B37A)));
    }

    private static List<ResourceLocation> icons(String... p) {
        List<ResourceLocation> l = new ArrayList<>();
        for (String s : p) l.add(new ResourceLocation("minecraft", s));
        return l;
    }

    public static final RegistryObject<Item> TEMPLATE_ASCENSION = ITEMS.register("template_ascension", () -> new SmithingTemplateItem(
        gold("Weapons, armor and curios"), gold("Regalia"), gold("Template of Ascension"),
        gold("Add a weapon, armor piece or curio with a free Vessel"), gold("Add a regalia"),
        icons("item/empty_armor_slot_chestplate", "item/empty_slot_sword"), icons("item/empty_slot_diamond")));

    public static final RegistryObject<Item> TEMPLATE_DESECRATION = ITEMS.register("template_desecration", () -> new SmithingTemplateItem(
        gold("Socketed weapons, armor and curios"), gold("Empty Regalia"), gold("Template of Desecration"),
        gold("Add an item with regalia in its Vessels"), gold("Add an Empty Regalia"),
        icons("item/empty_armor_slot_chestplate", "item/empty_slot_sword"), icons("item/empty_slot_diamond")));

    public static final RegistryObject<RecipeSerializer<?>> SOCKET_SERIALIZER =
        SERIALIZERS.register("regalia_socket", () -> new RegaliaRecipes.Ser<>(RegaliaRecipes.Socket::new));
    public static final RegistryObject<RecipeSerializer<?>> DESECRATE_SERIALIZER =
        SERIALIZERS.register("regalia_desecrate", () -> new RegaliaRecipes.Ser<>(RegaliaRecipes.Desecrate::new));

    static {
        for (Regalia r : Regalia.ALL) {
            for (int t = 1; t <= 3; t++) {
                final int tier = t;
                ORBS.add(ITEMS.register("regalia_" + r.id + "_" + TIER_IDS[t - 1], () -> new RegaliaItem(r, tier)));
            }
        }
        ORBS.add(EMPTY);
        ORBS.add(PERSEVERANCE);
        ORBS.add(ADDITION);
    }

    public static final RegistryObject<CreativeModeTab> TAB = TABS.register("regalia", () -> CreativeModeTab.builder()
        .title(Component.literal("Sovereign Regalia"))
        .icon(() -> new ItemStack(ORBS.get(2).get()))
        .displayItems((p, out) -> {
            out.accept(FORGE_ITEM.get());
            out.accept(TEMPLATE_ASCENSION.get());
            out.accept(TEMPLATE_DESECRATION.get());
            for (RegistryObject<Item> o : ORBS) out.accept(o.get());
        }).build());

    /** The orb for a regalia id and tier, or null. */
    public static Item orb(String id, int tier) {
        for (RegistryObject<Item> o : ORBS) {
            if (o.get() instanceof RegaliaItem ri && ri.kind == RegaliaItem.Kind.NORMAL && ri.regalia.id.equals(id) && ri.tier == tier) return ri;
        }
        return null;
    }

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
        TABS.register(bus);
        SERIALIZERS.register(bus);
        com.sovereignrpg.core.forge.ModForge.register(bus);
    }
}
