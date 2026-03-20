package app.organicmaps.cloud;

/**
 * Listener interface for bookmark changes.
 * 
 * Called by BookmarkManager when bookmarks are added, deleted, or modified.
 * Used by auto-backup system to detect when backup is needed.
 */
public interface BookmarkChangeListener {
  /**
   * Called when a bookmark is added.
   * 
   * @param bookmarkId The ID of the added bookmark
   * @param categoryId The ID of the category
   */
  void onBookmarkAdded(long bookmarkId, long categoryId);

  /**
   * Called when a bookmark is deleted.
   * 
   * @param bookmarkId The ID of the deleted bookmark
   * @param categoryId The ID of the category
   */
  void onBookmarkDeleted(long bookmarkId, long categoryId);

  /**
   * Called when a bookmark is modified.
   * 
   * @param bookmarkId The ID of the modified bookmark
   * @param categoryId The ID of the category
   */
  void onBookmarkModified(long bookmarkId, long categoryId);

  /**
   * Called when a bookmark category is added.
   * 
   * @param categoryId The ID of the added category
   */
  void onCategoryAdded(long categoryId);

  /**
   * Called when a bookmark category is deleted.
   * 
   * @param categoryId The ID of the deleted category
   */
  void onCategoryDeleted(long categoryId);

  /**
   * Called when a bookmark category is modified.
   * 
   * @param categoryId The ID of the modified category
   */
  void onCategoryModified(long categoryId);
}

