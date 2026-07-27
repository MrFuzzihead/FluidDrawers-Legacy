package com.mrfuzzihead.fluiddrawers.util;

import java.util.EnumSet;

import javax.annotation.Nullable;

import net.minecraft.nbt.NBTTagCompound;

import com.jaquadro.minecraft.storagedrawers.api.storage.attribute.LockAttribute;

/**
 * Standalone attributes implementation — the SD 1.7.10 GTNH API does NOT provide a unified
 * {@code IDrawerAttributes} interface (it uses per-attribute interfaces instead). Our callers
 * reference the method names from the 1.12.2 FD source ({@code canItemLock}, {@code isVoid}, etc.)
 * as concrete calls on this class.
 */
public class SimpleDrawerAttributes {

    @Nullable
    private EnumSet<LockAttribute> lockAttrs = null;
    private boolean concealed = false;
    private boolean showingQty = false;
    private boolean voiding = false;
    private boolean unlimitedStorage = false;
    private boolean vending = false;
    private boolean converting = false;

    public boolean canItemLock(LockAttribute attr) {
        return true;
    }

    public boolean isItemLocked(LockAttribute attr) {
        return this.lockAttrs != null && this.lockAttrs.contains(attr);
    }

    public void setItemLocked(LockAttribute attr, boolean isLocked) {
        if (isLocked) {
            if (this.lockAttrs == null) {
                this.lockAttrs = EnumSet.of(attr);
                this.onAttributeChanged();
            } else if (this.lockAttrs.add(attr)) {
                this.onAttributeChanged();
            }
        } else if (this.lockAttrs != null && this.lockAttrs.remove(attr)) {
            this.onAttributeChanged();
        }
    }

    public boolean isConcealed() {
        return this.concealed;
    }

    public void setConcealed(boolean state) {
        if (this.concealed != state) {
            this.concealed = state;
            this.onAttributeChanged();
        }
    }

    public boolean isVoid() {
        return this.voiding;
    }

    public void setVoid(boolean state) {
        if (this.voiding != state) {
            this.voiding = state;
            this.onAttributeChanged();
        }
    }

    public boolean isShowingQuantity() {
        return this.showingQty;
    }

    public void setShowingQuantity(boolean state) {
        if (this.showingQty != state) {
            this.showingQty = state;
            this.onAttributeChanged();
        }
    }

    public boolean isUnlimitedStorage() {
        return this.unlimitedStorage;
    }

    public void setUnlimitedStorage(boolean state) {
        if (this.unlimitedStorage != state) {
            this.unlimitedStorage = state;
            this.onAttributeChanged();
        }
    }

    public boolean isUnlimitedVending() {
        return this.vending;
    }

    public void setUnlimitedVending(boolean state) {
        if (this.vending != state) {
            this.vending = state;
            this.onAttributeChanged();
        }
    }

    public boolean isDictConvertible() {
        return this.converting;
    }

    public void setDictConvertible(boolean state) {
        if (this.converting != state) {
            this.converting = state;
            this.onAttributeChanged();
        }
    }

    public NBTTagCompound serializeNBT() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setByte("itemLock", (byte) LockAttribute.getBitfield(this.lockAttrs));
        tag.setBoolean("concealed", this.concealed);
        tag.setBoolean("void", this.voiding);
        tag.setBoolean("quant", this.showingQty);
        tag.setBoolean("unlimited", this.unlimitedStorage);
        tag.setBoolean("vending", this.vending);
        tag.setBoolean("conv", this.converting);
        return tag;
    }

    public void deserializeNBT(NBTTagCompound tag) {
        this.lockAttrs = LockAttribute.getEnumSet(tag.getByte("itemLock"));
        this.concealed = tag.getBoolean("concealed");
        this.voiding = tag.getBoolean("void");
        this.showingQty = tag.getBoolean("quant");
        this.unlimitedStorage = tag.getBoolean("unlimited");
        this.vending = tag.getBoolean("vending");
        this.converting = tag.getBoolean("conv");
    }

    protected void onAttributeChanged() {}
}
