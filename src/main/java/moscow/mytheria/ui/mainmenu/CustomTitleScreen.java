package moscow.mytheria.ui.mainmenu;

import java.util.ArrayList;
import java.util.List;
import moscow.mytheria.Mytheria;
import moscow.mytheria.framework.base.CustomScreen;
import moscow.mytheria.framework.base.UIContext;
import moscow.mytheria.framework.msdf.Font;
import moscow.mytheria.framework.msdf.Fonts;
import moscow.mytheria.framework.objects.BorderRadius;
import moscow.mytheria.framework.objects.MouseButton;
import moscow.mytheria.systems.localization.Localizator;
import moscow.mytheria.systems.modules.modules.visuals.MenuModule;
import moscow.mytheria.utility.animation.base.Animation;
import moscow.mytheria.utility.animation.base.Easing;
import moscow.mytheria.utility.colors.ColorRGBA;
import moscow.mytheria.utility.game.TextUtility;
import moscow.mytheria.utility.game.cursor.CursorType;
import moscow.mytheria.utility.game.cursor.CursorUtility;
import net.minecraft.class_310;
import net.minecraft.class_429;
import net.minecraft.class_500;
import net.minecraft.class_526;

public class CustomTitleScreen extends CustomScreen {
   private static final List<CustomButton> buttons = new ArrayList<>();

   private final Animation activeAnimation = new Animation(1000L, 1.0F, Easing.FIGMA_EASE_IN_OUT);
   private final Animation backgroundPanelAnimation = new Animation(260L, 0.0F, Easing.FIGMA_EASE_IN_OUT);
   private boolean active = true;
   private boolean backgroundPanelOpen;

   @Override
   protected void method_25426() {
      if (buttons.isEmpty()) {
         String var1 = "image/mainmenu/icons/";
         buttons.add(new CustomButton(var1 + "single.png", 12.0F, this::openSingleplayer));
         buttons.add(new CustomButton(var1 + "multi.png", 12.0F, this::openMultiplayer));
         buttons.add(new CustomButton(var1 + "settings.png", 12.0F, this::openOptions));
         buttons.add(new CustomButton(var1 + "quit.png", 14.0F, () -> class_310.method_1551().method_1490()));
      }

      super.method_25426();
   }

   @Override
   public void render(UIContext var1) {
      if (!Fonts.isInitialized()) {
         return;
      }

      Font var2 = Fonts.ROUND_BOLD.getFont(65.0F);
      Font var3 = Fonts.MEDIUM.getFont(16.0F);
      Font var4 = Fonts.REGULAR.getFont(10.0F);
      float var5 = 255.0F * (0.5F + 0.5F * this.activeAnimation.getValue());
      float var6 = (float)this.field_22789;
      float var7 = (float)this.field_22790;
      float var8 = (float)MathUtilityInterpolate(this.field_22790 / 2.0F - 20.0F, 80.0F, this.activeAnimation.getValue());

      this.activeAnimation.update(this.active);
      MainMenuBackdrop.draw(var1, var6, var7);

      this.drawGlassCenteredText(var1, var2, TextUtility.getCurrentTime(), var6 / 2.0F, var8, 255.0F * this.activeAnimation.getValue(), true);
      this.drawGlassCenteredText(var1, var3, TextUtility.getFormattedDateDigital(), var6 / 2.0F, var8 + 70.0F, 178.0F * this.activeAnimation.getValue(), false);
      var1.drawRoundedRect(
         var6 / 2.0F - 36.0F,
         var7 - 5.0F - 3.0F * this.activeAnimation.getValue(),
         72.0F,
         3.0F,
         BorderRadius.all(1.0F),
         ColorRGBA.WHITE.withAlpha(255.0F * this.activeAnimation.getValue())
      );
      var1.drawCenteredText(
         var4,
         Localizator.translate("mainmenu.next"),
         var6 / 2.0F,
         var7 - 15.0F + 3.0F * this.activeAnimation.getValue(),
         ColorRGBA.WHITE.withAlpha(155.0F * (1.0F - this.activeAnimation.getValue()))
      );

      float var9 = 0.0F;
      for (CustomButton var11 : buttons) {
         var11.getActiveAnim()
            .update((float)(buttons.size() - buttons.indexOf(var11)) > (1.0F - this.activeAnimation.getValue()) * (float)buttons.size() + 0.5F);
         var11.set(var6 / 2.0F - 69.0F + var9, var7 - 80.0F - 10.0F * var11.getActiveAnim().getValue(), 30.0F, 30.0F);
         var9 += var11.getWidth() + 6.0F;
         var11.draw(var1);
      }

      this.drawBackgroundChooser(var1, var6, var7);

      if (this.shouldShowIsland()) {
         Mytheria.getInstance().getHud().getIsland().render(var1);
      }
   }

   private void drawBackgroundChooser(UIContext context, float screenWidth, float screenHeight) {
      Font buttonFont = Fonts.MEDIUM.getFont(10.0F);
      float alpha = this.activeAnimation.getValue();
      float buttonWidth = 54.0F;
      float buttonHeight = 24.0F;
      float buttonX = screenWidth - buttonWidth - 18.0F;
      float buttonY = 18.0F;
      this.backgroundPanelAnimation.update(this.backgroundPanelOpen);
      this.drawGlassButton(context, "Фон", buttonX, buttonY, buttonWidth, buttonHeight, buttonFont, alpha, this.isHovered(buttonX, buttonY, buttonWidth, buttonHeight, context));

      float panelAlpha = alpha * this.backgroundPanelAnimation.getValue();
      if (panelAlpha <= 0.01F) {
         return;
      }

      MenuModule menu = Mytheria.getInstance().getModuleManager().getModuleSafe(MenuModule.class);
      if (menu == null) {
         return;
      }

      Font titleFont = Fonts.SEMIBOLD.getFont(11.0F);
      Font textFont = Fonts.REGULAR.getFont(8.0F);
      float panelWidth = 210.0F;
      float panelHeight = 126.0F;
      float panelX = screenWidth - panelWidth - 18.0F;
      float panelY = buttonY + 32.0F - 8.0F * (1.0F - this.backgroundPanelAnimation.getValue());
      BorderRadius radius = BorderRadius.all(12.0F);
      context.drawShadow(panelX, panelY, panelWidth, panelHeight, 22.0F, radius, ColorRGBA.rgba(0, 0, 0, (int)(70.0F * panelAlpha)));
      context.drawLiquidGlass(panelX, panelY, panelWidth, panelHeight, 0.9F, radius, ColorRGBA.rgba(255, 255, 255, (int)(58.0F * panelAlpha)), true);
      context.drawRoundedRect(panelX, panelY, panelWidth, panelHeight, radius, ColorRGBA.rgba(18, 20, 24, (int)(82.0F * panelAlpha)));
      context.drawRoundedBorder(panelX, panelY, panelWidth, panelHeight, 1.0F, radius, ColorRGBA.rgba(255, 255, 255, (int)(95.0F * panelAlpha)));
      context.drawText(titleFont, "Фон main screen", panelX + 12.0F, panelY + 11.0F, ColorRGBA.WHITE.withAlpha(240.0F * panelAlpha));
      context.drawRightText(textFont, MainMenuBackgroundManager.getAvailableCount() + " файлов", panelX + panelWidth - 12.0F, panelY + 14.0F, ColorRGBA.WHITE.withAlpha(150.0F * panelAlpha));

      float modeY = panelY + 34.0F;
      float modeWidth = 62.0F;
      this.drawModeButton(context, menu, "Встроенный", panelX + 10.0F, modeY, modeWidth, 20.0F, panelAlpha, menu.isMainBackgroundBuiltin());
      this.drawModeButton(context, menu, "Папка", panelX + 76.0F, modeY, modeWidth, 20.0F, panelAlpha, !menu.isMainBackgroundBuiltin() && !menu.isMainBackgroundRandom());
      this.drawModeButton(context, menu, "Случайный", panelX + 142.0F, modeY, modeWidth, 20.0F, panelAlpha, menu.isMainBackgroundRandom());

      String fileName = MainMenuBackgroundManager.getSelectedFileName();
      context.drawFadeoutText(textFont, fileName, panelX + 12.0F, panelY + 64.0F, ColorRGBA.WHITE.withAlpha(180.0F * panelAlpha), 0.75F, 0.98F, panelWidth - 24.0F);

      float actionY = panelY + 86.0F;
      this.drawGlassButton(context, "<", panelX + 10.0F, actionY, 28.0F, 22.0F, buttonFont, panelAlpha, this.isHovered(panelX + 10.0F, actionY, 28.0F, 22.0F, context));
      this.drawGlassButton(context, ">", panelX + 42.0F, actionY, 28.0F, 22.0F, buttonFont, panelAlpha, this.isHovered(panelX + 42.0F, actionY, 28.0F, 22.0F, context));
      this.drawGlassButton(context, "Папка", panelX + 76.0F, actionY, 64.0F, 22.0F, buttonFont, panelAlpha, this.isHovered(panelX + 76.0F, actionY, 64.0F, 22.0F, context));
      this.drawGlassButton(context, "Обновить", panelX + 146.0F, actionY, 64.0F, 22.0F, buttonFont, panelAlpha, this.isHovered(panelX + 146.0F, actionY, 64.0F, 22.0F, context));
   }

   private void drawModeButton(UIContext context, MenuModule menu, String text, float x, float y, float width, float height, float alpha, boolean selected) {
      Font font = Fonts.REGULAR.getFont(8.0F);
      boolean hovered = this.isHovered(x, y, width, height, context);
      float glow = selected ? 1.0F : 0.0F;
      this.drawGlassButton(context, text, x, y, width, height, font, alpha, hovered);
      if (selected) {
         context.drawRoundedBorder(x, y, width, height, 1.0F, BorderRadius.all(8.0F), ColorRGBA.rgba(255, 255, 255, (int)(125.0F * alpha * glow)));
      }
   }

   private void drawGlassButton(UIContext context, String text, float x, float y, float width, float height, Font font, float alpha, boolean hovered) {
      if (hovered && alpha > 0.25F) {
         CursorUtility.set(CursorType.HAND);
      }

      float hover = hovered ? 1.0F : 0.0F;
      BorderRadius radius = BorderRadius.all(Math.min(width, height) / 2.0F);
      context.drawLiquidGlass(x, y, width, height, 0.85F, radius, ColorRGBA.rgba(255, 255, 255, (int)((34.0F + 24.0F * hover) * alpha)), true);
      context.drawRoundedRect(x, y, width, height, radius, ColorRGBA.rgba(24, 26, 30, (int)((72.0F + 18.0F * hover) * alpha)));
      context.drawRoundedRect(x + 6.0F, y + 3.0F, Math.max(1.0F, width - 12.0F), Math.max(1.0F, height * 0.28F), BorderRadius.all(height * 0.22F), ColorRGBA.rgba(255, 255, 255, (int)((18.0F + 18.0F * hover) * alpha)));
      context.drawRoundedBorder(x, y, width, height, 0.9F, radius, ColorRGBA.rgba(255, 255, 255, (int)((72.0F + 46.0F * hover) * alpha)));
      context.drawCenteredText(font, text, x + width / 2.0F, y + height / 2.0F - font.height() / 2.0F + 1.0F, ColorRGBA.WHITE.withAlpha((205.0F + 35.0F * hover) * alpha));
   }

   private boolean isHovered(float x, float y, float width, float height, UIContext context) {
      return this.isHovered(x, y, width, height, context.getMouseX(), context.getMouseY());
   }

   private boolean isHovered(float x, float y, float width, float height, double mouseX, double mouseY) {
      return mouseX >= (double)x && mouseX <= (double)(x + width) && mouseY >= (double)y && mouseY <= (double)(y + height);
   }

   private void drawGlassCenteredText(UIContext var1, Font var2, String var3, float var4, float var5, float var6, boolean var7) {
      float var8 = Math.max(0.0F, Math.min(255.0F, var6));
      var1.drawCenteredText(var2, var3, var4 + 3.0F, var5 + 4.0F, ColorRGBA.rgba(0, 0, 0, (int)(74.0F * var8 / 255.0F)));
      var1.drawCenteredText(var2, var3, var4 - 1.0F, var5 - 1.0F, ColorRGBA.rgba(255, 255, 255, (int)((var7 ? 92.0F : 58.0F) * var8 / 255.0F)));
      var1.drawCenteredText(var2, var3, var4 + 1.0F, var5 + 1.0F, ColorRGBA.rgba(80, 86, 96, (int)((var7 ? 86.0F : 58.0F) * var8 / 255.0F)));
      var1.drawCenteredText(var2, var3, var4, var5, ColorRGBA.rgba(244, 246, 250, (int)var8));
   }

   @Override
   public void onMouseClicked(double var1, double var3, MouseButton var5) {
      if (this.shouldShowIsland() && Mytheria.getInstance().getHud().getIsland().handleClick((float)var1, (float)var3, var5.getButtonIndex())) {
         return;
      }

      if (var5 == MouseButton.LEFT && this.handleBackgroundChooserClick(var1, var3)) {
         return;
      }

      for (CustomButton var7 : buttons) {
         if (var7.hovered(var1, var3) && var7.getActiveAnim().getValue() == 1.0F) {
            var7.click(var1, var3, var5.getButtonIndex());
            return;
         }
      }

      super.onMouseClicked(var1, var3, var5);
   }

   private boolean handleBackgroundChooserClick(double mouseX, double mouseY) {
      float screenWidth = (float)this.field_22789;
      float buttonWidth = 54.0F;
      float buttonHeight = 24.0F;
      float buttonX = screenWidth - buttonWidth - 18.0F;
      float buttonY = 18.0F;
      if (this.isHovered(buttonX, buttonY, buttonWidth, buttonHeight, mouseX, mouseY)) {
         this.backgroundPanelOpen = !this.backgroundPanelOpen;
         return true;
      }

      if (!this.backgroundPanelOpen) {
         return false;
      }

      MenuModule menu = Mytheria.getInstance().getModuleManager().getModuleSafe(MenuModule.class);
      if (menu == null) {
         return false;
      }

      float panelWidth = 210.0F;
      float panelX = screenWidth - panelWidth - 18.0F;
      float panelY = buttonY + 32.0F;
      float modeY = panelY + 34.0F;
      if (this.isHovered(panelX + 10.0F, modeY, 62.0F, 20.0F, mouseX, mouseY)) {
         menu.selectMainBackgroundBuiltin();
         return true;
      }
      if (this.isHovered(panelX + 76.0F, modeY, 62.0F, 20.0F, mouseX, mouseY)) {
         menu.selectMainBackgroundFolder();
         return true;
      }
      if (this.isHovered(panelX + 142.0F, modeY, 62.0F, 20.0F, mouseX, mouseY)) {
         menu.selectMainBackgroundRandom();
         MainMenuBackgroundManager.refresh();
         return true;
      }

      float actionY = panelY + 86.0F;
      if (this.isHovered(panelX + 10.0F, actionY, 28.0F, 22.0F, mouseX, mouseY)) {
         MainMenuBackgroundManager.selectOffset(-1);
         return true;
      }
      if (this.isHovered(panelX + 42.0F, actionY, 28.0F, 22.0F, mouseX, mouseY)) {
         MainMenuBackgroundManager.selectOffset(1);
         return true;
      }
      if (this.isHovered(panelX + 76.0F, actionY, 64.0F, 22.0F, mouseX, mouseY)) {
         MainMenuBackgroundManager.openFolder();
         return true;
      }
      if (this.isHovered(panelX + 146.0F, actionY, 64.0F, 22.0F, mouseX, mouseY)) {
         MainMenuBackgroundManager.refresh();
         return true;
      }

      return this.isHovered(panelX, panelY, panelWidth, 126.0F, mouseX, mouseY);
   }

   @Override
   public boolean method_25404(int var1, int var2, int var3) {
      if (var1 == 69) {
         Mytheria.getInstance().getThemeManager().switchTheme();
      }

      if (method_25441() && var1 == 82) {
         this.openMultiplayer();
      }

      if (method_25441() && var1 == 84) {
         this.openSingleplayer();
      }

      return super.method_25404(var1, var2, var3);
   }

   @Override
   public boolean method_25422() {
      return false;
   }

   private boolean shouldShowIsland() {
      return Mytheria.getInstance().getMusicTracker().haveActiveSession();
   }

   private void openSingleplayer() {
      class_310.method_1551().method_1507(new class_526(this));
   }

   private void openMultiplayer() {
      class_310.method_1551().method_1507(new class_500(this));
   }

   private void openOptions() {
      class_310 var1 = class_310.method_1551();
      var1.method_1507(new class_429(this, var1.field_1690));
   }

   private static double MathUtilityInterpolate(double var0, double var2, double var4) {
      return var0 + (var2 - var0) * var4;
   }
}
