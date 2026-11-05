# Самообновляющаяся золотая точка: план

> Для исполнителя: используйте superpowers:executing-plans. Шаги отмечены `- [ ]`. Состояние: **план утверждён пользователем в целом, решения приняты, не реализован**.

**Цель:** мод создаёт в Xaero's Minimap золотой вейпоинт крепости и сам двигает его при любом изменении бросков, а если пользователь его удалил, создаёт заново при следующем изменении.

**Архитектура:** чистая логика выбора действия живёт в `core` и проверяется обычными тестами. Всё, что касается классов Xaero, лежит в пакете `dev.strongholdfinder.minimap`, грузится только при установленной миникарте и защищено перехватом исключений.

**Спецификация:** `docs/superpowers/specs/2026-11-05-minimap-and-gold-point-design.md`

## Global Constraints

- Версия игры 26.3, Java 25. Классы Xaero не попадают в jar.
- Миникарта необязательна: `suggests` в `fabric.mod.json`, код миникарты вызывается только после `FabricLoader.isModLoaded("xaerominimap")`.
- Точка только в основном мире. Цвет `WaypointColor.GOLD`, символ `S`.
- Имя точки: `Stronghold ±N` или `Крепость ±N`, где N это погрешность в блоках. Ключи перевода в `ru_ru.json` и `en_us.json`.
- Коммиты заканчиваются строкой `Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>`.

## Review Focus

1. Второй бросок двигает ту же точку, а не создаёт вторую.
2. Пользователь удалил вейпоинт: следующий бросок создаёт его заново.
3. Миникарта не установлена: игра запускается, нет `NoClassDefFoundError`.
4. Среднего нет (меньше двух бросков): вейпоинт убирается.
5. Смена мира и заход в Нетер: точка не создаётся там, где не должна, и не остаётся в чужом мире.

---

### Task 1: Разведка API миникарты

**Files:** `build.gradle`, `gradle.properties`, `src/main/resources/fabric.mod.json`, временный тест в `src/gametest`.

**Interfaces:**
- Produces: проверенный путь к набору вейпоинтов и ответы на три вопроса ниже, записанные в комментарии к `MinimapBridge`.

- [ ] **Step 1:** Подключить миникарту в `build.gradle` как `compileOnly` и `localRuntime` (`maven.modrinth:xaeros-minimap:fabric-26.3-26.6.2`, `transitive = false`). Добавить `xaerominimap` в `suggests` и версию `xaerominimap_version` в `gradle.properties`.
- [ ] **Step 2:** Игровым тестом ответить на три вопроса и записать ответы:
  1. Как из `BuiltInHudModules.MINIMAP` получить текущую `MinimapSession` и что она возвращает до захода в мир.
  2. Видна ли точка после `WaypointSet.add` сразу, и сохраняется ли она в файл без вызова записи.
  3. Что с набором при смене мира и при заходе в Нетер.
- [ ] **Step 3:** Если путь без reflection невозможен, остановиться и спросить пользователя.
- [ ] **Step 4:** Закоммитить `chore: compile against Xaero's Minimap`.

### Task 2: Выбор действия (чистая логика)

**Files:** `src/main/java/dev/strongholdfinder/core/GoldPointAction.java`, `GoldPointName.java`, тесты `GoldPointActionTest.java`, `GoldPointNameTest.java`.

**Interfaces:**
- Produces:
  - `enum GoldPointAction { NONE, CREATE, MOVE, REMOVE }`
  - `GoldPointAction.of(Solution solution, boolean exists)`: среднее есть и точки нет, `CREATE`; есть оба, `MOVE`; среднего нет и точка есть, `REMOVE`; иначе `NONE`
  - `GoldPointName.of(boolean russian, double err): String`, например `Stronghold ±15` и `Крепость ±15`

- [ ] **Step 1: Тесты.** Четыре случая `of`; в `GoldPointNameTest` оба языка, округление погрешности вверх до целого блока и отсутствие минуса на нулевой погрешности.
- [ ] **Step 2:** `./gradlew test --tests '*GoldPoint*'`. Expected: FAIL, классов нет.
- [ ] **Step 3:** Реализовать.
- [ ] **Step 4:** Тот же запуск. Expected: PASS.
- [ ] **Step 5:** Закоммитить `feat: choose the gold point action`.

### Task 3: Вейпоинт миникарты

**Files:** `src/client/java/dev/strongholdfinder/minimap/GoldPoint.java`, `MinimapBridge.java`, `StrongholdFinderClient.java`, файлы перевода.

**Interfaces:**
- Consumes: `GoldPointAction.of`, `GoldPointName.of`, `ThrowStore.solution()`, путь из Task 1.
- Produces: `GoldPoint.sync(Solution)`, `MinimapBridge.isActive()`.

- [ ] **Step 1:** `GoldPoint.sync` находит вейпоинт в текущем наборе по метке (имя начинается с `Stronghold` или `Крепость`, символ `S`, цвет `GOLD`) и выполняет действие: `MOVE` меняет `x`, `z`, имя; `CREATE` добавляет `new Waypoint(x, y, z, name, "S", WaypointColor.GOLD)`; `REMOVE` убирает. `Y` всегда `64` (принятое решение 1 спецификации).
- [ ] **Step 2:** Вызывать `sync` после каждого изменения данных и при `JOIN`. В основном мире, иначе `REMOVE`, если точка осталась.
- [ ] **Step 3:** Обернуть вызовы в `MinimapBridge`: проверка `isModLoaded`, перехват `Throwable`, одно сообщение в лог, отключение до конца сессии. Предупреждение на экране `J` рядом с предупреждением о слое карты.
- [ ] **Step 4:** Наш жёлтый ромб на World Map не рисуется, пока вейпоинт существует и `MinimapBridge.isActive()` (принятое решение 2 спецификации). Без миникарты или после её ошибки ромб рисуется как раньше. Тест: оба состояния.
- [ ] **Step 5:** Закоммитить `feat: keep a gold waypoint on the Xaero minimap`.

### Task 4: Проверка в игре

**Files:** `src/gametest/java/dev/strongholdfinder/test/StrongholdClientGameTest.java`.

- [ ] **Step 1:** Тест с миникартой по списку из раздела «Тесты» спецификации: создание, перемещение той же точки, повторное создание после удаления, удаление при очистке, Нетер, смена мира.
- [ ] **Step 2:** Тест без миникарты: переопределить набор модов в запуске, убедиться в отсутствии ошибок.
- [ ] **Step 3:** `./gradlew clean build` и `./gradlew runClientGameTest`. Expected: BUILD SUCCESSFUL.
- [ ] **Step 4:** Снимки миникарты и карты мира с золотой точкой. Закоммитить `test: cover the gold waypoint`.

### Task 5: Документация и выпуск

- [ ] **Step 1:** Обновить `README.md` (миникарта: золотая точка, она сама двигается, её можно удалить) и `docs/modrinth/description.md`, добавить Xaero's Minimap как необязательную зависимость и новый скриншот.
- [ ] **Step 2:** Поднять `mod_version` до `1.0.2`, собрать, проверить jar без классов Xaero.
- [ ] **Step 3:** Закоммитить `docs: describe the gold waypoint`. Релиз на GitHub только по команде пользователя.
