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
package org.spongepowered.test.myhomes;

import com.google.common.reflect.TypeToken;
import org.spongepowered.api.data.DataQuery;
import org.spongepowered.api.data.DataRegistration;
import org.spongepowered.api.data.key.Key;
import org.spongepowered.api.data.value.mutable.ListValue;
import org.spongepowered.api.data.value.mutable.MapValue;
import org.spongepowered.api.data.value.mutable.Value;
import org.spongepowered.test.myhomes.data.friends.FriendsData;
import org.spongepowered.test.myhomes.data.friends.ImmutableFriendsData;
import org.spongepowered.test.myhomes.data.friends.impl.FriendsDataBuilder;
import org.spongepowered.test.myhomes.data.friends.impl.FriendsDataImpl;
import org.spongepowered.test.myhomes.data.friends.impl.ImmutableFriendsDataImpl;
import org.spongepowered.test.myhomes.data.home.Home;
import org.spongepowered.test.myhomes.data.home.HomeData;
import org.spongepowered.test.myhomes.data.home.ImmutableHomeData;
import org.spongepowered.test.myhomes.data.home.impl.HomeDataBuilder;
import org.spongepowered.test.myhomes.data.home.impl.HomeDataImpl;
import org.spongepowered.test.myhomes.data.home.impl.ImmutableHomeDataImpl;

import java.util.UUID;

@SuppressWarnings("UnstableApiUsage")
public class HomeKeys {

    public static final DataRegistration<FriendsData, ImmutableFriendsData> FRIENDS_DATA = DataRegistration.builder()
        .dataClass(FriendsData.class)
        .immutableClass(ImmutableFriendsData.class)
        .dataImplementation(FriendsDataImpl.class)
        .immutableImplementation(ImmutableFriendsDataImpl.class)
        .builder(new FriendsDataBuilder())
        .name("Friends Data")
        .id("friends")
        .build();

    public static final DataRegistration<HomeData, ImmutableHomeData> HOME_DATA = DataRegistration.builder()
        .dataClass(HomeData .class)
            .immutableClass(ImmutableHomeData .class)
            .dataImplementation(HomeDataImpl .class)
            .immutableImplementation(ImmutableHomeDataImpl .class)
            .builder(new HomeDataBuilder())
            .name("Home Data")
            .id("home")
            .build();

    public static final Key<Value<Home>> DEFAULT_HOME = Key.builder()
        .type(new TypeToken<Value<Home>>() {
            public static final long serialVersionUID = 1L;
        })
        .id("default_home")
        .name("Default Home")
        .query(DataQuery.of("DefaultHome"))
        .build();

    public static final Key<MapValue<String, Home>> HOMES = Key.builder()
        .type(new TypeToken<MapValue<String, Home>>() {
            public static final long serialVersionUID = 1L;
        })
        .id("homes")
        .name("Homes")
        .query(DataQuery.of("Homes"))
        .build();

    public static final Key<ListValue<UUID>> FRIENDS = Key.builder()
        .type(new TypeToken<ListValue<UUID>>() {
            public static final long serialVersionUID = 1L;
        })
        .id("friends")
        .name("Friends")
        .query(DataQuery.of("Friends"))
        .build();


}
