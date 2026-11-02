package com.sovereignrpg.core.regalia;

import com.google.gson.JsonObject;
import java.util.function.Function;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.level.Level;

/** Smithing table recipes: Ascension puts a regalia into a Vessel, Desecration clears them. */
public final class RegaliaRecipes {
    private RegaliaRecipes() {}

    /** Can this regalia be applied to this item right now? */
    public static boolean canApply(ItemStack base, RegaliaItem ri) {
        if (base.isEmpty()) return false;
        switch (ri.kind) {
            case PERSEVERANCE:
                return base.isDamageableItem() && !(base.getTag() != null && base.getTag().getBoolean("Unbreakable"));
            case ADDITION:
                return RegaliaData.socketable(base) && RegaliaData.vessels(base) < RegaliaData.MAX_VESSELS;
            default:
                if (RegaliaData.entries(base).size() >= RegaliaData.vessels(base)) return false;
                if (RegaliaData.has(base, ri.regalia.id)) return false;
                return ri.regalia.jobAny(RegaliaData.cats(base, null)) != null;
        }
    }

    abstract static class Base implements SmithingRecipe {
        final ResourceLocation id;
        Base(ResourceLocation id) { this.id = id; }
        @Override public boolean canCraftInDimensions(int w, int h) { return w >= 3 && h >= 1; }
        @Override public ItemStack getResultItem(RegistryAccess r) { return ItemStack.EMPTY; }
        @Override public ResourceLocation getId() { return id; }
        @Override public RecipeType<?> getType() { return RecipeType.SMITHING; }
        @Override public boolean isSpecial() { return true; }
    }

    public static final class Socket extends Base {
        public Socket(ResourceLocation id) { super(id); }
        @Override public boolean isTemplateIngredient(ItemStack s) { return s.is(ModItems.TEMPLATE_ASCENSION.get()); }
        @Override public boolean isBaseIngredient(ItemStack s) { return RegaliaData.socketable(s) || s.isDamageableItem(); }
        @Override public boolean isAdditionIngredient(ItemStack s) { return s.getItem() instanceof RegaliaItem; }

        @Override public boolean matches(Container c, Level l) {
            return isTemplateIngredient(c.getItem(0)) && c.getItem(2).getItem() instanceof RegaliaItem ri && canApply(c.getItem(1), ri);
        }

        @Override public ItemStack assemble(Container c, RegistryAccess r) {
            ItemStack out = c.getItem(1).copy();
            out.setCount(1);
            RegaliaItem ri = (RegaliaItem) c.getItem(2).getItem();
            switch (ri.kind) {
                case PERSEVERANCE -> out.getOrCreateTag().putBoolean("Unbreakable", true);
                case ADDITION -> RegaliaData.setVessels(out, RegaliaData.MAX_VESSELS);
                default -> RegaliaData.add(out, ri.regalia.id, ri.tier);
            }
            return out;
        }

        @Override public RecipeSerializer<?> getSerializer() { return ModItems.SOCKET_SERIALIZER.get(); }
    }

    public static final class Desecrate extends Base {
        public Desecrate(ResourceLocation id) { super(id); }
        @Override public boolean isTemplateIngredient(ItemStack s) { return s.is(ModItems.TEMPLATE_DESECRATION.get()); }
        @Override public boolean isBaseIngredient(ItemStack s) { return !RegaliaData.entries(s).isEmpty(); }
        @Override public boolean isAdditionIngredient(ItemStack s) { return s.is(Items.AMETHYST_SHARD); }

        @Override public boolean matches(Container c, Level l) {
            return isTemplateIngredient(c.getItem(0)) && isBaseIngredient(c.getItem(1)) && isAdditionIngredient(c.getItem(2));
        }

        @Override public ItemStack assemble(Container c, RegistryAccess r) {
            ItemStack out = c.getItem(1).copy();
            out.setCount(1);
            RegaliaData.clear(out);
            return out;
        }

        @Override public RecipeSerializer<?> getSerializer() { return ModItems.DESECRATE_SERIALIZER.get(); }
    }

    public static final class Ser<T extends SmithingRecipe> implements RecipeSerializer<T> {
        private final Function<ResourceLocation, T> make;
        public Ser(Function<ResourceLocation, T> make) { this.make = make; }
        @Override public T fromJson(ResourceLocation id, JsonObject j) { return make.apply(id); }
        @Override public T fromNetwork(ResourceLocation id, FriendlyByteBuf b) { return make.apply(id); }
        @Override public void toNetwork(FriendlyByteBuf b, T r) {}
    }
}
