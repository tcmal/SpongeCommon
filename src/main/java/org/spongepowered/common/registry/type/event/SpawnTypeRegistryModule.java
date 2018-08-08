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
package org.spongepowered.common.registry.type.event;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableSet;
import org.spongepowered.api.CatalogKey;
import org.spongepowered.api.event.cause.entity.spawn.SpawnType;
import org.spongepowered.api.event.cause.entity.spawn.SpawnTypes;
import org.spongepowered.api.registry.AdditionalCatalogRegistryModule;
import org.spongepowered.api.registry.AlternateCatalogRegistryModule;
import org.spongepowered.api.registry.util.RegisterCatalog;

import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class SpawnTypeRegistryModule implements AlternateCatalogRegistryModule<SpawnType>, AdditionalCatalogRegistryModule<SpawnType> {

    @RegisterCatalog(SpawnTypes.class)
    private final Map<String, SpawnType> spawnTypeMap = new HashMap<>();

    @Override
    public void registerAdditionalCatalog(SpawnType extraCatalog) {
        checkArgument(!this.spawnTypeMap.containsKey(extraCatalog.getKey().toString().toLowerCase(Locale.ENGLISH)),
                "SpawnType with the same id is already registered: {}", extraCatalog.getKey().toString());
        this.spawnTypeMap.put(extraCatalog.getKey().toString().toLowerCase(Locale.ENGLISH), extraCatalog);
    }

    @Override
    public Optional<SpawnType> getById(String id) {
        String key = checkNotNull(id).toLowerCase(Locale.ENGLISH);
        if (!key.contains(":")) {
            key = "sponge:" + key; // There are no minecraft based spawn types.
        }
        return Optional.ofNullable(this.spawnTypeMap.get(key));
    }

    @Override
    public Optional<SpawnType> get(CatalogKey key) {
        return getById(key.toString());
    }

    @Override
    public Collection<SpawnType> getAll() {
        return ImmutableSet.copyOf(this.spawnTypeMap.values());
    }

    @Override
    public void registerDefaults() {
        this.spawnTypeMap.put("sponge:block_spawning", InternalSpawnTypes.BLOCK_SPAWNING);
        this.spawnTypeMap.put("sponge:breeding", InternalSpawnTypes.BREEDING);
        this.spawnTypeMap.put("sponge:dispense", InternalSpawnTypes.DISPENSE);
        this.spawnTypeMap.put("sponge:dropped_item", InternalSpawnTypes.DROPPED_ITEM);
        this.spawnTypeMap.put("sponge:experience", InternalSpawnTypes.EXPERIENCE);
        this.spawnTypeMap.put("sponge:falling_block", InternalSpawnTypes.FALLING_BLOCK);
        this.spawnTypeMap.put("sponge:mob_spawner", InternalSpawnTypes.MOB_SPAWNER);
        this.spawnTypeMap.put("sponge:passive", InternalSpawnTypes.PASSIVE);
        this.spawnTypeMap.put("sponge:placement", InternalSpawnTypes.PLACEMENT);
        this.spawnTypeMap.put("sponge:projectile", InternalSpawnTypes.PROJECTILE);
        this.spawnTypeMap.put("sponge:spawn_egg", InternalSpawnTypes.SPAWN_EGG);
        this.spawnTypeMap.put("sponge:structure", InternalSpawnTypes.STRUCTURE);
        this.spawnTypeMap.put("sponge:tnt_ignite", InternalSpawnTypes.TNT_IGNITE);
        this.spawnTypeMap.put("sponge:weather", InternalSpawnTypes.WEATHER);
        this.spawnTypeMap.put("sponge:custom", InternalSpawnTypes.CUSTOM);
        this.spawnTypeMap.put("sponge:chunk_load", InternalSpawnTypes.CHUNK_LOAD);
        this.spawnTypeMap.put("sponge:world_spawner", InternalSpawnTypes.WORLD_SPAWNER);
        this.spawnTypeMap.put("sponge:plugin", InternalSpawnTypes.PLUGIN);
    }

    @Override
    public Map<String, SpawnType> provideCatalogMap() {
        final HashMap<String, SpawnType> map = new HashMap<>();
        for (Map.Entry<String, SpawnType> entry : this.spawnTypeMap.entrySet()) {
            map.put(entry.getKey().replace("sponge:", ""), entry.getValue());
        }
        return map;
    }
}
