package com.mrfuzzihead.fluiddrawers.integration;

import java.util.List;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidStack;

import com.jaquadro.minecraft.storagedrawers.api.storage.attribute.LockAttribute;
import com.mrfuzzihead.fluiddrawers.block.BlockTank;
import com.mrfuzzihead.fluiddrawers.drawers.FluidDrawer;
import com.mrfuzzihead.fluiddrawers.init.ModBlocks;
import com.mrfuzzihead.fluiddrawers.tile.TileTank;

import mcp.mobius.waila.api.IWailaConfigHandler;
import mcp.mobius.waila.api.IWailaDataAccessor;
import mcp.mobius.waila.api.IWailaDataProvider;
import mcp.mobius.waila.api.IWailaRegistrar;

public final class WailaIntegration {

    private WailaIntegration() {}

    public static void registerProvider(IWailaRegistrar registrar) {
        registrar.registerBodyProvider(new TankDataProvider(), BlockTank.class);
        registrar.registerStackProvider(new TankDataProvider(), BlockTank.class);
    }

    public static class TankDataProvider implements IWailaDataProvider {

        @Override
        public ItemStack getWailaStack(IWailaDataAccessor accessor, IWailaConfigHandler config) {
            TileEntity tile = accessor.getTileEntity();
            if (!(tile instanceof TileTank)) {
                return null;
            }
            TileTank tank = (TileTank) tile;

            // Build a transient tank item carrying the live tile's portable NBT (the same "Tile"
            // subtag format BlockTank.getDrops writes for sealed tanks) plus a "WailaLive" marker.
            // The item renderer (ItemRendererTank) reads the marker and renders the current fluid +
            // seal/trim/lock/void overlays so the WAILA HUD mirrors the in-world tank instead of a
            // clear-slate empty tank. The accessor's tile is the synced client-side TE (full NBT
            // via the description packet), so fluid/upgrades/attributes/owner are all present.
            ItemStack stack = new ItemStack(Item.getItemFromBlock(ModBlocks.TANK), 1, 0);
            NBTTagCompound tag = new NBTTagCompound();
            NBTTagCompound tileTag = new NBTTagCompound();
            tank.writeToPortableNBT(tileTag);
            tag.setTag("Tile", tileTag);
            tag.setBoolean("WailaLive", true);
            stack.setTagCompound(tag);
            return stack;
        }

        @Override
        public List<String> getWailaHead(ItemStack itemStack, List<String> currenttip, IWailaDataAccessor accessor,
            IWailaConfigHandler config) {
            TileEntity tile = accessor.getTileEntity();
            if (tile instanceof TileTank) {
                TileTank tank = (TileTank) tile;
                String customName = tank.getCustomName();
                if (customName != null && !customName.isEmpty()) {
                    currenttip
                        .set(0, StatCollector.translateToLocal("fluiddrawers.waila.tank_prefix") + " " + customName);
                }
            }
            return currenttip;
        }

        @Override
        public List<String> getWailaBody(ItemStack itemStack, List<String> currenttip, IWailaDataAccessor accessor,
            IWailaConfigHandler config) {
            TileEntity tile = accessor.getTileEntity();
            if (!(tile instanceof TileTank)) {
                return currenttip;
            }

            TileTank tank = (TileTank) tile;

            FluidDrawer drawer = tank.getFluidDrawerGroup()
                .getFluidDrawer(0);
            FluidStack fluid = drawer.getStoredFluid();

            if (tank.getAttributes()
                .isUnlimitedVending()) {
                // Creative vending: infinite content and capacity
                String fluidName = fluid != null && fluid.amount > 0 ? fluid.getLocalizedName()
                    : StatCollector.translateToLocal("fluiddrawers.waila.empty");
                currenttip.add(StatCollector.translateToLocalFormatted("fluiddrawers.waila.fluid_creative", fluidName));
            } else if (fluid != null && fluid.amount > 0) {
                String fluidName = fluid.getLocalizedName();
                int amount = fluid.amount;
                int capacity = drawer.getMaxCapacity();
                if (tank.getAttributes()
                    .isUnlimitedStorage()) {
                    // Creative storage: finite content, infinite capacity
                    currenttip.add(
                        StatCollector
                            .translateToLocalFormatted("fluiddrawers.waila.fluid_unlimited", fluidName, amount));
                } else {
                    // Normal: finite content and capacity
                    currenttip.add(
                        StatCollector
                            .translateToLocalFormatted("fluiddrawers.waila.fluid", fluidName, amount, capacity));
                }
            } else {
                currenttip.add(StatCollector.translateToLocal("fluiddrawers.waila.empty"));
            }

            StringBuilder attrib = new StringBuilder();

            if (tank.getAttributes()
                .isItemLocked(LockAttribute.LOCK_POPULATED)) {
                attrib.append(appendComma(attrib))
                    .append(StatCollector.translateToLocal("fluiddrawers.waila.locked"));
            }
            if (tank.getAttributes()
                .isVoid()) {
                attrib.append(appendComma(attrib))
                    .append(StatCollector.translateToLocal("fluiddrawers.waila.void"));
            }
            if (tank.getAttributes()
                .isConcealed()) {
                attrib.append(appendComma(attrib))
                    .append(StatCollector.translateToLocal("fluiddrawers.tooltip.concealed"));
            }
            if (tank.isSealed()) {
                attrib.append(appendComma(attrib))
                    .append(StatCollector.translateToLocal("fluiddrawers.waila.sealed"));
            }
            if (tank.getOwner() != null) {
                attrib.append(appendComma(attrib))
                    .append(StatCollector.translateToLocal("fluiddrawers.waila.protected"));
            }
            if (tank.getAttributes()
                .isUnlimitedStorage()
                || tank.getAttributes()
                    .isUnlimitedVending()) {
                attrib.append(appendComma(attrib))
                    .append(StatCollector.translateToLocal("fluiddrawers.waila.creative"));
            }

            if (attrib.length() > 0) {
                currenttip.add(attrib.toString());
            }

            return currenttip;
        }

        @Override
        public List<String> getWailaTail(ItemStack itemStack, List<String> currenttip, IWailaDataAccessor accessor,
            IWailaConfigHandler config) {
            return currenttip;
        }

        @Override
        public NBTTagCompound getNBTData(EntityPlayerMP player, TileEntity te, NBTTagCompound tag, World world, int x,
            int y, int z) {
            return null;
        }

        private static String appendComma(StringBuilder current) {
            return current.length() > 0 ? ", " : "";
        }
    }
}
