package io.github.trouvaiilx.xray.core.config;

import io.github.trouvaiilx.xray.core.model.XrayPresets;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Data Transfer Object (DTO) for persisted X-ray configuration JSON serialization.
 */
public final class XrayConfigData {
    public static final int MIN_RENDER_DISTANCE = 2;
    public static final int MAX_RENDER_DISTANCE = 32;
    public static final int DEFAULT_RENDER_DISTANCE = 8;

    public static final int MIN_PEEK_RADIUS = 1;
    public static final int MAX_PEEK_RADIUS = 10;
    public static final int DEFAULT_PEEK_RADIUS = 4;

    public static final int MIN_PEEK_OPACITY = 1;
    public static final int MAX_PEEK_OPACITY = 100;
    public static final int DEFAULT_PEEK_OPACITY = 40;

    public static final int DEFAULT_PEEK_COLOR = 0x00E5FF;

    public int renderDistance = DEFAULT_RENDER_DISTANCE;
    public boolean fullbright = true;
    public boolean alwaysShowFluids = true;
    public boolean peekEnabled = true;
    public int peekRadius = DEFAULT_PEEK_RADIUS;
    public int peekOpacity = DEFAULT_PEEK_OPACITY;
    public int peekColor = DEFAULT_PEEK_COLOR;
    public String activePreset = XrayPresets.DEFAULT;
    public Set<String> whitelist = new LinkedHashSet<>();
}
