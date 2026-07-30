package com.mrfuzzihead.fluiddrawers.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraftforge.fluids.FluidStack;

public class FluidTypeMultimap<T> {

    private final FluidTypeMap<Collection<T>> backing = new FluidTypeMap<>();
    private final Supplier<? extends Collection<T>> collectionFactory;

    public FluidTypeMultimap(Supplier<? extends Collection<T>> collectionFactory) {
        this.collectionFactory = collectionFactory;
    }

    public FluidTypeMultimap() {
        this(ArrayList::new);
    }

    public void put(@Nullable FluidStack fluid, T value) {
        this.backing.getOrPut(fluid, this.collectionFactory)
            .add(value);
    }

    public Collection<T> get(@Nullable FluidStack fluid) {
        Collection<T> result = this.backing.get(fluid);
        return result != null ? result : Collections.emptyList();
    }

    public void clear() {
        this.backing.clear();
    }

    public void forEach(FluidTypeMap.Visitor<Collection<T>> visitor) {
        this.backing.forEach(visitor);
    }

    public void forEachBinding(FluidTypeMap.Visitor<T> visitor) {
        this.backing.forEach((fluid, collection) -> {
            for (T value : collection) {
                visitor.visit(fluid, value);
            }
        });
    }
}
