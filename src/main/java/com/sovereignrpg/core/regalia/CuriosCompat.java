package com.sovereignrpg.core.regalia;

import java.util.function.Predicate;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;

/** Everything that touches the Curios API lives here so the rest of Core loads without it. */
final class CuriosCompat {
    private CuriosCompat() {}

    static double sum(Player p, Eff eff, Predicate<Cat> catOk) {
        double[] t = {0};
        CuriosApi.getCuriosInventory(p).ifPresent(h -> h.getCurios().forEach((id, sh) -> {
            var st = sh.getStacks();
            for (int i = 0; i < st.getSlots(); i++) {
                ItemStack s = st.getStackInSlot(i);
                t[0] += Effects.of(s, id, Effects.CURIO, eff, catOk);
            }
        }));
        return t[0];
    }
}
