package com.mrfuzzihead.fluiddrawers.mixins;

import javax.annotation.Nonnull;

import com.gtnewhorizon.gtnhmixins.builders.IMixins;
import com.gtnewhorizon.gtnhmixins.builders.MixinBuilder;

public enum Mixins implements IMixins {

    // Read the Javadoc of IMixins and MixinBuilder for further information
    // You should declare all of your mixins early and late in this same enum
    STORAGEDRAWERS(new MixinBuilder().setPhase(Phase.LATE)
        .addCommonMixins("storagedrawers.")
        .addRequiredMod(TargetMods.STORAGEDRAWERS));

    private final MixinBuilder builder;

    Mixins(MixinBuilder builder) {
        this.builder = builder;
    }

    @Nonnull
    @Override
    public MixinBuilder getBuilder() {
        return builder;
    }
}
