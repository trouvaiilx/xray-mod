package io.github.trouvaiilx.xray.gui;

import io.github.trouvaiilx.xray.config.XrayConfig;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;

/**
 * A standard vanilla AbstractSliderButton mapping [0, 1] to
 * [XrayConfig.MIN_RENDER_DISTANCE, XrayConfig.MAX_RENDER_DISTANCE] chunks. The range is capped
 * at 32 (XrayConfig.MAX_RENDER_DISTANCE) -- the same ceiling vanilla's own render distance
 * option uses -- specifically so this can never be set to something that forces X-ray-driven
 * rebuilds far outside a sane view distance; see the product requirement to avoid encouraging
 * performance-hurting settings.
 */
public final class RenderDistanceSlider extends AbstractSliderButton {
    private static final int RANGE = XrayConfig.MAX_RENDER_DISTANCE - XrayConfig.MIN_RENDER_DISTANCE;

    public RenderDistanceSlider(int x, int y, int width, int height) {
        super(x, y, width, height, Component.empty(), valueToProgress(XrayConfig.getRenderDistance()));
        this.updateMessage();
    }

    private static double valueToProgress(int chunks) {
        return (chunks - XrayConfig.MIN_RENDER_DISTANCE) / (double) RANGE;
    }

    private int progressToValue() {
        return (int) Math.round(XrayConfig.MIN_RENDER_DISTANCE + this.value * RANGE);
    }

    @Override
    protected void updateMessage() {
        this.setMessage(Component.literal("X-ray Distance: " + this.progressToValue() + " chunks"));
    }

    @Override
    protected void applyValue() {
        // Fires continuously while dragging -- XrayConfig#setRenderDistance only marks the
        // config dirty (debounced autosave on the next tick), it doesn't write to disk on
        // every pixel of drag. See XrayConfig#tick().
        XrayConfig.setRenderDistance(this.progressToValue());
    }
}
