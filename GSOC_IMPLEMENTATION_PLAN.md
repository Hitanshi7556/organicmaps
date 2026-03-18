# GSoC Pre-Proposal: Google Drive Backup/Restore for Organic Maps
## 12-15 Day Implementation Plan (66 hours)

---

## TIMELINE & DELIVERABLES

### **PHASE 1: Days 1-2 - OAuth + Manual Backup (6 hrs)**
**Goal**: User can tap "Backup Now" → Signs in → Uploads bookmarks as ZIP to Drive
**Status**: ✅ IN PROGRESS

**Files to Create/Modify**:
- ✅ `GoogleDriveManager.java` (OAuth) — **DONE**
- ✅ `GoogleDriveClient.java` (Drive API) — **DONE**
- 🔨 `BackupService.java` (Zip & Upload) — **TODO**
- 🔨 `BookmarkBackupHelper.java` (Export bookmarks) — **TODO**
- 🔨 Settings UI Fragment — **TODO**

**Compile & Test**: Should build successfully with manual backup working end-to-end

---

### **PHASE 2: Days 3-7 - SynchronizationStateResolver + Tests (30 hrs)**
**Goal**: Core architecture for sync logic + 20 passing JUnit tests
**Status**: 🔨 TODO (This is your GSoC showcase)

**Files to Create**:
- `SynchronizationStateResolver.java` (~400 lines) — Core sync logic
- `SyncState.java` — Enum for states (EMPTY, LOCAL_ONLY, CLOUD_ONLY, CONFLICT, SYNCED)
- `LocalMetadata.java` — Model for local backup metadata
- `SynchronizationStateResolverTest.java` (~300 lines, 20+ tests)

**Test Coverage**:
- Empty → Empty (no action needed)
- Local-only (upload needed)
- Cloud-only (download needed)
- Conflicts (manual resolution needed)
- Incremental updates (delta sync)
- MD5 checksum validation

---

### **PHASE 3: Days 8-10 - Manual Restore (12 hrs)**
**Goal**: User can tap "Restore from Drive" → Downloads backup → Restores locally
**Status**: 🔨 TODO

**Files to Create/Modify**:
- `RestoreService.java` (Download & Extract)
- Settings UI: Add "Restore Now" button
- Error handling & user feedback

**Compile & Test**: End-to-end demo working on test device

---

### **PHASE 4: Days 11-13 - Settings UI (8 hrs)**
**Goal**: User-facing settings for backup control
**Status**: 🔨 TODO

**UI Components**:
- Toggle: "Enable Google Drive Backup"
- Display: "Last Backup: [timestamp]"
- Button: "Backup Now"
- Button: "Restore Now"
- Status messages: Success/error feedback

---

### **PHASE 5: Days 14-15 - Demo + Proposal (4 hrs)**
**Goal**: Record demo video + write winning GSoC proposal
**Status**: 🔨 TODO

**Demo Video Should Show**:
1. Tap "Backup Now"
2. Sign in with Google
3. Bookmarks upload to Drive
4. Sign in on another device
5. Tap "Restore Now"
6. Bookmarks appear locally
7. End-to-end sync complete ✅

**Proposal Should Include**:
- Architecture diagram (sync states)
- Code snippets from tests (proving quality)
- Roadmap for GSoC phases
- Why your approach is scalable

---

## ARCHITECTURE OVERVIEW

```
User Action (Settings UI)
    ↓
BackupService / RestoreService
    ↓
SynchronizationStateResolver ⭐ (Core logic)
    ↓
GoogleDriveClient (Upload/Download)
    ↓
GoogleDriveManager (OAuth)
    ↓
Google Drive API
```

### Key Files & Responsibilities

| File | Lines | Responsibility |
|------|-------|-----------------|
| GoogleDriveManager.java | 195 | ✅ OAuth sign-in/sign-out |
| GoogleDriveClient.java | 265 | ✅ Drive API CRUD operations |
| BackupService.java | ~150 | 🔨 Export bookmarks → ZIP → Upload |
| RestoreService.java | ~150 | 🔨 Download ZIP → Extract → Import |
| SynchronizationStateResolver.java | ~400 | 🔨 Core sync logic (YOUR SHOWCASE) |
| SynchronizationStateResolverTest.java | ~300 | 🔨 20+ unit tests |
| BookmarkBackupHelper.java | ~80 | 🔨 Bookmark export/import utilities |
| LocalMetadata.java | ~80 | 🔨 Local backup metadata model |
| Settings UI | ~100 | 🔨 User-facing controls |

---

## CURRENT STATUS

### ✅ ALREADY DONE (Google Drive API Setup)
- [x] build.gradle: Google Drive API dependencies added
- [x] GoogleDriveManager.java: OAuth flow complete
- [x] GoogleDriveClient.java: Drive API methods complete
- [x] Fixed dependency conflicts (META-INF issues)

### 🔨 IMMEDIATE NEXT STEPS (TODAY)
1. Create `BackupService.java` — Zip bookmarks & upload
2. Create `BookmarkBackupHelper.java` — Export bookmarks to JSON
3. Create Settings UI fragment with "Backup Now" button
4. Compile & test backup flow end-to-end

---

## HOW TO GET BOOKMARKS

The Organic Maps Android app uses **JNI/C++ bridge** for bookmarks:

```java
// In BookmarkManager.java or similar (already exists)
nativeGetBookmarks(); // Returns List<Bookmark>
```

**For MVP (Days 1-2)**: Use existing Bookmark APIs to export as JSON → ZIP

**For GSoC**: Add JNI bridge to monitor file changes in real-time

---

## SUCCESS METRICS

### By End of Day 2:
- ✅ Backup feature compiles
- ✅ User can sign in with Google
- ✅ Bookmarks exported as ZIP
- ✅ ZIP uploaded to Google Drive
- ✅ Manual backup demo works

### By End of Day 7:
- ✅ SynchronizationStateResolver complete & tested
- ✅ 20+ unit tests all passing
- ✅ Architecture proven with code quality

### By End of Day 10:
- ✅ Restore feature complete
- ✅ End-to-end backup + restore demo works
- ✅ All manual operations tested

### By End of Day 15:
- ✅ Settings UI polished
- ✅ Demo video recorded
- ✅ Winning proposal written
- ✅ Ready for GSoC submission

---

## HOW GSOC PHASES BUILD ON THIS

```
Pre-Proposal (Days 1-15):          GSoC Phase 1 (Week 1-2):
Manual Backup ✅                    → Add JNI bridge
Manual Restore ✅                   → Live bookmark reload
Settings UI ✅
SyncStateResolver ✅ (foundation)   → FileObserver (weeks 2-3)
                                   → WorkManager (weeks 3-4)
                                   → Conflict resolution UI (weeks 5+)
```

**Key Principle**: Your code NEVER changes. New code builds ON TOP.

---

## NEXT ACTION: START DAY 1

Ready to build? Let's start with:

1. **BackupService.java** — Export bookmarks & create ZIP
2. **BookmarkBackupHelper.java** — JSON serialization
3. **Settings UI** — "Backup Now" button

Let's go! 🚀

