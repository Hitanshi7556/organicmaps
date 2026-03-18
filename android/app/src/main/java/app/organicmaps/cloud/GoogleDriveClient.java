package app.organicmaps.cloud;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Client for interacting with Google Drive API.
 * 
 * Provides methods to:
 * - Upload files to Drive App Data folder
 * - Download files from Drive
 * - List files in App Data folder
 * - Delete files from Drive
 * - Get file metadata (size, modified time, MD5 hash)
 */
public class GoogleDriveClient {
  private static final String TAG = "GoogleDriveClient";
  private static final String APP_DATA_FOLDER_ID = "appDataFolder";
  private static final int BUFFER_SIZE = 8192;

  @Nullable
  private Drive mDriveService;

  private final GoogleAccountCredential mCredential;

  public GoogleDriveClient(@NonNull GoogleAccountCredential credential) {
    mCredential = credential;
    initializeDriveService();
  }

  /**
   * Initialize the Drive service with the provided credential
   */
  private void initializeDriveService() {
    try {
      HttpTransport transport = new NetHttpTransport();
      JsonFactory jsonFactory = GsonFactory.getDefaultInstance();

      mDriveService = new Drive.Builder(transport, jsonFactory, mCredential)
          .setApplicationName("Organic Maps")
          .build();

      Log.d(TAG, "Drive service initialized");
    } catch (Exception e) {
      Log.e(TAG, "Failed to initialize Drive service", e);
    }
  }

  /**
   * Upload a file to the App Data folder on Google Drive
   * 
   * @param localFilePath Path to local file to upload
   * @param fileName Name for the file on Drive
   * @return Drive file ID if successful, null otherwise
   */
  @Nullable
  public String uploadFile(@NonNull String localFilePath, @NonNull String fileName) {
    if (mDriveService == null) {
      Log.e(TAG, "❌ Drive service not initialized");
      return null;
    }

    try {
      // ============================================================
      // DIAGNOSTIC STEP 1: Verify credential status
      // ============================================================
      Log.d(TAG, "═══════════════════════════════════════════════════════");
      Log.d(TAG, "📤 STARTING UPLOAD: " + fileName);
      Log.d(TAG, "═══════════════════════════════════════════════════════");
      
      if (mCredential == null) {
        Log.e(TAG, "❌ ERROR: Credential is NULL");
        return null;
      }
      
      Log.d(TAG, "✅ Credential object exists");
      Log.d(TAG, "   Credential class: " + mCredential.getClass().getName());
      
      // Check if credential has an account selected
      try {
        android.accounts.Account selectedAccount = mCredential.getSelectedAccount();
        if (selectedAccount == null) {
          Log.e(TAG, "❌ ERROR: No account selected in credential");
          return null;
        }
        Log.d(TAG, "✅ Selected account: " + selectedAccount.name);
      } catch (Exception e) {
        Log.e(TAG, "⚠️  Could not verify selected account: " + e.getMessage());
      }
      
      // ============================================================
      // DIAGNOSTIC STEP 2: Verify local file
      // ============================================================
      java.io.File filePath = new java.io.File(localFilePath);
      if (!filePath.exists()) {
        Log.e(TAG, "❌ ERROR: Local file does not exist");
        Log.e(TAG, "   Path: " + localFilePath);
        return null;
      }
      
      long fileSize = filePath.length();
      Log.d(TAG, "✅ Local file exists");
      Log.d(TAG, "   Path: " + localFilePath);
      Log.d(TAG, "   Size: " + fileSize + " bytes (" + (fileSize / 1024.0) + " KB)");
      Log.d(TAG, "   Name: " + filePath.getName());
      Log.d(TAG, "   Readable: " + filePath.canRead());
      
      // ============================================================
      // DIAGNOSTIC STEP 3: Create metadata
      // ============================================================
      Log.d(TAG, "📝 Creating file metadata...");
      File fileMetadata = new File();
      fileMetadata.setName(fileName);
      fileMetadata.setParents(Arrays.asList(APP_DATA_FOLDER_ID));
      Log.d(TAG, "✅ Metadata created");
      Log.d(TAG, "   File name: " + fileName);
      Log.d(TAG, "   Parent folder: " + APP_DATA_FOLDER_ID + " (app-private)");
      
      // ============================================================
      // DIAGNOSTIC STEP 4: Create content
      // ============================================================
      Log.d(TAG, "📦 Creating file content...");
      FileContent mediaContent = new FileContent("application/zip", filePath);
      Log.d(TAG, "✅ Content created");
      Log.d(TAG, "   MIME type: application/zip");
      Log.d(TAG, "   Content size: " + mediaContent.getLength() + " bytes");
      
      // ============================================================
      // DIAGNOSTIC STEP 5: Build and execute request
      // ============================================================
      Log.d(TAG, "🔗 Building Drive API request...");
      Log.d(TAG, "   Endpoint: files().create()");
      Log.d(TAG, "   Fields: id, webContentLink, size, modifiedTime, md5Checksum");
      
      Log.d(TAG, "🚀 Executing upload request...");
      Log.d(TAG, "   This may take a few seconds depending on file size");
      
      long uploadStartTime = System.currentTimeMillis();
      
      File uploadedFile = mDriveService.files().create(fileMetadata, mediaContent)
          .setFields("id, webContentLink, size, modifiedTime, md5Checksum")
          .execute();
      
      long uploadDuration = System.currentTimeMillis() - uploadStartTime;
      
      // ============================================================
      // DIAGNOSTIC STEP 6: Verify upload result
      // ============================================================
      if (uploadedFile == null) {
        Log.e(TAG, "❌ ERROR: Upload returned NULL file object");
        return null;
      }
      
      String fileId = uploadedFile.getId();
      Log.d(TAG, "═══════════════════════════════════════════════════════");
      Log.d(TAG, "✅ UPLOAD SUCCESSFUL!");
      Log.d(TAG, "═══════════════════════════════════════════════════════");
      Log.d(TAG, "📊 Upload Result:");
      Log.d(TAG, "   File ID: " + fileId);
      Log.d(TAG, "   File Name: " + uploadedFile.getName());
      Log.d(TAG, "   File Size: " + uploadedFile.getSize() + " bytes");
      Log.d(TAG, "   Web Link: " + uploadedFile.getWebContentLink());
      Log.d(TAG, "   MD5: " + uploadedFile.getMd5Checksum());
      Log.d(TAG, "   Modified: " + uploadedFile.getModifiedTime());
      Log.d(TAG, "   Upload Time: " + uploadDuration + " ms");
      Log.d(TAG, "═══════════════════════════════════════════════════════");
      
      return fileId;
      
    } catch (com.google.api.client.googleapis.json.GoogleJsonResponseException e) {
      // This exception is thrown for HTTP errors from Drive API
      int code = e.getStatusCode();
      String message = e.getDetails() != null ? e.getDetails().getMessage() : e.getMessage();
      String reason = e.getDetails() != null && e.getDetails().getErrors() != null 
          && e.getDetails().getErrors().size() > 0 
          ? e.getDetails().getErrors().get(0).getReason() 
          : "unknown";
      
      Log.e(TAG, "═══════════════════════════════════════════════════════");
      Log.e(TAG, "❌ GOOGLE API ERROR - Upload failed!");
      Log.e(TAG, "═══════════════════════════════════════════════════════");
      Log.e(TAG, "🔴 HTTP Status Code: " + code);
      Log.e(TAG, "🔴 Error Reason: " + reason);
      Log.e(TAG, "🔴 Error Message: " + message);
      Log.e(TAG, "═══════════════════════════════════════════════════════");
      
      // Specific error handling
      if (code == 403) {
        Log.e(TAG, "💡 DIAGNOSIS: PERMISSION DENIED");
        Log.e(TAG, "   Possible causes:");
        Log.e(TAG, "   1. User didn't click 'Allow' during sign-in");
        Log.e(TAG, "   2. Drive API is not enabled in Google Cloud Console");
        Log.e(TAG, "   3. OAuth token doesn't have DRIVE_APPDATA scope");
        Log.e(TAG, "   ");
        Log.e(TAG, "   ✅ FIX: Sign out and sign in again");
        Log.e(TAG, "         Make sure to click 'Allow' in permission dialog");
      } else if (code == 401) {
        Log.e(TAG, "💡 DIAGNOSIS: UNAUTHORIZED / TOKEN EXPIRED");
        Log.e(TAG, "   OAuth token is invalid or expired");
        Log.e(TAG, "   ");
        Log.e(TAG, "   ✅ FIX: Sign out and sign in again");
      } else if (code == 400) {
        Log.e(TAG, "💡 DIAGNOSIS: BAD REQUEST");
        Log.e(TAG, "   Invalid file metadata or request format");
      } else if (code == 429) {
        Log.e(TAG, "💡 DIAGNOSIS: RATE LIMITED");
        Log.e(TAG, "   Too many requests. Wait before retrying.");
      } else if (code == 500) {
        Log.e(TAG, "💡 DIAGNOSIS: GOOGLE SERVER ERROR");
        Log.e(TAG, "   Try again in a few moments");
      }
      
      e.printStackTrace();
      return null;
      
    } catch (java.net.SocketTimeoutException e) {
      Log.e(TAG, "═══════════════════════════════════════════════════════");
      Log.e(TAG, "❌ NETWORK ERROR - Connection Timeout");
      Log.e(TAG, "═══════════════════════════════════════════════════════");
      Log.e(TAG, "⚠️  Socket timeout: " + e.getMessage());
      Log.e(TAG, "   Network is too slow or Drive API unreachable");
      Log.e(TAG, "   ");
      Log.e(TAG, "   ✅ FIX: Check internet connection");
      Log.e(TAG, "         Try again with stronger connection");
      e.printStackTrace();
      return null;
      
    } catch (IOException e) {
      Log.e(TAG, "═══════════════════════════════════════════════════════");
      Log.e(TAG, "❌ IO ERROR - Upload failed!");
      Log.e(TAG, "═══════════════════════════════════════════════════════");
      Log.e(TAG, "💥 Exception Class: " + e.getClass().getName());
      Log.e(TAG, "💥 Error Message: " + e.getMessage());
      
      if (e.getCause() != null) {
        Log.e(TAG, "💥 Caused by: " + e.getCause().getClass().getName());
        Log.e(TAG, "💥 Cause message: " + e.getCause().getMessage());
      }
      
      // Print stack trace for debugging
      Log.e(TAG, "═══════════════════════════════════════════════════════");
      e.printStackTrace();
      return null;
    }
  }

  /**
   * Download a file from Google Drive to local storage
   * 
   * @param driveFileId ID of file on Drive
   * @param localFilePath Path where to save the file locally
   * @return true if successful, false otherwise
   */
  public boolean downloadFile(@NonNull String driveFileId, @NonNull String localFilePath) {
    if (mDriveService == null) {
      Log.e(TAG, "Drive service not initialized");
      return false;
    }

    try {
      // Create output file
      java.io.File outputFile = new java.io.File(localFilePath);
      outputFile.getParentFile().mkdirs(); // Ensure directory exists

      // Download file
      Drive.Files.Get getRequest = mDriveService.files().get(driveFileId);
      getRequest.getMediaHttpDownloader().setProgressListener(
          progress -> Log.d(TAG, "Download progress: " + (progress.getProgress() * 100) + "%")
      );

      try (FileOutputStream output = new FileOutputStream(outputFile)) {
        getRequest.executeMediaAndDownloadTo(output);
      }

      Log.d(TAG, "File downloaded: " + driveFileId + " to " + localFilePath);
      return true;
    } catch (IOException e) {
      Log.e(TAG, "Download failed for " + driveFileId, e);
      return false;
    }
  }

  /**
   * List all files in the App Data folder
   * 
   * @return List of FileMetadata objects, or empty list if error
   */
  @NonNull
  public List<FileMetadata> listFiles() {
    if (mDriveService == null) {
      Log.e(TAG, "Drive service not initialized");
      return new ArrayList<>();
    }

    try {
      FileList result = mDriveService.files().list()
          .setSpaces(APP_DATA_FOLDER_ID)
          .setFields("files(id, name, modifiedTime, md5Checksum, size)")
          .setPageSize(100)
          .execute();

      List<FileMetadata> files = new ArrayList<>();
      java.util.List<?> fileList = result.getFiles();
      if (fileList != null) {
        for (Object obj : fileList) {
          File file = (File) obj;
          files.add(new FileMetadata(
              file.getId(),
              file.getName(),
              file.getModifiedTime() != null ? file.getModifiedTime().getValue() : 0,
              file.getMd5Checksum(),
              file.getSize() != null ? file.getSize() : 0
          ));
        }
      }

      Log.d(TAG, "Listed " + files.size() + " files from Drive");
      return files;
    } catch (IOException e) {
      Log.e(TAG, "Failed to list files", e);
      return new ArrayList<>();
    }
  }

  /**
   * Delete a file from Google Drive
   * 
   * @param driveFileId ID of file to delete
   * @return true if successful, false otherwise
   */
  public boolean deleteFile(@NonNull String driveFileId) {
    if (mDriveService == null) {
      Log.e(TAG, "Drive service not initialized");
      return false;
    }

    try {
      mDriveService.files().delete(driveFileId).execute();
      Log.d(TAG, "File deleted: " + driveFileId);
      return true;
    } catch (IOException e) {
      Log.e(TAG, "Failed to delete file " + driveFileId, e);
      return false;
    }
  }

  /**
   * Get metadata for a specific file
   * 
   * @param driveFileId ID of file on Drive
   * @return FileMetadata if found, null otherwise
   */
  @Nullable
  public FileMetadata getFileMetadata(@NonNull String driveFileId) {
    if (mDriveService == null) {
      Log.e(TAG, "Drive service not initialized");
      return null;
    }

    try {
      File file = mDriveService.files().get(driveFileId)
          .setFields("id, name, modifiedTime, md5Checksum, size")
          .execute();

      return new FileMetadata(
          file.getId(),
          file.getName(),
          file.getModifiedTime() != null ? file.getModifiedTime().getValue() : 0,
          file.getMd5Checksum(),
          file.getSize() != null ? file.getSize() : 0
      );
    } catch (IOException e) {
      Log.e(TAG, "Failed to get file metadata", e);
      return null;
    }
  }

  /**
   * Data class to hold file metadata
   */
  public static class FileMetadata {
    public final String id;
    public final String name;
    public final long modifiedTime;
    public final String md5Checksum;
    public final long size;

    public FileMetadata(String id, String name, long modifiedTime, String md5Checksum, long size) {
      this.id = id;
      this.name = name;
      this.modifiedTime = modifiedTime;
      this.md5Checksum = md5Checksum;
      this.size = size;
    }

    @Override
    public String toString() {
      return "FileMetadata{" +
          "id='" + id + '\'' +
          ", name='" + name + '\'' +
          ", modifiedTime=" + modifiedTime +
          ", md5Checksum='" + md5Checksum + '\'' +
          ", size=" + size +
          '}';
    }
  }
}
