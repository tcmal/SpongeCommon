package org.spongepowered.common.command.brigadier.tree;

import com.mojang.brigadier.RedirectModifier;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import org.spongepowered.api.command.managed.CommandExecutor;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.common.command.SpongeCommandExecutorWrapper;

import java.util.function.Predicate;

import javax.annotation.Nullable;

public class SpongeLiteralCommandNode extends LiteralCommandNode<Cause> {

    @Nullable private final CommandExecutor executor;

    public SpongeLiteralCommandNode(String literal,
            @Nullable CommandExecutor executor,
            Predicate<Cause> requirement,
            CommandNode<Cause> redirect,
            RedirectModifier<Cause> modifier,
            boolean forks) {
        super(literal, executor == null ? null : new SpongeCommandExecutorWrapper(executor), requirement, redirect, modifier, forks);
        this.executor = executor;
    }


}
