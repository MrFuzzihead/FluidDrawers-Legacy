package com.mrfuzzihead.fluiddrawers.init;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import com.mrfuzzihead.fluiddrawers.FluidDrawers;
import com.mrfuzzihead.fluiddrawers.client.gui.GuiTank;
import com.mrfuzzihead.fluiddrawers.inventory.ContainerTank;
import com.mrfuzzihead.fluiddrawers.tile.TileTank;

import cpw.mods.fml.common.network.IGuiHandler;
import cpw.mods.fml.common.network.NetworkRegistry;

public class FdGuis {

    public static final int GUI_TANK = 0;

    public static void init() {
        NetworkRegistry.INSTANCE.registerGuiHandler(FluidDrawers.instance, new GuiHandler());
    }

    public static class GuiHandler implements IGuiHandler {

        @Override
        public Object getServerGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
            if (ID == GUI_TANK) {
                TileEntity tile = world.getTileEntity(x, y, z);
                if (tile instanceof TileTank) {
                    return new ContainerTank(player.inventory, (TileTank) tile);
                }
            }
            return null;
        }

        @Override
        public Object getClientGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
            if (ID == GUI_TANK) {
                TileEntity tile = world.getTileEntity(x, y, z);
                if (tile instanceof TileTank) {
                    return new GuiTank(player.inventory, (TileTank) tile);
                }
            }
            return null;
        }
    }
}
