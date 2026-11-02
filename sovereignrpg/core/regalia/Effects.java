package com.sovereignrpg.core.regalia;

import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;

/** Adds up what a player's gear gives for one effect. */
public final class Effects {
    private Effects() {}

    public static final int HAND = 0, ARMOR = 1, CURIO = 2;

    /** Value of one stack for one effect, only counting jobs in the given group. */
    public static double of(ItemStack s, String curioSlot, int group, Eff eff, Predicate<Cat> catOk) {
        if (s.isEmpty()) return 0;
        var list = RegaliaData.entries(s);
        if (list.isEmpty()) return 0;
        Set<Cat> cats = RegaliaData.cats(s, curioSlot);
        double sum = 0;
        for (RegaliaData.Entry e : list) {
            Regalia r = Regalia.byId(e.id());
            if (r == null) continue;
            Regalia.Job j = r.jobFor(cats, group);
            if (j != null && j.eff() == eff && catOk.test(j.cat())) sum += j.value(e.tier());
        }
        return sum;
    }

    /** Sum over main hand, armor and curios (any of the groups in mask: 1<<HAND | 1<<ARMOR | 1<<CURIO). Percentages come back as fractions. */
    public static double sum(Player p, Eff eff, int mask, Predicate<Cat> catOk) {
        double t = 0;
        if ((mask & (1 << HAND)) != 0) t += of(p.getMainHandItem(), null, HAND, eff, catOk);
        if ((mask & (1 << ARMOR)) != 0) for (ItemStack a : p.getInventory().armor) t += of(a, null, ARMOR, eff, catOk);
        if ((mask & (1 << CURIO)) != 0 && ModList.get().isLoaded("curios")) t += CuriosCompat.sum(p, eff, catOk);
        return eff.flat ? t : t / 100.0;
    }

    public static double sum(Player p, Eff eff, int mask) { return sum(p, eff, mask, c -> true); }

    public static int bit(int g) { return 1 << g; }
}
