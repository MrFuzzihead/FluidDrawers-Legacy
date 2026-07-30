package com.mrfuzzihead.fluiddrawers.util;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

public class FluidTypeMap<T> {

    private final Map<Fluid, FluidTypeMap.SubMap<T>> backing = new HashMap<>();
    @Nullable
    private T nullMapping = (T) null;

    public void put(@Nullable FluidStack fluid, T value) {
        if (fluid == null) {
            this.nullMapping = value;
        } else {
            this.backing.computeIfAbsent(fluid.getFluid(), k -> new FluidTypeMap.SubMap<>())
                .put(fluid.tag, value);
        }
    }

    @Nullable
    public T get(@Nullable FluidStack fluid) {
        if (fluid == null) {
            return this.nullMapping;
        } else {
            FluidTypeMap.SubMap<T> subMap = this.backing.get(fluid.getFluid());
            return subMap != null ? subMap.get(fluid.tag) : null;
        }
    }

    public T getOrPut(@Nullable FluidStack fluid, Supplier<? extends T> valueFactory) {
        if (fluid == null) {
            if (this.nullMapping == null) {
                this.nullMapping = valueFactory.get();
            }
            return this.nullMapping;
        } else {
            return this.backing.computeIfAbsent(fluid.getFluid(), k -> new FluidTypeMap.SubMap<>())
                .getOrPut(fluid.tag, valueFactory);
        }
    }

    public void clear() {
        this.backing.clear();
    }

    public void forEach(FluidTypeMap.Visitor<T> visitor) {
        if (this.nullMapping != null) {
            visitor.visit(null, this.nullMapping);
        }
        this.backing.forEach((fluidType, subMap) -> subMap.forEach(fluidType, visitor));
    }

    private static class SubMap<T> {

        private final Map<NBTTagCompound, T> backing = new HashMap<>();
        @Nullable
        private T nullMapping = (T) null;

        private SubMap() {}

        void put(@Nullable NBTTagCompound tag, T value) {
            if (tag == null) {
                this.nullMapping = value;
            } else {
                this.backing.put(tag, value);
            }
        }

        @Nullable
        T get(@Nullable NBTTagCompound tag) {
            return tag == null ? this.nullMapping : this.backing.get(tag);
        }

        T getOrPut(@Nullable NBTTagCompound tag, Supplier<? extends T> valueFactory) {
            if (tag == null) {
                if (this.nullMapping == null) {
                    this.nullMapping = valueFactory.get();
                }
                return this.nullMapping;
            } else {
                return this.backing.computeIfAbsent(tag, k -> valueFactory.get());
            }
        }

        void forEach(Fluid fluidType, FluidTypeMap.Visitor<T> visitor) {
            if (this.nullMapping != null) {
                visitor.visit(new FluidStack(fluidType, 1000), this.nullMapping);
            }
            this.backing.forEach((tag, value) -> visitor.visit(new FluidStack(fluidType, 1000, tag), value));
        }
    }

    @FunctionalInterface
    public interface Visitor<T> {

        void visit(@Nullable FluidStack fluid, T value);
    }
}
