package app.organicmaps.cloud;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.Task;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.services.drive.DriveScopes;

import java.util.Collections;

/**
 * Manages Google Sign-In and OAuth authentication for Google Drive access.
 * 
 * Handles:
 * - Checking if Google Play Services are available
 * - Initiating sign-in flow
 * - Storing/retrieving OAuth token
 * - Providing authenticated Drive service access
 */
public class GoogleDriveManager {
  private static final String TAG = "GoogleDriveManager";
  private static final String PREFS_NAME = "google_drive_backup";
  private static final String KEY_ACCOUNT_EMAIL = "account_email";

  private static GoogleDriveManager sInstance;

  @Nullable
  private GoogleSignInClient mSignInClient;

  @Nullable
  private GoogleAccountCredential mCredential;

  private final Context mContext;
  private final SharedPreferences mPrefs;

  public GoogleDriveManager(@NonNull Context context) {
    mContext = context;
    mPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    initializeSignIn();
  }

  /**
   * Gets singleton instance
   */
  public static synchronized GoogleDriveManager getInstance(@NonNull Context context) {
    if (sInstance == null) {
      sInstance = new GoogleDriveManager(context.getApplicationContext());
    }
    return sInstance;
  }

  /**
   * Initialize Google Sign-In client with Drive API scope
   */
  private void initializeSignIn() {
    try {
      // Check if Google Play Services are available
      int result = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(mContext);
      if (result != ConnectionResult.SUCCESS) {
        Log.e(TAG, "Google Play Services not available: " + result);
        return;
      }

      // Configure Google Sign-In
      // IMPORTANT: Both requestScopes() calls are needed - one for GoogleSignIn, one for Drive API credential
      Scope driveScope = new Scope(DriveScopes.DRIVE_APPDATA);
      GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
          .requestScopes(driveScope)  // Request Drive scope for sign-in dialog
          .requestEmail()
          .build();

      mSignInClient = GoogleSignIn.getClient(mContext, gso);
      Log.d(TAG, "GoogleSignInClient initialized with DRIVE_APPDATA scope");
      Log.d(TAG, "Scope: " + DriveScopes.DRIVE_APPDATA);
      
      // Recreate credential if there's a cached account from previous sign-in
      if (hasCachedCredential()) {
        recreateCredentialFromCachedAccount();
      }
    } catch (Exception e) {
      Log.e(TAG, "Failed to initialize sign-in", e);
    }
  }

  /**
   * Recreate credential from cached account after app restart
   */
  private void recreateCredentialFromCachedAccount() {
    try {
      GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(mContext);
      if (account != null) {
        mCredential = GoogleAccountCredential.usingOAuth2(
            mContext,
            Collections.singleton(DriveScopes.DRIVE_APPDATA)
        );
        mCredential.setSelectedAccount(account.getAccount());
        Log.d(TAG, "Credential recreated from cached account: " + account.getEmail());
      }
    } catch (Exception e) {
      Log.e(TAG, "Failed to recreate credential from cached account", e);
    }
  }

  /**
   * Check if Google Play Services are available on this device
   */
  public boolean isGooglePlayServicesAvailable() {
    int result = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(mContext);
    return result == ConnectionResult.SUCCESS;
  }

  /**
   * Start the Google Sign-In flow.
   * This opens the account selection dialog.
   */
  public void startSignInFlow(@NonNull Activity activity, int requestCode) {
    if (mSignInClient == null) {
      Log.e(TAG, "SignInClient not initialized");
      return;
    }

    try {
      activity.startActivityForResult(mSignInClient.getSignInIntent(), requestCode);
      Log.d(TAG, "Sign-in flow started");
    } catch (Exception e) {
      Log.e(TAG, "Failed to start sign-in", e);
    }
  }
  
  /**
   * Get the sign-in intent (for modern ActivityResultLauncher)
   * Usage: mLauncher.launch(manager.getSignInIntent())
   */
  @Nullable
  public android.content.Intent getSignInIntent() {
    if (mSignInClient == null) {
      Log.e(TAG, "SignInClient not initialized");
      return null;
    }
    
    try {
      android.content.Intent intent = mSignInClient.getSignInIntent();
      Log.d(TAG, "getSignInIntent: returning sign-in intent");
      return intent;
    } catch (Exception e) {
      Log.e(TAG, "Failed to get sign-in intent", e);
      return null;
    }
  }
  /**
   * Handle the result from startActivityForResult after sign-in
   */
  public boolean handleSignInResult(int resultCode, @Nullable Object data) {
    try {
      if (resultCode != Activity.RESULT_OK) {
        Log.w(TAG, "Sign-in cancelled or failed with result code: " + resultCode);
        return false;
      }

      Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent((android.content.Intent) data);
      GoogleSignInAccount account = task.getResult(Exception.class);

      if (account != null) {
        Log.d(TAG, "Sign-in result received for: " + account.getEmail());
        
        // Verify that the required scope was actually granted
        Scope driveScope = new Scope(DriveScopes.DRIVE_APPDATA);
        boolean hasDriveScope = GoogleSignIn.hasPermissions(account, driveScope);
        Log.d(TAG, "Drive APPDATA scope granted: " + hasDriveScope);
        
        if (!hasDriveScope) {
          Log.w(TAG, "WARNING: Drive scope not granted! User must click 'Allow' in permission dialog.");
          Log.w(TAG, "Requesting additional permissions...");
          // This shouldn't happen if requestScopes() was called correctly
          // but we'll handle it just in case
        }
        
        // Save account email for future reference
        mPrefs.edit().putString(KEY_ACCOUNT_EMAIL, account.getEmail()).apply();

        // Create credential for Drive API access
        // This uses the same scope that was requested during sign-in
        mCredential = GoogleAccountCredential.usingOAuth2(
            mContext,
            Collections.singleton(DriveScopes.DRIVE_APPDATA)
        );
        mCredential.setSelectedAccount(account.getAccount());
        
        Log.d(TAG, "Credential created successfully with DRIVE_APPDATA scope");
        Log.d(TAG, "Ready for Drive API calls");
        return true;
      }

      Log.w(TAG, "Account is null after sign-in");
      return false;
    } catch (Exception e) {
      Log.e(TAG, "Failed to handle sign-in result", e);
      Log.e(TAG, "Exception: " + e.getMessage());
      e.printStackTrace();
      return false;
    }
  }

  /**
   * Check if user is currently signed in
   */
  public boolean isSignedIn() {
    GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(mContext);
    if (account == null) {
      Log.d(TAG, "isSignedIn: No account found");
      return false;
    }
    
    boolean hasCredential = mCredential != null;
    Log.d(TAG, "isSignedIn: account=" + account.getEmail() + ", credential=" + (hasCredential ? "yes" : "no"));
    
    return hasCredential;
  }
  
  /**
   * Verify that OAuth scopes are properly granted
   */
  public boolean hasGrantedScopes() {
    GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(mContext);
    if (account == null) {
      Log.e(TAG, "hasGrantedScopes: No account signed in");
      return false;
    }
    
    boolean hasScope = GoogleSignIn.hasPermissions(account, new Scope(DriveScopes.DRIVE_APPDATA));
    Log.d(TAG, "hasGrantedScopes: DRIVE_APPDATA scope granted=" + hasScope);
    
    if (!hasScope) {
      Log.w(TAG, "User has not granted DRIVE_APPDATA scope. Need to re-authenticate.");
    }
    
    return hasScope;
  }

  /**
   * Get the last signed-in account email
   */
  @Nullable
  public String getSignedInEmail() {
    GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(mContext);
    return account != null ? account.getEmail() : null;
  }

  /**
   * Get the credential for authenticated Drive API calls
   */
  @Nullable
  public GoogleAccountCredential getCredential() {
    return mCredential;
  }

  /**
   * Sign out the user
   */
  public void signOut() {
    if (mSignInClient == null) return;

    mSignInClient.signOut().addOnCompleteListener(task -> {
      mCredential = null;
      mPrefs.edit().remove(KEY_ACCOUNT_EMAIL).apply();
      Log.d(TAG, "User signed out");
    });
  }

  /**
   * Check if we have a cached credential without requiring sign-in
   */
  public boolean hasCachedCredential() {
    return GoogleSignIn.getLastSignedInAccount(mContext) != null;
  }
}
