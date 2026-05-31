package moscow.mytheria.ui.menu.dropdown.components.settings.impl;

import moscow.mytheria.framework.base.CustomComponent;
import moscow.mytheria.framework.base.UIContext;
import moscow.mytheria.framework.msdf.Font;
import moscow.mytheria.framework.msdf.Fonts;
import moscow.mytheria.framework.objects.BorderRadius;
import moscow.mytheria.framework.objects.MouseButton;
import moscow.mytheria.systems.localization.Localizator;
import moscow.mytheria.systems.setting.settings.StringSetting;
import moscow.mytheria.ui.components.textfield.TextField;
import moscow.mytheria.ui.menu.dropdown.components.settings.MenuSettingComponent;
import moscow.mytheria.utility.colors.Colors;
import moscow.mytheria.utility.game.cursor.CursorType;
import moscow.mytheria.utility.game.cursor.CursorUtility;

public class StringSettingComponent extends MenuSettingComponent<StringSetting> {
   private static final float COMPONENT_HEIGHT = 44.0F;
   private TextField textField;

   public StringSettingComponent(StringSetting setting, CustomComponent parent) {
      super(setting, parent);
   }

   @Override
   public void onInit() {
      this.width = 13.0F;
      this.height = COMPONENT_HEIGHT;
      this.textField = new TextField(Fonts.REGULAR.getFont(8.0F));
      this.textField.paste(this.setting.getText());
      this.textField.setPreview(Localizator.translate("type_text"));
      super.onInit();
   }

   @Override
   protected void renderComponent(UIContext context) {
      if (this.getOpacity() <= 0.01F) {
         return;
      }

      float alpha = this.getOpacity();
      float padding = 10.0F;
      float fieldX = this.x + padding;
      float fieldY = this.y + 21.0F;
      float fieldWidth = this.width - padding * 2.0F;
      float fieldHeight = 17.0F;

      this.hoverAnimation.update(this.isHovered(context.getMouseX(), context.getMouseY()));
      if (this.isHovered(context.getMouseX(), context.getMouseY())) {
         CursorUtility.set(CursorType.HAND);
      }

      Font nameFont = Fonts.REGULAR.getFont(8.0F);
      context.drawFadeoutText(
         nameFont,
         Localizator.translate(this.setting.getName()),
         this.x + padding,
         this.y + 7.0F,
         Colors.getTextColor().withAlpha(255.0F * alpha * (0.72F + 0.28F * this.hoverAnimation.getValue())),
         0.7F,
         0.99F,
         fieldWidth
      );
      context.drawRoundedRect(fieldX, fieldY, fieldWidth, fieldHeight, BorderRadius.all(4.0F), Colors.getBackgroundColor().withAlpha(95.0F * alpha));

      this.textField.set(fieldX + 5.0F, fieldY, fieldWidth - 10.0F, fieldHeight);
      this.textField.setAlpha(alpha);
      this.textField.setTextColor(Colors.getTextColor().withAlpha(255.0F * alpha));
      this.textField.render(context);
      this.setting.text(this.textField.getBuiltText());
   }

   @Override
   public void drawRegular8(UIContext context) {
   }

   @Override
   public void drawSplit(UIContext context) {
      if (this.getOpacity() <= 0.01F) {
         return;
      }

      context.drawRect(this.x, this.y + this.height, this.width, 0.5F, Colors.getTextColor().withAlpha(5.1F * this.getOpacity()));
   }

   @Override
   public void onKeyPressed(int keyCode, int scanCode, int modifiers) {
      this.textField.onKeyPressed(keyCode, scanCode, modifiers);
   }

   @Override
   public boolean charTyped(char chr, int modifiers) {
      return this.textField.charTyped(chr, modifiers);
   }

   @Override
   public void onMouseClicked(double mouseX, double mouseY, MouseButton button) {
      this.textField.onMouseClicked(mouseX, mouseY, button);
      super.onMouseReleased(mouseX, mouseY, button);
   }

   @Override
   public void onMouseReleased(double mouseX, double mouseY, MouseButton button) {
      this.textField.onMouseReleased(mouseX, mouseY, button);
   }

   @Override
   public float getHeight() {
      this.height = COMPONENT_HEIGHT;
      return COMPONENT_HEIGHT;
   }
}
