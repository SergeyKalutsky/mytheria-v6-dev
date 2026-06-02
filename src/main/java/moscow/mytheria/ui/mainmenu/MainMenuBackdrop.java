package moscow.mytheria.ui.mainmenu;

import java.util.concurrent.ThreadLocalRandom;
import moscow.mytheria.Mytheria;
import moscow.mytheria.framework.base.UIContext;
import moscow.mytheria.framework.objects.BorderRadius;
import moscow.mytheria.utility.colors.ColorRGBA;

final class MainMenuBackdrop {
   private static final String[] WALLPAPERS = new String[]{
      "image/mainmenu/wallpapers/wallpaper_1.png",
      "image/mainmenu/wallpapers/wallpaper_2.png",
      "image/mainmenu/wallpapers/wallpaper_3.png",
      "image/mainmenu/wallpapers/wallpaper_4.png",
      "image/mainmenu/wallpapers/wallpaper_5.png"
   };
   private static String wallpaper = WALLPAPERS[ThreadLocalRandom.current().nextInt(WALLPAPERS.length)];
   private static float parallaxX;
   private static float parallaxY;

   private MainMenuBackdrop() {
   }

   static void draw(UIContext context, float screenWidth, float screenHeight) {
      float targetX = ((float)context.getMouseX() / Math.max(1.0F, screenWidth) - 0.5F) * 34.0F;
      float targetY = ((float)context.getMouseY() / Math.max(1.0F, screenHeight) - 0.5F) * 24.0F;
      parallaxX += (targetX - parallaxX) * 0.06F;
      parallaxY += (targetY - parallaxY) * 0.06F;
      float scale = 1.055F;
      float width = screenWidth * scale;
      float height = screenHeight * scale;
      if (!MainMenuBackgroundManager.draw(context, (screenWidth - width) / 2.0F - parallaxX, (screenHeight - height) / 2.0F - parallaxY, width, height, ColorRGBA.WHITE)) {
         context.drawTexture(Mytheria.id(wallpaper), (screenWidth - width) / 2.0F - parallaxX, (screenHeight - height) / 2.0F - parallaxY, width, height, ColorRGBA.WHITE);
      }
      context.drawRoundedRect(0.0F, 0.0F, screenWidth, screenHeight, BorderRadius.ZERO, ColorRGBA.rgba(0, 0, 0, 30));
   }
}
