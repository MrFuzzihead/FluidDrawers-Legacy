package com.mrfuzzihead.fluiddrawers.block;

import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.mrfuzzihead.fluiddrawers.FluidDrawersCreativeTab;
import com.mrfuzzihead.fluiddrawers.init.ModBlocks;
import com.mrfuzzihead.fluiddrawers.tile.TileTank;
import com.mrfuzzihead.fluiddrawers.util.BlockInteractionUtils;

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

    // --- Light emission ---

    // A tank holding a luminous fluid (e.g. lava) emits that fluid's light level. Delegates to the
    // tile's stored fluid luminosity (unscaled by fill -- matches the OpenBlocks Tank reference;
    // 1.12.2 FluidDrawers does not override getLightValue). The Forge-added
    // getLightValue(IBlockAccess, x, y, z) is the world/lighting-engine entry point (Block:1497);
    // the rendering pipeline also feeds it into getLightBrightnessForSkyBlocks (Block:609/615).
    // TileTank.onStoredFluidChanged triggers a relight (func_147451_t) when the luminosity changes.
    @Override
    public int getLightValue(IBlockAccess world, int x, int y, int z) {
        TileEntity tile = world.getTileEntity(x, y, z);
        return tile instanceof TileTank ? ((TileTank) tile).getFluidLightLevel() : 0;
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

    // --- Interaction dispatcher ---
    //
    // Ordered per section 4 of the backport plan (settled, do not reorder):
    // 1. tile == null → false
    // 2. security first (Phase 11)
    // 3. held-item: ItemKey/tape → ItemUpgrade → ItemPersonalKey → fluid transfer
    // 4. empty hand + sneak: unseal → open GUI

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX,
        float hitY, float hitZ) {
        TileTank tile = (TileTank) world.getTileEntity(x, y, z);
        if (tile == null) return false;

        // TODO Phase 11: security-first guard — SecurityManager.hasAccess(player, tile)

        // Capture the original held item ONCE, before any inventory changes happen on
        // either client or server. Both sides operate on this same snapshot (the same
        // pattern OpenBlocks uses in TileEntityTank.tryEmptyItem), so the server never
        // mistakes a client-predicted swap for the actual held item.
        ItemStack heldItem = player.getHeldItem();

        if (heldItem != null) {
            // Held-item dispatch:
            // TODO Phase 11: ItemKey/ModItems.tape → return false
            // TODO Phase 9: ItemUpgrade → add upgrade
            // TODO Phase 11: ItemPersonalKey → toggle ownership
            // Fluid transfer (Phase 5) — gated by facing != UP

            if (side != 1 && !tile.getAttributes()
                .isVoid() /* TODO Phase 11: && !tile.isSealed() */) {
                // Both client and server run the same transferFluid with the original
                // heldItem snapshot. The client-side TE modification is harmless (it gets
                // overwritten by the next server → client TE sync), and the item swap
                // is identical on both sides so the hotbar stays in sync immediately.
                if (BlockInteractionUtils
                    .transferFluid(tile, player, heldItem, ForgeDirection.getOrientation(side), true)) {
                    return true;
                }
            }
        } else if (player.isSneaking()) {
            // Empty hand + sneak:
            // TODO Phase 11: if sealed → unseal
            // TODO Phase 8: else if enableDrawerUI → open GUI
        }

        return false;
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
