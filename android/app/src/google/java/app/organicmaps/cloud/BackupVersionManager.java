package app.organicmaps.cloud;

import android.util.Log;
import androidx.annotation.NonNull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BackupVersionManager {
  private static final String TAG = "BackupVersionManager";
  private static final int MAX_VERSIONS_TO_KEEP = 7;

  private final MultiDeviceMetadata mMultiDeviceMetadata;
  private static BackupVersionManager sInstance;

  public static class BackupVersion {
    public String revisionId;
    public String deviceName;
    public String formattedDate;
    public int bookmarkCount;
    public long timestamp;
    public String displayText;
    public boolean isFromCurrentDevice;

    @Override
    public String toString() {
      return displayText;
    }
  }

  private BackupVersionManager(MultiDeviceMetadata multiDeviceMetadata) {
    mMultiDeviceMetadata = multiDeviceMetadata;
  }

  public static synchronized BackupVersionManager getInstance(MultiDeviceMetadata multiDeviceMetadata) {
    if (sInstance == null) {
      sInstance = new BackupVersionManager(multiDeviceMetadata);
    }
    return sInstance;
  }

  public List<BackupVersion> getBackupVersions(@NonNull String backupFileId) {
    List<BackupVersion> versions = new ArrayList<>();
    Log.d(TAG, "getBackupVersions called for: " + backupFileId);
    return versions;
  }

  public BackupVersion getLatestBackupVersion(@NonNull String backupFileId) {
    List<BackupVersion> versions = getBackupVersions(backupFileId);
    if (versions.isEmpty()) {
      Log.w(TAG, "No backup versions available");
      return null;
    }
    return versions.get(0);
  }

  public boolean hasBackupVersions(@NonNull String backupFileId) {
    return getBackupVersions(backupFileId).size() > 0;
  }

  public List<String> getBackupVersionDisplayList(@NonNull String backupFileId) {
    List<String> displayList = new ArrayList<>();
    List<BackupVersion> versions = getBackupVersions(backupFileId);
    for (BackupVersion version : versions) {
      displayList.add(version.displayText);
    }
    return displayList;
  }

  public Map<String, List<BackupVersion>> getVersionsByDevice(@NonNull String backupFileId) {
    Map<String, List<BackupVersion>> byDevice = new HashMap<>();
    List<BackupVersion> versions = getBackupVersions(backupFileId);
    for (BackupVersion version : versions) {
      List<BackupVersion> deviceVersions = byDevice.getOrDefault(version.deviceName, new ArrayList<>());
      deviceVersions.add(version);
      byDevice.put(version.deviceName, deviceVersions);
    }
    return byDevice;
  }
}

