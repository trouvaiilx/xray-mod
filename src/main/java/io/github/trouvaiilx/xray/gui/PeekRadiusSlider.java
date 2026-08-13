package io.github.trouvaiilx.xray.gui;

import io.github.trouvaiilx.xray.config.XrayConfig;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;

public final class PeekRadiusSlider extends AbstractSliderButton {
    private static final int RANGE = XrayConfig.MAX_PEEK_RADIUS - XrayConfig.MIN_PEEK_RADIUS;

    public PeekRadiusSlider(int x, int y, int width, int height) {
        super(x, y, width, height, Component.empty(), valueToProgress(XrayConfig.getPeekRadius()));
        this.updateMessage();
    }

    private static double valueToProgress(int radius) {
        return (radius - XrayConfig.MIN_PEEK_RADIUS) / (double) RANGE;
    }

    private int progressToValue() {
        return (int) Math.round(XrayConfig.MIN_PEEK_RADIUS + this.value * RANGE);
    }

    @Override
    protected void updateMessage() {
        this.setMessage(Component.literal("Peek Radius: " + this.progressToValue() + " blocks"));
    }

    @Override
    protected void applyValue() {
        XrayConfig.setPeekRadius(this.progressToValue());
    }
}
