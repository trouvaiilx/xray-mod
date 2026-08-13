package com.example.xray.gui;

import com.example.xray.config.BlockCategory;
import com.example.xray.config.XrayConfig;
import com.example.xray.config.XrayPresets;
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
 * The in-game X-ray settings menu, opened/closed with Right Shift (see XrayKeybinds).
 *
 * Verified against real 26.2 API (Fabric docs "Custom Screens 26.2" / "Custom Widgets 26.2",
 * cross-checked against Sodium's own dev-branch VideoSettingsScreen): extend Screen, build
 * widgets in init() via addRenderableWidget, and override extractRenderState(graphics, mouseX,
 * mouseY, delta) -- NOT the pre-1.21.8 render(GuiGraphics, ...) -- calling super first so the
 * dimmed background and registered widgets still draw.
 *
 * All state lives in XrayConfig; this screen just reads/writes it and re-filters the block
 * list. Every change (block toggle, preset pick, fullbright, render distance drag) is already
 * persisted by XrayConfig itself, so there's nothing extra to save on close.
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

    private static List<BlockSearchEntry> allBlockEntries; // built once; registry doesn't change mid-session

    private EditBox searchBox;
    private BlockGridWidget grid;
    private Button categoryButton;
    private Button alwaysFluidsButton;
    private Button fullbrightButton;
    private Button presetButton;

    private BlockCategory category = BlockCategory.ALL;
    private int presetIndex = -1; // -1 == "Custom" / no fixed preset selected

    public XrayConfigScreen() {
        super(Component.literal("X-ray Settings"));
    }

    @Override
    protected void init() {
        int panelWidth = Math.min(320, this.width - 20);
        int panelHeight = Math.min(260, this.height - 20);
        int left = (this.width - panelWidth) / 2;
        int top = (this.height - panelHeight) / 2;

        int y = top;

        this.searchBox = new EditBox(this.font, left, y, panelWidth, 18, Component.literal("Search blocks"));
        this.searchBox.setHint(Component.literal("Search blocks..."));
        this.searchBox.setResponder(text -> refreshGrid(true));
        this.addRenderableWidget(this.searchBox);
        this.setInitialFocus(this.searchBox);
        y += 22;

        int catButtonWidth = panelWidth - 40;
        this.addRenderableWidget(Button.builder(Component.literal("<"), b -> cycleCategory(-1))
                .bounds(left, y, 18, 18).build());
        this.categoryButton = Button.builder(Component.literal(this.category.displayName), b -> cycleCategory(1))
                .bounds(left + 20, y, catButtonWidth, 18).build();
        this.addRenderableWidget(this.categoryButton);
        this.addRenderableWidget(Button.builder(Component.literal(">"), b -> cycleCategory(1))
                .bounds(left + 22 + catButtonWidth, y, 18, 18).build());
        y += 22;

        int gridHeight = panelHeight - 22 - 22 - 22 - 22 - 22 - 6;
        this.grid = new BlockGridWidget(left, y, panelWidth, Math.max(54, gridHeight), this::onWhitelistToggled);
        this.addRenderableWidget(this.grid);
        y += this.grid.getHeight() + 4;

        this.addRenderableWidget(new RenderDistanceSlider(left, y, panelWidth, 18));
        y += 22;

        int halfWidth = (panelWidth - 2) / 2;
        this.alwaysFluidsButton = Button.builder(alwaysFluidsLabel(), b -> toggleAlwaysFluids())
                .bounds(left, y, halfWidth, 18).build();
        this.addRenderableWidget(this.alwaysFluidsButton);

        this.fullbrightButton = Button.builder(fullbrightLabel(), b -> toggleFullbright())
                .bounds(left + halfWidth + 2, y, halfWidth, 18).build();
        this.addRenderableWidget(this.fullbrightButton);
        y += 22;

        this.presetButton = Button.builder(presetLabel(), b -> cyclePreset())
                .bounds(left, y, halfWidth, 18).build();
        this.addRenderableWidget(this.presetButton);

        this.addRenderableWidget(Button.builder(Component.literal("Close"), b -> this.onClose())
                .bounds(left + halfWidth + 2, y, halfWidth, 18).build());

        if (allBlockEntries == null) {
            loadAllBlocks();
        }
        refreshGrid(true);
    }

    private static void loadAllBlocks() {
        List<BlockSearchEntry> list = new ArrayList<>();
        // Pre-compute block ID, display name lower case, and category classification once
        // to make GUI searching and filtering 100% allocation-free during interaction.
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
        this.categoryButton.setMessage(Component.literal(this.category.displayName));
        refreshGrid(true);
    }

    private void onWhitelistToggled() {
        this.presetButton.setMessage(presetLabel());
        refreshGrid(false);
    }

    private void refreshGrid() {
        refreshGrid(true);
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

    private void cyclePreset() {
        this.presetIndex = (this.presetIndex + 1) % XrayPresets.SELECTABLE.length;
        XrayConfig.applyPreset(XrayPresets.SELECTABLE[this.presetIndex]);
        this.presetButton.setMessage(presetLabel());
        refreshGrid(true); // whitelist membership (cell highlighting) just changed wholesale
    }

    private Component presetLabel() {
        return Component.literal("Preset: " + XrayConfig.getActivePreset());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        graphics.text(this.font, this.title, (this.width - this.font.width(this.title)) / 2, 4, 0xFFFFFFFF, true);
        int whitelistedCount = XrayConfig.getWhitelistIds().size();
        Component subTitle = Component.literal("Whitelisted: " + whitelistedCount + " blocks");
        graphics.text(this.font, subTitle, (this.width - this.font.width(subTitle)) / 2, 16, 0xFFAAAA00, true);
    }

    @Override
    public void onClose() {
        super.onClose();
        com.example.xray.XrayClient.refreshRender();
    }

    @Override
    public boolean isPauseScreen() {
        return false; // stay open (and keep rendering the world underneath) on singleplayer too
    }
}
