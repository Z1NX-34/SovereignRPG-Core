package com.sovereignrpg.core.forge;

import com.sovereignrpg.core.regalia.ModItems;
import com.sovereignrpg.core.regalia.RegaliaData;
import com.sovereignrpg.core.regalia.RegaliaItem;
import com.sovereignrpg.core.regalia.RegaliaRecipes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Tab 0 (Charm): template of Ascension + item + regalia -> the item with the regalia socketed.
 * Tab 1 (Discharm): template of Desecration + item + Empty Regalia -> the removed regalia AND the item.
 * Slots 0-2 inputs, 3 = Charm result, 4 = Discharm regalia, 5 = Discharm item, 6-41 player inventory.
 */
public class ForgeMenu extends AbstractContainerMenu {
    private static final RegaliaRecipes.Socket SOCKET = new RegaliaRecipes.Socket(new ResourceLocation("sovereignrpgcore", "forge"));

    private final ContainerLevelAccess access;
    private final DataSlot tab = DataSlot.standalone();
    private final Container in = new SimpleContainer(3) {
        @Override public void setChanged() { super.setChanged(); slotsChanged(this); }
    };
    private final Container out = new SimpleContainer(3);

    public ForgeMenu(int id, Inventory inv) { this(id, inv, ContainerLevelAccess.NULL); }

    public ForgeMenu(int id, Inventory inv, ContainerLevelAccess access) {
        super(ModForge.MENU.get(), id);
        this.access = access;
        addDataSlot(tab);

        addSlot(new Slot(in, 0, 15, 63) {
            @Override public boolean mayPlace(ItemStack s) { return s.is(ModItems.TEMPLATE_ASCENSION.get()) || s.is(ModItems.TEMPLATE_DESECRATION.get()); }
        });
        addSlot(new Slot(in, 1, 39, 63));
        addSlot(new Slot(in, 2, 63, 63) {
            @Override public boolean mayPlace(ItemStack s) { return s.getItem() instanceof RegaliaItem; }
        });
        addSlot(new OutSlot(0, 133, 63));
        addSlot(new OutSlot(1, 129, 45));
        addSlot(new OutSlot(2, 129, 77));

        for (int r = 0; r < 3; r++) for (int c = 0; c < 9; c++) addSlot(new Slot(inv, 9 + r * 9 + c, 9 + c * 18, 111 + r * 18));
        for (int c = 0; c < 9; c++) addSlot(new Slot(inv, c, 9 + c * 18, 169));
    }

    public int tab() { return tab.get(); }

    private final class OutSlot extends Slot {
        private final int idx;
        OutSlot(int idx, int x, int y) { super(out, idx, x, y); this.idx = idx; }
        @Override public boolean mayPlace(ItemStack s) { return false; }
        @Override public boolean isActive() { return idx == 0 ? tab.get() == 0 : tab.get() == 1; }
        @Override public void onTake(Player p, ItemStack s) { craft(p, idx); }
    }

    private void craft(Player p, int taken) {
        ItemStack other = ItemStack.EMPTY;
        if (tab.get() == 1) other = out.getItem(taken == 1 ? 2 : 1).copy();
        for (int i = 0; i < 3; i++) in.getItem(i).shrink(1);
        for (int i = 0; i < 3; i++) out.setItem(i, ItemStack.EMPTY);
        update();
        if (!other.isEmpty() && !p.level().isClientSide && !p.getInventory().add(other)) p.drop(other, false);
    }

    private void update() {
        for (int i = 0; i < 3; i++) out.setItem(i, ItemStack.EMPTY);
        ItemStack t = in.getItem(0), b = in.getItem(1), a = in.getItem(2);
        if (tab.get() == 0) {
            if (SOCKET.matches(in, null)) out.setItem(0, SOCKET.assemble(in, null));
        } else if (t.is(ModItems.TEMPLATE_DESECRATION.get()) && !b.isEmpty() && a.getItem() instanceof RegaliaItem ri
                && ri.kind == RegaliaItem.Kind.EMPTY && !RegaliaData.entries(b).isEmpty()) {
            ItemStack item = b.copy();
            item.setCount(1);
            RegaliaData.Entry gone = RegaliaData.removeLast(item);
            Item orb = gone == null ? null : ModItems.orb(gone.id(), gone.tier());
            if (orb != null) {
                out.setItem(1, new ItemStack(orb));
                out.setItem(2, item);
            }
        }
        broadcastChanges();
    }

    @Override public void slotsChanged(Container c) { if (c == in) update(); }

    @Override
    public boolean clickMenuButton(Player p, int id) {
        if (id != 0 && id != 1) return false;
        tab.set(id);
        update();
        return true;
    }

    @Override
    public void removed(Player p) {
        super.removed(p);
        access.execute((l, pos) -> clearContainer(p, in));
    }

    @Override
    public boolean stillValid(Player p) {
        return access == ContainerLevelAccess.NULL || access.evaluate((l, pos) -> l.getBlockState(pos).is(ModForge.BLOCK.get()) && p.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0, true);
    }

    @Override
    public ItemStack quickMoveStack(Player p, int i) {
        Slot slot = slots.get(i);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;
        ItemStack st = slot.getItem();
        ItemStack copy = st.copy();
        if (i >= 3 && i <= 5) {
            if (!slot.isActive()) return ItemStack.EMPTY;
            if (!moveItemStackTo(st, 6, 42, true)) return ItemStack.EMPTY;
        } else if (i < 3) {
            if (!moveItemStackTo(st, 6, 42, false)) return ItemStack.EMPTY;
        } else {
            if (!moveItemStackTo(st, 0, 3, false)) {
                if (i < 33) { if (!moveItemStackTo(st, 33, 42, false)) return ItemStack.EMPTY; }
                else if (!moveItemStackTo(st, 6, 33, false)) return ItemStack.EMPTY;
            }
        }
        if (st.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        if (st.getCount() == copy.getCount()) return ItemStack.EMPTY;
        slot.onTake(p, st);
        return copy;
    }
}
