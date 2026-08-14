package io.github.trouvaiilx.xray;

import io.github.trouvaiilx.xray.network.XrayNetworkRegistry;
import net.fabricmc.api.ModInitializer;

/**
 * Common entrypoint for X-Ray Mod.
 */
public final class XrayCommon implements ModInitializer {
    @Override
    public void onInitialize() {
        XrayNetworkRegistry.register();
    }
}
