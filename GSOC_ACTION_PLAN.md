# GSoC Action Plan - Get Unstuck Now

## Current Problem
Build is failing due to permission naming issue. Sign-in UI isn't working properly.

## Quick Fixes (Next 2 Hours)

### Fix #1: Permission Error (1 hour)

**Error:**
```
APK contains: app.organicmaps.google.debug.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION
Expected:    app\.organicmaps(\.web)?(\.debug|\.beta)?\.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION
```

**Solution:**

```bash
# 1. Find the manifest file
find /Users/hitanshigoklani/Documents/organicmaps -name "AndroidManifest.xml" | head -5

# 2. Edit the main one
nano /Users/hitanshigoklani/Documents/organicmaps/android/app/src/main/AndroidManifest.xml
```

**Look for:**
```xml
<permission android:name="app.organicmaps.google.debug.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION"
```

**Change to:**
```xml
<permission android:name="app.organicmaps.debug.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION"
```

**Then rebuild:**
```bash
cd /Users/hitanshigoklani/Documents/organicmaps/android
./gradlew clean assembleGoogleDebug
```

**If still fails:**
- Check there's no other variant with `.google` in the name
- Search the entire build/ directory and remove any .google references

---

### Fix #2: Sign-In Flow Issue (1 hour)

**Problem:** You're signed in, but Settings page keeps redirecting.

**Likely Causes:**

1. **OAuth scope issue**
   - Check GoogleDriveManager is requesting correct scope
   - Should be: `DriveScopes.DRIVE_APPDATA` (app-private folder)
   - Not: `DriveScopes.DRIVE` (full access)

2. **Redirect loop**
   - Check BackupSettingsFragment doesn't call sign-in repeatedly
   - Verify `handleSignInResult()` is called in `onActivityResult()`

3. **Activity result not wired**
   - Verify BackupSettingsFragment has:
   ```java
   @Override
   public void onActivityResult(int requestCode, int resultCode, Intent data) {
     super.onActivityResult(requestCode, resultCode, data);
     mDriveManager.handleSignInResult(requestCode, data, ...);
   }
   ```

**Debug Steps:**
```bash
# 1. Check logcat output
adb logcat | grep "GoogleDrive\|SignIn\|OAuth" | cat

# Look for messages like:
# GoogleDriveManager: Sign-in flow started
# GoogleDriveManager: Credential created successfully

# 2. If you see errors, they'll show in logcat
# 3. Take screenshot of logcat output
# 4. Share the full error message
```

**Common Sign-In Issues:**
```
Issue: "User cancelled sign-in"
Fix:   This is normal - user just needs to complete auth

Issue: "Sign-in result is null"
Fix:   Check that onActivityResult is being called
       Add logging: Log.d(TAG, "onActivityResult called");

Issue: "Credential is null after sign-in"
Fix:   GoogleAccountCredential.usingOAuth2() might be failing
       Check permission scope is DRIVE_APPDATA

Issue: "Can't create Drive API service"
Fix:   Check GooglePlayServicesHelper.isGooglePlayServicesAvailable()
       Returns true before attempting auth
```

---

## Next Steps: Complete Settings UI (6-8 Hours)

### Step 1: Create prefs_backup.xml (30 min)

**File location:**
```
/android/app/src/main/res/xml/prefs_backup.xml
```

**File content:**
```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.preference.PreferenceScreen
  xmlns:android="http://schemas.android.com/apk/res/android"
  xmlns:app="http://schemas.android.com/apk/res-auto">

  <androidx.preference.PreferenceCategory
    android:title="Google Account"
    android:order="1">
    <Preference
      android:key="pref_backup_account"
      android:title="Account"
      android:summary="Not signed in"
      app:singleLineTitle="false"
      android:persistent="false"
      android:selectable="false"
      android:order="1" />
    <Preference
      android:key="pref_backup_sign_in"
      android:title="Sign in with Google"
      app:singleLineTitle="false"
      android:persistent="false"
      android:order="2" />
  </androidx.preference.PreferenceCategory>

  <androidx.preference.PreferenceCategory
    android:title="Bookmarks"
    android:order="2">
    <Preference
      android:key="pref_backup_now"
      android:title="Backup Now"
      android:summary="Upload bookmarks to Google Drive"
      app:singleLineTitle="false"
      android:persistent="false"
      android:order="1" />
    <Preference
      android:key="pref_restore_now"
      android:title="Restore from Drive"
      android:summary="Download bookmarks from Google Drive"
      app:singleLineTitle="false"
      android:persistent="false"
      android:order="2" />
    <Preference
      android:key="pref_backup_status"
      android:title="Last Backup"
      android:summary="Never"
      app:singleLineTitle="false"
      android:persistent="false"
      android:selectable="false"
      android:order="3" />
  </androidx.preference.PreferenceCategory>

</androidx.preference.PreferenceScreen>
```

---

### Step 2: Update prefs_main.xml (15 min)

**File location:**
```
/android/app/src/main/res/xml/prefs_main.xml
```

**Find this section:**
```xml
<androidx.preference.PreferenceCategory
  android:key="pref_privacy_category"
  android:title="Privacy"
  android:order="6">
```

**Add this BEFORE it (between order 5 and 6):**
```xml
<androidx.preference.PreferenceCategory
  android:key="pref_backup_category"
  android:title="Backup &amp; Restore"
  android:order="5">
  <Preference
    android:key="pref_backup_screen"
    android:title="Google Drive Backup"
    app:singleLineTitle="false"
    android:summary="Back up and restore your bookmarks"
    android:persistent="false"
    android:order="1" />
</androidx.preference.PreferenceCategory>
```

---

### Step 3: Create BackupSettingsFragment.java (2-3 hours)

**File location:**
```
/android/app/src/main/java/app/organicmaps/settings/BackupSettingsFragment.java
```

**File content:**
```java
package app.organicmaps.settings;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;

import app.organicmaps.R;
import app.organicmaps.cloud.GoogleDriveManager;

public class BackupSettingsFragment extends BaseXmlSettingsFragment
{
  private GoogleDriveManager mDriveManager;
  private static final int SIGN_IN_REQUEST_CODE = 100;

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
  }

  private void updateAccountStatus()
  {
    final Preference accountPref = findPreference("pref_backup_account");
    if (accountPref == null) return;

    if (mDriveManager.isSignedIn())
    {
      String email = mDriveManager.getSignedInEmail();
      accountPref.setSummary(email != null ? email : "Signed in");
    }
    else
    {
      accountPref.setSummary("Not signed in");
    }
  }

  private void initSignInPref()
  {
    final Preference pref = findPreference("pref_backup_sign_in");
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
        mDriveManager.startSignInFlow(requireActivity(), SIGN_IN_REQUEST_CODE);
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
    final Preference pref = findPreference("pref_backup_now");
    if (pref == null) return;

    pref.setOnPreferenceClickListener(preference -> {
      if (!mDriveManager.isSignedIn())
      {
        Toast.makeText(requireContext(), "Please sign in first", Toast.LENGTH_SHORT).show();
        return true;
      }
      Toast.makeText(requireContext(), "Backup started...", Toast.LENGTH_SHORT).show();
      // BackupService will be triggered here
      return true;
    });
  }

  private void initRestorePref()
  {
    final Preference pref = findPreference("pref_restore_now");
    if (pref == null) return;

    pref.setOnPreferenceClickListener(preference -> {
      if (!mDriveManager.isSignedIn())
      {
        Toast.makeText(requireContext(), "Please sign in first", Toast.LENGTH_SHORT).show();
        return true;
      }
      Toast.makeText(requireContext(), "Restore started...", Toast.LENGTH_SHORT).show();
      // RestoreService will be triggered here
      return true;
    });
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data)
  {
    super.onActivityResult(requestCode, resultCode, data);
    
    if (requestCode == SIGN_IN_REQUEST_CODE)
    {
      if (mDriveManager.handleSignInResult(resultCode, data))
      {
        updateAccountStatus();
        updateSignInPrefTitle(findPreference("pref_backup_sign_in"));
        Toast.makeText(requireContext(), "Signed in successfully!", Toast.LENGTH_SHORT).show();
      }
      else
      {
        Toast.makeText(requireContext(), "Sign in failed", Toast.LENGTH_SHORT).show();
      }
    }
  }
}
```

---

### Step 4: Wire in SettingsPrefsFragment.java (30 min)

**File location:**
```
/android/app/src/main/java/app/organicmaps/settings/SettingsPrefsFragment.java
```

**Find the `onPreferenceTreeClick()` method:**
```java
@Override
public boolean onPreferenceTreeClick(Preference preference)
{
  String key = preference.getKey();
  
  // ... existing if/else statements ...
```

**Add this before the final `return` statement:**
```java
  else if (key.equals("pref_backup_screen"))
  {
    getSettingsActivity().stackFragment(BackupSettingsFragment.class,
                                        getString(R.string.pref_backup_category), null);
    return true;
  }
```

---

## Build & Test (1 hour)

### Build

```bash
cd /Users/hitanshigoklani/Documents/organicmaps/android

# Clean build
./gradlew clean

# Build with debug
./gradlew assembleGoogleDebug

# If successful, you'll see:
# BUILD SUCCESSFUL in XXs
```

### If Build Fails

```bash
# Get more details
./gradlew assembleGoogleDebug --stacktrace 2>&1 | tail -50

# Check for compilation errors in BackupSettingsFragment
./gradlew compileGoogleDebugJava --stacktrace

# Check R.xml.prefs_backup is recognized
./gradlew compileGoogleDebugResources --stacktrace
```

### Manual Test

```bash
# Install on device/emulator
./gradlew installGoogleDebug

# Open app and navigate:
# 1. Settings
# 2. Scroll to "Backup & Restore"
# 3. Tap "Google Drive Backup"
# 4. Should see:
#    - Account: Not signed in
#    - Sign in with Google button
#    - Backup Now button (disabled unless signed in)
#    - Restore from Drive button (disabled unless signed in)
#    - Last Backup: Never
```

---

## Immediate Action Items (Do These Now)

### RIGHT NOW (Today)

- [ ] Fix AndroidManifest.xml permission name (1 hr)
- [ ] Create prefs_backup.xml (30 min)
- [ ] Update prefs_main.xml (15 min)
- [ ] Create BackupSettingsFragment.java (2-3 hrs)
- [ ] Wire in SettingsPrefsFragment.java (30 min)
- [ ] Run `./gradlew clean assembleGoogleDebug` (5 min)
- [ ] Manual test on device (30 min)

**Time Required: 5-6 hours**

### TOMORROW (Day 7-8)

- [ ] Build RestoreService.java (8-10 hrs)
- [ ] Implement download + restore logic
- [ ] Manual test restore flow
- [ ] Write 3 basic unit tests

### BY DAY 13

- [ ] Write remaining 17 unit tests
- [ ] All 20 tests passing
- [ ] Manual end-to-end test: Backup → Delete → Restore

### BY DAY 15

- [ ] Record demo video
- [ ] Write proposal document
- [ ] Submit GSoC application

---

## If You Get Stuck

### Build Issues

```bash
# Clean everything
rm -rf /Users/hitanshigoklani/Documents/organicmaps/android/build
rm -rf /Users/hitanshigoklani/Documents/organicmaps/android/.gradle

# Rebuild
./gradlew clean assembleGoogleDebug --refresh-dependencies
```

### XML Issues

```bash
# Check XML syntax
xmllint /Users/hitanshigoklani/Documents/organicmaps/android/app/src/main/res/xml/prefs_backup.xml

# If error, check:
# 1. All opening tags are closed
# 2. All & are &amp;
# 3. Quotes match (opening and closing)
```

### Runtime Issues

```bash
# Check logcat for errors
adb logcat | grep -i "error\|exception\|crash" | head -20

# Check specifically for backup errors
adb logcat | grep "BackupSettingsFragment\|GoogleDrive" | cat
```

---

## Success Criteria for Phase 1 (15 Days)

✅ By Day 15, you should have:

- [ ] All Java files compile without error
- [ ] OAuth sign-in flow works (tested on device)
- [ ] Backup upload to Google Drive works (file visible in drive.google.com)
- [ ] Settings UI fully functional
- [ ] 20 unit tests written and passing
- [ ] Manual end-to-end test succeeds (backup → restore)
- [ ] Demo video recorded (3-5 minutes)
- [ ] Proposal document written
- [ ] Ready to submit GSoC application

---

## Final Checklist

### Before You Start
- [ ] Close VS Code, use Android Studio
- [ ] Make sure Android SDK is installed
- [ ] Make sure emulator or device is connected

### As You Work
- [ ] Save files frequently (Cmd+S)
- [ ] Build after each major change
- [ ] Check logcat for errors
- [ ] Screenshot errors for debugging

### When Done
- [ ] All files compile
- [ ] No runtime errors
- [ ] Manual test passes
- [ ] Demo works
- [ ] Ready to ship ✅

---

## Support Resources

If you get stuck, check:

1. **Existing code**
   - SettingsPrefsFragment.java (example of how to navigate)
   - BaseXmlSettingsFragment.java (base class to extend)
   - AndroidManifest.xml (reference for permissions)

2. **Documentation**
   - Android Preferences: https://developer.android.com/guide/topics/ui/settings
   - Google Sign-In: https://developers.google.com/identity/sign-in/android
   - Google Drive API: https://developers.google.com/drive/android/studio

3. **Debug Tools**
   - Android Studio Debugger
   - adb logcat
   - Google Play Services Console
   - Chrome DevTools (for OAuth debugging)

---

**You're ready. Go build this. 🚀**

The architecture is solid. The hard part is done. Now it's just wiring UI and finishing touches.

Good luck! 💪

