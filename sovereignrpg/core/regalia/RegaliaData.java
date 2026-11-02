package com.sovereignrpg.core.regalia;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

/** Reading and writing what an item has socketed, and working out what kind of item it is. */
public final class RegaliaData {
    private RegaliaData() {}

    public static final String LIST = "SovRegalia", VESSELS = "SovVessels";
    public static final int MAX_VESSELS = 2;

    public record Entry(String id, int tier) {}

    public static List<Entry> entries(ItemStack s) {
        List<Entry> out = new ArrayList<>();
        CompoundTag t = s.getTag();
        if (t == null || !t.contains(LIST, Tag.TAG_LIST)) return out;
        ListTag l = t.getList(LIST, Tag.TAG_COMPOUND);
        for (int i = 0; i < l.size(); i++) out.add(new Entry(l.getCompound(i).getString("id"), l.getCompound(i).getInt("tier")));
        return out;
    }

    public static int vessels(ItemStack s) {
        CompoundTag t = s.getTag();
        return t != null && t.contains(VESSELS) ? Math.max(1, Math.min(MAX_VESSELS, t.getInt(VESSELS))) : 1;
    }

    public static void add(ItemStack s, String id, int tier) {
        CompoundTag t = s.getOrCreateTag();
        ListTag l = t.getList(LIST, Tag.TAG_COMPOUND);
        CompoundTag c = new CompoundTag();
        c.putString("id", id);
        c.putInt("tier", tier);
        l.add(c);
        t.put(LIST, l);
    }

    public static void clear(ItemStack s) {
        if (s.getTag() != null) s.getTag().remove(LIST);
    }

    public static void setVessels(ItemStack s, int n) { s.getOrCreateTag().putInt(VESSELS, n); }

    public static boolean has(ItemStack s, String id) {
        for (Entry e : entries(s)) if (e.id().equals(id)) return true;
        return false;
    }

    // ---- what kind of item is this ----

    private static ResourceLocation tagId(String path) { return new ResourceLocation("sovereignrpgcore", path); }

    private static boolean inTag(ItemStack s, ResourceLocation id) { return s.is(ItemTags.create(id)); }

    private static final String[] CURIO_SLOTS = {"ring", "necklace", "charm", "spellstone", "spellbook", "belt", "bracelet", "hands", "head", "body", "back", "curio"};

    private static Cat curioCat(String slot) {
        return switch (slot) {
            case "ring" -> Cat.RING;
            case "necklace" -> Cat.NECKLACE;
            case "charm" -> Cat.CHARM;
            case "spellstone" -> Cat.SPELLSTONE;
            case "spellbook" -> Cat.SPELLBOOK;
            default -> null;
        };
    }

    /** Categories of an item. curioSlot is the Curios slot it sits in, or null when unknown (inventory, tooltip, recipe). */
    public static Set<Cat> cats(ItemStack s, String curioSlot) {
        Set<Cat> out = EnumSet.noneOf(Cat.class);
        if (s.isEmpty()) return out;
        Item it = s.getItem();
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(it);
        String path = key == null ? "" : key.getPath();
        boolean irons = key != null && key.getNamespace().equals("irons_spellbooks");

        if (it instanceof ArmorItem a) {
            out.add(Cat.ARMOR);
            EquipmentSlot sl = a.getEquipmentSlot();
            if (sl == EquipmentSlot.HEAD) out.add(Cat.HELMET);
            else if (sl == EquipmentSlot.CHEST) out.add(Cat.CHEST);
            else if (sl == EquipmentSlot.LEGS || sl == EquipmentSlot.FEET) out.add(Cat.LOWER);
        }

        // curios: by the slot it is in, or by its curios item tags
        if (curioSlot != null) {
            out.add(Cat.CURIOS);
            Cat c = curioCat(curioSlot);
            if (c != null) out.add(c);
        } else {
            for (String slot : CURIO_SLOTS) {
                if (inTag(s, new ResourceLocation("curios", slot))) {
                    out.add(Cat.CURIOS);
                    Cat c = curioCat(slot);
                    if (c != null) out.add(c);
                }
            }
        }
        if (irons && path.contains("spell_book") || inTag(s, tagId("spellbooks"))) out.add(Cat.SPELLBOOK);

        // weapons
        if (it instanceof CrossbowItem) {
            out.add(Cat.HEAVY_RANGED); out.add(Cat.RANGED); out.add(Cat.WEAPON);
        } else if (it instanceof BowItem) {
            out.add(inTag(s, tagId("heavy_ranged")) ? Cat.HEAVY_RANGED : Cat.LIGHT_RANGED);
            out.add(Cat.RANGED); out.add(Cat.WEAPON);
        } else if (inTag(s, tagId("wands")) || irons && (path.contains("staff") || path.contains("wand"))) {
            out.add(Cat.WAND); out.add(Cat.WEAPON);
        } else if (!out.contains(Cat.ARMOR) && !out.contains(Cat.CURIOS)) {
            double dmg = 0, spd = 0;
            for (AttributeModifier m : it.getDefaultAttributeModifiers(EquipmentSlot.MAINHAND).get(Attributes.ATTACK_DAMAGE)) dmg += m.getAmount();
            for (AttributeModifier m : it.getDefaultAttributeModifiers(EquipmentSlot.MAINHAND).get(Attributes.ATTACK_SPEED)) spd += m.getAmount();
            if (dmg > 0) {
                out.add(Cat.MELEE); out.add(Cat.WEAPON);
                out.add(inTag(s, tagId("heavy_melee")) || spd <= -3.0 ? Cat.HEAVY_MELEE : Cat.LIGHT_MELEE);
            }
        }
        return out;
    }

    public static boolean socketable(ItemStack s) {
        if (s.isEmpty()) return false;
        Set<Cat> c = cats(s, null);
        for (Regalia r : Regalia.ALL) if (r.jobAny(c) != null) return true;
        return false;
    }

    public static Attribute attr(String ns, String path) {
        return ForgeRegistries.ATTRIBUTES.getValue(new ResourceLocation(ns, path));
    }
}
