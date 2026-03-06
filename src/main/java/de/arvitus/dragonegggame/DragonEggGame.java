package de.arvitus.dragonegggame;

import de.arvitus.dragonegggame.config.Config;
import de.arvitus.dragonegggame.features.Actions;
import dev.dragonslegacy.egg.event.EggEventHandler;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.fabricmc.loader.api.metadata.Person;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

public class DragonEggGame implements ModInitializer {
    public static final String MOD_ID = "dragonegggame";
    public static final String MOD_ID_ALIAS = "deg";
    public static final Path CONFIG_DIR = FabricLoader.getInstance().getConfigDir().resolve(MOD_ID);
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final boolean BLUEMAP_INSTALLED = FabricLoader.getInstance().isModLoaded("bluemap");
    public static Config CONFIG;
    @Nullable
    public static MinecraftServer server;

    public static void devLogger(String message, Object... args) {
        if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
            LOGGER.info("[DEV] " + message, args);
        }
    }

    @Override
    public void onInitialize() {
        FabricLoader.getInstance().getModContainer(MOD_ID).ifPresent(modContainer -> {
            ModMetadata meta = modContainer.getMetadata();
            LOGGER.info(
                "Loaded {} v{} by {}",
                meta.getName(),
                meta.getVersion(),
                meta.getAuthors().stream().findFirst().map(Person::getName).orElse("unknown")
            );
        });

        Placeholders.register();
        LootConditions.register();
        CONFIG = Config.loadAndUpdateOrCreate();
        Commands.register();
        Events.register();

        Actions.init();

        MCIntegration.init();
        if (BLUEMAP_INSTALLED) {
            LOGGER.info("BlueMap detected, enabling BlueMap integration");
            BlueMapIntegration.init();
        }

        // Register Dragon's Legacy Fabric event hooks
        EggEventHandler.register();
    }
}