package app.organicmaps.cloud;

/**
 * Enum representing the synchronization state between local and cloud backups.
 * Used by SynchronizationStateResolver to determine what action should be taken.
 * 
 * This is the MVP (Phase 2) implementation with 7 states.
 * Future phases will add deletion tracking and advanced conflict resolution.
 */
public enum SyncState {
  /**
   * Both local and cloud are empty - nothing to sync.
   * Action: No action needed - skip sync.
   */
  EMPTY_LOCAL_EMPTY_CLOUD,

  /**
   * Bookmarks exist locally but not on cloud - need to upload.
   * Action: Backup bookmarks to Google Drive.
   */
  LOCAL_ONLY,

  /**
   * Bookmarks exist on cloud but not locally - need to download.
   * Action: Restore bookmarks from Google Drive.
   */
  CLOUD_ONLY,

  /**
   * Local and cloud have identical content (same MD5 hash).
   * Action: No action needed - already in sync.
   */
  CONFLICT_IDENTICAL_MD5,

  /**
   * Local and cloud have different content but same timestamp.
   * This is a true conflict that requires user decision.
   * Action: Manual resolution (ask user which version to keep).
   */
  CONFLICT_DIFFERENT_CONTENT,

  /**
   * Local backup is newer than cloud backup (local timestamp > cloud timestamp).
   * Action: Backup local changes to cloud (upload).
   */
  LOCAL_NEWER,

  /**
   * Cloud backup is newer than local backup (cloud timestamp > local timestamp).
   * Action: Restore from cloud (download).
   */
  CLOUD_NEWER;

  /**
   * Returns true if this is a conflict state that needs user intervention.
   */
  public boolean isConflict() {
    return this == CONFLICT_DIFFERENT_CONTENT || this == CONFLICT_IDENTICAL_MD5;
  }

  /**
   * Returns true if sync is complete and no action needed.
   */
  public boolean isInSync() {
    return this == EMPTY_LOCAL_EMPTY_CLOUD || this == CONFLICT_IDENTICAL_MD5;
  }

  /**
   * Returns true if an action should be taken (upload or download).
   */
  public boolean requiresAction() {
    return this == LOCAL_ONLY || this == CLOUD_ONLY || 
           this == LOCAL_NEWER || this == CLOUD_NEWER;
  }
}

