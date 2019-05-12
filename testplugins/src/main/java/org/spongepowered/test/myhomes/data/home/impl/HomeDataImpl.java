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
package org.spongepowered.test.myhomes.data.home.impl;

import org.spongepowered.test.myhomes.HomeKeys;
import org.spongepowered.test.myhomes.MyHomes;
import org.spongepowered.test.myhomes.data.home.Home;
import org.spongepowered.test.myhomes.data.home.HomeData;
import org.spongepowered.test.myhomes.data.home.ImmutableHomeData;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.DataContainer;
import org.spongepowered.api.data.DataHolder;
import org.spongepowered.api.data.DataQuery;
import org.spongepowered.api.data.DataView;
import org.spongepowered.api.data.manipulator.mutable.common.AbstractData;
import org.spongepowered.api.data.merge.MergeFunction;
import org.spongepowered.api.data.value.mutable.MapValue;
import org.spongepowered.api.data.value.mutable.Value;

import java.util.Map;
import java.util.Optional;

import javax.annotation.Nullable;

public class HomeDataImpl extends AbstractData<HomeData, ImmutableHomeData> implements HomeData {

    private Home defaultHome;
    private Map<String, Home> homes;

    public HomeDataImpl(Home defaultHome, Map<String, Home> homes) {
        this.defaultHome = defaultHome;
        this.homes = homes;
    }

    // It's best to provide an empty constructor with "default" values
    HomeDataImpl() {
        this(null, ImmutableMap.of());
    }

    // Override if you have a separate interface
    @Override
    public Value<Home> defaultHome() {
        return Sponge.getRegistry().getValueFactory()
                .createValue(HomeKeys.DEFAULT_HOME, this.defaultHome, null);
    }

    // Override if you have a separate interface
    @Override
    public MapValue<String, Home> homes() {
        return Sponge.getRegistry().getValueFactory()
                .createMapValue(HomeKeys.HOMES, this.homes, ImmutableMap.of());
    }

    private Home getDefaultHome() {
        return this.defaultHome;
    }

    private Map<String, Home> getHomes() {
        return Maps.newHashMap(this.homes);
    }

    private void setDefaultHome(@Nullable Home defaultHome) {
        this.defaultHome = defaultHome;
    }

    private void setHomes(Map<String, Home> homes) {
        Preconditions.checkNotNull(homes);
        this.homes = Maps.newHashMap(homes);
    }

    @Override
    protected void registerGettersAndSetters() {
        registerKeyValue(HomeKeys.DEFAULT_HOME, this::defaultHome);
        registerKeyValue(HomeKeys.HOMES, this::homes);

        registerFieldGetter(HomeKeys.DEFAULT_HOME, this::getDefaultHome);
        registerFieldGetter(HomeKeys.HOMES, this::getHomes);

        registerFieldSetter(HomeKeys.DEFAULT_HOME, this::setDefaultHome);
        registerFieldSetter(HomeKeys.HOMES, this::setHomes);
    }

    @Override
    public int getContentVersion() {
        // Update whenever the serialization format changes
        return HomeDataBuilder.CONTENT_VERSION;
    }

    @Override
    public ImmutableHomeData asImmutable() {
        return new ImmutableHomeDataImpl(this.defaultHome, this.homes);
    }

    // Only required on mutable implementations
    @Override
    public Optional<HomeData> fill(DataHolder dataHolder, MergeFunction overlap) {
        HomeData merged = overlap.merge(this, dataHolder.get(HomeData.class).orElse(null));
        this.defaultHome = merged.defaultHome().get();
        this.homes = merged.homes().get();

        return Optional.of(this);
    }

    // Only required on mutable implementations
    @Override
    public Optional<HomeData> from(DataContainer container) {
        if (!container.contains(HomeKeys.DEFAULT_HOME, HomeKeys.HOMES)) {
            return Optional.empty();
        }
        // Loads the structure defined in toContainer
        this.defaultHome = container.getSerializable(HomeKeys.DEFAULT_HOME.getQuery(), Home.class).get();

        // Loads the map of homes
        this.homes = Maps.newHashMap();
        DataView homes = container.getView(HomeKeys.HOMES.getQuery()).get();
        for (DataQuery homeQuery : homes.getKeys(false)) {
            homes.getSerializable(homeQuery, Home.class)
                    .ifPresent(home -> this.homes.put(homeQuery.toString(), home));
        }

        return Optional.of(this);
    }

    @Override
    public HomeData copy() {
        return new HomeDataImpl(this.defaultHome, this.homes);
    }

    @Override
    public DataContainer toContainer() {
        DataContainer container = super.toContainer();
        // This is the simplest, but use whatever structure you want!
        if(this.defaultHome != null) {
            container.set(HomeKeys.DEFAULT_HOME, this.defaultHome);
        }
        container.set(HomeKeys.HOMES, this.homes);

        return container;
    }
}
