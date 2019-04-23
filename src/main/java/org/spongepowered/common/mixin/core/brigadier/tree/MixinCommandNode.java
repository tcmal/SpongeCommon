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
package org.spongepowered.common.mixin.core.brigadier.tree;

import com.mojang.brigadier.AmbiguityConsumer;
import com.mojang.brigadier.tree.CommandNode;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import javax.annotation.Nullable;

@Mixin(value = CommandNode.class, remap = false)
public abstract class MixinCommandNode<S> {

    private Cause storedCause = null;

    @Shadow protected abstract boolean isValidInput(String input);

    @Shadow public abstract void findAmbiguities(AmbiguityConsumer<S> consumer);

    public boolean isValidInput(String input, Cause cause) {
        return isValidInput(input);
    }

    public void findAmbiguities(AmbiguityConsumer<S> consumer, @Nullable Cause cause) {
        this.storedCause = cause;
        findAmbiguities(consumer);
        this.storedCause = null;
    }

}
