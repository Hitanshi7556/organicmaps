package app.organicmaps.cloud;

import android.content.Context;
import android.util.Log;

/**
 * Implementation of BookmarkChangeListener for auto-backup system.
 * 
 * Sets the backup_needed flag whenever bookmarks change,
 * allowing BackupWorker to detect changes and trigger backups.
 */
public class AutoBackupBookmarkListener implements BookmarkChangeListener {
  private static final String TAG = "AutoBackupBookmarkListener";

  private final Context mContext;
  private final BackupPreferences mPrefs;

  public AutoBackupBookmarkListener(Context context) {
    mContext = context.getApplicationContext();
    mPrefs = BackupPreferences.getInstance(mContext);
  }

  @Override
  public void onBookmarkAdded(long bookmarkId, long categoryId) {
    Log.d(TAG, "Bookmark added: " + bookmarkId + " in category: " + categoryId);
    markBackupNeeded();
  }

  @Override
  public void onBookmarkDeleted(long bookmarkId, long categoryId) {
    Log.d(TAG, "Bookmark deleted: " + bookmarkId + " from category: " + categoryId);
    markBackupNeeded();
  }

  @Override
  public void onBookmarkModified(long bookmarkId, long categoryId) {
    Log.d(TAG, "Bookmark modified: " + bookmarkId + " in category: " + categoryId);
    markBackupNeeded();
  }

  @Override
  public void onCategoryAdded(long categoryId) {
    Log.d(TAG, "Category added: " + categoryId);
    markBackupNeeded();
  }

  @Override
  public void onCategoryDeleted(long categoryId) {
    Log.d(TAG, "Category deleted: " + categoryId);
    markBackupNeeded();
  }

  @Override
  public void onCategoryModified(long categoryId) {
    Log.d(TAG, "Category modified: " + categoryId);
    markBackupNeeded();
  }

  /**
   * Mark that a backup is needed.
   */
  private void markBackupNeeded() {
    mPrefs.setBackupNeeded(true);
    Log.d(TAG, "Backup marked as needed");
  }
}

