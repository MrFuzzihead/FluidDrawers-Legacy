package com.mrfuzzihead.fluiddrawers;

import com.mrfuzzihead.fluiddrawers.init.FdGuis;
import com.mrfuzzihead.fluiddrawers.init.ModBlocks;
import com.mrfuzzihead.fluiddrawers.tile.TileTank;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.registry.GameRegistry;

public class CommonProxy {

    public void preInit(FMLPreInitializationEvent event) {
        Config.synchronizeConfiguration(event.getSuggestedConfigurationFile());

        ModBlocks.init();

        GameRegistry.registerTileEntity(TileTank.class, "fluiddrawers_tile");

        FluidDrawers.LOG.info("I am FluidDrawers at version " + Tags.VERSION);
    }

    public void init(FMLInitializationEvent event) {
        FdGuis.init();
    }

    public void postInit(FMLPostInitializationEvent event) {}

    public void serverStarting(FMLServerStartingEvent event) {}
}
