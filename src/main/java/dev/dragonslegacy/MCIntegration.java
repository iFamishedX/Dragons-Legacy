package dev.dragonslegacy;

import dev.dragonslegacy.api.DragonEggAPI;
import dev.dragonslegacy.config.Data;
import eu.pb4.placeholders.api.PlaceholderContext;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

import java.util.Optional;
import java.util.UUID;

import static dev.dragonslegacy.DragonsLegacyMod.CONFIG;

public class MCIntegration {
    private static UUID BEARER;

    public static void init() {
        DragonEggAPI.onUpdate(MCIntegration::onUpdate);
    }

    public static void onUpdate(Data data) {
        if (data.playerUUID != null) {
            if (BEARER != null && !data.playerUUID.equals(BEARER)) announceChange(data.playerUUID);
            BEARER = data.playerUUID;
        } else BEARER = UUID.randomUUID();
    }

    public static void announceChange(UUID newBearer) {
        Optional.ofNullable(DragonsLegacyMod.server).ifPresent(server -> {
            ServerPlayer player = server.getPlayerList().getPlayer(newBearer);
            if (player == null) return;
            server.getPlayerList().broadcastSystemMessage(
                CONFIG.messages.bearerChanged.node.toText(
                    PlaceholderContext.of(player
                        .createCommandSourceStack()
                        .withMaximumPermission(LevelBasedPermissionSet.OWNER))
                ),
                false
            );
            server.getPlayerList().broadcastAll(
                new ClientboundSoundPacket(
                    BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.EXPERIENCE_ORB_PICKUP),
                    SoundSource.MASTER,
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    .5f,
                    1f,
                    RandomSource.create().nextLong()
                )
            );
        });
    }
}
