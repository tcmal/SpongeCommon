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
package org.spongepowered.common.item.recipe.crafting;

import static com.google.common.base.Preconditions.checkState;

import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import org.spongepowered.api.CatalogKey;
import org.spongepowered.api.item.inventory.crafting.CraftingGridInventory;
import org.spongepowered.api.item.recipe.crafting.CraftingRecipe;
import org.spongepowered.api.item.recipe.crafting.CraftingRecipeRegistry;
import org.spongepowered.api.item.recipe.crafting.Ingredient;
import org.spongepowered.api.world.World;
import org.spongepowered.common.SpongeImplHooks;
import org.spongepowered.common.registry.RegistryHelper;
import org.spongepowered.common.registry.SpongeAdditionalCatalogRegistryModule;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;


/**
 * Implementation of the CraftingRecipeRegistry.
 * Proxy for {@link CraftingManager}
 */
@SuppressWarnings("deprecation")
public class SpongeCraftingRecipeRegistry implements CraftingRecipeRegistry,
        SpongeAdditionalCatalogRegistryModule<CraftingRecipe> {

    public static SpongeCraftingRecipeRegistry getInstance() {
        return Holder.INSTANCE;
    }

    SpongeCraftingRecipeRegistry() {
    }

    @Override
    public Collection<CraftingRecipe> getAll() {
        return SpongeImplHooks.getCraftingRecipes().stream()
                .map(recipe -> {
                    // Unwrap delegate recipes
                    if (recipe instanceof DelegateSpongeCraftingRecipe) {
                        return ((DelegateSpongeCraftingRecipe) recipe).getDelegate();
                    }
                    return recipe;
                })
                .collect(Collectors.toList());
    }

    @Override
    public Optional<CraftingRecipe> findMatchingRecipe(CraftingGridInventory inventory, World world) {
        return SpongeImplHooks.findMatchingRecipe(inventory, world);
    }

    @Override
    public boolean allowsApiRegistration() {
        // Only allow the SpongeGameRegistryRegisterEvent be automatically called in vanilla,
        // a custom event is thrown in the forge environment.
        return SpongeImplHooks.isVanilla();
    }

    @Override
    public void registerDefaults() {
        RegistryHelper.setFinalStatic(Ingredient.class, "NONE", net.minecraft.item.crafting.Ingredient.EMPTY);
    }

    @Override
    public void registerAdditionalCatalog(CraftingRecipe recipe) {
        checkState(SpongeImplHooks.isVanilla()); // Should only be called in vanilla
        if (!(recipe instanceof IRecipe)) {
            recipe = new DelegateSpongeCraftingRecipe(recipe);
        }
        CraftingManager.register(recipe.getKey().toString(), (IRecipe) recipe);
    }

    @Override
    public Optional<CraftingRecipe> getById(String id) {
        return SpongeImplHooks.getRecipeById(id);
    }

    @Override
    public Optional<CraftingRecipe> get(CatalogKey key) {
        return SpongeImplHooks.getRecipeById(key);
    }

    private static final class Holder {
        static final SpongeCraftingRecipeRegistry INSTANCE = new SpongeCraftingRecipeRegistry();
    }
}
