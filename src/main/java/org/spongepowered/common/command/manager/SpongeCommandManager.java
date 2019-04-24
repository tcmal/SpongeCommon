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

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.command.CommandSource;
import net.minecraft.command.ICommandSource;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.Command;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.command.manager.CommandManager;
import org.spongepowered.api.command.manager.CommandMapping;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.command.parameter.managed.standard.CatalogedValueParameters;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.EventContextKeys;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.channel.MessageChannel;
import org.spongepowered.api.text.channel.MessageReceiver;
import org.spongepowered.common.command.CommandHelper;
import org.spongepowered.common.command.brigadier.SpongeStringReader;
import org.spongepowered.common.command.brigadier.tree.SpongeArgumentCommandNode;
import org.spongepowered.common.command.parameter.SpongeParameterKey;
import org.spongepowered.common.mixin.core.brigadier.builder.MixinArgumentBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Singleton;

@Singleton
public class SpongeCommandManager extends CommandDispatcher<ICommandSource> implements CommandManager {

    private static final Parameter.Value<String> OPTIONAL_REMAINING_STRING =
            Parameter.remainingRawJoinedStrings().setKey("arguments").optional().build();

    private final Map<String, SpongeCommandMapping> commandMappings = new HashMap<>();
    private final Multimap<PluginContainer, SpongeCommandMapping> pluginToCommandMap = HashMultimap.create();

    // For mods and others that use this. We get the plugin container from the CauseStack
    @Override
    public LiteralCommandNode<ICommandSource> register(LiteralArgumentBuilder<ICommandSource> command) {
        // Get the plugin container
        PluginContainer container = Sponge.getCauseStackManager().getCurrentCause().first(PluginContainer.class)
                .orElseThrow(() -> new IllegalStateException("Cannot register command without knowing it's origin."));

        // Get the builder and the first literal.
        String requestedAlias = command.getLiteral();

        String aliasToRegister = container.getId() + ":" + requestedAlias;

        // Check the mappings we have.
        CommandMapping mapping = this.commandMappings.get(aliasToRegister);
        if (mapping != null && mapping.getCommand().isPresent()) {
            // Already registered as a Sponge command, abort, we're not merging Sponge commands.
            throw new IllegalArgumentException("The command " + aliasToRegister + " is already registered.");
        } else if (mapping != null) {
            // This is a mod. Let the trees merge as expected
            return super.register(adjustForNamespace(aliasToRegister, command));
        }

        LiteralArgumentBuilder<ICommandSource> toBuild = adjustForNamespace(aliasToRegister, command);

        // permissions
        final String permission = container.getId() + ".command." + requestedAlias;

        // TODO: can we use IPhaseContexts to our advantage and store the subject?
        //  May just do what mods expect too.
        Predicate<ICommandSource> requirement = toBuild.getRequirement().and(source -> {
            if (source instanceof Subject) {
                return ((Subject) source).hasPermission(permission);
            } else {
                return CommandHelper.getSubject(
                        Sponge.getCauseStackManager().getCurrentCause())
                            .orElseGet(() -> Sponge.getServer().getConsole()).hasPermission(permission);
            }
        });
        toBuild.requires(requirement);
        // TODO: Check
        Predicate<Cause> causeRequirement = cause -> CommandHelper.getSubject(cause)
                .orElseGet(() -> Sponge.getServer().getConsole())
                .hasPermission(permission);

        boolean registerRootCommand = this.commandMappings.containsKey(requestedAlias);
        Set<String> aliases = new HashSet<>();
        aliases.add(aliasToRegister);
        if (registerRootCommand) {
            aliases.add(requestedAlias);
        }

        // Create the mapping
        SpongeCommandMapping newMapping = new SpongeCommandMapping(
                aliasToRegister, aliases, container, null, false, causeRequirement);
        aliases.forEach(x -> this.commandMappings.put(x, newMapping));
        this.pluginToCommandMap.put(container, newMapping);

        LiteralCommandNode<ICommandSource> node = super.register(toBuild);

        if (registerRootCommand) {
            // Create a redirect
            super.register(LiteralArgumentBuilder.<ICommandSource>literal(requestedAlias).requires(requirement).redirect(node));
        }

        return node;
    }

    // Sponge command
    @Override
    public Optional<CommandMapping> register(PluginContainer container,
                                             Command command,
                                             String primaryAlias,
                                             String... secondaryAliases) {
        // We have a Sponge command, so let's start by checking to see what
        // we're going to register.
        String primaryAliasLowercase = primaryAlias.toLowerCase(Locale.ENGLISH);
        String namespacedAlias = container.getId() + ":" + primaryAlias.toLowerCase(Locale.ENGLISH);
        if (this.commandMappings.containsKey(namespacedAlias)) {
            // It's registered.
            throw new IllegalArgumentException(
                    "The command alias " + primaryAlias + " has already been registered for this plugin");
        }

        Set<String> aliases = new HashSet<>();
        aliases.add(primaryAliasLowercase);
        for (String secondaryAlias : secondaryAliases) {
            aliases.add(secondaryAlias.toLowerCase(Locale.ENGLISH));
        }

        // Okay, what can we register?
        aliases.removeIf(this.commandMappings::containsKey);

        // Now we need to create the command tree.
        LiteralArgumentBuilder<ICommandSource> baseNode = null;
        // TODO: Built commands, parameterized but not Sponge commands need doing.
        if (command instanceof Command.Parameterized) {

        } else {
            // We have a command that isn't parameterised. Best we can do is
            // create a single string argument and put it into the tree.
            baseNode = LiteralArgumentBuilder.<ICommandSource>literal(namespacedAlias)
                        .then(new SpongeArgumentCommandNode<>(
                            OPTIONAL_REMAINING_STRING,
                            commandContext -> command.process(
                                    commandContext.getCause(),
                                    commandContext.getOne(OPTIONAL_REMAINING_STRING).orElse(""))
                        ));

        }

        if (baseNode == null) {
            return Optional.empty();
        }

        // Add to mapping.
        SpongeCommandMapping mapping = new SpongeCommandMapping(
                namespacedAlias,
                aliases,
                container,
                command,
                true,
                c -> true);
        this.pluginToCommandMap.put(container, mapping);
        this.commandMappings.put(namespacedAlias, mapping);
        aliases.forEach(key -> this.commandMappings.put(key, mapping));

        return Optional.of(mapping);
    }

    @Override
    public Optional<CommandMapping> unregister(CommandMapping mapping) {
        return Optional.empty(); // TODO
    }

    @Override
    public Collection<CommandMapping> unregisterAll(PluginContainer container) {
        Collection<SpongeCommandMapping> mappingsToRemove = this.pluginToCommandMap.get(container);
        ImmutableList.Builder<CommandMapping> commandMappingBuilder = ImmutableList.builder();
        for (CommandMapping toRemove : mappingsToRemove) {
            unregister(toRemove).ifPresent(commandMappingBuilder::add);
        }

        return commandMappingBuilder.build();
    }

    @Override
    public Collection<PluginContainer> getPlugins() {
        return ImmutableSet.copyOf(this.pluginToCommandMap.keySet());
    }


    @Override
    public CommandResult process(String arguments) throws CommandException {
        return process(Sponge.getCauseStackManager().getCurrentCause(), arguments);
    }

    @Override
    public <T extends Subject & MessageReceiver> CommandResult process(T subjectReceiver, String arguments) throws CommandException {
        try (CauseStackManager.StackFrame frame = Sponge.getCauseStackManager().pushCauseFrame()) {
            frame.addContext(EventContextKeys.SUBJECT, subjectReceiver);
            frame.addContext(EventContextKeys.MESSAGE_CHANNEL, MessageChannel.fixed(subjectReceiver));
            return process(frame.getCurrentCause(), arguments);
        }
    }

    @Override
    public CommandResult process(Subject subject, MessageChannel receiver, String arguments) throws CommandException {
        try (CauseStackManager.StackFrame frame = Sponge.getCauseStackManager().pushCauseFrame()) {
            frame.addContext(EventContextKeys.SUBJECT, subject);
            frame.addContext(EventContextKeys.MESSAGE_CHANNEL, receiver);
            return process(frame.getCurrentCause(), arguments);
        }
    }

    @Override
    public List<String> suggest(String arguments) {
        return suggest(Sponge.getCauseStackManager().getCurrentCause(), arguments);
    }

    @Override
    public <T extends Subject & MessageReceiver> List<String> suggest(T subjectReceiver, String arguments) {
        try (CauseStackManager.StackFrame frame = Sponge.getCauseStackManager().pushCauseFrame()) {
            frame.addContext(EventContextKeys.SUBJECT, subjectReceiver);
            frame.addContext(EventContextKeys.MESSAGE_CHANNEL, MessageChannel.fixed(subjectReceiver));
            return suggest(frame.getCurrentCause(), arguments);
        }
    }

    @Override
    public List<String> suggest(Subject subject, MessageChannel receiver, String arguments) {
        try (CauseStackManager.StackFrame frame = Sponge.getCauseStackManager().pushCauseFrame()) {
            frame.addContext(EventContextKeys.SUBJECT, subject);
            frame.addContext(EventContextKeys.MESSAGE_CHANNEL, receiver);
            return suggest(frame.getCurrentCause(), arguments);
        }
    }

    private LiteralArgumentBuilder<ICommandSource> adjustForNamespace(String commandName, LiteralArgumentBuilder<ICommandSource> command) {
        LiteralArgumentBuilder<ICommandSource> toReturn = LiteralArgumentBuilder.<ICommandSource>literal(commandName)
                .executes(command.getCommand())
                .requires(command.getRequirement());
        if (command.getRedirectModifier() != null) {
            toReturn.forward(command.getRedirect(), command.getRedirectModifier(), command.isFork());
        }
        ((MixinArgumentBuilder<CommandSource>) command).setArguments(((MixinArgumentBuilder<CommandSource>) command).getArguments());
        return command;
    }

    private CommandResult process(Cause cause, String arguments) throws CommandException {
        try (CauseStackManager.StackFrame frame = Sponge.getCauseStackManager().pushCauseFrame()) {
            int result;
            try {
                SpongeStringReader reader = new SpongeStringReader(arguments);
                ICommandSource source = getCommandSourceFor(reader.readString(), null, cause);
                result = execute(arguments, source);
            } catch (CommandSyntaxException e) {
                throw new CommandException(Text.of(e.getMessage()), e);
            }

            return CommandResult.builder().setResult(result).build();
        }
    }

    @Override
    public ParseResults<ICommandSource> parse(StringReader input, ICommandSource source) {
        try (CauseStackManager.StackFrame frame = Sponge.getCauseStackManager().pushCauseFrame()) {
            StringReader copy = new SpongeStringReader(input);

            // This will select the correct ICommandSource based on the command that is about to be parsed.
            ICommandSource sourceToUse = getCommandSourceFor(input.getRead(), source, frame.getCurrentCause());
            return super.parse(copy, sourceToUse);
        }
    }

    @Override
    public String[] getAllUsage(CommandNode<ICommandSource> node, ICommandSource source, boolean restricted) {
        return super.getAllUsage(node, source, restricted);
    }

    private List<String> suggest(Cause cause, String arguments) {
        try (CauseStackManager.StackFrame frame = Sponge.getCauseStackManager().pushCauseFrame()) {
            return ImmutableList.of(); // TODO: obviously not this
        }
    }

    private ICommandSource getCommandSourceFor(String command, @Nullable ICommandSource requestedSource, Cause cause) {
        // Is the command registered?
        SpongeCommandMapping mapping = this.commandMappings.get(command);
        if (mapping != null && mapping.isRegisteredAsSpongeCommand()) {
            return requestedSource instanceof CauseCommandSource ? requestedSource : new CauseCommandSource(cause);
        }

        return requestedSource instanceof CauseCommandSource ? ((CauseCommandSource) requestedSource).getWrappedSource() :
                CommandHelper.getCommandSource(cause);
    }

}
