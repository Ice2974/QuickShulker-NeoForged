package com.ice2974.quickshulkerneoforged.neoforge.client;

import com.ice2974.quickshulkerneoforged.common.config.KeyBindingSpec;
import com.ice2974.quickshulkerneoforged.common.config.QuickShulkerConfig;
import com.ice2974.quickshulkerneoforged.neoforge.NeoForgeQuickShulkerConfig;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.Objects;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public final class NeoForgeQuickShulkerConfigScreen extends Screen {
    private static final int BUTTON_WIDTH = 170;
    private static final int FULL_WIDTH = BUTTON_WIDTH * 2 + 12;
    private static final int TAB_WIDTH = 100;
    private static final int TAB_GAP = 6;
    private static final int ROW_HEIGHT = 24;
    private static final int COLUMN_GAP = 12;
    private static final int SECTION_GAP = 8;
    private static final int BOTTOM_MARGIN = 28;
    private static final Component TITLE = Component.translatable("screen.quickshulker_neoforged.config.title");
    private static final Component CAPTURE_HINT = Component.translatable("screen.quickshulker_neoforged.config.capture_hint");
    private static final Component UNIMPLEMENTED_HINT = Component.translatable("screen.quickshulker_neoforged.config.unimplemented");

    private final Screen parent;
    private String activationKey;
    private String openSettingsKey;
    private boolean rightClickToOpen;
    private boolean keybindInHand;
    private boolean keybindInInventory;
    private boolean rightClickInInventory;
    private boolean supportsBundlingInsert;
    private boolean supportsBundlingPickup;
    private boolean supportsBundlingTransfer;
    private boolean supportsBundlingExtract;
    private boolean supportsMouseDragged;
    private boolean openSettingsKeyEnabled;
    private boolean quickShulkerBox;
    private boolean quickCraftingTables;
    private boolean quickStonecutter;
    private boolean quickEnderChest;
    private boolean quickAnvil;
    private KeyTarget capturingTarget = KeyTarget.NONE;
    private ConfigPage currentPage = ConfigPage.ACTIVATION;
    private Button activationKeyButton;
    private Button openSettingsKeyButton;
    private String loadedActivationKey;
    private String loadedOpenSettingsKey;

    public NeoForgeQuickShulkerConfigScreen(Screen parent) {
        super(TITLE);
        this.parent = parent;
        loadState(NeoForgeQuickShulkerConfig.snapshot());
    }

    @Override
    protected void init() {
        super.init();
        clearWidgets();

        int left = this.width / 2 - BUTTON_WIDTH - COLUMN_GAP / 2;
        int right = this.width / 2 + COLUMN_GAP / 2;

        addPageTabs();
        switch (currentPage) {
            case ACTIVATION -> initActivationPage(left, right);
            case QUICK_OPEN -> initQuickOpenPage(left, right);
            case BUNDLING -> initBundlingPage(left, right);
        }

        addFooter(left, right);
        syncDisplayedKeysFromMappings();
        refreshKeyButtons();
    }

    @Override
    public void onClose() {
        saveAndClose();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (capturingTarget != KeyTarget.NONE) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                capturingTarget = KeyTarget.NONE;
                refreshKeyButtons();
                return true;
            }

            String keyName = InputConstants.getKey(keyCode, scanCode).getName();
            if (capturingTarget == KeyTarget.ACTIVATION) {
                activationKey = keyName;
            } else if (capturingTarget == KeyTarget.OPEN_SETTINGS) {
                openSettingsKey = keyName;
            }
            capturingTarget = KeyTarget.NONE;
            refreshKeyButtons();
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 16, 0xFFFFFF);
        guiGraphics.drawCenteredString(
            this.font,
            Component.translatable(currentPage.titleKey),
            this.width / 2,
            58,
            0xE0E0E0
        );
        if (capturingTarget != KeyTarget.NONE) {
            guiGraphics.drawCenteredString(this.font, CAPTURE_HINT, this.width / 2, 28, 0xFFFF55);
        }
        if (currentPage == ConfigPage.BUNDLING) {
            guiGraphics.drawCenteredString(this.font, UNIMPLEMENTED_HINT, this.width / 2, this.height - 52, 0xA0A0A0);
        }
    }

    private void addPageTabs() {
        int totalWidth = TAB_WIDTH * ConfigPage.values().length + TAB_GAP * (ConfigPage.values().length - 1);
        int x = this.width / 2 - totalWidth / 2;
        int y = 36;
        for (ConfigPage page : ConfigPage.values()) {
            Button tabButton = addRenderableWidget(Button.builder(Component.translatable(page.tabKey), button -> {
                currentPage = page;
                capturingTarget = KeyTarget.NONE;
                init();
            }).bounds(x, y, TAB_WIDTH, 20).build());
            tabButton.active = page != currentPage;
            x += TAB_WIDTH + TAB_GAP;
        }
    }

    private void initActivationPage(int left, int right) {
        int top = 78;

        activationKeyButton = addRenderableWidget(Button.builder(Component.empty(), button -> {
            capturingTarget = KeyTarget.ACTIVATION;
            refreshKeyButtons();
        }).bounds(left, top, FULL_WIDTH, 20).build());
        top += ROW_HEIGHT;

        openSettingsKeyButton = addRenderableWidget(Button.builder(Component.empty(), button -> {
            capturingTarget = KeyTarget.OPEN_SETTINGS;
            refreshKeyButtons();
        }).bounds(left, top, FULL_WIDTH, 20).build());
        top += ROW_HEIGHT + SECTION_GAP;

        addToggle(left, top, "config.quickshulker_neoforged.right_click_to_open", rightClickToOpen, value -> rightClickToOpen = value);
        addToggle(right, top, "config.quickshulker_neoforged.keybind_in_hand", keybindInHand, value -> keybindInHand = value);
        top += ROW_HEIGHT;
        addToggle(left, top, "config.quickshulker_neoforged.keybind_in_inventory", keybindInInventory, value -> keybindInInventory = value);
        addToggle(right, top, "config.quickshulker_neoforged.right_click_in_inventory", rightClickInInventory, value -> rightClickInInventory = value);
        top += ROW_HEIGHT;
        addToggle(left, top, "config.quickshulker_neoforged.open_settings_key_enabled", openSettingsKeyEnabled, value -> openSettingsKeyEnabled = value);
    }

    private void initQuickOpenPage(int left, int right) {
        int top = 96;

        addToggle(left, top, "config.quickshulker_neoforged.quick_shulker_box", quickShulkerBox, value -> quickShulkerBox = value);
        addToggle(right, top, "config.quickshulker_neoforged.quick_crafting_table", quickCraftingTables, value -> quickCraftingTables = value);
        top += ROW_HEIGHT;
        addToggle(left, top, "config.quickshulker_neoforged.quick_stonecutter", quickStonecutter, value -> quickStonecutter = value);
        addToggle(right, top, "config.quickshulker_neoforged.quick_ender_chest", quickEnderChest, value -> quickEnderChest = value);
        top += ROW_HEIGHT;
        addToggle(left, top, "config.quickshulker_neoforged.quick_anvil", quickAnvil, value -> quickAnvil = value);
    }

    private void initBundlingPage(int left, int right) {
        int top = 96;

        addToggle(left, top, "config.quickshulker_neoforged.supports_bundling_insert", supportsBundlingInsert, value -> supportsBundlingInsert = value);
        addToggle(right, top, "config.quickshulker_neoforged.supports_bundling_pickup", supportsBundlingPickup, value -> supportsBundlingPickup = value);
        top += ROW_HEIGHT;
        addToggle(left, top, "config.quickshulker_neoforged.supports_bundling_transfer", supportsBundlingTransfer, value -> supportsBundlingTransfer = value);
        addToggle(right, top, "config.quickshulker_neoforged.supports_bundling_extract", supportsBundlingExtract, value -> supportsBundlingExtract = value);
        top += ROW_HEIGHT;
        CycleButton<Boolean> mouseDraggedButton = addToggle(
            left,
            top,
            "config.quickshulker_neoforged.supports_mouse_dragged",
            supportsMouseDragged,
            value -> supportsMouseDragged = value
        );
        mouseDraggedButton.active = false;
    }

    private void addFooter(int left, int right) {
        int footerY = this.height - BOTTOM_MARGIN;
        addRenderableWidget(Button.builder(Component.translatable("screen.quickshulker_neoforged.config.defaults"), button -> {
            loadState(QuickShulkerConfig.defaults());
            init();
        }).bounds(left, footerY, BUTTON_WIDTH, 20).build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> saveAndClose())
            .bounds(right, footerY, BUTTON_WIDTH, 20)
            .build());
    }

    private void saveAndClose() {
        NeoForgeQuickShulkerConfig.apply(new QuickShulkerConfig(
            new KeyBindingSpec("activation", activationKey),
            new KeyBindingSpec("open_settings", openSettingsKey),
            rightClickToOpen,
            keybindInHand,
            keybindInInventory,
            rightClickInInventory,
            supportsBundlingInsert,
            supportsBundlingPickup,
            supportsBundlingTransfer,
            supportsBundlingExtract,
            supportsMouseDragged,
            openSettingsKeyEnabled,
            quickShulkerBox,
            quickCraftingTables,
            quickStonecutter,
            quickEnderChest,
            quickAnvil
        ));
        NeoForgeKeyMappings.syncFromConfig();
        Minecraft.getInstance().setScreen(parent);
    }

    private void loadState(QuickShulkerConfig config) {
        activationKey = config.activationKey().translationKey();
        openSettingsKey = config.openSettingsKey().translationKey();
        rightClickToOpen = config.rightClickToOpen();
        keybindInHand = config.keybindInHand();
        keybindInInventory = config.keybindInInventory();
        rightClickInInventory = config.rightClickInInventory();
        supportsBundlingInsert = config.supportsBundlingInsert();
        supportsBundlingPickup = config.supportsBundlingPickup();
        supportsBundlingTransfer = config.supportsBundlingTransfer();
        supportsBundlingExtract = config.supportsBundlingExtract();
        supportsMouseDragged = config.supportsMouseDragged();
        openSettingsKeyEnabled = config.openSettingsKeyEnabled();
        quickShulkerBox = config.quickShulkerBox();
        quickCraftingTables = config.quickCraftingTables();
        quickStonecutter = config.quickStonecutter();
        quickEnderChest = config.quickEnderChest();
        quickAnvil = config.quickAnvil();
        capturingTarget = KeyTarget.NONE;
        loadedActivationKey = activationKey;
        loadedOpenSettingsKey = openSettingsKey;
    }

    @Override
    public void tick() {
        super.tick();
        syncDisplayedKeysFromMappings();
    }

    private void syncDisplayedKeysFromMappings() {
        if (capturingTarget != KeyTarget.NONE) {
            return;
        }

        String liveActivationKey = NeoForgeKeyMappings.OPEN_HELD_SHULKER.getKey().getName();
        if (Objects.equals(activationKey, loadedActivationKey) && !Objects.equals(activationKey, liveActivationKey)) {
            activationKey = liveActivationKey;
            loadedActivationKey = liveActivationKey;
            refreshKeyButtons();
        }

        String liveOpenSettingsKey = NeoForgeKeyMappings.OPEN_SETTINGS_SCREEN.getKey().getName();
        if (Objects.equals(openSettingsKey, loadedOpenSettingsKey) && !Objects.equals(openSettingsKey, liveOpenSettingsKey)) {
            openSettingsKey = liveOpenSettingsKey;
            loadedOpenSettingsKey = liveOpenSettingsKey;
            refreshKeyButtons();
        }
    }

    private void refreshKeyButtons() {
        if (activationKeyButton != null) {
            activationKeyButton.setMessage(keyMessage(
                "config.quickshulker_neoforged.activation_key",
                activationKey,
                capturingTarget == KeyTarget.ACTIVATION
            ));
        }
        if (openSettingsKeyButton != null) {
            openSettingsKeyButton.setMessage(keyMessage(
                "config.quickshulker_neoforged.open_settings_key",
                openSettingsKey,
                capturingTarget == KeyTarget.OPEN_SETTINGS
            ));
        }
    }

    private CycleButton<Boolean> addToggle(int x, int y, String labelKey, boolean initialValue, Consumer<Boolean> setter) {
        return addRenderableWidget(CycleButton.onOffBuilder(initialValue)
            .create(x, y, BUTTON_WIDTH, 20, Component.translatable(labelKey), (button, value) -> setter.accept(value)));
    }

    private Component keyMessage(String labelKey, String translationKey, boolean listening) {
        Component valueComponent = listening
            ? Component.translatable("screen.quickshulker_neoforged.config.listening")
            : translateKey(translationKey);
        return Component.translatable(labelKey).append(": ").append(valueComponent);
    }

    private Component translateKey(String translationKey) {
        return I18n.exists(translationKey) ? Component.translatable(translationKey) : Component.literal(Objects.toString(translationKey));
    }

    private enum KeyTarget {
        NONE,
        ACTIVATION,
        OPEN_SETTINGS
    }

    private enum ConfigPage {
        ACTIVATION("screen.quickshulker_neoforged.config.tab.activation", "screen.quickshulker_neoforged.config.section.activation"),
        QUICK_OPEN("screen.quickshulker_neoforged.config.tab.quick_open", "screen.quickshulker_neoforged.config.section.quick_open"),
        BUNDLING("screen.quickshulker_neoforged.config.tab.bundling", "screen.quickshulker_neoforged.config.section.bundling");

        private final String tabKey;
        private final String titleKey;

        ConfigPage(String tabKey, String titleKey) {
            this.tabKey = tabKey;
            this.titleKey = titleKey;
        }
    }
}
