package app.organicmaps.cloud;

import android.util.JsonWriter;
import android.util.Log;
import androidx.annotation.NonNull;
import app.organicmaps.sdk.bookmarks.data.BookmarkCategory;
import app.organicmaps.sdk.bookmarks.data.BookmarkInfo;
import app.organicmaps.sdk.bookmarks.data.BookmarkManager;
import app.organicmaps.sdk.bookmarks.data.ElevationInfo;
import app.organicmaps.sdk.bookmarks.data.Track;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

/**
 * Helper class to export bookmarks and tracks to GeoJSON format for backup.
 * 
 * GeoJSON format is fully supported by BookmarkManager's native import mechanism.
 * Exports bookmarks with their exact coordinates, names, descriptions via the public API.
 */
public class BookmarkBackupHelper {
  private static final String TAG = "BookmarkBackupHelper";
  
  public static boolean exportBookmarksToJson(@NonNull File outputFile) {
    try {
      outputFile.getParentFile().mkdirs();
      try (FileWriter fileWriter = new FileWriter(outputFile);
           JsonWriter jsonWriter = new JsonWriter(fileWriter)) {
        jsonWriter.setIndent("  ");
        
        // Export as GeoJSON FeatureCollection (required format for import)
        jsonWriter.beginObject();
        jsonWriter.name("type").value("FeatureCollection");
        jsonWriter.name("features");
        jsonWriter.beginArray();
        
        List<BookmarkCategory> categories = BookmarkManager.INSTANCE.getCategories();
        int totalBookmarks = 0;
        int totalTracks = 0;
        
        for (BookmarkCategory category : categories) {
          totalBookmarks += exportBookmarksFromCategory(jsonWriter, category);
          totalTracks += exportTracksFromCategory(jsonWriter, category);
        }
        
        jsonWriter.endArray();
        jsonWriter.endObject();
        
        Log.d(TAG, "Bookmarks exported to " + outputFile.getAbsolutePath() + 
                   " (" + totalBookmarks + " bookmarks, " + totalTracks + " tracks)");
        return true;
      }
    } catch (IOException e) {
      Log.e(TAG, "Failed to export bookmarks", e);
      return false;
    }
  }
  
  /**
   * Export all bookmarks from a category as GeoJSON features.
   * Returns the number of bookmarks exported.
   */
  private static int exportBookmarksFromCategory(@NonNull JsonWriter jsonWriter,
                                                 @NonNull BookmarkCategory category) throws IOException {
    int bookmarkCount = category.getBookmarksCount();
    int exportCount = 0;
    
    // Iterate through all bookmarks in category by position
    for (int i = 0; i < bookmarkCount; i++) {
      long bookmarkId = category.getBookmarkIdByPosition(i);
      BookmarkInfo info = BookmarkManager.INSTANCE.getBookmarkInfo(bookmarkId);
      if (info != null) {
        exportBookmark(jsonWriter, info, category);
        exportCount++;
      }
    }
    
    return exportCount;
  }
  
  /**
   * Export a single bookmark as a GeoJSON Feature with Point geometry.
   */
  private static void exportBookmark(@NonNull JsonWriter jsonWriter,
                                     @NonNull BookmarkInfo bookmarkInfo,
                                     @NonNull BookmarkCategory category) throws IOException {
    jsonWriter.beginObject();
    jsonWriter.name("type").value("Feature");
    
    // Geometry: Point with [longitude, latitude] - GeoJSON standard order
    jsonWriter.name("geometry");
    jsonWriter.beginObject();
    jsonWriter.name("type").value("Point");
    jsonWriter.name("coordinates");
    jsonWriter.beginArray();
    jsonWriter.value(bookmarkInfo.getLon());
    jsonWriter.value(bookmarkInfo.getLat());
    jsonWriter.endArray();
    jsonWriter.endObject();
    
    // Properties: bookmark metadata
    jsonWriter.name("properties");
    jsonWriter.beginObject();
    jsonWriter.name("name").value(bookmarkInfo.getName());
    
    String description = bookmarkInfo.getDescription();
    if (description != null && !description.isEmpty()) {
      jsonWriter.name("description").value(description);
    }
    
    jsonWriter.name("category").value(category.getName());
    jsonWriter.name("category_id").value(category.getId());
    // Default marker color - red
    jsonWriter.name("marker-color").value("#FF0000");
    
    jsonWriter.endObject();
    
    jsonWriter.endObject();
  }
  
  /**
   * Export all tracks from a category as GeoJSON features.
   * Returns the number of tracks exported.
   */
  private static int exportTracksFromCategory(@NonNull JsonWriter jsonWriter,
                                              @NonNull BookmarkCategory category) throws IOException {
    int trackCount = category.getTracksCount();
    int exportCount = 0;
    
    // Iterate through all tracks in category by position
    for (int i = 0; i < trackCount; i++) {
      long trackId = category.getTrackIdByPosition(i);
      Track track = BookmarkManager.INSTANCE.getTrack(trackId);
      if (track != null) {
        exportTrack(jsonWriter, track, category);
        exportCount++;
      }
    }
    
    return exportCount;
  }
  
  /**
   * Export a single track as a GeoJSON Feature with LineString geometry.
   */
  private static void exportTrack(@NonNull JsonWriter jsonWriter,
                                  @NonNull Track track,
                                  @NonNull BookmarkCategory category) throws IOException {
    jsonWriter.beginObject();
    jsonWriter.name("type").value("Feature");
    
    // Geometry: LineString with track points from ElevationInfo
    jsonWriter.name("geometry");
    jsonWriter.beginObject();
    jsonWriter.name("type").value("LineString");
    jsonWriter.name("coordinates");
    jsonWriter.beginArray();
    
    // Export track points as [longitude, latitude] pairs from ElevationInfo
    ElevationInfo elevationInfo = track.getElevationInfo();
    if (elevationInfo != null) {
      List<ElevationInfo.Point> points = elevationInfo.getPoints();
      for (ElevationInfo.Point point : points) {
        jsonWriter.beginArray();
        jsonWriter.value(point.getLongitude());
        jsonWriter.value(point.getLatitude());
        jsonWriter.endArray();
      }
    }
    
    jsonWriter.endArray();
    jsonWriter.endObject();
    
    // Properties: track metadata
    jsonWriter.name("properties");
    jsonWriter.beginObject();
    jsonWriter.name("name").value(track.getName());
    
    String description = track.getDescription();
    if (description != null && !description.isEmpty()) {
      jsonWriter.name("description").value(description);
    }
    
    jsonWriter.name("category").value(category.getName());
    jsonWriter.name("category_id").value(category.getId());
    jsonWriter.name("type").value("LineString");
    
    jsonWriter.endObject();
    
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
