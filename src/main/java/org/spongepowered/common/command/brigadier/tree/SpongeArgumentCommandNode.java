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
import org.spongepowered.common.command.parameter.SpongeValueCompleter;

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.CompletableFuture;

import javax.annotation.Nullable;

public class SpongeArgumentCommandNode<T> extends ArgumentCommandNode<Cause, T> {
    private static final String REQUIRED_ARGUMENT_OPEN = "<";
    private static final String REQUIRED_ARGUMENT_CLOSE = ">";

    private static final String OPTIONAL_ARGUMENT_OPEN = "[";
    private static final String OPTIONAL_ARGUMENT_CLOSE = "]";

    private final Parameter.Value<T> parameter;
    private final String name;
    @Nullable private final CommandExecutor executor;

    public SpongeArgumentCommandNode(Parameter.Value<T> parameter, @Nullable CommandExecutor executor) {
        super(parameter.getKey().key(),
                new DummyArgumentType<>(), // TODO: make this representative
                executor == null ? null : new SpongeCommandExecutorWrapper(executor),
                parameter.getRequirement(),
                null,
                null,
                false,
                new SpongeValueCompleter(parameter.getCompleter()));
        this.parameter = parameter;
        this.name = this.parameter.getKey().key();
        this.executor = executor;
    }

    protected boolean isValidInput(CommandDispatcher<Cause> dispatcher, Cause cause, String input) {
        for (ValueParser<? extends T> param : this.parameter.getParsers()) {
            try {
                final SpongeStringReader reader = new SpongeStringReader(input);
                param.getValue(reader, new SpongeCommandContextBuilder(dispatcher, cause, 0));
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
        return isValidInput(new CommandDispatcher<>(), Sponge.getCauseStackManager().getCurrentCause(), input);
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
    public void parse(StringReader reader, CommandContextBuilder<Cause> contextBuilder) throws CommandSyntaxException {
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
    public CompletableFuture<Suggestions> listSuggestions(CommandContext<Cause> context, SuggestionsBuilder builder) throws CommandSyntaxException {
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
                result = parserIterator.next().getValue(argReader, (org.spongepowered.api.command.parameter.CommandContext.Builder) contextBuilder)
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

    private static class DummyArgumentType<T> implements ArgumentType<T> {

        @Override public <S> T parse(StringReader reader) throws CommandSyntaxException {
            throw new AssertionError("This should not be called.");
        }
    }
}
