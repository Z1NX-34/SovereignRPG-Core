package com.sovereignrpg.core.server;

import com.sovereignrpg.core.CoreConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import top.theillusivec4.curios.api.CuriosApi;

/**
 * Enigmatic Legacy gives every player the Ring of the Seven Curses at spawn. This keeps it for exactly one class
 * (config: cursed_ring.class), always worn, and removes it from everyone who picked another class.
 * Players who have not picked a class yet are left alone until they do.
 */
@Mod.EventBusSubscriber(modid = "sovereignrpgcore")
public final class RingRule {
    private RingRule() {}

    private static final ResourceLocation RING = new ResourceLocation("enigmaticlegacy", "cursed_ring");

    @SubscribeEvent
    public static void onTick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || e.player.level().isClientSide) return;
        if (e.player.tickCount % 40 != 0) return;
        if (!CoreConfig.RING_RESTRICT.get()) return;
        if (!ModList.get().isLoaded("enigmaticlegacy") || !ModList.get().isLoaded("curios")) return;
        Item ring = ForgeRegistries.ITEMS.getValue(RING);
        if (ring == null || ring == net.minecraft.world.item.Items.AIR) return;

        Player p = e.player;
        String cls = p.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG).getString("sovereign_class");
        if (cls.isEmpty()) return;
        if (isAllowed(cls)) ensureWorn(p, ring); else strip(p, ring);
    }

    private static boolean isAllowed(String cls) {
        String want = CoreConfig.RING_CLASS.get().trim();
        String a = cls.contains(":") ? cls.substring(cls.indexOf(':') + 1) : cls;
        String b = want.contains(":") ? want.substring(want.indexOf(':') + 1) : want;
        return a.equalsIgnoreCase(b);
    }

    private static void strip(Player p, Item ring) {
        Inventory inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) if (inv.getItem(i).is(ring)) inv.setItem(i, ItemStack.EMPTY);
        CuriosApi.getCuriosInventory(p).ifPresent(h -> h.getCurios().values().forEach(sh -> {
            var st = sh.getStacks();
            for (int i = 0; i < st.getSlots(); i++) if (st.getStackInSlot(i).is(ring)) st.setStackInSlot(i, ItemStack.EMPTY);
        }));
    }

    private static void ensureWorn(Player p, Item ring) {
        CuriosApi.getCuriosInventory(p).ifPresent(h -> {
            if (h.findFirstCurio(ring).isPresent()) return;
            h.getStacksHandler("ring").ifPresent(sh -> {
                var st = sh.getStacks();
                for (int i = 0; i < st.getSlots(); i++) {
                    if (!st.getStackInSlot(i).isEmpty()) continue;
                    // use the ring already in the inventory if there is one, otherwise a fresh one
                    Inventory inv = p.getInventory();
                    ItemStack give = new ItemStack(ring);
                    for (int j = 0; j < inv.getContainerSize(); j++) {
                        if (inv.getItem(j).is(ring)) { give = inv.getItem(j).copy(); inv.setItem(j, ItemStack.EMPTY); break; }
                    }
                    st.setStackInSlot(i, give);
                    return;
                }
            });
        });
    }
}
