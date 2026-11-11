package com.sovereignrpg.core.server;

import com.sovereignrpg.core.CoreConfig;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Locale;
import java.util.stream.Stream;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Remnants Minimap keeps map data and markers per save-folder name, so a new world named like an old one inherits the
 * old beacons. When a single-player world has never been played (no player data yet), the old data for that folder name
 * is deleted before the map mod opens it.
 */
@Mod.EventBusSubscriber(modid = "sovereignrpgcore")
public final class NewWorldWipe {
    private NewWorldWipe() {}

    private static final Logger LOG = LogManager.getLogger("SovereignCore/NewWorldWipe");

    @SubscribeEvent
    public static void onAboutToStart(ServerAboutToStartEvent e) {
        if (!CoreConfig.WIPE_NEW_WORLD.get() || !ModList.get().isLoaded("remnantminimaps")) return;
        MinecraftServer server = e.getServer();
        if (server.isDedicatedServer()) return;
        try {
            Path world = server.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize();
            Path nameP = world.getFileName();
            if (nameP == null) return;
            Path players = server.getWorldPath(LevelResource.PLAYER_DATA_DIR);
            if (Files.isDirectory(players)) {
                try (Stream<Path> s = Files.list(players)) {
                    if (s.findAny().isPresent()) return; // world has been played before
                }
            }
            String id = nameP.toString().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9._-]", "_");
            if (id.isEmpty()) id = "world";
            Path dir = FMLPaths.GAMEDIR.get().resolve("remnantminimaps").resolve("worlds").resolve("sp_" + id).normalize();
            Path base = FMLPaths.GAMEDIR.get().resolve("remnantminimaps").resolve("worlds").normalize();
            if (!dir.startsWith(base) || !Files.isDirectory(dir)) return;
            try (Stream<Path> walk = Files.walk(dir)) {
                walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                    try {
                        Files.delete(p);
                    } catch (IOException ex) {
                        LOG.warn("Could not delete {}: {}", p, ex.toString());
                    }
                });
            }
            LOG.info("New world '{}': cleared old Remnants Minimap data", nameP);
        } catch (Exception ex) {
            LOG.warn("New-world map cleanup skipped: {}", ex.toString());
        }
    }
}
