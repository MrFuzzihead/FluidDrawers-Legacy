package com.mrfuzzihead.fluiddrawers;

import net.minecraft.item.Item;
import net.minecraftforge.client.MinecraftForgeClient;

import com.mrfuzzihead.fluiddrawers.client.renderer.BlockTankRenderer;
import com.mrfuzzihead.fluiddrawers.client.renderer.ItemRendererTank;
import com.mrfuzzihead.fluiddrawers.client.tesr.RenderTileTank;
import com.mrfuzzihead.fluiddrawers.init.ModBlocks;
import com.mrfuzzihead.fluiddrawers.tile.TileTank;

import cpw.mods.fml.client.registry.ClientRegistry;
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

        // Bind the fluid TESR (Phase 7) -- draws the stored fluid inside the glass frame.
        ClientRegistry.bindTileEntitySpecialRenderer(TileTank.class, new RenderTileTank());

        // Register the custom item renderer (Phase 11/12 overlay fix) -- shows the tape overlay
        // on sealed tank items in inventory/hand/dropped (1.12.2 tank_sealed.json equivalent).
        MinecraftForgeClient.registerItemRenderer(Item.getItemFromBlock(ModBlocks.TANK), new ItemRendererTank());
    }
}
