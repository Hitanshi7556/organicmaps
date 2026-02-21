# Show imported/recorded track names on the map (issue #12198)

This guide is a practical implementation plan for adding **track name labels** directly on the map.

## Goal

Display each visible track name (imported GPX/KML/GeoJSON and recorded tracks) as text on the map.

---

## Fast workarounds (no renderer changes)

If you need a quick user-facing workaround before a full renderer implementation:

1. Keep current behavior and expose track names in the track list / place page only.
2. For “map-visible names”, generate temporary point marks at track centers with the track names as titles.
   - Pros: very fast to ship.
   - Cons: labels are detached from line geometry and can overlap badly.

Use workaround #2 only as a temporary patch.

---

## Proper implementation path

### 1) Understand the current data flow

- Track names live in `kml::TrackData::m_name` and are already parsed/imported.
- Runtime track object is `Track` (`libs/map/track.hpp`, `libs/map/track.cpp`).
- Track line rendering is driven by `df::UserLineMark` and generated in:
  - `libs/drape_frontend/drape_engine.cpp` (collect line render params)
  - `libs/drape_frontend/user_mark_shapes.cpp` (draw line geometry)

Today, `UserLineMark` has no text/title API, so line names cannot be rendered yet.

### 2) Add title API for line marks

Files to change:

- `libs/drape_frontend/user_marks_provider.hpp`
  - Extend `class UserLineMark` with title accessors, for example:
    - `virtual bool HasTitle() const = 0;`
    - `virtual std::string GetTitle() const = 0;`
    - `virtual int GetMinTitleZoom() const = 0;`

- `libs/map/track.hpp`
- `libs/map/track.cpp`
  - Implement these methods in `Track` using `GetName()` and a sensible min zoom.

### 3) Pass title params through line render info

Files to change:

- `libs/drape_frontend/drape_engine.cpp`
  - In the line-mark extraction path (where `UserLineRenderParams` are built), store title text and title zoom.

- `libs/drape_frontend/user_mark_shapes.cpp`
  - During line drawing, for each clipped spline, generate one or more text anchors (middle point of spline is fine for v1).
  - Draw text via existing text shape infrastructure (same pipeline used for POI/bookmark titles).

### 4) Placement rules (v1)

Keep v1 simple and stable:

- Show at most **one label per clipped visible track part**.
- Hide below min zoom (e.g., 13 or 14).
- Skip empty names.
- Reuse existing collision/overlay settings used for map text to avoid heavy overlap.

### 5) Update on edits and visibility changes

Track updates already flow via bookmark manager change tracking. Ensure title redraw happens when:

- track name changes,
- track style/visibility changes,
- category visibility toggles.

### 6) Tests/checks to run

Core checks:

```bash
# 1) Build drape/map targets (Linux, debug)
cmake -S . -B build -DCMAKE_BUILD_TYPE=Debug
cmake --build build -j8

# 2) Run map tests related to bookmarks/tracks
ctest --test-dir build --output-on-failure -R "bookmarks_test|track_statistics_tests|gps_track"
```

Manual validation:

1. Import a GPX with 2+ named tracks.
2. Toggle category visibility on/off.
3. Rename a track.
4. Verify labels appear/disappear/update accordingly.
5. Record a track and save it; verify its name appears on map.

---

## Suggested file checklist

- `libs/drape_frontend/user_marks_provider.hpp`
- `libs/map/track.hpp`
- `libs/map/track.cpp`
- `libs/drape_frontend/drape_engine.cpp`
- `libs/drape_frontend/user_mark_shapes.cpp`
- (optional) tests in `libs/map/map_tests/` if behavior is represented there

---

## Minimal delivery process (step by step)

1. Create branch and sync:
   ```bash
   git checkout -b fix/12198-track-names-on-map
   git fetch origin
   git rebase origin/master
   ```
2. Implement API extension in `UserLineMark` + `Track`.
3. Thread title params through drape engine line render params.
4. Draw labels in user line shape pass.
5. Build + run tests.
6. Manual GPX import/recording check.
7. Commit with a focused message:
   ```bash
   git add libs/drape_frontend/user_marks_provider.hpp libs/map/track.hpp libs/map/track.cpp libs/drape_frontend/drape_engine.cpp libs/drape_frontend/user_mark_shapes.cpp
   git commit -m "[bookmarks] Show track names on map for visible tracks"
   ```
8. Push and open PR with before/after screenshots and test logs.

---

## PR description template

```md
## What
Show imported and recorded track names directly on the map.

## How
- Added title API to `UserLineMark` and implemented it in `Track`.
- Passed track title params through drape line rendering pipeline.
- Rendered one collision-aware label per visible clipped track segment above min zoom.

## Validation
- Build: OK
- Tests: `bookmarks_test`, `track_statistics_tests`, `gps_track*` OK
- Manual: GPX import, track rename, category visibility toggle, track recording save
```
