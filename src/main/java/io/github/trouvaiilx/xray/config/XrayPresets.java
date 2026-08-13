package io.github.trouvaiilx.xray.config;

import java.util.Set;

/**
 * Legacy forwarding wrapper for core.model.XrayPresets.
 */
public final class XrayPresets {
    public static final String DEFAULT = io.github.trouvaiilx.xray.core.model.XrayPresets.DEFAULT;
    public static final String ORES_ONLY = io.github.trouvaiilx.xray.core.model.XrayPresets.ORES_ONLY;
    public static final String FLUIDS_ONLY = io.github.trouvaiilx.xray.core.model.XrayPresets.FLUIDS_ONLY;
    public static final String VALUABLES = io.github.trouvaiilx.xray.core.model.XrayPresets.VALUABLES;
    public static final String CUSTOM = io.github.trouvaiilx.xray.core.model.XrayPresets.CUSTOM;
    public static final String[] SELECTABLE = io.github.trouvaiilx.xray.core.model.XrayPresets.SELECTABLE;

    private XrayPresets() {
    }

    public static Set<String> blockIds(String presetName) {
        return io.github.trouvaiilx.xray.core.model.XrayPresets.blockIds(presetName);
    }
}
