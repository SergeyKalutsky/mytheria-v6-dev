/*
 * Decompiled with CFR 0.152.
 */
package moscow.mytheria.ui.menu.dropdown.components.settings.impl;

import java.util.ArrayList;
import java.util.Comparator;
import moscow.mytheria.Mytheria;
import moscow.mytheria.framework.base.CustomComponent;
import moscow.mytheria.framework.base.UIContext;
import moscow.mytheria.framework.msdf.Font;
import moscow.mytheria.framework.msdf.Fonts;
import moscow.mytheria.framework.objects.BorderRadius;
import moscow.mytheria.framework.objects.MouseButton;
import moscow.mytheria.systems.localization.Localizator;
import moscow.mytheria.systems.setting.settings.SelectSetting;
import moscow.mytheria.ui.components.animated.AnimatedNumber;
import moscow.mytheria.ui.menu.dropdown.components.settings.MenuSettingComponent;
import moscow.mytheria.utility.animation.base.Easing;
import moscow.mytheria.utility.colors.ColorRGBA;
import moscow.mytheria.utility.colors.Colors;
import moscow.mytheria.utility.game.cursor.CursorType;
import moscow.mytheria.utility.game.cursor.CursorUtility;
import moscow.mytheria.utility.gui.GuiUtility;
import moscow.mytheria.utility.render.DrawUtility;
import moscow.mytheria.utility.render.penis.PenisPlayer;
import moscow.mytheria.utility.time.Timer;

public class SelectSettingComponent
extends MenuSettingComponent<SelectSetting> {
    private AnimatedNumber numberAnim;
    private SelectSetting.Value dragging;
    private final Timer sortTimer = new Timer();
    private boolean initialized;

    public SelectSettingComponent(SelectSetting setting, CustomComponent parent) {
        super(setting, parent);
        ArrayList enabled = new ArrayList();
        setting.getValues().forEach(sel -> {
            if (sel.isSelected()) {
                enabled.add(sel);
            }
        });
        setting.getSelectedValues().clear();
        setting.getSelectedValues().addAll(enabled);
    }

    @Override
    protected void renderComponent(UIContext context) {
        if (!this.initialized) {
            for (SelectSetting.Value value : ((SelectSetting)this.setting).getValues()) {
                value.setEnablePenis(new PenisPlayer(Mytheria.id("penises/check_enable.penis")));
                value.setDisablePenis(new PenisPlayer(Mytheria.id("penises/check_disable.penis")));
                value.setLastState(value.isSelected());
                value.setCurrentPenis(value.isLastState() ? value.getEnablePenis() : value.getDisablePenis());
                if (value.isLastState()) {
                    value.getEnablePenis().playOnce();
                    continue;
                }
                value.getDisablePenis().setFrame(0);
                value.getDisablePenis().stop();
            }
            this.initialized = true;
        }
        float x2 = this.x + 9.0f;
        float y = this.y + 1.0f;
        float width = this.width - 18.0f;
        Font nameFont = Fonts.REGULAR.getFont(8.0f);
        float leftPadding = 10.0f;
        float nameHeight = Fonts.REGULAR.getFont(7.0f).height();
        float headerHeight = 19.0f;
        this.hoverAnimation.update(this.isHovered(context.getMouseX(), context.getMouseY()));
        String rightText = String.format(" %s", Localizator.translate("setting_of", new Object[0]) + " " + ((SelectSetting)this.setting).getValues().size());
        if (this.numberAnim == null) {
            this.numberAnim = new AnimatedNumber(Fonts.MEDIUM.getFont(7.0f), 5.0f, 500L, Easing.BAKEK);
        }
        context.drawFadeoutText(nameFont, Localizator.translate(((SelectSetting)this.setting).getName(), new Object[0]), this.x + leftPadding, y - 1.0f + GuiUtility.getMiddleOfBox(nameFont.height(), headerHeight), Colors.getTextColor().withAlpha(255.0f * (0.75f + 0.25f * this.hoverAnimation.getValue())), 0.8f, 1.0f, this.getParent().getWidth() - leftPadding - Fonts.REGULAR.getFont(7.0f).width(rightText) - this.numberAnim.getWidth() - 10.0f);
        this.numberAnim.settings(false, Colors.getTextColor().withAlpha(255.0f * (0.75f + 0.25f * this.hoverAnimation.getValue())));
        this.numberAnim.update(((SelectSetting)this.setting).getSelectedValues().size());
        this.numberAnim.pos(x2 + width - Fonts.REGULAR.getFont(7.0f).width(rightText) - this.numberAnim.getWidth(), y - 1.0f + GuiUtility.getMiddleOfBox(nameHeight, headerHeight));
        this.numberAnim.render(context);
        context.drawRightText(Fonts.REGULAR.getFont(7.0f), rightText, x2 + width, y - 1.0f + GuiUtility.getMiddleOfBox(nameHeight, headerHeight), Colors.getTextColor().withAlpha(255.0f * (0.75f + 0.25f * this.hoverAnimation.getValue())));
        float boxY = y + 17.0f;
        float boxHeight = (float)(8 + ((SelectSetting)this.setting).getValues().size() * 12);
        context.drawLiquidGlass(x2 - 1.0f, boxY, width + 2.0f, boxHeight, 6.0f, BorderRadius.all(6.0f), ColorRGBA.WHITE.withAlpha(32.0f), true);
        context.drawRoundedRect(x2 - 1.0f, boxY, width + 2.0f, boxHeight, BorderRadius.all(6.0f), Colors.getBackgroundColor().withAlpha(58.0f));
        context.drawRoundedBorder(x2 - 1.0f, boxY, width + 2.0f, boxHeight, 0.7f, BorderRadius.all(6.0f), ColorRGBA.WHITE.withAlpha(62.0f));
        float offset = 0.0f;
        for (SelectSetting.Value valuex : ((SelectSetting)this.setting).getValues()) {
            if (valuex.isHidden()) continue;
            boolean currentState = valuex.isSelected();
            if (currentState != valuex.isLastState()) {
                if (currentState) {
                    valuex.setCurrentPenis(valuex.getEnablePenis());
                } else {
                    valuex.setCurrentPenis(valuex.getDisablePenis());
                }
                valuex.getCurrentPenis().playOnce();
                valuex.setLastState(currentState);
            }
            valuex.getCurrentPenis().update();
            float elmtY = this.dragging == valuex ? Math.clamp((float)(context.getMouseY() - 2), y + 18.0f, y + 20.0f + (float)(((SelectSetting)this.setting).getValues().size() * 12)) : y + 24.0f + offset;
            boolean hover = GuiUtility.isHovered((double)(x2 - 1.0f), (double)(elmtY - 4.0f), (double)(width + 2.0f), 12.0, context.getMouseX(), context.getMouseY());
            valuex.getYAnim().setEasing(Easing.BAKEK_SMALLER);
            valuex.getYAnim().update(elmtY - y);
            valuex.setYFactor(elmtY);
            if (hover && this.dragging != valuex && !valuex.isAlwaysEnabled()) {
                CursorUtility.set(CursorType.HAND);
            }
            valuex.getHoverAnimation().update(hover);
            valuex.getActiveAnimation().update(valuex.isSelected());
            if (((SelectSetting)this.setting).isDraggable()) {
                context.drawTexture(Mytheria.id("icons/hud/drag.png"), x2 + 7.0f, y + valuex.getYAnim().getValue(), 6.0f, 6.0f, Colors.getTextColor());
            }
            if (GuiUtility.isHovered(x2, elmtY - 2.0f, 17.0, 10.0, context) || valuex == this.dragging) {
                CursorUtility.set(CursorType.ARROW_VERTICAL);
            }
            context.drawFadeoutText(Fonts.REGULAR.getFont(7.0f), Localizator.translate(valuex.getName(), new Object[0]), x2 + (float)(((SelectSetting)this.setting).isDraggable() ? 18 : 7), y + valuex.getYAnim().getValue() + 0.5f, Colors.getTextColor().withAlpha(255.0f * (0.75f + 0.25f * valuex.getHoverAnimation().getValue() + 0.25f * valuex.getActiveAnimation().getValue())), 0.8f, 1.0f, width - 12.0f - valuex.getActiveAnimation().getValue() * 10.0f);
            if (valuex.getActiveAnimation().getValue() > 0.0f || valuex.getCurrentPenis().isPlaying()) {
                DrawUtility.drawAnimationSprite(context.method_51448(), valuex.getCurrentPenis().getCurrentSprite(), x2 + width - 11.0f - valuex.getActiveAnimation().getValue() * 2.0f, y + valuex.getYAnim().getValue(), 6.0f, 6.0f, Colors.getTextColor().mulAlpha(0.1f + 0.9f * valuex.getActiveAnimation().getValue()));
            }
            offset += 12.0f;
        }
        if (this.sortTimer.finished(100L)) {
            ((SelectSetting)this.setting).getValues().sort(Comparator.comparingDouble(SelectSetting.Value::getYFactor));
            this.sortTimer.reset();
        }
    }

    @Override
    public void drawSplit(UIContext context) {
        float separatorHeight = 0.5f;
        context.drawRect(this.x, this.y + this.height, this.width, separatorHeight, Colors.getTextColor().withAlpha(5.1f));
    }

    @Override
    public void onMouseClicked(double mouseX, double mouseY, MouseButton button) {
        if (button == MouseButton.LEFT) {
            float x2 = this.x + 9.0f;
            float y = this.y + 1.0f;
            float offset = 0.0f;
            for (SelectSetting.Value value : ((SelectSetting)this.setting).getValues()) {
                if (value.isHidden()) continue;
                boolean hover = GuiUtility.isHovered((double)(x2 - 1.0f), (double)(y + 20.0f + offset), (double)(this.width - 2.0f), 12.0, mouseX, mouseY);
                if (GuiUtility.isHovered((double)x2, (double)(y + 22.0f + offset), 17.0, 10.0, mouseX, mouseY) && ((SelectSetting)this.setting).isDraggable()) {
                    this.dragging = value;
                } else if (hover) {
                    value.toggle();
                }
                offset += 12.0f;
            }
            super.onMouseClicked(mouseX, mouseY, button);
        }
    }

    @Override
    public void onMouseReleased(double mouseX, double mouseY, MouseButton button) {
        this.dragging = null;
        super.onMouseReleased(mouseX, mouseY, button);
    }

    @Override
    public float getHeight() {
        this.height = 31 + ((SelectSetting)this.setting).getValues().size() * 12;
        return this.height;
    }
}
