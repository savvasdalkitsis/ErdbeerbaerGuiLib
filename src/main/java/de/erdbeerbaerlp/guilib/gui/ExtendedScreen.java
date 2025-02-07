package de.erdbeerbaerlp.guilib.gui;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import de.erdbeerbaerlp.guilib.components.GuiComponent;
import de.erdbeerbaerlp.guilib.components.TextField;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.minecraft.client.gui.screen.ConfirmOpenLinkScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
@OnlyIn(Dist.CLIENT)
public abstract class ExtendedScreen extends Screen {
    private final List<GuiComponent> components = new ArrayList<>();
    private final int nextComponentID = 0;
    private int currentPage = 0;
    private Screen parentGui;

    /**
     * The constructor, you need to call this!
     *
     * @param parentGui The gui this was opened from. Can be null
     */
    public ExtendedScreen(Screen parentGui) {
        super(new StringTextComponent("An GUI"));
        this.parentGui = parentGui;
        buildGui();
    }

    /**
     * Gets current open page
     *
     * @return current page
     */
    public int getCurrentPage() {
        return currentPage;
    }

    /**
     * Add your components here!
     */
    public abstract void buildGui();

    /**
     * Gets called often to e.g. update components postions
     */
    public abstract void updateGui();

    @Override
    public boolean isPauseScreen() {
        return doesGuiPauseGame();
    }

    /**
     * Should this GUI pause Singleplayer game when open?
     */
    public abstract boolean doesGuiPauseGame();

    /**
     * Should pressing ESC close the GUI?
     */
    public abstract boolean doesEscCloseGui();

    /**
     * Open the next page of the GUI (if available)
     */
    public void nextPage() {
        if (currentPage < Integer.MAX_VALUE) currentPage++;
    }

    /**
     * Open the previous page of the GUI (if available)
     */
    public void prevPage() {
        if (currentPage > 0) currentPage--;
    }

    /**
     * Sets the gui to a specific Page (if it is available!)
     *
     * @param page Page to set the GUI to
     */
    public void setPage(int page) {
        if (page < Integer.MAX_VALUE)
            this.currentPage = page;
    }

    /**
     * Use this to add your components
     *
     * @param component The component to add
     */
    public final void addComponent(GuiComponent component) {
        this.components.add(component);
    }

    /**
     * Use this to add your components
     *
     * @param component The component to add
     * @param page      The page to register the component to
     */
    public final void addComponent(GuiComponent component, int page) {
        this.components.add(component);
        component.assignToPage(page);
    }

    /**
     * Use this to add multiple components at once
     *
     * @param components The components to add
     */
    public final void addAllComponents(GuiComponent... components) {
        for (GuiComponent c : components) {
            this.addComponent(c);
        }
    }

    /**
     * Assigns a component to the page by calling comp.assignToPage(page)
     *
     * @param comp The component to assign
     * @param page The page for the component
     */
    public void assignComponentToPage(GuiComponent comp, int page) {
        comp.assignToPage(page);
    }

    @Override
    @Deprecated
    protected final <T extends Widget> T addButton(T buttonIn) {
        throw new UnsupportedOperationException();
    }

    @Deprecated
    protected final void actionPerformed(Widget button) {
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scroll) {
        for (GuiComponent comp : components) {
            if (comp.getAssignedPage() != -1) if (comp.getAssignedPage() != currentPage) continue;
            if (comp.isVisible() && mouseX >= comp.getX() && mouseY >= comp.getY() && mouseX < comp.getX() + comp.getWidth() && mouseY < comp.getY() + comp.getComponentHeight())
                comp.mouseScrolled(mouseX, mouseY, scroll);
        }
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int mouseButton, double deltaX, double deltaY) {
        for (GuiComponent comp : components) {
            if (comp.getAssignedPage() != -1) if (comp.getAssignedPage() != currentPage) continue;
            if (comp.isVisible())
                comp.mouseDragged(mouseX, mouseY, mouseButton, deltaX, deltaY);
        }
        return false;
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        for (GuiComponent comp : components) {
            if (comp.getAssignedPage() != -1) if (comp.getAssignedPage() != currentPage) continue;
            comp.keyReleased(keyCode, scanCode, modifiers);
        }
        return false;
    }

    private boolean unloadOnClose = true;

    public Screen getParentGui() {
        return parentGui;
    }

    public void setParentGui(Screen parentGui) {
        this.parentGui = parentGui;
    }

    @Override
    public final void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        super.render(matrixStack, mouseX, mouseY, partialTicks);
        updateGui();
        renderBackground(matrixStack);
        for (final GuiComponent comp : components) {
            if (comp.getAssignedPage() != -1) if (comp.getAssignedPage() != currentPage) continue;
            if (comp.isVisible()) comp.render(matrixStack, mouseX, mouseY, partialTicks);
        }
        //Second for to not have components overlap the tooltips
        for (final GuiComponent comp : Lists.reverse(components)) { //Reversing to call front component
            if (!comp.isVisible() || (comp.getAssignedPage() != -1 && comp.getAssignedPage() != currentPage)) continue;
            if (comp.canHaveTooltip() && isHovered(comp, mouseX, mouseY)) {

                final TextComponent txt = new StringTextComponent("");
                if (comp.getTooltips() != null) {
                    for (int i = 0; i < comp.getTooltips().length; i++) {
                        txt.append(new StringTextComponent(comp.getTooltips()[i] + (i == comp.getTooltips().length - 1 ? "" : "\n")));
                    }
                }
                if (!txt.getSiblings().isEmpty()) {
                    renderTooltip(matrixStack, minecraft.font.split(txt, Math.max(this.width / 2, 220)), mouseX, mouseY);
                    break;
                }
            }
        }


    }

    protected boolean forceDirtBackground() {
        return false;
    }

    /**
     * @return Resource Location of GUIs Background, defaults to {@link net.minecraft.client.gui.AbstractGui#BACKGROUND_LOCATION}
     */
    protected ResourceLocation getBackground() {
        return BACKGROUND_LOCATION;
    }

    private boolean isHovered(GuiComponent comp, int mouseX, int mouseY) {
        if (comp.getAssignedPage() != -1) if (comp.getAssignedPage() != currentPage) return false;
        final int x = comp.getX();
        final int y = comp.getY();
        final int w = comp.getWidth();
        final int h = comp.getComponentHeight();
        return (mouseX >= x && mouseY >= y && mouseX < x + w && mouseY < y + h);
    }

    public final GuiComponent getComponent(int index) {
        return components.get(index);
    }

    /**
     * Opens an GUI
     *
     * @param gui GuiScreen or null
     */
    public final void openGui(Screen gui) {
        minecraft.setScreen(gui);
    }

    /**
     * Closes this GUI, returning to parent or null
     */
    public void close() {
        openGui(parentGui);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        for (GuiComponent comp : components) {
            if (comp.getAssignedPage() != -1) if (comp.getAssignedPage() != currentPage) continue;
            if (comp.isVisible() && ((mouseX >= comp.getX() && mouseY >= comp.getY() && mouseX < comp.getX() + comp.getWidth() && mouseY < comp.getY() + comp.getComponentHeight()) || comp instanceof TextField))
                comp.mouseClicked(mouseX, mouseY, mouseButton);
        }
        return true;
    }

    public boolean charTyped(char character, int keyCode) {
        for (GuiComponent comp : components) {
            if (comp.getAssignedPage() != -1) if (comp.getAssignedPage() != currentPage) continue;
            comp.charTyped(character, keyCode);
        }
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int mouseButton) {
        for (GuiComponent comp : components) {
            if (comp.getAssignedPage() != -1) if (comp.getAssignedPage() != currentPage) continue;
            comp.mouseRelease(mouseX, mouseY, mouseButton);
        }
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int p_keyPressed_2_, int p_keyPressed_3_) {
        if (keyCode == 256 && doesEscCloseGui()) {
            //noinspection RedundantCast
            this.minecraft.setScreen((Screen) null);

            if (this.minecraft.screen == null) {
                this.minecraft.setWindowActive(true);
            }
        }
        for (GuiComponent comp : components) {
            if (comp.getAssignedPage() != -1) if (comp.getAssignedPage() != currentPage) continue;
            comp.keyPressed(keyCode, p_keyPressed_2_, p_keyPressed_3_);
        }
        return true;
    }

    /**
     * Override to draw a custom background
     */
    public void renderBackground(MatrixStack matrixStack) {
        if (this.minecraft.level != null && !forceDirtBackground()) {
            this.fillGradient(matrixStack, 0, 0, this.width, this.height, -1072689136, -804253680);
        } else {
            RenderSystem.disableLighting();
            RenderSystem.disableFog();
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder bufferbuilder = tessellator.getBuilder();
            this.minecraft.getTextureManager().bind(getBackground());
            RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
            float f = 32.0F;
            bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
            bufferbuilder.vertex(0.0D, this.height, 0.0D).uv(0.0F, (float) this.height / 32.0F + (float) 0).color(64, 64, 64, 255).endVertex();
            bufferbuilder.vertex(this.width, this.height, 0.0D).uv((float) this.width / 32.0F, (float) this.height / 32.0F + (float) 0).color(64, 64, 64, 255).endVertex();
            bufferbuilder.vertex(this.width, 0.0D, 0.0D).uv((float) this.width / 32.0F, (float) 0).color(64, 64, 64, 255).endVertex();
            bufferbuilder.vertex(0.0D, 0.0D, 0.0D).uv(0.0F, (float) 0).color(64, 64, 64, 255).endVertex();
            tessellator.end();
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.client.event.GuiScreenEvent.BackgroundDrawnEvent(this, matrixStack));
        }
    }

    private BooleanConsumer confirmCallback;

    @Override
    public boolean handleComponentClicked(Style style) {
        return super.handleComponentClicked(style);
    }

    @Override
    public void onClose() {
        if (unloadOnClose)
            for (GuiComponent comp : components) {
                comp.unload();
            }
    }

    private void confirmed(boolean confirmed) {
        confirmCallback.accept(confirmed);
        openGui(ExtendedScreen.this);
        confirmCallback = null;
        unloadOnClose = true;
    }

    /**
     * Prompts the user to open an url<br>
     *
     * @param callback gets called with true if the user opened it, false if user copied or closed it
     */
    public final void openURL(String URL, final BooleanConsumer callback) {
        confirmCallback = callback;
        unloadOnClose = false;
        final ConfirmOpenLinkScreen s = new ConfirmOpenLinkScreen(this::confirmed, URL, true);
        openGui(s);
    }
}
