package com.mrfuzzihead.fluiddrawers.init;

import com.mrfuzzihead.fluiddrawers.block.BlockTank;

import cpw.mods.fml.common.registry.GameRegistry;

public class ModBlocks {

    public static BlockTank TANK;

    public static void init() {
        TANK = new BlockTank();
        GameRegistry.registerBlock(TANK, "tank");
    }
}
