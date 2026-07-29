package com.mrfuzzihead.fluiddrawers.init;

import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.ShapedOreRecipe;

import com.mrfuzzihead.fluiddrawers.FluidDrawers;

import cpw.mods.fml.common.registry.GameRegistry;

/**
 * Ports the 1.12.2 {@code assets/fluiddrawers/recipes/tank.json} {@code "forge:ore_shaped"}
 * recipe to 1.7.10 via {@link ShapedOreRecipe} and {@link GameRegistry#addRecipe}.
 *
 * <p>
 * Ingredients (from the 1.12.2 recipe):
 * </p>
 * <table>
 * <tr>
 * <th>Key</th>
 * <th>Ingredient</th>
 * <th>1.7.10 mapping</th>
 * </tr>
 * <tr>
 * <td>G</td>
 * <td>{@code paneGlass} (ore dict)</td>
 * <td>{@code "paneGlass"}</td>
 * </tr>
 * <tr>
 * <td>P</td>
 * <td>{@code heavy_weighted_pressure_plate}</td>
 * <td>{@link Blocks#heavy_weighted_pressure_plate}</td>
 * </tr>
 * <tr>
 * <td>B</td>
 * <td>{@code bucket}</td>
 * <td>{@link Items#bucket}</td>
 * </tr>
 * </table>
 *
 * <p>
 * Pattern:
 * </p>
 *
 * <pre>
 * GPG
 * GBG
 * GPG
 * </pre>
 */
public final class ModRecipes {

    private ModRecipes() {}

    /** Register all Fluid Drawers recipes. Call from CommonProxy.init(). */
    public static void init() {
        FluidDrawers.LOG.info("Registering recipes");

        GameRegistry.addRecipe(
            new ShapedOreRecipe(
                new ItemStack(ModBlocks.TANK),
                "GPG",
                "GBG",
                "GPG",
                'G',
                "paneGlass",
                'P',
                new ItemStack(Blocks.heavy_weighted_pressure_plate),
                'B',
                Items.bucket));
    }
}
