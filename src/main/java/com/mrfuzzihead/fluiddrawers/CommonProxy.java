package com.mrfuzzihead.fluiddrawers;

import com.mrfuzzihead.fluiddrawers.init.FdGuis;
import com.mrfuzzihead.fluiddrawers.init.ModBlocks;
import com.mrfuzzihead.fluiddrawers.init.ModRecipes;
import com.mrfuzzihead.fluiddrawers.tile.TileTank;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLInterModComms;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.registry.GameRegistry;

public class CommonProxy {

    public static final String WAILA = "Waila";

    public void preInit(FMLPreInitializationEvent event) {
        Config.synchronizeConfiguration(event.getSuggestedConfigurationFile());

        ModBlocks.init();

        GameRegistry.registerTileEntity(TileTank.class, FluidDrawers.MODID + "_tile");
    }

    public void init(FMLInitializationEvent event) {
        FdGuis.init();
        ModRecipes.init();

        // Register WAILA integration if WAILA is loaded (compileOnly + runtimeOnly dep).
        if (Loader.isModLoaded(WAILA)) {
            FMLInterModComms.sendMessage(
                WAILA,
                "register",
                "com.mrfuzzihead.fluiddrawers.integration.WailaIntegration.registerProvider");
        }
    }

    public void postInit(FMLPostInitializationEvent event) {}

    public void serverStarting(FMLServerStartingEvent event) {}
}
