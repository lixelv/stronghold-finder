# 1.1.4 Ring Check and Ray Numbers Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Eye ray on G, per-crossing ring check with dim grey-red crossings and a red out-of-ring mean, and gap-free shown ray numbers.

**Architecture:** Two pure core pieces carry the logic: `RayNumbers` (real id → shown number from list order) and a new `Pair.inRing` flag set by `Solver`. Client code only reads them for colors and labels.

**Tech Stack:** Java 25, Fabric 26.3, Xaero's World Map / Minimap APIs, JUnit 5, Fabric client gametests.

**Spec:** `docs/superpowers/specs/2026-11-14-ring-check-and-ray-numbers-design.md`

## Global Constraints

- Real `Throw.id()` stays unchanged in saves, pair keys (`"a-b"`) and undo.
- Out-of-ring crossings still count toward the mean.
- Per-crossing margin: 64 blocks. Mean check: existing `Solution.ring().inside()`, unchanged.
- Red: `0xFFFF5555`. Dim grey-red: `0xFF996666`.
- Version: `mod_version=1.1.4`.

## Review Focus

- Deleting the first ray: every remaining ray drops by one, colors follow (RayNumbersTest `deleteShiftsLaterRaysOnly`).
- Unknown id asked from `RayNumbers` (stale reference after delete) returns 0, never throws (RayNumbersTest `unknownIdIsZero`).
- Crossing exactly on a ring edge or within 64 blocks outside counts as in ring (SolverTest `crossingNearRingEdgeIsInRing`).
- Mean outside rings while all crossings are inside is still red (covered by mean using `ring().inside()`; SolverTest `meanOutsideRingKeepsItsOwnCheck`).
- Older saves without new fields still load (no save format change; existing ThrowStoreTest stays green).

---

### Task 1: Shown ray numbers in core

**Files:**
- Create: `src/main/java/dev/strongholdfinder/core/RayNumbers.java`
- Test: `src/test/java/dev/strongholdfinder/core/RayNumbersTest.java`

**Interfaces:**
- Produces: `public final class RayNumbers { static RayNumbers of(List<Throw> all); int of(Throw t); int of(int id); }` — 1-based position of the id in `all`, 0 for an id not in the list.

- [ ] **Step 1: Write failing tests** `numbersFollowListOrder` (ids 1,2,3 → 1,2,3), `deleteShiftsLaterRaysOnly` (ids 2,3,5 → 1,2,3; and ids 1,3 → 1,2), `switchedOffRayKeepsNumber` (off throw still numbered), `unknownIdIsZero`.
- [ ] **Step 2: Run** `./gradlew test --tests '*RayNumbersTest'` → FAIL (class missing).
- [ ] **Step 3: Implement** with a `Map<Integer,Integer>` built once in `of(List)`.
- [ ] **Step 4: Run** same command → PASS.
- [ ] **Step 5: Commit** `feat: number rays by their place in the list`.

### Task 2: Ring flag on crossings

**Files:**
- Modify: `src/main/java/dev/strongholdfinder/core/Pair.java` (add `boolean inRing` as the last component)
- Modify: `src/main/java/dev/strongholdfinder/core/Solver.java` (set it; keep the weak-fallback copy passing it through)
- Modify: `src/main/java/dev/strongholdfinder/core/Geometry.java` (add `public static boolean inRing(double distance)` = `ringOf(distance, 64).inside()`)
- Test: `src/test/java/dev/strongholdfinder/core/SolverTest.java`

**Interfaces:**
- Produces: `Pair.inRing()` — true for a usable crossing whose distance from 0,0 is within any ring ± 64; false for out-of-ring crossings and for crossings with NaN coordinates.
- Produces: `Geometry.inRing(double distance)`.

- [ ] **Step 1: Write failing tests** `crossingInsideRingIsInRing` (two rays crossing at ~(1500, 0)), `crossingFarOutsideRingIsNotInRingButCounts` (crossing at ~(600, 0): `inRing()==false`, `used()==true`, mean includes it), `crossingNearRingEdgeIsInRing` (distance 1280 − 50), `meanOutsideRingKeepsItsOwnCheck` (single pair at ~(600,0) → `ring().inside()==false`).
- [ ] **Step 2: Run** `./gradlew test --tests '*SolverTest'` → FAIL to compile.
- [ ] **Step 3: Implement**; fix every `new Pair(...)` call site in main and test code.
- [ ] **Step 4: Run** `./gradlew test` → all PASS.
- [ ] **Step 5: Commit** `feat: mark crossings outside the stronghold rings`.

### Task 3: Shown numbers in the client

**Files:**
- Modify: `src/client/java/dev/strongholdfinder/RayColors.java` (`hasOwnColor(Throw, int number)`, `of(Throw, int number)`; color index `number - 1`)
- Modify: `src/client/java/dev/strongholdfinder/xaero/StrongholdRenderer.java` (rays, throw squares, pair labels)
- Modify: `src/client/java/dev/strongholdfinder/xaero/StrongholdReader.java` (tooltips)
- Modify: `src/client/java/dev/strongholdfinder/xaero/MapFrame.java` (pair label text if built there)
- Modify: `src/client/java/dev/strongholdfinder/screen/ThrowListWidget.java`, `PairListWidget.java`, `ResultPanel.java` (`walk` row)
- Modify: `src/client/java/dev/strongholdfinder/ThrowCapture.java` (`added` / `added_result` messages show the shown number)

**Interfaces:**
- Consumes: `RayNumbers.of(store.throwsList())` from Task 1, built once per frame / per screen refresh.

- [ ] **Step 1:** Replace every user-visible `id()` with the shown number; keep `id()` for `store.remove`, `setOn`, pair keys.
- [ ] **Step 2: Run** `./gradlew build` → BUILD SUCCESSFUL.
- [ ] **Step 3: Commit** `feat: show gap-free ray numbers everywhere`.

### Task 4: Ring colors in the client

**Files:**
- Modify: `StrongholdRenderer.java` (`drawDot`: out-of-ring crossing filled `0xFF996666` when used, edged with it when not; `drawMean`: `0xFFFF5555` instead of `MEAN_COLOR` when `!solution.ring().inside()`)
- Modify: `MapMark.java` (`MeanMark` gains `boolean inRing`), `MapFrame.java` (pass `solution.ring().inside()`)
- Modify: `PairListWidget.java` (row text `0xFF996666` when `!pair.inRing()`)
- Modify: `ResultPanel.java` (ring value and coordinates `RED` when outside)
- Modify: `src/client/java/dev/strongholdfinder/minimap/GoldPoint.java` (waypoint `WaypointColor.RED` when outside; `isOurs` accepts GOLD or RED)

- [ ] **Step 1:** Implement the colors above.
- [ ] **Step 2: Run** `./gradlew build` → BUILD SUCCESSFUL.
- [ ] **Step 3: Commit** `feat: color crossings and the mean outside the rings`.

### Task 5: Eye ray on G, look ray on Shift+G

**Files:**
- Modify: `src/client/java/dev/strongholdfinder/StrongholdFinderClient.java:44-48` (swap `fromEye` / `fromLook`)
- Modify: `src/main/resources/assets/stronghold_finder/lang/en_us.json`, `ru_ru.json` (`key.stronghold_finder.capture`, `advice.first_throw`)
- Modify: `README.md`, `docs/modrinth/description.md` (key descriptions)
- Modify: gametests that press G / Shift+G expecting the old meaning

- [ ] **Step 1:** Swap and update texts: ru capture `"Добавить луч по летящему оку (Shift: по прицелу)"`, en `"Add ray from the flying eye (Shift: from your aim)"`.
- [ ] **Step 2: Run** `./gradlew build runClientGameTest` → BUILD SUCCESSFUL.
- [ ] **Step 3: Commit** `feat: read the flying eye on G, aim on Shift+G`.

### Task 6: Release 1.1.4

- [ ] **Step 1:** `mod_version=1.1.4` in `gradle.properties`.
- [ ] **Step 2: Run** `./gradlew build runClientGameTest` → BUILD SUCCESSFUL, 0 failed tests.
- [ ] **Step 3:** Commit `chore: release 1.1.4`, push, tag `v1.1.4`, `gh release create v1.1.4 build/libs/stronghold-finder-1.1.4.jar` with notes in the 1.1.2 style.
