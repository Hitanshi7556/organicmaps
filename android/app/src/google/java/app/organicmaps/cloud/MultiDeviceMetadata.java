package app.organicmaps.cloud;

import android.content.Context;
import android.os.Build;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;
import app.organicmaps.BuildConfig;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class MultiDeviceMetadata {
  private static final String TAG = "MultiDeviceMetadata";
  private static final String PREF_DEVICE_ID = "device_id";
  private static final String DEVICE_ID_PREFIX = "om_";

  private final Context mContext;
  private static MultiDeviceMetadata sInstance;

  private MultiDeviceMetadata(Context context) {
    mContext = context.getApplicationContext();
  }

  public static synchronized MultiDeviceMetadata getInstance(Context context) {
    if (sInstance == null) {
      sInstance = new MultiDeviceMetadata(context);
    }
    return sInstance;
  }

  public String getDeviceId() {
    android.content.SharedPreferences prefs =
        PreferenceManager.getDefaultSharedPreferences(mContext);

    String deviceId = prefs.getString(PREF_DEVICE_ID, null);
    if (deviceId == null) {
      deviceId = DEVICE_ID_PREFIX + UUID.randomUUID().toString();
      prefs.edit().putString(PREF_DEVICE_ID, deviceId).apply();
      Log.d(TAG, "Generated new device ID: " + deviceId);
    }
    return deviceId;
  }

  public String getDeviceName() {
    String manufacturer = Build.MANUFACTURER;
    String model = Build.MODEL;
    String androidVersion = Build.VERSION.RELEASE;

    manufacturer = manufacturer.substring(0, 1).toUpperCase() + manufacturer.substring(1);

    return String.format("%s %s (Android %s)", manufacturer, model, androidVersion);
  }

  public Map<String, String> createBackupMetadata(int bookmarkCount) {
    Map<String, String> metadata = new HashMap<>();

    metadata.put("device_id", getDeviceId());
    metadata.put("device_name", getDeviceName());
    metadata.put("backup_timestamp", String.valueOf(System.currentTimeMillis()));
    metadata.put("bookmark_count", String.valueOf(bookmarkCount));
    metadata.put("app_version", BuildConfig.VERSION_NAME);

    Log.d(TAG, "Created backup metadata: " + metadata);
    return metadata;
  }

  public String formatMetadataForDisplay(@NonNull Map<String, String> metadata) {
    try {
      String deviceName = metadata.getOrDefault("device_name", "Unknown Device");
      String timestampStr = metadata.getOrDefault("backup_timestamp", "0");
      String bookmarkCount = metadata.getOrDefault("bookmark_count", "0");

      long timestamp = Long.parseLong(timestampStr);
      String formattedDate = formatTimestamp(timestamp);

      return String.format("%s on %s — %s bookmarks", deviceName, formattedDate, bookmarkCount);
    } catch (Exception e) {
      Log.e(TAG, "Error formatting metadata", e);
      return "Unknown backup";
    }
  }

  private String formatTimestamp(long timestamp) {
    if (timestamp == 0) return "Unknown date";

    SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy 'at' h:mm a", Locale.getDefault());
    return sdf.format(new Date(timestamp));
  }

  public String getDeviceNameFromMetadata(@NonNull Map<String, String> metadata) {
    return metadata.getOrDefault("device_name", "Unknown Device");
  }

  public String getFormattedDateFromMetadata(@NonNull Map<String, String> metadata) {
    try {
      String timestampStr = metadata.getOrDefault("backup_timestamp", "0");
      long timestamp = Long.parseLong(timestampStr);
      return formatTimestamp(timestamp);
    } catch (Exception e) {
      return "Unknown date";
    }
  }

  public int getBookmarkCountFromMetadata(@NonNull Map<String, String> metadata) {
    try {
      String count = metadata.getOrDefault("bookmark_count", "0");
      return Integer.parseInt(count);
    } catch (Exception e) {
      return 0;
    }
  }

  public long getBackupTimestampFromMetadata(@NonNull Map<String, String> metadata) {
    try {
      String timestampStr = metadata.getOrDefault("backup_timestamp", "0");
      return Long.parseLong(timestampStr);
    } catch (Exception e) {
      return 0;
    }
  }

  public boolean isFromCurrentDevice(@NonNull Map<String, String> metadata) {
    String backupDeviceId = metadata.getOrDefault("device_id", "");
    return backupDeviceId.equals(getDeviceId());
  }
}

