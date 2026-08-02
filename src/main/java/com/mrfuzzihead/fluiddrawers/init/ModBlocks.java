package com.mrfuzzihead.fluiddrawers.init;

import com.mrfuzzihead.fluiddrawers.block.BlockTank;
import com.mrfuzzihead.fluiddrawers.block.BlockTankCustom;
import com.mrfuzzihead.fluiddrawers.item.block.ItemBlockTank;
import com.mrfuzzihead.fluiddrawers.item.block.ItemBlockTankCustom;

import cpw.mods.fml.common.registry.GameRegistry;

public class ModBlocks {

    public static BlockTank TANK;
    public static BlockTankCustom TANK_CUSTOM;

    // Allocated client-side in ClientProxy.init() via RenderingRegistry.getNextAvailableRenderId().
    // Stays 0 on the dedicated server (which never renders); BlockTank.getRenderType() reads this.
    public static int tankRenderId = 0;
    public static int tankCustomRenderId = 0;

    public static void init() {
        TANK = new BlockTank();
        // Register with our custom ItemBlockTank (Phase 12) so break/place preserves NBT.
        GameRegistry.registerBlock(TANK, ItemBlockTank.class, "tank");

        TANK_CUSTOM = new BlockTankCustom();
        GameRegistry.registerBlock(TANK_CUSTOM, ItemBlockTankCustom.class, "tank_custom");
    }
}
