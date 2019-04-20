/*
 * This file is part of Sponge, licensed under the MIT License (MIT).
 *
 * Copyright (c) SpongePowered <https://www.spongepowered.org>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.spongepowered.common.command.manager;

import com.google.common.collect.ImmutableSet;
import org.spongepowered.api.command.Command;
import org.spongepowered.api.command.manager.CommandMapping;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.plugin.PluginContainer;

import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import javax.annotation.Nullable;

public class SpongeCommandMapping implements CommandMapping {

    private final String alias;
    private final Set<String> allAliases;
    private final PluginContainer container;
    @Nullable private final Command command;
    private final boolean isSpongeCommand;
    private final Predicate<Cause> requirements;

    public SpongeCommandMapping(String alias,
                                Set<String> allAliases,
                                PluginContainer container,
                                @Nullable Command command,
                                boolean isSpongeCommand,
                                Predicate<Cause> requirements) {
        this.alias = alias;
        this.allAliases = ImmutableSet.copyOf(allAliases);
        this.container = container;
        this.command = command;
        this.isSpongeCommand = isSpongeCommand;
        this.requirements = requirements;
    }

    @Override
    public String getPrimaryAlias() {
        return this.alias;
    }

    @Override
    public Set<String> getAllAliases() {
        return this.allAliases;
    }

    @Override
    public PluginContainer getOwningPlugin() {
        return this.container;
    }

    @Override
    public Optional<Command> getCommand() {
        return Optional.ofNullable(this.command);
    }

    @Override
    public Predicate<Cause> getRequirements() {
        return this.requirements;
    }

    public boolean isRegisteredAsSpongeCommand() {
        return this.isSpongeCommand;
    }

}
