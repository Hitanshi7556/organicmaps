package app.organicmaps.cloud;

/**
 * Enum representing the synchronization state between local and cloud backups.
 * Used by SynchronizationStateResolver to determine what action should be taken.
 */
public enum SyncState {
  /**
   * Both local and cloud are empty - nothing to sync.
   * Action: No action needed.
   */
  EMPTY,

  /**
   * Bookmarks exist locally but not on cloud - need to upload.
   * Action: Backup to Google Drive.
   */
  LOCAL_ONLY,

  /**
   * Bookmarks exist on cloud but not locally - need to download.
   * Action: Restore from Google Drive.
   */
  CLOUD_ONLY,

  /**
   * Bookmarks exist in both locations with different content.
   * Action: Manual resolution required (ask user which version to keep).
   */
  CONFLICT,

  /**
   * Local and cloud backups are identical and in sync.
   * Action: No action needed.
   */
  SYNCED,

  /**
   * Local backup is newer than cloud backup - incremental upload recommended.
   * Action: User can choose to backup again.
   */
  LOCAL_NEWER,

  /**
   * Cloud backup is newer than local backup - incremental download recommended.
   * Action: User can choose to restore again.
   */
  CLOUD_NEWER;

  /**
   * Returns true if user action is required (not auto-synced).
   */
  public boolean requiresUserAction() {
    return this == CONFLICT || this == LOCAL_ONLY || this == CLOUD_ONLY;
  }

  /**
   * Returns true if local and cloud are in agreement (no sync needed).
   */
  public boolean isInSync() {
    return this == SYNCED || this == EMPTY;
  }
}

