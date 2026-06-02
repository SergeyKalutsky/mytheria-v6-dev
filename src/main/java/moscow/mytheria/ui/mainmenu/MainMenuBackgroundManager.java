package moscow.mytheria.ui.mainmenu;

import java.awt.Desktop;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import javax.imageio.ImageIO;
import moscow.mytheria.Mytheria;
import moscow.mytheria.framework.base.UIContext;
import moscow.mytheria.systems.modules.modules.visuals.MenuModule;
import moscow.mytheria.ui.components.gif.GifDecoder;
import moscow.mytheria.utility.colors.ColorRGBA;
import moscow.mytheria.utility.game.WebUtility;
import net.minecraft.class_1011;
import net.minecraft.class_1043;
import net.minecraft.class_1044;
import net.minecraft.class_2960;
import net.minecraft.class_310;

public final class MainMenuBackgroundManager {
   private static final String README = "Put .png, .jpg, .jpeg, or animated .gif files here.\r\n"
      + "GIF files are used as looped live wallpapers.\r\n"
      + "For mp4/webm, convert the short loop to gif first.\r\n";
   private static final List<Path> files = new ArrayList<>();
   private static Path loadedPath;
   private static Path randomPath;
   private static class_2960 textureId;
   private static class_1043 dynamicTexture;
   private static AnimatedGif animatedGif;
   private static long lastScanAtMs;

   private MainMenuBackgroundManager() {
   }

   public static Path getFolder() {
      File runDirectory = class_310.method_1551().field_1697;
      return runDirectory.toPath().resolve("mytheria").resolve("mainmenu-backgrounds");
   }

   public static void openFolder() {
      try {
         ensureFolder();
         if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().open(getFolder().toFile());
         }
      } catch (Throwable ignored) {
      }
   }

   public static void refresh() {
      lastScanAtMs = 0L;
      randomPath = null;
      loadedPath = null;
      dispose();
      scan();
   }

   public static int getAvailableCount() {
      scan();
      return files.size();
   }

   public static String getSelectedFileName() {
      MenuModule menu = Mytheria.getInstance().getModuleManager().getModuleSafe(MenuModule.class);
      if (menu == null) {
         return "";
      }

      Path path = selectPath(menu);
      return path == null ? "файлов нет" : path.getFileName().toString();
   }

   public static void selectOffset(int offset) {
      MenuModule menu = Mytheria.getInstance().getModuleManager().getModuleSafe(MenuModule.class);
      if (menu == null) {
         return;
      }

      scan();
      if (files.isEmpty()) {
         menu.setMainBackgroundFile("");
         return;
      }

      String currentName = menu.getMainBackgroundFile();
      int index = 0;
      if (currentName != null && !currentName.isBlank()) {
         String cleanName = Path.of(currentName.trim()).getFileName().toString();
         for (int i = 0; i < files.size(); ++i) {
            if (files.get(i).getFileName().toString().equalsIgnoreCase(cleanName)) {
               index = i;
               break;
            }
         }
      }

      int nextIndex = Math.floorMod(index + offset, files.size());
      menu.setMainBackgroundFile(files.get(nextIndex).getFileName().toString());
      menu.selectMainBackgroundFolder();
      randomPath = null;
      loadedPath = null;
      dispose();
   }

   public static boolean draw(UIContext context, float x, float y, float width, float height, ColorRGBA color) {
      MenuModule menu = Mytheria.getInstance().getModuleManager().getModuleSafe(MenuModule.class);
      if (menu == null || menu.isMainBackgroundBuiltin()) {
         return false;
      }

      Path path = selectPath(menu);
      if (path == null) {
         return false;
      }

      if (!path.equals(loadedPath) && !load(path)) {
         return false;
      }

      if (animatedGif != null) {
         animatedGif.update();
      }

      if (textureId == null) {
         return false;
      }

      context.drawTexture(textureId, x, y, width, height, color);
      return true;
   }

   private static Path selectPath(MenuModule menu) {
      scan();
      if (files.isEmpty()) {
         return null;
      }

      if (menu.isMainBackgroundRandom()) {
         if (randomPath == null || !files.contains(randomPath)) {
            randomPath = files.get(ThreadLocalRandom.current().nextInt(files.size()));
         }
         return randomPath;
      }

      String name = menu.getMainBackgroundFile();
      if (name != null && !name.isBlank()) {
         String cleanName = Path.of(name.trim()).getFileName().toString();
         for (Path path : files) {
            if (path.getFileName().toString().equalsIgnoreCase(cleanName)) {
               return path;
            }
         }
      }

      return files.get(0);
   }

   private static void scan() {
      long now = System.currentTimeMillis();
      if (now - lastScanAtMs < 2000L) {
         return;
      }

      lastScanAtMs = now;
      ensureFolder();
      files.clear();
      try (var stream = Files.list(getFolder())) {
         stream.filter(Files::isRegularFile)
            .filter(MainMenuBackgroundManager::isSupported)
            .sorted(Comparator.comparing(path -> path.getFileName().toString().toLowerCase(Locale.ROOT)))
            .forEach(files::add);
      } catch (IOException ignored) {
      }
   }

   private static void ensureFolder() {
      try {
         Files.createDirectories(getFolder());
         Path readme = getFolder().resolve("README.txt");
         if (!Files.exists(readme)) {
            Files.writeString(readme, README);
         }
      } catch (IOException ignored) {
      }
   }

   private static boolean isSupported(Path path) {
      String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
      return name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".gif");
   }

   private static boolean load(Path path) {
      dispose();
      try {
         if (path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".gif")) {
            animatedGif = AnimatedGif.load(path);
            textureId = animatedGif.textureId;
            dynamicTexture = animatedGif.texture;
         } else {
            BufferedImage image = ImageIO.read(path.toFile());
            if (image == null) {
               return false;
            }

            textureId = Mytheria.id("local/mainmenu_background_" + Integer.toHexString(path.toAbsolutePath().toString().hashCode()));
            dynamicTexture = new class_1043(WebUtility.bufferedImageToNativeImage(image, false));
            class_310.method_1551().method_1531().method_4616(textureId, (class_1044)dynamicTexture);
         }

         loadedPath = path;
         return true;
      } catch (Throwable ignored) {
         dispose();
         return false;
      }
   }

   private static void dispose() {
      if (textureId != null) {
         try {
            class_310.method_1551().method_1531().method_4615(textureId);
         } catch (Throwable ignored) {
         }
      }

      if (dynamicTexture != null) {
         try {
            dynamicTexture.close();
         } catch (Throwable ignored) {
         }
      }

      textureId = null;
      dynamicTexture = null;
      animatedGif = null;
   }

   private static final class AnimatedGif {
      private final GifDecoder decoder;
      private final class_1011 image;
      private final class_1043 texture;
      private final class_2960 textureId;
      private final int frameCount;
      private int currentFrame;
      private long lastFrameAtMs;

      private AnimatedGif(GifDecoder decoder, class_1011 image, class_1043 texture, class_2960 textureId) {
         this.decoder = decoder;
         this.image = image;
         this.texture = texture;
         this.textureId = textureId;
         this.frameCount = Math.max(1, decoder.getFrameCount());
      }

      private static AnimatedGif load(Path path) throws IOException {
         GifDecoder decoder = new GifDecoder();
         try (FileInputStream stream = new FileInputStream(path.toFile())) {
            decoder.read(stream);
         }

         BufferedImage firstFrame = decoder.getFrame(0);
         class_1011 image = new class_1011(class_1011.class_1012.field_4997, firstFrame.getWidth(), firstFrame.getHeight(), false);
         copy(firstFrame, image);
         class_1043 texture = new class_1043(image);
         class_2960 textureId = Mytheria.id("local/mainmenu_live_background_" + Integer.toHexString(path.toAbsolutePath().toString().hashCode()));
         class_310.method_1551().method_1531().method_4616(textureId, (class_1044)texture);
         texture.method_4524();
         return new AnimatedGif(decoder, image, texture, textureId);
      }

      private void update() {
         int delay = Math.max(20, this.decoder.getDelay(this.currentFrame));
         long now = System.currentTimeMillis();
         if (now - this.lastFrameAtMs < delay) {
            return;
         }

         this.lastFrameAtMs = now;
         this.currentFrame = (this.currentFrame + 1) % this.frameCount;
         copy(this.decoder.getFrame(this.currentFrame), this.image);
         this.texture.method_4524();
      }

      private static void copy(BufferedImage source, class_1011 target) {
         for (int y = 0; y < source.getHeight(); ++y) {
            for (int x = 0; x < source.getWidth(); ++x) {
               target.method_61941(x, y, source.getRGB(x, y));
            }
         }
      }
   }
}
