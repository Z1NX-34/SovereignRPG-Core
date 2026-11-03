package com.sovereignrpg.core.regalia;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/** One orb. Kinds: a normal regalia at a tier, Perseverance (makes an item unbreakable) and Addition (adds a Vessel). */
public class RegaliaItem extends Item {
    public static final String[] TIER_NAMES = {"Lesser", "Greater", "Eternal"};
    public static final int[] TIER_COLORS = {0xFFFFFF, 0xFF55FF, 0xFFAA00};
    public static final int PERSEVERANCE_COLOR = 0xFFC832, ADDITION_COLOR = 0xCD7F32;

    public enum Kind { NORMAL, PERSEVERANCE, ADDITION, EMPTY }

    public final Kind kind;
    public final Regalia regalia;
    public final int tier;

    public RegaliaItem(Regalia r, int tier) {
        super(new Item.Properties().stacksTo(16));
        this.kind = Kind.NORMAL; this.regalia = r; this.tier = tier;
    }

    public RegaliaItem(Kind k) {
        super(new Item.Properties().stacksTo(16));
        this.kind = k; this.regalia = null; this.tier = 0;
    }

    private static Component col(String s, int rgb) {
        return Component.literal(s).withStyle(st -> st.withColor(TextColor.fromRgb(rgb)));
    }

    @Override
    public Component getName(ItemStack s) {
        return switch (kind) {
            case NORMAL -> col("Regalia of " + regalia.name, regalia.color);
            case PERSEVERANCE -> col("Regalia of Perseverance", PERSEVERANCE_COLOR);
            case ADDITION -> col("Regalia of Addition", ADDITION_COLOR);
            case EMPTY -> col("Empty Regalia", 0xA0A0B0);
        };
    }

    @Override
    public void appendHoverText(ItemStack s, @Nullable Level lvl, List<Component> tip, TooltipFlag f) {
        switch (kind) {
            case NORMAL -> {
                tip.add(col(TIER_NAMES[tier - 1], TIER_COLORS[tier - 1]));
                tip.add(Component.empty());
                tip.add(col("When socketed in:", 0x5555FF));
                for (Regalia.Job j : regalia.jobs) tip.add(col(j.cat().label + ": " + j.text(tier), 0x55FF55));
                tip.add(Component.empty());
                tip.add(col("Smithing table with a Template of Ascension", 0x555555));
            }
            case PERSEVERANCE -> {
                tip.add(col("Eternal", TIER_COLORS[2]));
                tip.add(Component.empty());
                tip.add(col("Apply to any item with durability:", 0x5555FF));
                tip.add(col("It becomes unbreakable. Uses no Vessel.", 0x55FF55));
            }
            case EMPTY -> {
                tip.add(col("Smithing table with a Template of Desecration", 0x555555));
                tip.add(Component.empty());
                tip.add(col("Pulls the last socketed regalia out of an item", 0x5555FF));
                tip.add(col("and becomes that regalia.", 0x55FF55));
            }
            case ADDITION -> {
                tip.add(col("Eternal", TIER_COLORS[2]));
                tip.add(Component.empty());
                tip.add(col("Apply to any socketable item:", 0x5555FF));
                tip.add(col("+1 Vessel (max " + RegaliaData.MAX_VESSELS + " per item)", 0x55FF55));
            }
        }
    }

    @Override
    public boolean isFoil(ItemStack s) { return tier == 3 || kind == Kind.PERSEVERANCE || kind == Kind.ADDITION; }
}
