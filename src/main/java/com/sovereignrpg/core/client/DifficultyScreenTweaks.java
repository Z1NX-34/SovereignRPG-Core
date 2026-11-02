package com.sovereignrpg.core.client;

import com.mojang.blaze3d.systems.RenderSystem;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import com.sovereignrpg.core.CoreConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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
            if (b[1].equals(selected)) {
                // gold outline that follows the icon's own shape (pre-made image, transparent inside)
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                g.blit(new ResourceLocation("sovereignrpgcore", "textures/gui/sel_" + b[1] + ".png"), w.getX(), w.getY(), 0, 0, 100, 100, 100, 100);
                RenderSystem.disableBlend();
            }
            if (tip == null && w.isMouseOver(mx, my)) {
                tip = new ArrayList<>();
                tip.add(Component.translatable("sovereignrpgcore.difficulty." + b[1] + ".name").withStyle(st -> st.withBold(true)));
                tip.add(Component.translatable("sovereignrpgcore.difficulty." + b[1] + ".desc").withStyle(ChatFormatting.GRAY));
                tip.addAll(effects(b[1]));
                tip.add(Component.translatable("sovereignrpgcore.difficulty." + b[1] + ".tag").withStyle(tagColor(b[1])));
            }
        }
        if (selected != null && s instanceof AbstractContainerScreen<?> cs) {
            Component t = Component.translatable("sovereignrpgcore.difficulty.selected",
                Component.translatable("sovereignrpgcore.difficulty." + selected + ".name"));
            // fitted to the panel, so any font (Modern UI etc.) stays inside the frame
            int maxW = 330, w = font.width(t);
            float sc = w > maxW ? (float) maxW / w : 1f;
            g.pose().pushPose();
            g.pose().translate(cs.getGuiLeft(), cs.getGuiTop() + 31, 0);
            g.pose().scale(sc, sc, 1f);
            g.drawString(font, t, -w / 2, 0, 0xFFFFD84A, true);
            g.pose().popPose();
        }
        if (tip != null) g.renderComponentTooltip(font, tip, mx, my);
    }

    private static ChatFormatting tagColor(String k) {
        return switch (k) {
            case "casual" -> ChatFormatting.GREEN;
            case "normal" -> ChatFormatting.AQUA;
            case "expert" -> ChatFormatting.GOLD;
            default -> ChatFormatting.DARK_RED;
        };
    }

    private static String num(double v) {
        return Math.abs(v - Math.rint(v)) < 0.001 ? String.valueOf((long) Math.rint(v)) : String.valueOf(Math.round(v * 10) / 10.0);
    }

    private static int pct(double v) { return (int) Math.round(Math.abs(v) * 100); }

    /** The real numbers, read from Core's config, so the text can never disagree with what the difficulty does. */
    private static List<Component> effects(String k) {
        List<Component> l = new ArrayList<>();
        CoreConfig.Mode m = CoreConfig.mode(k);
        if (m == null) {
            l.add(Component.literal("Vanilla damage both ways.").withStyle(ChatFormatting.GREEN));
            l.add(Component.literal("No bonuses, no penalties.").withStyle(ChatFormatting.GREEN));
            return l;
        }
        double t = m.taken.get(), d = m.dealt.get();
        if (t < 1) l.add(Component.literal("You take " + pct(1 - t) + "% less damage.").withStyle(ChatFormatting.GREEN));
        else if (t > 1) l.add(Component.literal("You take " + num(t) + "x damage.").withStyle(ChatFormatting.RED));
        if (d < 1) l.add(Component.literal("You deal " + pct(1 - d) + "% less damage.").withStyle(ChatFormatting.RED));
        else if (d > 1) l.add(Component.literal("You deal " + pct(d - 1) + "% more damage.").withStyle(ChatFormatting.GREEN));
        mob(l, "health", m.mobHealth.get());
        mob(l, "damage", m.mobDamage.get());
        mob(l, "speed", m.mobSpeed.get());
        double a = m.mobArmor.get();
        if (a != 0) l.add(Component.literal("Enemies have " + (a > 0 ? "+" : "-") + num(Math.abs(a)) + " armor.").withStyle(a > 0 ? ChatFormatting.RED : ChatFormatting.GREEN));
        double x = m.mobExp.get();
        if (x != 1) l.add(Component.literal("Enemies drop " + pct(x) + "% XP.").withStyle(x > 1 ? ChatFormatting.GREEN : ChatFormatting.RED));
        return l;
    }

    private static void mob(List<Component> l, String what, double v) {
        if (v == 0) return;
        l.add(Component.literal("Enemies have " + (v > 0 ? "+" : "-") + pct(v) + "% " + what + ".").withStyle(v > 0 ? ChatFormatting.RED : ChatFormatting.GREEN));
    }
}
