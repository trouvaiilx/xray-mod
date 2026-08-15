package io.github.trouvaiilx.xray.gui;

import io.github.trouvaiilx.xray.config.XrayConfig;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;

import java.util.Locale;

public final class PeekThicknessSlider extends AbstractSliderButton {
    private static final float RANGE = XrayConfig.MAX_PEEK_THICKNESS - XrayConfig.MIN_PEEK_THICKNESS;

    public PeekThicknessSlider(int x, int y, int width, int height) {
        super(x, y, width, height, Component.empty(), valueToProgress(XrayConfig.getPeekThickness()));
        this.updateMessage();
    }

    private static double valueToProgress(float thickness) {
        return (thickness - XrayConfig.MIN_PEEK_THICKNESS) / (double) RANGE;
    }

    private float progressToValue() {
        double raw = XrayConfig.MIN_PEEK_THICKNESS + this.value * RANGE;
        return (float) (Math.round(raw * 10.0) / 10.0);
    }

    @Override
    protected void updateMessage() {
        this.setMessage(Component.literal("Peek Thickness: " + String.format(Locale.ROOT, "%.1f", this.progressToValue()) + "px"));
    }

    @Override
    protected void applyValue() {
        XrayConfig.setPeekThickness(this.progressToValue());
    }
}
