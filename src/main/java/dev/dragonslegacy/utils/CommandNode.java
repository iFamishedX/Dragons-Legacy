package dev.dragonslegacy.utils;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.permissions.PermissionLevel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public record CommandNode(
    String name,
    @Nullable String description,
    @Nullable Predicate<CommandSourceStack> condition,
    @Nullable com.mojang.brigadier.Command<CommandSourceStack> callback,
    @NotNull CommandNode @Nullable [] children,
    @Nullable WeakReference<CommandNode> parent
) {
    public CommandNode {
        if (children != null) {
            var newChildren = new CommandNode[children.length];
            for (int i = 0; i < children.length; i++)
                newChildren[i] = children[i].withParent(this);
            children = newChildren;
        }
    }

    public CommandNode(String name) {
        this(name, null, null, null, null, null);
    }

    public CommandNode(
        String name,
        String description,
        com.mojang.brigadier.Command<CommandSourceStack> callback
    ) {
        this(name, description, null, callback, null, null);
    }

    public CommandNode withDescription(String description) {
        return new CommandNode(name, description, condition, callback, children, parent);
    }

    public CommandNode withOptionalPermission(@NotNull String permission) {
        return withPermission(permission, PermissionLevel.ALL);
    }

    public CommandNode withPermission(@NotNull String permission, @NotNull PermissionLevel permissionLevel) {
        return new CommandNode(
            name,
            description,
            Permissions.require(permission, permissionLevel),
            callback,
            children,
            parent
        );
    }

    public CommandNode withCondition(Predicate<CommandSourceStack> condition) {
        return new CommandNode(name, description, condition, callback, children, parent);
    }

    public CommandNode withCallback(com.mojang.brigadier.Command<CommandSourceStack> callback) {
        return new CommandNode(name, description, condition, callback, children, parent);
    }

    private CommandNode withParent(CommandNode parent) {
        return new CommandNode(name, description, condition, callback, children, new WeakReference<>(parent));
    }

    public CommandNode addChild(CommandNode child) {
        if (children == null) {
            return new CommandNode(
                name,
                description,
                condition,
                callback,
                new CommandNode[]{child.withParent(this)},
                parent
            );
        }

        CommandNode[] newChildren = new CommandNode[children.length + 1];
        System.arraycopy(children, 0, newChildren, 0, children.length);
        newChildren[children.length] = child.withParent(this);
        return new CommandNode(name, description, condition, callback, newChildren, parent);
    }

    public String[] getPath() {
        var parent_obj = parent == null ? null : parent.get();
        if (parent_obj == null)
            return new String[]{name};
        String[] parentPath = parent_obj.getPath();
        String[] path = new String[parentPath.length + 1];
        System.arraycopy(parentPath, 0, path, 0, parentPath.length);
        path[parentPath.length] = name;
        return path;
    }

    public boolean testCondition(CommandSourceStack source) {
        return condition == null || condition.test(source);
    }

    public boolean hasCondition() {
        return condition != null;
    }

    public boolean hasCallback() {
        return callback != null;
    }

    public CommandNode[] children() {
        return children != null ? children : new CommandNode[0];
    }

    public List<CommandNode> getActionNodes() {
        var nodes = new ArrayList<CommandNode>();
        if (hasCallback()) nodes.add(this);
        for (CommandNode child : children())
            nodes.addAll(child.getActionNodes());
        return nodes;
    }

    public LiteralArgumentBuilder<CommandSourceStack> build() {
        var builder = net.minecraft.commands.Commands.literal(name);
        if (hasCondition())
            builder.requires(condition);
        if (hasCallback())
            builder.executes(callback);
        for (CommandNode child : children())
            builder.then(child.build());
        return builder;
    }
}
