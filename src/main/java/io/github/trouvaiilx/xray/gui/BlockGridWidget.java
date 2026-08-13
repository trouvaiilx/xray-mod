package io.github.trouvaiilx.xray.gui;

import io.github.trouvaiilx.xray.config.XrayConfig;
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
 * An optimized, interactive grid of block icons with scrolling and visual feedback.
 * Features a real-time scrollbar, emerald whitelist badges, and allocation-free rendering.
 */
public final class BlockGridWidget extends AbstractWidget {
    private static final int CELL_SIZE = 18;
    private static final int SCROLLBAR_WIDTH = 5;

    private static final Map<Block, ItemStack> ITEM_CACHE = new HashMap<>();

    private final Runnable onToggle;
    private List<Block> blocks = List.of();
    private int scrollOffset = 0;
    private int hoveredIndex = -1;
    private boolean isDraggingScrollbar = false;

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

    private int gridWidth() {
        return this.width - SCROLLBAR_WIDTH - 2;
    }

    private int columns() {
        return Math.max(1, gridWidth() / CELL_SIZE);
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
        // Panel Background & Border
        graphics.fill(getX(), getY(), getX() + this.width, getY() + this.height, 0xD0121417);
        graphics.outline(getX(), getY(), this.width, this.height, 0xFF2A2E35);

        int columns = columns();
        int firstIndex = this.scrollOffset * columns;
        int visibleRows = rows() + 1;
        int lastIndex = Math.min(this.blocks.size(), firstIndex + columns * visibleRows);

        this.hoveredIndex = -1;
        Minecraft mc = Minecraft.getInstance();

        // 1. Render Block Grid Cells
        for (int i = firstIndex; i < lastIndex; i++) {
            int slot = i - firstIndex;
            int col = slot % columns;
            int row = slot / columns;
            int cellX = getX() + 2 + col * CELL_SIZE;
            int cellY = getY() + 2 + row * CELL_SIZE;
            if (cellY + CELL_SIZE > getY() + this.height) {
                break;
            }

            Block block = this.blocks.get(i);
            boolean hovered = mouseX >= cellX && mouseX < cellX + CELL_SIZE
                    && mouseY >= cellY && mouseY < cellY + CELL_SIZE
                    && mouseX >= getX() && mouseX < getX() + gridWidth()
                    && mouseY >= getY() && mouseY < getY() + this.height;

            if (hovered) {
                this.hoveredIndex = i;
            }

            boolean whitelisted = XrayConfig.isWhitelisted(block);

            int bg = whitelisted ? (hovered ? 0xFF286428 : 0xFF1C4A1C) : (hovered ? 0xFF353B42 : 0xFF1E2228);
            graphics.fill(cellX, cellY, cellX + CELL_SIZE, cellY + CELL_SIZE, bg);

            if (whitelisted) {
                graphics.outline(cellX, cellY, CELL_SIZE, CELL_SIZE, 0xFF00FF66);
                // Emerald Top-Right Badge
                graphics.fill(cellX + CELL_SIZE - 4, cellY + 1, cellX + CELL_SIZE - 1, cellY + 4, 0xFF00FF66);
            } else if (hovered) {
                graphics.outline(cellX, cellY, CELL_SIZE, CELL_SIZE, 0xFF708090);
            }

            ItemStack stack = ITEM_CACHE.computeIfAbsent(block, b -> new ItemStack(b.asItem()));
            if (!stack.isEmpty()) {
                graphics.item(stack, cellX + 1, cellY + 1);
            }
        }

        // 2. Render Vertical Scrollbar
        int maxScroll = maxScroll();
        if (maxScroll > 0) {
            int trackX = getX() + this.width - SCROLLBAR_WIDTH - 1;
            int trackY = getY() + 2;
            int trackHeight = this.height - 4;

            graphics.fill(trackX, trackY, trackX + SCROLLBAR_WIDTH, trackY + trackHeight, 0xFF181A1F);

            int thumbHeight = Math.max(12, trackHeight * rows() / ((this.blocks.size() + columns() - 1) / columns()));
            int thumbY = trackY + (trackHeight - thumbHeight) * this.scrollOffset / maxScroll;

            boolean thumbHovered = mouseX >= trackX && mouseX <= trackX + SCROLLBAR_WIDTH
                    && mouseY >= trackY && mouseY <= trackY + trackHeight;

            int thumbColor = (this.isDraggingScrollbar || thumbHovered) ? 0xFF00E5FF : 0xFF4A5260;
            graphics.fill(trackX, thumbY, trackX + SCROLLBAR_WIDTH, thumbY + thumbHeight, thumbColor);
        }

        // 3. Render Tooltip
        if (this.hoveredIndex >= 0 && this.hoveredIndex < this.blocks.size()) {
            Block block = this.blocks.get(this.hoveredIndex);
            Component name = block.getName();
            int tw = mc.font.width(name);
            int tx = Math.min(mouseX + 10, getX() + this.width - tw - 8);
            int ty = Math.max(getY() + 4, mouseY - 14);

            graphics.fill(tx - 5, ty - 4, tx + tw + 5, ty + mc.font.lineHeight + 4, 0xF00D0F12);
            graphics.outline(tx - 5, ty - 4, tw + 10, mc.font.lineHeight + 8, 0xFF3E4552);

            int textColor = XrayConfig.isWhitelisted(block) ? 0xFF00FF66 : 0xFFEEEEEE;
            graphics.text(mc.font, name, tx, ty, textColor, false);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (!this.visible || !this.active) {
            return false;
        }

        int maxScroll = maxScroll();
        if (maxScroll > 0) {
            int trackX = getX() + this.width - SCROLLBAR_WIDTH - 3;
            if (event.x() >= trackX && event.x() <= getX() + this.width) {
                this.isDraggingScrollbar = true;
                updateScrollFromMouse(event.y());
                return true;
            }
        }

        if (this.hoveredIndex >= 0 && this.hoveredIndex < this.blocks.size()) {
            XrayConfig.toggleWhitelist(this.blocks.get(this.hoveredIndex));
            if (this.onToggle != null) {
                this.onToggle.run();
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        this.isDraggingScrollbar = false;
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (this.isDraggingScrollbar) {
            updateScrollFromMouse(event.y());
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    private void updateScrollFromMouse(double mouseY) {
        int maxScroll = maxScroll();
        if (maxScroll <= 0) {
            return;
        }
        int trackY = getY() + 2;
        int trackHeight = this.height - 4;
        double relativeY = Math.max(0, Math.min(trackHeight, mouseY - trackY));
        this.scrollOffset = (int) Math.round(relativeY / trackHeight * maxScroll);
        this.scrollOffset = Math.max(0, Math.min(maxScroll, this.scrollOffset));
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
