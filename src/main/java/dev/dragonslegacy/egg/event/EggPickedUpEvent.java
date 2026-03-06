package dev.dragonslegacy.egg.event;

import net.minecraft.server.level.ServerPlayer;

/**
 * Published when the canonical Dragon Egg is picked up by a player.
 */
public final class EggPickedUpEvent implements DragonEggEvent {

    private final ServerPlayer player;

    public EggPickedUpEvent(ServerPlayer player) {
        this.player = player;
    }

    /** The player who picked up the egg. */
    public ServerPlayer getPlayer() { return player; }
}
