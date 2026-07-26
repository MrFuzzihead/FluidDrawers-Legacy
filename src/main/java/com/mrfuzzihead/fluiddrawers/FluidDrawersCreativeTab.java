package com.mrfuzzihead.fluiddrawers;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;

import com.mrfuzzihead.fluiddrawers.init.ModBlocks;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class FluidDrawersCreativeTab extends CreativeTabs {

    public static final FluidDrawersCreativeTab INSTANCE = new FluidDrawersCreativeTab();

    public FluidDrawersCreativeTab() {
        super("fluiddrawers");
    }

    @Override
    @SideOnly(Side.CLIENT)
    public Item getTabIconItem() {
        return Item.getItemFromBlock(ModBlocks.TANK);
    }
}
