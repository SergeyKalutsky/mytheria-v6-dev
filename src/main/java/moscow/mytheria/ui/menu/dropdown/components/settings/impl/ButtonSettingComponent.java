/*
 * Decompiled with CFR 0.152.
 */
package moscow.mytheria.ui.menu.dropdown.components.settings.impl;

import moscow.mytheria.framework.base.CustomComponent;
import moscow.mytheria.framework.base.UIContext;
import moscow.mytheria.framework.msdf.Font;
import moscow.mytheria.framework.msdf.Fonts;
import moscow.mytheria.framework.objects.BorderRadius;
import moscow.mytheria.framework.objects.MouseButton;
import moscow.mytheria.systems.localization.Localizator;
import moscow.mytheria.systems.setting.settings.ButtonSetting;
import moscow.mytheria.ui.menu.dropdown.components.settings.MenuSettingComponent;
import moscow.mytheria.utility.colors.ColorRGBA;
import moscow.mytheria.utility.colors.Colors;
import moscow.mytheria.utility.game.cursor.CursorType;
import moscow.mytheria.utility.game.cursor.CursorUtility;
import moscow.mytheria.utility.gui.GuiUtility;

public class ButtonSettingComponent
extends MenuSettingComponent<ButtonSetting> {
    public ButtonSettingComponent(ButtonSetting setting, CustomComponent parent) {
        super(setting, parent);
    }

    @Override
    public void onInit() {
        this.width = 13.0f;
        this.height = 8.0f;
        super.onInit();
    }

    @Override
    public void update(UIContext context) {
        super.update(context);
    }

    @Override
    protected void renderComponent(UIContext context) {
        this.hoverAnimation.update(this.isHovered(context.getMouseX(), context.getMouseY()));
        if (this.isHovered(context.getMouseX(), context.getMouseY())) {
            CursorUtility.set(CursorType.HAND);
        }
        Font nameFont = Fonts.REGULAR.getFont(8.0f);
        float alpha = this.getOpacity();
        float buttonX = this.x + 7.0f;
        float buttonY = this.y + 4.0f;
        float buttonWidth = this.width - 14.0f;
        float buttonHeight = this.height - 7.0f;
        context.drawLiquidGlass(buttonX, buttonY, buttonWidth, buttonHeight, 6.0f, BorderRadius.all(6.0f), ColorRGBA.WHITE.withAlpha((38.0f + 34.0f * this.hoverAnimation.getValue()) * alpha), true);
        context.drawRoundedRect(buttonX, buttonY, buttonWidth, buttonHeight, BorderRadius.all(6.0f), Colors.getBackgroundColor().withAlpha((58.0f + 28.0f * this.hoverAnimation.getValue()) * alpha));
        context.drawRoundedRect(buttonX + 4.0f, buttonY + 2.0f, buttonWidth - 8.0f, 4.0f, BorderRadius.all(4.0f), ColorRGBA.WHITE.withAlpha((18.0f + 24.0f * this.hoverAnimation.getValue()) * alpha));
        context.drawRoundedBorder(buttonX, buttonY, buttonWidth, buttonHeight, 0.8f, BorderRadius.all(6.0f), ColorRGBA.WHITE.withAlpha((70.0f + 36.0f * this.hoverAnimation.getValue()) * alpha));
        context.drawCenteredText(nameFont, Localizator.translate(((ButtonSetting)this.setting).getName(), new Object[0]), this.x + this.width / 2.0f, this.y + GuiUtility.getMiddleOfBox(nameFont.height(), this.height) - 0.5f, Colors.getTextColor().withAlpha(255.0f * (0.75f + 0.25f * this.hoverAnimation.getValue())));
    }

    @Override
    public void drawRegular8(UIContext context) {
    }

    @Override
    public void drawSplit(UIContext context) {
        float separatorHeight = 0.5f;
        context.drawRect(this.x, this.y + this.height, this.width, separatorHeight, Colors.getTextColor().withAlpha(5.1f));
    }

    @Override
    public void onMouseClicked(double mouseX, double mouseY, MouseButton button) {
        if (this.isHovered(mouseX, mouseY) && button == MouseButton.LEFT) {
            ((ButtonSetting)this.setting).getAction().run();
        }
        super.onMouseReleased(mouseX, mouseY, button);
    }

    @Override
    public float getHeight() {
        this.height = 24.0f;
        return 24.0f;
    }
}
