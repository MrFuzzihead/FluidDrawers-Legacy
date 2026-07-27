package com.mrfuzzihead.fluiddrawers;

import com.mrfuzzihead.fluiddrawers.client.renderer.BlockTankRenderer;
import com.mrfuzzihead.fluiddrawers.init.ModBlocks;

import cpw.mods.fml.client.registry.RenderingRegistry;
import cpw.mods.fml.common.event.FMLInitializationEvent;

public class ClientProxy extends CommonProxy {

    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);

        // Allocate the custom block render id and register the hollow-frame ISBRH.
        // Block registration happened in CommonProxy.preInit(); getRenderType() is only read at
        // render time, so allocating the id here (init) is in time.
        ModBlocks.tankRenderId = RenderingRegistry.getNextAvailableRenderId();
        RenderingRegistry.registerBlockHandler(ModBlocks.tankRenderId, new BlockTankRenderer());
    }
}
