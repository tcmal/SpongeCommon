package org.spongepowered.common.item.inventory.custom;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.inventory.Container;
import org.spongepowered.api.item.inventory.custom.ContainerType;
import org.spongepowered.api.item.inventory.menu.ClickHandler;
import org.spongepowered.api.item.inventory.menu.ClickTypes;
import org.spongepowered.api.item.inventory.menu.CloseHandler;
import org.spongepowered.api.item.inventory.menu.InventoryMenu;
import org.spongepowered.api.item.inventory.menu.KeySwapHandler;
import org.spongepowered.api.item.inventory.menu.SlotChangeHandler;
import org.spongepowered.api.item.inventory.menu.SlotClickHandler;
import org.spongepowered.api.item.inventory.slot.SlotIndex;
import org.spongepowered.api.item.inventory.type.ViewableInventory;
import org.spongepowered.api.text.Text;
import org.spongepowered.common.interfaces.IMixinContainer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;

import javax.annotation.Nullable;

public class SpongeInventoryMenu implements InventoryMenu {

    private ViewableInventory inventory;

    private Map<Container, Player> tracked = new HashMap<>();
    private Text title;

    @Nullable private SlotClickHandler slotClickHandler;
    @Nullable private ClickHandler clickHandler;
    @Nullable private KeySwapHandler keySwapHandler;
    @Nullable private SlotChangeHandler changeHandler;
    @Nullable private CloseHandler closeHandler;

    private boolean readonly;

    public SpongeInventoryMenu(ViewableInventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public ViewableInventory getCurrentInventory() {
        return this.inventory;
    }

    @Override
    public ContainerType getType() {
        return this.inventory.getContainerType();
    }

    @Override
    public void setCurrentInventory(ViewableInventory inventory) {
        if (this.getType().equals(inventory.getContainerType())) {
            // ideally we would just swap out the IInventory from existing slots
            // TODO handle container changes
            this.reopen(); // if not possible reopen
        } else {
            // Get all distinct players and reopen inventory for them
            this.reopen();
        }
        this.inventory = inventory;
    }

    private void reopen() {
        new ArrayList<>(tracked.values()).stream().distinct().forEach(this::open);
    }

    @Override
    public void setTitle(Text title) {
        this.title = title;
        this.reopen();
    }

    @Override
    public void registerClose(CloseHandler handler) {
        this.closeHandler = handler;
    }

    @Override
    public InventoryMenu setReadOnly(boolean readOnly) {
        this.readonly = readOnly;
        return this;
    }

    @Override
    public void registerHandler(Object handler) {
        if (handler instanceof ClickHandler) {
            this.registerClick(((ClickHandler) handler));
        }
        if (handler instanceof SlotClickHandler) {
            this.registerSlotClick(((SlotClickHandler) handler));
        }
        if (handler instanceof KeySwapHandler) {
            this.registerKeySwap(((KeySwapHandler) handler));
        }
        if (handler instanceof CloseHandler) {
            this.registerClose(((CloseHandler) handler));
        }
        if (handler instanceof SlotChangeHandler) {
            this.registerChange(((SlotChangeHandler) handler));
        }
    }

    @Override
    public void registerClick(ClickHandler handler) {
        this.clickHandler = handler;
    }

    @Override
    public void registerSlotClick(SlotClickHandler handler) {
        this.slotClickHandler = handler;
    }

    @Override
    public void registerKeySwap(KeySwapHandler handler) {
        this.keySwapHandler = handler;
    }

    @Override
    public void registerChange(SlotChangeHandler handler) {
        this.changeHandler = handler;
    }

    @Override
    public void unregisterAt(SlotIndex... slotIndices) {
        for (SlotIndex slotIndex : slotIndices) {
        }// TODO
    }

    @Override
    public void unregisterAll() {
        // TODO
    }

    @Override
    public Optional<Container> open(Player player) {
        Optional<Container> container = player.openInventory(this.inventory, this.title);
        container.ifPresent(c -> {
            if (c instanceof IMixinContainer) {
                ((IMixinContainer)c).setMenu(this);
                tracked.put(c, player);
            }
        });
        return container;
    }

    public void onClose(Container container) {
        // TODO close callbacks
        this.tracked.remove(container);
    }

    public boolean onClick(int slotId, int dragType, ClickType clickTypeIn, EntityPlayer player, Container container) {

        if (clickTypeIn == ClickType.QUICK_CRAFT) {
            return this.onClickDrag(slotId, dragType, container);
        }

        SlotIndex idx = SlotIndex.of(slotId);
        Optional<org.spongepowered.api.item.inventory.Slot> slot = container.getSlot(idx);

        if (slot.isPresent()) {
            switch (clickTypeIn) {
                case SWAP:
                    if (dragType >= 0 && dragType < 9) {
                        SlotIndex idx2 = SlotIndex.of(dragType);
                        Optional<org.spongepowered.api.item.inventory.Slot> slot2 = container.getSlot(idx2);
                        if (slot2.isPresent() && this.keySwapHandler != null) {
                            return this.keySwapHandler.handle(container, slot.get(), idx, ClickTypes.KEY_SWAP, slot2.get());
                        }
                    }
                    return true;
                case CLONE:
                    if (this.slotClickHandler != null) {
                        return this.slotClickHandler.handle(container, slot.get(), idx, ClickTypes.CLICK_MIDDLE);
                    }
                case PICKUP_ALL:
                    if (this.slotClickHandler != null) {
                        return this.slotClickHandler.handle(container, slot.get(), idx, ClickTypes.DOUBLE_CLICK);
                    }
                default:
                    if (this.slotClickHandler != null) {
                        if (dragType == 0) {
                            return this.onClickLeft(this.slotClickHandler, clickTypeIn, container, idx, slot.get());
                        } else if (dragType == 1) {
                            return this.onClickRight(this.slotClickHandler, clickTypeIn, container, idx, slot.get());
                        }
                        // else unknown drag-type
                    }
                    return true;
            }
        }
        // else no slot present
        switch (clickTypeIn) {
            case PICKUP:
                if (slotId == -999 && clickHandler != null) {
                    if (dragType == 0) {
                        return this.clickHandler.handle(container, ClickTypes.CLICK_LEFT_OUTSIDE);
                    } else if (dragType == 1) {
                        return this.clickHandler.handle(container, ClickTypes.CLICK_RIGHT_OUTSIDE);
                    }
                }
                // else unknown slotId/drag-type
                break;
            case THROW:
                if (slotId == -999) {
                    // TODO check packets - does THROW with slotid -999 exist or is this actually PICKUP?
                    // its supposed to be l/r-click with nothing in hand
                    // TODO check if those exist
                    ///**
                    // * Throwing one item on the cursor by clicking outside the inventory window.
                    // */
                    //public static final org.spongepowered.api.item.inventory.menu.ClickType
                    //        CLICK_THROW_ONE = DummyObjectProvider.createFor(org.spongepowered.api.item.inventory.menu.ClickType.class, "click_throw_one");
                    ///**
                    // * Throwing all items on the cursor by clicking outside the inventory window.
                    // */
                    //public static final org.spongepowered.api.item.inventory.menu.ClickType
                    //        CLICK_THROW_ALL = DummyObjectProvider.createFor(org.spongepowered.api.item.inventory.menu.ClickType.class, "click_throw_all");
                }
                // else unknown slotId/drag-type
                break;
        }

        return true;
    }

    private boolean onClickDrag(int slotId, int dragType, Container container) {


        int dragMode = dragType >> 2 & 3; // (0 : evenly split, 1 : one item by slot, 2 : creative)
        int dragEvent = dragType & 3; // (0 : start drag, 1 : add slot, 2 : end drag)

        switch (dragEvent) {
            case 0: // start drag
                if (this.clickHandler != null) {
                    return this.clickHandler.handle(container, ClickTypes.DRAG_START);
                }
            case 1: // add drag
                SlotIndex idx = SlotIndex.of(slotId);
                Optional<org.spongepowered.api.item.inventory.Slot> slot = container.getSlot(idx);
                if (slot.isPresent() && this.slotClickHandler != null) {
                    switch (dragMode) {
                        case 0:
                            return this.slotClickHandler.handle(container, slot.get(), idx, ClickTypes.DRAG_LEFT_ADD);
                        case 1:
                            return this.slotClickHandler.handle(container, slot.get(), idx, ClickTypes.DRAG_RIGHT_ADD);
                        case 2:
                            return this.slotClickHandler.handle(container, slot.get(), idx, ClickTypes.DRAG_MIDDLE_ADD);
                    }
                }
                break;
            case 2: // end drag
                if (this.clickHandler != null) {
                    return this.clickHandler.handle(container, ClickTypes.DRAG_END);
                }
        }
        return true;
    }

    private boolean onClickRight(SlotClickHandler handler, ClickType clickTypeIn, Container container, SlotIndex idx, org.spongepowered.api.item.inventory.Slot slot) {
        switch (clickTypeIn) {
            case PICKUP:
                return handler.handle(container, slot, idx, ClickTypes.CLICK_RIGHT);
            case QUICK_MOVE:
                return handler.handle(container, slot, idx, ClickTypes.SHIFT_CLICK_RIGHT);
            case THROW:
                // TODO empty cursor check?
                return handler.handle(container, slot, idx, ClickTypes.KEY_THROW_ALL);

        }
        return true;
    }

    private Boolean onClickLeft(SlotClickHandler handler, ClickType clickTypeIn, Container container, SlotIndex idx, org.spongepowered.api.item.inventory.Slot slot) {
        switch (clickTypeIn) {
            case PICKUP:
                return handler.handle(container, slot, idx, ClickTypes.CLICK_LEFT);
            case QUICK_MOVE:
                return handler.handle(container, slot, idx, ClickTypes.SHIFT_CLICK_LEFT);
            case THROW:
                // TODO empty cursor check?
                return handler.handle(container, slot, idx, ClickTypes.KEY_THROW_ONE);
        }
        return true;
    }

    public boolean onChange(ItemStack itemstack, ItemStack oldStack, Container mixinContainer, int slotIndex, Slot slot) {
        if (this.readonly) {
            // readonly by default cancels top inventory changes . but can be overridden by change callbacks
        }
        // TODO change callbacks
        return true;
    }
}