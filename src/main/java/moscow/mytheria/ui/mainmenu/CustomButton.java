package moscow.mytheria.ui.mainmenu;

import moscow.mytheria.Mytheria;
import moscow.mytheria.framework.base.UIContext;
import moscow.mytheria.framework.objects.BorderRadius;
import moscow.mytheria.utility.animation.base.Animation;
import moscow.mytheria.utility.animation.base.Easing;
import moscow.mytheria.utility.colors.ColorRGBA;
import moscow.mytheria.utility.game.cursor.CursorType;
import moscow.mytheria.utility.game.cursor.CursorUtility;
import moscow.mytheria.utility.render.obj.Rect;

public class CustomButton extends Rect {
   private final String icon;
   private final float iconSize;
   private final Runnable onClick;
   private final Animation activeAnim = new Animation(420L, 0.0F, Easing.FIGMA_EASE_IN_OUT);
   private final Animation hoverAnim = new Animation(300L, 0.0F, Easing.FIGMA_EASE_IN_OUT);
   private final Animation pressAnim = new Animation(150L, 0.0F, Easing.FIGMA_EASE_IN_OUT);
   private long pressedUntilMs;

   public CustomButton(String var1, float var2, Runnable var3) {
      this.icon = var1;
      this.iconSize = var2;
      this.onClick = var3;
   }

   public void draw(UIContext var1) {
      boolean var2 = this.hovered(var1.getMouseX(), var1.getMouseY()) && this.activeAnim.getValue() == 1.0F;
      if (var2) {
         CursorUtility.set(CursorType.HAND);
      }

      this.hoverAnim.update(var2);
      this.pressAnim.update(System.currentTimeMillis() < this.pressedUntilMs);
      float var3 = this.activeAnim.getValue();
      float var4 = this.hoverAnim.getValue();
      float var10 = this.pressAnim.getValue();
      float var11 = 2.4F * var10;
      float var5 = this.x - 3.0F * var4 + var11;
      float var6 = this.y - 3.0F * var4 + var11;
      float var7 = this.width + 6.0F * var4 - var11 * 2.0F;
      float var8 = this.height + 6.0F * var4 - var11 * 2.0F;
      BorderRadius var9 = BorderRadius.all(Math.min(var7, var8) / 2.0F);
      var1.drawShadow(var5, var6 + 2.0F * var10, var7, var8, 12.0F, var9, ColorRGBA.rgba(0, 0, 0, (int)((86.0F - 24.0F * var10) * var3)));
      var1.drawBlurredRect(var5, var6, var7, var8, 12.0F, var9, ColorRGBA.rgba(255, 255, 255, (int)((20.0F + 16.0F * var4 + 16.0F * var10) * var3)));
      var1.drawLiquidGlass(var5, var6, var7, var8, 0.82F, var9, ColorRGBA.rgba(255, 255, 255, (int)((38.0F + 26.0F * var4 + 30.0F * var10) * var3)), true);
      var1.drawRoundedBorder(var5, var6, var7, var8, 1.0F, var9, ColorRGBA.rgba(255, 255, 255, (int)((76.0F + 64.0F * var4 + 70.0F * var10) * var3)));
      var1.drawRoundedRect(var5 + 6.0F, var6 + 4.0F, var7 - 12.0F, Math.max(1.0F, var8 * 0.32F), BorderRadius.all(var8 * 0.22F), ColorRGBA.rgba(255, 255, 255, (int)((18.0F + 20.0F * var4 + 22.0F * var10) * var3)));
      float var12 = this.iconSize - 1.4F * var10;
      var1.drawTexture(
         Mytheria.id(this.icon),
         this.x + (this.width - var12) / 2.0F,
         this.y + (this.height - var12) / 2.0F + 0.8F * var10,
         var12,
         var12,
         ColorRGBA.WHITE.withAlpha(255.0F * var3)
      );
   }

   public void click(double var1, double var3, int var5) {
      if (this.hovered(var1, var3) && var5 == 0 && this.activeAnim.getValue() >= 0.98F) {
         this.pressedUntilMs = System.currentTimeMillis() + 145L;
         this.onClick.run();
      }
   }

   public Animation getActiveAnim() {
      return this.activeAnim;
   }
}
