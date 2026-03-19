package app.organicmaps.cloud;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Model representing cloud backup metadata.
 * Tracks the state of the most recent backup on Google Drive for sync comparison.
 */
public class RemoteMetadata {
  private String mDriveFileId;
  private long mUploadTimestamp;
  private String mMd5;
  private int mBookmarkCount;
  private long mFileSize;

  public RemoteMetadata() {
    this.mDriveFileId = "";
    this.mUploadTimestamp = 0;
    this.mMd5 = "";
    this.mBookmarkCount = 0;
    this.mFileSize = 0;
  }

  public RemoteMetadata(String fileId, long timestamp, String md5, int bookmarkCount, long fileSize) {
    this.mDriveFileId = fileId;
    this.mUploadTimestamp = timestamp;
    this.mMd5 = md5;
    this.mBookmarkCount = bookmarkCount;
    this.mFileSize = fileSize;
  }

  @NonNull
  public String getDriveFileId() {
    return mDriveFileId;
  }

  public void setDriveFileId(@NonNull String fileId) {
    this.mDriveFileId = fileId;
  }

  public long getUploadTimestamp() {
    return mUploadTimestamp;
  }

  public void setUploadTimestamp(long timestamp) {
    this.mUploadTimestamp = timestamp;
  }

  @NonNull
  public String getMd5() {
    return mMd5;
  }

  public void setMd5(@NonNull String md5) {
    this.mMd5 = md5;
  }

  public int getBookmarkCount() {
    return mBookmarkCount;
  }

  public void setBookmarkCount(int count) {
    this.mBookmarkCount = count;
  }

  public long getFileSize() {
    return mFileSize;
  }

  public void setFileSize(long fileSize) {
    this.mFileSize = fileSize;
  }

  /**
   * Check if remote metadata is empty (no cloud backup exists).
   */
  public boolean isEmpty() {
    return mDriveFileId.isEmpty() || mUploadTimestamp == 0;
  }

  @Override
  public String toString() {
    return "RemoteMetadata{" +
        "driveFileId='" + mDriveFileId + '\'' +
        ", uploadTimestamp=" + mUploadTimestamp +
        ", md5='" + mMd5 + '\'' +
        ", bookmarkCount=" + mBookmarkCount +
        ", fileSize=" + mFileSize +
        '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    RemoteMetadata that = (RemoteMetadata) o;

    if (mUploadTimestamp != that.mUploadTimestamp) return false;
    if (mBookmarkCount != that.mBookmarkCount) return false;
    if (mFileSize != that.mFileSize) return false;
    if (!mDriveFileId.equals(that.mDriveFileId)) return false;
    return mMd5.equals(that.mMd5);
  }

  @Override
  public int hashCode() {
    int result = mDriveFileId.hashCode();
    result = 31 * result + (int) (mUploadTimestamp ^ (mUploadTimestamp >>> 32));
    result = 31 * result + mMd5.hashCode();
    result = 31 * result + mBookmarkCount;
    result = 31 * result + (int) (mFileSize ^ (mFileSize >>> 32));
    return result;
  }
}

