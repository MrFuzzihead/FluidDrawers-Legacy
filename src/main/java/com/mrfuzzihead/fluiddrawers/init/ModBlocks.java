package com.mrfuzzihead.fluiddrawers.init;

import com.mrfuzzihead.fluiddrawers.block.BlockTank;
import com.mrfuzzihead.fluiddrawers.item.block.ItemBlockTank;

import cpw.mods.fml.common.registry.GameRegistry;

public class ModBlocks {

    public static BlockTank TANK;

    // Allocated client-side in ClientProxy.init() via RenderingRegistry.getNextAvailableRenderId().
    // Stays 0 on the dedicated server (which never renders); BlockTank.getRenderType() reads this.
    public static int tankRenderId = 0;

    public static void init() {
        TANK = new BlockTank();
        // Register with our custom ItemBlockTank (Phase 12) so break/place preserves NBT.
        GameRegistry.registerBlock(TANK, ItemBlockTank.class, "tank");
    }
}
