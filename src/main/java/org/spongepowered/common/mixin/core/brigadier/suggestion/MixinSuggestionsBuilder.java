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
package org.spongepowered.common.mixin.core.brigadier.suggestion;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import org.spongepowered.api.command.parameter.Completions;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;
import java.util.stream.Collectors;

@Mixin(value = SuggestionsBuilder.class, remap = false)
public abstract class MixinSuggestionsBuilder implements Completions.Builder {

    @Shadow @Final private List<Suggestion> result;

    @Shadow public abstract SuggestionsBuilder suggest(final String text);
    @Shadow public abstract SuggestionsBuilder suggest(final String text, final Message message);
    @Shadow public abstract SuggestionsBuilder suggest(final int integer);
    @Shadow public abstract SuggestionsBuilder suggest(final int integer, final Message message);

    @Override
    public Completions.Builder suggestion(String completion) {
        suggest(completion);
        return this;
    }

    @Override
    public Completions.Builder suggestion(String completion, String tooltip) {
        suggest(completion, new LiteralMessage(tooltip)); // TODO: can this be translatable text? If so, add the overloads
        return this;
    }

    @Override
    public Completions.Builder suggestion(int completion) {
        suggest(completion);
        return this;
    }

    @Override
    public Completions.Builder suggestion(int completion, String tooltip) {
        suggest(completion, new LiteralMessage(tooltip)); // TODO: can this be translatable text? If so, add the overloads
        return this;
    }

    @Override
    public List<String> buildList() {
        return this.result.stream().map(Suggestion::getText).collect(Collectors.toList());
    }

    @Override
    public Completions.Builder reset() {
        this.result.clear();
        return this;
    }

}
