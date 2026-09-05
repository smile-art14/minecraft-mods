package shenmi.gridworld;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ShenmiGridWorld implements ModInitializer {
    public static final String MOD_ID = "shenmi_grid_world";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        GridConfig.load();
        LOGGER.info(
                "Shenmi Grid World initialized: spacing={}, thickness={}",
                GridConfig.spacing(),
                GridConfig.thickness());
    }
}
