# mytheria-v6-dev

Development workspace for patching the **Mytheria v6 ("AutoEnchanter" / old-clickgui)**
Fabric mod for **Minecraft 1.21.4**.

We don't have the mod's source — we decompile the shipped JAR, edit the few classes
we care about, recompile only those, and **merge our recompiled classes over the
original JAR** to produce a new mod JAR. The rest of the mod is untouched.

Current patches:
- **Action logging** — structured JSON-lines log of the AutoEnchanter
  (`AutoEnchanter`) state machine: buys, crafts, sells, chat, state changes.
- **Menu sections** — the AutoEnchanter settings are split into tabs
  (`Раздел`: Общие / Кирки / Мечи) via per-setting visibility conditions.

---

## 1. Prerequisites

| Tool | Version | Notes |
|------|---------|-------|
| Git | any | to clone |
| JDK to **run Gradle** | 17–24 | Gradle 8.14 does **not** run on Java 25+. See §3. |
| JDK 21 to **compile** | auto | downloaded automatically by the Gradle toolchain |
| (optional) Vineflower | 1.11.x | only to regenerate the `decompiled/` reference |

You do **not** need a separate Gradle install — the repo ships the Gradle wrapper
(`gradlew` / `gradlew.bat`).

---

## 2. Repo layout

```
mytheria-v6-dev/
├── build.gradle              # build + deploy pipeline
├── gradle.properties         # versions + per-machine override hints
├── settings.gradle
├── gradlew / gradlew.bat      # Gradle wrapper (use these, not a global gradle)
├── gradle/wrapper/           # committed wrapper jar
├── libs/
│   └── mytheria-v6.jar       # ORIGINAL mod JAR — REQUIRED (build base + classpath)
├── src/main/java/moscow/mytheria/
│   ├── logger/MytheriaLogger.java          # our JSON logger
│   └── systems/.../AutoEnchanter.java  # the patched module
└── decompiled/               # full decompiled reference (gitignored; regenerate)
```

`libs/mytheria-v6.jar` is the unmodified mod. It is used **twice**: as a
`compileOnly` dependency (so our edited class sees the mod's other classes), and as
the **base** that our recompiled classes are merged into at deploy time.

---

## 3. One-time setup

### a) Clone & confirm the base JAR

```bash
git clone <repo-url> mytheria-v6-dev
cd mytheria-v6-dev
# Make sure libs/mytheria-v6.jar exists. If your repo uses Git LFS, run `git lfs pull`.
```

### b) Point the deploy at YOUR game — "where the game is"

The build copies the finished JAR straight into your Minecraft `mods` folder. The
default is **TLauncher's legacy path**. Override it for your launcher by setting
`mods_dir` in `gradle.properties` (or pass `-Pmods_dir=...` on the command line):

```properties
# gradle.properties
mods_dir=C:/Users/YOU/AppData/Roaming/.minecraft/mods
```

Common locations:

| Launcher | mods folder |
|----------|-------------|
| TLauncher (legacy) — **default** | `%APPDATA%/.tlauncher/legacy/Minecraft/game/mods` |
| Vanilla / Mojang launcher | `%APPDATA%/.minecraft/mods` |
| MultiMC / Prism | `<instance>/.minecraft/mods` |
| macOS vanilla | `~/Library/Application Support/minecraft/mods` |
| Linux vanilla | `~/.minecraft/mods` |

> The deploy always writes the file as **`mytheria-2.0.6.jar`** — the name the game
> loads — so it replaces the active mod. Keep only one Mytheria JAR in `mods/`.

### c) Make sure Gradle can run (Java version)

Gradle 8.14 runs on Java 17–24. If your **default** `java` is newer (e.g. 25/26),
Gradle fails with *"Unsupported class file major version …"*. Fix it **without
committing a machine path** by setting `org.gradle.java.home` in your **user-level**
Gradle file (not the project one):

- Windows: `%USERPROFILE%\.gradle\gradle.properties`
- macOS/Linux: `~/.gradle/gradle.properties`

```properties
org.gradle.java.home=C:/Path/To/jdk-21
```

(Compilation always uses an auto-downloaded JDK 21 toolchain regardless; this line
only controls which JDK *runs* Gradle itself.)

---

## 4. Build & deploy

```bash
./gradlew deploy          # Windows: .\gradlew.bat deploy
```

Pipeline (`deploy` → `repackJar` → `prepareModDir` → `remapJar` → `compileJava`):

1. **compileJava** – compiles `src/` against MC 1.21.4 + the base JAR.
2. **remapJar** – remaps our classes to intermediary names (Loom).
3. **prepareModDir** – extracts `libs/mytheria-v6.jar` to `build/mod-work/`, then
   overwrites it with our recompiled `*.class` files (ours always win).
4. **repackJar** – zips `build/mod-work/` into `mytheria-2.0.6.jar` in your `mods/`.

Then **restart Minecraft** to load the new JAR.

First run downloads MC, mappings, Fabric, and a JDK 21 toolchain (~once, a few min).
Later builds take ~15–25s.

---

## 5. Day-to-day: editing a class

We only put files we actually edit in `src/`; everything else stays compiled in the
base JAR.

1. **Find the class** in `decompiled/` (regenerate it first if missing — see §7).
2. **Copy it into `src/`**, preserving its package path, e.g.
   `decompiled/mytheria/systems/modules/modules/other/Foo.java`
   → `src/main/java/moscow/mytheria/systems/modules/modules/other/Foo.java`
3. **Edit** it. Minecraft APIs appear as intermediary names (`class_1799`,
   `method_7909`, …) — this is expected (see §6). Use `decompiled/` for cross-reference.
4. `./gradlew deploy`, restart Minecraft.

### Decompiler artifacts
Vineflower output usually compiles as-is, but occasionally needs a hand-fix
(e.g. a lost generic on a raw `Map.values()`). Fix the compile error and re-run.

---

## 6. Why Minecraft names look like `class_1799`

We build with **intermediary mappings**, not Yarn, because the shipped mod was
compiled against intermediary. That lets the decompiled source compile **unchanged**.
The trade-off: MC symbols are obfuscated-ish (`class_310`, `method_1551`). Keep
`decompiled/` open for reference, or look names up in a Yarn↔intermediary table.

Our own classes overriding the mod's must keep the **exact same package + name**
(e.g. `moscow.mytheria.systems.modules.modules.other.AutoEnchanter`) so they
replace the originals during the merge.

---

## 7. Regenerating the `decompiled/` reference

`decompiled/` is gitignored (it's large and derivable). To recreate it:

```bash
# Download Vineflower 1.11.1 (vineflower.jar), then:
java -jar vineflower.jar --silent=1 \
  <extracted libs/mytheria-v6.jar>/moscow ./decompiled
```

Or decompile a single class on demand and drop it straight into `src/`.

---

## 8. Logging

`MytheriaLogger` writes one JSON object per line. The path is resolved at runtime —
**no hardcoded user path**, so it works for any Windows account / OS:

1. `<minecraft_run_dir>/mytheria/actions.jsonl`  ← used in practice
2. `%APPDATA%/Mytheria/actions.jsonl`            (Windows fallback)
3. `~/Mytheria/actions.jsonl`                    (macOS/Linux fallback)

Tail it while testing:

```powershell
Get-Content "$env:APPDATA\.tlauncher\legacy\Minecraft\game\mytheria\actions.jsonl" -Wait -Tail 30
```

Event types: `class_loaded`, `module_enable`/`module_disable`, `state_change`,
`craft_stage_change`, `chat_recv`, `buy_search`, `craft_dispatch`, `craft_result`,
`evaluate`. The `craft_*` events are the ones to watch when the bot crafts the wrong
item.

---

## 9. Gotchas / FAQ

- **"Unsupported class file major version 70/69…"** → Gradle is running on too-new a
  Java. See §3c.
- **`Missing libs/mytheria-v6.jar`** → the base JAR isn't present; obtain it and place
  it in `libs/` (or `git lfs pull`).
- **Deploy succeeds but game shows old behavior** → you didn't restart Minecraft, or
  another Mytheria JAR is also in `mods/`. Keep exactly one.
- **Cyrillic looks garbled in console** → cosmetic console encoding only; the JAR and
  log file are UTF-8 and correct.
- **The module is hidden in-game** → it ships hidden behind "Secret Mode"; enable that
  in the Mytheria menu, then the **AutoEnchanter** module appears under *Other*.

---

## 10. Editing the menu sections (quick reference)

Settings are filtered by the `Раздел` tab via a `hideCondition`:

```java
// show only in the "Мечи" tab:
new SliderSetting(this, "Название", () -> !this.section.is(this.secSword))...

// show a single dropdown option only in the "Кирки" tab:
new ModeSetting.Value(this.targetEnchant, "Опция", "", () -> !this.section.is(this.secPickaxe));
```

`secGeneral` / `secPickaxe` / `secSword` are the three section values declared at the
top of `AutoEnchanter`.
