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
package org.spongepowered.common.command.brigadier.tree;

import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ImmutableStringReader;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.CommandContextBuilder;
import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import net.minecraft.command.ICommandSource;
import org.lwjgl.system.CallbackI;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.exception.ArgumentParseException;
import org.spongepowered.api.command.CommandExecutor;
import org.spongepowered.api.command.parameter.ArgumentReader;
import org.spongepowered.api.command.parameter.Completions;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.command.parameter.managed.ValueParser;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.common.command.SpongeCommandExecutorWrapper;
import org.spongepowered.common.command.brigadier.SpongeStringReader;
import org.spongepowered.common.command.brigadier.context.SpongeCommandContextBuilder;
import org.spongepowered.common.command.manager.CauseCommandSource;
import org.spongepowered.common.command.parameter.SpongeValueCompleter;

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

import javax.annotation.Nullable;

public class SpongeArgumentCommandNode<T> extends ArgumentCommandNode<ICommandSource, T> {
    private static final String REQUIRED_ARGUMENT_OPEN = "<";
    private static final String REQUIRED_ARGUMENT_CLOSE = ">";

    private static final String OPTIONAL_ARGUMENT_OPEN = "[";
    private static final String OPTIONAL_ARGUMENT_CLOSE = "]";

    private final Parameter.Value<T> parameter;
    private final String name;

    public SpongeArgumentCommandNode(Parameter.Value<T> parameter, @Nullable CommandExecutor executor) {
        super(parameter.getKey().key(),
                new DummyArgumentType<>(), // TODO: make this representative so we can register it (check if a type already exists,
                                           // in the MC registry, if not, create it)
                executor == null ? null : new SpongeCommandExecutorWrapper(executor),
                wrapCausePredicate(parameter.getRequirement()),
                null,
                null,
                false,
                new SpongeValueCompleter(parameter.getCompleter()));
        this.parameter = parameter;
        this.name = this.parameter.getKey().key();
    }

    protected boolean isValidInput(CommandDispatcher<ICommandSource> dispatcher, ICommandSource commandSource, String input) {
        for (ValueParser<? extends T> param : this.parameter.getParsers()) {
            try {
                final SpongeStringReader reader = new SpongeStringReader(input);
                param.getValue(reader, new SpongeCommandContextBuilder(dispatcher, commandSource, 0));
                if (!reader.canRead() || reader.peek() == ' ') {
                    return true;
                }
            } catch (final ArgumentParseException ignored) {
                // ignored
            }
        }

        return false;
    }

    @Override
    public boolean isValidInput(String input) {
        return isValidInput(new CommandDispatcher<>(),
                new CauseCommandSource(Sponge.getCauseStackManager().getCurrentCause()),
                input);
    }

    @Override
    public String getName() {
        return this.name;
    }

    // TODO: Cause aware?
    @Override
    public String getUsageText() {
        if (this.parameter.isOptional()) {
            return OPTIONAL_ARGUMENT_OPEN + this.parameter.getKey() + OPTIONAL_ARGUMENT_CLOSE;
        } else {
            return REQUIRED_ARGUMENT_OPEN + this.parameter.getKey() + REQUIRED_ARGUMENT_CLOSE;
        }
    }

    @Override
    public void parse(StringReader reader, CommandContextBuilder<ICommandSource> contextBuilder) throws CommandSyntaxException {
        ArgumentReader.Mutable mutableReader;
        SpongeCommandContextBuilder builder;
        if (reader instanceof ArgumentReader.Mutable) {
            mutableReader = (ArgumentReader.Mutable) reader;
        } else {
            mutableReader = new SpongeStringReader(reader);
        }

        if (contextBuilder instanceof SpongeCommandContextBuilder) {
            builder = (SpongeCommandContextBuilder) contextBuilder;
        } else {
            builder = SpongeCommandContextBuilder.createFrom(contextBuilder);
        }

        parseArg(mutableReader, builder);
    }

    @Override
    public CompletableFuture<Suggestions> listSuggestions(CommandContext<ICommandSource> context, SuggestionsBuilder builder) throws CommandSyntaxException {
        this.parameter.getCompleter().complete(
                (Completions.Builder) builder,
                (org.spongepowered.api.command.parameter.CommandContext) context);
        return builder.buildFuture();
    }

    @Override
    protected String getSortedKey() {
        return this.parameter.getKey().key();
    }

    @Override
    public Collection<String> getExamples() {
        return ImmutableList.of(); // TODO: Expose to plugins
    }

    public void parseArg(ArgumentReader.Mutable argReader, SpongeCommandContextBuilder contextBuilder) throws CommandSyntaxException {
        final int start = argReader.getCursor();
        T result = null;
        Iterator<ValueParser<? extends T>> parserIterator = this.parameter.getParsers().iterator();
        while (result == null && parserIterator.hasNext()) {
            try {
                result = parserIterator.next()
                        .getValue(argReader, (org.spongepowered.api.command.parameter.CommandContext.Builder) contextBuilder)
                        .orElse(null);
            } catch (ArgumentParseException e) {
                // TODO: Create exception
                e.printStackTrace();
            }
        }

        if (result == null) {
            if (this.parameter.isOptional()) {
                // if optional, then we just return, else, throw error.
                return;
            } else {
                throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherParseException()
                        .createWithContext((ImmutableStringReader) argReader.getImmutable(), "todo");
            }
        }
        // final T result = parameter.getParsers().parse(reader);
        // final ParsedArgument<S, T> parsed = new ParsedArgument<>(start, reader.getCursor(), result);

        contextBuilder.putEntry(this.parameter.getKey(), result);
        contextBuilder.withNode(this, new StringRange(start, argReader.getCursor()));
    }

    private static Predicate<ICommandSource> wrapCausePredicate(Predicate<Cause> causePredicate) {
        return commandSource -> {
            if (commandSource instanceof CauseCommandSource) {
                return causePredicate.test(((CauseCommandSource) commandSource).getCause());
            }

            return false;
        };
    }

    private static class DummyArgumentType<T> implements ArgumentType<T> {

        @Override
        public T parse(StringReader reader) throws CommandSyntaxException {
            throw new AssertionError("This should not be called.");
        }
    }
}
