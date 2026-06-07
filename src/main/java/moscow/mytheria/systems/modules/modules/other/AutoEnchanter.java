package moscow.mytheria.systems.modules.modules.other;

import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import hook.aeu;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import moscow.mytheria.framework.base.CustomDrawContext;
import moscow.mytheria.framework.msdf.Font;
import moscow.mytheria.framework.msdf.Fonts;
import moscow.mytheria.framework.objects.BorderRadius;
import moscow.mytheria.systems.event.EventListener;
import moscow.mytheria.systems.event.impl.game.ChatReceiveEvent;
import moscow.mytheria.systems.event.impl.game.GameTickEvent;
import moscow.mytheria.systems.event.impl.render.HudRenderEvent;
import moscow.mytheria.systems.modules.api.ModuleCategory;
import moscow.mytheria.systems.modules.api.ModuleInfo;
import moscow.mytheria.systems.modules.impl.BaseModule;
import moscow.mytheria.systems.secret.SecretModeManager;
import moscow.mytheria.systems.setting.settings.BooleanSetting;
import moscow.mytheria.systems.setting.settings.ModeSetting;
import moscow.mytheria.systems.setting.settings.SliderSetting;
import moscow.mytheria.systems.setting.settings.StringSetting;
import moscow.mytheria.utility.colors.ColorRGBA;
import moscow.mytheria.utility.game.MessageUtility;
import moscow.mytheria.utility.time.Timer;
import net.minecraft.class_1268;
import net.minecraft.class_1703;
import net.minecraft.class_1706;
import net.minecraft.class_1713;
import net.minecraft.class_1714;
import net.minecraft.class_1715;
import net.minecraft.class_1718;
import net.minecraft.class_1731;
import net.minecraft.class_1735;
import net.minecraft.class_1792;
import net.minecraft.class_1799;
import net.minecraft.class_1802;
import net.minecraft.class_1887;
import net.minecraft.class_1937;
import net.minecraft.class_2246;
import net.minecraft.class_2248;
import net.minecraft.class_2338;
import net.minecraft.class_2350;
import net.minecraft.class_243;
import net.minecraft.class_2561;
import net.minecraft.class_299;
import net.minecraft.class_337;
import net.minecraft.class_345;
import net.minecraft.class_3532;
import net.minecraft.class_3803;
import net.minecraft.class_3965;
import net.minecraft.class_437;
import net.minecraft.class_465;
import net.minecraft.class_4862;
import net.minecraft.class_516;
import net.minecraft.class_639;
import net.minecraft.class_642;
import net.minecraft.class_10297;
import net.minecraft.class_10352;
import net.minecraft.class_10363;
import net.minecraft.class_6880;
import net.minecraft.class_9290;
import net.minecraft.class_9334;

@ModuleInfo(
   name = "AutoEnchanter",
   category = ModuleCategory.OTHER,
   desc = "Автоматическая покупка, крафт, зачарование и продажа предметов через аукцион"
)
public class AutoEnchanter extends BaseModule {
   private static final java.util.function.BooleanSupplier HIDDEN_SETTING = () -> true;

   static {
      System.out.println("[Mytheria-PATCH] AutoEnchanter (v6/AutoEnchanter) class loaded at startup");
      moscow.mytheria.logger.MytheriaLogger.log("class_loaded", "AutoEnchanter-v6");
   }

   // ============== PATCH: logging helpers ==============
   private void setState(AutoEnchanter.State newState) {
      AutoEnchanter.State old = this.state;
      this.state = newState;
      if (old != newState) {
         moscow.mytheria.logger.MytheriaLogger.event("state_change")
            .with("from", old == null ? "null" : old.name())
            .with("to", newState == null ? "null" : newState.name())
            .with("craftStage", this.craftStage == null ? "null" : this.craftStage.name())
            .with("buyIndex", this.buyIndex)
            .with("buyAttempts", this.buyAttempts)
            .emit();
      }
   }

   private void setCraftStage(AutoEnchanter.CraftStage newStage) {
      AutoEnchanter.CraftStage old = this.craftStage;
      this.craftStage = newStage;
      if (old != newStage) {
         moscow.mytheria.logger.MytheriaLogger.event("craft_stage_change")
            .with("from", old == null ? "null" : old.name())
            .with("to", newStage == null ? "null" : newStage.name())
            .with("state", this.state == null ? "null" : this.state.name())
            .emit();
      }
   }

   private static String describeStack(net.minecraft.class_1799 stack) {
      try {
         if (stack == null || stack.method_7960()) return "empty";
         String id = net.minecraft.class_7923.field_41178.method_10221(stack.method_7909()).toString();
         int count = stack.method_7947();
         String name = stack.method_7964() != null ? stack.method_7964().getString() : "";
         return id + " x" + count + (name.isEmpty() ? "" : " \"" + name + "\"");
      } catch (Throwable t) {
         return "<err:" + t.getClass().getSimpleName() + ">";
      }
   }
   // ============== END PATCH ==============

   private static final Pattern PRICE_PATTERN = Pattern.compile("([0-9][0-9\\s,\\.]+)");
   private static final Pattern AUCTION_PAGE_PATTERN = Pattern.compile("\\[(\\d+)\\s*/\\s*(\\d+)\\]");
   private static final Pattern ENCHANT_LEVEL_TWO_PATTERN = Pattern.compile("(?i)(\\b2\\b|\\bII\\b|\\u2161|\\u2171|\\bll\\b)");
   private static final String SEARCH_MAGNET = "Магнит";
   private static final String SEARCH_MAGNET_EN = "Magnet";
   private static final String SEARCH_BULLDOZER = "Бульдозер";
   private static final String SEARCH_BULLDOZER_EN = "Bulldozer";
   private static final String SEARCH_WATER_WALK = "Подводная ходьба";
   private static final String SEARCH_WATER_WALK_EN = "Depth Strider";
   private static final String SEARCH_DETECTION = "Детекция";
   private static final String SEARCH_DETECTION_EN = "Detection";
   private static final String SEARCH_POISON = "Яд";
   private static final String SEARCH_POISON_EN = "Poison";
   private static final String SEARCH_VAMPIRISM = "Вампиризм";
   private static final String SEARCH_VAMPIRISM_EN = "Vampirism";
   private static final String SEARCH_OXIDATION = "Окисление";
   private static final String SEARCH_OXIDATION_EN = "Oxidation";
   private static final String SEARCH_SHARPNESS_RU = "Острота";
   private static final String SEARCH_SHARPNESS_EN = "Sharpness";
   private static final String SEARCH_SMITE_RU = "Кара";
   private static final String SEARCH_SMITE_EN = "Smite";
   private static final String SEARCH_BANE_RU = "Бич";
   private static final String SEARCH_BANE_EN = "Bane of Arthropods";
   private static final String SEARCH_SWEEPING_RU = "Разящ";
   private static final String SEARCH_SWEEPING_EN = "Sweeping Edge";
   private static final String SEARCH_FIRE_RU = "Огня";
   private static final String SEARCH_FIRE_EN = "Fire Aspect";
   private static final String SEARCH_LOOTING_RU = "Добыч";
   private static final String SEARCH_LOOTING_EN = "Looting";
   private static final String SEARCH_UNBREAKING_RU = "Прочн";
   private static final String SEARCH_UNBREAKING_EN = "Unbreaking";
   private static final String SEARCH_EXPERT_RU = "Опытн";
   private static final String SEARCH_HELL_RU = "Ад";
   private static final String[] MARKET_ENCHANT_NEEDLES = new String[]{"Бульдозер", "Bulldozer"};
   private static final boolean MARKET_ENCHANT_REQUIRES_LEVEL_TWO = true;
   private static final String XP_SEARCH_DEFAULT = "Опыт с уровнем 15";
   private static final String WOOD_SEARCH_DEFAULT = "Дерево";
   private static final int XP_LEVELS_PER_BOTTLE = 15;
   private static final int MIN_XP_BOTTLES = 3;
   private static final float MIN_AUCTION_DURABILITY = 0.35F;
   private static final int MAX_LAPIS_RESERVE = 128;
   private static final long RELIST_INTERVAL_MS = 60000L;
   private static final long RELIST_SCREEN_TIMEOUT_MS = 3000L;
   private static final long RELIST_ESC_DELAY_MS = 200L;
   private static final long RELIST_ESC_TIMEOUT_MS = 2000L;
   private static final long BUY_CLOSE_DELAY_MS = 200L;
   private static final long BUY_CLOSE_TIMEOUT_MS = 2000L;
   private static final long AUCTION_STUCK_CHECK_MS = 5000L;
   private static final long SEARCH_MIN_VIEW_MS = 300L;
   private static final long SELL_HOLD_MS = 500L;
   private static final long BUY_SCREEN_OPEN_DELAY_MS = 1500L;
   private static final long BUY_CONFIRM_STUCK_MS = 4000L;
   private static final long ANVIL_SCREEN_TIMEOUT_MS = 3000L;
   private static final long SMITH_SCREEN_TIMEOUT_MS = 3000L;
   private static final long AFK_RETRY_WALK_MS = 1200L;
   private static final long AFK_MOVE_STEP_MS = 120L;
   private static final float AFK_MOUSE_YAW_DELTA = 6.0F;
   private static final float AFK_MOUSE_PITCH_DELTA = 3.0F;
   private static final int BLOCK_OPEN_RETRY_COUNT = 10;
   private static final long BLOCK_OPEN_RETRY_DELAY_MS = 120L;
   private static final float ROTATION_BASE_STEP = 28.0F;
   private static final float ROTATION_JITTER_YAW = 0.35F;
   private static final float ROTATION_JITTER_PITCH = 0.25F;
   private static final double ROTATION_MULTIPOINT_RADIUS = 0.18;
   private static final long ACCOUNT_QUEUE_HEARTBEAT_MS = 1000L;
   private static final long ACCOUNT_QUEUE_STALE_MS = 7000L;
   private static final long ACCOUNT_QUEUE_RECONNECT_MS = 2000L;
   private static final long ACCOUNT_QUEUE_TURN_MAX_MS = 12000L;
   private static final String KEY_BASE = "base";
   private static final String KEY_DIAMOND = "diamond";
   private static final String KEY_WOOD = "wood";
   private static final String KEY_LAPIS = "lapis";
   private static final String KEY_XP = "xp";
   private static final String KEY_OUTPUT = "output";
   private static final SwordEnchantRequirement[] SWORD_REQUIREMENTS = new SwordEnchantRequirement[]{
      new SwordEnchantRequirement("sharpness", new String[]{"Острота", "Sharpness"}, 7),
      new SwordEnchantRequirement("smite", new String[]{"Кара", "Smite"}, 7),
      new SwordEnchantRequirement("bane", new String[]{"Бич", "Bane of Arthropods"}, 7),
      new SwordEnchantRequirement("sweeping", new String[]{"Разящ", "Sweeping Edge"}, 3),
      new SwordEnchantRequirement("fire", new String[]{"Огня", "Fire Aspect"}, 2),
      new SwordEnchantRequirement("looting", new String[]{"Добыч", "Looting"}, 5),
      new SwordEnchantRequirement("unbreaking", new String[]{"Прочн", "Unbreaking"}, 5),
      new SwordEnchantRequirement("expert", new String[]{"Опытн"}, 3),
      new SwordEnchantRequirement("vampirism", new String[]{"Вампиризм", "Vampirism"}, 2),
      new SwordEnchantRequirement("oxidation", new String[]{"Окисление", "Oxidation"}, 2),
      new SwordEnchantRequirement("hell", new String[]{"Ад"}, 3),
      new SwordEnchantRequirement("detection", new String[]{"Детекция", "Detection"}, 3)
   };
   // ============== PATCH: section/tab selector (menu navigation) ==============
   private final ItemClassifier classifier = new ItemClassifier(this);
   private final ModeSetting section = new ModeSetting(this, "Раздел");
   private final ModeSetting.Value secGeneral = new ModeSetting.Value(this.section, "Общие");
   private final ModeSetting.Value secPickaxe = new ModeSetting.Value(this.section, "Кирки");
   private final ModeSetting.Value secSword = new ModeSetting.Value(this.section, "Мечи");
   private final ModeSetting.Value secItems = new ModeSetting.Value(this.section, "Предметы");
   private final ModeSetting.Value secAccounts = new ModeSetting.Value(this.section, "Аккаунты");
   // Show only when the matching section tab is active:
   private final java.util.function.BooleanSupplier inGeneral = () -> !this.section.is(this.secGeneral);
   private final java.util.function.BooleanSupplier inPickaxe = () -> !this.section.is(this.secPickaxe);
   private final java.util.function.BooleanSupplier inSword   = () -> !this.section.is(this.secSword);
   // ============== END PATCH ==============
   private final ModeSetting mode = new ModeSetting(this, "Режим", HIDDEN_SETTING);
   private final ModeSetting.Value modeEnchantTarget = new ModeSetting.Value(this.mode, "Чары (выбор)");
   private final ModeSetting targetItemType = new ModeSetting(this, "Тип предмета", HIDDEN_SETTING);
   private final ModeSetting.Value typePickaxes = new ModeSetting.Value(this.targetItemType, "Кирки");
   private final ModeSetting.Value typeSwords = new ModeSetting.Value(this.targetItemType, "Мечи");
   private final ModeSetting.Value typeBooks = new ModeSetting.Value(this.targetItemType, "Книги", "", HIDDEN_SETTING);
   private final ModeSetting.Value typeBoots = new ModeSetting.Value(this.targetItemType, "Ботинки", "", HIDDEN_SETTING);
   // Целевые чары: shown in BOTH Кирки and Мечи sections (hidden in Общие);
   // individual options are filtered by section below.
   private final ModeSetting targetEnchant = new ModeSetting(this, "Целевые чары", HIDDEN_SETTING);
   private final ModeSetting.Value enchantMagnet = new ModeSetting.Value(this.targetEnchant, "Магнит", "", HIDDEN_SETTING);
   private final ModeSetting.Value enchantBulldozer = new ModeSetting.Value(this.targetEnchant, "Бульдозер", "", () -> !this.section.is(this.secPickaxe));
   private final ModeSetting.Value enchantWaterWalk = new ModeSetting.Value(this.targetEnchant, "Подводная ходьба", "", HIDDEN_SETTING);
   private final ModeSetting.Value enchantDetection = new ModeSetting.Value(this.targetEnchant, "Детекция", "", HIDDEN_SETTING);
   private final ModeSetting.Value enchantPoison = new ModeSetting.Value(this.targetEnchant, "Яд", "", () -> !this.section.is(this.secSword));
   private final ModeSetting.Value enchantVampirism = new ModeSetting.Value(this.targetEnchant, "Вампиризм", "", HIDDEN_SETTING);
   private final ModeSetting.Value enchantOxidation = new ModeSetting.Value(this.targetEnchant, "Окисление", "", HIDDEN_SETTING);
   private final SliderSetting actionDelay = new SliderSetting(this, "Задержка действий", HIDDEN_SETTING).min(1.0F).max(2000.0F).step(1.0F).currentValue(50.0F).suffix("ms");
   private final SliderSetting searchTimeoutMs = new SliderSetting(this, "Таймаут поиска", HIDDEN_SETTING)
      .min(1000.0F)
      .max(20000.0F)
      .step(500.0F)
      .currentValue(6000.0F)
      .suffix("ms");
   private final SliderSetting priceCacheSeconds = new SliderSetting(this, "Кэш цен", HIDDEN_SETTING).min(0.0F).max(600.0F).step(5.0F).currentValue(30.0F).suffix("s");
   private final BooleanSetting sellEnabled = new BooleanSetting(this, "Автопродажа", HIDDEN_SETTING).enabled(true);
   private final ModeSetting sellPriceMode = new ModeSetting(this, "Режим цены", HIDDEN_SETTING);
   private final ModeSetting.Value priceMarket = new ModeSetting.Value(this.sellPriceMode, "Маркет");
   private final ModeSetting.Value priceFixed = new ModeSetting.Value(this.sellPriceMode, "Фикс");
   private final SliderSetting marketPercent = new SliderSetting(this, "Процент от рынка", HIDDEN_SETTING)
      .min(1.0F)
      .max(300.0F)
      .step(1.0F)
      .currentValue(100.0F)
      .suffix("%");
   private final SliderSetting sellFixedPrice = new SliderSetting(this, "Цена продажи", HIDDEN_SETTING)
      .min(1.0F)
      .max(1000000.0F)
      .step(1.0F)
      .currentValue(100000.0F);
   private final SliderSetting minProfit;
   private final SliderSetting minXpPriceStack;
   private final SliderSetting maxXpPriceStack;
   private final SliderSetting countFarm;
   private final SliderSetting targetXpLevel;
   private final SliderSetting lapisPerEnchant;
   private final SliderSetting tableRange;
   private final SliderSetting grindstoneRange;
   private final SliderSetting craftRange;
   private final SliderSetting anvilRange;
   private final SliderSetting smithRange;
   private final SliderSetting maxBuyAttempts;
   private final BooleanSetting autoEatInModule;
   private final SliderSetting autoEatThreshold;
   private final StringSetting xpSearchName;
   private final StringSetting woodSearchName;
   private final BooleanSetting preferStickBuy;
   private final StringSetting anarchyNumber;
   private final BooleanSetting accountQueueEnabled;
   private final ModeSetting accountMode;
   private final ModeSetting.Value accountModeMain;
   private final ModeSetting.Value accountModeTwink;
   private final SliderSetting accountQueuePort;
   private final StringSetting accountQueueName;
   private final StringSetting sellFixedPriceInput;
   private final StringSetting maxXpPriceStackInput;
   private final StringSetting maxLapisPriceInput;
   private final StringSetting maxDiamondPriceInput;
   private final StringSetting maxWoodPriceInput;
   private final StringSetting maxNetheritePriceInput;
   private final StringSetting maxSharpnessSwordPriceInput;
   private final StringSetting maxEnchantSwordPriceInput;
   private final BooleanSetting anarchyRelogEnabled;
   private final StringSetting anarchyNumberInput;
   private final BooleanSetting periodicAntiAfkEnabled;
   private final BooleanSetting autoReconnectEnabled;
   private AutoEnchanter.State state;
   private AutoEnchanter.CraftStage craftStage;
   private final Timer actionTimer;
   private final Timer searchTimer;
   private final Timer buyTimer;
   private final Timer craftTimer;
   private final Timer enchantTimer;
   private final Timer sellTimer;
   private final Timer afkTimer;
   private final Timer an18Timer;
   private final Timer periodicAfkTimer;
   private final Timer reconnectTimer;
   private final Timer relistFlowTimer;
   private final Timer auctionStuckTimer;
   private long buyScreenOpenedAtMs;
   private long searchScreenOpenedAtMs;
   private final List<AutoEnchanter.PriceRequest> priceRequests;
   private int priceIndex;
   private final Map<String, AutoEnchanter.PriceCache> priceCache;
   private final Map<String, Long> unitPrices;
   private final List<AutoEnchanter.BuyRequest> buyRequests;
   private int buyIndex;
   private int buyAttempts;
   private long buyConfirmOpenedAtMs;
   private boolean buyBlocked;
   private boolean lastBuySuccess;
   private boolean lastBuyFailed;
   private boolean searchFailed;
   private long buyNavigationCooldownUntilMs;
   private long outputUnitPrice;
   private double outputPricePerQuality;
   private long craftUnitCost;
   private int craftFailTicks;
   private int dropSlot;
   private int pendingEnchantOption;
   private boolean afkRetryActive;
   private String afkRetryCommand;
   private int afkMoveStep;
   private long afkMoveStepAtMs;
   private int afkMoveDir;
   private int afkMouseStep;
   private float afkBaseYaw;
   private float afkBasePitch;
   private float afkMouseDeltaYaw;
   private float afkMouseDeltaPitch;
   private String lastSentCommand;
   private boolean relistPending;
   private long relistNextAtMs;
   private boolean relistEscPending;
   private long relistEscAtMs;
   private long relistEscUntilMs;
   private boolean buyClosePending;
   private long buyCloseAtMs;
   private long buyCloseUntilMs;
   private boolean auctionStuckActive;
   private String auctionStuckTitle;
   private boolean isAnarchyBossBarActive;
   private boolean sellCommandQueued;
   private long sellHoldUntilMs;
   private long pendingSellPrice;
   private long totalEarned;
   private long totalSpent;
   private long lastSalePrice;
   private long lastBuyPrice;
   private String lastBuyRecordText;
   private long lastBuyRecordAtMs;
   private int salesCount;
   private int buysCount;
   private int anvilLeftSlot;
   private int anvilRightSlot;
   private long anvilScreenOpenedAtMs;
   private long smithScreenOpenedAtMs;
   private class_2338 pendingOpenBlockPos;
   private AutoEnchanter.State pendingOpenState;
   private int pendingOpenAttempts;
   private long pendingOpenNextAttemptAtMs;
   private long lastRotationFrameTimeNs;
   private boolean renderRotationActive;
   private float renderTargetYaw;
   private float renderTargetPitch;
   private boolean renderRotationJitter;
   private boolean autoEatActive;
   private int autoEatPrevSlot;
   private int autoEatHotbarSlot;
   private long autoEatStartAtMs;
   private boolean periodicAfkActive;
   private long periodicAfkUntilMs;
   private class_642 lastServerInfo;
   private AutoEnchanter.QueueCoordinatorServer queueServer;
   private AutoEnchanter.QueueCoordinatorClient queueClient;
   private boolean buyTurnActive;
   private boolean buyTurnRequested;
   private boolean buyTurnGranted;
   private String queueAccountId;
   private long queueServerStartRetryAtMs;
   private final EventListener<ChatReceiveEvent> onChatReceive;
   private final EventListener<GameTickEvent> onTick;
   private final EventListener<HudRenderEvent> onRender;
   private static final long SELL_ACTION_DELAY_MS = 200L;
   private final SliderSetting maxSharpnessSwordPrice;
   private final SliderSetting maxEnchantSwordPrice;

   public AutoEnchanter() {
      this.priceFixed.select();
      this.sellFixedPrice.max(1.0E7F);
      this.sellFixedPriceInput = new StringSetting(this, "Цена продажи", () -> !this.section.is(this.secGeneral)).text("100000");
      this.minProfit = new SliderSetting(this, "Минимальная прибыль", HIDDEN_SETTING).min(0.0F).max(1000000.0F).step(1.0F).currentValue(0.0F);
      this.minXpPriceStack = new SliderSetting(this, "Мин. цена XP за стак", HIDDEN_SETTING).min(1.0F).max(500000.0F).step(1.0F).currentValue(1.0F);
      this.maxXpPriceStack = new SliderSetting(this, "Макс. XP / стак", HIDDEN_SETTING).min(1.0F).max(7000000.0F).step(1.0F).currentValue(50000.0F);
      this.maxXpPriceStack.max(1.5E7F);
      this.maxXpPriceStackInput = new StringSetting(this, "Макс. XP / стак", () -> !this.section.is(this.secItems)).text("50000");
      this.maxLapisPriceInput = new StringSetting(this, "Лазурит до", () -> !this.section.is(this.secItems)).text("100000");
      this.maxDiamondPriceInput = new StringSetting(this, "Алмаз до", () -> !this.section.is(this.secItems)).text("100000000");
      this.maxWoodPriceInput = new StringSetting(this, "Дерево до", () -> !this.section.is(this.secItems)).text("100000000");
      this.maxNetheritePriceInput = new StringSetting(this, "Незерит до", () -> !this.section.is(this.secItems)).text("100000000");
      this.maxSharpnessSwordPrice = new SliderSetting(this, "Острота 7 до", HIDDEN_SETTING).min(1.0F).max(1.0E8F).step(1.0F).currentValue(1000000.0F);
      this.maxEnchantSwordPrice = new SliderSetting(this, "Меч под яд до", HIDDEN_SETTING).min(1.0F).max(1.0E8F).step(1.0F).currentValue(100000.0F);
      this.maxSharpnessSwordPriceInput = new StringSetting(this, "Острота 7 до", () -> !this.section.is(this.secSword)).text("1000000");
      this.maxEnchantSwordPriceInput = new StringSetting(this, "Меч под яд до", () -> !this.section.is(this.secSword)).text("100000");
      this.countFarm = new SliderSetting(this, "Цель фарма", () -> !this.section.is(this.secGeneral)).min(1.0F).max(20.0F).step(1.0F).currentValue(1.0F);
      this.targetXpLevel = new SliderSetting(this, "Целевой уровень XP", HIDDEN_SETTING).min(1.0F).max(100.0F).step(1.0F).currentValue(30.0F);
      this.lapisPerEnchant = new SliderSetting(this, "Лазурит на чарку", HIDDEN_SETTING).min(1.0F).max(64.0F).step(1.0F).currentValue(3.0F);
      this.tableRange = new SliderSetting(this, "Радиус стола чар", HIDDEN_SETTING).min(1.0F).max(8.0F).step(1.0F).currentValue(4.0F);
      this.grindstoneRange = new SliderSetting(this, "Радиус точила", HIDDEN_SETTING).min(1.0F).max(8.0F).step(1.0F).currentValue(4.0F);
      this.craftRange = new SliderSetting(this, "Радиус верстака", HIDDEN_SETTING).min(1.0F).max(8.0F).step(1.0F).currentValue(4.0F);
      this.anvilRange = new SliderSetting(this, "Радиус наковальни", HIDDEN_SETTING).min(1.0F).max(8.0F).step(1.0F).currentValue(4.0F);
      this.smithRange = new SliderSetting(this, "Радиус кузницы", HIDDEN_SETTING).min(1.0F).max(8.0F).step(1.0F).currentValue(4.0F);
      this.maxBuyAttempts = new SliderSetting(this, "Макс. попыток покупки", HIDDEN_SETTING).min(1.0F).max(512.0F).step(1.0F).currentValue(96.0F);
      this.autoEatInModule = new BooleanSetting(this, "Автоеда в модуле", HIDDEN_SETTING);
      this.autoEatThreshold = new SliderSetting(this, "Порог голода", HIDDEN_SETTING).min(1.0F).max(20.0F).step(1.0F).currentValue(18.0F);
      this.xpSearchName = null;
      this.woodSearchName = null;
      this.preferStickBuy = new BooleanSetting(this, "Приоритет покупки палок", HIDDEN_SETTING);
      this.anarchyNumber = null;
      this.accountQueueEnabled = new BooleanSetting(this, "Очередь", () -> !this.section.is(this.secAccounts)).enabled(false);
      this.accountMode = new ModeSetting(this, "Роль", () -> !this.section.is(this.secAccounts) || !this.accountQueueEnabled.isEnabled());
      this.accountModeMain = new ModeSetting.Value(this.accountMode, "Основной");
      this.accountModeTwink = new ModeSetting.Value(this.accountMode, "Твинк");
      this.accountQueuePort = new SliderSetting(this, "Порт очереди", HIDDEN_SETTING).min(1025.0F).max(65535.0F).step(1.0F).currentValue(29876.0F);
      this.accountQueueName = null;
      this.anarchyRelogEnabled = new BooleanSetting(this, "Перезаход /an", () -> !this.section.is(this.secAccounts)).enabled(false);
      this.anarchyNumberInput = new StringSetting(this, "Анархия", () -> !this.section.is(this.secAccounts) || !this.anarchyRelogEnabled.isEnabled()).text("214");
      this.periodicAntiAfkEnabled = new BooleanSetting(this, "Анти-AFK", () -> !this.section.is(this.secAccounts)).enabled(true);
      this.autoReconnectEnabled = new BooleanSetting(this, "Авто reconnect", () -> !this.section.is(this.secAccounts)).enabled(false);
      this.setState(AutoEnchanter.State.IDLE);
      this.setCraftStage(AutoEnchanter.CraftStage.NONE);
      this.actionTimer = new Timer();
      this.searchTimer = new Timer();
      this.buyTimer = new Timer();
      this.craftTimer = new Timer();
      this.enchantTimer = new Timer();
      this.sellTimer = new Timer();
      this.afkTimer = new Timer();
      this.an18Timer = new Timer();
      this.periodicAfkTimer = new Timer();
      this.reconnectTimer = new Timer();
      this.relistFlowTimer = new Timer();
      this.auctionStuckTimer = new Timer();
      this.buyScreenOpenedAtMs = 0L;
      this.searchScreenOpenedAtMs = 0L;
      this.priceRequests = new ArrayList<>();
      this.priceIndex = 0;
      this.priceCache = new HashMap<>();
      this.unitPrices = new HashMap<>();
      this.buyRequests = new ArrayList<>();
      this.buyIndex = 0;
      this.buyAttempts = 0;
      this.buyConfirmOpenedAtMs = 0L;
      this.buyBlocked = false;
      this.lastBuySuccess = false;
      this.lastBuyFailed = false;
      this.searchFailed = false;
      this.buyNavigationCooldownUntilMs = 0L;
      this.outputUnitPrice = -1L;
      this.outputPricePerQuality = -1.0;
      this.craftUnitCost = -1L;
      this.craftFailTicks = 0;
      this.dropSlot = -1;
      this.pendingEnchantOption = 2;
      this.afkRetryActive = false;
      this.afkRetryCommand = "";
      this.afkMoveStep = 0;
      this.afkMoveStepAtMs = 0L;
      this.afkMoveDir = -1;
      this.afkMouseStep = 0;
      this.afkBaseYaw = 0.0F;
      this.afkBasePitch = 0.0F;
      this.afkMouseDeltaYaw = 0.0F;
      this.afkMouseDeltaPitch = 0.0F;
      this.lastSentCommand = "";
      this.relistPending = false;
      this.relistNextAtMs = 0L;
      this.relistEscPending = false;
      this.relistEscAtMs = 0L;
      this.relistEscUntilMs = 0L;
      this.buyClosePending = false;
      this.buyCloseAtMs = 0L;
      this.buyCloseUntilMs = 0L;
      this.auctionStuckActive = false;
      this.auctionStuckTitle = "";
      this.isAnarchyBossBarActive = false;
      this.sellCommandQueued = false;
      this.sellHoldUntilMs = 0L;
      this.pendingSellPrice = -1L;
      this.totalEarned = 0L;
      this.totalSpent = 0L;
      this.lastSalePrice = 0L;
      this.lastBuyPrice = 0L;
      this.lastBuyRecordText = "";
      this.lastBuyRecordAtMs = 0L;
      this.salesCount = 0;
      this.buysCount = 0;
      this.anvilLeftSlot = -1;
      this.anvilRightSlot = -1;
      this.anvilScreenOpenedAtMs = 0L;
      this.smithScreenOpenedAtMs = 0L;
      this.pendingOpenBlockPos = null;
      this.pendingOpenState = null;
      this.pendingOpenAttempts = 0;
      this.pendingOpenNextAttemptAtMs = 0L;
      this.lastRotationFrameTimeNs = 0L;
      this.renderRotationActive = false;
      this.renderTargetYaw = 0.0F;
      this.renderTargetPitch = 0.0F;
      this.renderRotationJitter = false;
      this.autoEatActive = false;
      this.autoEatPrevSlot = -1;
      this.autoEatHotbarSlot = -1;
      this.autoEatStartAtMs = 0L;
      this.periodicAfkActive = false;
      this.periodicAfkUntilMs = 0L;
      this.lastServerInfo = null;
      this.queueServer = null;
      this.queueClient = null;
      this.buyTurnActive = false;
      this.buyTurnRequested = false;
      this.buyTurnGranted = false;
      this.queueAccountId = "";
      this.queueServerStartRetryAtMs = 0L;
      this.onChatReceive = var1 -> {
         if (mc.field_1724 != null) {
            String var2 = var1.getText();
            if (var2 != null && !var2.isBlank()) {
               String var3 = AutoEnchanterText.stripFormatting(var2).toLowerCase(Locale.ROOT);
               String var4 = AutoEnchanterText.normalizeLettersOnly(var3);
               String cls = "other";
               if (this.isAfkBlockedMessage(var3, var2)) {
                  cls = "afk_blocked";
                  this.startAfkRetry(this.lastSentCommand);
               } else if (this.isStorageFullMessage(var4)) {
                  cls = "storage_full";
                  this.startRelistCycle();
               } else if (this.isSoldMessage(var4)) {
                  cls = "sold";
                  this.recordSale(var3);
                  this.stopRelistCycle();
               } else {
                  if (var4.contains(AutoEnchanterText.normalizeLettersOnly("такого предмета не существует"))
                     || var4.contains(AutoEnchanterText.normalizeLettersOnly("item does not exist"))
                     || var4.contains(AutoEnchanterText.normalizeLettersOnly("no such item"))) {
                     cls = "search_failed";
                     this.searchFailed = true;
                  }

                  if (var4.contains(AutoEnchanterText.normalizeLettersOnly("нет денег"))
                     || var4.contains(AutoEnchanterText.normalizeLettersOnly("недостаточно"))
                     || var4.contains(AutoEnchanterText.normalizeLettersOnly("not enough"))
                     || var4.contains(AutoEnchanterText.normalizeLettersOnly("no money"))) {
                     cls = "buy_failed_no_money";
                     this.lastBuyFailed = true;
                     this.buyBlocked = true;
                  }

                  if (var4.contains(AutoEnchanterText.normalizeLettersOnly("вы купили"))
                     || var4.contains(AutoEnchanterText.normalizeLettersOnly("\u043a\u0443\u043f\u0438\u043b\u0438"))
                     || var4.contains(AutoEnchanterText.normalizeLettersOnly("\u0443\u0441\u043f\u0435\u0448\u043d\u043e \u043a\u0443\u043f\u0438\u043b\u0438"))
                     || var4.contains(AutoEnchanterText.normalizeLettersOnly("purchased"))
                     || var4.contains(AutoEnchanterText.normalizeLettersOnly("you bought"))) {
                     cls = "buy_success";
                     this.recordBuy(var3);
                     this.lastBuySuccess = true;
                     this.scheduleBuyClose();
                  }
               }
               moscow.mytheria.logger.MytheriaLogger.event("chat_recv")
                  .with("class", cls)
                  .with("text", var3)
                  .with("state", this.state == null ? "null" : this.state.name())
                  .emit();
            }
         }
      };
      this.onTick = var1 -> {
         this.handleAutoReconnectTick();
         if (mc.field_1724 != null) {
            if (mc.field_1724.method_29504()) {
               this.disable();
               return;
            }

            if (mc.field_1724.method_6032() <= 0.0F) {
               this.disable();
               return;
            }
         }

         if (mc.field_1724 != null && !this.afkRetryActive && this.afkTimer.finished(30000L)) {
            this.afkBaseYaw = mc.field_1724.method_36454();
            this.afkBasePitch = mc.field_1724.method_36455();
            this.afkMouseStep = 0;
            this.handleAfkMouseMove();
            this.afkTimer.reset();
         }

         if (mc.field_1724 != null && mc.field_1687 != null) {
            this.handleAccountQueueTick();
            this.handleAn18();
            this.handlePeriodicAnarchyRelog();
            if (!this.isAnarchyBossBarActive) {
               if (this.afkRetryActive) {
                  if (this.buyTurnActive || this.buyTurnRequested || this.buyTurnGranted) {
                     this.releaseBuyTurn();
                  }

                  this.handleAfkRetry();
               } else if (this.handlePeriodicAntiAfk()) {
                  if (this.buyTurnActive || this.buyTurnRequested || this.buyTurnGranted) {
                     this.releaseBuyTurn();
                  }
               } else if (this.handleAutoEat()) {
                  if (this.buyTurnActive || this.buyTurnRequested || this.buyTurnGranted) {
                     this.releaseBuyTurn();
                  }
               } else {
                  this.handleAuctionStuckCheck();
                  this.handleOpenBlockRetry();
                  if (!this.isBuyState() && (this.buyTurnActive || this.buyTurnRequested || this.buyTurnGranted)) {
                     this.releaseBuyTurn();
                  }

                  if (this.isRelistState()) {
                     this.handleRelistFlow();
                  } else {
                     this.handleRelistEscape();
                     this.handleBuyClose();
                     if (this.state == AutoEnchanter.State.IDLE && this.relistPending && this.shouldStartRelist() && mc.field_1755 == null) {
                        this.setState(AutoEnchanter.State.RELIST_OPEN_AH);
                        this.relistFlowTimer.reset();
                     } else {
                        switch (this.state) {
                           case IDLE:
                              this.handleIdle();
                              break;
                           case PRICE_SEND:
                              this.handlePriceSend();
                              break;
                           case PRICE_WAIT:
                              this.handlePriceWait();
                              break;
                           case DECIDE:
                              this.handleDecide();
                              break;
                           case BUY_SEND:
                              this.handleBuySend();
                              break;
                           case BUY_WAIT_SCREEN:
                              this.handleBuyWaitScreen();
                              break;
                           case BUY_CLICK:
                              this.handleBuyClick();
                              break;
                           case BUY_WAIT_RESULT:
                              this.handleBuyWaitResult();
                              break;
                           case USE_XP:
                              this.handleUseXp();
                              break;
                           case CRAFT_OPEN:
                              this.handleCraftOpen();
                              break;
                           case CRAFT_WAIT:
                              this.handleCraftWait();
                              break;
                           case CRAFTING:
                              this.handleCrafting();
                              break;
                           case ENCHANT_OPEN:
                              this.handleEnchantOpen();
                              break;
                           case ENCHANT_WAIT:
                              this.handleEnchantWait();
                              break;
                           case ENCHANT_PLACE:
                              this.handleEnchantPlace();
                              break;
                           case ENCHANT_CLICK:
                              this.handleEnchantClick();
                              break;
                           case ENCHANT_WAIT_RESULT:
                              this.handleEnchantWaitResult();
                              break;
                           case EVALUATE:
                              this.handleEvaluate();
                              break;
                           case GRIND_OPEN:
                              this.handleGrindOpen();
                              break;
                           case GRIND_WAIT:
                              this.handleGrindWait();
                              break;
                           case GRIND_PLACE:
                              this.handleGrindPlace();
                              break;
                           case GRIND_TAKE:
                              this.handleGrindTake();
                              break;
                           case ANVIL_OPEN:
                              this.handleAnvilOpen();
                              break;
                           case ANVIL_WAIT:
                              this.handleAnvilWait();
                              break;
                           case ANVIL_PLACE:
                              this.handleAnvilPlace();
                              break;
                           case ANVIL_COMBINE:
                              this.handleAnvilCombine();
                              break;
                           case ANVIL_TAKE:
                              this.handleAnvilTake();
                              break;
                           case SMITH_OPEN:
                              this.handleSmithOpen();
                              break;
                           case SMITH_WAIT:
                              this.handleSmithWait();
                              break;
                           case SMITH_PLACE:
                              this.handleSmithPlace();
                              break;
                           case SMITH_TAKE:
                              this.handleSmithTake();
                              break;
                           case SELL_SEND:
                              this.handleSellSend();
                              break;
                           case SELL_WAIT:
                              this.handleSellWait();
                              break;
                           case DROP_BAD:
                              this.handleDropBad();
                        }
                     }
                  }
               }
            }
         }
      };
      this.onRender = var1 -> {
         if (mc.field_1724 != null && mc.field_1687 != null) {
            this.updateRenderRotation();
            this.renderProfitHud(var1);
         }
      };
   }

   @Override
   public boolean isHidden() {
      return !SecretModeManager.getInstance().isSecretModeEnabled();
   }

   @Override
   public void onEnable() {
      moscow.mytheria.logger.MytheriaLogger.log("module_enable", "AutoEnchanter");
      super.onEnable();
      this.resetState();
      this.startAccountQueue();
      if (this.shouldPriceCheck()) {
         this.schedulePriceCheck();
      }
   }

   @Override
   public void onDisable() {
      moscow.mytheria.logger.MytheriaLogger.log("module_disable", "AutoEnchanter");
      this.releaseBuyTurn();
      this.stopAccountQueue();
      this.resetState();
      super.onDisable();
   }

   private void resetState() {
      this.setState(AutoEnchanter.State.IDLE);
      this.setCraftStage(AutoEnchanter.CraftStage.NONE);
      this.actionTimer.reset();
      this.searchTimer.reset();
      this.buyTimer.reset();
      this.craftTimer.reset();
      this.enchantTimer.reset();
      this.sellTimer.reset();
      this.afkTimer.reset();
      this.an18Timer.reset();
      this.periodicAfkTimer.reset();
      this.reconnectTimer.reset();
      this.relistFlowTimer.reset();
      this.auctionStuckTimer.reset();
      this.priceRequests.clear();
      this.priceCache.clear();
      this.unitPrices.clear();
      this.priceIndex = 0;
      this.buyRequests.clear();
      this.buyIndex = 0;
      this.buyAttempts = 0;
      this.buyConfirmOpenedAtMs = 0L;
      this.buyBlocked = false;
      this.lastBuySuccess = false;
      this.lastBuyFailed = false;
      this.searchFailed = false;
      this.searchScreenOpenedAtMs = 0L;
      this.outputUnitPrice = -1L;
      this.outputPricePerQuality = -1.0;
      this.craftUnitCost = -1L;
      this.craftFailTicks = 0;
      this.dropSlot = -1;
      this.pendingEnchantOption = 2;
      this.afkRetryActive = false;
      this.afkRetryCommand = "";
      this.afkMoveStep = 0;
      this.afkMoveStepAtMs = 0L;
      this.afkMoveDir = -1;
      this.afkMouseStep = 0;
      this.afkBaseYaw = 0.0F;
      this.afkBasePitch = 0.0F;
      this.afkMouseDeltaYaw = 0.0F;
      this.afkMouseDeltaPitch = 0.0F;
      this.lastSentCommand = "";
      this.relistPending = false;
      this.relistNextAtMs = 0L;
      this.relistEscPending = false;
      this.relistEscAtMs = 0L;
      this.relistEscUntilMs = 0L;
      this.buyClosePending = false;
      this.buyCloseAtMs = 0L;
      this.buyCloseUntilMs = 0L;
      this.auctionStuckActive = false;
      this.auctionStuckTitle = "";
      this.sellCommandQueued = false;
      this.sellHoldUntilMs = 0L;
      this.pendingSellPrice = -1L;
      this.anvilLeftSlot = -1;
      this.anvilRightSlot = -1;
      this.anvilScreenOpenedAtMs = 0L;
      this.smithScreenOpenedAtMs = 0L;
      this.lastRotationFrameTimeNs = 0L;
      this.renderRotationActive = false;
      this.renderTargetYaw = 0.0F;
      this.renderTargetPitch = 0.0F;
      this.renderRotationJitter = false;
      this.autoEatActive = false;
      this.autoEatPrevSlot = -1;
      this.autoEatHotbarSlot = -1;
      this.autoEatStartAtMs = 0L;
      this.periodicAfkActive = false;
      this.periodicAfkUntilMs = 0L;
      this.buyTurnActive = false;
      this.buyTurnRequested = false;
      this.buyTurnGranted = false;
      if (mc != null && mc.field_1690 != null) {
         mc.field_1690.field_1904.method_23481(false);
      }

      this.clearOpenBlockRetry();
   }

   private boolean isAccountQueueEnabled() {
      return this.accountQueueEnabled.isEnabled();
   }

   private boolean isMainAccountMode() {
      return this.accountMode.is(this.accountModeMain);
   }

   private int getAccountQueuePort() {
      return 29876;
   }

   private String resolveQueueAccountId() {
      String var1 = this.accountQueueName == null ? null : this.accountQueueName.getText();
      if (var1 != null && !var1.isBlank()) {
         return var1.trim();
      } else {
         if (mc.field_1724 != null) {
            String var2 = mc.field_1724.method_5477().getString();
            if (var2 != null && !var2.isBlank()) {
               return var2.trim();
            }
         }

         return "acc-" + Integer.toHexString(System.identityHashCode(this));
      }
   }

   private void startAccountQueue() {
      if (!this.isAccountQueueEnabled()) {
         this.stopAccountQueue();
      } else {
         this.queueAccountId = this.resolveQueueAccountId();
         int var1 = this.getAccountQueuePort();
         if (!this.isMainAccountMode() && this.queueServer != null) {
            this.queueServer.stop();
            this.queueServer = null;
         }

         if (this.isMainAccountMode() && this.queueServer != null && !this.queueServer.isForPort(var1)) {
            this.queueServer.stop();
            this.queueServer = null;
         }

         if (this.isMainAccountMode() && (this.queueServer == null || !this.queueServer.isRunning()) && System.currentTimeMillis() >= this.queueServerStartRetryAtMs) {
            this.queueServerStartRetryAtMs = System.currentTimeMillis() + 5000L;
            this.queueServer = new AutoEnchanter.QueueCoordinatorServer(var1);
            this.queueServer.start();
            MessageUtility.info(class_2561.method_43470("[AutoEnchanter] Локальный сервер очереди запущен на порту " + var1));
         }

         if (this.queueClient == null || !this.queueClient.isRunning() || !this.queueClient.isFor(this.queueAccountId, var1, this.isMainAccountMode())) {
            if (this.queueClient != null) {
               this.queueClient.stop();
            }

            this.queueClient = new AutoEnchanter.QueueCoordinatorClient("127.0.0.1", var1, this.queueAccountId, this.isMainAccountMode());
            this.queueClient.start();
         }
      }
   }

   private void stopAccountQueue() {
      if (this.queueClient != null) {
         this.queueClient.stop();
         this.queueClient = null;
      }

      if (this.queueServer != null) {
         this.queueServer.stop();
         this.queueServer = null;
      }

      this.buyTurnActive = false;
      this.buyTurnRequested = false;
      this.buyTurnGranted = false;
   }

   private void handleAccountQueueTick() {
      if (this.isAccountQueueEnabled()) {
         this.startAccountQueue();
         if (this.queueClient != null && this.queueClient.consumeJustConnected()) {
            MessageUtility.info(class_2561.method_43470("[AutoEnchanter] Аккаунт подключен к локальному серверу очереди."));
         }

         if (this.isMainAccountMode() && this.queueServer != null) {
            String var1 = this.queueServer.consumeLastConnectedAccountId();
            if (var1 != null && !var1.isBlank() && !var1.equals(this.queueAccountId)) {
               MessageUtility.info(class_2561.method_43470("[AutoEnchanter] К серверу очереди подключен аккаунт: " + var1));
            }
         }

         if (this.queueClient != null) {
            this.buyTurnGranted = this.queueClient.hasTurn();
            if (!this.queueClient.isConnected()) {
               this.buyTurnGranted = false;
               this.buyTurnRequested = false;
            }
         } else {
            this.buyTurnGranted = false;
            this.buyTurnRequested = false;
         }
      } else {
         if (this.queueClient != null || this.queueServer != null || this.buyTurnRequested || this.buyTurnGranted || this.buyTurnActive) {
            this.stopAccountQueue();
         }
      }
   }

   private boolean isBuyState() {
      return this.state == AutoEnchanter.State.BUY_SEND
         || this.state == AutoEnchanter.State.BUY_WAIT_SCREEN
         || this.state == AutoEnchanter.State.BUY_CLICK
         || this.state == AutoEnchanter.State.BUY_WAIT_RESULT;
   }

   private boolean canProceedBuyTurn() {
      if (!this.isAccountQueueEnabled()) {
         return true;
      } else if (this.queueClient != null && this.queueClient.isConnected()) {
         if (!this.buyTurnRequested) {
            this.queueClient.setWantTurn(true);
            this.buyTurnRequested = true;
         }

         this.buyTurnGranted = this.queueClient.hasTurn();
         if (this.buyTurnGranted) {
            this.buyTurnActive = true;
            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private void releaseBuyTurn() {
      if (this.queueClient != null) {
         this.queueClient.setWantTurn(false);
         this.queueClient.releaseTurn();
      }

      this.buyTurnActive = false;
      this.buyTurnRequested = false;
      this.buyTurnGranted = false;
   }

   private void handleIdle() {
      if (mc.field_1755 == null) {
         if (this.shouldPriceCheck() && this.needsPriceUpdate()) {
            this.schedulePriceCheck();
         } else if (!this.isProfitable()) {
            if (this.actionTimer.finished(this.delayMs())) {
               this.actionTimer.reset();
            }
         } else if (this.needsXp() && this.hasXpBottle()) {
            this.setState(AutoEnchanter.State.USE_XP);
            this.actionTimer.reset();
         } else {
            int var2 = this.findPickaxeSlotWithTarget();
            if (this.isSwordMode() && var2 != -1 && this.isFinalSword(mc.field_1724.method_31548().method_5438(var2))) {
               if (this.shouldUpgradeFinalSword()) {
                  if (this.countItem(class_1802.field_22020) <= 0) {
                     this.buildBuyRequests();
                     if (!this.buyRequests.isEmpty()) {
                        this.setState(AutoEnchanter.State.BUY_SEND);
                        this.actionTimer.reset();
                        return;
                     }
                  }

                  this.setState(AutoEnchanter.State.SMITH_OPEN);
                  this.actionTimer.reset();
                  return;
               }

               if (!this.relistPending) {
                  this.setState(AutoEnchanter.State.SELL_SEND);
                  this.sellTimer.reset();
                  return;
               }
            } else if (!this.isSwordMode() && !this.relistPending && var2 != -1) {
               this.setState(AutoEnchanter.State.SELL_SEND);
               this.sellTimer.reset();
               return;
            }

            int var1;
            if ((var1 = this.findPickaxeSlotWithBad()) != -1) {
               class_1799 var3 = mc.field_1724.method_31548().method_5438(var1);
               if (this.isSwordMode() && this.hasKnockbackEnchant(var3)) {
                  this.setState(AutoEnchanter.State.GRIND_OPEN);
                  this.actionTimer.reset();
               } else {
                  this.dropSlot = var1;
                  this.setState(AutoEnchanter.State.DROP_BAD);
                  this.actionTimer.reset();
               }
            } else {
               boolean var5 = this.relistPending || false;
               if (this.isSwordMode() && this.hasAnvilCandidates()) {
                  this.setState(AutoEnchanter.State.ANVIL_OPEN);
                  this.actionTimer.reset();
                  return;
               }

               int var4 = this.findPickaxeSlotWithAnyEnchant(var5);
               if (var4 != -1) {
                  this.setState(AutoEnchanter.State.GRIND_OPEN);
                  this.actionTimer.reset();
               } else if (this.needsMaterials()) {
                  this.buildBuyRequests();
                  if (!this.buyRequests.isEmpty()) {
                     this.setState(AutoEnchanter.State.BUY_SEND);
                     this.actionTimer.reset();
                  }
               } else if (this.needsCraft()) {
                  this.setState(AutoEnchanter.State.CRAFT_OPEN);
                  this.actionTimer.reset();
               } else if (this.hasUnenchantedPickaxe()) {
                  this.setState(AutoEnchanter.State.ENCHANT_OPEN);
                  this.actionTimer.reset();
               }
            }
         }
      }
   }

   private void handlePriceSend() {
      if (this.priceIndex >= this.priceRequests.size()) {
         this.setState(AutoEnchanter.State.DECIDE);
      } else {
         AutoEnchanter.PriceRequest var1 = this.priceRequests.get(this.priceIndex);
         AutoEnchanter.PriceCache var2 = this.priceCache.get(var1.key);
         if (var2 != null && var2.isValid()) {
            this.unitPrices.put(var1.key, var2.price);
            if (var1.output) {
               this.outputUnitPrice = var2.price;
               this.outputPricePerQuality = var2.perQuality > 0.0 ? var2.perQuality : -1.0;
            }

            this.priceIndex++;
         } else if (mc.field_1755 == null && this.actionTimer.finished(this.buyDelayMs())) {
            this.searchFailed = false;
            this.searchScreenOpenedAtMs = 0L;
            this.sendChat("/ah search " + this.sanitizeSearchName(var1.query, var1.keepDigits));
            this.searchTimer.reset();
            this.setState(AutoEnchanter.State.PRICE_WAIT);
         }
      }
   }

   private void handlePriceWait() {
      if (this.priceIndex >= this.priceRequests.size()) {
         this.setState(AutoEnchanter.State.DECIDE);
      } else if (!this.searchFailed) {
         if (mc.field_1755 instanceof class_465 var2) {
            class_1703 var3 = var2.method_17577();
            if (!var3.method_34255().method_7960()) {
               this.clearCursorToInventory(var3);
               return;
            }

            if (!this.isSearchScreen()) {
               this.searchScreenOpenedAtMs = 0L;
               if (this.searchTimer.finished(this.timeoutMs())) {
                  this.markPriceMissing();
               }
            } else {
               if (this.searchScreenOpenedAtMs == 0L) {
                  this.searchScreenOpenedAtMs = System.currentTimeMillis();
               }

               if (System.currentTimeMillis() - this.searchScreenOpenedAtMs >= 300L && this.searchTimer.finished(this.delayMs())) {
                  AutoEnchanter.PriceRequest var4 = this.priceRequests.get(this.priceIndex);
                  long var5 = var4.output
                     ? (this.isPickaxeMode() ? this.findCheapestTargetPickaxePrice(var4.query) : this.findCheapestUnitPrice(var4.query))
                     : this.findCheapestUnitPrice(var4.query);
                  this.unitPrices.put(var4.key, var5);
                  if (var5 > 0L) {
                     this.priceCache
                        .put(
                           var4.key,
                           new AutoEnchanter.PriceCache(var5, System.currentTimeMillis() + this.cacheMs(), var4.output ? this.outputPricePerQuality : -1.0)
                        );
                  }

                  if (this.closeHandledScreenSafely()) {
                     this.priceIndex++;
                     this.setState(AutoEnchanter.State.PRICE_SEND);
                     this.actionTimer.reset();
                  }
               }
            }
         } else {
            this.searchScreenOpenedAtMs = 0L;
            if (this.searchTimer.finished(this.timeoutMs())) {
               this.markPriceMissing();
            }
         }
      } else {
         this.searchScreenOpenedAtMs = 0L;
         if (mc.field_1755 == null || this.closeHandledScreenSafely()) {
            this.markPriceMissing();
         }
      }
   }

   private void markPriceMissing() {
      this.unitPrices.put(this.priceRequests.get(this.priceIndex).key, -1L);
      this.priceIndex++;
      this.setState(AutoEnchanter.State.PRICE_SEND);
      this.actionTimer.reset();
   }

   private void handleDecide() {
      this.craftUnitCost = this.computeCraftUnitCost();
      long var1 = this.getPlannedSellPrice();
      if (false && var1 > 0L && this.craftUnitCost > 0L && var1 - this.craftUnitCost < Math.round(this.minProfit.getCurrentValue())) {
         MessageUtility.info(class_2561.method_43470("[AutoEnchanter] Not profitable"));
         this.setState(AutoEnchanter.State.IDLE);
         this.actionTimer.reset();
      } else {
         this.setState(AutoEnchanter.State.IDLE);
         this.actionTimer.reset();
      }
   }

   private void handleBuySend() {
      if (this.buyIndex >= this.buyRequests.size()) {
         if (this.needsMaterials()) {
            this.buildBuyRequests();
            if (!this.buyRequests.isEmpty()) {
               this.setState(AutoEnchanter.State.BUY_SEND);
               this.actionTimer.reset();
               return;
            }
         }

         this.releaseBuyTurn();
         this.setState(AutoEnchanter.State.IDLE);
      } else {
         if (!this.canProceedBuyTurn()) {
            return;
         }

         AutoEnchanter.BuyRequest var1 = this.buyRequests.get(this.buyIndex);
         if (this.isBuySatisfied(var1)) {
            this.buyIndex++;
         } else if (mc.field_1755 != null) {
            if (this.tryConfirmPurchase()) {
               this.buyTimer.reset();
               this.buyConfirmOpenedAtMs = System.currentTimeMillis();
               this.setState(AutoEnchanter.State.BUY_WAIT_RESULT);
            } else {
               this.closeHandledScreenSafely();
            }
         } else if (this.actionTimer.finished(this.delayMs())) {
            this.buyScreenOpenedAtMs = 0L;
            this.buyConfirmOpenedAtMs = 0L;
            this.lastBuySuccess = false;
            this.lastBuyFailed = false;
            this.searchFailed = false;
            this.buyNavigationCooldownUntilMs = 0L;
            String searchQuery = this.sanitizeSearchName(var1.query, var1.keepDigits);
            moscow.mytheria.logger.MytheriaLogger.event("buy_search")
               .with("query", searchQuery)
               .with("req_key", var1.key)
               .with("req_query", var1.query)
               .with("target_count", var1.targetCount)
               .with("buy_index", this.buyIndex)
               .emit();
            this.sendChat("/ah search " + searchQuery);
            this.searchTimer.reset();
            this.setState(AutoEnchanter.State.BUY_WAIT_SCREEN);
         }
      }
   }

   private void handleBuyWaitScreen() {
      if (mc.field_1755 instanceof class_465 var2) {
         class_1703 var3 = var2.method_17577();
         if (!var3.method_34255().method_7960()) {
            this.clearCursorToInventory(var3);
         } else if (!this.isSearchScreen()) {
            if (this.tryConfirmPurchase()) {
               this.buyTimer.reset();
               this.buyConfirmOpenedAtMs = System.currentTimeMillis();
               this.setState(AutoEnchanter.State.BUY_WAIT_RESULT);
            } else if (this.searchTimer.finished(this.timeoutMs())) {
               this.buyScreenOpenedAtMs = 0L;
               this.buyIndex++;
               this.setState(AutoEnchanter.State.BUY_SEND);
               this.actionTimer.reset();
            } else {
               this.closeHandledScreenSafely();
            }
         } else {
            if (this.buyScreenOpenedAtMs == 0L) {
               this.buyScreenOpenedAtMs = System.currentTimeMillis();
            }

            if (this.searchFailed) {
               if (this.closeHandledScreenSafely()) {
                  this.buyScreenOpenedAtMs = 0L;
                  this.buyIndex++;
                  this.setState(AutoEnchanter.State.BUY_SEND);
                  this.actionTimer.reset();
               }
            } else if (System.currentTimeMillis() - this.buyScreenOpenedAtMs >= 1500L) {
               this.setState(AutoEnchanter.State.BUY_CLICK);
            }
         }
      } else if (this.searchTimer.finished(this.timeoutMs())) {
         this.buyScreenOpenedAtMs = 0L;
         this.buyIndex++;
         this.setState(AutoEnchanter.State.BUY_SEND);
         this.actionTimer.reset();
      }
   }

   private int getContainerSlotCount(class_1703 var1) {
      for (int var2 = 0; var2 < var1.field_7761.size(); var2++) {
         class_1735 var3 = (class_1735)var1.field_7761.get(var2);
         if (var3 != null && var3.field_7871 == mc.field_1724.method_31548()) {
            return var2;
         }
      }

      return var1.field_7761.size();
   }

   private void handleBuyWaitResult() {
      class_437 var2 = mc.field_1755;
      class_465 var1;
      if (var2 instanceof class_465 && !(var1 = (class_465)var2).method_17577().method_34255().method_7960()) {
         this.clearCursorToInventory(var1.method_17577());
      } else {
         if (this.isConfirmScreen()) {
            long var3 = System.currentTimeMillis();
            if (this.buyConfirmOpenedAtMs == 0L) {
               this.buyConfirmOpenedAtMs = var3;
            }

            if (this.buyTimer.finished(this.buyDelayMs()) && this.tryConfirmPurchase()) {
               this.buyTimer.reset();
               return;
            }

            if (var3 - this.buyConfirmOpenedAtMs >= 4000L) {
               if (!this.forceConfirmPurchaseFallback()) {
                  this.closeHandledScreenSafely();
                  this.setState(AutoEnchanter.State.BUY_SEND);
                  this.actionTimer.reset();
               } else {
                  this.buyTimer.reset();
               }

               this.buyConfirmOpenedAtMs = 0L;
               return;
            }
         } else {
            this.buyConfirmOpenedAtMs = 0L;
         }

         if (this.buyTimer.finished(this.buyDelayMs())) {
            if (this.tryConfirmPurchase()) {
               this.buyTimer.reset();
            } else if (this.lastBuySuccess) {
               this.scheduleBuyClose();
               this.buyConfirmOpenedAtMs = 0L;
               this.buyAttempts = 0;
               this.buyBlocked = false;
               this.lastBuySuccess = false;
               this.lastBuyFailed = false;
               if (!this.buyRequests.isEmpty() && this.buyIndex < this.buyRequests.size()) {
                  this.buyIndex++;
               }

               this.setState(AutoEnchanter.State.BUY_SEND);
               this.actionTimer.reset();
            } else {
               if (!this.buyRequests.isEmpty() && this.buyIndex < this.buyRequests.size() && this.isBuySatisfied(this.buyRequests.get(this.buyIndex))) {
                  if (!this.closeHandledScreenSafely()) {
                     return;
                  }

                  this.buyIndex++;
                  this.buyAttempts = 0;
                  this.buyBlocked = false;
                  this.buyConfirmOpenedAtMs = 0L;
                  this.lastBuySuccess = false;
                  this.lastBuyFailed = false;
                  this.setState(AutoEnchanter.State.BUY_SEND);
                  this.actionTimer.reset();
                  return;
               }

               if (this.lastBuyFailed) {
                  this.buyBlocked = true;
               }

               this.lastBuySuccess = false;
               this.lastBuyFailed = false;
               this.setState(AutoEnchanter.State.BUY_CLICK);
            }
         }
      }
   }

   private void handleUseXp() {
      if (this.actionTimer.finished(this.delayMs())) {
         if (!this.needsXp() || !this.hasXpBottle()) {
            this.setState(AutoEnchanter.State.IDLE);
         } else if (this.useXpBottle()) {
            this.actionTimer.reset();
         } else {
            this.setState(AutoEnchanter.State.IDLE);
         }
      }
   }

   private boolean handleAutoEat() {
      if (this.isAutoEatModuleEnabled() && mc.field_1724 != null && mc.field_1761 != null) {
         int var1 = mc.field_1724.method_7344().method_7586();
         int var2 = Math.max(1, Math.round(this.autoEatThreshold.getCurrentValue()));
         boolean var3 = var1 < var2;
         if (!this.autoEatActive && !var3) {
            return false;
         } else {
            if (!this.autoEatActive) {
               int var4 = this.findFoodInventorySlot();
               if (var4 == -1) {
                  return false;
               }

               if (mc.field_1755 != null) {
                  this.closeHandledScreenSafely();
               }

               int var5 = var4;
               if (var4 > 8) {
                  var5 = this.findEmptyHotbarSlot();
                  if (var5 == -1) {
                     this.compactHotbarToInventory();
                     var5 = this.findEmptyHotbarSlot();
                  }

                  if (var5 == -1 || !this.moveSingleToHotbar(var4, var5)) {
                     return false;
                  }
               }

               this.autoEatActive = true;
               this.autoEatPrevSlot = mc.field_1724.method_31548().field_7545;
               this.autoEatHotbarSlot = var5;
               this.autoEatStartAtMs = System.currentTimeMillis();
               this.setState(AutoEnchanter.State.IDLE);
            }

            if (mc.field_1755 != null) {
               this.closeHandledScreenSafely();
            }

            if (this.autoEatHotbarSlot >= 0 && this.autoEatHotbarSlot <= 8) {
               class_1799 var6 = mc.field_1724.method_31548().method_5438(this.autoEatHotbarSlot);
               if (!this.isFoodStack(var6)) {
                  this.stopAutoEat();
                  return false;
               } else {
                  mc.field_1724.method_31548().field_7545 = this.autoEatHotbarSlot;
                  if (!mc.field_1724.method_6115()) {
                     mc.field_1761.method_2919(mc.field_1724, class_1268.field_5808);
                  }

                  mc.field_1690.field_1904.method_23481(true);
                  if (mc.field_1724.method_7344().method_7586() >= 20 || System.currentTimeMillis() - this.autoEatStartAtMs > 15000L) {
                     this.stopAutoEat();
                  }

                  return this.autoEatActive;
               }
            } else {
               this.stopAutoEat();
               return false;
            }
         }
      } else {
         if (this.autoEatActive) {
            this.stopAutoEat();
         }

         return false;
      }
   }

   private void stopAutoEat() {
      mc.field_1690.field_1904.method_23481(false);
      if (this.autoEatPrevSlot >= 0 && this.autoEatPrevSlot <= 8) {
         mc.field_1724.method_31548().field_7545 = this.autoEatPrevSlot;
      }

      this.autoEatActive = false;
      this.autoEatPrevSlot = -1;
      this.autoEatHotbarSlot = -1;
      this.autoEatStartAtMs = 0L;
      this.periodicAfkActive = false;
      this.periodicAfkUntilMs = 0L;
   }

   private int findFoodInventorySlot() {
      for (int var1 = 0; var1 < this.getAccessibleInventorySize(); var1++) {
         class_1799 var2 = mc.field_1724.method_31548().method_5438(var1);
         if (this.isFoodStack(var2)) {
            return var1;
         }
      }

      return -1;
   }

   private boolean isFoodStack(class_1799 var1) {
      return var1 != null && !var1.method_7960() && var1.method_57826(class_9334.field_50075);
   }

   private void handleCraftOpen() {
      if (!this.isPickaxeMode() && !this.isSwordMode()) {
         this.setState(AutoEnchanter.State.IDLE);
      } else {
         if (mc.field_1755 == null && this.actionTimer.finished(this.delayMs())) {
            class_2338 var1 = this.findNearestBlock(class_2246.field_9980, 4);
            if (var1 == null) {
               MessageUtility.info(class_2561.method_43470("[AutoEnchanter] Crafting table not found."));
               this.setState(AutoEnchanter.State.IDLE);
            } else {
               this.beginBlockOpen(var1, AutoEnchanter.State.CRAFT_WAIT);
               this.craftTimer.reset();
               this.setCraftStage(AutoEnchanter.CraftStage.NONE);
               this.craftFailTicks = 0;
               this.setState(AutoEnchanter.State.CRAFT_WAIT);
            }
         }
      }
   }

   private void handleCraftWait() {
      if (mc.field_1755 instanceof class_465 var2) {
         if (var2.method_17577() instanceof class_1714) {
            this.prepareCraftStage();
            this.craftTimer.reset();
            this.setState(AutoEnchanter.State.CRAFTING);
         } else if (this.craftTimer.finished(this.timeoutMs())) {
            this.setState(AutoEnchanter.State.IDLE);
         }
      } else if (this.craftTimer.finished(this.timeoutMs())) {
         this.setState(AutoEnchanter.State.IDLE);
      }
   }

   private void handleCrafting() {
      if (!(mc.field_1755 instanceof class_465 var2 && var2.method_17577() instanceof class_1714 var3)) {
         this.setState(AutoEnchanter.State.IDLE);
      } else if (this.needsMaterials() && !this.hasCraftingInProgress(var3)) {
         if (this.closeHandledScreenSafely()) {
            this.buildBuyRequests();
            if (!this.buyRequests.isEmpty()) {
               this.setState(AutoEnchanter.State.BUY_SEND);
               this.actionTimer.reset();
            } else {
               this.setState(AutoEnchanter.State.IDLE);
            }
         }
      } else if (!var3.method_34255().method_7960()) {
         this.clearCursorToInventory(var3);
      } else if (this.craftTimer.finished(this.delayMs())) {
         this.prepareCraftStage();
         moscow.mytheria.logger.MytheriaLogger.event("craft_dispatch")
            .with("stage", this.craftStage == null ? "null" : this.craftStage.name())
            .with("output_slot", describeStack(var3.method_7611(0).method_7677()))
            .emit();
         if (this.craftStage == AutoEnchanter.CraftStage.NONE) {
            this.closeHandledScreenSafely();
            this.setState(AutoEnchanter.State.IDLE);
         } else {
            boolean var5 = false;
            switch (this.craftStage) {
               case PLANKS:
                  var5 = this.craftPlanks(var3);
                  break;
               case STICKS:
                  var5 = this.craftSticks(var3);
                  break;
               case PICKAXE:
                  var5 = this.craftPickaxe(var3);
                  break;
               case SWORD:
                  var5 = this.craftSword(var3);
            }

            moscow.mytheria.logger.MytheriaLogger.event("craft_result")
               .with("stage", this.craftStage == null ? "null" : this.craftStage.name())
               .with("progressed", var5)
               .with("output_slot", describeStack(var3.method_7611(0).method_7677()))
               .with("fail_ticks", this.craftFailTicks)
               .emit();

            if (!var5) {
               this.craftFailTicks++;
               if (this.craftFailTicks >= 4) {
                  this.closeHandledScreenSafely();
                  this.setState(AutoEnchanter.State.IDLE);
               }
            } else {
               this.craftFailTicks = 0;
               this.prepareCraftStage();
               if (this.craftStage == AutoEnchanter.CraftStage.NONE) {
                  this.closeHandledScreenSafely();
                  this.setState(AutoEnchanter.State.IDLE);
               }
            }
         }
      }
   }

   private void handleEnchantOpen() {
      if (mc.field_1755 == null && this.actionTimer.finished(this.delayMs())) {
         class_2338 var1 = this.findNearestBlock(class_2246.field_10485, 4);
         if (var1 == null) {
            MessageUtility.info(class_2561.method_43470("[AutoEnchanter] Enchanting table not found."));
            this.setState(AutoEnchanter.State.IDLE);
         } else {
            this.beginBlockOpen(var1, AutoEnchanter.State.ENCHANT_WAIT);
            this.enchantTimer.reset();
            this.setState(AutoEnchanter.State.ENCHANT_WAIT);
         }
      }
   }

   private void handleEnchantWait() {
      if (mc.field_1755 instanceof class_465 var2) {
         if (var2.method_17577() instanceof class_1718 var4) {
            if (!var4.method_34255().method_7960()) {
               this.clearCursorToInventory(var4);
            } else {
               this.enchantTimer.reset();
               this.setState(AutoEnchanter.State.ENCHANT_PLACE);
            }
         } else if (this.enchantTimer.finished(this.timeoutMs())) {
            this.closeHandledScreenSafely();
            this.setState(AutoEnchanter.State.IDLE);
         }
      } else if (this.enchantTimer.finished(this.timeoutMs())) {
         this.setState(AutoEnchanter.State.IDLE);
      }
   }

   private void handleEnchantPlace() {
      if (!(mc.field_1755 instanceof class_465 var2 && var2.method_17577() instanceof class_1718 var3)) {
         this.setState(AutoEnchanter.State.IDLE);
      } else if (!var3.method_34255().method_7960()) {
         this.clearCursorToInventory(var3);
      } else if (this.enchantTimer.finished(this.delayMs())) {
         int var10 = this.findUnenchantedPickaxeSlot();
         if (var10 == -1) {
            this.closeHandledScreenSafely();
            this.setState(AutoEnchanter.State.IDLE);
         } else {
            int var5 = this.findInventorySlot(class_1802.field_8759);
            if (var5 == -1) {
               this.closeHandledScreenSafely();
               this.setState(AutoEnchanter.State.IDLE);
            } else {
               int var6 = this.findPlayerSlotId(var3, var10);
               int var7 = this.findPlayerSlotId(var3, var5);
               if (var6 != -1 && var7 != -1) {
                  int var8 = 3;
                  class_1799 var9 = var3.method_7611(1).method_7677();
                  if (!var9.method_7960() && var9.method_7909() != class_1802.field_8759) {
                     this.closeHandledScreenSafely();
                     this.setState(AutoEnchanter.State.IDLE);
                  } else if (!this.moveStackToSlot(var3, var6, 0)) {
                     this.closeHandledScreenSafely();
                     this.setState(AutoEnchanter.State.IDLE);
                  } else if (!this.ensureLapisInSlot(var3, var7, var8)) {
                     this.closeHandledScreenSafely();
                     this.setState(AutoEnchanter.State.IDLE);
                  } else {
                     this.pendingEnchantOption = this.getEnchantOptionIndex(var8);
                     this.enchantTimer.reset();
                     this.setState(AutoEnchanter.State.ENCHANT_CLICK);
                  }
               } else {
                  this.closeHandledScreenSafely();
                  this.setState(AutoEnchanter.State.IDLE);
               }
            }
         }
      }
   }

   private void handleEnchantClick() {
      if (!(mc.field_1755 instanceof class_465 var2 && var2.method_17577() instanceof class_1718 var3)) {
         this.setState(AutoEnchanter.State.IDLE);
      } else if (this.enchantTimer.finished(this.delayMs())) {
         int var6 = Math.max(0, Math.min(2, this.pendingEnchantOption));
         if (var3.field_7808[var6] <= 0) {
            for (int var5 = 2; var5 >= 0; var5--) {
               if (var3.field_7808[var5] > 0) {
                  var6 = var5;
                  break;
               }
            }
         }

         this.pendingEnchantOption = var6;
         int var7 = var3.field_7808[this.pendingEnchantOption];
         if (var7 > 0 && mc.field_1724.field_7520 < var7) {
            MessageUtility.info(class_2561.method_43470("[AutoEnchanter] Not enough XP for enchant (need " + var7 + "), closing to use bottles."));
            if (this.closeHandledScreenSafely()) {
               this.setState(AutoEnchanter.State.IDLE);
               this.enchantTimer.reset();
            }
         } else {
            mc.field_1761.method_2900(var3.field_7763, var6);
            this.enchantTimer.reset();
            this.setState(AutoEnchanter.State.ENCHANT_WAIT_RESULT);
         }
      }
   }

   private void handleEnchantWaitResult() {
      if (mc.field_1755 instanceof class_465 var2 && var2.method_17577() instanceof class_1718 var3) {
         if (!var3.method_34255().method_7960()) {
            this.clearCursorToInventory(var3);
         } else if (this.enchantTimer.finished(this.delayMs())) {
            mc.field_1761.method_2906(var3.field_7763, 0, 0, class_1713.field_7794, mc.field_1724);
            if (this.findUnenchantedPickaxeSlot() != -1
               && this.countItem(class_1802.field_8759) >= 3) {
               this.setState(AutoEnchanter.State.ENCHANT_PLACE);
               this.enchantTimer.reset();
            } else if (this.closeHandledScreenSafely()) {
               this.setState(AutoEnchanter.State.EVALUATE);
               this.enchantTimer.reset();
            }
         }
      } else {
         this.setState(AutoEnchanter.State.EVALUATE);
      }
   }

   private void handleEvaluate() {
      int var2 = this.findPickaxeSlotWithTarget();
      moscow.mytheria.logger.MytheriaLogger.event("evaluate")
         .with("target_slot", var2)
         .with("target_item", var2 == -1 ? "none"
               : describeStack(mc.field_1724.method_31548().method_5438(var2)))
         .with("sword_mode", this.isSwordMode())
         .with("sell_enabled", true)
         .with("relist_pending", this.relistPending)
         .emit();
      if (this.isSwordMode() && var2 != -1 && this.isFinalSword(mc.field_1724.method_31548().method_5438(var2))) {
         if (this.shouldUpgradeFinalSword()) {
            this.setState(AutoEnchanter.State.SMITH_OPEN);
            this.actionTimer.reset();
            return;
         }

         if (!this.relistPending) {
            this.setState(AutoEnchanter.State.SELL_SEND);
            this.sellTimer.reset();
            return;
         }
      } else if (!this.isSwordMode() && !this.relistPending && var2 != -1) {
         this.setState(AutoEnchanter.State.SELL_SEND);
         this.sellTimer.reset();
         return;
      }

      int var1;
      if ((var1 = this.findPickaxeSlotWithBad()) != -1) {
         class_1799 var3 = mc.field_1724.method_31548().method_5438(var1);
         if (this.isSwordMode() && this.hasKnockbackEnchant(var3)) {
            this.setState(AutoEnchanter.State.GRIND_OPEN);
            this.actionTimer.reset();
         } else {
            this.dropSlot = var1;
            this.setState(AutoEnchanter.State.DROP_BAD);
            this.actionTimer.reset();
         }
      } else {
         boolean var5 = this.relistPending || false;
         if (this.isSwordMode() && this.hasAnvilCandidates()) {
            this.setState(AutoEnchanter.State.ANVIL_OPEN);
            this.actionTimer.reset();
            return;
         }

         int var4 = this.findPickaxeSlotWithAnyEnchant(var5);
         if (var4 != -1) {
            // A grindable sword (issue #2) = netherite, Sharpness < 7, no Яд≥2, not final.
            // If we can anvil-combine (have both a sharpness sword and a poison sword) do
            // that; otherwise grind it clean so it can be re-enchanted toward Sharpness 7.
            // Poison swords (Яд≥2) are NOT grindable, so they are never re-enchanted.
            if (this.isSwordMode() && this.hasAnvilCandidates()) {
               this.setState(AutoEnchanter.State.ANVIL_OPEN);
            } else {
               this.setState(AutoEnchanter.State.GRIND_OPEN);
            }
            this.actionTimer.reset();
            return;
         }

         this.setState(AutoEnchanter.State.IDLE);
      }
   }

   private void handleGrindOpen() {
      int var1 = this.findPickaxeSlotWithBad();
      if (var1 != -1) {
         class_1799 var2 = mc.field_1724.method_31548().method_5438(var1);
         if (!this.isSwordMode() || !this.hasKnockbackEnchant(var2)) {
            this.dropSlot = var1;
            this.setState(AutoEnchanter.State.DROP_BAD);
            this.actionTimer.reset();
            return;
         }
      }

      if (mc.field_1755 == null && this.actionTimer.finished(this.delayMs())) {
         class_2338 var3 = this.findNearestBlock(class_2246.field_16337, 4);
         if (var3 == null) {
            MessageUtility.info(class_2561.method_43470("[AutoEnchanter] Grindstone not found."));
            this.setState(AutoEnchanter.State.IDLE);
         } else {
            this.beginBlockOpen(var3, AutoEnchanter.State.GRIND_WAIT);
            this.craftTimer.reset();
            this.setState(AutoEnchanter.State.GRIND_WAIT);
         }
      }
   }

   private void handleGrindWait() {
      if (mc.field_1755 instanceof class_465 var2) {
         if (var2.method_17577() instanceof class_3803 var4) {
            if (!var4.method_34255().method_7960()) {
               this.clearCursorToInventory(var4);
            } else {
               this.craftTimer.reset();
               this.setState(AutoEnchanter.State.GRIND_PLACE);
            }
         } else if (this.craftTimer.finished(this.timeoutMs())) {
            this.closeHandledScreenSafely();
            this.setState(AutoEnchanter.State.IDLE);
         }
      } else if (this.craftTimer.finished(this.timeoutMs())) {
         this.setState(AutoEnchanter.State.IDLE);
      }
   }

   private void handleGrindPlace() {
      if (!(mc.field_1755 instanceof class_465 var2 && var2.method_17577() instanceof class_3803 var3)) {
         this.setState(AutoEnchanter.State.IDLE);
      } else if (!var3.method_34255().method_7960()) {
         this.clearCursorToInventory(var3);
      } else if (this.craftTimer.finished(this.delayMs())) {
         int var7 = this.findPickaxeSlotWithBad();
         if (var7 != -1) {
            class_1799 var5 = mc.field_1724.method_31548().method_5438(var7);
            if (!this.isSwordMode() || !this.hasKnockbackEnchant(var5)) {
               if (!this.closeHandledScreenSafely()) {
                  return;
               } else {
                  this.dropSlot = var7;
                  this.setState(AutoEnchanter.State.DROP_BAD);
                  this.actionTimer.reset();
                  return;
               }
            }
         }

         int var8 = this.findPickaxeSlotWithAnyEnchant(this.relistPending || false);
         if (var8 == -1) {
            this.closeHandledScreenSafely();
            this.setState(AutoEnchanter.State.IDLE);
         } else {
            int var6 = this.findPlayerSlotId(var3, var8);
            if (var6 == -1) {
               this.closeHandledScreenSafely();
               this.setState(AutoEnchanter.State.IDLE);
            } else if (!this.moveStackToSlot(var3, var6, 0)) {
               this.closeHandledScreenSafely();
               this.setState(AutoEnchanter.State.IDLE);
            } else {
               this.craftTimer.reset();
               this.setState(AutoEnchanter.State.GRIND_TAKE);
            }
         }
      }
   }

   private void handleGrindTake() {
      if (!(mc.field_1755 instanceof class_465 var2 && var2.method_17577() instanceof class_3803 var3)) {
         this.setState(AutoEnchanter.State.IDLE);
      } else if (this.craftTimer.finished(this.delayMs())) {
         if (!var3.method_34255().method_7960()) {
            this.clearCursorToInventory(var3);
         } else {
            mc.field_1761.method_2906(var3.field_7763, 2, 0, class_1713.field_7794, mc.field_1724);
            if (!var3.method_7611(2).method_7677().method_7960()) {
               mc.field_1761.method_2906(var3.field_7763, 2, 0, class_1713.field_7790, mc.field_1724);
               this.clearCursorToInventory(var3);
            }

            int var6 = this.findPickaxeSlotWithAnyEnchant(this.relistPending || false);
            int var5 = this.findPickaxeSlotWithBad();
            if (var6 != -1 || var5 != -1) {
               this.setState(AutoEnchanter.State.GRIND_PLACE);
               this.craftTimer.reset();
            } else if (this.closeHandledScreenSafely()) {
               this.setState(AutoEnchanter.State.IDLE);
            }
         }
      }
   }

   private void handleAnvilOpen() {
      if (mc.field_1755 == null && this.actionTimer.finished(this.delayMs())) {
         class_2338 var1 = this.findNearestAnvilBlock(4);
         if (var1 == null) {
            MessageUtility.info(class_2561.method_43470("[AutoEnchanter] Anvil not found."));
            this.setState(AutoEnchanter.State.IDLE);
            return;
         }

         int[] var2 = this.findAnvilCandidates();
         if (var2 == null) {
            this.setState(AutoEnchanter.State.IDLE);
            return;
         }

         this.anvilLeftSlot = var2[0];
         this.anvilRightSlot = var2[1];
         this.beginBlockOpen(var1, AutoEnchanter.State.ANVIL_WAIT);
         this.craftTimer.reset();
         this.anvilScreenOpenedAtMs = System.currentTimeMillis();
         this.setState(AutoEnchanter.State.ANVIL_WAIT);
      }
   }

   private void handleAnvilWait() {
      if (mc.field_1755 instanceof class_465 var2) {
         if (var2.method_17577() instanceof class_1706 var4) {
            if (!var4.method_34255().method_7960()) {
               this.clearCursorToInventory(var4);
            } else {
               this.craftTimer.reset();
               this.setState(AutoEnchanter.State.ANVIL_PLACE);
            }
         } else if (System.currentTimeMillis() - this.anvilScreenOpenedAtMs > 3000L) {
            this.closeHandledScreenSafely();
            this.setState(AutoEnchanter.State.IDLE);
         }
      } else if (System.currentTimeMillis() - this.anvilScreenOpenedAtMs > 3000L) {
         this.setState(AutoEnchanter.State.IDLE);
      }
   }

   private void handleAnvilPlace() {
      if (!(mc.field_1755 instanceof class_465 var2 && var2.method_17577() instanceof class_1706 var3)) {
         this.setState(AutoEnchanter.State.IDLE);
      } else if (this.craftTimer.finished(this.delayMs())) {
         if (this.anvilLeftSlot != -1 && this.anvilRightSlot != -1) {
            int var6 = this.findPlayerSlotId(var3, this.anvilLeftSlot);
            int var5 = this.findPlayerSlotId(var3, this.anvilRightSlot);
            if (var6 == -1 || var5 == -1) {
               this.closeHandledScreenSafely();
               this.setState(AutoEnchanter.State.IDLE);
            } else if (!this.moveStackToSlot(var3, var6, 0)) {
               this.closeHandledScreenSafely();
               this.setState(AutoEnchanter.State.IDLE);
            } else if (!this.moveStackToSlot(var3, var5, 1)) {
               this.closeHandledScreenSafely();
               this.setState(AutoEnchanter.State.IDLE);
            } else {
               this.craftTimer.reset();
               this.setState(AutoEnchanter.State.ANVIL_COMBINE);
            }
         } else {
            this.closeHandledScreenSafely();
            this.setState(AutoEnchanter.State.IDLE);
         }
      }
   }

   private void handleAnvilCombine() {
      if (!(mc.field_1755 instanceof class_465 var2 && var2.method_17577() instanceof class_1706 var3)) {
         this.setState(AutoEnchanter.State.IDLE);
      } else if (!var3.method_34255().method_7960()) {
         this.clearCursorToInventory(var3);
      } else if (this.craftTimer.finished(this.delayMs())) {
         if (var3.method_7611(2).method_7677().method_7960()) {
            this.closeHandledScreenSafely();
            this.setState(AutoEnchanter.State.IDLE);
         } else {
            mc.field_1761.method_2906(var3.field_7763, 2, 0, class_1713.field_7794, mc.field_1724);
            this.craftTimer.reset();
            this.setState(AutoEnchanter.State.ANVIL_TAKE);
         }
      }
   }

   private void handleAnvilTake() {
      if (!(mc.field_1755 instanceof class_465 var2 && var2.method_17577() instanceof class_1706)) {
         this.setState(AutoEnchanter.State.IDLE);
      } else if (this.craftTimer.finished(this.delayMs())) {
         if (this.closeHandledScreenSafely()) {
            this.anvilLeftSlot = -1;
            this.anvilRightSlot = -1;
            this.setState(AutoEnchanter.State.EVALUATE);
         }
      }
   }

   private void handleSmithOpen() {
      if (mc.field_1755 == null && this.actionTimer.finished(this.delayMs())) {
         class_2338 var1 = this.findNearestBlock(class_2246.field_16329, 4);
         if (var1 == null) {
            MessageUtility.info(class_2561.method_43470("[AutoEnchanter] Smithing table not found."));
            this.setState(AutoEnchanter.State.IDLE);
         } else {
            this.beginBlockOpen(var1, AutoEnchanter.State.SMITH_WAIT);
            this.craftTimer.reset();
            this.smithScreenOpenedAtMs = System.currentTimeMillis();
            this.setState(AutoEnchanter.State.SMITH_WAIT);
         }
      }
   }

   private void handleSmithWait() {
      if (mc.field_1755 instanceof class_465 var2) {
         if (var2.method_17577() instanceof class_4862 var4) {
            if (!var4.method_34255().method_7960()) {
               this.clearCursorToInventory(var4);
            } else {
               this.craftTimer.reset();
               this.setState(AutoEnchanter.State.SMITH_PLACE);
            }
         } else if (System.currentTimeMillis() - this.smithScreenOpenedAtMs > 3000L) {
            this.closeHandledScreenSafely();
            this.setState(AutoEnchanter.State.IDLE);
         }
      } else if (System.currentTimeMillis() - this.smithScreenOpenedAtMs > 3000L) {
         this.setState(AutoEnchanter.State.IDLE);
      }
   }

   private void handleSmithPlace() {
      if (!(mc.field_1755 instanceof class_465 var2 && var2.method_17577() instanceof class_4862 var3)) {
         this.setState(AutoEnchanter.State.IDLE);
      } else if (this.craftTimer.finished(this.delayMs())) {
         int var10 = this.findFinalDiamondSwordSlot();
         int var5 = this.findInventorySlot(class_1802.field_22020);
         if (var10 != -1 && var5 != -1) {
            int var6 = this.findPlayerSlotId(var3, var10);
            int var7 = this.findPlayerSlotId(var3, var5);
            if (var6 != -1 && var7 != -1) {
               int var8 = this.findInventorySlot(class_1802.field_41946);
               if (var8 != -1) {
                  int var9 = this.findPlayerSlotId(var3, var8);
                  if (var9 != -1 && !this.moveStackToSlot(var3, var9, 0)) {
                     this.closeHandledScreenSafely();
                     this.setState(AutoEnchanter.State.IDLE);
                     return;
                  }
               }

               if (!this.moveStackToSlot(var3, var6, 1)) {
                  this.closeHandledScreenSafely();
                  this.setState(AutoEnchanter.State.IDLE);
               } else if (!this.moveStackToSlot(var3, var7, 2)) {
                  this.closeHandledScreenSafely();
                  this.setState(AutoEnchanter.State.IDLE);
               } else {
                  this.craftTimer.reset();
                  this.setState(AutoEnchanter.State.SMITH_TAKE);
               }
            } else {
               this.closeHandledScreenSafely();
               this.setState(AutoEnchanter.State.IDLE);
            }
         } else {
            this.closeHandledScreenSafely();
            this.setState(AutoEnchanter.State.IDLE);
         }
      }
   }

   private void handleSmithTake() {
      if (!(mc.field_1755 instanceof class_465 var2 && var2.method_17577() instanceof class_4862 var3)) {
         this.setState(AutoEnchanter.State.IDLE);
      } else if (this.craftTimer.finished(this.delayMs())) {
         if (!var3.method_34255().method_7960()) {
            this.clearCursorToInventory(var3);
         } else {
            if (!var3.method_7611(3).method_7677().method_7960()) {
               mc.field_1761.method_2906(var3.field_7763, 3, 0, class_1713.field_7794, mc.field_1724);
               if (!var3.method_7611(3).method_7677().method_7960()) {
                  mc.field_1761.method_2906(var3.field_7763, 3, 0, class_1713.field_7790, mc.field_1724);
                  this.clearCursorToInventory(var3);
               }
            }

            if (this.closeHandledScreenSafely()) {
               this.setState(AutoEnchanter.State.EVALUATE);
            }
         }
      }
   }

   private void handleSellSend() {
      if (true && !this.relistPending) {
         if (this.sellTimer.finished(200L)) {
            int var1 = this.findPickaxeSlotWithTarget();
            if (var1 == -1) {
               this.setState(AutoEnchanter.State.IDLE);
               return;
            }

            class_1799 var2 = mc.field_1724.method_31548().method_5438(var1);
            long var3 = this.getPlannedSellPrice(var2);
            if (var3 <= 0L) {
               if (this.sellPriceMode.is(this.priceMarket)) {
                  this.schedulePriceCheck();
               } else {
                  MessageUtility.info(class_2561.method_43470("[AutoEnchanter] Invalid fixed price."));
                  this.setState(AutoEnchanter.State.IDLE);
               }

               return;
            }

            int var5 = this.findEmptyHotbarSlot();
            if (var5 == -1) {
               this.compactHotbarToInventory();
               var5 = this.findEmptyHotbarSlot();
            }

            if (var5 == -1) {
               MessageUtility.info(class_2561.method_43470("[AutoEnchanter] Hotbar is full."));
               this.setState(AutoEnchanter.State.IDLE);
               return;
            }

            if (!this.moveSingleToHotbar(var1, var5)) {
               this.setState(AutoEnchanter.State.IDLE);
               return;
            }

            mc.field_1724.method_31548().field_7545 = var5;
            this.pendingSellPrice = var3;
            this.sellCommandQueued = true;
            this.sellHoldUntilMs = System.currentTimeMillis() + 500L;
            this.sellTimer.reset();
            this.setState(AutoEnchanter.State.SELL_WAIT);
         }
      } else {
         this.setState(AutoEnchanter.State.IDLE);
      }
   }

   private void handleSellWait() {
      if (this.sellCommandQueued) {
         if (this.pendingSellPrice <= 0L) {
            this.sellCommandQueued = false;
            this.setState(AutoEnchanter.State.IDLE);
         } else if (System.currentTimeMillis() >= this.sellHoldUntilMs) {
            this.sendChat("/ah sell " + this.pendingSellPrice);
            this.sellCommandQueued = false;
            this.pendingSellPrice = -1L;
            this.sellHoldUntilMs = 0L;
            this.sellTimer.reset();
         }
      } else if (this.sellTimer.finished(200L)) {
         this.setState(AutoEnchanter.State.IDLE);
      }
   }

   private void handleDropBad() {
      if (this.actionTimer.finished(this.delayMs())) {
         if (this.dropSlot == -1) {
            this.setState(AutoEnchanter.State.IDLE);
         } else if (mc.field_1755 == null) {
            mc.field_1761.method_2906(mc.field_1724.field_7512.field_7763, this.inventoryIndexToSlotId(this.dropSlot), 1, class_1713.field_7795, mc.field_1724);
            this.dropSlot = -1;
            this.setState(AutoEnchanter.State.IDLE);
         }
      }
   }

   private void handleRelistFlow() {
      if (!this.relistPending) {
         this.closeHandledScreenSafely();
         this.setState(AutoEnchanter.State.IDLE);
         this.relistFlowTimer.reset();
      } else {
         switch (this.state) {
            case RELIST_OPEN_AH:
               if (!this.relistFlowTimer.finished(200L)) {
                  return;
               }

               if (mc.field_1755 != null) {
                  this.closeHandledScreenSafely();
                  return;
               }

               this.sendChat("/ah");
               this.setState(AutoEnchanter.State.RELIST_WAIT_AH);
               this.relistFlowTimer.reset();
               break;
            case RELIST_WAIT_AH:
               if (this.isAuctionMainScreen()) {
                  this.setState(AutoEnchanter.State.RELIST_CLICK_STORAGE);
                  this.relistFlowTimer.reset();
                  return;
               }

               if (this.relistFlowTimer.finished(3000L)) {
                  this.setState(AutoEnchanter.State.RELIST_OPEN_AH);
                  this.relistFlowTimer.reset();
               }
               break;
            case RELIST_CLICK_STORAGE:
               class_1703 var4 = mc.field_1724.field_7512;
               if (var4 instanceof class_1703) {
                  if (!this.relistFlowTimer.finished(200L)) {
                     return;
                  }

                  if (!var4.method_34255().method_7960()) {
                     this.clearCursorToInventory(var4);
                     return;
                  }

                  int var5 = this.findSlotByItem(var4, class_1802.field_8251, "хранилище", "storage");
                  if (var5 == -1) {
                     var5 = this.findSlotByItem(var4, class_1802.field_8251);
                  }

                  if (var5 == -1) {
                     var5 = this.findSlotByKeyword(var4, "хранилище", "storage");
                  }

                  if (var5 != -1 && this.clickMenuSlot(var4, var5)) {
                     this.setState(AutoEnchanter.State.RELIST_WAIT_STORAGE);
                     this.relistFlowTimer.reset();
                  }
               }
               break;
            case RELIST_WAIT_STORAGE:
               if (this.isStorageScreen()) {
                  this.setState(AutoEnchanter.State.RELIST_CLICK_RELIST);
                  this.relistFlowTimer.reset();
                  return;
               }

               if (this.relistFlowTimer.finished(3000L)) {
                  this.setState(AutoEnchanter.State.RELIST_OPEN_AH);
                  this.relistFlowTimer.reset();
               }
               break;
            case RELIST_CLICK_RELIST:
               class_1703 var1 = mc.field_1724.field_7512;
               if (var1 instanceof class_1703) {
                  if (!this.relistFlowTimer.finished(200L)) {
                     return;
                  }

                  if (!var1.method_34255().method_7960()) {
                     this.clearCursorToInventory(var1);
                     return;
                  }

                  int var3 = this.findSlotByItem(var1, class_1802.field_8407, "перевыстав", "relist");
                  if (var3 == -1) {
                     var3 = this.findSlotByKeyword(var1, "перевыстав", "relist");
                  }

                  if (var3 == -1) {
                     var3 = this.findSlotByItem(var1, class_1802.field_8407);
                  }

                  if (var3 != -1 && this.clickMenuSlot(var1, var3)) {
                     this.scheduleRelistEscape();
                     this.closeHandledScreenSafely();
                     this.relistNextAtMs = System.currentTimeMillis() + 60000L;
                     this.setState(AutoEnchanter.State.IDLE);
                     this.relistFlowTimer.reset();
                  }
               }
         }
      }
   }

   private void schedulePriceCheck() {
      if (this.shouldPriceCheck()) {
         this.buildPriceRequests();
         if (!this.priceRequests.isEmpty()) {
            this.setState(AutoEnchanter.State.PRICE_SEND);
            this.actionTimer.reset();
         }
      }
   }

   private boolean shouldPriceCheck() {
      return false;
   }

   private void buildPriceRequests() {
      this.priceRequests.clear();
      this.unitPrices.clear();
      this.priceIndex = 0;
      if (this.shouldPriceCheck()) {
         this.priceRequests.add(new AutoEnchanter.PriceRequest("lapis", this.buildSearchName(new class_1799(class_1802.field_8759)), false, false));
         this.priceRequests.add(new AutoEnchanter.PriceRequest("xp", this.getXpSearchQuery(), false, true));
         if (this.isPickaxeMode()) {
            this.priceRequests.add(new AutoEnchanter.PriceRequest("diamond", this.buildSearchName(new class_1799(class_1802.field_8477)), false, false));
            this.priceRequests.add(new AutoEnchanter.PriceRequest("wood", this.getWoodSearchQuery(), false, false));
         } else if (this.isSwordMode()) {
            this.priceRequests.add(new AutoEnchanter.PriceRequest("diamond", this.buildSearchName(new class_1799(class_1802.field_8477)), false, false));
            this.priceRequests.add(new AutoEnchanter.PriceRequest("wood", this.getWoodSearchQuery(), false, false));
         } else {
            class_1792 var2 = this.getTargetBaseItem();
            if (var2 != null) {
               this.priceRequests.add(new AutoEnchanter.PriceRequest("base", this.buildSearchName(new class_1799(var2)), false, false));
            }
         }

         class_1792 var1;
         if (this.sellPriceMode.is(this.priceMarket) && (var1 = this.getTargetOutputItem()) != null) {
            this.priceRequests.add(new AutoEnchanter.PriceRequest("output", this.buildSearchName(new class_1799(var1)), true, false));
         }
      }
   }

   private boolean needsPriceUpdate() {
      if (!this.shouldPriceCheck()) {
         return false;
      } else {
         for (AutoEnchanter.PriceRequest var2 : this.priceRequests) {
            AutoEnchanter.PriceCache var3 = this.priceCache.get(var2.key);
            if (var3 == null || !var3.isValid()) {
               return true;
            }
         }

         return false;
      }
   }

   private boolean isProfitable() {
      return true;
   }

   private long getPlannedSellPrice() {
      return this.getPlannedSellPrice(null);
   }

   private long getPlannedSellPrice(class_1799 var1) {
      return this.getLongInput(this.sellFixedPriceInput, 100000L, 1L, 10000000L);
   }

   private long computeCraftUnitCost() {
      long var1 = this.getUnitPrice("lapis");
      long var3 = this.getUnitPrice("xp");
      long var5 = 0L;
      if (this.isPickaxeMode()) {
         long var7 = this.getUnitPrice("diamond");
         long var9 = this.getUnitPrice("wood");
         if (var7 <= 0L || var9 <= 0L) {
            return -1L;
         }

         var5 += var7 * 3L + var9;
      } else if (this.isSwordMode()) {
         long var12 = this.getUnitPrice("diamond");
         long var14 = this.getUnitPrice("wood");
         if (var12 <= 0L || var14 <= 0L) {
            return -1L;
         }

         var5 += var12 * 2L + var14;
      } else {
         long var13 = this.getUnitPrice("base");
         if (var13 <= 0L) {
            return -1L;
         }

         var5 += var13;
      }

      if (var1 > 0L) {
         var5 += var1 * 3L;
      }

      if (var3 > 0L && this.needsXp()) {
         var5 += var3 * Math.max(1L, (long)this.requiredXpBottles());
      }

      return var5;
   }

   private long getUnitPrice(String var1) {
      AutoEnchanter.PriceCache var2 = this.priceCache.get(var1);
      if (var2 != null && var2.isValid()) {
         return var2.price;
      } else {
         Long var3 = this.unitPrices.get(var1);
         return var3 == null ? -1L : var3;
      }
   }

   private void buildBuyRequests() {
      if (this.isSwordMode()) {
         this.buildSwordBuyRequests();
      } else {
         this.buyRequests.clear();
         this.buyIndex = 0;
         this.buyAttempts = 0;
         this.buyBlocked = false;
         int var1 = this.countItem(class_1802.field_8759);
         int var2 = 3;
         if (var1 < var2 && var1 < 128) {
            this.buyRequests
               .add(new AutoEnchanter.BuyRequest("lapis", this.buildSearchName(new class_1799(class_1802.field_8759)), class_1802.field_8759, var2));
         }

         int var3 = Math.round(this.countFarm.getCurrentValue());
         int var4 = this.countTotalTargetBase() + (this.isSwordMode() ? this.countItem(class_1802.field_22022) : 0);
         int var5 = Math.max(0, var3 - var4);
         if (var5 > 0) {
            if (this.isPickaxeMode()) {
               if (this.countItem(class_1802.field_8477) < 3 * var5) {
                  this.buyRequests
                     .add(
                        new AutoEnchanter.BuyRequest(
                           "diamond", this.buildSearchName(new class_1799(class_1802.field_8477)), class_1802.field_8477, 3 * var5
                        )
                     );
               }

               int var6 = this.countLogs() * 4 + this.countPlanks();
               int var7 = var5 * 2;
               int var8 = (int)Math.ceil(Math.max(0, var7 - var6) / 4.0);
               if (this.countItem(class_1802.field_8600) < 2 * var5 && var8 > 0) {
                  this.buyRequests.add(new AutoEnchanter.BuyRequest("wood", this.getWoodSearchQuery(), null, var8));
               }
            } else if (this.isSwordMode()) {
               if (this.countItem(class_1802.field_8477) < 2 * var5) {
                  this.buyRequests
                     .add(
                        new AutoEnchanter.BuyRequest(
                           "diamond", this.buildSearchName(new class_1799(class_1802.field_8477)), class_1802.field_8477, 2 * var5
                        )
                     );
               }

               int var9 = this.countLogs() * 4 + this.countPlanks();
               int var12 = var5 * 1;
               int var14 = (int)Math.ceil(Math.max(0, var12 - var9) / 4.0);
               if (this.countItem(class_1802.field_8600) < 1 * var5 && var14 > 0) {
                  this.buyRequests.add(new AutoEnchanter.BuyRequest("wood", this.getWoodSearchQuery(), null, var14));
               }

               if (this.shouldUpgradeFinalSword() && this.countItem(class_1802.field_22020) <= 0) {
                  this.buyRequests
                     .add(new AutoEnchanter.BuyRequest("netherite", "Незеритовый слиток", class_1802.field_22020, 1 + (int)Math.floor(Math.random() * 4.0)));
               }
            } else {
               class_1792 var10 = this.getTargetBaseItem();
               if (var10 != null && this.countItem(var10) < 1) {
                  this.buyRequests.add(new AutoEnchanter.BuyRequest("base", this.buildSearchName(new class_1799(var10)), var10, 1));
               }
            }
         }

         int var11 = this.requiredXpBottles();
         int var13 = this.countItem(class_1802.field_8287);
         if (this.needsXp() && var13 < var11) {
            this.buyRequests.add(new AutoEnchanter.BuyRequest("xp", this.getXpSearchQuery(), class_1802.field_8287, var11, true));
         }
      }
   }

   private boolean needsMaterials() {
      if (this.isSwordMode()) {
         return this.needsSwordMaterials();
      } else if (this.needsXp() && !this.hasXpBottle()) {
         return true;
      } else {
         int var2 = this.countItem(class_1802.field_8759);
         if (var2 < 3 && var2 < 128) {
            return true;
         } else {
            int var3 = Math.round(this.countFarm.getCurrentValue());
            int var4 = this.countTotalTargetBase() + (this.isSwordMode() ? this.countItem(class_1802.field_22022) : 0);
            int var5 = Math.max(0, var3 - var4);
            if (var5 > 0) {
               if (this.isPickaxeMode()) {
                  return this.countItem(class_1802.field_8477) < 3 * var5
                     || this.countItem(class_1802.field_8600) < 2 * var5 && this.countPlanks() < 2 * var5 && this.countLogs() < var5;
               }

               if (this.isSwordMode()) {
                  return this.countItem(class_1802.field_8477) < 2 * var5
                     || this.countItem(class_1802.field_8600) < 1 * var5 && this.countPlanks() < 2 * var5 && this.countLogs() < var5;
               }
            }

            class_1792 var6 = this.getTargetBaseItem();
            return var6 != null && this.countItem(var6) < 1;
         }
      }
   }

   private int countTotalTargetBase() {
      if (this.isPickaxeMode()) {
         return this.countItem(class_1802.field_8377);
      } else if (this.isSwordMode()) {
         return this.countItem(class_1802.field_8802);
      } else {
         class_1792 var1 = this.getTargetBaseItem();
         return var1 != null ? this.countItem(var1) : 0;
      }
   }

   private boolean needsCraft() {
      if (this.isSwordMode()) {
         return false;
      } else if (!this.isPickaxeMode() && !this.isSwordMode()) {
         return false;
      } else {
         int var1 = Math.round(this.countFarm.getCurrentValue());
         int var2 = this.countTotalTargetBase() + (this.isSwordMode() ? this.countItem(class_1802.field_22022) : 0);
         return var2 < var1;
      }
   }

   private boolean hasPickaxeWithAnyState() {
      return this.hasUnenchantedPickaxe() || this.findPickaxeSlotWithAnyEnchant() != -1;
   }

   private boolean hasUnenchantedPickaxe() {
      return this.findUnenchantedPickaxeSlot() != -1;
   }

   private class_1792 getTargetBaseItem() {
      return this.isSwordMode() ? class_1802.field_8802 : class_1802.field_8377;
   }

   private class_1792 getTargetOutputItem() {
      return this.isSwordMode() ? class_1802.field_22022 : this.getTargetBaseItem();
   }

   private boolean isPickaxeMode() {
      return !this.section.is(this.secSword);
   }

   private boolean isAutoEatModuleEnabled() {
      return false;
   }

   private boolean isBookMode() {
      return false;
   }

   private boolean isSwordMode() {
      return this.section.is(this.secSword);
   }

   private String[] getTargetEnchantNeedles() {
      if (this.isPickaxeMode()) {
         return new String[]{"Бульдозер", "Bulldozer"};
      } else if (this.isSwordMode()) {
         return new String[]{"Яд", "Poison"};
      } else if (this.targetEnchant.is(this.enchantMagnet)) {
         return new String[]{"Магнит", "Magnet"};
      } else if (this.targetEnchant.is(this.enchantBulldozer)) {
         return new String[]{"Бульдозер", "Bulldozer"};
      } else if (this.targetEnchant.is(this.enchantWaterWalk)) {
         return new String[]{"Подводная ходьба", "Depth Strider"};
      } else if (this.targetEnchant.is(this.enchantDetection)) {
         return new String[]{"Детекция", "Detection"};
      } else if (this.targetEnchant.is(this.enchantPoison)) {
         return new String[]{"Яд", "Poison"};
      } else if (this.targetEnchant.is(this.enchantVampirism)) {
         return new String[]{"Вампиризм", "Vampirism"};
      } else {
         return this.targetEnchant.is(this.enchantOxidation) ? new String[]{"Окисление", "Oxidation"} : new String[0];
      }
   }

   private boolean targetRequiresLevelTwo() {
      return this.isPickaxeMode() || this.targetEnchant.is(this.enchantBulldozer);
   }

   private boolean needsXp() {
      return this.getCurrentLevelExact() < 30.0F;
   }

   private int requiredXpBottles() {
      if (!this.needsXp()) {
         return 0;
      } else {
         float var1 = Math.max(0.0F, 30.0F - this.getCurrentLevelExact());
         int var2 = (int)Math.ceil(var1 / 15.0);
         return var2 <= 0 ? 0 : Math.max(3, var2);
      }
   }

   private float getCurrentLevelExact() {
      return mc.field_1724.field_7520 + mc.field_1724.field_7510;
   }

   private boolean hasXpBottle() {
      return this.countItem(class_1802.field_8287) >= this.requiredXpBottles();
   }

   private boolean useXpBottle() {
      int var1 = this.findHotbarSlot(class_1802.field_8287);
      if (var1 == -1) {
         int var2 = this.findInventorySlot(class_1802.field_8287);
         if (var2 == -1) {
            return false;
         }

         int var3 = this.findEmptyHotbarSlot();
         if (var3 == -1) {
            this.compactHotbarToInventory();
            var3 = this.findEmptyHotbarSlot();
         }

         if (var3 == -1) {
            return false;
         }

         if (!this.moveSingleToHotbar(var2, var3)) {
            return false;
         }

         var1 = var3;
      }

      float var4 = mc.field_1724.method_36454();
      float var5 = mc.field_1724.method_36455();
      mc.field_1724.method_31548().field_7545 = var1;
      this.rotateToAnglesSmooth(var4, 88.0F + (float)(Math.random() * 2.0), false);
      mc.field_1761.method_2919(mc.field_1724, class_1268.field_5808);
      mc.field_1724.method_6104(class_1268.field_5808);
      this.rotateToAnglesSmooth(var4, var5, false);
      return true;
   }

   private void prepareCraftStage() {
      if (!this.isPickaxeMode() && !this.isSwordMode()) {
         this.setCraftStage(AutoEnchanter.CraftStage.NONE);
      } else {
         int var1 = this.countAvailableForCraft(class_1802.field_8600);
         int var2 = this.countAvailableForCraft(class_1802.field_8477);
         if (this.isPickaxeMode()) {
            if (var1 < 2) {
               this.craftStage = this.countPlanks() < 2 ? AutoEnchanter.CraftStage.PLANKS : AutoEnchanter.CraftStage.STICKS;
            } else {
               this.setCraftStage(AutoEnchanter.CraftStage.PICKAXE);
               if ((var2 < 3 || var1 < 2) && !this.isPickaxeRecipePlacedInGrid()) {
                  this.setCraftStage(AutoEnchanter.CraftStage.NONE);
               }

               if (this.countItem(class_1802.field_8377) >= Math.round(this.countFarm.getCurrentValue())) {
                  this.setCraftStage(AutoEnchanter.CraftStage.NONE);
               }
            }
         } else if (var1 < 1) {
            this.craftStage = this.countPlanks() < 2 ? AutoEnchanter.CraftStage.PLANKS : AutoEnchanter.CraftStage.STICKS;
         } else {
            this.setCraftStage(AutoEnchanter.CraftStage.SWORD);
            if ((var2 < 2 || var1 < 1) && !this.isSwordRecipePlacedInGrid()) {
               this.setCraftStage(AutoEnchanter.CraftStage.NONE);
            }

            if (this.countItem(class_1802.field_8802) >= Math.round(this.countFarm.getCurrentValue())) {
               this.setCraftStage(AutoEnchanter.CraftStage.NONE);
            }
         }
      }
   }

   private boolean craftPlanks(class_1714 var1) {
      this.clearCraftingGrid(var1);
      return !this.placeOneCraftIngredient(var1, this::isLogItem, 4) ? false : this.takeCraftingResult(var1, null);
   }

   private boolean craftSticks(class_1714 var1) {
      this.clearCraftingGrid(var1);
      if (!this.placeOneCraftIngredient(var1, this::isPlankItem, 1)) {
         return false;
      } else {
         return !this.placeOneCraftIngredient(var1, this::isPlankItem, 4) ? false : this.takeCraftingResult(var1, class_1802.field_8600);
      }
   }

   private boolean craftPickaxe(class_1714 var1) {
      if (!this.isPickaxeMode() || this.countItem(class_1802.field_8377) >= Math.round(this.countFarm.getCurrentValue())) {
         return false;
      } else if (this.isPickaxeRecipePlaced(var1)) {
         return this.takeCraftingResult(var1, class_1802.field_8377, this::isCleanDiamondPickaxeOutput) || this.isPickaxeRecipePlaced(var1);
      } else if (this.craftViaRecipeBook(var1, this::isCleanDiamondPickaxeOutput)) {
         return true;
      } else {
         this.clearCraftingGrid(var1);
         if (!this.placeOneCraftIngredient(var1, var0 -> var0.method_7909() == class_1802.field_8477, 0)) {
            return false;
         } else if (!this.placeOneCraftIngredient(var1, var0 -> var0.method_7909() == class_1802.field_8477, 1)) {
            return false;
         } else if (!this.placeOneCraftIngredient(var1, var0 -> var0.method_7909() == class_1802.field_8477, 2)) {
            return false;
         } else if (!this.placeOneCraftIngredient(var1, var0 -> var0.method_7909() == class_1802.field_8600, 4)) {
            return false;
         } else {
            return !this.placeOneCraftIngredient(var1, var0 -> var0.method_7909() == class_1802.field_8600, 7)
               ? false
               : this.takeCraftingResult(var1, class_1802.field_8377, this::isCleanDiamondPickaxeOutput) || this.isPickaxeRecipePlaced(var1);
         }
      }
   }

   private boolean isCleanDiamondPickaxeOutput(class_1799 var1) {
      return !var1.method_7960() && var1.method_7909() == class_1802.field_8377 && !this.hasAnyEnchant(var1);
   }

   private boolean craftSword(class_1714 var1) {
      if (!this.isSwordMode() || this.countItem(class_1802.field_8802) >= Math.round(this.countFarm.getCurrentValue())) {
         return false;
      } else if (this.isSwordRecipePlaced(var1)) {
         return this.takeCraftingResult(var1, class_1802.field_8802) || this.isSwordRecipePlaced(var1);
      } else if (this.craftViaRecipeBook(var1, var0 -> var0.method_7909() == class_1802.field_8802)) {
         return true;
      } else {
         this.clearCraftingGrid(var1);
         if (!this.placeOneCraftIngredient(var1, var0 -> var0.method_7909() == class_1802.field_8477, 1)) {
            return false;
         } else if (!this.placeOneCraftIngredient(var1, var0 -> var0.method_7909() == class_1802.field_8477, 4)) {
            return false;
         } else {
            return !this.placeOneCraftIngredient(var1, var0 -> var0.method_7909() == class_1802.field_8600, 7)
               ? false
               : this.takeCraftingResult(var1, class_1802.field_8802) || this.isSwordRecipePlaced(var1);
         }
      }
   }

   private boolean craftViaRecipeBook(class_1714 var1, Predicate<class_1799> var2) {
      int var3 = this.findCraftingResultSlotId(var1);
      if (var3 >= 0 && var3 < var1.field_7761.size()) {
         class_1799 var4 = ((class_1735)var1.field_7761.get(var3)).method_7677();
         if (!var4.method_7960() && var2.test(var4)) {
            return this.takeCraftingResult(var1, var4.method_7909());
         }
      }

      if (mc.field_1724 == null || mc.field_1687 == null || mc.field_1761 == null || !var1.method_34255().method_7960()) {
         return false;
      }

      class_299 var9 = mc.field_1724.method_3130();
      class_10352 var10 = class_10363.method_65008((class_1937)mc.field_1687);

      for (class_516 var6 : var9.method_1393()) {
         for (class_10297 var8 : var6.method_2650()) {
            if (var8.comp_3263().comp_3258().method_64738(var10).stream().anyMatch(var2)) {
               this.clearCraftingGrid(var1);
               mc.field_1761.method_2912(var1.field_7763, var8.comp_3262(), false);
               return true;
            }
         }
      }

      return false;
   }

   private void clearCraftingGrid(class_1714 var1) {
      if (this.isInventoryFull()) {
         this.stopForFullInventory();
      } else {
         if (var1.method_34255().method_7960()) {
            for (int var2 = 0; var2 < var1.field_7761.size(); var2++) {
               class_1735 var3 = (class_1735)var1.field_7761.get(var2);
               if (var3.field_7871 instanceof class_1715 && !var3.method_7677().method_7960()) {
                  mc.field_1761.method_2906(var1.field_7763, var2, 0, class_1713.field_7794, mc.field_1724);
               }
            }
         }
      }
   }

   private int findCraftingInputSlotId(class_1714 var1, int var2) {
      for (int var3 = 0; var3 < var1.field_7761.size(); var3++) {
         class_1735 var4 = (class_1735)var1.field_7761.get(var3);
         if (var4.field_7871 instanceof class_1715 && var4.method_34266() == var2) {
            return var3;
         }
      }

      return -1;
   }

   private int findCraftingResultSlotId(class_1714 var1) {
      for (int var2 = 0; var2 < var1.field_7761.size(); var2++) {
         class_1735 var3 = (class_1735)var1.field_7761.get(var2);
         if (var3.field_7871 instanceof class_1731) {
            return var2;
         }
      }

      return 0;
   }

   private boolean hasAnyItemsInCraftGrid(class_1714 var1) {
      for (int var2 = 0; var2 < var1.field_7761.size(); var2++) {
         class_1735 var3 = (class_1735)var1.field_7761.get(var2);
         if (var3.field_7871 instanceof class_1715 && !var3.method_7677().method_7960()) {
            return true;
         }
      }

      return false;
   }

   private boolean hasCraftingInProgress(class_1714 var1) {
      if (var1 == null) {
         return false;
      } else if (this.hasAnyItemsInCraftGrid(var1)) {
         return true;
      } else {
         int var2 = this.findCraftingResultSlotId(var1);
         return var2 >= 0 && var2 < var1.field_7761.size() ? !((class_1735)var1.field_7761.get(var2)).method_7677().method_7960() : false;
      }
   }

   private boolean placeOneCraftIngredient(class_1714 var1, Predicate<class_1799> var2, int var3) {
      int var4 = this.findInventorySlotByPredicate(var2);
      if (var4 == -1) {
         return false;
      } else {
         int var5 = this.findPlayerSlotId(var1, var4);
         int var6 = this.findCraftingInputSlotId(var1, var3);
         return var5 != -1 && var6 != -1 ? this.moveCountToSlot(var1, var5, var6, 1) : false;
      }
   }

   private boolean takeCraftingResult(class_1714 var1, class_1792 var2) {
      return this.takeCraftingResult(var1, var2, null);
   }

   private boolean takeCraftingResult(class_1714 var1, class_1792 var2, Predicate<class_1799> var6) {
      int var3 = this.findCraftingResultSlotId(var1);
      if (var3 >= 0 && var3 < var1.field_7761.size()) {
         class_1799 var4 = ((class_1735)var1.field_7761.get(var3)).method_7677();
         if (var4.method_7960()) {
            return false;
         } else if (var2 != null && var4.method_7909() != var2) {
            this.clearCraftingGrid(var1);
            return false;
         } else if (var6 != null && !var6.test(var4)) {
            this.clearCraftingGrid(var1);
            return false;
         } else {
            mc.field_1761.method_2906(var1.field_7763, var3, 0, class_1713.field_7794, mc.field_1724);
            class_1799 var5 = ((class_1735)var1.field_7761.get(var3)).method_7677();
            if (!var5.method_7960()) {
               mc.field_1761.method_2906(var1.field_7763, var3, 0, class_1713.field_7790, mc.field_1724);
               this.clearCursorToInventory(var1);
            }

            this.clearCursorSafely(var1, -1);
            return ((class_1735)var1.field_7761.get(var3)).method_7677().method_7960();
         }
      } else {
         return false;
      }
   }

   private boolean isPickaxeRecipePlacedInGrid() {
      return mc.field_1755 instanceof class_465 var2 && var2.method_17577() instanceof class_1714 var3 ? this.isPickaxeRecipePlaced(var3) : false;
   }

   private boolean isSwordRecipePlacedInGrid() {
      return mc.field_1755 instanceof class_465 var2 && var2.method_17577() instanceof class_1714 var3 ? this.isSwordRecipePlaced(var3) : false;
   }

   private boolean isPickaxeRecipePlaced(class_1714 var1) {
      return this.isGridItem(var1, 0, class_1802.field_8477)
         && this.isGridItem(var1, 1, class_1802.field_8477)
         && this.isGridItem(var1, 2, class_1802.field_8477)
         && this.isGridItem(var1, 4, class_1802.field_8600)
         && this.isGridItem(var1, 7, class_1802.field_8600);
   }

   private boolean isSwordRecipePlaced(class_1714 var1) {
      return this.isGridItem(var1, 1, class_1802.field_8477)
         && this.isGridItem(var1, 4, class_1802.field_8477)
         && this.isGridItem(var1, 7, class_1802.field_8600);
   }

   private boolean isGridItem(class_1714 var1, int var2, class_1792 var3) {
      int var4 = this.findCraftingInputSlotId(var1, var2);
      if (var4 >= 0 && var4 < var1.field_7761.size()) {
         class_1799 var5 = ((class_1735)var1.field_7761.get(var4)).method_7677();
         return !var5.method_7960() && var5.method_7909() == var3;
      } else {
         return false;
      }
   }

   private int countItemInCraftGrid(class_1714 var1, class_1792 var2) {
      int var3 = 0;

      for (int var4 = 0; var4 < var1.field_7761.size(); var4++) {
         class_1735 var5 = (class_1735)var1.field_7761.get(var4);
         if (var5.field_7871 instanceof class_1715) {
            class_1799 var6 = var5.method_7677();
            if (!var6.method_7960() && var6.method_7909() == var2) {
               var3 += var6.method_7947();
            }
         }
      }

      return var3;
   }

   private int countAvailableForCraft(class_1792 var1) {
      int var2 = this.countItem(var1);
      if (mc.field_1755 instanceof class_465 var4 && var4.method_17577() instanceof class_1714 var5) {
         var2 += this.countItemInCraftGrid(var5, var1);
      }

      return var2;
   }

   private int findPickaxeSlotWithTarget() {
      for (int var1 = 0; var1 < mc.field_1724.method_31548().method_5439(); var1++) {
         class_1799 var2 = mc.field_1724.method_31548().method_5438(var1);
         if (!var2.method_7960() && this.isTargetOutputItem(var2) && this.hasTargetEnchant(var2) && !this.hasBadEnchant(var2)) {
            return var1;
         }
      }

      return -1;
   }

   private int findFinalDiamondSwordSlot() {
      if (!this.isSwordMode()) {
         return -1;
      } else {
         for (int var1 = 0; var1 < mc.field_1724.method_31548().method_5439(); var1++) {
            class_1799 var2 = mc.field_1724.method_31548().method_5438(var1);
            if (!var2.method_7960() && var2.method_7909() == class_1802.field_8802 && this.isFinalSword(var2) && !this.hasBadEnchant(var2)) {
               return var1;
            }
         }

         return -1;
      }
   }

   private int findPickaxeSlotWithBad() {
      for (int var1 = 0; var1 < mc.field_1724.method_31548().method_5439(); var1++) {
         class_1799 var2 = mc.field_1724.method_31548().method_5438(var1);
         if (!var2.method_7960()
            && this.isTargetOutputItem(var2)
            && (!this.isSwordMode() || !this.isSharpnessSword(var2))
            && this.hasBadEnchant(var2)) {
            return var1;
         }
      }

      return -1;
   }

   private int findPickaxeSlotWithAnyEnchant() {
      return this.findPickaxeSlotWithAnyEnchant(false);
   }

   private int findPickaxeSlotWithAnyEnchant(boolean var1) {
      for (int var2 = 0; var2 < mc.field_1724.method_31548().method_5439(); var2++) {
         class_1799 var3 = mc.field_1724.method_31548().method_5438(var2);
         if (!var3.method_7960()
            && this.isTargetOutputItem(var3)
            && (!this.isSwordMode() || this.isGrindableSword(var3))
            && (!var1 || !this.hasTargetEnchant(var3))
            && this.hasAnyEnchant(var3)) {
            return var2;
         }
      }

      return -1;
   }

   private int findUnenchantedPickaxeSlot() {
      for (int var1 = 0; var1 < mc.field_1724.method_31548().method_5439(); var1++) {
         class_1799 var2 = mc.field_1724.method_31548().method_5438(var1);
         if (!var2.method_7960() && this.isTargetBaseItem(var2) && !this.hasAnyEnchant(var2)) {
            return var1;
         }
      }

      return -1;
   }

   private boolean hasEnchantNeedles(class_1799 var1, String[] var2, boolean var3) {
      if (!var1.method_7960() && var2 != null && var2.length != 0) {
         for (String var5 : this.classifier.getLoreLines(var1)) {
            if (this.lineHasNeedle(var5, var2, var3)) {
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   private boolean lineHasNeedle(String var1, String[] var2, boolean var3) {
      if (var1 != null && !var1.isBlank()) {
         String var4 = AutoEnchanterText.normalizeLettersOnly(AutoEnchanterText.stripFormatting(var1));
         return AutoEnchanterText.containsAnyNeedle(var4, var2) && (!var3 || this.lineHasLevelTwo(var1));
      } else {
         return false;
      }
   }

   private boolean hasBadEnchant(class_1799 var1) {
      if (var1.method_7960()) {
         return false;
      } else if (AutoEnchanterText.isBadEnchantText(var1.method_7964().getString())) {
         return true;
      } else {
         for (String var3 : this.classifier.getLoreLines(var1)) {
            if (AutoEnchanterText.isBadEnchantText(var3)) {
               return true;
            }
         }

         for (Entry var5 : var1.method_58657().method_57539()) {
            if (var5.getKey() != null
               && var5.getValue() > 0
               && AutoEnchanterText.isBadEnchantText(((class_1887)((class_6880)var5.getKey()).comp_349()).comp_2686().getString())) {
               return true;
            }
         }

         return false;
      }
   }

   private boolean hasKnockbackEnchant(class_1799 var1) {
      if (var1.method_7960()) {
         return false;
      } else if (AutoEnchanterText.isKnockbackText(var1.method_7964().getString())) {
         return true;
      } else {
         for (String var3 : this.classifier.getLoreLines(var1)) {
            if (AutoEnchanterText.isKnockbackText(var3)) {
               return true;
            }
         }

         for (Entry var5 : var1.method_58657().method_57539()) {
            if (var5.getKey() != null
               && var5.getValue() > 0
               && AutoEnchanterText.isKnockbackText(((class_1887)((class_6880)var5.getKey()).comp_349()).comp_2686().getString())) {
               return true;
            }
         }

         return false;
      }
   }

   private boolean hasAnyEnchant(class_1799 var1) {
      if (this.hasTargetEnchant(var1)) {
         return true;
      } else if (!var1.method_58657().method_57534().isEmpty()) {
         return true;
      } else {
         for (String var3 : this.classifier.getLoreLines(var1)) {
            if (AutoEnchanterText.lineHasEnchantLevel(var3)) {
               return true;
            }
         }

         return false;
      }
   }

   private boolean hasAnvilCandidates() {
      return this.findAnvilCandidates() != null;
   }

   private boolean shouldUpgradeFinalSword() {
      if (!this.isSwordMode()) {
         return false;
      } else {
         int var1 = this.findPickaxeSlotWithTarget();
         if (var1 == -1) {
            return false;
         } else {
            class_1799 var2 = mc.field_1724.method_31548().method_5438(var1);
            return this.isFinalSword(var2) && var2.method_7909() == class_1802.field_8802 && this.countItem(class_1802.field_22020) > 0;
         }
      }
   }

   private double computePickaxeQualityScore(class_1799 var1) {
      if (!var1.method_7960() && this.isTargetOutputItem(var1) && !this.hasBadEnchant(var1)) {
         double var2 = this.getDurabilityRatio(var1);
         if (var2 < 0.35) {
            return 0.0;
         } else {
            int var4 = this.countPickaxeEnchantLines(var1);
            if (var4 == 0 && !var1.method_58657().method_57534().isEmpty()) {
               var4 = 1;
            }

            if (var4 == 0 && this.hasTargetEnchant(var1)) {
               var4 = 1;
            }

            return 1.0 + var2 + var4 * 0.5;
         }
      } else {
         return 0.0;
      }
   }

   private int countPickaxeEnchantLines(class_1799 var1) {
      int var2 = 0;

      for (String var4 : this.classifier.getLoreLines(var1)) {
         if (this.isLikelyEnchantLine(var4)) {
            var2++;
         }
      }

      return var2;
   }

   private boolean isLikelyEnchantLine(String var1) {
      String var2 = AutoEnchanterText.stripFormatting(var1).trim();
      if (var2.isEmpty()) {
         return false;
      } else {
         String var3 = AutoEnchanterText.normalizeLettersOnly(var2);
         if (var3.contains(AutoEnchanterText.normalizeLettersOnly("когда"))
            || var3.contains("when")
            || var3.contains(AutoEnchanterText.normalizeLettersOnly("прочн"))
            || var3.contains("durability")
            || var3.contains(AutoEnchanterText.normalizeLettersOnly("цена"))
            || var3.contains("price")
            || var2.contains("$")) {
            return false;
         } else if (var3.contains("attack")
            || var3.contains("damage")
            || var3.contains("speed")
            || var3.contains(AutoEnchanterText.normalizeLettersOnly("урон"))
            || var3.contains(AutoEnchanterText.normalizeLettersOnly("скорост"))) {
            return false;
         } else if (var3.contains(AutoEnchanterText.normalizeLettersOnly("продав"))
            || var3.contains("seller")
            || var3.contains("owner")
            || var3.contains(AutoEnchanterText.normalizeLettersOnly("остал"))
            || var3.contains("time")
            || var3.contains("ends")
            || var3.contains(AutoEnchanterText.normalizeLettersOnly("аук"))
            || var3.contains("auction")) {
            return false;
         } else {
            return AutoEnchanterText.containsAnyNeedle(var3, this.getTargetEnchantNeedles())
               ? true
               : !var3.startsWith(AutoEnchanterText.normalizeLettersOnly("Нест"))
                  && !var3.startsWith(AutoEnchanterText.normalizeLettersOnly("Тяж"))
                  && !var3.contains(AutoEnchanterText.normalizeLettersOnly("Нестабильн"))
                  && !var3.contains("unstable")
                  && AutoEnchanterText.lineHasEnchantLevel(var2);
         }
      }
   }

   private boolean isBadAuctionPickaxe(class_1799 var1) {
      return this.hasBadEnchant(var1) || this.isLowDurability(var1);
   }

   private boolean isLowDurability(class_1799 var1) {
      if (!var1.method_7963()) {
         return false;
      } else {
         int var2 = var1.method_7936();
         return var2 > 0 && (float)(var2 - var1.method_7919()) / var2 < 0.35F;
      }
   }

   private double getDurabilityRatio(class_1799 var1) {
      if (!var1.method_7963()) {
         return 1.0;
      } else {
         int var2 = var1.method_7936();
         return var2 <= 0 ? 1.0 : Math.max(0.0, Math.min(1.0, (double)(var2 - var1.method_7919()) / var2));
      }
   }

   private boolean lineHasLevelTwo(String var1) {
      return ENCHANT_LEVEL_TWO_PATTERN.matcher(AutoEnchanterText.stripFormatting(var1)).find();
   }

   private long findCheapestUnitPrice(String var1) {
      if (!(mc.field_1755 instanceof class_465 var3)) {
         return -1L;
      } else {
         class_1703 var4 = var3.method_17577();
         String var5 = AutoEnchanterText.normalizeLettersOnly(var1);
         long var6 = Long.MAX_VALUE;

         for (int var8 = 0; var8 < var4.field_7761.size(); var8++) {
            class_1799 var13 = ((class_1735)var4.field_7761.get(var8)).method_7677();
            long var9;
            long var11;
            if (!var13.method_7960()
               && !this.isDecorative(var13.method_7909())
               && this.matchesQuery(var13, var5)
               && (var11 = this.extractPrice(var13)) > 0L
               && (var9 = Math.max(1L, var11 / Math.max(1, var13.method_7947()))) < var6) {
               if (var13.method_7909() == class_1802.field_8287) {
                  long var14 = var9 * 64L;
                  if (var14 < 1L || var14 > this.getLongInput(this.maxXpPriceStackInput, 50000L, 1L, 15000000L)) {
                     continue;
                  }
               } else if (!this.isMaterialAuctionPriceAllowed(var13, var11)) {
                  continue;
               }

               var6 = var9;
            }
         }

         return var6 == Long.MAX_VALUE ? -1L : var6;
      }
   }

   private long findCheapestTargetPickaxePrice(String var1) {
      if (!(mc.field_1755 instanceof class_465 var3)) {
         return -1L;
      } else {
         class_1703 var4 = var3.method_17577();
         String var5 = AutoEnchanterText.normalizeLettersOnly(var1);
         String[] var6 = this.getTargetEnchantNeedles();
         boolean var7 = this.targetRequiresLevelTwo();
         if (this.sellPriceMode.is(this.priceMarket) && this.isPickaxeMode()) {
            var6 = MARKET_ENCHANT_NEEDLES;
            var7 = true;
         }

         long var8 = Long.MAX_VALUE;
         double var10 = Double.MAX_VALUE;
         this.outputPricePerQuality = -1.0;

         for (int var12 = 0; var12 < var4.field_7761.size(); var12++) {
            class_1799 var19 = ((class_1735)var4.field_7761.get(var12)).method_7677();
            long var15;
            double var17;
            if (!var19.method_7960()
               && !this.isDecorative(var19.method_7909())
               && this.matchesQuery(var19, var5)
               && this.isTargetOutputItem(var19)
               && this.hasEnchantNeedles(var19, var6, var7)
               && !this.isBadAuctionPickaxe(var19)
               && (var17 = this.computePickaxeQualityScore(var19)) > 0.0
               && (var15 = this.extractPrice(var19)) > 0L) {
               long var20 = Math.max(1L, var15 / Math.max(1, var19.method_7947()));
               if (var20 < var8) {
                  var8 = var20;
               }

               double var13;
               if ((var13 = var20 / var17) < var10) {
                  var10 = var13;
               }
            }
         }

         if (var10 != Double.MAX_VALUE) {
            this.outputPricePerQuality = var10;
         }

         return var8 == Long.MAX_VALUE ? -1L : var8;
      }
   }

   private int findCheapestSlot(class_1703 var1, String var2, boolean var3, int var4) {
      String var5 = AutoEnchanterText.normalizeLettersOnly(var2);
      int var16 = this.getAuctionListingSlotLimit(var4);
      long var6 = Long.MAX_VALUE;
      int var8 = -1;

      // ---- PATCH: sword-search diagnostics (why is no sword matched?) ----
      if (this.isSwordMode() && AutoEnchanterText.isSwordAuctionNeedle(var5)) {
         int diagNonEmpty = 0;
         int diagSwords = 0;
         for (int d = 0; d < var16; d++) {
            class_1799 ds = ((class_1735) var1.field_7761.get(d)).method_7677();
            if (ds.method_7960()) continue;
            diagNonEmpty++;
            if (ds.method_7909() != class_1802.field_22022) continue;
            diagSwords++;
            long dprice = this.extractPrice(ds);
            moscow.mytheria.logger.MytheriaLogger.event("sword_candidate")
               .with("slot", d)
               .with("name", this.classifier.getItemName(ds))
               .with("price", dprice)
               .with("priceAllowed", this.isSwordAuctionPriceAllowed(ds))
               .with("sharpLevel", this.classifier.getSwordEnchantLevel(ds, SWORD_REQUIREMENTS[0]))
               .with("isSharp7", this.isSharpnessSword(ds))
               .with("isClean", this.isCleanNetheriteSword(ds))
               .with("hasBad", this.hasBadEnchant(ds))
               .emit();
         }
         moscow.mytheria.logger.MytheriaLogger.event("sword_search_scan")
            .with("query", var2)
            .with("slotLimit", var16)
            .with("nonEmpty", diagNonEmpty)
            .with("netheriteSwords", diagSwords)
            .with("haveSharpAlready", this.countSharpnessSwords())
            .emit();
      }
      // ---- END PATCH ----

      for (int var9 = 0; var9 < var16; var9++) {
         class_1799 var14 = ((class_1735)var1.field_7761.get(var9)).method_7677();
         long var10;
         long var12;
         if (!var14.method_7960()
            && !this.isDecorative(var14.method_7909())
            && this.matchesQuery(var14, var5)
            && (
               !this.isSwordMode()
                  || !AutoEnchanterText.isSwordAuctionNeedle(var5)
                  || this.isSwordAuctionPriceAllowed(var14)
                     && (this.countSharpnessSwords() <= 0 ? this.isSharpnessSword(var14) : this.isCleanNetheriteSword(var14))
            )
            && (!var3 || this.isTargetOutputItem(var14) && this.hasTargetEnchant(var14) && !this.isBadAuctionPickaxe(var14))
            && (var12 = this.extractPrice(var14)) > 0L
            && (var10 = Math.max(1L, var12 / Math.max(1, var14.method_7947()))) < var6) {
            if (var14.method_7909() == class_1802.field_8287) {
               long var15 = var10 * 64L;
               if (var15 < 1L || var15 > this.getLongInput(this.maxXpPriceStackInput, 50000L, 1L, 15000000L)) {
                  continue;
               }
            } else if (!this.isMaterialAuctionPriceAllowed(var14, var12)) {
               continue;
            }

            var6 = var10;
            var8 = var9;
         }
      }

      return var8;
   }

   private boolean isMaterialAuctionPriceAllowed(class_1799 var1, long var2) {
      if (var1.method_7909() == class_1802.field_8759) {
         return var2 <= this.getLongInput(this.maxLapisPriceInput, 100000L, 1L, 100000000L);
      } else if (var1.method_7909() == class_1802.field_8477) {
         return var2 <= this.getLongInput(this.maxDiamondPriceInput, 100000000L, 1L, 100000000L);
      } else if (var1.method_7909() == class_1802.field_22020) {
         return var2 <= this.getLongInput(this.maxNetheritePriceInput, 100000000L, 1L, 100000000L);
      } else {
         return !this.isLogItem(var1) && !this.isPlankItem(var1)
            || var2 <= this.getLongInput(this.maxWoodPriceInput, 100000000L, 1L, 100000000L);
      }
   }

   private boolean matchesQuery(class_1799 var1, String var2) {
      if (var2 == null || var2.isBlank()) {
         return false;
      } else if (AutoEnchanterText.isSwordAuctionNeedle(var2)) {
         return var1.method_7909() == class_1802.field_22022;
      } else if (AutoEnchanterText.isXpNeedle(var2)) {
         return var1.method_7909() == class_1802.field_8287;
      } else if (AutoEnchanterText.isWoodNeedle(var2)) {
         return this.isLogItem(var1) || this.isPlankItem(var1);
      } else if (var2.contains(AutoEnchanterText.normalizeLettersOnly("лазурит")) || var2.contains("lapis")) {
         return var1.method_7909() == class_1802.field_8759;
      } else if (!var2.contains(AutoEnchanterText.normalizeLettersOnly("алмаз")) && !var2.contains("diamond")) {
         if (!var2.contains(AutoEnchanterText.normalizeLettersOnly("незерит")) && !var2.contains("netherite")) {
            String var3 = AutoEnchanterText.normalizeLettersOnly(this.classifier.getItemName(var1));
            if (var3.contains(var2)) {
               return true;
            } else {
               for (String var5 : this.classifier.getLoreLines(var1)) {
                  if (AutoEnchanterText.normalizeLettersOnly(AutoEnchanterText.stripFormatting(var5)).contains(var2)) {
                     return true;
                  }
               }

               return false;
            }
         } else {
            return var1.method_7909() == class_1802.field_22020;
         }
      } else {
         return var1.method_7909() == class_1802.field_8477;
      }
   }

   private int getAuctionListingSlotLimit(int var1) {
      return Math.max(0, Math.min(var1, 45));
   }

   private boolean tryConfirmPurchase() {
      if (!(mc.field_1755 instanceof class_465 var3)) {
         return false;
      } else if (this.isSearchScreen()) {
         return false;
      } else {
         class_1703 var4 = var3.method_17577();
         if (!var4.method_34255().method_7960()) {
            this.clearCursorToInventory(var4);
            if (!var4.method_34255().method_7960()) {
               return false;
            }
         }

         int var1;
         if ((var1 = this.findConfirmSlot(var4)) == -1) {
            return false;
         } else {
            boolean var5 = this.clickMenuSlot(var4, var1);
            if (var5) {
               this.scheduleBuyClose();
            }

            return var5;
         }
      }
   }

   private boolean forceConfirmPurchaseFallback() {
      class_437 var1 = mc.field_1755;
      if (!(var1 instanceof class_465)) {
         return false;
      } else {
         class_1703 var2 = ((class_465)var1).method_17577();
         int var3 = this.findConfirmSlot(var2);
         if (var3 == -1) {
            var3 = this.findFallbackMenuActionSlot(var2);
         }

         if (var3 == -1) {
            return false;
         } else {
            this.clickMenuSlot(var2, var3);
            return true;
         }
      }
   }

   private int findFallbackMenuActionSlot(class_1703 var1) {
      int var2 = this.getContainerSlotCount(var1);

      for (int var3 = 0; var3 < var2; var3++) {
         class_1735 var4 = (class_1735)var1.field_7761.get(var3);
         if (var4 != null && var4.method_7681()) {
            class_1799 var5 = var4.method_7677();
            if (!var5.method_7960() && !this.isDecorative(var5.method_7909()) && this.hasKeywords(var5, "confirm", "подтверд", "buy", "purchase", "куп")) {
               return var3;
            }
         }
      }

      for (int var6 = 0; var6 < var2; var6++) {
         class_1735 var7 = (class_1735)var1.field_7761.get(var6);
         if (var7 != null && var7.method_7681()) {
            class_1799 var8 = var7.method_7677();
            if (!var8.method_7960() && !this.isDecorative(var8.method_7909())) {
               return var6;
            }
         }
      }

      return -1;
   }

   private int findConfirmSlot(class_1703 var1) {
      for (int var2 = 0; var2 < var1.field_7761.size(); var2++) {
         class_1735 var5 = (class_1735)var1.field_7761.get(var2);
         if (var5 != null && var5.method_7681()) {
            class_1799 var3 = var5.method_7677();
            class_1792 var4 = var3.method_7909();
            if (this.hasKeywords(var3, "подтверд", "confirm", "куп", "buy", "purchase")) {
               return var2;
            }

            if (var4 == class_1802.field_8131
               || var4 == class_1802.field_8408
               || var4 == class_1802.field_8581
               || var4 == class_1802.field_8656
               || var4 == class_1802.field_19049
               || var4 == class_1802.field_19057
               || var4 == class_1802.field_8733) {
               return var2;
            }
         }
      }

      return -1;
   }

   private String getXpSearchQuery() {
      return "Опыт с уровнем 15";
   }

   private String getWoodSearchQuery() {
      return "Дерево";
   }

   private long getLongInput(StringSetting var1, long var2, long var4, long var6) {
      if (var1 == null) {
         return Math.max(var4, Math.min(var6, var2));
      }

      String var8 = var1.getText();
      if (var8 == null) {
         return Math.max(var4, Math.min(var6, var2));
      }

      String var9 = var8.replaceAll("[^0-9]", "");
      if (var9.isBlank()) {
         return Math.max(var4, Math.min(var6, var2));
      }

      try {
         return Math.max(var4, Math.min(var6, Long.parseLong(var9)));
      } catch (NumberFormatException var10) {
         return Math.max(var4, Math.min(var6, var2));
      }
   }

   private String getAnarchyNumber() {
      if (this.anarchyNumberInput == null) {
         return "214";
      }

      String var1 = this.anarchyNumberInput.getText();
      if (var1 == null) {
         return "214";
      }

      String var2 = var1.replaceAll("[^0-9]", "");
      return var2.isBlank() ? "214" : var2;
   }

   private int getAccessibleInventorySize() {
      return Math.min(36, mc.field_1724.method_31548().method_5439());
   }

   private int findInventorySlot(class_1792 var1) {
      for (int var2 = 0; var2 < this.getAccessibleInventorySize(); var2++) {
         if (mc.field_1724.method_31548().method_5438(var2).method_7909() == var1) {
            return var2;
         }
      }

      return -1;
   }

   private int findInventorySlotByPredicate(Predicate<class_1799> var1) {
      for (int var2 = 0; var2 < this.getAccessibleInventorySize(); var2++) {
         class_1799 var3 = mc.field_1724.method_31548().method_5438(var2);
         if (!var3.method_7960() && var1.test(var3)) {
            return var2;
         }
      }

      return -1;
   }

   private int findHotbarSlot(class_1792 var1) {
      for (int var2 = 0; var2 < 9; var2++) {
         if (mc.field_1724.method_31548().method_5438(var2).method_7909() == var1) {
            return var2;
         }
      }

      return -1;
   }

   private int findEmptyHotbarSlot() {
      for (int var1 = 0; var1 < 9; var1++) {
         if (mc.field_1724.method_31548().method_5438(var1).method_7960()) {
            return var1;
         }
      }

      return -1;
   }

   private boolean moveSingleToHotbar(int var1, int var2) {
      class_1703 var3 = mc.field_1724.field_7512;
      if (var3 == null) {
         return false;
      } else {
         int var4 = this.inventoryIndexToSlotId(var1);
         int var5 = 36 + var2;
         if (!var3.method_34255().method_7960()) {
            this.clearCursorSafely(var3, -1);
         }

         if (!mc.field_1724.method_31548().method_5438(var2).method_7960()) {
            return false;
         } else {
            mc.field_1761.method_2906(var3.field_7763, var4, 0, class_1713.field_7790, mc.field_1724);
            mc.field_1761.method_2906(var3.field_7763, var5, 0, class_1713.field_7790, mc.field_1724);
            mc.field_1761.method_2906(var3.field_7763, var4, 0, class_1713.field_7790, mc.field_1724);
            boolean var6 = var3.method_34255().method_7960();
            return this.clearCursorSafely(var3, var4) && var6;
         }
      }
   }

   private boolean moveStackToSlot(class_1703 var1, int var2, int var3) {
      if (var1 != null && var2 >= 0 && var3 >= 0 && var2 < var1.field_7761.size() && var3 < var1.field_7761.size()) {
         if (!var1.method_34255().method_7960()) {
            this.clearCursorSafely(var1, -1);
         }

         if (((class_1735)var1.field_7761.get(var2)).method_7677().method_7960()) {
            return false;
         } else if (!((class_1735)var1.field_7761.get(var3)).method_7680(((class_1735)var1.field_7761.get(var2)).method_7677())) {
            return false;
         } else {
            mc.field_1761.method_2906(var1.field_7763, var2, 0, class_1713.field_7790, mc.field_1724);
            mc.field_1761.method_2906(var1.field_7763, var3, 0, class_1713.field_7790, mc.field_1724);
            mc.field_1761.method_2906(var1.field_7763, var2, 0, class_1713.field_7790, mc.field_1724);
            boolean var4 = var1.method_34255().method_7960();
            return this.clearCursorSafely(var1, var2) && var4;
         }
      } else {
         return false;
      }
   }

   private boolean clearCursorSafely(class_1703 var1, int var2) {
      if (var1 != null && !var1.method_34255().method_7960()) {
         if (var2 >= 0 && var2 < var1.field_7761.size()) {
            mc.field_1761.method_2906(var1.field_7763, var2, 0, class_1713.field_7790, mc.field_1724);
         }

         return var1.method_34255().method_7960();
      } else {
         return true;
      }
   }

   private boolean ensureLapisInSlot(class_1718 var1, int var2, int var3) {
      if (var1 == null) {
         return false;
      } else {
         if (!var1.method_34255().method_7960()) {
            this.clearCursorToInventory(var1);
            if (!var1.method_34255().method_7960()) {
               return false;
            }
         }

         class_1799 var6;
         int var7 = !(var6 = var1.method_7611(1).method_7677()).method_7960() && var6.method_7909() == class_1802.field_8759 ? var6.method_7947() : 0;
         if (var7 >= var3) {
            return true;
         } else {
            int var9 = this.countItem(class_1802.field_8759) + var7;
            if (var9 < var3) {
               return false;
            } else {
               int var10 = var3 - var7;
               int var4;
               class_1799 var5;
               if (var2 >= 0
                  && var2 < var1.field_7761.size()
                  && !(var5 = ((class_1735)var1.field_7761.get(var2)).method_7677()).method_7960()
                  && var5.method_7909() == class_1802.field_8759
                  && (var4 = Math.min(var10, var5.method_7947())) > 0) {
                  this.moveCountToSlot(var1, var2, 1, var4);
                  var7 = this.getLapisCount(var1);
                  var10 = var3 - var7;
               }

               for (int var11 = 0; var11 < this.getAccessibleInventorySize() && var7 < var3; var11++) {
                  class_1799 var14 = mc.field_1724.method_31548().method_5438(var11);
                  int var12;
                  int var13;
                  if (!var14.method_7960()
                     && var14.method_7909() == class_1802.field_8759
                     && (var13 = this.findPlayerSlotId(var1, var11)) != -1
                     && var13 != var2
                     && (var12 = Math.min(var10, var14.method_7947())) > 0) {
                     this.moveCountToSlot(var1, var13, 1, var12);
                     var7 = this.getLapisCount(var1);
                     var10 = var3 - var7;
                  }
               }

               return var7 >= var3;
            }
         }
      }
   }

   private boolean moveCountToSlot(class_1703 var1, int var2, int var3, int var4) {
      if (var2 < 0 || var3 < 0 || var4 <= 0 || var2 >= var1.field_7761.size() || var3 >= var1.field_7761.size()) {
         return false;
      } else if (!var1.method_34255().method_7960()) {
         return false;
      } else {
         class_1799 var5 = ((class_1735)var1.field_7761.get(var2)).method_7677();
         if (!var5.method_7960() && var5.method_7947() >= var4) {
            mc.field_1761.method_2906(var1.field_7763, var2, 0, class_1713.field_7790, mc.field_1724);
            int var6 = 0;

            for (int var7 = 0; var7 < var4 && !var1.method_34255().method_7960(); var7++) {
               mc.field_1761.method_2906(var1.field_7763, var3, 1, class_1713.field_7790, mc.field_1724);
               var6++;
            }

            boolean var8 = var6 == var4;
            return this.clearCursorSafely(var1, var2) && var8;
         } else {
            return false;
         }
      }
   }

   private int getLapisCount(class_1718 var1) {
      class_1799 var2 = var1.method_7611(1).method_7677();
      return !var2.method_7960() && var2.method_7909() == class_1802.field_8759 ? var2.method_7947() : 0;
   }

   private int getEnchantOptionIndex(int var1) {
      return Math.max(0, Math.min(2, var1 - 1));
   }

   private int findPlayerSlotId(class_1703 var1, int var2) {
      for (int var3 = 0; var3 < var1.field_7761.size(); var3++) {
         class_1735 var4 = (class_1735)var1.field_7761.get(var3);
         if (var4 != null && var4.field_7871 == mc.field_1724.method_31548() && var4.method_34266() == var2) {
            return var3;
         }
      }

      return -1;
   }

   private void compactHotbarToInventory() {
      if (mc.field_1755 == null && mc.field_1724.field_7512 != null) {
         class_1703 var1 = mc.field_1724.field_7512;

         for (int var2 = 0; var2 < 9; var2++) {
            if (!mc.field_1724.method_31548().method_5438(var2).method_7960()) {
               mc.field_1761.method_2906(var1.field_7763, 36 + var2, 0, class_1713.field_7794, mc.field_1724);
            }
         }

         if (!var1.method_34255().method_7960()) {
            this.clearCursorSafely(var1, -1);
         }
      }
   }

   private boolean isInventoryFull() {
      for (int var1 = 0; var1 < this.getAccessibleInventorySize(); var1++) {
         if (mc.field_1724.method_31548().method_5438(var1).method_7960()) {
            return false;
         }
      }

      return true;
   }

   private void stopForFullInventory() {
      MessageUtility.info(class_2561.method_43470("[AutoEnchanter] Inventory full. Stopping."));
      this.setEnabled(false);
   }

   private boolean closeHandledScreenSafely() {
      if (mc.field_1755 instanceof class_465 var2) {
         class_1703 var3 = var2.method_17577();
         if (this.clearCursorToInventory(var3) && var3.method_34255().method_7960()) {
            mc.field_1724.method_7346();
            return true;
         } else {
            return false;
         }
      } else {
         return true;
      }
   }

   private boolean clearCursorToInventory(class_1703 var1) {
      if (var1 == null) {
         return true;
      } else if (!var1.method_34255().method_7960()) {
         for (int var2 = 0; var2 < var1.field_7761.size(); var2++) {
            class_1735 var3 = (class_1735)var1.field_7761.get(var2);
            if (var3 != null && var3.field_7871 == mc.field_1724.method_31548() && var3.method_7677().method_7960()) {
               mc.field_1761.method_2906(var1.field_7763, var2, 0, class_1713.field_7790, mc.field_1724);
               return var1.method_34255().method_7960();
            }
         }

         this.stopForFullInventory();
         return false;
      } else {
         return true;
      }
   }

   private int countItem(class_1792 var1) {
      int var2 = 0;

      for (int var3 = 0; var3 < this.getAccessibleInventorySize(); var3++) {
         class_1799 var4 = mc.field_1724.method_31548().method_5438(var3);
         if (var4.method_7909() == var1) {
            var2 += var4.method_7947();
         }
      }

      return var2;
   }

   private int countPlanks() {
      int var1 = 0;

      for (int var2 = 0; var2 < this.getAccessibleInventorySize(); var2++) {
         class_1799 var3 = mc.field_1724.method_31548().method_5438(var2);
         if (!var3.method_7960() && this.isPlankItem(var3)) {
            var1 += var3.method_7947();
         }
      }

      return var1;
   }

   private int countLogs() {
      int var1 = 0;

      for (int var2 = 0; var2 < this.getAccessibleInventorySize(); var2++) {
         class_1799 var3 = mc.field_1724.method_31548().method_5438(var2);
         if (!var3.method_7960() && this.isLogItem(var3)) {
            var1 += var3.method_7947();
         }
      }

      return var1;
   }

   private boolean isPlankItem(class_1799 var1) {
      String var2 = AutoEnchanterText.normalizeLettersOnly(this.classifier.getItemName(var1));
      return var2.contains("plank") || var2.contains(AutoEnchanterText.normalizeLettersOnly("доск"));
   }

   private boolean isLogItem(class_1799 var1) {
      String var2 = AutoEnchanterText.normalizeLettersOnly(this.classifier.getItemName(var1));
      return var2.contains("log")
         || var2.contains("wood")
         || var2.contains(AutoEnchanterText.normalizeLettersOnly("бревн"))
         || var2.contains(AutoEnchanterText.normalizeLettersOnly("дерев"));
   }

   private boolean isSearchScreen() {
      if (!(mc.field_1755 instanceof class_465 var2)) {
         return false;
      } else {
         String var3 = AutoEnchanterText.normalizeLettersOnly(AutoEnchanterText.stripFormatting(var2.method_25440().getString()).toLowerCase(Locale.ROOT));
         return var3.contains(AutoEnchanterText.normalizeLettersOnly("поиск")) || var3.contains("search") || var3.contains(AutoEnchanterText.normalizeLettersOnly("аукцион"));
      }
   }

   private boolean isAuctionMainScreen() {
      if (!(mc.field_1755 instanceof class_465 var2)) {
         return false;
      } else {
         String var3 = AutoEnchanterText.normalizeLettersOnly(AutoEnchanterText.stripFormatting(var2.method_25440().getString()).toLowerCase(Locale.ROOT));
         return var3.contains(AutoEnchanterText.normalizeLettersOnly("аукцион")) || var3.contains("auction");
      }
   }

   private boolean isStorageScreen() {
      if (!(mc.field_1755 instanceof class_465 var2)) {
         return false;
      } else {
         String var3 = AutoEnchanterText.normalizeLettersOnly(AutoEnchanterText.stripFormatting(var2.method_25440().getString()).toLowerCase(Locale.ROOT));
         return var3.contains(AutoEnchanterText.normalizeLettersOnly("хранилище")) || var3.contains("storage");
      }
   }

   private boolean isConfirmScreen() {
      if (!(mc.field_1755 instanceof class_465 var2)) {
         return false;
      } else {
         String var3 = AutoEnchanterText.stripFormatting(var2.method_25440().getString()).toLowerCase(Locale.ROOT);
         String var4 = AutoEnchanterText.normalizeLettersOnly(var3);
         return !var4.contains(AutoEnchanterText.normalizeLettersOnly("подтверд")) && !var4.contains(AutoEnchanterText.normalizeLettersOnly("куп"))
            ? var3.contains("confirm") || var3.contains("buy") || var3.contains("purchase")
            : true;
      }
   }

   private void clickContainerSlot(class_1703 var1, int var2) {
      if (var1 != null && var2 >= 0 && var2 < var1.field_7761.size()) {
         class_1799 var3 = ((class_1735)var1.field_7761.get(var2)).method_7677();
         if (!var3.method_7960() && !this.isDecorative(var3.method_7909())) {
            if (!var1.method_34255().method_7960()) {
               this.clearCursorToInventory(var1);
               if (!var1.method_34255().method_7960()) {
                  return;
               }
            }

            mc.field_1761.method_2906(var1.field_7763, var2, 0, class_1713.field_7790, mc.field_1724);
            if (!var1.method_34255().method_7960()) {
               this.clearCursorToInventory(var1);
            }
         }
      }
   }

   private boolean clickMenuSlot(class_1703 var1, int var2) {
      if (var1 != null && var2 >= 0 && var2 < var1.field_7761.size()) {
         if (!var1.method_34255().method_7960()) {
            this.clearCursorToInventory(var1);
            if (!var1.method_34255().method_7960()) {
               return false;
            }
         }

         mc.field_1761.method_2906(var1.field_7763, var2, 0, class_1713.field_7790, mc.field_1724);
         if (var1.method_34255().method_7960()) {
            return true;
         } else {
            mc.field_1761.method_2906(var1.field_7763, var2, 0, class_1713.field_7790, mc.field_1724);
            if (var1.method_34255().method_7960()) {
               return true;
            } else {
               this.clearCursorToInventory(var1);
               return var1.method_34255().method_7960();
            }
         }
      } else {
         return false;
      }
   }

   private boolean isDecorative(class_1792 var1) {
      return var1 == class_1802.field_8157
         || var1 == class_1802.field_8871
         || var1 == class_1802.field_8240
         || var1 == class_1802.field_8736
         || var1 == class_1802.field_8077;
   }

   private int findSlotByItem(class_1703 var1, class_1792 var2, String... var3) {
      for (int var4 = 0; var4 < var1.field_7761.size(); var4++) {
         class_1799 var5 = ((class_1735)var1.field_7761.get(var4)).method_7677();
         if (!var5.method_7960() && var5.method_7909() == var2 && (var3.length == 0 || this.hasKeywords(var5, var3))) {
            return var4;
         }
      }

      return -1;
   }

   private int findSlotByKeyword(class_1703 var1, String... var2) {
      for (int var3 = 0; var3 < var1.field_7761.size(); var3++) {
         class_1799 var4 = ((class_1735)var1.field_7761.get(var3)).method_7677();
         if (!var4.method_7960() && this.hasKeywords(var4, var2)) {
            return var3;
         }
      }

      return -1;
   }

   private boolean hasKeywords(class_1799 var1, String... var2) {
      String var3 = AutoEnchanterText.stripFormatting(var1.method_7964().getString()).toLowerCase(Locale.ROOT);
      String var4 = AutoEnchanterText.normalizeLettersOnly(var3);

      for (String var8 : var2) {
         if (var3.contains(var8)) {
            return true;
         }

         String var9 = AutoEnchanterText.normalizeLettersOnly(var8);
         if (!var9.isEmpty() && var4.contains(var9)) {
            return true;
         }
      }

      for (String var15 : this.classifier.getLoreLines(var1)) {
         String var16 = AutoEnchanterText.stripFormatting(var15).toLowerCase(Locale.ROOT);
         String var17 = AutoEnchanterText.normalizeLettersOnly(var16);

         for (String var12 : var2) {
            if (var16.contains(var12)) {
               return true;
            }

            String var13 = AutoEnchanterText.normalizeLettersOnly(var12);
            if (!var13.isEmpty() && var17.contains(var13)) {
               return true;
            }
         }
      }

      return false;
   }

   private long extractPrice(class_1799 var1) {
      for (String var3 : this.classifier.getLoreLines(var1)) {
         String var6 = AutoEnchanterText.stripFormatting(var3).toLowerCase(Locale.ROOT);
         long var4;
         if ((var6.contains("$") || var6.contains("price") || AutoEnchanterText.normalizeLettersOnly(var6).contains(AutoEnchanterText.normalizeLettersOnly("цена")))
            && (var4 = this.parseMoney(var6)) > 0L) {
            return var4;
         }
      }

      return -1L;
   }

   private long parseMoney(String var1) {
      if (var1 != null && !var1.isBlank()) {
         Matcher var2 = PRICE_PATTERN.matcher(var1.replace(' ', ' '));
         long var3 = -1L;

         while (var2.find()) {
            String var5 = var2.group(1).replaceAll("[^0-9]", "");
            if (!var5.isBlank()) {
               try {
                  var3 = Long.parseLong(var5);
               } catch (NumberFormatException var7) {
                  return -1L;
               }
            }
         }

         return var3;
      } else {
         return -1L;
      }
   }

   private String buildSearchName(class_1799 var1) {
      return this.applySearchNameOverrides(var1, this.sanitizeSearchName(this.classifier.getItemName(var1), false));
   }

   private String applySearchNameOverrides(class_1799 var1, String var2) {
      return var1 != null && var1.method_7909() == class_1802.field_8287 ? "Bottle o` Enchanting" : var2;
   }

   private String sanitizeSearchName(String var1, boolean var2) {
      String var3 = AutoEnchanterText.stripFormatting(var1).replace("\"", "").replaceAll("\\s*\\[[^\\]]*\\]$", "");
      return var2 ? this.stripToLettersDigitsAndSpaces(var3) : this.stripToLettersAndSpaces(var3);
   }

   private String stripToLettersAndSpaces(String var1) {
      return var1 == null ? "" : var1.replaceAll("[^\\p{L}\\s]+", " ").replaceAll("\\s{2,}", " ").trim();
   }

   private String stripToLettersDigitsAndSpaces(String var1) {
      return var1 == null ? "" : var1.replaceAll("[^\\p{L}\\p{N}\\s]+", " ").replaceAll("\\s{2,}", " ").trim();
   }

   private int inventoryIndexToSlotId(int var1) {
      return var1 >= 0 && var1 <= 8 ? 36 + var1 : var1;
   }

   private boolean isTargetOutputItem(class_1799 var1) {
      if (var1.method_7960()) {
         return false;
      } else {
         return this.isSwordMode()
            ? var1.method_7909() == class_1802.field_8802 || var1.method_7909() == class_1802.field_22022
            : var1.method_7909() == this.getTargetOutputItem();
      }
   }

   private class_2338 findNearestBlock(class_2248 var1, int var2) {
      class_2338 var3 = mc.field_1724.method_24515();
      double var4 = Double.MAX_VALUE;
      class_2338 var6 = null;

      for (int var7 = -var2; var7 <= var2; var7++) {
         for (int var8 = -var2; var8 <= var2; var8++) {
            for (int var9 = -var2; var9 <= var2; var9++) {
               class_2338 var12 = var3.method_10069(var7, var8, var9);
               double var10;
               if (mc.field_1687.method_8320(var12).method_26204() == var1 && (var10 = var3.method_10262(var12)) < var4) {
                  var4 = var10;
                  var6 = var12;
               }
            }
         }
      }

      return var6;
   }

   private class_2338 findNearestAnvilBlock(int var1) {
      class_2338 var2 = mc.field_1724.method_24515();
      double var3 = Double.MAX_VALUE;
      class_2338 var5 = null;
      class_2248[] var6 = new class_2248[]{class_2246.field_10535, class_2246.field_10105, class_2246.field_10414};

      for (int var7 = -var1; var7 <= var1; var7++) {
         for (int var8 = -var1; var8 <= var1; var8++) {
            for (int var9 = -var1; var9 <= var1; var9++) {
               class_2338 var10 = var2.method_10069(var7, var8, var9);
               class_2248 var11 = mc.field_1687.method_8320(var10).method_26204();

               for (class_2248 var15 : var6) {
                  if (var11 == var15) {
                     double var16 = var2.method_10262(var10);
                     if (var16 < var3) {
                        var3 = var16;
                        var5 = var10;
                     }
                     break;
                  }
               }
            }
         }
      }

      return var5;
   }

   private void beginBlockOpen(class_2338 var1, AutoEnchanter.State var2) {
      if (var1 != null) {
         this.pendingOpenBlockPos = var1.method_10062();
         this.pendingOpenState = var2;
         this.pendingOpenAttempts = 10;
         this.pendingOpenNextAttemptAtMs = System.currentTimeMillis() + 120L;
         this.openBlock(this.pendingOpenBlockPos);
      }
   }

   private void handleOpenBlockRetry() {
      if (this.pendingOpenBlockPos != null) {
         if (mc.field_1755 != null) {
            this.clearOpenBlockRetry();
         } else if (this.pendingOpenState == null || this.state != this.pendingOpenState) {
            this.clearOpenBlockRetry();
         } else if (this.pendingOpenAttempts <= 0) {
            this.clearOpenBlockRetry();
         } else {
            long var1 = System.currentTimeMillis();
            if (var1 >= this.pendingOpenNextAttemptAtMs) {
               this.openBlock(this.pendingOpenBlockPos);
               this.pendingOpenAttempts--;
               this.pendingOpenNextAttemptAtMs = var1 + 120L;
            }
         }
      }
   }

   private void clearOpenBlockRetry() {
      this.pendingOpenBlockPos = null;
      this.pendingOpenState = null;
      this.pendingOpenAttempts = 0;
      this.pendingOpenNextAttemptAtMs = 0L;
      this.renderRotationActive = false;
   }

   private void openBlock(class_2338 var1) {
      if (var1 != null) {
         this.buyClosePending = false;
         this.relistEscPending = false;
         this.auctionStuckActive = false;
         this.auctionStuckTitle = "";
         this.lookAtPos(var1);
         class_243 var2 = class_243.method_24953(var1);
         double var3 = mc.field_1724.method_23317() - (var1.method_10263() + 0.5);
         double var5 = mc.field_1724.method_23318() + mc.field_1724.method_18381(mc.field_1724.method_18376()) - (var1.method_10264() + 0.5);
         double var7 = mc.field_1724.method_23321() - (var1.method_10260() + 0.5);
         class_2350 var9 = class_2350.method_10142(var3, var5, var7);
         class_243 var10 = var2.method_1019(class_243.method_24954(var9.method_62675()).method_1021(0.5));
         class_3965 var11 = new class_3965(var10, var9, var1, false);
         if (mc.field_1761 != null) {
            mc.field_1761.method_2896(mc.field_1724, class_1268.field_5808, var11);
         }

         mc.field_1724.method_6104(class_1268.field_5808);
      }
   }

   private void lookAtPos(class_2338 var1) {
      class_243 var2 = this.getBlockAimPoint(var1);
      double var3 = var2.field_1352 - mc.field_1724.method_23317();
      double var5 = var2.field_1350 - mc.field_1724.method_23321();
      double var7 = var2.field_1351 - (mc.field_1724.method_23318() + mc.field_1724.method_18381(mc.field_1724.method_18376()));
      double var9 = Math.sqrt(var3 * var3 + var5 * var5);
      this.requestRenderRotation((float)(Math.atan2(var5, var3) * 180.0 / Math.PI) - 90.0F, (float)(-(Math.atan2(var7, var9) * 180.0 / Math.PI)), true);
      this.updateRenderRotation();
   }

   private class_243 getBlockAimPoint(class_2338 var1) {
      double var2 = 0.18;
      double var4 = (Math.random() - 0.5) * var2 * 2.0;
      double var6 = (Math.random() - 0.5) * var2 * 1.4;
      double var8 = (Math.random() - 0.5) * var2 * 2.0;
      return class_243.method_24953(var1).method_1031(var4, var6, var8);
   }

   private void rotateToAnglesSmooth(float var1, float var2, boolean var3) {
      this.requestRenderRotation(var1, var2, var3);
      this.updateRenderRotation();
   }

   private void requestRenderRotation(float var1, float var2, boolean var3) {
      this.renderRotationActive = true;
      this.renderTargetYaw = var1;
      this.renderTargetPitch = Math.max(-90.0F, Math.min(90.0F, var2));
      this.renderRotationJitter = var3;
   }

   private void updateRenderRotation() {
      if (this.renderRotationActive && mc.field_1724 != null) {
         float var1 = this.renderTargetYaw;
         float var2 = this.renderTargetPitch;
         if (this.renderRotationJitter) {
            var1 += ((float)Math.random() * 2.0F - 1.0F) * 0.35F;
            var2 += ((float)Math.random() * 2.0F - 1.0F) * 0.25F;
            var2 = Math.max(-90.0F, Math.min(90.0F, var2));
         }

         float var3 = mc.field_1724.method_36454();
         float var4 = mc.field_1724.method_36455();
         float var5 = this.wrapDegrees(var1 - var3);
         float var6 = var2 - var4;
         float var7 = (float)Math.hypot(Math.abs(var5), Math.abs(var6));
         if (var7 < 0.02F) {
            this.renderRotationActive = false;
         } else {
            float var8 = (float)(this.getRotationDeltaTime() * 60.0);
            float var9 = (float)class_3532.method_15350(28.0F * var8 * this.getRotationSpeedMultiplier(var7), 2.0, 82.0);
            float var10 = var3 + this.clamp(var5, -var9, var9);
            float var11 = var4 + this.clamp(var6, -var9, var9);
            var11 = Math.max(-90.0F, Math.min(90.0F, var11));
            mc.field_1724.method_36456(var10);
            mc.field_1724.method_5847(var10);
            mc.field_1724.method_36457(var11);
         }
      }
   }

   private float wrapDegrees(float var1) {
      float var2 = var1 % 360.0F;
      if (var2 >= 180.0F) {
         var2 -= 360.0F;
      }

      if (var2 < -180.0F) {
         var2 += 360.0F;
      }

      return var2;
   }

   private float clamp(float var1, float var2, float var3) {
      return Math.max(var2, Math.min(var3, var1));
   }

   private double getRotationDeltaTime() {
      long var1 = System.nanoTime();
      double var3 = this.lastRotationFrameTimeNs > 0L ? (var1 - this.lastRotationFrameTimeNs) / 1.0E9 : 0.016;
      this.lastRotationFrameTimeNs = var1;
      return class_3532.method_15350(var3, 0.001, 0.1);
   }

   private float getRotationSpeedMultiplier(float var1) {
      if (var1 < 3.0F) {
         return 0.7F;
      } else if (var1 < 10.0F) {
         return 1.0F;
      } else {
         return var1 < 25.0F ? 1.25F : 1.55F;
      }
   }

   private void startRelistCycle() {
      this.relistPending = true;
      this.relistNextAtMs = 0L;
      this.relistFlowTimer.reset();
   }

   private void stopRelistCycle() {
      this.relistPending = false;
      this.relistNextAtMs = 0L;
      if (this.isRelistState()) {
         this.closeHandledScreenSafely();
         this.setState(AutoEnchanter.State.IDLE);
      }
   }

   private boolean shouldStartRelist() {
      return this.relistNextAtMs <= 0L || System.currentTimeMillis() >= this.relistNextAtMs;
   }

   private boolean isRelistState() {
      return this.state == AutoEnchanter.State.RELIST_OPEN_AH
         || this.state == AutoEnchanter.State.RELIST_WAIT_AH
         || this.state == AutoEnchanter.State.RELIST_CLICK_STORAGE
         || this.state == AutoEnchanter.State.RELIST_WAIT_STORAGE
         || this.state == AutoEnchanter.State.RELIST_CLICK_RELIST;
   }

   private void scheduleRelistEscape() {
      this.relistEscPending = true;
      this.relistEscAtMs = System.currentTimeMillis() + 200L;
      this.relistEscUntilMs = System.currentTimeMillis() + 2000L;
   }

   private void handleRelistEscape() {
      if (this.relistEscPending) {
         long var1 = System.currentTimeMillis();
         if (!this.isSearchScreen() && !this.isAuctionMainScreen() && !this.isStorageScreen()) {
            this.relistEscPending = false;
         } else {
            if (var1 >= this.relistEscAtMs) {
               if (mc.field_1755 instanceof class_465) {
                  if (!this.closeHandledScreenSafely()) {
                     mc.field_1755.method_25419();
                  }
               } else if (mc.field_1755 != null) {
                  mc.field_1755.method_25419();
               }

               if (mc.field_1755 != null && var1 < this.relistEscUntilMs) {
                  this.relistEscAtMs = var1 + 200L;
               } else {
                  this.relistEscPending = false;
               }
            }
         }
      }
   }

   private void scheduleBuyClose() {
      this.buyClosePending = true;
      this.buyCloseAtMs = System.currentTimeMillis() + 200L;
      this.buyCloseUntilMs = System.currentTimeMillis() + 2000L;
   }

   private void handleBuyClose() {
      if (this.buyClosePending) {
         long var1 = System.currentTimeMillis();
         if (!this.isSearchScreen() && !this.isAuctionMainScreen() && !this.isStorageScreen() && !this.isConfirmScreen()) {
            this.buyClosePending = false;
         } else {
            if (var1 >= this.buyCloseAtMs) {
               if (mc.field_1755 instanceof class_465) {
                  if (!this.closeHandledScreenSafely()) {
                     mc.field_1755.method_25419();
                  }
               } else if (mc.field_1755 != null) {
                  mc.field_1755.method_25419();
               }

               if (mc.field_1755 != null && var1 < this.buyCloseUntilMs) {
                  this.buyCloseAtMs = var1 + 200L;
               } else {
                  this.buyClosePending = false;
               }
            }
         }
      }
   }

   private void handleAuctionStuckCheck() {
      if (!(mc.field_1755 instanceof class_465 var2)) {
         this.auctionStuckActive = false;
         this.auctionStuckTitle = "";
      } else {
         String var3 = AutoEnchanterText.normalizeLettersOnly(AutoEnchanterText.stripFormatting(var2.method_25440().getString()).toLowerCase(Locale.ROOT));
         if (!this.isSearchScreen() && !this.isAuctionMainScreen() && !this.isStorageScreen() && !this.isConfirmScreen()) {
            this.auctionStuckActive = false;
            this.auctionStuckTitle = "";
         } else {
            if (!this.auctionStuckActive || !var3.equals(this.auctionStuckTitle)) {
               this.auctionStuckActive = true;
               this.auctionStuckTitle = var3;
               this.auctionStuckTimer.reset();
            } else if (this.auctionStuckTimer.finished(5000L)) {
               this.closeHandledScreenSafely();
               this.auctionStuckActive = false;
               this.auctionStuckTitle = "";
               this.auctionStuckTimer.reset();
            }
         }
      }
   }

   private void handleAn18() {
      boolean var1 = false;
      if (mc.field_1705 != null && mc.field_1705.method_1740() != null) {
         try {
            class_337 var2 = mc.field_1705.method_1740();
            Field var3 = class_337.class.getField("bossBars");
            Map var4 = (Map)var3.get(var2);

            for (class_345 var6 : (java.util.Collection<class_345>)(java.util.Collection<?>)var4.values()) {
               String var7 = var6.method_5414().getString().toLowerCase(Locale.ROOT);
               if (var7.contains("подпишись") || var7.contains("наш") || var7.contains("вы играете")) {
                  var1 = true;
                  break;
               }
            }
         } catch (Exception var8) {
         }
      }

      if (var1) {
         this.isAnarchyBossBarActive = true;
         if (this.an18Timer.finished(15000L)) {
            String var9 = this.getAnarchyNumber();
            if (var9 != null && !var9.isEmpty()) {
               this.sendChat("/an" + var9);
            }

            this.an18Timer.reset();
         }
      } else {
         this.isAnarchyBossBarActive = false;
      }
   }

   private void handlePeriodicAnarchyRelog() {
      // The lobby-aware handler above already sends /an when needed.
      // Sending /an periodically while already in-game causes "already connected" spam
      // and can get the socket reset by the server.
   }

   private boolean handlePeriodicAntiAfk() {
      if (this.periodicAntiAfkEnabled == null || !this.periodicAntiAfkEnabled.isEnabled() || mc.field_1755 != null) {
         if (this.periodicAfkActive) {
            this.stopMovementKeys();
            this.periodicAfkActive = false;
            this.periodicAfkUntilMs = 0L;
         }

         return false;
      }

      long var1 = System.currentTimeMillis();
      if (!this.periodicAfkActive && this.periodicAfkTimer.finished(60000L)) {
         this.periodicAfkActive = true;
         this.periodicAfkUntilMs = var1 + 5000L;
         this.afkMoveStep = 0;
         this.afkMoveStepAtMs = 0L;
         this.afkMoveDir = -1;
         this.afkMouseStep = 0;
         this.afkBaseYaw = mc.field_1724.method_36454();
         this.afkBasePitch = mc.field_1724.method_36455();
      }

      if (!this.periodicAfkActive) {
         return false;
      }

      if (var1 < this.periodicAfkUntilMs) {
         this.handleAfkWalk();
         return true;
      }

      this.stopMovementKeys();
      this.periodicAfkActive = false;
      this.periodicAfkUntilMs = 0L;
      this.periodicAfkTimer.reset();
      return false;
   }

   private void handleAutoReconnectTick() {
      if (this.autoReconnectEnabled == null || !this.autoReconnectEnabled.isEnabled()) {
         return;
      }

      if (mc.field_1724 != null && mc.field_1687 != null) {
         class_642 var1 = mc.method_1558();
         if (var1 != null) {
            this.lastServerInfo = var1;
         }

         this.reconnectTimer.reset();
      } else if (this.reconnectTimer.finished(300000L)) {
         if (this.lastServerInfo != null && this.lastServerInfo.field_3761 != null && !this.lastServerInfo.field_3761.isBlank() && mc.field_1755 != null) {
            aeu.connect(mc.field_1755, mc, class_639.method_2950(this.lastServerInfo.field_3761), this.lastServerInfo, false, null);
         }

         this.reconnectTimer.reset();
      }
   }

   private boolean isAfkBlockedMessage(String var1, String var2) {
      return this.containsAfkKeywords(var1) || this.containsAfkKeywords(var2.toLowerCase(Locale.ROOT));
   }

   private boolean containsAfkKeywords(String var1) {
      String var2 = AutoEnchanterText.normalizeLettersOnly(var1.toLowerCase(Locale.ROOT));
      return (var2.contains("afk") || var2.contains(AutoEnchanterText.normalizeLettersOnly("афк")))
         && (var2.contains(AutoEnchanterText.normalizeLettersOnly("команд")) || var1.contains("command"))
         && (var2.contains(AutoEnchanterText.normalizeLettersOnly("недоступ")) || var1.contains("not available"));
   }

   private boolean isStorageFullMessage(String var1) {
      return var1.contains(AutoEnchanterText.normalizeLettersOnly("Освободите хранилище")) || var1.contains(AutoEnchanterText.normalizeLettersOnly("уберите предметы с продаж"));
   }

   private boolean isSoldMessage(String var1) {
      return var1.contains(AutoEnchanterText.normalizeLettersOnly("У вас купили")) || var1.contains("you sold");
   }

   private void recordSale(String var1) {
      long var2 = this.parseMoney(var1);
      if (var2 > 0L) {
         this.lastSalePrice = var2;
         this.totalEarned += var2;
         this.salesCount++;
      }
   }

   private void recordBuy(String var1) {
      long var2 = this.parseMoney(var1);
      if (var2 > 0L) {
         long var4 = System.currentTimeMillis();
         String var6 = AutoEnchanterText.stripFormatting(var1).trim();
         if (var6.equals(this.lastBuyRecordText) && var4 - this.lastBuyRecordAtMs < 1500L) {
            return;
         }

         this.lastBuyRecordText = var6;
         this.lastBuyRecordAtMs = var4;
         this.lastBuyPrice = var2;
         this.totalSpent += var2;
         this.buysCount++;
      }
   }

   private void renderProfitHud(HudRenderEvent var1) {
      try {
         CustomDrawContext var2 = var1.getContext();
         Fonts.ensureInitialized();
         Font var3 = new Font(Fonts.SEMIBOLD, 8.0F);
         Font var4 = new Font(Fonts.MEDIUM, 7.0F);
         String var5 = "AutoEnchanter";
         long var6 = this.totalEarned - this.totalSpent;
         String var7 = "Profit: $" + this.formatMoneyShort(var6);
         String var8 = "Earned: $" + this.formatMoneyShort(this.totalEarned) + "  Spent: $" + this.formatMoneyShort(this.totalSpent);
         String var9 = "Sales: " + this.salesCount + "  Buys: " + this.buysCount;
         String var10 = "Last: +$" + this.formatMoneyShort(this.lastSalePrice) + " / -$" + this.formatMoneyShort(this.lastBuyPrice);
         float var11 = Math.max(Math.max(var3.width(var5), var4.width(var7)), Math.max(var4.width(var8), Math.max(var4.width(var9), var4.width(var10)))) + 18.0F;
         float var12 = 52.0F;
         float var13 = 8.0F;
         float var14 = 56.0F;
         ColorRGBA var15 = var6 >= 0L ? ColorRGBA.rgba(124, 241, 141, 255) : ColorRGBA.rgba(255, 118, 118, 255);
         var2.drawRoundedRect(var13, var12, var11, var14, BorderRadius.all(5.0F), ColorRGBA.rgba(12, 14, 18, 185));
         var2.drawRoundedBorder(var13, var12, var11, var14, 1.0F, BorderRadius.all(5.0F), ColorRGBA.rgba(106, 232, 122, 155));
         var2.drawText(var3, var5, var13 + 7.0F, var12 + 5.0F, ColorRGBA.rgba(236, 246, 238, 255));
         var2.drawText(var4, var7, var13 + 7.0F, var12 + 17.0F, var15);
         var2.drawText(var4, var8, var13 + 7.0F, var12 + 27.0F, ColorRGBA.rgba(202, 210, 205, 235));
         var2.drawText(var4, var9, var13 + 7.0F, var12 + 37.0F, ColorRGBA.rgba(202, 210, 205, 230));
         var2.drawText(var4, var10, var13 + 7.0F, var12 + 47.0F, ColorRGBA.rgba(202, 210, 205, 220));
      } catch (Throwable ignored) {
      }
   }

   private String formatMoneyShort(long var1) {
      if (var1 >= 1000000000L) {
         return String.format(Locale.ROOT, "%.2fb", var1 / 1000000000.0);
      } else if (var1 >= 1000000L) {
         return String.format(Locale.ROOT, "%.2fm", var1 / 1000000.0);
      } else {
         return var1 >= 1000L ? String.format(Locale.ROOT, "%.1fk", var1 / 1000.0) : Long.toString(var1);
      }
   }

   private void startAfkRetry(String var1) {
      if (var1 != null && !var1.isBlank() && !this.afkRetryActive) {
         this.afkRetryActive = true;
         this.afkRetryCommand = var1;
         this.afkMoveStep = 0;
         this.afkMoveStepAtMs = 0L;
         this.afkMoveDir = -1;
         this.afkMouseStep = 0;
         this.afkBaseYaw = mc.field_1724.method_36454();
         this.afkBasePitch = mc.field_1724.method_36455();
         this.afkTimer.reset();
      }
   }

   private void handleAfkRetry() {
      long var1 = this.afkTimer.getElapsedTime();
      if (var1 < 1200L) {
         this.handleAfkWalk();
      } else {
         this.stopMovementKeys();
         this.afkRetryActive = false;
         if (!this.afkRetryCommand.isBlank()) {
            this.sendChat(this.afkRetryCommand);
         }

         this.afkRetryCommand = "";
      }
   }

   private void handleAfkWalk() {
      long var1 = System.currentTimeMillis();
      if (this.afkMoveStepAtMs == 0L || var1 >= this.afkMoveStepAtMs) {
         this.afkMoveStepAtMs = var1 + 120L;
         this.handleAfkMouseMove();
         if (this.afkMoveStep % 2 == 0) {
            this.afkMoveDir = (int)(Math.random() * 4.0);
            this.setMovementDir(this.afkMoveDir);
         } else {
            this.setMovementDir(this.getOppositeMoveDir(this.afkMoveDir));
         }

         this.afkMoveStep++;
      }
   }

   private void handleAfkMouseMove() {
      if (mc.field_1724 != null && this.afkMouseStep < 2) {
         if (this.afkMouseStep == 0) {
            float var2 = (float)(Math.random() * 3.0 * 2.0 - 3.0);
            float var1;
            this.afkMouseDeltaYaw = var1 = Math.max(-6.0F, Math.min(6.0F, var2 * (1.2F + (float)(Math.random() * 1.3))));
            this.afkMouseDeltaPitch = var2;
            this.applyMouseAngles(this.afkBaseYaw + var1, this.afkBasePitch + var2);
         } else {
            this.applyMouseAngles(this.afkBaseYaw, this.afkBasePitch);
         }

         this.afkMouseStep++;
      }
   }

   private void applyMouseAngles(float var1, float var2) {
      var2 = Math.max(-90.0F, Math.min(90.0F, var2));
      mc.field_1724.method_36456(var1);
      mc.field_1724.method_5847(var1);
      mc.field_1724.method_36457(var2);
   }

   private int getOppositeMoveDir(int var1) {
      return switch (var1) {
         case 0 -> 1;
         case 1 -> 0;
         case 2 -> 3;
         default -> 2;
      };
   }

   private void setMovementDir(int var1) {
      this.stopMovementKeys();
      switch (var1) {
         case 0:
            mc.field_1690.field_1894.method_23481(true);
            break;
         case 1:
            mc.field_1690.field_1881.method_23481(true);
            break;
         case 2:
            mc.field_1690.field_1913.method_23481(true);
            break;
         case 3:
            mc.field_1690.field_1849.method_23481(true);
      }
   }

   private void stopMovementKeys() {
      mc.field_1690.field_1894.method_23481(false);
      mc.field_1690.field_1881.method_23481(false);
      mc.field_1690.field_1913.method_23481(false);
      mc.field_1690.field_1849.method_23481(false);
   }

   private void sendChat(String var1) {
      if (mc.method_1562() != null && var1 != null) {
         this.lastSentCommand = var1;
         String var2 = var1.startsWith("/") ? var1.substring(1) : var1;
         if (!var2.isBlank()) {
            mc.method_1562().method_45731(var2);
         }
      }
   }

   private long delayMs() {
      return 50L;
   }

   private long buyDelayMs() {
      return Math.max(200L, this.delayMs());
   }

   private long timeoutMs() {
      return 6000L;
   }

   private long cacheMs() {
      return 30000L;
   }

   private boolean isBuySatisfied(AutoEnchanter.BuyRequest var1) {
      if ("sword_sharp".equals(var1.key)) {
         return this.countSharpnessSwords() >= var1.targetCount;
      } else if ("sword_clean".equals(var1.key)) {
         return this.countCleanNetheriteSwords() >= var1.targetCount;
      } else if (var1.item != null) {
         return this.countItem(var1.item) >= var1.targetCount;
      } else {
         return "wood".equals(var1.key) ? this.countLogs() * 4 + this.countPlanks() >= var1.targetCount * 4 : false;
      }
   }

   private boolean hasTargetEnchant(class_1799 var1) {
      return this.isSwordMode() ? this.isPoisonSword(var1) : this.hasEnchantNeedles(var1, this.getTargetEnchantNeedles(), this.targetRequiresLevelTwo());
   }

   private boolean isTargetBaseItem(class_1799 var1) {
      if (var1.method_7960()) {
         return false;
      } else {
         if (this.isSwordMode()) {
            if (var1.method_7909() != class_1802.field_22022) {
               return false;
            }
         } else {
            class_1792 var2 = this.getTargetBaseItem();
            if (var2 == null || var1.method_7909() != var2) {
               return false;
            }
         }

         return true;
      }
   }

   private boolean isFinalSword(class_1799 var1) {
      return var1 != null
         && !var1.method_7960()
         && var1.method_7909() == class_1802.field_22022
         && !this.hasBadEnchant(var1)
         && this.isSharpnessSword(var1)
         && this.isPoisonSword(var1);
   }

   private int[] findAnvilCandidates() {
      int var1 = -1;
      int var2 = -1;

      for (int var3 = 0; var3 < mc.field_1724.method_31548().method_5439(); var3++) {
         class_1799 var4 = mc.field_1724.method_31548().method_5438(var3);
         if (!var4.method_7960() && !this.hasBadEnchant(var4) && !this.isFinalSword(var4)) {
            if (var1 == -1 && this.isSharpnessSword(var4)) {
               var1 = var3;
            } else if (var2 == -1 && this.isPoisonSword(var4)) {
               var2 = var3;
            }
         }
      }

      return var1 != -1 && var2 != -1 ? new int[]{var1, var2} : null;
   }

   private boolean shouldCombineSword(class_1799 var1, class_1799 var2) {
      return var1 != null && var2 != null && this.isSharpnessSword(var1) && this.isPoisonSword(var2);
   }

   private void buildSwordBuyRequests() {
      this.buyRequests.clear();
      this.buyIndex = 0;
      this.buyAttempts = 0;
      this.buyBlocked = false;
      int var1 = this.countItem(class_1802.field_8759);
      int var2 = 3;
      if (var1 < var2 && var1 < 128) {
         this.buyRequests
            .add(new AutoEnchanter.BuyRequest("lapis", this.buildSearchName(new class_1799(class_1802.field_8759)), class_1802.field_8759, var2));
      }

      if (this.countSharpnessSwords() <= 0) {
         this.buyRequests.add(new AutoEnchanter.BuyRequest("sword_sharp", "незеритовый меч", null, 1, false));
      }

      if (this.countCleanNetheriteSwords() <= 0 && this.countPoisonSwords() <= 0) {
         this.buyRequests.add(new AutoEnchanter.BuyRequest("sword_clean", "незеритовый меч", null, 1, false));
      }

      int var3 = this.requiredXpBottles();
      int var4 = this.countItem(class_1802.field_8287);
      if (this.needsXp() && var4 < var3) {
         this.buyRequests.add(new AutoEnchanter.BuyRequest("xp", this.getXpSearchQuery(), class_1802.field_8287, var3, true));
      }
   }

   private boolean needsSwordMaterials() {
      return this.needsXp() && !this.hasXpBottle()
         || this.countItem(class_1802.field_8759) < 1
         || this.countSharpnessSwords() <= 0
         || this.countCleanNetheriteSwords() <= 0 && this.countPoisonSwords() <= 0;
   }

   private boolean isSharpnessSword(class_1799 var1) {
      return var1 != null
         && !var1.method_7960()
         && var1.method_7909() == class_1802.field_22022
         && !this.hasBadEnchant(var1)
         && this.classifier.getSwordEnchantLevel(var1, SWORD_REQUIREMENTS[0]) >= 7;
   }

   // Enchants that disqualify a sword as "clean" (can't be ground off / unwanted).
   private static final String[] UNSTABLE_NEEDLES = {"Нестабильн", "Unstable"};
   private static final String[] HEAVY_NEEDLES = {"Тяжел", "Heavy"};

   // A "clean" netherite sword = any netherite sword WITHOUT Нестабильный (Unstable)
   // or Тяжелый (Heavy). It does NOT require zero enchantments (those don't exist on
   // the AH). Price is checked separately in findCheapestSlot.
   private boolean isCleanNetheriteSword(class_1799 var1) {
      return var1 != null
         && !var1.method_7960()
         && var1.method_7909() == class_1802.field_22022
         && !this.classifier.hasEnchant(var1, UNSTABLE_NEEDLES)
         && !this.classifier.hasEnchant(var1, HEAVY_NEEDLES);
   }

   /** Minimum Яд (Poison) level worth keeping. Яд 1 is not worth it. */
   private static final int POISON_MIN_LEVEL = 2;

   private boolean isPoisonSword(class_1799 var1) {
      return var1 != null
         && !var1.method_7960()
         && var1.method_7909() == class_1802.field_22022
         && !this.hasBadEnchant(var1)
         // Read Яд from BOTH lore and the enchantment component (issue #1),
         // and require level >= 2 (issue #3 — Яд 1 is skipped).
         && this.classifier.getEnchantLevel(var1, this.getTargetEnchantNeedles()) >= POISON_MIN_LEVEL;
   }

   private int countSharpnessSwords() {
      int var1 = 0;

      for (int var2 = 0; var2 < mc.field_1724.method_31548().method_5439(); var2++) {
         class_1799 var3 = mc.field_1724.method_31548().method_5438(var2);
         if (this.isSharpnessSword(var3)) {
            var1++;
         }
      }

      return var1;
   }

   private int countCleanNetheriteSwords() {
      int var1 = 0;

      for (int var2 = 0; var2 < mc.field_1724.method_31548().method_5439(); var2++) {
         class_1799 var3 = mc.field_1724.method_31548().method_5438(var2);
         if (this.isCleanNetheriteSword(var3)) {
            var1++;
         }
      }

      return var1;
   }

   private int countPoisonSwords() {
      int var1 = 0;

      for (int var2 = 0; var2 < mc.field_1724.method_31548().method_5439(); var2++) {
         class_1799 var3 = mc.field_1724.method_31548().method_5438(var2);
         if (this.isPoisonSword(var3)) {
            var1++;
         }
      }

      return var1;
   }

   private boolean isSwordAuctionPriceAllowed(class_1799 var1) {
      long var2 = this.extractPrice(var1);
      if (var2 > 0L) {
         long var4 = Math.max(1L, var2 / Math.max(1, var1.method_7947()));
         long var6 = this.countSharpnessSwords() > 0
            ? this.getLongInput(this.maxEnchantSwordPriceInput, 100000L, 1L, 100000000L)
            : this.getLongInput(this.maxSharpnessSwordPriceInput, 1000000L, 1L, 100000000L);
         if (var6 >= var4) {
            return true;
         }
      }

      return false;
   }

   private void handleBuyClick() {
      if (this.buyTimer.finished(this.delayMs())) {
         if (this.tryConfirmPurchase()) {
            this.buyTimer.reset();
            this.buyConfirmOpenedAtMs = System.currentTimeMillis();
            this.setState(AutoEnchanter.State.BUY_WAIT_RESULT);
         } else {
            if (mc.field_1755 instanceof class_465 var2) {
               class_1703 var3 = var2.method_17577();
               if (!var3.method_34255().method_7960()) {
                  this.clearCursorToInventory(var3);
               } else if (!this.isSearchScreen()) {
                  if (this.closeHandledScreenSafely()) {
                     this.setState(AutoEnchanter.State.BUY_SEND);
                     this.actionTimer.reset();
                  }
               } else {
                  AutoEnchanter.BuyRequest var4 = this.buyRequests.get(this.buyIndex);
                  if (this.isBuySatisfied(var4)) {
                     if (this.closeHandledScreenSafely()) {
                        this.buyIndex++;
                        this.buyAttempts = 0;
                        this.buyBlocked = false;
                        this.setState(AutoEnchanter.State.BUY_SEND);
                        this.actionTimer.reset();
                     }
                  } else {
                     if (!this.buyBlocked && this.buyAttempts < 96 && !this.isInventoryFull()) {
                        int var5 = this.getContainerSlotCount(var3);
                        int var6 = this.findCheapestSlot(var3, var4.query, var4.outputFilterTarget, var5);
                        if (var6 != -1) {
                           this.clickContainerSlot(var3, var6);
                           this.buyAttempts++;
                           this.buyTimer.reset();
                           this.buyConfirmOpenedAtMs = 0L;
                           this.setState(AutoEnchanter.State.BUY_WAIT_RESULT);
                           return;
                        }

                        if (this.tryClickAuctionNextOrRefresh(var3, var5)) {
                           return;
                        }
                     }

                     if (this.closeHandledScreenSafely()) {
                        this.buyIndex++;
                        this.buyAttempts = 0;
                        this.buyBlocked = false;
                        this.setState(AutoEnchanter.State.BUY_SEND);
                        this.actionTimer.reset();
                     }
                  }
               }
            } else {
               this.setState(AutoEnchanter.State.BUY_SEND);
               this.actionTimer.reset();
            }
         }
      }
   }

   private boolean tryClickAuctionNextOrRefresh(class_1703 var1, int var2) {
      long var5 = System.currentTimeMillis();
      if (var5 < this.buyNavigationCooldownUntilMs) {
         return true;
      }

      int[] var3 = this.getAuctionPageInfo();
      boolean var4 = var3 == null || var3[0] < var3[1];
      int var7 = var4 ? this.findAuctionButtonSlot(var1, var2, "следующаястраница", "nextpage") : -1;
      if (var7 == -1 && var4) {
         var7 = this.findAuctionButtonSlot(
            var1,
            var2,
            "\u0441\u043b\u0435\u0434\u0443\u044e\u0449\u0430\u044f\u0441\u0442\u0440\u0430\u043d\u0438\u0446\u0430",
            "\u0434\u0430\u043b\u0435\u0435"
         );
      }

      if (var7 == -1 && !var4) {
         var7 = this.findAuctionButtonSlot(var1, var2, "обновить", "refresh");
      }

      if (var7 == -1 && var3 == null) {
         var7 = this.findAuctionButtonSlot(var1, var2, "обновить", "refresh");
      }

      if (var7 == -1 && !var4) {
         var7 = this.findAuctionButtonSlot(var1, var2, "\u043e\u0431\u043d\u043e\u0432\u0438\u0442\u044c");
      }

      if (var7 == -1) {
         if (var3 != null) {
            this.buyNavigationCooldownUntilMs = var5 + 750L;
            this.buyTimer.reset();
            this.actionTimer.reset();
            return true;
         }

         return false;
      }

      this.clickContainerSlot(var1, var7);
      this.buyNavigationCooldownUntilMs = var5 + 1500L;
      this.buyTimer.reset();
      this.actionTimer.reset();
      return true;
   }

   private int[] getAuctionPageInfo() {
      if (!(mc.field_1755 instanceof class_465 var1)) {
         return null;
      }

      String var2 = AutoEnchanterText.stripFormatting(var1.method_25440().getString());
      Matcher var3 = AUCTION_PAGE_PATTERN.matcher(var2);
      if (!var3.find()) {
         return null;
      }

      try {
         int var4 = Integer.parseInt(var3.group(1));
         int var5 = Integer.parseInt(var3.group(2));
         return var4 > 0 && var5 > 0 ? new int[]{var4, var5} : null;
      } catch (NumberFormatException var6) {
         return null;
      }
   }

   private int findAuctionButtonSlot(class_1703 var1, int var2, String... var3) {
      for (int var5 = 0; var5 < var2; var5++) {
         class_1799 var6 = ((class_1735)var1.field_7761.get(var5)).method_7677();
         if (!var6.method_7960() && this.stackContainsText(var6, var3)) {
            return var5;
         }
      }

      return -1;
   }

   private boolean isGrindableSword(class_1799 var1) {
      return var1 != null
         && !var1.method_7960()
         && var1.method_7909() == class_1802.field_22022
         && !this.isSharpnessSword(var1)
         && !this.isFinalSword(var1)
         && !this.isPoisonSword(var1);
   }

   private boolean stackContainsText(class_1799 var1, String... var2) {
      if (this.textContainsAnyNeedle(this.classifier.getItemName(var1), var2)) {
         return true;
      }

      for (String var4 : this.classifier.getLoreLines(var1)) {
         if (this.textContainsAnyNeedle(AutoEnchanterText.stripFormatting(var4), var2)) {
            return true;
         }
      }

      return false;
   }

   private boolean textContainsAnyNeedle(String var1, String... var2) {
      String var3 = AutoEnchanterText.normalizeLettersOnly(var1);

      for (String var6 : var2) {
         String var5 = AutoEnchanterText.normalizeLettersOnly(var6);
         if (!var5.isBlank() && var3.contains(var5)) {
            return true;
         }
      }

      return false;
   }

   private static class BuyRequest {
      final String key;
      final String query;
      final class_1792 item;
      final int targetCount;
      final boolean outputFilterTarget;
      final boolean keepDigits;

      BuyRequest(String key, String query, class_1792 item, int targetCount) {
         this(key, query, item, targetCount, false);
      }

      BuyRequest(String key, String query, class_1792 item, int targetCount, boolean keepDigits) {
         this.key = key;
         this.query = query;
         this.item = item;
         this.targetCount = targetCount;
         this.outputFilterTarget = false;
         this.keepDigits = keepDigits;
      }
   }

   private static enum CraftStage {
      NONE,
      PLANKS,
      STICKS,
      PICKAXE,
      SWORD;
   }

   private static class PriceCache {
      final long price;
      final long expiresAt;
      final double perQuality;

      PriceCache(long price, long expiresAt, double perQuality) {
         this.price = price;
         this.expiresAt = expiresAt;
         this.perQuality = perQuality;
      }

      boolean isValid() {
         return System.currentTimeMillis() <= this.expiresAt;
      }
   }

   private record PriceRequest(String key, String query, boolean output, boolean keepDigits) {
   }

   private static final class QueueCoordinatorClient {
      private final String host;
      private final int port;
      private final String accountId;
      private final boolean main;
      private volatile boolean running;
      private volatile boolean connected;
      private volatile boolean wantTurn;
      private volatile boolean hasTurn;
      private volatile int queuePosition;
      private volatile String holderId;
      private volatile boolean justConnected;
      private volatile Thread thread;
      private volatile Socket socket;
      private volatile BufferedReader reader;
      private volatile BufferedWriter writer;
      private volatile long lastHeartbeatAtMs;
      private volatile long lastConnectAttemptAtMs;

      private QueueCoordinatorClient(String host, int port, String accountId, boolean main) {
         this.host = host;
         this.port = port;
         this.accountId = accountId;
         this.main = main;
         this.running = false;
         this.connected = false;
         this.wantTurn = false;
         this.hasTurn = false;
         this.queuePosition = 0;
         this.holderId = "";
         this.justConnected = false;
         this.lastHeartbeatAtMs = 0L;
         this.lastConnectAttemptAtMs = 0L;
      }

      private boolean isRunning() {
         return this.running;
      }

      private boolean isConnected() {
         return this.connected;
      }

      private boolean isFor(String id, int port, boolean main) {
         return this.port == port && this.accountId.equals(id) && this.main == main;
      }

      private boolean hasTurn() {
         return this.connected && this.hasTurn;
      }

      private int getQueuePosition() {
         return this.queuePosition;
      }

      private String getHolderId() {
         return this.holderId;
      }

      private boolean consumeJustConnected() {
         if (!this.justConnected) {
            return false;
         } else {
            this.justConnected = false;
            return true;
         }
      }

      private void setWantTurn(boolean wantTurn) {
         this.wantTurn = wantTurn;
      }

      private void releaseTurn() {
         this.sendLine("RELEASE");
      }

      private void start() {
         if (!this.running) {
            this.running = true;
            this.thread = new Thread(() -> {
               while (this.running) {
                  if (!this.connected) {
                     long now = System.currentTimeMillis();
                     if (now - this.lastConnectAttemptAtMs >= 2000L) {
                        this.lastConnectAttemptAtMs = now;
                        this.tryConnect();
                     }
                  } else {
                     long now = System.currentTimeMillis();
                     if (now - this.lastHeartbeatAtMs >= 1000L) {
                        this.lastHeartbeatAtMs = now;
                        this.sendLine("PING " + (this.wantTurn ? 1 : 0));
                     }

                     this.readIncoming();
                  }

                  try {
                     Thread.sleep(50L);
                  } catch (InterruptedException var3) {
                     Thread.currentThread().interrupt();
                     break;
                  }
               }

               this.disconnect();
            }, "nev-queue-client-" + this.port + "-" + this.accountId);
            this.thread.setDaemon(true);
            this.thread.start();
         }
      }

      private void stop() {
         this.running = false;
         this.disconnect();
      }

      private void tryConnect() {
         try {
            Socket newSocket = new Socket(this.host, this.port);
            newSocket.setSoTimeout(250);
            BufferedReader newReader = new BufferedReader(new InputStreamReader(newSocket.getInputStream(), StandardCharsets.UTF_8));
            BufferedWriter newWriter = new BufferedWriter(new OutputStreamWriter(newSocket.getOutputStream(), StandardCharsets.UTF_8));
            this.socket = newSocket;
            this.reader = newReader;
            this.writer = newWriter;
            this.sendLine("HELLO " + this.accountId + " " + (this.main ? "MAIN" : "TWINK"));
            this.connected = true;
            this.hasTurn = false;
            this.queuePosition = 0;
            this.holderId = "";
            this.justConnected = true;
         } catch (Throwable var4) {
            this.disconnect();
         }
      }

      private void disconnect() {
         this.connected = false;
         this.hasTurn = false;
         this.queuePosition = 0;
         this.holderId = "";

         try {
            if (this.socket != null) {
               this.socket.close();
            }
         } catch (Throwable var2) {
         }

         this.socket = null;
         this.reader = null;
         this.writer = null;
      }

      private void readIncoming() {
         BufferedReader in = this.reader;
         if (in != null) {
            try {
               while (true) {
                  String line = in.readLine();
                  if (line != null) {
                     this.handleServerLine(line);
                  }

                  if (line == null || !in.ready()) {
                     if (line == null) {
                        this.disconnect();
                     }
                     break;
                  }
               }
            } catch (SocketTimeoutException var3) {
            } catch (Throwable var4) {
               this.disconnect();
            }
         }
      }

      private void handleServerLine(String line) {
         if (line != null && !line.isBlank()) {
            if (line.startsWith("STATUS ")) {
               String[] parts = line.split(" ", 4);
               if (parts.length >= 2) {
                  this.hasTurn = "1".equals(parts[1].trim());
               }

               if (parts.length >= 3) {
                  try {
                     this.queuePosition = Integer.parseInt(parts[2].trim());
                  } catch (NumberFormatException var4) {
                     this.queuePosition = 0;
                  }
               }

               if (parts.length >= 4) {
                  this.holderId = parts[3].trim();
               }
            }
         }
      }

      private void sendLine(String line) {
         BufferedWriter out = this.writer;
         if (out != null) {
            synchronized (this) {
               try {
                  out.write(line);
                  out.newLine();
                  out.flush();
               } catch (Throwable var6) {
                  this.disconnect();
               }
            }
         }
      }
   }

   private static final class QueueCoordinatorServer {
      private final int port;
      private final Map<String, AutoEnchanter.QueuePeerSession> peers = new ConcurrentHashMap<>();
      private final AtomicLong joinCounter = new AtomicLong(1L);
      private volatile boolean running;
      private volatile ServerSocket serverSocket;
      private volatile Thread acceptThread;
      private volatile String currentHolderId = "";
      private volatile long holderSinceMs = 0L;
      private volatile long lastGrantedOrderKey = Long.MIN_VALUE;
      private volatile String lastConnectedAccountId = "";

      private QueueCoordinatorServer(int port) {
         this.port = port;
      }

      private boolean isRunning() {
         return this.running;
      }

      private boolean isForPort(int port) {
         return this.port == port;
      }

      private void start() {
         if (!this.running) {
            this.running = true;
            this.acceptThread = new Thread(() -> {
               try {
                  this.serverSocket = new ServerSocket(this.port, 30, InetAddress.getLoopbackAddress());
                  this.serverSocket.setSoTimeout(300);

                  for (; this.running; this.tickQueue()) {
                     try {
                        Socket socket = this.serverSocket.accept();
                        this.startPeerWorker(socket);
                     } catch (SocketTimeoutException var7) {
                     } catch (IOException var8) {
                        if (!this.running) {
                           break;
                        }
                     }
                  }
               } catch (IOException var9) {
                  this.running = false;
               } finally {
                  this.closeAll();
               }
            }, "nev-queue-server-" + this.port);
            this.acceptThread.setDaemon(true);
            this.acceptThread.start();
         }
      }

      private void stop() {
         this.running = false;
         this.closeAll();
      }

      private String consumeLastConnectedAccountId() {
         String id = this.lastConnectedAccountId;
         this.lastConnectedAccountId = "";
         return id;
      }

      private void closeAll() {
         try {
            if (this.serverSocket != null) {
               this.serverSocket.close();
            }
         } catch (Throwable var3) {
         }

         this.serverSocket = null;

         for (AutoEnchanter.QueuePeerSession session : this.peers.values()) {
            session.close();
         }

         this.peers.clear();
         this.currentHolderId = "";
         this.holderSinceMs = 0L;
      }

      private void startPeerWorker(Socket socket) {
         Thread worker = new Thread(() -> {
            AutoEnchanter.QueuePeerSession session = null;

            try {
               socket.setSoTimeout(1200);
               BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
               BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
               String hello = reader.readLine();
               if (hello == null || !hello.startsWith("HELLO ")) {
                  socket.close();
                  return;
               }

               String[] parts = hello.split(" ", 3);
               if (parts.length < 3) {
                  socket.close();
                  return;
               }

               String id = parts[1].trim();
               boolean main = parts[2].trim().equalsIgnoreCase("MAIN");
               if (!id.isBlank()) {
                  session = new AutoEnchanter.QueuePeerSession(socket, reader, writer, id, main, this.joinCounter.getAndIncrement());
                  AutoEnchanter.QueuePeerSession old = this.peers.put(id, session);
                  if (old != null) {
                     old.close();
                  }

                  this.lastConnectedAccountId = id;
                  session.sendLine("WELCOME " + id);

                  while (this.running && !socket.isClosed()) {
                     try {
                        String line = reader.readLine();
                        if (line == null) {
                           return;
                        }

                        session.lastSeenAtMs = System.currentTimeMillis();
                        if (line.startsWith("PING")) {
                           String[] ping = line.split(" ");
                           session.wantTurn = ping.length >= 2 && "1".equals(ping[1].trim());
                        } else if (line.equals("RELEASE") && session.id.equals(this.currentHolderId)) {
                           this.currentHolderId = "";
                           this.holderSinceMs = 0L;
                        } else if (line.startsWith("READY ")) {
                           String[] ready = line.split(" ");
                           session.wantTurn = ready.length >= 2 && "1".equals(ready[1].trim());
                        }
                     } catch (SocketTimeoutException var24) {
                     }
                  }

                  return;
               }

               socket.close();
            } catch (Throwable var25) {
               return;
            } finally {
               if (session != null) {
                  this.peers.remove(session.id, session);
                  if (session.id.equals(this.currentHolderId)) {
                     this.currentHolderId = "";
                     this.holderSinceMs = 0L;
                  }

                  session.close();
               } else {
                  try {
                     socket.close();
                  } catch (Throwable var23) {
                  }
               }
            }
         }, "nev-queue-peer-" + this.port);
         worker.setDaemon(true);
         worker.start();
      }

      private void tickQueue() {
         long now = System.currentTimeMillis();
         List<AutoEnchanter.QueuePeerSession> toRemove = new ArrayList<>();

         for (AutoEnchanter.QueuePeerSession peer : this.peers.values()) {
            if (now - peer.lastSeenAtMs > 7000L) {
               toRemove.add(peer);
            }
         }

         for (AutoEnchanter.QueuePeerSession peerx : toRemove) {
            this.peers.remove(peerx.id, peerx);
            if (peerx.id.equals(this.currentHolderId)) {
               this.currentHolderId = "";
               this.holderSinceMs = 0L;
            }

            peerx.close();
         }

         AutoEnchanter.QueuePeerSession holder = this.currentHolderId.isEmpty() ? null : this.peers.get(this.currentHolderId);
         if (holder == null || !holder.wantTurn || now - this.holderSinceMs > 12000L) {
            this.currentHolderId = "";
            this.holderSinceMs = 0L;
         }

         if (this.currentHolderId.isEmpty()) {
            List<AutoEnchanter.QueuePeerSession> ready = new ArrayList<>();

            for (AutoEnchanter.QueuePeerSession peerx : this.peers.values()) {
               if (peerx.wantTurn) {
                  ready.add(peerx);
               }
            }

            ready.sort(Comparator.comparingLong(AutoEnchanter.QueuePeerSession::orderKey));
            if (!ready.isEmpty()) {
               AutoEnchanter.QueuePeerSession next = null;

               for (AutoEnchanter.QueuePeerSession peerxx : ready) {
                  if (peerxx.orderKey() > this.lastGrantedOrderKey) {
                     next = peerxx;
                     break;
                  }
               }

               if (next == null) {
                  next = ready.get(0);
               }

               this.currentHolderId = next.id;
               this.holderSinceMs = now;
               this.lastGrantedOrderKey = next.orderKey();
            }
         }

         this.broadcastStatus();
      }

      private void broadcastStatus() {
         List<AutoEnchanter.QueuePeerSession> orderedReady = new ArrayList<>();

         for (AutoEnchanter.QueuePeerSession peer : this.peers.values()) {
            if (peer.wantTurn) {
               orderedReady.add(peer);
            }
         }

         orderedReady.sort(Comparator.comparingLong(AutoEnchanter.QueuePeerSession::orderKey));
         if (!this.currentHolderId.isEmpty()) {
            int holderIndex = -1;

            for (int i = 0; i < orderedReady.size(); i++) {
               if (orderedReady.get(i).id.equals(this.currentHolderId)) {
                  holderIndex = i;
                  break;
               }
            }

            if (holderIndex > 0) {
               List<AutoEnchanter.QueuePeerSession> rotated = new ArrayList<>(orderedReady.size());

               for (int ix = holderIndex; ix < orderedReady.size(); ix++) {
                  rotated.add(orderedReady.get(ix));
               }

               for (int ix = 0; ix < holderIndex; ix++) {
                  rotated.add(orderedReady.get(ix));
               }

               orderedReady = rotated;
            }
         }

         for (AutoEnchanter.QueuePeerSession peerx : this.peers.values()) {
            boolean turn = !this.currentHolderId.isEmpty() && this.currentHolderId.equals(peerx.id);
            int position = 0;

            for (int ix = 0; ix < orderedReady.size(); ix++) {
               if (orderedReady.get(ix).id.equals(peerx.id)) {
                  position = ix + 1;
                  break;
               }
            }

            try {
               peerx.sendLine("STATUS " + (turn ? 1 : 0) + " " + position + " " + (this.currentHolderId.isEmpty() ? "-" : this.currentHolderId));
            } catch (Throwable var7) {
            }
         }
      }
   }

   private static final class QueuePeerSession {
      private final Socket socket;
      private final BufferedReader reader;
      private final BufferedWriter writer;
      private final String id;
      private final boolean main;
      private final long joinOrder;
      private volatile boolean wantTurn;
      private volatile long lastSeenAtMs;

      private QueuePeerSession(Socket socket, BufferedReader reader, BufferedWriter writer, String id, boolean main, long joinOrder) {
         this.socket = socket;
         this.reader = reader;
         this.writer = writer;
         this.id = id;
         this.main = main;
         this.joinOrder = joinOrder;
         this.wantTurn = false;
         this.lastSeenAtMs = System.currentTimeMillis();
      }

      private long orderKey() {
         long role = this.main ? 0L : 1L;
         return role * 1000000000000L + this.joinOrder;
      }

      private synchronized void sendLine(String line) throws IOException {
         this.writer.write(line);
         this.writer.newLine();
         this.writer.flush();
      }

      private void close() {
         try {
            this.socket.close();
         } catch (Throwable var2) {
         }
      }
   }

   private static enum State {
      IDLE,
      PRICE_SEND,
      PRICE_WAIT,
      DECIDE,
      BUY_SEND,
      BUY_WAIT_SCREEN,
      BUY_CLICK,
      BUY_WAIT_RESULT,
      USE_XP,
      CRAFT_OPEN,
      CRAFT_WAIT,
      CRAFTING,
      ENCHANT_OPEN,
      ENCHANT_WAIT,
      ENCHANT_PLACE,
      ENCHANT_CLICK,
      ENCHANT_WAIT_RESULT,
      EVALUATE,
      GRIND_OPEN,
      GRIND_WAIT,
      GRIND_PLACE,
      GRIND_TAKE,
      ANVIL_OPEN,
      ANVIL_WAIT,
      ANVIL_PLACE,
      ANVIL_COMBINE,
      ANVIL_TAKE,
      SMITH_OPEN,
      SMITH_WAIT,
      SMITH_PLACE,
      SMITH_TAKE,
      SELL_SEND,
      SELL_WAIT,
      DROP_BAD,
      RELIST_OPEN_AH,
      RELIST_WAIT_AH,
      RELIST_CLICK_STORAGE,
      RELIST_WAIT_STORAGE,
      RELIST_CLICK_RELIST;
   }

}
