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

import static org.spongepowered.api.command.args.GenericArguments.onlyOne;
import static org.spongepowered.api.command.args.GenericArguments.optional;
import static org.spongepowered.api.command.args.GenericArguments.player;
import static org.spongepowered.api.command.args.GenericArguments.string;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.ArgumentParseException;
import org.spongepowered.api.command.args.ChildCommandElementExecutor;
import org.spongepowered.api.command.args.CommandArgs;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.data.DataManager;
import org.spongepowered.api.data.DataRegistration;
import org.spongepowered.api.data.key.Key;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.GameRegistryEvent;
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.chat.ChatTypes;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.format.TextStyles;
import org.spongepowered.api.text.translation.ResourceBundleTranslation;
import org.spongepowered.test.LoadableModule;
import org.spongepowered.test.myhomes.data.friends.FriendsData;
import org.spongepowered.test.myhomes.data.friends.ImmutableFriendsData;
import org.spongepowered.test.myhomes.data.friends.impl.FriendsDataBuilder;
import org.spongepowered.test.myhomes.data.friends.impl.FriendsDataImpl;
import org.spongepowered.test.myhomes.data.friends.impl.ImmutableFriendsDataImpl;
import org.spongepowered.test.myhomes.data.home.Home;
import org.spongepowered.test.myhomes.data.home.HomeData;
import org.spongepowered.test.myhomes.data.home.ImmutableHomeData;
import org.spongepowered.test.myhomes.data.home.impl.HomeBuilder;
import org.spongepowered.test.myhomes.data.home.impl.HomeDataBuilder;
import org.spongepowered.test.myhomes.data.home.impl.HomeDataImpl;
import org.spongepowered.test.myhomes.data.home.impl.ImmutableHomeDataImpl;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.Function;

import javax.annotation.Nullable;

@Plugin(id = "myhomes", name = "MyHomes", version = "0.0.0", description = "A simple homes plugin")
public class MyHomes implements LoadableModule {

    public static final Text HOME_NAME = Text.of(TextColors.DARK_GREEN, "name");
    public static final Text PLAYER = Text.of(TextColors.BLUE, "player");
    @Inject private PluginContainer container;
    @Inject private Logger logger;

    private static final CommandElement DUMMY_ELEMENT = new CommandElement(Text.EMPTY) {
        @Nullable
        @Override protected Object parseValue(CommandSource source, CommandArgs args) throws ArgumentParseException {
            throw args.createError(t("No subcommand was specified")); // this will never be visible, but just in case
        }

        @Override public List<String> complete(CommandSource src, CommandArgs args, CommandContext context) {
            return ImmutableList.of();
        }
    };

    private static final Function<Locale, ResourceBundle> LOOKUP_FUNC = new Function<Locale, ResourceBundle>() {
        @Nullable
        @Override
        public ResourceBundle apply(Locale input) {
            return ResourceBundle.getBundle("org.spongepowered.common.Translations", input);
        }
    };

    /**
     * Get the translated text for a given string.
     *
     * @param key The translation key
     * @param args Translation parameters
     * @return The translatable text
     */
    public static Text t(String key, Object... args) {
        return Text.of(new ResourceBundleTranslation(key, LOOKUP_FUNC), args);
    }

    private DataRegistration<FriendsData, ImmutableFriendsData> friendsDataRegistration;
    private DataRegistration<HomeData, ImmutableHomeData> homeDataRegistration;

    private final MyHomeListener listener = new MyHomeListener();


    @Listener
    public void onGameInit(GameInitializationEvent e) {
        final ChildCommandElementExecutor nonFlagChildren = new ChildCommandElementExecutor(null, DUMMY_ELEMENT, true);
        nonFlagChildren.register(CommandSpec.builder()
            .description(Text.of("Sets a new home"))
            .permission("myhomes.create")
            .arguments(optional(onlyOne(player(PLAYER))), string(HOME_NAME))
            .executor((src, args) -> {
                final Optional<String> one = args.getOne(HOME_NAME);
                if (!one.isPresent()) {
                    return CommandResult.empty();
                }
                // todo - create a home
                if (src instanceof Player) {
                    final Home playerHome = new Home(((Player) src).getTransform(), one.get());
                    final HomeData data = ((Player) src).get(HomeKeys.HOME_DATA.getManipulatorClass())
                        .orElseGet(() -> new HomeDataImpl(playerHome, new HashMap<>()));


                }
                return CommandResult.success();
            })
                .build());
        Sponge.getCommandManager().register(this, CommandSpec.builder()
                .description(Text.of(TextColors.DARK_AQUA, "Go Home"))
                .arguments(GenericArguments.firstParsing(nonFlagChildren))
                .executor(nonFlagChildren).build(),
            "home", "myhome"
        );
    }
    @Listener
    public void onKeyRegistration(GameRegistryEvent.Register<Key<?>> event) {
        this.logger.info("onKeyRegistration");
        event.register(HomeKeys.DEFAULT_HOME);
        event.register(HomeKeys.HOMES);
        event.register(HomeKeys.FRIENDS);
    }

    @Listener
    public void onDataRegistration(GameRegistryEvent.Register<DataRegistration<?, ?>> event) {
        this.logger.info("onDataRegistration");
        final DataManager dataManager = Sponge.getDataManager();
        // Home stuff
        dataManager.registerBuilder(Home.class, new HomeBuilder());
        dataManager.registerContentUpdater(Home.class, new HomeBuilder.NameUpdater());
        dataManager.registerContentUpdater(HomeData.class, new HomeDataBuilder.HomesUpdater());

        event.register(HomeKeys.HOME_DATA);
    }



    @Override
    public void enable(CommandSource src) {
        Sponge.getEventManager().registerListeners(this.container, this.listener);
    }

    public static class MyHomeListener {

        @Listener
        public void onClientConnectionJoin(ClientConnectionEvent.Join event) {
            Player player = event.getTargetEntity();
            player.get(HomeKeys.DEFAULT_HOME).ifPresent(home -> {
                player.setTransform(home.getTransform());
                player.sendMessage(ChatTypes.ACTION_BAR, Text.of("Teleported to home - ", TextStyles.BOLD, home.getName()));
            });
        }

    }
}
