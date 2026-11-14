# 1.1.4: eye ray on G, ring check, relative ray numbers

## Why

On speedrun servers the eye-flight ray is the main way to record a throw, so it belongs on the plain key.
Crossings far outside every stronghold ring are almost always bad throws, and the player should see that at a glance.
Ray numbers with gaps after a delete (1, 3, 4) are confusing and waste palette colors.

## Goals

1. **Keys.** G records a ray from the eye's flight (today's Shift+G). Shift+G records a ray from the look direction (today's G).
2. **Ring check per crossing.** Every usable crossing is checked against the stronghold rings with a 64-block margin.
   A crossing outside every ring is drawn dim grey-red on Xaero's map and its row in the pair list uses the same color.
   It still counts toward the mean exactly as before.
3. **Red mean.** When the mean is outside every ring (the existing `Solution.ring().inside()` check, unchanged), the mean marker on the map,
   the gold waypoint label and the ring line in the result panel are red instead of the usual color.
4. **Relative ray numbers.** A ray's shown number is its position in the throw list, starting at 1 with no gaps.
   Deleting a ray shifts every later ray down by one. Colors, map labels, the throw list, pair labels such as `1×2`
   and "walk from ray N" all use the shown number. A ray that is switched off keeps its number and stays muted.

## Non-goals

- Several candidate strongholds or clusters. There is one mean.
- Excluding out-of-ring crossings from the mean.
- Changing stored ids: the real `Throw.id()` stays in saves, pair keys and undo.

## Approaches

- **Shown number derived from list position (chosen).** One pure helper maps a throw to its 1-based index in the store's list.
  Saves stay compatible and pair overrides keyed by real ids keep working.
- Renumbering stored ids on delete. Rejected: breaks pair override keys, undo snapshots and older saves.
- Keeping a deleted-count C per ray. Same result as list position but more state to keep in sync.

## Builder's calls

- Margin for the per-crossing check: 64 blocks, the same margin the mean already gets on top of its error.
- Exact dim grey-red and red shades; red reuses the existing `0xFFFF5555`.
