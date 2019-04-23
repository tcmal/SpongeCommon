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

import net.minecraft.command.ICommandSource;
import net.minecraft.util.text.ITextComponent;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.common.command.CommandHelper;
import org.spongepowered.common.text.SpongeTexts;

public class CauseCommandSource implements ICommandSource {

    private final Cause cause;
    private final ICommandSource wrappedSource;

    public CauseCommandSource(Cause cause) {
        this.cause = cause;
        this.wrappedSource = CommandHelper.getCommandSource(cause);
    }

    public Cause getCause() {
        return this.cause;
    }

    public ICommandSource getWrappedSource() {
        return this.wrappedSource;
    }

    @Override
    public void sendMessage(ITextComponent component) {
        CommandHelper.getTargetMessageChannel(this.cause).send(SpongeTexts.toText(component));
    }

    @Override
    public boolean shouldReceiveFeedback() {
        return this.wrappedSource.shouldReceiveFeedback();
    }

    @Override
    public boolean shouldReceiveErrors() {
        return this.wrappedSource.shouldReceiveFeedback();
    }

    @Override
    public boolean allowLogging() {
        return this.wrappedSource.allowLogging();
    }
}
