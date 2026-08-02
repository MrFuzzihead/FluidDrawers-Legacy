package com.mrfuzzihead.fluiddrawers.block;

import java.util.ArrayList;

import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

import com.mrfuzzihead.fluiddrawers.FluidDrawers;
import com.mrfuzzihead.fluiddrawers.init.ModBlocks;
import com.mrfuzzihead.fluiddrawers.tile.TileTankCustom;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Framed Fluid Tank. Ports the 1.12.2 {@code BlockTankCustom} to a 1.7.10 plain {@link Block} (no
 * Chameleon {@code MaterialData}/{@code IExtendedBlockState} -- materials are inlined in the tile
 * and read by the custom {@code ISimpleBlockRenderingHandler}).
 *
 * <p>
 * The frame (bottom/top slabs + corner posts) renders with the <b>side</b> material, the outer
 * trim border with the <b>trim</b> material, and the interior pane with the glass texture (the
 * framed model's {@code #front}) -- matching the 1.12.2 {@code tank_custom_base.json}. With no
 * materials set the block renders its raw/blank shape using the StorageDrawers {@code raw_side}
 * texture (there is no dedicated "empty framed tank" texture).
 */
public class BlockTankCustom extends BlockTank {

    @SideOnly(Side.CLIENT)
    private IIcon iconDefaultTank;

    public BlockTankCustom() {
        super(FluidDrawers.MODID + ".tank_custom", Material.wood);
        setStepSound(soundTypeWood);
        setHarvestLevel("axe", 0);
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TileTankCustom();
    }

    @Override
    public int getRenderType() {
        return ModBlocks.tankCustomRenderId;
    }

    /**
     * The default (raw / un-framed) face texture, used as the renderer and icon fallback when no
     * material has been applied. Reuses StorageDrawers' {@code drawers_raw_side} (a hard dependency
     * texture), matching the 1.12.2 model's {@code default_textures}.
     */
    @SideOnly(Side.CLIENT)
    public IIcon getDefaultTexture() {
        return iconDefaultTank;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister reg) {
        super.registerBlockIcons(reg);
        this.iconDefaultTank = reg.registerIcon("storagedrawers:drawers_raw_side");
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int meta) {
        return iconDefaultTank;
    }

    /**
     * Preserves the frame materials on the dropped item so a broken framed tank can be re-framed
     * or re-placed with its trim intact. The base {@link BlockTank#getDrops} already writes the
     * sealed {@code "Tile"} portable NBT (which now also carries the materials via
     * {@link TileTankCustom#writeToPortableNBT}); here we additionally write the materials to the
     * item's top-level {@code MatS}/{@code MatT}/{@code MatF} tags (the format
     * {@code ItemCustomDrawers.makeItemStack} uses) so an unsealed break still preserves the frame.
     */
    @Override
    public ArrayList<ItemStack> getDrops(World world, int x, int y, int z, int metadata, int fortune) {
        ArrayList<ItemStack> drops = super.getDrops(world, x, y, z, metadata, fortune);
        if (drops.isEmpty()) return drops;

        TileEntity te = world.getTileEntity(x, y, z);
        if (te instanceof TileTankCustom) {
            TileTankCustom tile = (TileTankCustom) te;
            ItemStack dropStack = drops.get(0);
            NBTTagCompound data = dropStack.getTagCompound();
            if (data == null) data = new NBTTagCompound();

            boolean any = false;
            if (tile.getMaterialSide() != null) {
                data.setTag(
                    "MatS",
                    tile.getMaterialSide()
                        .writeToNBT(new NBTTagCompound()));
                any = true;
            }
            if (tile.getMaterialTrim() != null) {
                data.setTag(
                    "MatT",
                    tile.getMaterialTrim()
                        .writeToNBT(new NBTTagCompound()));
                any = true;
            }
            if (tile.getMaterialFront() != null) {
                data.setTag(
                    "MatF",
                    tile.getMaterialFront()
                        .writeToNBT(new NBTTagCompound()));
                any = true;
            }
            if (any) dropStack.setTagCompound(data);
        }

        return drops;
    }
}
