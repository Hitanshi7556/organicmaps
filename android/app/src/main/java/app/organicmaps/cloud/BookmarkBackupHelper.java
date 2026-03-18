package app.organicmaps.cloud;
import android.util.JsonWriter;
import android.util.Log;
import androidx.annotation.NonNull;
import app.organicmaps.sdk.bookmarks.data.BookmarkCategory;
import app.organicmaps.sdk.bookmarks.data.BookmarkManager;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
/**
 * Helper class to export bookmarks and tracks to JSON format for backup.
 */
public class BookmarkBackupHelper {
  private static final String TAG = "BookmarkBackupHelper";
  public static boolean exportBookmarksToJson(@NonNull File outputFile) {
    try {
      outputFile.getParentFile().mkdirs();
      try (FileWriter fileWriter = new FileWriter(outputFile);
           JsonWriter jsonWriter = new JsonWriter(fileWriter)) {
        jsonWriter.setIndent("  ");
        jsonWriter.beginObject();
        jsonWriter.name("version").value(1);
        jsonWriter.name("timestamp").value(System.currentTimeMillis());
        jsonWriter.name("categories");
        jsonWriter.beginArray();
        List<BookmarkCategory> categories = BookmarkManager.INSTANCE.getCategories();
        for (BookmarkCategory category : categories) {
          exportCategory(jsonWriter, category);
        }
        jsonWriter.endArray();
        jsonWriter.endObject();
        Log.d(TAG, "Bookmarks exported to " + outputFile.getAbsolutePath());
        return true;
      }
    } catch (IOException e) {
      Log.e(TAG, "Failed to export bookmarks", e);
      return false;
    }
  }
  private static void exportCategory(@NonNull JsonWriter jsonWriter,
                                     @NonNull BookmarkCategory category) throws IOException {
    jsonWriter.beginObject();
    jsonWriter.name("id").value(category.getId());
    jsonWriter.name("name").value(category.getName());
    jsonWriter.name("annotation").value(category.getAnnotation());
    jsonWriter.name("description").value(category.getDescription());
    jsonWriter.name("isVisible").value(category.isVisible());
    jsonWriter.name("bookmarkCount").value(category.getBookmarksCount());
    jsonWriter.name("trackCount").value(category.getTracksCount());
    jsonWriter.endObject();
  }
  @NonNull
  public static String calculateMd5(@NonNull File file) {
    try {
      java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
      try (java.io.FileInputStream fis = new java.io.FileInputStream(file)) {
        byte[] buffer = new byte[8192];
        int count;
        while ((count = fis.read(buffer)) > 0) {
          md.update(buffer, 0, count);
        }
      }
      byte[] digest = md.digest();
      StringBuilder sb = new StringBuilder();
      for (byte b : digest) {
        sb.append(String.format("%02x", b));
      }
      return sb.toString();
    } catch (Exception e) {
      Log.e(TAG, "Failed to calculate MD5", e);
      return "";
    }
  }
}
