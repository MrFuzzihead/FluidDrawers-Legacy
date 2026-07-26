package com.mrfuzzihead.fluiddrawers.mixins;

import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;

import com.gtnewhorizon.gtnhmixins.ILateMixinLoader;
import com.gtnewhorizon.gtnhmixins.LateMixin;
import com.gtnewhorizon.gtnhmixins.builders.IMixins;

// The annotation is required, it indicates to
// the mixins framework to instantiate this class
// and look for LateMixins to load.
@LateMixin
public class LateMixinsLoader implements ILateMixinLoader {

    @Override
    public String getMixinConfig() {
        // rename the associated .json file by replacing the "mymodid" with your own mod ID
        // in the .json file edit the "package" and "refmap" properties to match your mod
        // also edit the "refmap" property in the "mixins.mymodid.json" file
        return "mixins.fuzzitweaks.late.json";
    }

    @Nonnull
    @Override
    public List<String> getMixins(Set<String> loadedMods) {
        // Load config here so that setApplyIf conditions read values from the .cfg file
        // rather than the hard-coded field defaults. getMixins() is called before
        // FMLPreInitializationEvent, so we must resolve the config file path ourselves
        // using Launch.minecraftHome which is set by LaunchWrapper very early.
        return IMixins.getLateMixins(Mixins.class, loadedMods);
    }
}
