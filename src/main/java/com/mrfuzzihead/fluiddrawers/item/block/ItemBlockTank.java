package com.mrfuzzihead.fluiddrawers.item.block;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidStack;

import com.jaquadro.minecraft.storagedrawers.api.storage.attribute.LockAttribute;
import com.mrfuzzihead.fluiddrawers.Config;
import com.mrfuzzihead.fluiddrawers.drawers.DrawerUpgradable;
import com.mrfuzzihead.fluiddrawers.tile.TileTank;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * ItemBlock for the Fluid Tank. Ports the 1.12.2 {@code ItemBlockTank} (which extended libnine's
 * {@code L9ItemBlockStated}) to a plain 1.7.10 {@link ItemBlock}.
 *
 * <p>
 * Phase 12 responsibilities:
 * <ul>
 * <li><b>Place:</b> restore the tile's portable NBT (fluid, upgrades, lock/key statuses, custom
 * name) from the item's {@code "Tile"} subtag, then unseal the tank so it is interactable
 * again. If the item has no {@code "Tile"} tag but was anvil-renamed, carry the display name
 * to the new tile as its custom name.</li>
 * <li><b>Tooltip:</b> show the base capacity always; when the item carries a {@code "Tile"} tag
 * (sealed tank), show a "Sealed" marker plus the preserved fluid type/amount, installed
 * upgrades, and lock status -- matching the 1.7.10 StorageDrawers item-tooltip style.</li>
 * </ul>
 */
public class ItemBlockTank extends ItemBlock {

    public ItemBlockTank(Block block) {
        super(block);
        setMaxDamage(0);
        setHasSubtypes(false);
    }

    @Override
    public boolean placeBlockAt(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side,
        float hitX, float hitY, float hitZ, int metadata) {
        if (!super.placeBlockAt(stack, player, world, x, y, z, side, hitX, hitY, hitZ, metadata)) {
            return false;
        }

        TileEntity tileUnchecked = world.getTileEntity(x, y, z);
        if (tileUnchecked instanceof TileTank) {
            TileTank tile = (TileTank) tileUnchecked;
            NBTTagCompound stackTag = stack.getTagCompound();

            boolean restored = false;
            if (stackTag != null && stackTag.hasKey("Tile", 10)) {
                // 10 = NBT.TAG_COMPOUND. Restore the sealed tank's full portable state.
                tile.readFromPortableNBT(stackTag.getCompoundTag("Tile"));
                restored = true;
            }

            // If the item has no portable NBT but was anvil-renamed (clean-slate item with a
            // custom display name), carry that name to the freshly-placed tile.
            if (!restored && stack.hasDisplayName()) {
                tile.setCustomName(stack.getDisplayName());
            }

            // Placing always unseals the tank (ports 1.12.2 tile.setIsSealed(false)). A restored
            // sealed tank had sealed=true in its portable NBT; this flips it back so the tank is
            // interactable. setIsSealed handles the markDirty/markBlockForUpdate when the state
            // actually changes.
            tile.setIsSealed(false);

            // Push the restored state to the client so the fluid TESR + overlays render
            // immediately without needing a chunk reload.
            tile.markDirty();
            world.markBlockForUpdate(x, y, z);
        }

        return true;
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean advanced) {
        super.addInformation(stack, player, list, advanced);
        list.add(StatCollector.translateToLocalFormatted("fluiddrawers.info.tank_capacity", Config.baseCapacity));

        NBTTagCompound stackTag = stack.getTagCompound();
        if (stackTag == null || !stackTag.hasKey("Tile", 10)) {
            return;
        }

        NBTTagCompound tileTag = stackTag.getCompoundTag("Tile");

        // Sealed marker (yellow, matching 1.12.2 ItemBlockTank).
        list.add(EnumChatFormatting.YELLOW + StatCollector.translateToLocal("storagedrawers.drawers.sealed"));

        // --- Compute effective capacity from the preserved upgrades ---
        int multiplier = 0;
        boolean downgraded = false;
        List<ItemStack> upgrades = new ArrayList<ItemStack>();
        if (tileTag.hasKey("Upgrades", 10)) {
            NBTTagCompound upgradesTag = tileTag.getCompoundTag("Upgrades");
            for (int i = 0; i < TileTank.UPGRADE_SLOT_COUNT; i++) {
                String key = "Upgrade" + i;
                if (upgradesTag.hasKey(key, 10)) {
                    ItemStack upgrade = ItemStack.loadItemStackFromNBT(upgradesTag.getCompoundTag(key));
                    if (upgrade != null) {
                        upgrades.add(upgrade);
                        if (DrawerUpgradable.isStorageUpgrade(upgrade)) {
                            multiplier += DrawerUpgradable.getStorageMultiplier(upgrade);
                        }
                        if (DrawerUpgradable.isDowngrade(upgrade)) {
                            downgraded = true;
                        }
                    }
                }
            }
        }
        if (multiplier == 0) multiplier = 1;
        int effectiveCapacity = (downgraded ? Config.baseCapacityDowngraded : Config.baseCapacity) * multiplier;

        // --- Fluid info ---
        if (tileTag.hasKey("Drawer", 10)) {
            NBTTagCompound drawerTag = tileTag.getCompoundTag("Drawer");
            if (drawerTag.hasKey("Fluid", 10)) {
                FluidStack fluid = FluidStack.loadFluidStackFromNBT(drawerTag.getCompoundTag("Fluid"));
                if (fluid != null && fluid.amount > 0) {
                    String fluidName = fluid.getLocalizedName();
                    list.add(
                        EnumChatFormatting.GRAY + StatCollector.translateToLocalFormatted(
                            "fluiddrawers.tooltip.fluid",
                            fluidName,
                            fluid.amount,
                            effectiveCapacity));
                }
            }
        }

        // --- Installed upgrades ---
        for (ItemStack upgrade : upgrades) {
            list.add(EnumChatFormatting.GRAY + "- " + upgrade.getDisplayName());
        }

        // --- Lock (key) status ---
        if (tileTag.hasKey("Attributes", 10)) {
            NBTTagCompound attrsTag = tileTag.getCompoundTag("Attributes");
            EnumSet<LockAttribute> lockAttrs = LockAttribute.getEnumSet(attrsTag.getByte("itemLock"));
            if (lockAttrs != null && !lockAttrs.isEmpty()) {
                list.add(EnumChatFormatting.GRAY + StatCollector.translateToLocal("fluiddrawers.tooltip.locked"));
            }
            // --- Concealment (shroud) status ---
            if (attrsTag.getBoolean("concealed")) {
                list.add(EnumChatFormatting.GRAY + StatCollector.translateToLocal("fluiddrawers.tooltip.concealed"));
            }
        }
    }
}
