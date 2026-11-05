# Stronghold Finder: план реализации мода

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Клиентский мод Fabric 26.3: клавиши записывают лучи бросков ока, карта Xaero's World Map показывает лучи и средние координаты, экран в стиле Minecraft показывает результат.

**Architecture:** Чистый расчёт и хранение живут в `core` и не зависят от классов игры, кроме Gson. Клиентская часть подключает клавиши, экран и слой карты. Слой карты добавляется через публичный `WorldMap.mapElementRenderHandler.add(...)`, без mixin, и отключается при ошибке, не роняя карту.

**Tech Stack:** Java 25, Gradle 9.6.0, Fabric Loom 1.17.21, Fabric Loader 0.19.5, Fabric API `0.165.0+26.3`, Xaero's World Map `fabric-26.3-1.47.2` (только для компиляции и запуска в разработке), JUnit 5.

**Spec:** `docs/superpowers/specs/2026-11-05-stronghold-finder-mod-design.md`

## Global Constraints

- Корень проекта: `stronghold-finder-mod/`. Все пути в задачах указаны от него.
- `mod id`: `stronghold_finder`. Пакет: `dev.strongholdfinder`. Плагин Loom: `net.fabricmc.fabric-loom`.
- `fabric.mod.json`: `environment: client`, `depends`: `minecraft ~26.3`, `xaeroworldmap >=1.47.2 <1.48`, `fabric-api *`, `java >=25`.
- Классы World Map не попадают в наш jar. Зависимость подключается как `compileOnly` и `localRuntime` из `maven.modrinth:xaeros-world-map:fabric-26.3-1.47.2`, репозиторий `https://api.modrinth.com/maven`.
- Клавиши по умолчанию: `G` записывает луч, `Shift+G` наводит на око, `J` открывает экран. Shift проверяется через `Minecraft.hasShiftDown()`.
- Поведение расчёта берётся из `../stronghold-finder/src/lib/core.ts` и `entry.ts` без изменений: `SIGMA 0.2`, `MIN_ANGLE 2`, `PARALLEL 0.05`, `BACK 8`, те же кольца.
- Весь видимый текст идёт через ключи перевода, в файлах `assets/stronghold_finder/lang/ru_ru.json` и `en_us.json`.
- Сообщения над хотбаром: `player.sendOverlayMessage(Component)`.
- Каждый коммит заканчивается строкой `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.
- Отклонение от спецификации: `ThrowStore` лежит в `core`, а не в клиентской части, потому что без классов игры его можно проверить обычными тестами.

## Review Focus

1. **`Shift+G` сразу после броска.** Око появляется точно над игроком, и угол не определён. Ожидается «Око слишком близко, подождите», а не ложный луч на юг. Тест в задаче 5.
2. **Неограниченный угол игрока.** `getYRot()` может вернуть 725°. В луч и на экран должно попасть значение в диапазоне от −180 до 180. Тест в задаче 4.
3. **Повторное нажатие без движения.** Дубликат не добавляется, игрок видит сообщение. Тест в задаче 4.
4. **Адрес сервера как имя файла.** `play.example.com:25565`, `[::1]:25565` и кириллица в названии мира дают допустимые и разные имена файлов. Тест в задаче 4.
5. **Повреждённый или пустой файл бросков.** Игра не падает, файл становится `.bak`, список начинается пустым. Тест в задаче 4.

---

### Task 1: Каркас проекта

**Files:**
- Create: `settings.gradle`, `build.gradle`, `gradle.properties`, `gradlew`, `gradlew.bat`, `gradle/wrapper/*`, `.gitignore`
- Create: `src/main/resources/fabric.mod.json`, `src/main/resources/assets/stronghold_finder/lang/ru_ru.json`, `en_us.json`
- Create: `src/client/java/dev/strongholdfinder/StrongholdFinderClient.java`

**Interfaces:**
- Produces: `StrongholdFinderClient implements ClientModInitializer`, раздельные source set `main` и `client` через `loom { splitEnvironmentSourceSets() }`.

- [ ] **Step 1: Инициализировать репозиторий и закоммитить документы**

```bash
git init
git add docs
git commit -m "docs: add mod spec and plan"
```

- [ ] **Step 2: Создать wrapper Gradle 9.6.0 и файлы сборки**

Toolchain Java 25, в `settings.gradle` плагин `org.gradle.toolchains.foojay-resolver-convention`, чтобы Gradle сам скачал JDK. `test { useJUnitPlatform() }`, JUnit Jupiter 5. Зависимости из Global Constraints.

- [ ] **Step 3: Проверить сборку**

Run: `./gradlew build`
Expected: `BUILD SUCCESSFUL`, а в `build/libs/` jar без каталога `xaero/`. Проверка: `unzip -l build/libs/*.jar | grep -c xaero/` выводит `0`.

- [ ] **Step 4: Запустить игру**

Run: `./gradlew runClient`
Expected: главное меню открывается, в списке модов есть `stronghold_finder` и `xaeroworldmap`, в логе нет ошибок загрузки.

- [ ] **Step 5: Закоммитить**

```bash
git add .
git commit -m "chore: scaffold Fabric 26.3 project"
```

### Task 2: Проверка слоя на карте

Закрывает главный риск спецификации. Если здесь не получится, работа останавливается, и пользователь решает, добавлять ли mixin.

**Files:**
- Create: `src/client/java/dev/strongholdfinder/xaero/XaeroBridge.java`
- Create: `src/client/java/dev/strongholdfinder/xaero/StrongholdRenderer.java`, `StrongholdReader.java`, `StrongholdProvider.java`
- Modify: `StrongholdFinderClient.java`

**Interfaces:**
- Consumes: `xaero.map.WorldMap.mapElementRenderHandler`, `ElementRenderer<E, C, R>`, `ElementReader<E, C, R>`, `ElementRenderProvider<E, C>`, `ElementRenderInfo`, `MapElementGraphics`.
- Produces: `XaeroBridge.install(): boolean` и `XaeroBridge.isActive(): boolean`; `MapMark` как тип элемента.

- [ ] **Step 1: Установить слой**

`XaeroBridge.install()` ждёт, пока `mapElementRenderHandler` не станет `null`, вызывает `add(new StrongholdRenderer(...))` один раз, ловит `Throwable`, включая `NoClassDefFoundError`. `shouldRender` возвращает `true` только для `ElementRenderLocation.WORLD_MAP`. `getOrder()` выше, чем у меток, чтобы слой был сверху.

- [ ] **Step 2: Нарисовать временную метку**

Провайдер отдаёт один элемент в мировой точке (0, 0). `isOnScreen` возвращает `true`. В `renderElement` с помощью `MapElementGraphics.fill` нарисовать крест и повёрнутую через `pose()` линию на 100 блоков на восток.

- [ ] **Step 3: Выяснить преобразование**

В `runClient` создать мир, открыть карту, поставить метку Xaero или пройти в известную точку. Записать в комментарий над `renderElement`, сколько экранных единиц занимает один блок при заданных `info.scale` и `info.screenSizeBasedScale`.

Expected: крест остаётся в (0, 0) при любом сдвиге и масштабе, конец линии совпадает с X=100 на карте.

- [ ] **Step 4: Проверить обработку ошибок**

Временно бросить исключение в `renderElement`. Expected: карта работает, в логе одна запись, слой отключён через `XaeroBridge.isActive() == false`. Убрать исключение.

- [ ] **Step 5: Закоммитить**

```bash
git add src/client
git commit -m "feat: attach overlay layer to Xaero world map"
```

### Task 3: Расчёт

**Files:**
- Create: `src/main/java/dev/strongholdfinder/core/` файлы `Throw.java`, `Point.java`, `PairStatus.java`, `Pair.java`, `Ring.java`, `Lsq.java`, `Solution.java`, `Compass.java`, `Geometry.java`, `Solver.java`, `Advice.java`
- Test: `src/test/java/dev/strongholdfinder/core/SolverTest.java`, `AdviceTest.java`

**Interfaces:**
- Produces:
  - `record Throw(int id, double x, double z, double yaw, boolean on)`
  - `record Point(double x, double z)`
  - `enum PairStatus { OK, WEAK, BEHIND, PARALLEL, SAME }`
  - `record Pair(String key, Throw a, Throw b, PairStatus status, double angle, double x, double z, boolean usable, boolean used)`, где `x` и `z` равны `NaN`, если точки нет
  - `record Ring(int n, boolean inside, double gap)`
  - `record Lsq(double x, double z, double off)`
  - `record Solution(List<Throw> on, List<Pair> pairs, List<Pair> used, boolean weakFallback, Point mean, double rms, double theory, double err, Lsq lsq, Throw last, double toLast, double yawNext, double origin, Ring ring, double minAngle)`, где `mean` равен `null`, если точки нет
  - `enum Compass { SOUTH, SOUTH_WEST, WEST, NORTH_WEST, NORTH, NORTH_EAST, EAST, SOUTH_EAST }` и `String key()`
  - `Geometry.normYaw(double)`, `dir(double): Point`, `yawTo(Point, Point): double`, `compass(double): Compass`, `RINGS`, `ringOf(double d, double tol): Ring`
  - `Solver.solve(List<Throw>, Map<String, Boolean> over): Solution`
  - `record AdviceLine(String key, boolean warn, Object... args)` и `Advice.of(Solution): List<AdviceLine>`

- [ ] **Step 1: Написать падающие тесты**

Перенести в `SolverTest` разделы `yaw geometry` и `solve` и `rings` из `../stronghold-finder/src/lib/core.test.ts` с теми же значениями: цель (1832, −1240), броски (100, 250), (780, 420), (1500, 200), ожидание `theory` по замкнутой формуле, статусы `PARALLEL`, `BEHIND`, `WEAK`, `SAME`, сумма колец 128.

В `AdviceTest` проверить ключи `advice.first_throw` при 0 бросков, `advice.move_aside` при 1 и `advice.go` при `err <= 30`, `advice.refine` при `err > 30`, `advice.behind`, `advice.weak` и `advice.outside_rings`.

- [ ] **Step 2: Убедиться, что тесты падают**

Run: `./gradlew test --tests '*SolverTest' --tests '*AdviceTest'`
Expected: FAIL, ошибка компиляции из-за отсутствующих классов.

- [ ] **Step 3: Реализовать `Geometry` и `Solver` по `core.ts` и `Advice` по `result-panel.tsx`**

Логика и условия переходят без изменений. В `Advice` значения для советов передаются аргументами, а текст живёт в файлах перевода.

- [ ] **Step 4: Проверить**

Run: `./gradlew test`
Expected: PASS.

- [ ] **Step 5: Закоммитить**

```bash
git add src/main src/test
git commit -m "feat: port stronghold solver"
```

### Task 4: Хранение бросков и разбор ввода

**Files:**
- Create: `src/main/java/dev/strongholdfinder/core/EntryParser.java`, `FieldEntry.java`, `ThrowStore.java`, `WorldKey.java`
- Test: `src/test/java/dev/strongholdfinder/core/EntryTest.java`, `ThrowStoreTest.java`

**Interfaces:**
- Consumes: `Throw`, `Solver.solve`, `Geometry.normYaw`.
- Produces:
  - `record ThrowInput(double x, double z, double yaw)`
  - `sealed interface ParsedPaste` с вариантами `Throws(List<ThrowInput>)`, `WrongDimension`, `Invalid`; `EntryParser.parsePaste(String): ParsedPaste`; `EntryParser.num(String): Double`, возвращает `null` при ошибке
  - `record EntryResult(String[] fields, int focus, int invalid, boolean commit)`, `invalid == -1`, если ошибки нет; `FieldEntry.enterText(String[] fields, int index, String text): EntryResult`; `FieldEntry.read(String[]): ThrowInput`, возвращает `null` при ошибке
  - `WorldKey.of(String raw): String`
  - `enum AddResult { ADDED, DUPLICATE }`; `class ThrowStore` с `ThrowStore(Path dir)`, `load(String key)`, `add(ThrowInput): AddResult`, `addAll(List<ThrowInput>)`, `remove(int id)`, `setOn(int id, boolean)`, `setPair(String key, boolean)`, `clear()`, `undo(): boolean`, `canUndo()`, `throwsList()`, `solution(): Solution`, `overlayVisible()`, `setOverlayVisible(boolean)`, `lastAddedId()`

- [ ] **Step 1: Написать падающие тесты**

`EntryTest`: разделы `num`, `parsePaste`, `enterText`, `readFields` из `core.test.ts` с теми же значениями.

`ThrowStoreTest`, с `@TempDir`:
- `addNormalizesYaw`: `add(new ThrowInput(1, 2, 725))` даёт угол `5`.
- `duplicateIsRejected`: второй такой же `add` возвращает `DUPLICATE`, в списке один бросок.
- `undoRestoresState`: после `add`, `remove` и `undo` бросок снова в списке.
- `survivesReload`: новый `ThrowStore` с тем же каталогом и `load("a")` видит те же броски, флаги и следующий номер.
- `worldsAreSeparate`: броски `load("a")` не видны после `load("b")`.
- `corruptFileBecomesBak`: файл с текстом `{oops` даёт пустой список и файл `a.json.bak`. Пустой файл обрабатывается так же.
- `WorldKey.of("play.example.com:25565")`, `of("[::1]:25565")` и `of("мой мир")` дают разные значения, в которых нет символов `<>:"/\|?*`.

- [ ] **Step 2: Убедиться, что тесты падают**

Run: `./gradlew test --tests '*EntryTest' --tests '*ThrowStoreTest'`
Expected: FAIL, ошибка компиляции.

- [ ] **Step 3: Реализовать**

`EntryParser` и `FieldEntry` переносятся из `entry.ts`. `ThrowStore` хранит JSON через Gson в `<dir>/<key>.json`, запись идёт во временный файл и затем замещает старый. Ограничение отмены: 30 шагов, только в памяти. `WorldKey.of` заменяет запрещённые символы на `_` и добавляет короткий хэш исходной строки, чтобы разные адреса не совпали.

- [ ] **Step 4: Проверить**

Run: `./gradlew test`
Expected: PASS.

- [ ] **Step 5: Закоммитить**

```bash
git add src/main src/test
git commit -m "feat: add throw store and input parsing"
```

### Task 5: Клавиши и наведение на око

**Files:**
- Create: `src/main/java/dev/strongholdfinder/core/EyeAim.java`
- Create: `src/client/java/dev/strongholdfinder/ThrowCapture.java`, `ModKeys.java`
- Modify: `StrongholdFinderClient.java`, файлы перевода
- Test: `src/test/java/dev/strongholdfinder/core/EyeAimTest.java`

**Interfaces:**
- Consumes: `ThrowStore`, `ThrowInput`, `Solution`.
- Produces:
  - `record Aim(double yaw, double pitch)`; `EyeAim.compute(double fromX, double fromY, double fromZ, double toX, double toY, double toZ): Aim`, возвращает `null`, если расстояние по горизонтали меньше `0.5`
  - `record EyeCandidate(double x, double y, double z, int age)`; `EyeAim.pick(List<EyeCandidate>): EyeCandidate`, берёт наименьший `age`, при пустом списке `null`
  - `StrongholdFinderClient.store(): ThrowStore`

- [ ] **Step 1: Написать падающие тесты**

В `EyeAimTest`:
- `aimsSouth`: от (0, 64, 0) к (0, 64, 10) даёт `yaw == 0`, `pitch == 0`.
- `aimsWest`: к (−10, 64, 0) даёт `yaw == 90`.
- `aimsUp`: к (10, 74, 0) даёт `pitch == -45`.
- `tooCloseIsNull`: от (5, 64, 5) к (5.2, 70, 5.1) даёт `null`.
- `picksNewest`: из возрастов 40, 3, 12 выбирает 3. Пустой список даёт `null`.

- [ ] **Step 2: Убедиться, что тесты падают**

Run: `./gradlew test --tests '*EyeAimTest'`
Expected: FAIL, ошибка компиляции.

- [ ] **Step 3: Реализовать `EyeAim`**

`yaw = Geometry.yawTo(from, to)`, `pitch = -toDegrees(atan2(dy, horizontal))`.

- [ ] **Step 4: Проверить**

Run: `./gradlew test --tests '*EyeAimTest'`
Expected: PASS.

- [ ] **Step 5: Подключить клавиши**

`ModKeys` регистрирует две `KeyMapping` через `KeyMappingHelper` из модуля `fabric-key-mapping-api-v1` и свою категорию `KeyMapping.Category(Identifier.fromNamespaceAndPath("stronghold_finder", "main"))`.

`ClientTickEvents.END_CLIENT_TICK` в цикле `consumeClick()`: при `hasShiftDown()` вызывает `ThrowCapture.fromEye()`, иначе `fromLook()`. `J` открывает экран, он появится в задаче 7.

`ClientPlayConnectionEvents.JOIN` вызывает `store.load(WorldKey.of(...))`: для одиночной игры имя папки мира, для сервера адрес из `getCurrentServer().ip`.

- [ ] **Step 6: Реализовать `ThrowCapture`**

- Проверка: `level.dimension() == Level.OVERWORLD`, иначе сообщение `message.stronghold_finder.overworld_only`.
- `fromLook()`: `store.add(new ThrowInput(player.getX(), player.getZ(), player.getYRot()))`.
- `fromEye()`: `getEntitiesOfClass(EyeOfEnder.class, player.getBoundingBox().inflate(64))`, из них `pick`. Нет ока: `message.stronghold_finder.no_eye`. `compute` вернул `null`: `message.stronghold_finder.eye_too_close`. Иначе цель это центр `getBoundingBox()` ока, от `player.getEyePosition()`. Угол и наклон ставятся через `setYRot`, `setXRot`, `yRotO`, `xRotO` и `setYHeadRot`, а в бросок идёт точное значение `aim.yaw()`.
- Итог над хотбаром: `message.stronghold_finder.added` с номером и, если есть средняя точка, `added_result` с X, Z и погрешностью. При дубликате: `message.stronghold_finder.duplicate`.

- [ ] **Step 7: Проверить в игре**

Run: `./gradlew runClient`, новый мир в творческом режиме. Expected:
- `G` пишет сообщение о луче.
- Бросить око и нажать `Shift+G`: камера смотрит на центр ока, луч добавлен.
- `Shift+G` без ока: «Око не найдено», луча нет.
- `G` в незере: сообщение, луча нет.

- [ ] **Step 8: Закоммитить**

```bash
git add src
git commit -m "feat: capture throws with keybinds and eye aim"
```

### Task 6: Настоящий слой карты

**Files:**
- Create: `src/client/java/dev/strongholdfinder/xaero/MapMark.java`
- Modify: `StrongholdRenderer.java`, `StrongholdReader.java`, `StrongholdProvider.java`

**Interfaces:**
- Consumes: `ThrowStore.solution()`, `ThrowStore.throwsList()`, `ThrowStore.overlayVisible()`; масштаб из комментария задачи 2.
- Produces: `sealed interface MapMark` с вариантами `RayMark(Throw)`, `ErrorCircleMark(Point, double)`, `PairMark(Pair)`, `ThrowMark(Throw)`, `MeanMark(Point, double err)`.

- [ ] **Step 1: Собрать элементы**

`begin` строит список в порядке отрисовки: лучи, круг, пересечения с `usable`, броски, средняя точка. Список пуст, если `overlayVisible()` ложно или `info.mapDimension != Level.OVERWORLD`.

- [ ] **Step 2: Нарисовать элементы**

- Линии постоянной экранной толщины: длина и толщина делятся на масштаб. Длина луча: диагональ экрана плюс расстояние от броска до центра вида.
- Цвета лучей: `0x55AAFF, 0xFF55AA, 0xFFAA00, 0xAA88FF, 0x55FFAA, 0xFF5555, 0xFF55FF, 0x00AA66`, номер больше 8 и выключенный бросок: `0x888888` пунктиром.
- Подписи рисует шрифт игры. Средняя точка подписывается всегда, пересечения подписываются, если не перекрывают уже занятые области. Сброс областей в `preRender`.

- [ ] **Step 3: Подсказки**

`isInteractable` истинно для бросков, пересечений и средней точки, ложно для лучей и круга. `getTooltip` возвращает номер и координаты. `isRightClickValid` ложно, `getRightClickOptions` пуст.

- [ ] **Step 4: Проверить в игре**

Run: `./gradlew runClient`. Expected:
- Луч `G` при взгляде на юг идёт вниз по карте.
- Два броска: точка и подпись совпадают с координатами при любом сдвиге и масштабе.
- `/tp` в координаты средней точки ставит игрока на неё на карте.
- На карте незера слоя нет.

- [ ] **Step 5: Закоммитить**

```bash
git add src/client
git commit -m "feat: draw throws and result on world map"
```

### Task 7: Экран в стиле Minecraft

**Files:**
- Create: `src/client/java/dev/strongholdfinder/screen/StrongholdScreen.java`, `ThrowListWidget.java`, `PairListWidget.java`, `ResultPanel.java`
- Modify: `ModKeys.java`, `StrongholdFinderClient.java`, файлы перевода

**Interfaces:**
- Consumes: `ThrowStore`, `Solution`, `Advice.of`, `EntryParser.parsePaste`, `FieldEntry.enterText`, `FieldEntry.read`, `XaeroBridge.isActive()`.
- Produces: `new StrongholdScreen()`.

- [ ] **Step 1: Собрать форму**

Три `EditBox` с `setMaxLength(128)`. В `setResponder`: если текст содержит пробел и `parsePaste` даёт `Throws`, бросок добавляется, а поля очищаются. Иначе результат `enterText` применяется к полям и фокусу. При `WrongDimension` показывается ошибка. Enter в `keyPressed` передаёт в `enterText` текст плюс `\n`. Рекурсию от `setValue` исключает флаг на время применения результата.

- [ ] **Step 2: Собрать результат**

`ResultPanel` рисует через `GuiGraphicsExtractor.text`: X и Z крупно, погрешность с оценкой (`<=30` точно, `<=100` нормально, иначе грубо), расстояние и угол, незер, кольцо, чанк, советы. Цвет оценки: зелёный, жёлтый и красный из палитры игры.

- [ ] **Step 3: Собрать списки и кнопки**

Два `ContainerObjectSelectionList` с прокруткой. Строка броска: цветной номер, X, Z, угол, сторона света, кнопка вкл/выкл, кнопка удаления. Строка пересечения: лучи, X, Z, угол, статус, кнопка вкл/выкл, если `usable`. Внизу кнопки «Копировать» (`X ~ Z` в `keyboardHandler.setClipboard`), «Отменить», «Очистить», «Показывать на карте», «Готово». При `!XaeroBridge.isActive()` строка предупреждения. `isPauseScreen()` возвращает `false`.

- [ ] **Step 4: Проверить в игре**

Run: `./gradlew runClient`, окно 1280×720, интерфейс 2. Expected:
- `120 310 -47.3` и пробел добавляют бросок, фокус в X.
- То же работает через Enter.
- `abc` и пробел: фокус на месте, ошибка.
- Ctrl+V строки F3+C и трёх чисел добавляет бросок.
- Отмена, очистка и переключатели меняют результат и карту.
- Все элементы видны, текст не обрезан.

- [ ] **Step 5: Закоммитить**

```bash
git add src/client src/main/resources
git commit -m "feat: add Minecraft-style stronghold screen"
```

### Task 8: Защита слоя, README и итоговая проверка

**Files:**
- Modify: `XaeroBridge.java`, `StrongholdRenderer.java`
- Create: `README.md`

**Interfaces:**
- Consumes: всё из предыдущих задач.

- [ ] **Step 1: Обернуть вызовы отрисовки**

`renderElement`, `preRender`, `postRender` и провайдер перехватывают `Throwable`, вызывают `XaeroBridge.disable(Throwable)` один раз и больше не рисуют. Состояние `pose()` восстанавливается.

- [ ] **Step 2: Написать README**

Установка: Fabric API и World Map `1.47.x` рядом с jar. Клавиши. Совет не двигаться между броском и `Shift+G`. Предупреждение, что автонаведение может быть запрещено на серверах.

- [ ] **Step 3: Прогнать сборку**

Run: `./gradlew clean build`
Expected: `BUILD SUCCESSFUL`, все тесты проходят, `unzip -l build/libs/*.jar | grep -c xaero/` выводит `0`.

- [ ] **Step 4: Пройти чек-лист спецификации**

Run: `./gradlew runClient`. Пройти шесть пунктов раздела «Проверка». Отдельно: выйти в меню, зайти в другой мир, вернуться и перезапустить игру, список должен совпадать.

- [ ] **Step 5: Закоммитить**

```bash
git add .
git commit -m "feat: harden overlay and document the mod"
```

- [ ] **Step 6: Предложить установку**

Спросить пользователя, скопировать ли jar, Xaero's World Map и обновлённые файлы в профиль Modrinth «New instance 26.3». Без подтверждения профиль не менять.
