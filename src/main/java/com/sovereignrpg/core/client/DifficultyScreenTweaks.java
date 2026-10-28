package com.sovereignrpg.core.client;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Optional integration with True Difficulty Scale. Nothing here references that mod's classes at compile time:
 * the screen is recognised by class name and its buttons are read by field name, so without the mod this does nothing.
 * Adds a description tooltip on hover and a highlight plus "Selected" text for the chosen difficulty.
 */
@Mod.EventBusSubscriber(modid = "sovereignrpgcore", value = Dist.CLIENT)
public final class DifficultyScreenTweaks {
    private DifficultyScreenTweaks() {}

    private static final String SCREEN = "net.somekoifee.truedifficultyscale.client.gui.DifficultySelectmenuScreen";
    /** button field in the TDS screen -> our lang key stem */
    private static final String[][] BUTTONS = {
        {"imagebutton_casual", "casual"},
        {"imagebutton_normal", "normal"},
        {"imagebutton_expert", "expert"},
        {"imagebutton_malice", "malice"},
    };
    private static String selected = null;

    private static boolean isTds(Screen s) { return s != null && s.getClass().getName().equals(SCREEN); }

    private static AbstractWidget widget(Screen s, String field) {
        try {
            Field f = s.getClass().getDeclaredField(field);
            f.setAccessible(true);
            Object o = f.get(s);
            return o instanceof AbstractWidget w ? w : null;
        } catch (Exception e) {
            return null;
        }
    }

    @SubscribeEvent
    public static void onInit(ScreenEvent.Init.Post e) {
        if (isTds(e.getScreen())) selected = null;
    }

    @SubscribeEvent
    public static void onClick(ScreenEvent.MouseButtonPressed.Pre e) {
        Screen s = e.getScreen();
        if (!isTds(s) || e.getButton() != 0) return;
        for (String[] b : BUTTONS) {
            AbstractWidget w = widget(s, b[0]);
            if (w != null && w.visible && w.active && w.isMouseOver(e.getMouseX(), e.getMouseY())) selected = b[1];
        }
    }

    @SubscribeEvent
    public static void onRender(ScreenEvent.Render.Post e) {
        Screen s = e.getScreen();
        if (!isTds(s)) return;
        GuiGraphics g = e.getGuiGraphics();
        var font = Minecraft.getInstance().font;
        int mx = e.getMouseX(), my = e.getMouseY();
        List<Component> tip = null;
        for (String[] b : BUTTONS) {
            AbstractWidget w = widget(s, b[0]);
            if (w == null || !w.visible) continue;
            if (b[1].equals(selected)) g.renderOutline(w.getX() - 2, w.getY() - 2, w.getWidth() + 4, w.getHeight() + 4, 0xFFFFD84A);
            if (tip == null && w.isMouseOver(mx, my)) {
                tip = new ArrayList<>();
                tip.add(Component.translatable("sovereignrpgcore.difficulty." + b[1] + ".name").withStyle(st -> st.withBold(true)));
                String desc = Component.translatable("sovereignrpgcore.difficulty." + b[1] + ".desc").getString();
                for (String line : desc.split("\\|")) tip.add(Component.literal(line.trim()));
            }
        }
        if (selected != null && s instanceof AbstractContainerScreen<?> cs) {
            Component t = Component.translatable("sovereignrpgcore.difficulty.selected",
                Component.translatable("sovereignrpgcore.difficulty." + selected + ".name"));
            g.drawCenteredString(font, t, cs.getGuiLeft(), cs.getGuiTop() + 66, 0xFFFFD84A);
        }
        if (tip != null) g.renderComponentTooltip(font, tip, mx, my);
    }
}
