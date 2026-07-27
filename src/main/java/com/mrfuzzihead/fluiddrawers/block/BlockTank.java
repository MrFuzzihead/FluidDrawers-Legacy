package com.mrfuzzihead.fluiddrawers.block;

import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

import com.mrfuzzihead.fluiddrawers.FluidDrawersCreativeTab;
import com.mrfuzzihead.fluiddrawers.init.ModBlocks;
import com.mrfuzzihead.fluiddrawers.tile.TileTank;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class BlockTank extends Block implements ITileEntityProvider {

    @SideOnly(Side.CLIENT)
    private IIcon iconTank;

    @SideOnly(Side.CLIENT)
    private IIcon iconGlass;

    public BlockTank() {
        super(Material.iron);
        this.isBlockContainer = true;
        this.setBlockName("fluiddrawers.tank");
        this.setHardness(5.0F);
        this.setStepSound(soundTypeMetal);
        this.setCreativeTab(FluidDrawersCreativeTab.INSTANCE);
    }

    // Custom render type -> BlockTankRenderer (registered in ClientProxy). Returns 0 on the
    // dedicated server (tankRenderId is only allocated client-side), which is fine since the
    // server never renders. Ports the 1.12.2 getRenderType() override.
    @Override
    public int getRenderType() {
        return ModBlocks.tankRenderId;
    }

    // Ports the 1.12.2 func_149662_c / func_149686_d overrides. The hollow frame must NOT be
    // treated as a full opaque cube, otherwise neighboring faces behind the glass get culled and
    // you see a black box through the glass, and AO/lighting is computed for a solid cube.
    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    @Override
    public boolean renderAsNormalBlock() {
        return false;
    }

    // Render across both passes: metal frame in pass 0 (solid), glass interior in pass 1 (alpha).
    // canRenderInPass(true) for every pass lets the chunk renderer call renderWorldBlock in both,
    // and BlockTankRenderer dispatches on ForgeHooksClient.getWorldRenderPass().
    @Override
    public int getRenderBlockPass() {
        return 1;
    }

    @Override
    public boolean canRenderInPass(int pass) {
        return true;
    }

    // --- ITileEntityProvider ---

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TileTank();
    }

    @Override
    public void breakBlock(World world, int x, int y, int z, Block block, int meta) {
        super.breakBlock(world, x, y, z, block, meta);
        world.removeTileEntity(x, y, z);
    }

    @Override
    public boolean onBlockEventReceived(World world, int x, int y, int z, int eventNum, int eventArg) {
        super.onBlockEventReceived(world, x, y, z, eventNum, eventArg);
        TileEntity tile = world.getTileEntity(x, y, z);
        return tile != null && tile.receiveClientEvent(eventNum, eventArg);
    }

    // --- icons ---

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister reg) {
        this.iconTank = reg.registerIcon("fluiddrawers:tank");
        // Vanilla glass block texture (1.12.2 model references "minecraft:blocks/glass").
        this.iconGlass = reg.registerIcon("minecraft:glass");
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int meta) {
        return iconTank;
    }

    @SideOnly(Side.CLIENT)
    public IIcon getIconTank() {
        return iconTank;
    }

    @SideOnly(Side.CLIENT)
    public IIcon getIconGlass() {
        return iconGlass;
    }
}
