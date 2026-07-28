package com.mrfuzzihead.fluiddrawers.drawers;

import net.minecraft.item.ItemStack;

import com.jaquadro.minecraft.storagedrawers.StorageDrawers;
import com.jaquadro.minecraft.storagedrawers.item.ItemUpgrade;
import com.jaquadro.minecraft.storagedrawers.item.ItemUpgradeCreative;
import com.jaquadro.minecraft.storagedrawers.item.ItemUpgradeDowngrade;
import com.jaquadro.minecraft.storagedrawers.item.ItemUpgradeLock;
import com.jaquadro.minecraft.storagedrawers.item.ItemUpgradeRedstone;
import com.jaquadro.minecraft.storagedrawers.item.ItemUpgradeStatus;
import com.jaquadro.minecraft.storagedrawers.item.ItemUpgradeVoid;

/**
 * Upgrade-item type-checking utility for the tank.
 * <p>
 * Ports the 1.12.2 FD {@code UpgradeItemHandler} which validated items against
 * libnine item references. In 1.7.10 the StorageDrawers API provides individual
 * item classes ({@link ItemUpgrade}, {@link ItemUpgradeDowngrade}, etc.) that we
 * check against directly.
 */
public class DrawerUpgradable {

    /**
     * Whether the given item stack is an upgrade that the tank can accept in its
     * upgrade slots. Accepts:
     * <ul>
     * <li>{@link ItemUpgrade} (storage upgrades, metadata = level)</li>
     * <li>{@link ItemUpgradeDowngrade} (capacity downgrade)</li>
     * <li>{@link ItemUpgradeVoid} (void upgrade)</li>
     * <li>{@link ItemUpgradeLock} (lock upgrade)</li>
     * <li>{@link ItemUpgradeCreative} (creative store/vend)</li>
     * <li>{@link ItemUpgradeRedstone} (redstone combined/max/min)</li>
     * <li>{@link ItemUpgradeStatus} (status level 1/2/3)</li>
     * </ul>
     */
    public static boolean isUpgradeItem(ItemStack stack) {
        if (stack == null) {
            return false;
        }
        return stack.getItem() instanceof ItemUpgrade || stack.getItem() instanceof ItemUpgradeDowngrade
            || stack.getItem() instanceof ItemUpgradeVoid
            || stack.getItem() instanceof ItemUpgradeLock
            || stack.getItem() instanceof ItemUpgradeCreative
            || stack.getItem() instanceof ItemUpgradeRedstone
            || stack.getItem() instanceof ItemUpgradeStatus;
    }

    /**
     * Whether the given upgrade item is a storage-level upgrade
     * ({@link ItemUpgrade} with metadata 2+).
     */
    public static boolean isStorageUpgrade(ItemStack stack) {
        return stack != null && stack.getItem() instanceof ItemUpgrade && stack.getItemDamage() >= 2;
    }

    /**
     * Whether the given upgrade item is a downgrade
     * ({@link ItemUpgradeDowngrade}).
     */
    public static boolean isDowngrade(ItemStack stack) {
        return stack != null && stack.getItem() instanceof ItemUpgradeDowngrade;
    }

    /**
     * Get the storage multiplier for a storage upgrade item.
     * Delegates to {@link StorageDrawers#config}'s
     * {@code getStorageUpgradeMultiplier}.
     *
     * @param stack the upgrade stack (must be an {@link ItemUpgrade})
     * @return the multiplier for this upgrade level, or 1 if the item is not a
     *         valid storage upgrade
     */
    public static int getStorageMultiplier(ItemStack stack) {
        if (stack != null && stack.getItem() instanceof ItemUpgrade) {
            return StorageDrawers.config.getStorageUpgradeMultiplier(stack.getItemDamage());
        }
        return 1;
    }

    /**
     * Get the storage level (metadata) from a storage upgrade.
     *
     * @param stack the upgrade stack
     * @return the metadata (storage level), or 0 if not a storage upgrade
     */
    public static int getStorageLevel(ItemStack stack) {
        if (stack != null && stack.getItem() instanceof ItemUpgrade) {
            return stack.getItemDamage();
        }
        return 0;
    }
}
