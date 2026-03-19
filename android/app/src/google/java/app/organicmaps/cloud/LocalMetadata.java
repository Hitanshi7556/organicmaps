package app.organicmaps.cloud;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Model representing local backup metadata.
 * Tracks the state of the last successful backup for sync comparison.
 */
public class LocalMetadata {
  private String mLocalFileId;
  private long mLocalTimestamp;
  private String mLocalMd5;
  private long mLocalFileSize;
  private int mLocalBookmarkCount;

  public LocalMetadata() {
    this.mLocalFileId = null;
    this.mLocalTimestamp = 0;
    this.mLocalMd5 = "";
    this.mLocalFileSize = 0;
    this.mLocalBookmarkCount = 0;
  }

  public LocalMetadata(String fileId, long timestamp, String md5, long fileSize, int bookmarkCount) {
    this.mLocalFileId = fileId;
    this.mLocalTimestamp = timestamp;
    this.mLocalMd5 = md5;
    this.mLocalFileSize = fileSize;
    this.mLocalBookmarkCount = bookmarkCount;
  }

  @Nullable
  public String getLocalFileId() {
    return mLocalFileId;
  }

  public void setLocalFileId(@Nullable String fileId) {
    this.mLocalFileId = fileId;
  }

  public long getLocalTimestamp() {
    return mLocalTimestamp;
  }

  public void setLocalTimestamp(long timestamp) {
    this.mLocalTimestamp = timestamp;
  }

  @NonNull
  public String getLocalMd5() {
    return mLocalMd5;
  }

  public void setLocalMd5(@NonNull String md5) {
    this.mLocalMd5 = md5;
  }

  public long getLocalFileSize() {
    return mLocalFileSize;
  }

  public void setLocalFileSize(long fileSize) {
    this.mLocalFileSize = fileSize;
  }

  public int getLocalBookmarkCount() {
    return mLocalBookmarkCount;
  }

  public void setLocalBookmarkCount(int bookmarkCount) {
    this.mLocalBookmarkCount = bookmarkCount;
  }

  public boolean isEmpty() {
    return mLocalFileId == null || mLocalTimestamp == 0;
  }

  @Override
  public String toString() {
    return "LocalMetadata{" +
        "fileId='" + mLocalFileId + '\'' +
        ", timestamp=" + mLocalTimestamp +
        ", md5='" + mLocalMd5 + '\'' +
        ", fileSize=" + mLocalFileSize +
        ", bookmarkCount=" + mLocalBookmarkCount +
        '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    LocalMetadata that = (LocalMetadata) o;

    if (mLocalTimestamp != that.mLocalTimestamp) return false;
    if (mLocalFileSize != that.mLocalFileSize) return false;
    if (mLocalBookmarkCount != that.mLocalBookmarkCount) return false;
    if (mLocalFileId != null ? !mLocalFileId.equals(that.mLocalFileId) : that.mLocalFileId != null)
      return false;
    return mLocalMd5 != null ? mLocalMd5.equals(that.mLocalMd5) : that.mLocalMd5 == null;
  }

  @Override
  public int hashCode() {
    int result = mLocalFileId != null ? mLocalFileId.hashCode() : 0;
    result = 31 * result + (int) (mLocalTimestamp ^ (mLocalTimestamp >>> 32));
    result = 31 * result + (mLocalMd5 != null ? mLocalMd5.hashCode() : 0);
    result = 31 * result + (int) (mLocalFileSize ^ (mLocalFileSize >>> 32));
    result = 31 * result + mLocalBookmarkCount;
    return result;
  }
}

