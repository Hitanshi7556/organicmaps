package app.organicmaps.settings;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import app.organicmaps.R;
import app.organicmaps.cloud.BackupService;
import app.organicmaps.cloud.GoogleDriveManager;
import app.organicmaps.cloud.RestoreService;

public class BackupSettingsFragment extends BaseXmlSettingsFragment
{
  private static final String TAG = "BackupSettingsFragment";
  private static final int SIGN_IN_REQUEST_CODE = 1001;
  private GoogleDriveManager mDriveManager;
  @Override
  protected int getXmlResources()
  {
    return R.xml.prefs_backup;
  }
  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState)
  {
    super.onViewCreated(view, savedInstanceState);
    mDriveManager = GoogleDriveManager.getInstance(requireActivity());
    updateAccountStatus();
    initSignInPref();
    initBackupPref();
    initRestorePref();
    updateLastBackupTime();
  }
  private void updateAccountStatus()
  {
    final Preference accountPref = getPreference("pref_backup_account");
    if (accountPref == null) return;
    
    boolean isSignedIn = mDriveManager.isSignedIn();
    Log.d(TAG, "updateAccountStatus: isSignedIn=" + isSignedIn);
    
    if (isSignedIn)
    {
      String email = mDriveManager.getSignedInEmail();
      Log.d(TAG, "Signed in as: " + email);
      accountPref.setSummary(email != null ? email : "Signed in");
    }
    else
    {
      Log.d(TAG, "Not signed in");
      accountPref.setSummary("Not signed in");
    }
  }
  private void initSignInPref()
  {
    final Preference pref = getPreference("pref_backup_sign_in");
    if (pref == null) return;
    updateSignInPrefTitle(pref);
    pref.setOnPreferenceClickListener(preference -> {
      if (mDriveManager.isSignedIn())
      {
        mDriveManager.signOut();
        updateSignInPrefTitle(preference);
        updateAccountStatus();
        Toast.makeText(requireContext(), "Signed out", Toast.LENGTH_SHORT).show();
      }
      else
      {
        Log.d(TAG, "Starting sign-in flow...");
        startActivityForResult(
            mDriveManager.getSignInIntent(),
            SIGN_IN_REQUEST_CODE
        );
      }
      return true;
    });
  }
  private void updateSignInPrefTitle(@NonNull Preference pref)
  {
    pref.setTitle(mDriveManager.isSignedIn() ? "Sign out" : "Sign in with Google");
  }
  private void initBackupPref()
  {
    final Preference pref = getPreference("pref_backup_now");
    if (pref == null) return;
    pref.setOnPreferenceClickListener(preference -> {
      if (!mDriveManager.isSignedIn())
      {
        Toast.makeText(requireContext(), "Please sign in first", Toast.LENGTH_SHORT).show();
        return true;
      }
      Toast.makeText(requireContext(), "Backup started...", Toast.LENGTH_SHORT).show();
      BackupService.startBackup(requireActivity(), new BackupService.BackupCallback() {
        @Override
        public void onBackupSuccess(String driveFileId, long fileSize)
        {
          if (isAdded()) {
            Toast.makeText(requireContext(), "Backup successful!", Toast.LENGTH_SHORT).show();
            updateLastBackupTime();
          }
        }
        @Override
        public void onBackupFailure(String errorMessage)
        {
          if (isAdded()) {
            Toast.makeText(requireContext(), "Backup failed: " + errorMessage, Toast.LENGTH_LONG).show();
          }
        }
      });
      return true;
    });
  }
  private void initRestorePref()
  {
    final Preference pref = getPreference("pref_restore_now");
    if (pref == null) return;
    pref.setOnPreferenceClickListener(preference -> {
      if (!mDriveManager.isSignedIn())
      {
        Toast.makeText(requireContext(), "Please sign in first", Toast.LENGTH_SHORT).show();
        return true;
      }
      Toast.makeText(requireContext(), "Restore started...", Toast.LENGTH_SHORT).show();
      
      // Get the last backup file ID from metadata
      android.content.SharedPreferences prefs = requireActivity().getSharedPreferences("backup_metadata", android.content.Context.MODE_PRIVATE);
      String fileId = prefs.getString("last_backup_file_id", null);
      
      if (fileId == null || fileId.isEmpty())
      {
        Toast.makeText(requireContext(), "No backup found to restore", Toast.LENGTH_SHORT).show();
        return true;
      }
      
      RestoreService.startRestore(requireActivity(), fileId, new RestoreService.RestoreCallback() {
        @Override
        public void onRestoreSuccess(int bookmarkCount)
        {
          Toast.makeText(requireContext(), "Restored " + bookmarkCount + " bookmarks!", Toast.LENGTH_SHORT).show();
          updateLastBackupTime();
        }
        @Override
        public void onRestoreFailure(String errorMessage)
        {
          Toast.makeText(requireContext(), "Restore failed: " + errorMessage, Toast.LENGTH_LONG).show();
        }
      });
      return true;
    });
  }
  private void updateLastBackupTime()
  {
    try {
      // Safety check: ensure fragment is still attached
      if (!isAdded()) {
        Log.d(TAG, "updateLastBackupTime: Fragment not attached, skipping");
        return;
      }
      
      final Preference statusPref = getPreference("pref_backup_status");
      if (statusPref == null) return;
      
      android.content.SharedPreferences prefs = requireActivity().getSharedPreferences("backup_metadata", android.content.Context.MODE_PRIVATE);
      long timestamp = prefs.getLong("last_backup_timestamp", 0);
      
      if (timestamp > 0)
      {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MMM dd, yyyy HH:mm");
        String timeStr = sdf.format(new java.util.Date(timestamp));
        statusPref.setSummary("Last backup: " + timeStr);
      }
      else
      {
        statusPref.setSummary("Never");
      }
    } catch (Exception e) {
      Log.e(TAG, "Error updating last backup time", e);
    }
  }
  @Override
  public void onResume()
  {
    super.onResume();
    updateAccountStatus();
    updateLastBackupTime();
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data)
  {
    super.onActivityResult(requestCode, resultCode, data);
    
    Log.d(TAG, "onActivityResult: requestCode=" + requestCode + ", resultCode=" + resultCode);
    
    if (requestCode == SIGN_IN_REQUEST_CODE) {
      boolean success = mDriveManager.handleSignInResult(resultCode, data);
      Log.d(TAG, "handleSignInResult returned: " + success);
      
      if (success) {
        Log.d(TAG, "✅ Sign-in successful, updating UI");
        updateAccountStatus();
        updateSignInPrefTitle(getPreference("pref_backup_sign_in"));
        Toast.makeText(requireContext(), "Signed in successfully!", Toast.LENGTH_SHORT).show();
      } else {
        Log.e(TAG, "❌ Sign-in failed");
        Toast.makeText(requireContext(), "Sign-in failed", Toast.LENGTH_SHORT).show();
      }
    }
  }
}
