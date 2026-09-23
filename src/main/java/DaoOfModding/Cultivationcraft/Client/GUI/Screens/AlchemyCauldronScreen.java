package DaoOfModding.Cultivationcraft.Client.GUI.Screens;

import DaoOfModding.Cultivationcraft.Common.Containers.AlchemyCauldronMenu;
import DaoOfModding.Cultivationcraft.Common.Containers.AlchemyQi;
import DaoOfModding.Cultivationcraft.Cultivationcraft;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import org.lwjgl.glfw.GLFW;

public class AlchemyCauldronScreen extends AbstractContainerScreen<AlchemyCauldronMenu> {
    private static final ResourceLocation FRAME = new ResourceLocation("textures/gui/advancements/window.png");
    private static final ResourceLocation FURNACE = new ResourceLocation("textures/gui/container/furnace.png");
    private static final int PANEL_X = 9, PANEL_Y = 18;
    private static final int CONTENT_WIDTH = 320, CONTENT_HEIGHT = 164;
    private static final int ELEMENT_X = 150, ELEMENT_Y = 88, ELEMENT_SPACING = 40;
    private double zoom = 1, panX, panY;
    private boolean panning, renderingPanel;
    private int stationLeft, stationTop, stationWidth, stationHeight;

    public AlchemyCauldronScreen(AlchemyCauldronMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void init() {
        imageWidth = 176;
        imageHeight = 100;
        super.init();
        stationWidth = Math.min(360, width - 12);
        stationHeight = Math.min(210, height - imageHeight - 18);
        stationLeft = (width - stationWidth) / 2;
        stationTop = (height - stationHeight - imageHeight - 6) / 2;
        topPos = stationTop + stationHeight + 6;
        clampPan();
    }

    private int panelWidth() { return stationWidth - 18; }
    private int panelHeight() { return stationHeight - 27; }
    private double virtualX(double x) { return leftPos + (x - stationLeft - PANEL_X - panX) / zoom; }
    private double virtualY(double y) { return topPos + (y - stationTop - PANEL_Y - panY) / zoom; }

    private boolean inPanel(double x, double y) {
        return x >= stationLeft + PANEL_X && x < stationLeft + PANEL_X + panelWidth()
                && y >= stationTop + PANEL_Y && y < stationTop + PANEL_Y + panelHeight();
    }

    private boolean inStation(double x, double y) {
        return x >= stationLeft && x < stationLeft + stationWidth
                && y >= stationTop && y < stationTop + stationHeight;
    }

    private boolean inInventory(double x, double y) {
        return x >= leftPos && x < leftPos + imageWidth && y >= topPos && y < topPos + imageHeight;
    }

    private void clampPan() {
        double extraX = panelWidth() - CONTENT_WIDTH * zoom;
        double extraY = panelHeight() - CONTENT_HEIGHT * zoom;
        panX = extraX >= 0 ? extraX / 2 : Mth.clamp(panX, extraX, 0);
        panY = extraY >= 0 ? extraY / 2 : Mth.clamp(panY, extraY, 0);
    }

    @Override
    public void render(PoseStack pose, int mouseX, int mouseY, float partialTick) {
        renderBackground(pose);
        fill(pose, stationLeft + PANEL_X, stationTop + PANEL_Y,
                stationLeft + PANEL_X + panelWidth(), stationTop + PANEL_Y + panelHeight(), 0xFF000000);
        double guiScale = minecraft.getWindow().getGuiScale();
        RenderSystem.enableScissor((int) ((stationLeft + PANEL_X) * guiScale),
                (int) (minecraft.getWindow().getHeight() - (stationTop + PANEL_Y + panelHeight()) * guiScale),
                (int) (panelWidth() * guiScale), (int) (panelHeight() * guiScale));
        PoseStack modelView = RenderSystem.getModelViewStack();
        modelView.pushPose();
        modelView.translate(stationLeft + PANEL_X + panX, stationTop + PANEL_Y + panY, 0);
        modelView.scale((float) zoom, (float) zoom, 1);
        modelView.translate(-leftPos, -topPos, 0);
        RenderSystem.applyModelViewMatrix();
        try {
            renderingPanel = true;
            menu.setVisibleSection(1);
            // Vanilla draws the station slots and drag previews. Draw the carried stack only
            // in the normal-scale inventory pass, so it can cross between both sections.
            super.render(pose, -10000, -10000, partialTick);
            for (int i = 0; i < AlchemyCauldronMenu.STATION_SLOTS; i++) {
                Slot slot = menu.slots.get(i);
                if (isHovering(slot.x, slot.y, 16, 16, mouseX, mouseY)) {
                    renderSlotHighlight(pose, leftPos + slot.x, topPos + slot.y, getBlitOffset());
                }
            }
        } finally {
            menu.setVisibleSection(0);
            renderingPanel = false;
            modelView.popPose();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.disableScissor();
        }
        RenderSystem.disableDepthTest();
        drawFrame(pose);
        font.draw(pose, title, stationLeft + 8, stationTop + 6, 0x404040);
        RenderSystem.enableDepthTest();
        try {
            menu.setVisibleSection(2);
            super.render(pose, mouseX, mouseY, partialTick);
        } finally {
            menu.setVisibleSection(0);
        }
        hoveredSlot = null;
        for (Slot slot : menu.slots) {
            if (isHovering(slot.x, slot.y, 16, 16, mouseX, mouseY)) {
                hoveredSlot = slot;
                break;
            }
        }
        renderTooltip(pose, mouseX, mouseY);
        if (hoveredSlot != null && hoveredSlot.index == menu.getPreviewSlot() && menu.getCarried().isEmpty())
            renderTooltip(pose, Component.literal("???"), mouseX, mouseY);
        if (hoveredSlot == null && menu.getCarried().isEmpty()) {
            if (inPanel(mouseX, mouseY)) drawPanelTooltip(pose, mouseX, mouseY);
            else if (inStation(mouseX, mouseY))
                renderTooltip(pose, Component.translatable("cultivationcraft.gui.alchemy.controls"), mouseX, mouseY);
        }
    }

    private void drawFrame(PoseStack pose) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.setShaderTexture(0, FRAME);
        // Nine-slice the original 252x140 advancement frame, keeping its borders pixel-sized.
        frameSlice(pose, 0, 0, 9, 18, 0, 0, 9, 18);
        frameSlice(pose, 9, 0, stationWidth - 18, 18, 9, 0, 234, 18);
        frameSlice(pose, stationWidth - 9, 0, 9, 18, 243, 0, 9, 18);
        frameSlice(pose, 0, 18, 9, panelHeight(), 0, 18, 9, 113);
        frameSlice(pose, stationWidth - 9, 18, 9, panelHeight(), 243, 18, 9, 113);
        frameSlice(pose, 0, stationHeight - 9, 9, 9, 0, 131, 9, 9);
        frameSlice(pose, 9, stationHeight - 9, stationWidth - 18, 9, 9, 131, 234, 9);
        frameSlice(pose, stationWidth - 9, stationHeight - 9, 9, 9, 243, 131, 9, 9);
    }

    private void frameSlice(PoseStack pose, int x, int y, int w, int h, int u, int v, int sw, int sh) {
        blit(pose, stationLeft + x, stationTop + y, w, h, u, v, sw, sh, 256, 256);
    }

    @Override
    protected void renderBg(PoseStack pose, float partialTick, int mouseX, int mouseY) {
        if (!renderingPanel) {
            // A separate, fixed vanilla-style inventory below the advancement frame.
            fill(pose, leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF373737);
            fill(pose, leftPos + 1, topPos + 1, leftPos + imageWidth - 1, topPos + imageHeight - 1, 0xFFFFFFFF);
            fill(pose, leftPos + 3, topPos + 3, leftPos + imageWidth - 1, topPos + imageHeight - 1, 0xFF555555);
            fill(pose, leftPos + 3, topPos + 3, leftPos + imageWidth - 3, topPos + imageHeight - 3, 0xFFC6C6C6);
        }
        for (Slot slot : menu.slots) {
            if (slot.isActive()) drawSlot(pose, leftPos + slot.x, topPos + slot.y);
        }
        if (!renderingPanel) return;
        int preview = menu.getPreviewSlot();
        if (preview >= 0) {
            Slot slot = menu.slots.get(preview);
            itemRenderer.renderAndDecorateItem(new net.minecraft.world.item.ItemStack(
                    DaoOfModding.Cultivationcraft.Common.Items.ItemRegister.ALCHEMY_PILL.get()), leftPos + slot.x, topPos + slot.y);
        }
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.setShaderTexture(0, FURNACE);
        // Charge is separate from the plants' elemental ingredient totals.
        blit(pose, leftPos + 51, topPos + 94, menu.isReceivingQi() ? 176 : 56,
                menu.isReceivingQi() ? 0 : 36, 14, 14);
        for (int i = 0; i < AlchemyQi.ELEMENTS.size(); i++) {
            ResourceLocation element = AlchemyQi.ELEMENTS.get(i);
            RenderSystem.setShaderTexture(0, new ResourceLocation(Cultivationcraft.MODID,
                    "textures/gui/plants/element_icons/" + element.getPath() + ".png"));
            blit(pose, leftPos + ELEMENT_X + (i % 4) * ELEMENT_SPACING,
                    topPos + ELEMENT_Y + (i / 4) * 38, 0, 0, 16, 16, 16, 16);
        }
    }

    private void drawSlot(PoseStack pose, int x, int y) {
        fill(pose, x - 1, y - 1, x + 17, y + 17, 0xFFDDDDDD);
        fill(pose, x - 1, y - 1, x + 16, y + 16, 0xFF373737);
        fill(pose, x, y, x + 16, y + 16, 0xFF737373);
    }

    @Override
    protected void renderLabels(PoseStack pose, int mouseX, int mouseY) {
        if (!renderingPanel) {
            font.draw(pose, playerInventoryTitle, AlchemyCauldronMenu.INVENTORY_X, 6, 0x404040);
            return;
        }
        font.draw(pose, Component.translatable("cultivationcraft.gui.alchemy.charge"), 30, 116, 0xAAAAAA);
        font.draw(pose, Integer.toString(menu.getStoredQi()), 30, 128, 0xFFFFFF);
        font.draw(pose, Component.translatable("cultivationcraft.gui.alchemy.ingredients"), 30, 14, 0xDDDDDD);
        font.draw(pose, Component.translatable("cultivationcraft.gui.alchemy.results"), 164, 14, 0xDDDDDD);
        font.draw(pose, Component.translatable("cultivationcraft.gui.alchemy.elemental_qi"), 150, 70, 0xDDDDDD);
        for (int i = 0; i < AlchemyQi.ELEMENTS.size(); i++) {
            String value = Integer.toString(menu.getElementalQi(i));
            int color = menu.getElementalQi(i) == 0 ? 0x777777 : 0xFFFFFF;
            float scale = Math.min(1f, 34f / Math.max(1, font.width(value)));
            pose.pushPose();
            pose.translate(ELEMENT_X + (i % 4) * ELEMENT_SPACING + 8, ELEMENT_Y + (i / 4) * 38 + 19, 0);
            pose.scale(scale, scale, 1);
            font.draw(pose, value, -font.width(value) / 2f, 0, color);
            pose.popPose();
        }
    }

    private void drawPanelTooltip(PoseStack pose, int mouseX, int mouseY) {
        double x = virtualX(mouseX) - leftPos, y = virtualY(mouseY) - topPos;
        for (int i = 0; i < AlchemyQi.ELEMENTS.size(); i++) {
            int ex = ELEMENT_X + (i % 4) * ELEMENT_SPACING, ey = ELEMENT_Y + (i / 4) * 38;
            if (x >= ex - 8 && x < ex + 24 && y >= ey && y < ey + 30) {
                renderTooltip(pose, Component.translatable("cultivationcraft.gui.alchemy.qi_total",
                        Component.translatable(AlchemyQi.ELEMENTS.get(i).getPath()), menu.getElementalQi(i)), mouseX, mouseY);
                return;
            }
        }
    }

    @Override
    protected boolean isHovering(int x, int y, int w, int h, double mouseX, double mouseY) {
        // Ingredient and inventory slots have distinct logical coordinates. All input stays
        // in screen space; only the ingredient hit boxes follow the panel transform.
        int column = x - AlchemyCauldronMenu.INGREDIENT_X;
        int row = y - AlchemyCauldronMenu.INGREDIENT_Y;
        boolean ingredient = column >= 0 && column <= 36 && column % 18 == 0
                && row >= 0 && row <= 36 && row % 18 == 0;
        boolean output = y == 30 && x >= 164 && x <= 244 && (x - 164) % 40 == 0;
        if (ingredient || output) return inPanel(mouseX, mouseY)
                && super.isHovering(x, y, w, h, virtualX(mouseX), virtualY(mouseY));
        return super.isHovering(x, y, w, h, mouseX, mouseY);
    }

    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int guiLeft, int guiTop, int button) {
        return !inStation(mouseX, mouseY) && !inInventory(mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (inStation(mouseX, mouseY) && !inPanel(mouseX, mouseY)) return true;
        boolean overSlot = menu.slots.stream().anyMatch(slot -> isHovering(slot.x, slot.y, 16, 16, mouseX, mouseY));
        if (inPanel(mouseX, mouseY) && menu.getCarried().isEmpty() && !isQuickCrafting
                && !overSlot && (button == 2 || button == 0)) {
            panning = true;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        if (panning) {
            panX += dx;
            panY += dy;
            clampPan();
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (panning) { panning = false; return true; }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (!inPanel(mouseX, mouseY)) return super.mouseScrolled(mouseX, mouseY, delta);
        if (isQuickCrafting) return true;
        if (hasShiftDown()) {
            panY += delta * 20;
        } else {
            double contentX = virtualX(mouseX) - leftPos, contentY = virtualY(mouseY) - topPos;
            zoom = Mth.clamp(zoom * Math.pow(1.1, delta), 0.75, 1.75);
            panX = mouseX - stationLeft - PANEL_X - contentX * zoom;
            panY = mouseY - stationTop - PANEL_Y - contentY * zoom;
        }
        clampPan();
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (key == GLFW.GLFW_KEY_HOME && menu.getCarried().isEmpty() && !isQuickCrafting) {
            zoom = 1;
            panX = panY = 0;
            clampPan();
            return true;
        }
        return super.keyPressed(key, scanCode, modifiers);
    }
}
