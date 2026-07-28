package com.mrfuzzihead.fluiddrawers.client.gui;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.util.IIcon;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.FluidStack;

import org.lwjgl.opengl.GL11;

import com.mrfuzzihead.fluiddrawers.drawers.FluidDrawer;
import com.mrfuzzihead.fluiddrawers.inventory.ContainerTank;
import com.mrfuzzihead.fluiddrawers.tile.TileTank;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * GUI for the Fluid Tank. Ports the 1.12.2 {@code GuiTank} which extended libnine's
 * {@code L9GuiContainer} (itself based on {@code GuiContainer}) and used
 * {@code GuiComponentFluidTank} for the fluid widget. In 1.7.10 we extend
 * {@link GuiContainer} directly and draw the fluid widget inline in
 * {@link #drawGuiContainerBackgroundLayer}.
 *
 * <p>
 * Layout (176w x 199h, matching 1.12.2 FD):
 * <ul>
 * <li>Fluid gauge area from y=18 to y=70, centered horizontally</li>
 * <li>7 upgrade slots at y=86, x offset 26 + i*18</li>
 * <li>Player inventory + hotbar below</li>
 * </ul>
 */
@SideOnly(Side.CLIENT)
public class GuiTank extends GuiContainer {

    private static final ResourceLocation BG_TEXTURE = new ResourceLocation("fluiddrawers", "textures/gui/tank.png");

    private final ContainerTank containerTank;

    // Widget positioning (matching 1.12.2 FD layout)
    private static final int FLUID_X = 80;
    private static final int FLUID_Y = 36;

    // Disabled-slot overlay coordinates on the BG_TEXTURE (1.12.2: (176, 0, 16, 16))
    private static final int SLOT_OVERLAY_U = 176;
    private static final int SLOT_OVERLAY_V = 0;
    private static final int SLOT_OVERLAY_SIZE = 16;

    public GuiTank(InventoryPlayer playerInv, TileTank tile) {
        super(new ContainerTank(playerInv, tile));
        this.containerTank = (ContainerTank) this.inventorySlots;
        this.xSize = 176;
        this.ySize = 199;
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        // Bind the GUI background texture
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager()
            .bindTexture(BG_TEXTURE);
        this.drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);

        // Draw the fluid sprite in the tank widget area
        FluidDrawer drawer = this.containerTank.getFluidDrawer();
        FluidStack fluid = drawer.getStoredFluid();
        if (fluid != null && fluid.amount > 0) {
            IIcon icon = fluid.getFluid()
                .getStillIcon();
            if (icon != null) {
                int capacity = drawer.getMaxCapacity();
                double fillPercent = capacity > 0 ? (double) fluid.amount / capacity : 0.0;
                fillPercent = Math.min(1.0, Math.max(0.01, fillPercent));

                // Bind the block texture atlas (where fluid icons live)
                this.mc.getTextureManager()
                    .bindTexture(TextureMap.locationBlocksTexture);

                // Fluid tint color
                int color = fluid.getFluid()
                    .getColor(fluid);
                float r = ((color >> 16) & 0xFF) / 255.0F;
                float g = ((color >> 8) & 0xFF) / 255.0F;
                float b = (color & 0xFF) / 255.0F;
                GL11.glColor4f(r, g, b, 1.0F);

                // Draw the fluid sprite as a partial-height textured quad using Tessellator.
                // The quad grows upward from the bottom of the 16x16 gauge area as fill
                // increases: full fill = full 16px height; empty = 0px (we floor to 1px so
                // at least a sliver is visible when fluid > 0). UV V-range samples the
                // corresponding bottom portion of the icon texture.
                int drawW = 16;
                int drawH = (int) (drawW * fillPercent);
                if (drawH < 1) drawH = 1;
                int drawX = guiLeft + FLUID_X;
                int drawY = guiTop + FLUID_Y + (drawW - drawH);

                double uMin = icon.getMinU();
                double uMax = icon.getMaxU();
                double vMin = icon.getInterpolatedV((1.0 - fillPercent) * 16.0);
                double vMax = icon.getMaxV();

                Tessellator tessellator = Tessellator.instance;
                tessellator.startDrawingQuads();
                tessellator.addVertexWithUV(drawX, drawY + drawH, this.zLevel, uMin, vMax);
                tessellator.addVertexWithUV(drawX + drawW, drawY + drawH, this.zLevel, uMax, vMax);
                tessellator.addVertexWithUV(drawX + drawW, drawY, this.zLevel, uMax, vMin);
                tessellator.addVertexWithUV(drawX, drawY, this.zLevel, uMin, vMin);
                tessellator.draw();

                // Reset color and re-bind the GUI texture for overlays
                GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
                this.mc.getTextureManager()
                    .bindTexture(BG_TEXTURE);
            }
        }

        // Draw disabled slot overlays for upgrade slots that are not interactable.
        // 1.12.2 condition: !slot.isSlotInInventory(player) which returns true when
        // the slot has an item AND the player cannot remove it (canTakeStack returns false).
        // Phase 8: canTakeStack returns true (inert upgrades), so no overlay is drawn.
        for (int i = 0; i < 7; i++) {
            Slot slot = (Slot) this.inventorySlots.inventorySlots.get(i);
            if (!slot.canTakeStack(this.mc.thePlayer)) {
                this.drawTexturedModalRect(
                    guiLeft + slot.xDisplayPosition,
                    guiTop + slot.yDisplayPosition,
                    SLOT_OVERLAY_U,
                    SLOT_OVERLAY_V,
                    SLOT_OVERLAY_SIZE,
                    SLOT_OVERLAY_SIZE);
            }
        }
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        // Draw the container title
        String title = I18n.format("fluiddrawers.container.tank");
        this.fontRendererObj.drawString(title, 8, 6, 0x404040);

        // Draw the "Upgrades" label
        String upgrades = I18n.format("storagedrawers.container.upgrades");
        this.fontRendererObj.drawString(upgrades, 8, 75, 0x404040);

        // Draw the player inventory label
        String playerInv = I18n.format("container.inventory");
        this.fontRendererObj.drawString(playerInv, 8, 107, 0x404040);
    }
}
