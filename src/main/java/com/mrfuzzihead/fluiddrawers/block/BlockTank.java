package com.mrfuzzihead.fluiddrawers.block;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.util.IIcon;

import com.mrfuzzihead.fluiddrawers.FluidDrawersCreativeTab;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class BlockTank extends Block {

    @SideOnly(Side.CLIENT)
    private IIcon iconTank;

    public BlockTank() {
        super(Material.iron);
        this.setBlockName("fluiddrawers.tank");
        this.setHardness(5.0F);
        this.setStepSound(soundTypeMetal);
        this.setCreativeTab(FluidDrawersCreativeTab.INSTANCE);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister reg) {
        this.iconTank = reg.registerIcon("fluiddrawers:tank");
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int meta) {
        return iconTank;
    }
}
