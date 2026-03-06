package dev.dragonslegacy.utils;

import net.minecraft.server.MinecraftServer;

import java.util.function.Consumer;

public class ScheduledEvent {
    private final Consumer<MinecraftServer> action;
    private long ticks;

    public ScheduledEvent(long ticks, Consumer<MinecraftServer> action) {
        this.ticks = ticks;
        this.action = action;
    }

    public boolean tryRun(MinecraftServer server) {
        this.ticks--;
        if (ticks <= 0) {
            action.accept(server);
            return true;
        }
        return false;
    }
}