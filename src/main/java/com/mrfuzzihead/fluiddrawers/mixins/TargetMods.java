package com.mrfuzzihead.fluiddrawers.mixins;

import javax.annotation.Nonnull;

import com.gtnewhorizon.gtnhmixins.builders.ITargetMod;
import com.gtnewhorizon.gtnhmixins.builders.TargetModBuilder;

public enum TargetMods implements ITargetMod {

    // Read the Javadoc of ITargetMod and TargetModBuilder for further information
    // Add to this enum information about the mods you need to identify during runtime
    // The mod id MUST match StorageDrawers' declared @Mod(modid=...) EXACTLY —
    // TargetModBuilder.isTargetPresent() does a case-sensitive loadedMods.contains(modId), and SD
    // declares MOD_ID = "StorageDrawers" (mixed case). A lowercase "storagedrawers" here silently
    // filters the mixin out of the late-mixin set ("Not loading the following LATE mixins"), so the
    // controller never gets IFluidHandler and right-clicking does nothing.
    STORAGEDRAWERS("StorageDrawers");

    private final TargetModBuilder builder;

    TargetMods(String coreModClass, String modId) {
        this.builder = new TargetModBuilder().setCoreModClass(coreModClass)
            .setModId(modId);
    }

    TargetMods(String modId) {
        this.builder = new TargetModBuilder().setModId(modId);
    }

    @Nonnull
    @Override
    public TargetModBuilder getBuilder() {
        return builder;
    }
}
