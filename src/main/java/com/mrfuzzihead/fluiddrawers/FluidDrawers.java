package com.mrfuzzihead.fluiddrawers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;

@Mod(
    modid = FluidDrawers.MODID,
    version = Tags.VERSION,
    name = FluidDrawers.MODNAME,
    dependencies = FluidDrawers.DEPENDENCIES,
    acceptedMinecraftVersions = "[1.7.10]")
public class FluidDrawers {

    public static final String MODID = "fluiddrawers";
    public static final String MODNAME = "Fluid Drawers";
    public static final String DEPENDENCIES = "after:StorageDrawers;after:waila;after:framedcompactdrawers";
    public static final Logger LOG = LogManager.getLogger(MODID);

    @Mod.Instance(FluidDrawers.MODID)
    public static FluidDrawers instance;

    @SidedProxy(
        clientSide = "com.mrfuzzihead.fluiddrawers.ClientProxy",
        serverSide = "com.mrfuzzihead.fluiddrawers.CommonProxy")
    public static CommonProxy proxy;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        proxy.preInit(event);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init(event);
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        proxy.postInit(event);
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        proxy.serverStarting(event);
    }
}
