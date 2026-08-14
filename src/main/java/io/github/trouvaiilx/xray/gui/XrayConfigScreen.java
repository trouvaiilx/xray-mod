package io.github.trouvaiilx.xray.gui;

import io.github.trouvaiilx.xray.config.BlockCategory;
import io.github.trouvaiilx.xray.config.XrayConfig;
import io.github.trouvaiilx.xray.config.XrayPresets;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Intuitive, optimized in-game X-ray control panel screen.
 * Opened with Right Shift by default.
 */
public final class XrayConfigScreen extends Screen {
    private static final class BlockSearchEntry {
        final Block block;
        final String idPath;
        final String displayNameLower;
        final BlockCategory category;

        BlockSearchEntry(Block block, String idPath, String displayNameLower, BlockCategory category) {
            this.block = block;
            this.idPath = idPath;
            this.displayNameLower = displayNameLower;
            this.category = category;
        }
    }

    private static List<BlockSearchEntry> allBlockEntries;

    private EditBox searchBox;
    private BlockGridWidget grid;
    private Button categoryButton;
    private Button alwaysFluidsButton;
    private Button fullbrightButton;
    private Button peekModeButton;
    private Button presetButton;

    private BlockCategory category = BlockCategory.ALL;
    private int presetIndex = -1;

    public XrayConfigScreen() {
        super(Component.literal("X-ray Settings"));
    }

    @Override
    protected void init() {
        int panelWidth = Math.min(340, this.width - 20);
        int panelHeight = Math.min(320, this.height - 20);
        int left = (this.width - panelWidth) / 2;
        int top = (this.height - panelHeight) / 2;

        int y = top + 22; // Leave space for header text

        // 1. Search Box
        this.searchBox = new EditBox(this.font, left, y, panelWidth, 18, Component.literal("Search blocks"));
        this.searchBox.setHint(Component.literal("Search blocks by name or id..."));
        this.searchBox.setResponder(text -> refreshGrid(true));
        this.addRenderableWidget(this.searchBox);
        this.setInitialFocus(this.searchBox);
        y += 22;

        // 2. Quick Category Bar
        int catButtonWidth = panelWidth - 40;
        this.addRenderableWidget(Button.builder(Component.literal("◄"), b -> cycleCategory(-1))
                .bounds(left, y, 18, 18).build());
        this.categoryButton = Button.builder(Component.literal("Category: " + this.category.displayName), b -> cycleCategory(1))
                .bounds(left + 20, y, catButtonWidth, 18).build();
        this.addRenderableWidget(this.categoryButton);
        this.addRenderableWidget(Button.builder(Component.literal("►"), b -> cycleCategory(1))
                .bounds(left + 22 + catButtonWidth, y, 18, 18).build());
        y += 22;

        // 3. Block Grid
        int gridHeight = panelHeight - 22 - 22 - 22 - 22 - 22 - 22 - 22 - 10;
        this.grid = new BlockGridWidget(left, y, panelWidth, Math.max(54, gridHeight), this::onWhitelistToggled);
        this.addRenderableWidget(this.grid);
        y += this.grid.getHeight() + 4;

        // 4. Sliders and Control Buttons
        this.addRenderableWidget(new RenderDistanceSlider(left, y, panelWidth, 18));
        y += 22;

        int halfWidth = (panelWidth - 2) / 2;

        this.peekModeButton = Button.builder(peekModeLabel(), b -> togglePeekMode())
                .bounds(left, y, halfWidth, 18).build();
        this.addRenderableWidget(this.peekModeButton);

        this.addRenderableWidget(new PeekRadiusSlider(left + halfWidth + 2, y, halfWidth, 18));
        y += 22;

        this.addRenderableWidget(new PeekOpacitySlider(left, y, halfWidth, 18));

        this.fullbrightButton = Button.builder(fullbrightLabel(), b -> toggleFullbright())
                .bounds(left + halfWidth + 2, y, halfWidth, 18).build();
        this.addRenderableWidget(this.fullbrightButton);
        y += 22;

        this.alwaysFluidsButton = Button.builder(alwaysFluidsLabel(), b -> toggleAlwaysFluids())
                .bounds(left, y, halfWidth, 18).build();
        this.addRenderableWidget(this.alwaysFluidsButton);

        this.presetButton = Button.builder(presetLabel(), b -> cyclePreset())
                .bounds(left + halfWidth + 2, y, halfWidth, 18).build();
        this.addRenderableWidget(this.presetButton);
        y += 22;

        // 5. Close Button
        this.addRenderableWidget(Button.builder(Component.literal("Close Settings"), b -> this.onClose())
                .bounds(left, y, panelWidth, 18).build());

        if (allBlockEntries == null) {
            loadAllBlocks();
        }
        refreshGrid(true);
    }

    private static void loadAllBlocks() {
        List<BlockSearchEntry> list = new ArrayList<>();
        for (Block block : BuiltInRegistries.BLOCK) {
            if (block == Blocks.AIR) {
                continue;
            }
            String idPath = BuiltInRegistries.BLOCK.getKey(block).getPath().toLowerCase(Locale.ROOT);
            String nameLower = block.getName().getString().toLowerCase(Locale.ROOT);
            BlockCategory cat = BlockCategory.classify(block);
            list.add(new BlockSearchEntry(block, idPath, nameLower, cat));
        }
        list.sort(Comparator.comparing(e -> BuiltInRegistries.BLOCK.getKey(e.block).toString()));
        allBlockEntries = list;
    }

    private void cycleCategory(int direction) {
        BlockCategory[] values = BlockCategory.values();
        int next = (this.category.ordinal() + direction + values.length) % values.length;
        this.category = values[next];
        this.categoryButton.setMessage(Component.literal("Category: " + this.category.displayName));
        refreshGrid(true);
    }

    private void onWhitelistToggled() {
        this.presetButton.setMessage(presetLabel());
        refreshGrid(false);
    }

    private void refreshGrid(boolean resetScroll) {
        String query = this.searchBox.getValue().trim().toLowerCase(Locale.ROOT);
        List<Block> filtered = new ArrayList<>();
        for (BlockSearchEntry entry : allBlockEntries) {
            if (this.category != BlockCategory.ALL && entry.category != this.category) {
                continue;
            }
            if (!query.isEmpty()) {
                if (!entry.idPath.contains(query) && !entry.displayNameLower.contains(query)) {
                    continue;
                }
            }
            filtered.add(entry.block);
        }

        if (this.category == BlockCategory.ALL) {
            filtered.sort((b1, b2) -> {
                boolean w1 = XrayConfig.isWhitelisted(b1);
                boolean w2 = XrayConfig.isWhitelisted(b2);
                if (w1 != w2) {
                    return w1 ? -1 : 1;
                }
                return 0;
            });
        }

        this.grid.setBlocks(filtered, resetScroll);
    }

    private void toggleAlwaysFluids() {
        XrayConfig.setAlwaysShowFluids(!XrayConfig.isAlwaysShowFluids());
        this.alwaysFluidsButton.setMessage(alwaysFluidsLabel());
        refreshGrid(false);
    }

    private Component alwaysFluidsLabel() {
        return Component.literal("Always Fluids: " + (XrayConfig.isAlwaysShowFluids() ? "ON" : "OFF"));
    }

    private void toggleFullbright() {
        XrayConfig.setFullbright(!XrayConfig.isFullbright());
        this.fullbrightButton.setMessage(fullbrightLabel());
    }

    private Component fullbrightLabel() {
        return Component.literal("Fullbright: " + (XrayConfig.isFullbright() ? "ON" : "OFF"));
    }

    private void togglePeekMode() {
        XrayConfig.setPeekEnabled(!XrayConfig.isPeekEnabled());
        this.peekModeButton.setMessage(peekModeLabel());
    }

    private Component peekModeLabel() {
        return Component.literal("Peek Mode: " + (XrayConfig.isPeekEnabled() ? "ON" : "OFF"));
    }

    private void cyclePreset() {
        this.presetIndex = (this.presetIndex + 1) % XrayPresets.SELECTABLE.length;
        XrayConfig.applyPreset(XrayPresets.SELECTABLE[this.presetIndex]);
        this.presetButton.setMessage(presetLabel());
        refreshGrid(true);
    }

    private Component presetLabel() {
        return Component.literal("Preset: " + XrayConfig.getActivePreset());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);

        // Header Title
        graphics.text(this.font, this.title, (this.width - this.font.width(this.title)) / 2, 4, 0xFFFFFFFF, true);

        // Subtitle badge showing active whitelisted block count and server opt-in status
        int whitelistedCount = XrayConfig.getWhitelistIds().size();
        Component subTitle;
        int statusColor;
        if (io.github.trouvaiilx.xray.core.state.XrayServerConsent.isSingleplayer()) {
            subTitle = Component.literal("Whitelisted: " + whitelistedCount + " blocks • Singleplayer (Allowed)");
            statusColor = 0xFF00FF66;
        } else if (io.github.trouvaiilx.xray.core.state.XrayServerConsent.isServerOptedIn()) {
            subTitle = Component.literal("Whitelisted: " + whitelistedCount + " blocks • Server Opt-In: Granted");
            statusColor = 0xFF00FF66;
        } else {
            subTitle = Component.literal("Whitelisted: " + whitelistedCount + " blocks • Server Opt-In: Required (Disabled)");
            statusColor = 0xFFFF5555;
        }
        graphics.text(this.font, subTitle, (this.width - this.font.width(subTitle)) / 2, 14, statusColor, true);
    }

    @Override
    public void onClose() {
        super.onClose();
        io.github.trouvaiilx.xray.XrayClient.refreshRender();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
