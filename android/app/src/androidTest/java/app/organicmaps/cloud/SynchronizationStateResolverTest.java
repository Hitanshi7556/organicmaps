package app.organicmaps.cloud;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Comprehensive unit tests for SynchronizationStateResolver - MVP Phase 2.
 * Tests all 7 sync states and edge cases.
 * 
 * Test coverage:
 * - Empty states (both empty)
 * - One-sided states (local only, cloud only)
 * - Identical content (CONFLICT_IDENTICAL_MD5)
 * - Different content with timestamps (LOCAL_NEWER, CLOUD_NEWER)
 * - True conflicts (CONFLICT_DIFFERENT_CONTENT)
 * - Edge cases (invalid timestamps, empty MD5, etc.)
 */
public class SynchronizationStateResolverTest {

  private LocalMetadata localMetadata;
  private RemoteMetadata remoteMetadata;

  @Before
  public void setUp() {
    localMetadata = null;
    remoteMetadata = null;
  }

  // ===== TEST 1: Both Empty =====

  @Test
  public void testBothEmpty_ReturnsEMPTY_LOCAL_EMPTY_CLOUD() {
    SyncState state = SynchronizationStateResolver.resolveSyncState(null, null);
    assertEquals(SyncState.EMPTY_LOCAL_EMPTY_CLOUD, state);
  }

  @Test
  public void testBothEmptyWithEmptyObjects_ReturnsEMPTY_LOCAL_EMPTY_CLOUD() {
    localMetadata = new LocalMetadata();
    remoteMetadata = new RemoteMetadata();

    SyncState state = SynchronizationStateResolver.resolveSyncState(localMetadata, remoteMetadata);
    assertEquals(SyncState.EMPTY_LOCAL_EMPTY_CLOUD, state);
  }

  // ===== TEST 2-3: Local-Only States =====

  @Test
  public void testLocalOnlyNoCloud_ReturnsLOCAL_ONLY() {
    localMetadata = new LocalMetadata("local_id", 1000L, "abc123", 5000L, 10);
    remoteMetadata = new RemoteMetadata();

    SyncState state = SynchronizationStateResolver.resolveSyncState(localMetadata, remoteMetadata);
    assertEquals(SyncState.LOCAL_ONLY, state);
  }

  @Test
  public void testLocalOnlyNullCloud_ReturnsLOCAL_ONLY() {
    localMetadata = new LocalMetadata("local_id", 1000L, "abc123", 5000L, 10);
    remoteMetadata = null;

    SyncState state = SynchronizationStateResolver.resolveSyncState(localMetadata, remoteMetadata);
    assertEquals(SyncState.LOCAL_ONLY, state);
  }

  // ===== TEST 4-5: Cloud-Only States =====

  @Test
  public void testCloudOnlyNoLocal_ReturnsCLOUD_ONLY() {
    localMetadata = null;
    remoteMetadata = new RemoteMetadata("cloud_id", 1000L, "cloud123", 10, 5000L);

    SyncState state = SynchronizationStateResolver.resolveSyncState(localMetadata, remoteMetadata);
    assertEquals(SyncState.CLOUD_ONLY, state);
  }

  @Test
  public void testCloudOnlyEmptyLocal_ReturnsCLOUD_ONLY() {
    localMetadata = new LocalMetadata();
    remoteMetadata = new RemoteMetadata("cloud_id", 1000L, "cloud123", 10, 5000L);

    SyncState state = SynchronizationStateResolver.resolveSyncState(localMetadata, remoteMetadata);
    assertEquals(SyncState.CLOUD_ONLY, state);
  }

  // ===== TEST 6: Identical Files (CONFLICT_IDENTICAL_MD5) =====

  @Test
  public void testIdenticalMD5_ReturnsCONFLICT_IDENTICAL_MD5() {
    String sharedMd5 = "identical_hash_123";
    localMetadata = new LocalMetadata("local_id", 1000L, sharedMd5, 5000L, 10);
    remoteMetadata = new RemoteMetadata("cloud_id", 1000L, sharedMd5, 10, 5000L);

    SyncState state = SynchronizationStateResolver.resolveSyncState(localMetadata, remoteMetadata);
    assertEquals(SyncState.CONFLICT_IDENTICAL_MD5, state);
  }

  @Test
  public void testIdenticalMD5_DifferentTimestamps_StillSynced() {
    String sharedMd5 = "same_content";
    localMetadata = new LocalMetadata("local_id", 1000L, sharedMd5, 5000L, 10);
    remoteMetadata = new RemoteMetadata("cloud_id", 2000L, sharedMd5, 10, 5000L);

    SyncState state = SynchronizationStateResolver.resolveSyncState(localMetadata, remoteMetadata);
    assertEquals(SyncState.CONFLICT_IDENTICAL_MD5, state);
  }

  // ===== TEST 7-8: Different Content, Local Newer =====

  @Test
  public void testLocalNewer_ReturnsLOCAL_NEWER() {
    localMetadata = new LocalMetadata("local_id", 2000L, "local_hash", 6000L, 15);
    remoteMetadata = new RemoteMetadata("cloud_id", 1000L, "cloud_hash", 10, 5000L);

    SyncState state = SynchronizationStateResolver.resolveSyncState(localMetadata, remoteMetadata);
    assertEquals(SyncState.LOCAL_NEWER, state);
  }

  @Test
  public void testLocalMuchNewer_ReturnsLOCAL_NEWER() {
    localMetadata = new LocalMetadata("local_id", 86400000L, "new_hash", 10000L, 50);
    remoteMetadata = new RemoteMetadata("cloud_id", 1000L, "old_hash", 5, 2000L);

    SyncState state = SynchronizationStateResolver.resolveSyncState(localMetadata, remoteMetadata);
    assertEquals(SyncState.LOCAL_NEWER, state);
  }

  // ===== TEST 9-10: Different Content, Cloud Newer =====

  @Test
  public void testCloudNewer_ReturnsCLOUD_NEWER() {
    localMetadata = new LocalMetadata("local_id", 1000L, "local_hash", 5000L, 10);
    remoteMetadata = new RemoteMetadata("cloud_id", 2000L, "cloud_hash", 15, 6000L);

    SyncState state = SynchronizationStateResolver.resolveSyncState(localMetadata, remoteMetadata);
    assertEquals(SyncState.CLOUD_NEWER, state);
  }

  @Test
  public void testCloudMuchNewer_ReturnsCLOUD_NEWER() {
    localMetadata = new LocalMetadata("local_id", 1000L, "old_hash", 2000L, 5);
    remoteMetadata = new RemoteMetadata("cloud_id", 86400000L, "new_hash", 50, 10000L);

    SyncState state = SynchronizationStateResolver.resolveSyncState(localMetadata, remoteMetadata);
    assertEquals(SyncState.CLOUD_NEWER, state);
  }

  // ===== TEST 11-12: True Conflicts =====

  @Test
  public void testSameTimestampDifferentMD5_ReturnsCONFLICT_DIFFERENT_CONTENT() {
    long timestamp = 1000L;
    localMetadata = new LocalMetadata("local_id", timestamp, "hash_a", 5000L, 10);
    remoteMetadata = new RemoteMetadata("cloud_id", timestamp, "hash_b", 15, 6000L);

    SyncState state = SynchronizationStateResolver.resolveSyncState(localMetadata, remoteMetadata);
    assertEquals(SyncState.CONFLICT_DIFFERENT_CONTENT, state);
  }

  @Test
  public void testInvalidLocalTimestamp_ReturnsCONFLICT() {
    localMetadata = new LocalMetadata("local_id", 0L, "local_hash", 5000L, 10);
    remoteMetadata = new RemoteMetadata("cloud_id", 1000L, "cloud_hash", 10, 5000L);

    SyncState state = SynchronizationStateResolver.resolveSyncState(localMetadata, remoteMetadata);
    assertEquals(SyncState.CONFLICT_DIFFERENT_CONTENT, state);
  }

  @Test
  public void testInvalidRemoteTimestamp_ReturnsCONFLICT() {
    localMetadata = new LocalMetadata("local_id", 1000L, "local_hash", 5000L, 10);
    remoteMetadata = new RemoteMetadata("cloud_id", 0L, "cloud_hash", 10, 5000L);

    SyncState state = SynchronizationStateResolver.resolveSyncState(localMetadata, remoteMetadata);
    assertEquals(SyncState.CONFLICT_DIFFERENT_CONTENT, state);
  }

  // ===== TEST 13-14: Edge Cases =====

  @Test
  public void testEmptyLocalMD5_TreatedAsDifferent() {
    localMetadata = new LocalMetadata("local_id", 1000L, "", 5000L, 10);
    remoteMetadata = new RemoteMetadata("cloud_id", 1000L, "cloud_hash", 10, 5000L);

    SyncState state = SynchronizationStateResolver.resolveSyncState(localMetadata, remoteMetadata);
    assertEquals(SyncState.CONFLICT_DIFFERENT_CONTENT, state);
  }

  @Test
  public void testBothEmptyMD5_SameTimestamp_StillConflict() {
    localMetadata = new LocalMetadata("local_id", 1000L, "", 5000L, 10);
    remoteMetadata = new RemoteMetadata("cloud_id", 1000L, "", 10, 5000L);

    SyncState state = SynchronizationStateResolver.resolveSyncState(localMetadata, remoteMetadata);
    assertEquals(SyncState.CONFLICT_DIFFERENT_CONTENT, state);
  }

  @Test
  public void testZeroBookmarkCount_StillValid() {
    localMetadata = new LocalMetadata("local_id", 1000L, "empty_hash", 0L, 0);
    remoteMetadata = new RemoteMetadata();

    SyncState state = SynchronizationStateResolver.resolveSyncState(localMetadata, remoteMetadata);
    assertEquals(SyncState.LOCAL_ONLY, state);
  }

  @Test
  public void testVeryLargeFileSize_StillValid() {
    localMetadata = new LocalMetadata("local_id", 1000L, "big_file", 1000000000L, 5000);
    remoteMetadata = new RemoteMetadata();

    SyncState state = SynchronizationStateResolver.resolveSyncState(localMetadata, remoteMetadata);
    assertEquals(SyncState.LOCAL_ONLY, state);
  }

  // ===== TEST 15-16: Recommended Actions =====

  @Test
  public void testRecommendedAction_LocalOnly() {
    String action = SynchronizationStateResolver.getRecommendedAction(SyncState.LOCAL_ONLY);
    assertTrue(action.contains("Backup"));
  }

  @Test
  public void testRecommendedAction_CloudOnly() {
    String action = SynchronizationStateResolver.getRecommendedAction(SyncState.CLOUD_ONLY);
    assertTrue(action.contains("Restore"));
  }

  @Test
  public void testRecommendedAction_LocalNewer() {
    String action = SynchronizationStateResolver.getRecommendedAction(SyncState.LOCAL_NEWER);
    assertTrue(action.contains("backup") || action.contains("cloud"));
  }

  @Test
  public void testRecommendedAction_CloudNewer() {
    String action = SynchronizationStateResolver.getRecommendedAction(SyncState.CLOUD_NEWER);
    assertTrue(action.contains("Restore") || action.contains("restore"));
  }

  // ===== TEST 17-18: SyncState Helper Methods =====

  @Test
  public void testIsConflict_IdenticalMD5() {
    assertTrue(SyncState.CONFLICT_IDENTICAL_MD5.isConflict());
  }

  @Test
  public void testIsConflict_DifferentContent() {
    assertTrue(SyncState.CONFLICT_DIFFERENT_CONTENT.isConflict());
  }

  @Test
  public void testIsConflict_NotConflict() {
    assertFalse(SyncState.LOCAL_ONLY.isConflict());
    assertFalse(SyncState.CLOUD_ONLY.isConflict());
    assertFalse(SyncState.LOCAL_NEWER.isConflict());
  }

  @Test
  public void testIsInSync_IdenticalMD5() {
    assertTrue(SyncState.CONFLICT_IDENTICAL_MD5.isInSync());
  }

  @Test
  public void testIsInSync_Empty() {
    assertTrue(SyncState.EMPTY_LOCAL_EMPTY_CLOUD.isInSync());
  }

  @Test
  public void testRequiresAction_LocalOnly() {
    assertTrue(SyncState.LOCAL_ONLY.requiresAction());
  }

  @Test
  public void testRequiresAction_CloudNewer() {
    assertTrue(SyncState.CLOUD_NEWER.requiresAction());
  }

  @Test
  public void testRequiresAction_Synced() {
    assertFalse(SyncState.CONFLICT_IDENTICAL_MD5.requiresAction());
  }

  // ===== TEST 19-20: Metadata Creation =====

  @Test
  public void testCreateLocalMetadata_AllFields() {
    LocalMetadata meta = SynchronizationStateResolver.createLocalMetadata(
        "file_id",
        1000L,
        "md5_hash",
        5000L,
        10
    );

    assertEquals("file_id", meta.getLocalFileId());
    assertEquals(1000L, meta.getLocalTimestamp());
    assertEquals("md5_hash", meta.getLocalMd5());
    assertEquals(5000L, meta.getLocalFileSize());
    assertEquals(10, meta.getLocalBookmarkCount());
  }

  @Test
  public void testCreateRemoteMetadata_AllFields() {
    RemoteMetadata meta = SynchronizationStateResolver.createRemoteMetadata(
        "drive_id",
        2000L,
        "cloud_md5",
        20,
        10000L
    );

    assertEquals("drive_id", meta.getDriveFileId());
    assertEquals(2000L, meta.getUploadTimestamp());
    assertEquals("cloud_md5", meta.getMd5());
    assertEquals(20, meta.getBookmarkCount());
    assertEquals(10000L, meta.getFileSize());
  }

  // ===== TEST 21: Time Formatting =====

  @Test
  public void testFormatTimeDifference_Seconds() {
    String formatted = SynchronizationStateResolver.formatTimeDifference(30000L);
    assertTrue(formatted.contains("30"));
    assertTrue(formatted.contains("second"));
  }

  @Test
  public void testFormatTimeDifference_Days() {
    long oneDayMs = 24L * 60 * 60 * 1000;
    String formatted = SynchronizationStateResolver.formatTimeDifference(oneDayMs + 3600000L);
    assertTrue(formatted.contains("day"));
  }
}

