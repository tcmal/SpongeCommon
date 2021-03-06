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
package org.spongepowered.common.mixin.core.tileentity;

import static net.minecraft.inventory.SlotFurnaceFuel.isBucket;
import static org.spongepowered.api.data.DataQuery.of;

import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.tileentity.carrier.Furnace;
import org.spongepowered.api.data.DataContainer;
import org.spongepowered.api.data.Transaction;
import org.spongepowered.api.event.SpongeEventFactory;
import org.spongepowered.api.event.block.tileentity.SmeltEvent;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.util.annotation.NonnullByDefault;
import org.spongepowered.asm.lib.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.Surrogate;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import org.spongepowered.common.SpongeImpl;
import org.spongepowered.common.interfaces.data.IMixinCustomNameable;
import org.spongepowered.common.item.inventory.adapter.InventoryAdapter;
import org.spongepowered.common.item.inventory.adapter.impl.slots.FuelSlotAdapter;
import org.spongepowered.common.item.inventory.adapter.impl.slots.InputSlotAdapter;
import org.spongepowered.common.item.inventory.adapter.impl.slots.OutputSlotAdapter;
import org.spongepowered.common.item.inventory.lens.Fabric;
import org.spongepowered.common.item.inventory.lens.SlotProvider;
import org.spongepowered.common.item.inventory.lens.impl.ReusableLens;
import org.spongepowered.common.item.inventory.lens.impl.collections.SlotCollection;
import org.spongepowered.common.item.inventory.lens.impl.minecraft.FurnaceInventoryLens;
import org.spongepowered.common.item.inventory.lens.impl.slots.FuelSlotLensImpl;
import org.spongepowered.common.item.inventory.lens.impl.slots.InputSlotLensImpl;
import org.spongepowered.common.item.inventory.lens.impl.slots.OutputSlotLensImpl;
import org.spongepowered.common.item.inventory.util.ItemStackUtil;

import java.util.Collections;

@NonnullByDefault
@Mixin(TileEntityFurnace.class)
public abstract class MixinTileEntityFurnace extends MixinTileEntityLockable implements Furnace, IMixinCustomNameable {

    @Shadow private String furnaceCustomName;

    @Shadow private NonNullList<ItemStack> furnaceItemStacks;

    @Shadow private int cookTime;
    @Shadow private int furnaceBurnTime;
    @Shadow private int currentItemBurnTime;

    @Shadow protected abstract boolean canSmelt();


    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public ReusableLens<?> generateLens(Fabric fabric, InventoryAdapter adapter) {
        return ReusableLens.getLens(FurnaceInventoryLens.class, ((InventoryAdapter) this), this::generateSlotProvider, this::generateRootLens);
    }

    @SuppressWarnings("unchecked")
    private SlotProvider generateSlotProvider() {
        return new SlotCollection.Builder().add(InputSlotAdapter.class, InputSlotLensImpl::new)
                .add(FuelSlotAdapter.class, (i) -> new FuelSlotLensImpl(i, (s) -> TileEntityFurnace.isItemFuel((ItemStack) s) || isBucket(
                        (ItemStack) s), t -> {
                    final ItemStack nmsStack = (ItemStack) org.spongepowered.api.item.inventory.ItemStack.of(t, 1);
                    return TileEntityFurnace.isItemFuel(nmsStack) || isBucket(nmsStack);
                }))
                // TODO represent the filtering in the API somehow
                .add(OutputSlotAdapter.class, (i) -> new OutputSlotLensImpl(i, (s) -> true, (t) -> true))
                .build();
    }

    @SuppressWarnings("unchecked")
    private FurnaceInventoryLens generateRootLens(SlotProvider slots) {
        return new FurnaceInventoryLens((InventoryAdapter) this, slots);
    }

    @Override
    public DataContainer toContainer() {
        DataContainer container = super.toContainer();
        container.set(of("BurnTime"), this.getField(0));
        container.set(of("BurnTimeTotal"), this.getField(1));
        container.set(of("CookTime"), this.getField(3) - this.getField(2));
        container.set(of("CookTimeTotal"), this.getField(3));
        if (this.furnaceCustomName != null) {
            container.set(of("CustomName"), this.furnaceCustomName);
        }
        return container;
    }

    @Override
    public void setCustomDisplayName(String customName) {
        ((TileEntityFurnace) (Object) this).setCustomInventoryName(customName);
    }

    // Shrink Fuel
    @Redirect(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;shrink(I)V"))
    private void onShrinkFuelStack(ItemStack itemStack, int quantity) {
        Cause cause = Sponge.getCauseStackManager().getCurrentCause();

        ItemStackSnapshot fuel = ItemStackUtil.snapshotOf(itemStack);
        ItemStackSnapshot shrinkedFuel = ItemStackUtil.snapshotOf(ItemStackUtil.cloneDefensive(itemStack, itemStack.getCount() - 1));

        Transaction<ItemStackSnapshot> transaction = new Transaction<>(fuel, shrinkedFuel);
        SmeltEvent.ConsumeFuel event = SpongeEventFactory.createSmeltEventConsumeFuel(cause, fuel, this, Collections.singletonList(transaction));
        SpongeImpl.postEvent(event);
        if (event.isCancelled()) {
            this.currentItemBurnTime = 0;
            return;
        }

        if (!transaction.isValid()) {
            return;
        }

        if (transaction.getCustom().isPresent()) {
            this.furnaceItemStacks.set(1, ItemStackUtil.fromSnapshotToNative(transaction.getFinal()));
        } else { // vanilla
            itemStack.shrink(quantity);
        }
    }

    // Tick up and Start
    @Redirect(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/tileentity/TileEntityFurnace;canSmelt()Z", ordinal = 1))
    private boolean onCanSmeltTickUp(TileEntityFurnace furnace) {
        if (!this.canSmelt()) {
            return false;
        }

        ItemStackSnapshot fuel = ItemStackUtil.snapshotOf(this.furnaceItemStacks.get(1));

        Cause cause = Sponge.getCauseStackManager().getCurrentCause();
        if (this.cookTime == 0) { // Start
            SmeltEvent.Start event = SpongeEventFactory.createSmeltEventStart(cause, fuel, this, Collections.emptyList());
            SpongeImpl.postEvent(event);
            return !event.isCancelled();

        } else { // Tick up
            SmeltEvent.Tick event = SpongeEventFactory.createSmeltEventTick(cause, fuel, this, Collections.emptyList());
            SpongeImpl.postEvent(event);
            return !event.isCancelled();
        }
    }

    // Tick down
    @Redirect(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/MathHelper;clamp(III)I"))
    private int onClampTickDown(int newCookTime, int zero, int totalCookTime) {
        int clampedCookTime = MathHelper.clamp(newCookTime, zero, totalCookTime);
        ItemStackSnapshot fuel = ItemStackUtil.snapshotOf(this.furnaceItemStacks.get(1));
        Cause cause = Sponge.getCauseStackManager().getCurrentCause();
        SmeltEvent.Tick event = SpongeEventFactory.createSmeltEventTick(cause, fuel, this, Collections.emptyList());
        SpongeImpl.postEvent(event);
        if (event.isCancelled()) {
            return this.cookTime; // dont tick down
        }

        return clampedCookTime;
    }

    // Interrupt-Active - e.g. a player removing the currently smelting item
    @Inject(method = "setInventorySlotContents", at = @At(value = "INVOKE", target = "Lnet/minecraft/tileentity/TileEntityFurnace;getCookTime(Lnet/minecraft/item/ItemStack;)I"))
    private void onResetCookTimeActive(CallbackInfo ci) {
        callInteruptSmeltEvent();
    }

    // Interrupt-Passive - if the currently smelting item was removed in some other way
    @Inject(method = "update", at = @At(shift = At.Shift.BEFORE, value = "FIELD", opcode = Opcodes.PUTFIELD, target = "Lnet/minecraft/tileentity/TileEntityFurnace;cookTime:I", ordinal = 0),
            slice = @Slice(from = @At(value = "INVOKE", target = "Lnet/minecraft/tileentity/TileEntityFurnace;smeltItem()V")))
    private void onResetCookTimePassive(CallbackInfo ci) {
        callInteruptSmeltEvent();
    }

    private void callInteruptSmeltEvent() {
        if (this.cookTime > 0) {
            ItemStackSnapshot fuel = ItemStackUtil.snapshotOf(this.furnaceItemStacks.get(1));
            Cause cause = Sponge.getCauseStackManager().getCurrentCause();
            SmeltEvent.Interrupt event = SpongeEventFactory.createSmeltEventInterrupt(cause, fuel, Collections.emptyList(), this);
            SpongeImpl.postEvent(event);
        }
    }

    // Finish
    @Inject(method = "smeltItem", locals = LocalCapture.CAPTURE_FAILHARD, at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;shrink(I)V"))
    private void afterSmeltItem(CallbackInfo ci, ItemStack itemStack, ItemStack result, ItemStack outputStack) {
        ItemStackSnapshot fuel = ItemStackUtil.snapshotOf(this.furnaceItemStacks.get(1));
        Cause cause = Sponge.getCauseStackManager().getCurrentCause();
        SmeltEvent.Finish event = SpongeEventFactory.createSmeltEventFinish(cause, fuel, Collections.singletonList(ItemStackUtil.snapshotOf(result)), this);
        SpongeImpl.postEvent(event);
    }

    /**
     * The jvm can optimize the local variable table in production (in raw vanilla, not decompiled/recompiled)
     * to where the local variable table ends up being trimmed. Because of that, development
     * environments and recompiled environments (such as in a forge environment), we end up with
     * the original three itemstacks still on the local variable table by the time shrink is called.
     *
     * Can be verified with <a href="https://i.imgur.com/IfeLzed.png">that image</a>.
     *
     * @param ci The callback injection
     * @param outputStack The output
     */
    @Surrogate
    private void afterSmeltItem(CallbackInfo ci, ItemStack outputStack) {
        ItemStackSnapshot fuel = ItemStackUtil.snapshotOf(this.furnaceItemStacks.get(1));
        Cause cause = Sponge.getCauseStackManager().getCurrentCause();
        ItemStackSnapshot result = ItemStackUtil.snapshotOf(FurnaceRecipes.instance().getSmeltingResult(this.furnaceItemStacks.get(0)));
        SmeltEvent.Finish event = SpongeEventFactory.createSmeltEventFinish(cause, fuel, Collections.singletonList(result), this);
        SpongeImpl.postEvent(event);
    }

}
