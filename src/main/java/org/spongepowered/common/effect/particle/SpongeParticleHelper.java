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
package org.spongepowered.common.effect.particle;

import com.flowpowered.math.vector.Vector3d;
import com.flowpowered.math.vector.Vector3f;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityFireworkRocket;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.network.Packet;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.network.play.server.SPacketDestroyEntities;
import net.minecraft.network.play.server.SPacketEffect;
import net.minecraft.network.play.server.SPacketEntityMetadata;
import net.minecraft.network.play.server.SPacketEntityStatus;
import net.minecraft.network.play.server.SPacketParticles;
import net.minecraft.network.play.server.SPacketSpawnObject;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionType;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.data.type.NotePitch;
import org.spongepowered.api.effect.particle.ParticleOptions;
import org.spongepowered.api.effect.particle.ParticleTypes;
import org.spongepowered.api.item.FireworkEffect;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.util.Color;
import org.spongepowered.api.util.Direction;
import org.spongepowered.common.data.processor.common.FireworkUtils;
import org.spongepowered.common.data.type.SpongeNotePitch;
import org.spongepowered.common.item.inventory.SpongeItemStackSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

public final class SpongeParticleHelper {

    /**
     * Gets the list of packets that are needed to spawn the particle effect at
     * the position. This method tries to minimize the amount of packets for
     * better performance and lower bandwidth use.
     *
     * @param effect The particle effect
     * @param position The position
     * @return The packets
     */
    public static List<Packet<?>> toPackets(SpongeParticleEffect effect, Vector3d position) {
        ICachedParticleEffect cachedPacket = effect.cachedParticle;
        if (cachedPacket == null) {
            cachedPacket = effect.cachedParticle = toCachedPacket(effect);
        }
        if (cachedPacket == EmptyCachedPacket.INSTANCE) {
            return Collections.emptyList();
        }
        final List<Packet<?>> packets = new ArrayList<>();
        cachedPacket.process(position, packets);
        return packets;
    }

    @SuppressWarnings("deprecation")
    private static int getBlockState(SpongeParticleEffect effect, Optional<BlockState> defaultBlockState) {
        Optional<BlockState> blockState = effect.getOption(ParticleOptions.BLOCK_STATE);
        if (blockState.isPresent()) {
            return Block.getStateId((IBlockState) blockState.get());
        }
        Optional<ItemStackSnapshot> optSnapshot = effect.getOption(ParticleOptions.ITEM_STACK_SNAPSHOT);
        if (optSnapshot.isPresent()) {
            ItemStackSnapshot snapshot = optSnapshot.get();
            Optional<BlockType> blockType = snapshot.getType().getBlock();
            if (blockType.isPresent()) {
                return Block.getStateId(((Block) blockType.get()).getStateFromMeta(
                        ((SpongeItemStackSnapshot) snapshot).getDamageValue()));
            }
            return 0;
        }
        return Block.getStateId((IBlockState) defaultBlockState.get());
    }

    private static int getDirectionData(Direction direction) {
        if (direction.isSecondaryOrdinal()) {
            direction = Direction.getClosest(direction.asOffset(), Direction.Division.ORDINAL);
        }
        switch (direction) {
            case SOUTHEAST:
                return 0;
            case SOUTH:
                return 1;
            case SOUTHWEST:
                return 2;
            case EAST:
                return 3;
            case WEST:
                return 5;
            case NORTHEAST:
                return 6;
            case NORTH:
                return 7;
            case NORTHWEST:
                return 8;
            default:
                return 4;
        }
    }

    private static ICachedParticleEffect toCachedPacket(SpongeParticleEffect effect) {
        SpongeParticleType type = effect.getType();

        EnumParticleTypes internal = type.getInternalType();
        // Special cases
        if (internal == null) {
            if (type == ParticleTypes.FIREWORKS) {
                final List<FireworkEffect> effects = effect.getOption(ParticleOptions.FIREWORK_EFFECTS).orElse(type.getDefaultOption(ParticleOptions.FIREWORK_EFFECTS).get());
                if (effects.isEmpty()) {
                    return EmptyCachedPacket.INSTANCE;
                }
                final net.minecraft.item.ItemStack itemStack = new net.minecraft.item.ItemStack(Items.FIREWORKS);
                FireworkUtils.setFireworkEffects(itemStack, effects);
                final SPacketEntityMetadata packetEntityMetadata = new SPacketEntityMetadata();
                packetEntityMetadata.entityId = CachedFireworkPacket.FIREWORK_ROCKET_ID;
                packetEntityMetadata.dataManagerEntries = new ArrayList<>();
                packetEntityMetadata.dataManagerEntries.add(new EntityDataManager.DataEntry<>(EntityFireworkRocket.FIREWORK_ITEM, itemStack));
                return new CachedFireworkPacket(packetEntityMetadata);
            }
            if (type == ParticleTypes.FERTILIZER) {
                int quantity = effect.getOptionOrDefault(ParticleOptions.QUANTITY).get();
                return new CachedEffectPacket(2005, quantity, false);
            } else if (type == ParticleTypes.SPLASH_POTION) {
                Potion potion = (Potion) effect.getOptionOrDefault(ParticleOptions.POTION_EFFECT_TYPE).get();
                for (PotionType potionType : PotionType.REGISTRY) {
                    for (net.minecraft.potion.PotionEffect potionEffect : potionType.getEffects()) {
                        if (potionEffect.getPotion() == potion) {
                            return new CachedEffectPacket(2002, PotionType.REGISTRY.getIDForObject(potionType), false);
                        }
                    }
                }
                return EmptyCachedPacket.INSTANCE;
            } else if (type == ParticleTypes.BREAK_BLOCK) {
                int state = getBlockState(effect, type.getDefaultOption(ParticleOptions.BLOCK_STATE));
                if (state == 0) {
                    return EmptyCachedPacket.INSTANCE;
                }
                return new CachedEffectPacket(2001, state, false);
            } else if (type == ParticleTypes.MOBSPAWNER_FLAMES) {
                return new CachedEffectPacket(2004, 0, false);
            } else if (type == ParticleTypes.ENDER_TELEPORT) {
                return new CachedEffectPacket(2003, 0, false);
            } else if (type == ParticleTypes.DRAGON_BREATH_ATTACK) {
                return new CachedEffectPacket(2006, 0, false);
            } else if (type == ParticleTypes.FIRE_SMOKE) {
                final Direction direction = effect.getOptionOrDefault(ParticleOptions.DIRECTION).get();
                return new CachedEffectPacket(2000, getDirectionData(direction), false);
            }
            return EmptyCachedPacket.INSTANCE;
        }

        Vector3f offset = effect.getOption(ParticleOptions.OFFSET).map(Vector3d::toFloat).orElse(Vector3f.ZERO);

        int quantity = effect.getOption(ParticleOptions.QUANTITY).orElse(1);
        int[] extra = null;

        // The extra values, normal behavior offsetX, offsetY, offsetZ
        double f0 = 0f;
        double f1 = 0f;
        double f2 = 0f;

        // Depends on behavior
        // Note: If the count > 0 -> speed = 0f else if count = 0 -> speed = 1f

        Optional<BlockState> defaultBlockState;
        if (internal != EnumParticleTypes.ITEM_CRACK && (defaultBlockState = type.getDefaultOption(ParticleOptions.BLOCK_STATE)).isPresent()) {
            int state = getBlockState(effect, defaultBlockState);
            if (state == 0) {
                return EmptyCachedPacket.INSTANCE;
            }
            extra = new int[] { state };
        }

        Optional<ItemStackSnapshot> defaultSnapshot;
        if (extra == null && (defaultSnapshot = type.getDefaultOption(ParticleOptions.ITEM_STACK_SNAPSHOT)).isPresent()) {
            Optional<ItemStackSnapshot> optSnapshot = effect.getOption(ParticleOptions.ITEM_STACK_SNAPSHOT);
            if (optSnapshot.isPresent()) {
                ItemStackSnapshot snapshot = optSnapshot.get();
                extra = new int[] { Item.getIdFromItem((Item) snapshot.getType()), ((SpongeItemStackSnapshot) snapshot).getDamageValue() };
            } else {
                Optional<BlockState> optBlockState = effect.getOption(ParticleOptions.BLOCK_STATE);
                if (optBlockState.isPresent()) {
                    BlockState blockState = optBlockState.get();
                    Optional<ItemType> optItemType = blockState.getType().getItem();
                    if (optItemType.isPresent()) {
                        extra = new int[] { Item.getIdFromItem((Item) optItemType.get()),
                                ((Block) blockState.getType()).getMetaFromState((IBlockState) blockState) };
                    } else {
                        return EmptyCachedPacket.INSTANCE;
                    }
                } else {
                    ItemStackSnapshot snapshot = defaultSnapshot.get();
                    extra = new int[] { Item.getIdFromItem((Item) snapshot.getType()), ((SpongeItemStackSnapshot) snapshot).getDamageValue() };
                }
            }
        }

        if (extra == null) {
            extra = new int[0];
        }

        Optional<Double> defaultScale = type.getDefaultOption(ParticleOptions.SCALE);
        Optional<Color> defaultColor;
        Optional<NotePitch> defaultNote;
        Optional<Vector3d> defaultVelocity;
        if (defaultScale.isPresent()) {
            double scale = effect.getOption(ParticleOptions.SCALE).orElse(defaultScale.get());

            // The formula of the large explosion acts strange
            // Client formula: sizeClient = 1 - sizeServer * 0.5
            // The particle effect returns the client value so
            // Server formula: sizeServer = (-sizeClient * 2) + 2
            if (internal == EnumParticleTypes.EXPLOSION_LARGE || internal == EnumParticleTypes.SWEEP_ATTACK) {
                scale = (-scale * 2f) + 2f;
            }

            if (scale == 0f) {
                return new CachedParticlePacket(internal, offset, quantity, extra);
            }

            f0 = scale;
        } else if ((defaultColor = type.getDefaultOption(ParticleOptions.COLOR)).isPresent()) {
            Color color = effect.getOption(ParticleOptions.COLOR).orElse(null);

            boolean isSpell = internal == EnumParticleTypes.SPELL_MOB || internal == EnumParticleTypes.SPELL_MOB_AMBIENT;

            if (!isSpell && (color == null || color.equals(defaultColor.get()))) {
                return new CachedParticlePacket(internal, offset, quantity, extra);
            } else if (isSpell && color == null) {
                color = defaultColor.get();
            }

            f0 = color.getRed() / 255f;
            f1 = color.getGreen() / 255f;
            f2 = color.getBlue() / 255f;

            // Make sure that the x and z component are never 0 for these effects,
            // they would trigger the slow horizontal velocity (unsupported on the server),
            // but we already chose for the color, can't have both
            if (isSpell) {
                f0 = Math.max(f0, 0.001f);
                f2 = Math.max(f0, 0.001f);
            }

            // If the f0 value 0 is, the redstone will set it automatically to red 255
            if (f0 == 0f && internal == EnumParticleTypes.REDSTONE) {
                f0 = 0.00001f;
            }
        } else if ((defaultNote = type.getDefaultOption(ParticleOptions.NOTE)).isPresent()) {
            NotePitch notePitch = effect.getOption(ParticleOptions.NOTE).orElse(defaultNote.get());
            float note = ((SpongeNotePitch) notePitch).getByteId();

            if (note == 0f) {
                return new CachedParticlePacket(internal, offset, quantity, extra);
            }

            f0 = note / 24f;
        } else if ((defaultVelocity = type.getDefaultOption(ParticleOptions.VELOCITY)).isPresent()) {
            Vector3d velocity = effect.getOption(ParticleOptions.VELOCITY).orElse(defaultVelocity.get());

            f0 = velocity.getX();
            f1 = velocity.getY();
            f2 = velocity.getZ();

            Optional<Boolean> slowHorizontalVelocity = type.getDefaultOption(ParticleOptions.SLOW_HORIZONTAL_VELOCITY);
            if (slowHorizontalVelocity.isPresent() &&
                    effect.getOption(ParticleOptions.SLOW_HORIZONTAL_VELOCITY).orElse(slowHorizontalVelocity.get())) {
                f0 = 0f;
                f2 = 0f;
            }

            // The y value won't work for this effect, if the value isn't 0 the velocity won't work
            if (internal == EnumParticleTypes.WATER_SPLASH) {
                f1 = 0f;
            }

            if (f0 == 0f && f1 == 0f && f2 == 0f) {
                return new CachedParticlePacket(internal, offset, quantity, extra);
            }
        }

        // Is this check necessary?
        if (f0 == 0f && f1 == 0f && f2 == 0f) {
            return new CachedParticlePacket(internal, offset, quantity, extra);
        }

        return new CachedOffsetParticlePacket(internal, new Vector3f(f0, f1, f2), offset, quantity, extra);
    }

    private static final class EmptyCachedPacket implements ICachedParticleEffect {

        public static final EmptyCachedPacket INSTANCE = new EmptyCachedPacket();

        @Override
        public void process(Vector3d position, List<Packet<?>> output) {
        }
    }

    private static final class CachedFireworkPacket implements ICachedParticleEffect {

        // Get the next free entity id
        private static final int FIREWORK_ROCKET_ID;
        private static final UUID FIREWORK_ROCKET_UNIQUE_ID;

        private static final SPacketDestroyEntities DESTROY_FIREWORK_ROCKET_DUMMY;
        private static final SPacketEntityStatus FIREWORK_ROCKET_DUMMY_EFFECT;

        static {
            FIREWORK_ROCKET_ID = Entity.nextEntityID++;
            FIREWORK_ROCKET_UNIQUE_ID = MathHelper.getRandomUUID(new Random());

            DESTROY_FIREWORK_ROCKET_DUMMY = new SPacketDestroyEntities(FIREWORK_ROCKET_ID);

            FIREWORK_ROCKET_DUMMY_EFFECT = new SPacketEntityStatus();
            FIREWORK_ROCKET_DUMMY_EFFECT.entityId = FIREWORK_ROCKET_ID;
            // The status index that is used to trigger the fireworks effect,
            // can be found at: EntityFireworkRocket#handleStatusUpdate
            // or: EntityFireworkRocket#onUpdate -> setEntityState
            FIREWORK_ROCKET_DUMMY_EFFECT.logicOpcode = 17;
        }

        private final SPacketEntityMetadata entityMetadataPacket;

        private CachedFireworkPacket(SPacketEntityMetadata entityMetadataPacket) {
            this.entityMetadataPacket = entityMetadataPacket;
        }

        @Override
        public void process(Vector3d position, List<Packet<?>> output) {
            final SPacketSpawnObject packetSpawnObject = new SPacketSpawnObject();
            packetSpawnObject.entityId = FIREWORK_ROCKET_ID;
            packetSpawnObject.uniqueId = FIREWORK_ROCKET_UNIQUE_ID;
            packetSpawnObject.x = position.getX();
            packetSpawnObject.y = position.getY();
            packetSpawnObject.z = position.getZ();
            // The internal id that that is used to spawn a "EntityFireworkRocket" on the client,
            // can be found at: EntityTrackerEntry#createSpawnPacket
            // or: NetHandlerPlayClient#handleSpawnObject
            packetSpawnObject.type = 76;
            output.add(packetSpawnObject);
            output.add(this.entityMetadataPacket);
            output.add(FIREWORK_ROCKET_DUMMY_EFFECT);
            output.add(DESTROY_FIREWORK_ROCKET_DUMMY);
        }
    }

    private static final class CachedParticlePacket implements ICachedParticleEffect {

        private final EnumParticleTypes particleType;
        private final Vector3f offset;
        private final int quantity;
        private final int[] extra;

        private CachedParticlePacket(EnumParticleTypes particleType, Vector3f offset, int quantity, int[] extra) {
            this.particleType = particleType;
            this.quantity = quantity;
            this.offset = offset;
            this.extra = extra;
        }

        @Override
        public void process(Vector3d position, List<Packet<?>> output) {
            final float px = (float) position.getX();
            final float py = (float) position.getY();
            final float pz = (float) position.getZ();

            final float odx = this.offset.getX();
            final float ody = this.offset.getY();
            final float odz = this.offset.getZ();

            final SPacketParticles message = new SPacketParticles(
                    this.particleType, true, px, py, pz, odx, ody, odz, 0f, this.quantity, this.extra);
            output.add(message);
        }
    }

    private static final class CachedOffsetParticlePacket implements ICachedParticleEffect {

        private final EnumParticleTypes particleType;
        private final Vector3f offsetData;
        private final Vector3f offset;
        private final int quantity;
        private final int[] extra;

        private CachedOffsetParticlePacket(EnumParticleTypes particleType, Vector3f offsetData, Vector3f offset, int quantity, int[] extra) {
            this.particleType = particleType;
            this.offsetData = offsetData;
            this.quantity = quantity;
            this.offset = offset;
            this.extra = extra;
        }

        @Override
        public void process(Vector3d position, List<Packet<?>> output) {
            final float px = (float) position.getX();
            final float py = (float) position.getY();
            final float pz = (float) position.getZ();

            final float odx = this.offsetData.getX();
            final float ody = this.offsetData.getY();
            final float odz = this.offsetData.getZ();

            if (this.offset.equals(Vector3f.ZERO)) {
                final SPacketParticles message = new SPacketParticles(
                        this.particleType, true, px, py, pz, odx, ody, odz, 1f, 0, this.extra);
                for (int i = 0; i < this.quantity; i++) {
                    output.add(message);
                }
            } else {
                final Random random = new Random();

                final float ox = this.offset.getX();
                final float oy = this.offset.getY();
                final float oz = this.offset.getZ();

                for (int i = 0; i < this.quantity; i++) {
                    final float px0 = px + (random.nextFloat() * 2f - 1f) * ox;
                    final float py0 = py + (random.nextFloat() * 2f - 1f) * oy;
                    final float pz0 = pz + (random.nextFloat() * 2f - 1f) * oz;

                    final SPacketParticles message = new SPacketParticles(
                            this.particleType, true, px0, py0, pz0, odx, ody, odz, 1f, 0, this.extra);
                    output.add(message);
                }
            }
        }
    }

    private static final class CachedEffectPacket implements ICachedParticleEffect {

        private final int type;
        private final int data;
        private final boolean broadcast;

        private CachedEffectPacket(int type, int data, boolean broadcast) {
            this.broadcast = broadcast;
            this.type = type;
            this.data = data;
        }

        @Override
        public void process(Vector3d position, List<Packet<?>> output) {
            final BlockPos blockPos = new BlockPos(position.getFloorX(), position.getFloorY(), position.getFloorZ());
            output.add(new SPacketEffect(this.type, blockPos, this.data, this.broadcast));
        }
    }

    private SpongeParticleHelper() {
    }
}
