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
package org.spongepowered.common.world.extent;

import com.flowpowered.math.vector.Vector3i;
import org.spongepowered.api.util.DiscreteTransform3;
import org.spongepowered.api.world.biome.BiomeType;
import org.spongepowered.api.world.extent.Volume;
import org.spongepowered.api.world.biome.ImmutableBiomeVolume;
import org.spongepowered.api.world.biome.UnmodifiableBiomeVolume;
import org.spongepowered.api.world.biome.worker.BiomeVolumeWorker;
import org.spongepowered.common.world.extent.worker.SpongeBiomeVolumeWorker;

public class UnmodifiableBiomeVolumeWrapper<V extends BlockVolume> implements UnmodifiableBiomeVolume<UnmodifiableBlockVolumeWrapper<V>> {

    private final V volume;

    public UnmodifiableBiomeVolumeWrapper(V volume) {
        this.volume = volume;
    }

    @Override
    public Vector3i getBiomeMin() {
        return this.volume.getBiomeMin();
    }

    @Override
    public Vector3i getBiomeMax() {
        return this.volume.getBiomeMax();
    }

    @Override
    public Vector3i getBiomeSize() {
        return this.volume.getBiomeSize();
    }

    @Override
    public boolean containsBiome(int x, int y, int z) {
        return this.volume.containsBiome(x, y, z);
    }

    @Override
    public BiomeType getBiome(int x, int y, int z) {
        return this.volume.getBiome(x, y, z);
    }

    @Override
    public UnmodifiableBiomeVolume getBiomeView(Vector3i newMin, Vector3i newMax) {
        return new UnmodifiableBiomeVolumeWrapper(this.volume.getBiomeView(newMin, newMax));
    }

    @Override
    public UnmodifiableBiomeVolume getBiomeView(DiscreteTransform3 transform) {
        return new UnmodifiableBiomeVolumeWrapper(this.volume.getBiomeView(transform));
    }

    @Override
    public BiomeVolumeWorker<UnmodifiableBlockVolumeWrapper<V>, ?> getBiomeWorker() {
        return new SpongeBiomeVolumeWorker<>(this);
    }

    @Override
    public ImmutableBiomeVolume asImmutableBiomeVolume() {
        return null;
    }

    @Override
    public Vector3i getBlockMin() {
        return null;
    }

    @Override
    public Vector3i getBlockMax() {
        return null;
    }

    @Override
    public Vector3i getBlockSize() {
        return null;
    }

    @Override
    public boolean containsBlock(int x, int y, int z) {
        return false;
    }

    @Override
    public boolean isAreaAvailable(int x, int y, int z) {
        return false;
    }

    @Override
    public Volume getView(Vector3i newMin, Vector3i newMax) {
        return null;
    }
}
