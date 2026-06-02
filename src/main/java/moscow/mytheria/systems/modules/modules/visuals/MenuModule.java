package moscow.mytheria.systems.modules.modules.visuals;

import moscow.mytheria.Mytheria;
import moscow.mytheria.systems.modules.api.ModuleCategory;
import moscow.mytheria.systems.modules.api.ModuleInfo;
import moscow.mytheria.systems.modules.impl.BaseModule;
import moscow.mytheria.systems.modules.modules.other.Sounds;
import moscow.mytheria.systems.setting.settings.ButtonSetting;
import moscow.mytheria.systems.setting.settings.ModeSetting;
import moscow.mytheria.systems.setting.settings.StringSetting;
import moscow.mytheria.ui.mainmenu.MainMenuBackgroundManager;
import moscow.mytheria.ui.menu.MenuScreen;
import moscow.mytheria.ui.menu.api.MenuCloseListener;
import moscow.mytheria.utility.sounds.ClientSounds;
import net.minecraft.class_437;

@ModuleInfo(name = "Menu", category = ModuleCategory.VISUALS, key = 344, desc = "modules.descriptions.menu")
public class MenuModule extends BaseModule {
   private static final MenuCloseListener menuCloseListener = new MenuCloseListener();
   private final ModeSetting mode = new ModeSetting(this, "modules.settings.menu.mode");
   private final ModeSetting.Value dropdown = new ModeSetting.Value(this.mode, "modules.settings.menu.mode.dropdown");
   private final ModeSetting mainBackground = new ModeSetting(this, "Фон main screen");
   private final ModeSetting.Value mainBackgroundBuiltin = new ModeSetting.Value(this.mainBackground, "Встроенный");
   private final ModeSetting.Value mainBackgroundFolder = new ModeSetting.Value(this.mainBackground, "Из папки");
   private final ModeSetting.Value mainBackgroundRandom = new ModeSetting.Value(this.mainBackground, "Случайный");
   private final StringSetting mainBackgroundFile = new StringSetting(this, "Файл фона", () -> this.mainBackgroundBuiltin.isSelected() || this.mainBackgroundRandom.isSelected()).text("");
   private final ButtonSetting previousBackground = new ButtonSetting(this, "Предыдущий фон", () -> this.mainBackgroundBuiltin.isSelected() || this.mainBackgroundRandom.isSelected()).action(() -> MainMenuBackgroundManager.selectOffset(-1));
   private final ButtonSetting nextBackground = new ButtonSetting(this, "Следующий фон", () -> this.mainBackgroundBuiltin.isSelected() || this.mainBackgroundRandom.isSelected()).action(() -> MainMenuBackgroundManager.selectOffset(1));
   private final ButtonSetting openBackgroundFolder = new ButtonSetting(this, "Открыть папку фонов").action(MainMenuBackgroundManager::openFolder);
   private final ButtonSetting refreshBackgrounds = new ButtonSetting(this, "Обновить фоны").action(MainMenuBackgroundManager::refresh);
   private final ButtonSetting randomizeBackground = new ButtonSetting(this, "Новый случайный фон", () -> !this.mainBackgroundRandom.isSelected()).action(MainMenuBackgroundManager::refresh);

   @Override
   public void onEnable() {
      if (!(mc.field_1755 instanceof MenuScreen)) {
         MenuScreen menuScreen = Mytheria.getInstance().getMenuScreen();
         mc.method_1507((class_437)menuScreen);
         Sounds soundsModule = Mytheria.getInstance().getModuleManager().getModule(Sounds.class);
         if (soundsModule.isEnabled()) {
            ClientSounds.CLICKGUI_OPEN.play(soundsModule.getVolume().getCurrentValue());
         }

         super.onEnable();
      }
   }

   @Override
   public void onDisable() {
      if (mc.field_1755 instanceof MenuScreen) {
         mc.method_1507(null);
         Mytheria.getInstance().getMenuScreen().setClosing(true);
      }

      super.onDisable();
   }

   public boolean isMainBackgroundBuiltin() {
      return this.mainBackgroundBuiltin.isSelected();
   }

   public boolean isMainBackgroundRandom() {
      return this.mainBackgroundRandom.isSelected();
   }

   public void selectMainBackgroundBuiltin() {
      this.mainBackgroundBuiltin.select();
   }

   public void selectMainBackgroundFolder() {
      this.mainBackgroundFolder.select();
   }

   public void selectMainBackgroundRandom() {
      this.mainBackgroundRandom.select();
   }

   public String getMainBackgroundModeName() {
      return this.mainBackground.getValue().getName();
   }

   public String getMainBackgroundFile() {
      return this.mainBackgroundFile.getText();
   }

   public void setMainBackgroundFile(String fileName) {
      this.mainBackgroundFile.setText(fileName);
   }
}
