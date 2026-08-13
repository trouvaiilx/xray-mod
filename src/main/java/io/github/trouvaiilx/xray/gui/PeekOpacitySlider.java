package io.github.trouvaiilx.xray.gui;

import io.github.trouvaiilx.xray.config.XrayConfig;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;

public final class PeekOpacitySlider extends AbstractSliderButton {
    private static final int RANGE = XrayConfig.MAX_PEEK_OPACITY - XrayConfig.MIN_PEEK_OPACITY;

    public PeekOpacitySlider(int x, int y, int width, int height) {
        super(x, y, width, height, Component.empty(), valueToProgress(XrayConfig.getPeekOpacity()));
        this.updateMessage();
    }

    private static double valueToProgress(int opacity) {
        return (opacity - XrayConfig.MIN_PEEK_OPACITY) / (double) RANGE;
    }

    private int progressToValue() {
        return (int) Math.round(XrayConfig.MIN_PEEK_OPACITY + this.value * RANGE);
    }

    @Override
    protected void updateMessage() {
        this.setMessage(Component.literal("Peek Opacity: " + this.progressToValue() + "%"));
    }

    @Override
    protected void applyValue() {
        XrayConfig.setPeekOpacity(this.progressToValue());
    }
}
