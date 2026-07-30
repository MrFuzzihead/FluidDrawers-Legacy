package com.mrfuzzihead.fluiddrawers;

import java.io.File;

import net.minecraftforge.common.config.Configuration;

public class Config {

    // Config field values (public so other classes can read them directly)
    public static int baseCapacity = 32000;
    public static int baseCapacityDowngraded = 1000;
    public static boolean quantifyShowsFluidName = true;

    public static void synchronizeConfiguration(File configFile) {
        Configuration configuration = new Configuration(configFile);

        baseCapacity = configuration.getInt(
            "baseCapacity",
            Configuration.CATEGORY_GENERAL,
            32000,
            1,
            Integer.MAX_VALUE,
            "The base capacity, in millibuckets (mB), of a basic storage tank (without a capacity downgrade).");
        baseCapacityDowngraded = configuration.getInt(
            "baseCapacityDowngraded",
            Configuration.CATEGORY_GENERAL,
            1000,
            1,
            Integer.MAX_VALUE,
            "The base capacity, in millibuckets (mB), of a basic storage tank with a capacity downgrade installed.");
        quantifyShowsFluidName = configuration.getBoolean(
            "quantifyShowsFluidName",
            Configuration.CATEGORY_GENERAL,
            true,
            "Whether fluid names should be shown or not when drawers have the quantity display enabled via a quantify key.");

        if (configuration.hasChanged()) {
            configuration.save();
        }
    }
}
