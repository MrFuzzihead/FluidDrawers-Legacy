package com.mrfuzzihead.fluiddrawers.client.renderer;

import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.IIcon;
import net.minecraftforge.client.IItemRenderer;

import org.lwjgl.opengl.GL11;

import com.mrfuzzihead.fluiddrawers.block.BlockTank;
import com.mrfuzzihead.fluiddrawers.client.tesr.RenderTileTank;
import com.mrfuzzihead.fluiddrawers.init.ModBlocks;
import com.mrfuzzihead.fluiddrawers.tile.TileTank;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Custom item renderer for the Fluid Tank. Shows the tape overlay on sealed tank items in the
 * inventory, held in hand, and dropped on the ground -- the 1.7.10 equivalent of the 1.12.2
 * {@code tank_sealed.json} item model (which combined the tank + seal_part models via libnine's
 * parameterized model system).
 *
 * <p>
 * Delegates the base frame + glass rendering to {@link BlockTankRenderer#renderInventoryBlock},
 * then draws the tape overlay (storagedrawers:tape) on all 4 side faces if the ItemStack carries a
 * {@code "Tile"} portable NBT subtag (i.e. the tank was sealed when broken).
 *
 * <p>
 * WAILA HUD path: {@link com.mrfuzzihead.fluiddrawers.integration.WailaIntegration#getWailaStack}
 * returns a transient stack carrying the live tile's portable NBT plus a {@code "WailaLive"}
 * marker. When that marker is present, this renderer additionally draws the stored fluid (via
 * {@link RenderTileTank#renderFluidForItem}) and the seal/trim/lock/void overlays (via
 * {@link BlockTankRenderer#renderInventoryOverlays}) so the WAILA display mirrors the in-world
 * tank instead of a clear-slate empty tank. The marker scopes the extra rendering to WAILA only;
 * real sealed items keep their tape-only behavior.
 */
@SideOnly(Side.CLIENT)
public class ItemRendererTank implements IItemRenderer {

    // BlockTankRenderer is stateless, so a single shared instance is safe.
    private static final BlockTankRenderer ISBRH = new BlockTankRenderer();

    private static final double U = 1.0 / 16.0;

    @Override
    public boolean handleRenderType(ItemStack item, IItemRenderer.ItemRenderType type) {
        return true;
    }

    @Override
    public boolean shouldUseRenderHelper(IItemRenderer.ItemRenderType type, ItemStack item,
        IItemRenderer.ItemRendererHelper helper) {
        return true;
    }

    @Override
    public void renderItem(IItemRenderer.ItemRenderType type, ItemStack stack, Object... data) {
        BlockTank block = ModBlocks.TANK;
        RenderBlocks renderer = (data != null && data.length > 0 && data[0] instanceof RenderBlocks)
            ? (RenderBlocks) data[0]
            : RenderBlocks.getInstance();

        NBTTagCompound tag = stack.getTagCompound();
        boolean hasTile = tag != null && tag.hasKey("Tile", 10);
        // Marker set by WailaIntegration.getWailaStack: the stack is a transient WAILA HUD icon
        // carrying the live tile's portable NBT. Render the full live state (frame + fluid +
        // seal/trim/lock/void overlays) so the WAILA display matches the in-world tank instead of
        // a clear-slate empty tank. Real sealed tank items (inventory/hand/dropped) do NOT carry
        // this marker, so they keep their existing tape-only behavior.
        boolean wailaLive = tag != null && tag.getBoolean("WailaLive");

        if (wailaLive && hasTile) {
            // Force full-bright lighting for the GUI HUD icon (WAILA's lightmap context is not
            // guaranteed to match the inventory-slot full-bright). Save/restore so the caller's
            // lightmap is unaffected. The fluid + overlays bake their own per-vertex brightness.
            float prevLX = OpenGlHelper.lastBrightnessX;
            float prevLY = OpenGlHelper.lastBrightnessY;
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240.0F, 240.0F);

            // Reconstruct a transient TileTank from the portable NBT FIRST, so we can pick the
            // vending frame icon (and reuse all existing read logic: fluid, capacity, trim level,
            // attributes, seal, owner). worldObj stays null; readFromPortableNBT + the render
            // helpers only touch portable state, so this is null-world-safe.
            TileTank tile = new TileTank();
            tile.readFromPortableNBT(tag.getCompoundTag("Tile"));

            // Draw the base frame + glass. Use the vending texture when the tank has a Creative
            // Vending upgrade, mirroring BlockTankRenderer.renderWorldBlock's in-world check.
            IIcon frameIcon = tile.isVending() ? block.getIconTankVending() : block.getIconTank();
            ISBRH.renderInventoryFrame(block, frameIcon, renderer);

            // Fluid + overlays are drawn in block-local 0..1 space; wrap in a -0.5 translate to
            // center the model (matching renderInventoryBlock's own centering).
            GL11.glTranslatef(-0.5F, -0.5F, -0.5F);
            RenderTileTank.renderFluidForItem(tile);
            ISBRH.renderInventoryOverlays(block, tile, renderer);
            GL11.glTranslatef(0.5F, 0.5F, 0.5F);

            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, prevLX, prevLY);
        } else {
            // Draw the base frame + glass (same as the ISBRH's renderInventoryBlock).
            ISBRH.renderInventoryBlock(block, 0, ModBlocks.tankRenderId, renderer);

            // If the item is sealed (has "Tile" portable NBT), draw the tape overlay on top.
            // renderInventoryBlock opened + closed its own Tessellator batch (startDrawingQuads/draw),
            // so we need a fresh batch for the overlay quads.
            if (hasTile) {
                IIcon tapeIcon = block.getIconTape();
                if (tapeIcon != null) {
                    GL11.glTranslatef(-0.5F, -0.5F, -0.5F);
                    Tessellator tess = Tessellator.instance;
                    tess.startDrawingQuads();
                    tess.setColorOpaque_F(1.0F, 1.0F, 1.0F);
                    double off = 0.003;
                    renderer.setRenderBounds(0, 0, -off, 1, 1, 1);
                    renderer.renderFaceZNeg(block, 0, 0, 0, tapeIcon);
                    renderer.setRenderBounds(0, 0, 0, 1, 1, 1 + off);
                    renderer.renderFaceZPos(block, 0, 0, 0, tapeIcon);
                    renderer.setRenderBounds(-off, 0, 0, 1, 1, 1);
                    renderer.renderFaceXNeg(block, 0, 0, 0, tapeIcon);
                    renderer.setRenderBounds(0, 0, 0, 1 + off, 1, 1);
                    renderer.renderFaceXPos(block, 0, 0, 0, tapeIcon);
                    tess.draw();
                    GL11.glTranslatef(0.5F, 0.5F, 0.5F);
                }
            }
        }
    }
}
