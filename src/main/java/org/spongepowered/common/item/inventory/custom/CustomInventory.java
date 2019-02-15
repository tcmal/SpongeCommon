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
package org.spongepowered.common.item.inventory.custom;

import com.flowpowered.math.vector.Vector2i;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.IInteractionObject;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.property.Property;
import org.spongepowered.api.event.item.inventory.InteractInventoryEvent;
import org.spongepowered.api.event.item.inventory.container.InteractContainerEvent;
import org.spongepowered.api.item.inventory.Carrier;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.InventoryArchetype;
import org.spongepowered.api.item.inventory.InventoryProperties;
import org.spongepowered.api.item.inventory.gui.GuiId;
import org.spongepowered.api.item.inventory.gui.GuiIds;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.TranslatableText;
import org.spongepowered.api.text.serializer.TextSerializers;
import org.spongepowered.common.data.type.SpongeGuiId;
import org.spongepowered.common.item.inventory.archetype.CompositeInventoryArchetype;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

public class CustomInventory implements IInventory, IInteractionObject {

    private InventoryBasic inv;
    protected InventoryArchetype archetype;
    private Map<Property<?>, Object> properties;
    private Carrier carrier;
    private final boolean isVirtual;

    private Set<EntityPlayer> viewers = new HashSet<>();

    @SuppressWarnings("deprecation")
    public CustomInventory(InventoryArchetype archetype, Map<Property<?>, Object> properties, Carrier carrier,
            Map<Class<? extends InteractContainerEvent>, List<Consumer<? extends InteractContainerEvent>>> listeners, boolean isVirtual,
            PluginContainer plugin) {
        this.archetype = archetype;
        this.properties = properties;
        this.carrier = carrier;
        this.isVirtual = isVirtual;

        int count;
        Vector2i size = (Vector2i) properties.get(InventoryProperties.DIMENSION); // TODO INVENTORY_CAPACITY
        if (size != null) {
            count = size.getX() * size.getY();
        } else {
            count = getSlotCount(archetype);
        }

        Text title = (Text) properties.getOrDefault(InventoryProperties.TITLE, archetype.getProperty(InventoryProperties.TITLE).orElse(null));
        boolean isCustom = !(title instanceof TranslatableText); // TODO: Check minecraft/custom translation

        String legacyTitle = title == null ? "" :
                isCustom ? TextSerializers.LEGACY_FORMATTING_CODE.serialize(title)
                        : ((TranslatableText) title).getTranslation().getId();
        this.inv = new InventoryBasic(legacyTitle, isCustom, count);

        // Updates the Inventory for all viewers on any change
        this.inv.addInventoryChangeListener(i -> this.viewers.forEach(v -> {
            v.openContainer.detectAndSendChanges();
        }));

        for (Map.Entry<Class<? extends InteractInventoryEvent>, List<Consumer<? extends InteractInventoryEvent>>> entry: listeners.entrySet()) {
            Sponge.getEventManager().registerListener(plugin, entry.getKey(), new CustomInventoryListener((Inventory) this, entry.getValue()));
        }
    }

    private static int getSlotCount(InventoryArchetype archetype) {
        Optional<Vector2i> dimension = archetype.getProperty(InventoryProperties.DIMENSION);
        if (dimension.isPresent()) {
            return dimension.get().getX() * dimension.get().getY();
        }
        Optional<Integer> capacity = archetype.getProperty(InventoryProperties.CAPACITY);
        if (capacity.isPresent()) {
            return capacity.get();
        }

        int count = 0;
        List<InventoryArchetype> childs = archetype.getChildArchetypes();
        if (childs.isEmpty()) {
            throw new IllegalArgumentException("Missing dimensions!");
        }
        for (InventoryArchetype childArchetype : childs) {
            count += getSlotCount(childArchetype);
        }
        return count;
    }

    public InventoryArchetype getArchetype() {
        return this.archetype;
    }

    @Override
    public Container createContainer(InventoryPlayer playerInventory, EntityPlayer playerIn) {

        // To be viewable the Inventory has to implement IInteractionObject and thus provide a Container
        // displayChest falls back to Chest for IInventory instance

        // if the ArcheType given is CHEST or DOUBLECHEST
        // TODO how can this be checked? ContainerProperty? (Class<? extends Container>)
        // vanilla ContainerChest takes the Size of the InventoryBasic / 9 as rows


        // Display property?
        // TODO custom container including filters and other slot stuff
        this.viewers.add(playerIn);

        if (this.archetype instanceof CompositeInventoryArchetype) {
            CompositeInventoryArchetype.ContainerProvider provider = ((CompositeInventoryArchetype) this.archetype).getContainerProvider();
            if (provider != null) {
                return provider.provide(this, playerIn);
            }
        }
        return new CustomContainer(playerIn, this);
    }

    @Override
    public String getGuiID() {
        GuiId guiId = (GuiId) this.properties.get(InventoryProperties.GUI_ID);
        if (guiId != null) {
            if (guiId instanceof SpongeGuiId) {
                return ((SpongeGuiId) guiId).getInternalId(); // Handle Vanilla EntityHorse GuiId
            }
            return guiId.getKey().toString();
        }
        guiId = this.archetype.getProperty(InventoryProperties.GUI_ID).orElse(GuiIds.CHEST);
        if (guiId instanceof SpongeGuiId) {
            return ((SpongeGuiId) guiId).getInternalId(); // Handle Vanilla EntityHorse GuiId
        }
        return guiId.getKey().toString();
    }

    // IInventory delegation

    @Override
    public ITextComponent getDisplayName() {
        return this.inv.getDisplayName();
    }

    @Override
    public String getName() {
        return this.inv.getName();
    }

    @Override
    public boolean hasCustomName() {
        return this.inv.hasCustomName();
    }

    @Override
    public int getSizeInventory() {
        return this.inv.getSizeInventory();
    }

    @Override
    public boolean isEmpty() {
        return this.inv.isEmpty();
    }

    @Override
    public ItemStack getStackInSlot(int index) {
        return this.inv.getStackInSlot(index);
    }

    @Override
    public ItemStack decrStackSize(int index, int count) {
        return this.inv.decrStackSize(index, count);
    }

    @Override
    public ItemStack removeStackFromSlot(int index) {
        return this.inv.removeStackFromSlot(index);
    }

    @Override
    public void setInventorySlotContents(int index, ItemStack stack) {
        this.inv.setInventorySlotContents(index, stack);
    }

    @Override
    public int getInventoryStackLimit() {
        return this.inv.getInventoryStackLimit();
    }

    @Override
    public void markDirty() {
        this.inv.markDirty();
    }

    @Override
    public boolean isUsableByPlayer(EntityPlayer player) {
        return this.inv.isUsableByPlayer(player);
    }

    @Override
    public void openInventory(EntityPlayer player) {
        this.viewers.add(player);
        this.inv.openInventory(player);
    }

    @Override
    public void closeInventory(EntityPlayer player) {
        this.viewers.remove(player);
        this.inv.closeInventory(player);
    }

    @Override
    public boolean isItemValidForSlot(int index, ItemStack stack) {
        return this.inv.isItemValidForSlot(index, stack);
    }

    @Override
    public int getField(int id) {
        return this.inv.getField(id);
    }

    @Override
    public void setField(int id, int value) {
        this.inv.setField(id, value);
    }

    @Override
    public int getFieldCount() {
        return this.inv.getFieldCount();
    }

    @Override
    public void clear() {
        this.inv.clear();
    }

    public Map<Property<?>, ?> getProperties() {
        return this.properties;
    }

    public Carrier getCarrier() {
        return this.carrier;
    }

    public boolean isVirtual() {
        return isVirtual;
    }
}
