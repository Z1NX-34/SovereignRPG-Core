package com.sovereignrpg.core.client;

import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

/**
 * True Difficulty Scale prints fixed English texts ("Casual Mode has been activated!") from code, so a language file
 * can't rename them. This swaps the old names for the new ones in the chat line on the client.
 */
@Mod.EventBusSubscriber(modid = "sovereignrpgcore", value = Dist.CLIENT)
public final class DifficultyChat {
    private DifficultyChat() {}

    @SubscribeEvent
    public static void onChat(ClientChatReceivedEvent e) {
        if (!ModList.get().isLoaded("truedifficultyscale")) return;
        // the mod writes colour codes (§d...) into the text itself, so strip them before comparing
        String t = net.minecraft.ChatFormatting.stripFormatting(e.getMessage().getString());
        String name = null;
        String color = null;
        if (t.equals("Casual Mode has been activated!")) { name = "Novice"; color = "§a"; }
        else if (t.equals("Normal Mode has been activated!")) { name = "Adept"; color = "§f"; }
        else if (t.equals("Expert Mode has been activated!")) { name = "Champion"; color = "§d"; }
        else if (t.equals("Malicious Mode has been activated!")) { name = "Sovereign"; color = "§4"; }
        if (name != null) e.setMessage(Component.literal(color + name + " difficulty has been activated!"));
    }
}
