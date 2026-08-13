package com.example.xray.gui;

import com.example.xray.config.XrayConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A creative-inventory-style grid of block icons. Clicking a cell toggles that block's
 * whitelist membership (see XrayConfig#toggleWhitelist); hovering shows the block's real name.
 *
 * Verified 26.2 widget shape (Fabric docs "Custom Widgets 26.2", cross-checked against
 * Sodium's own extractRenderState-based screens): extend AbstractWidget, override
 * extractWidgetRenderState(GuiGraphicsExtractor, mouseX, mouseY, delta) instead of the old
 * renderWidget(GuiGraphics, ...), and updateWidgetNarration(NarrationElementOutput). Click/
 * scroll handling uses the newer MouseButtonEvent-based overloads, matching what Sodium's own
 * VideoSettingsScreen (mouseClicked(MouseButtonEvent, boolean)) and mouseScrolled(double,
 * double, double, double) already use in this exact Sodium snapshot.
 */
public final class BlockGridWidget extends AbstractWidget {
    private static final int CELL_SIZE = 18;

    private final Map<Block, ItemStack> iconCache = new HashMap<>();
    private final Runnable onToggle;
    private List<Block> blocks = List.of();
    private int scrollOffset = 0;
    private int hoveredIndex = -1;

    public BlockGridWidget(int x, int y, int width, int height, Runnable onToggle) {
        super(x, y, width, height, Component.empty());
        this.onToggle = onToggle;
    }

    public void setBlocks(List<Block> blocks) {
        setBlocks(blocks, true);
    }

    public void setBlocks(List<Block> blocks, boolean resetScroll) {
        this.blocks = blocks;
        if (resetScroll) {
            this.scrollOffset = 0;
        } else {
            this.scrollOffset = Math.max(0, Math.min(maxScroll(), this.scrollOffset));
        }
    }

    private int columns() {
        return Math.max(1, this.width / CELL_SIZE);
    }

    private int rows() {
        return Math.max(1, this.height / CELL_SIZE);
    }

    private int maxScroll() {
        int totalRows = (this.blocks.size() + columns() - 1) / columns();
        return Math.max(0, totalRows - rows());
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.fill(getX(), getY(), getX() + this.width, getY() + this.height, 0xB0101010);

        int columns = columns();
        int firstIndex = this.scrollOffset * columns;
        int visibleRows = rows() + 1; // draw one extra partial row so scrolling doesn't pop
        int lastIndex = Math.min(this.blocks.size(), firstIndex + columns * visibleRows);

        this.hoveredIndex = -1;
        Minecraft mc = Minecraft.getInstance();

        for (int i = firstIndex; i < lastIndex; i++) {
            int slot = i - firstIndex;
            int col = slot % columns;
            int row = slot / columns;
            int cellX = getX() + col * CELL_SIZE;
            int cellY = getY() + row * CELL_SIZE;
            if (cellY + CELL_SIZE > getY() + this.height + CELL_SIZE) {
                break;
            }

            Block block = this.blocks.get(i);
            boolean hovered = mouseX >= cellX && mouseX < cellX + CELL_SIZE
                    && mouseY >= cellY && mouseY < cellY + CELL_SIZE
                    && mouseX >= getX() && mouseX < getX() + this.width
                    && mouseY >= getY() && mouseY < getY() + this.height;
            if (hovered) {
                this.hoveredIndex = i;
            }

            boolean whitelisted = XrayConfig.isWhitelisted(block);
            int bg = whitelisted ? 0xFF3A7A3A : (hovered ? 0xFF454545 : 0xFF2A2A2A);
            graphics.fill(cellX, cellY, cellX + CELL_SIZE, cellY + CELL_SIZE, bg);
            if (whitelisted) {
                graphics.outline(cellX, cellY, CELL_SIZE, CELL_SIZE, 0xFF7CFC7C);
            } else if (hovered) {
                graphics.outline(cellX, cellY, CELL_SIZE, CELL_SIZE, 0xFFFFFFFF);
            }

            ItemStack stack = this.iconCache.computeIfAbsent(block, b -> new ItemStack(b.asItem()));
            if (!stack.isEmpty()) {
                graphics.item(stack, cellX + 1, cellY + 1);
            }

            // High-visibility top-right green indicator badge for whitelisted items
            if (whitelisted) {
                graphics.fill(cellX + CELL_SIZE - 4, cellY + 1, cellX + CELL_SIZE - 1, cellY + 4, 0xFF7CFC7C);
            }
        }

        if (this.hoveredIndex >= 0) {
            Component name = this.blocks.get(this.hoveredIndex).getName();
            int tw = mc.font.width(name);
            int tx = Math.min(mouseX + 10, getX() + this.width - tw - 6);
            int ty = mouseY - 4;
            // Styled tooltip box with subtle border
            graphics.fill(tx - 4, ty - 3, tx + tw + 4, ty + mc.font.lineHeight + 3, 0xF0101010);
            graphics.outline(tx - 4, ty - 3, tw + 8, mc.font.lineHeight + 6, 0xFF555555);
            graphics.text(mc.font, name, tx, ty, 0xFFFFFFFF, false);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (this.hoveredIndex >= 0 && this.hoveredIndex < this.blocks.size() && this.active && this.visible) {
            XrayConfig.toggleWhitelist(this.blocks.get(this.hoveredIndex));
            if (this.onToggle != null) {
                this.onToggle.run();
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!this.visible) {
            return false;
        }
        this.scrollOffset = Math.max(0, Math.min(maxScroll(), this.scrollOffset - (int) Math.signum(scrollY)));
        return true;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput builder) {
        builder.add(net.minecraft.client.gui.narration.NarratedElementType.TITLE,
                Component.literal("X-ray block selector, " + this.blocks.size() + " blocks"));
    }
}
