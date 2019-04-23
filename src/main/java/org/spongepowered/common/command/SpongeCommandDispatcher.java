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
package org.spongepowered.common.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.command.ICommandSource;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.common.command.brigadier.SpongeStringReader;
import org.spongepowered.common.command.brigadier.context.SpongeCommandContextBuilder;
import org.spongepowered.common.command.brigadier.tree.SpongeArgumentCommandNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SpongeCommandDispatcher extends CommandDispatcher<ICommandSource> {

    @Override
    public ParseResults<ICommandSource> parse(String command, ICommandSource source) {
        return parse(new SpongeStringReader(command), source);
    }

    @Override
    public ParseResults<ICommandSource> parse(StringReader command, ICommandSource source) {
        return parse(new SpongeStringReader(command), source);
    }

    public ParseResults<ICommandSource> parse(SpongeStringReader command, ICommandSource source) {
        final SpongeCommandContextBuilder context = new SpongeCommandContextBuilder(this, source, 0);
        return parse(getRoot(), command, context);
    }

    /*
     * This method is mostly the same as CommandDispatcher#parseNodes, but due to the fact we want
     * to replace the StringReader and CommandContextBuilder, we override the method.
     *
     * We don't use mixins and we don't want to affect the base class, particularly for mod
     * compatibility.
     *
     * TODO: see if we can get away with not doing this like this.
     */
    private ParseResults<ICommandSource> parse(
            final CommandNode<ICommandSource> node,
            final SpongeStringReader originalReader,
            final SpongeCommandContextBuilder contextSoFar) {
        final ICommandSource source = contextSoFar.getSource();
        Map<CommandNode<ICommandSource>, CommandSyntaxException> errors = null;
        List<SpongePartialParse> potentials = null;
        final int cursor = originalReader.getCursor();

        for (final CommandNode<ICommandSource> child : node.getRelevantNodes(originalReader)) {
            if (!child.canUse(source)) {
                continue;
            }
            final SpongeCommandContextBuilder context = contextSoFar.copy();
            final SpongeStringReader reader = new SpongeStringReader(originalReader);
            try {
                try {
                    if (child instanceof SpongeArgumentCommandNode) {
                        ((SpongeArgumentCommandNode) child).parseArg(reader, context);
                    } else {
                        child.parse(reader, context);
                    }
                } catch (final RuntimeException ex) {
                    throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherParseException().createWithContext(reader, ex.getMessage());
                }
                if (reader.canRead()) {
                    if (reader.peek() != ARGUMENT_SEPARATOR_CHAR) {
                        throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherExpectedArgumentSeparator().createWithContext(reader);
                    }
                }
            } catch (final CommandSyntaxException ex) {
                if (errors == null) {
                    errors = new LinkedHashMap<>();
                }
                errors.put(child, ex);
                reader.setCursor(cursor);
                continue;
            }

            context.withCommand(child.getCommand());
            if (reader.canRead(child.getRedirect() == null ? 2 : 1)) {
                reader.skip();
                if (child.getRedirect() != null) {
                    final SpongeCommandContextBuilder childContext = new SpongeCommandContextBuilder(this, source, reader.getCursor());
                    childContext.withNode(child.getRedirect(), StringRange.between(cursor, reader.getCursor() - 1));
                    final ParseResults<ICommandSource> parse = parse(child.getRedirect(), reader, childContext);
                    context.withChild(parse.getContext());
                    return new ParseResults<>(context, originalReader.getCursor(), parse.getReader(), parse.getExceptions());
                } else {
                    final ParseResults<ICommandSource> parse = parse(child, reader, context);
                    if (potentials == null) {
                        potentials = new ArrayList<>(1);
                    }
                    potentials.add(new SpongePartialParse(context, parse));
                }
            } else {
                if (potentials == null) {
                    potentials = new ArrayList<>(1);
                }
                potentials.add(new SpongePartialParse(context, new ParseResults<>(context, originalReader.getCursor(), reader, Collections.emptyMap())));
            }
        }

        if (potentials != null) {
            if (potentials.size() > 1) {
                potentials.sort((a, b) -> {
                    if (!a.parse.getReader().canRead() && b.parse.getReader().canRead()) {
                        return -1;
                    }
                    if (a.parse.getReader().canRead() && !b.parse.getReader().canRead()) {
                        return 1;
                    }
                    if (a.parse.getExceptions().isEmpty() && !b.parse.getExceptions().isEmpty()) {
                        return -1;
                    }
                    if (!a.parse.getExceptions().isEmpty() && b.parse.getExceptions().isEmpty()) {
                        return 1;
                    }
                    return 0;
                });
            }
            return potentials.get(0).parse;
        }

        return new ParseResults<>(contextSoFar, originalReader.getCursor(), originalReader, errors == null ? Collections.emptyMap() : errors);
    }

    private static class SpongePartialParse {
        public final SpongeCommandContextBuilder context;
        public final ParseResults<ICommandSource> parse;

        private SpongePartialParse(final SpongeCommandContextBuilder context, final ParseResults<ICommandSource> parse) {
            this.context = context;
            this.parse = parse;
        }

    }

}
