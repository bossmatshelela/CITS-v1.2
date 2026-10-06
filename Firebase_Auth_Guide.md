# Implementing Firebase Authentication with Google Sign-In (Credential Manager)

This guide provides a comprehensive, production-ready, step-by-step tutorial and complete Kotlin implementation for integrating **Firebase Authentication** with **Google Sign-In** using Android's modern **Credential Manager API** (`androidx.credentials`).

## Project Metadata
- **Package Name:** `com.matshelela.childimmunisationtrackingsystemappv1`
- **Firebase Project ID:** `cits-4d830`
- **Minimum SDK Required:** API 24+ (Credential Manager has backwards compatibility back to API 19 via Play Services)
- **Primary Technology:** Kotlin & Jetpack Compose

---

## 1. Build Gradle Dependencies

To use Firebase Authentication with the modern Credential Manager, you must include the Firebase Bill of Materials (BoM), Firebase Auth, Google Play Services Auth, and Android Credential Manager libraries.

### Module-level Gradle File (`/app/build.gradle.kts`)

Add the following dependencies under your `dependencies` block:

```kotlin
dependencies {
    // 1. Firebase Bill of Materials (BoM)
    implementation(platform("com.google.firebase:firebase-bom:34.15.0"))

    // 2. Firebase Authentication Library (Version managed by BoM)
    implementation("com.google.firebase:firebase-auth")

    // 3. Android Credential Manager Library (Standardized API for Google Sign-In)
    implementation("androidx.credentials:credentials:1.3.0")

    // 4. Optional but highly recommended: Credential Play Services Helper (for devices running below Android 14)
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")

    // 5. Google Play Services Authentication Library
    implementation("com.google.android.gms:play-services-auth:21.2.0")
}
```

---

## 2. Configuration in Firebase Console & Google Cloud

Before running the code, you must link the Google Client ID:
1. Go to the [Google Cloud Console Credentials Page](https://console.cloud.google.com/apis/credentials).
2. Select your Firebase project: `cits-4d830`.
3. Locate the **OAuth 2.0 Client IDs** table.
4. Copy the **Web client ID** (this will be used as the `SERVER_CLIENT_ID` in your Android code).
5. Ensure your Android Debug SHA-1 (`F1:7C:B0:C1:B6:36:59:D6:FD:A5:0A:70:6F:11:02:A9:D5:3B:25:86`) is registered in your **Firebase Project Settings** for this Android app.

---

## 3. Core Kotlin Implementation

Below is a production-grade utility class `FirebaseAuthManager` that handles the complete lifecycle: initialization, creating credential requests, processing the response, authenticating with Firebase, and signing out.

### Create file: `/app/src/main/java/com/matshelela/childimmunisationtrackingsystemappv1/data/FirebaseAuthManager.kt`

```kotlin
package com.matshelela.childimmunisationtrackingsystemappv1.data

import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await

class FirebaseAuthManager(private val context: Context) {

    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private val credentialManager: CredentialManager = CredentialManager.create(context)

    // Replace with your actual OAuth Web Client ID copied from Google Cloud Console
    private val webClientId = "598530263591-YOUR_ACTUAL_OAUTH_CLIENT_ID.apps.googleusercontent.com"

    companion object {
        private const val TAG = "FirebaseAuthManager"
    }

    /**
     * Retrieves the current logged-in user (if any).
     */
    val currentUser get() = firebaseAuth.currentUser

    /**
     * Checks if the user is authenticated with Firebase.
     */
    fun isUserSignedIn(): Boolean = firebaseAuth.currentUser != null

    /**
     * Step 2: Credential Manager Setup & Option Construction
     * Constructs a GetCredentialRequest containing Google Sign-In options.
     */
    private fun buildGoogleSignInRequest(): GetCredentialRequest {
        // Build the Google ID Token retrieval option
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false) // Set to true to filter to already authorized accounts
            .setServerClientId(webClientId)
            .setAutoSelectEnabled(false) // Automatically selects account if only one is available
            .build()

        // Build and return the overall Credential Manager request
        return GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()
    }

    /**
     * Step 3: Firebase Authentication Exchange
     * Triggers the bottom sheet popup, retrieves the credential, extracts the ID token, 
     * and signs in with Firebase.
     *
     * @param activityContext The Activity Context needed to host the Credential Manager overlay UI.
     */
    suspend fun signInWithGoogle(activityContext: Context): Result<AuthResult> {
        return try {
            val request = buildGoogleSignInRequest()

            Log.d(TAG, "Requesting credential from Credential Manager...")
            // Trigger the native Credential Manager UI bottom-sheet prompt
            val result: GetCredentialResponse = credentialManager.getCredential(
                context = activityContext,
                request = request
            )

            // Parse the outcome
            handleCredentialResponse(result)
        } catch (e: GetCredentialException) {
            Log.e(TAG, "Credential Manager API failed: ${e.message}", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during sign-in flow: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Parses the credential manager result, extracts the Google ID Token, 
     * and exchanges it for a Firebase session.
     */
    private suspend fun handleCredentialResponse(response: GetCredentialResponse): Result<AuthResult> {
        val credential = response.credential

        // Check if the credential returned is indeed a Google ID Token
        if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            try {
                // Parse out Google-specific properties (e.g. ID Token, email, name)
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken

                Log.d(TAG, "Google ID Token extracted successfully. Authenticating with Firebase...")

                // Create a Firebase credential package using the extracted ID token
                val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)

                // Perform signing via suspending coroutines tasks await extension
                val authResult = firebaseAuth.signInWithCredential(firebaseCredential).await()
                
                Log.d(TAG, "Successfully authenticated with Firebase. User: ${authResult.user?.email}")
                return Result.success(authResult)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse Google ID Token or Authenticate with Firebase: ${e.message}", e)
                return Result.failure(e)
            }
        } else {
            val errMsg = "Unexpected credential type returned: ${credential.type}"
            Log.e(TAG, errMsg)
            return Result.failure(IllegalArgumentException(errMsg))
        }
    }

    /**
     * Step 4: Sign-Out Logic
     * Signs out from Firebase Authentication and clears the local credential manager cache state.
     */
    suspend fun signOutUser(): Result<Unit> {
        return try {
            // 1. Sign out from Firebase Auth session
            firebaseAuth.signOut()
            Log.d(TAG, "Firebase Auth session logged out successfully.")

            // 2. Clear local credential state in the Credential Manager (revokes single-click auto-sign-in caching)
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
            Log.d(TAG, "Credential Manager state cleared successfully.")

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error occurred during logout process: ${e.message}", e)
            Result.failure(e)
        }
    }
}
```

---

## 4. UI Integration Guide (Jetpack Compose)

Below is an elegant Compose template displaying how to trigger the auth flow and display the currently authenticated state with loading screens.

```kotlin
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun GoogleSignInScreen(authManager: FirebaseAuthManager) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var userEmail by remember { mutableStateOf(authManager.currentUser?.email) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (userEmail != null) {
            // Logged In UI
            Text("Welcome, Zimbabwe Health Worker", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Active EHR Session: $userEmail", style = MaterialTheme.typography.bodyMedium)
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = {
                    coroutineScope.launch {
                        isLoading = true
                        val result = authManager.signOutUser()
                        if (result.isSuccess) {
                            userEmail = null
                        } else {
                            errorMessage = "Sign Out Failed: ${result.exceptionOrNull()?.message}"
                        }
                        isLoading = false
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("End Session (Sign Out)")
            }
        } else {
            // Logged Out UI / Prompt
            Text("National EHR Central Authentication", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(16.dp))
            
            if (isLoading) {
                CircularProgressIndicator()
            } else {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            isLoading = true
                            errorMessage = null
                            val result = authManager.signInWithGoogle(context)
                            if (result.isSuccess) {
                                userEmail = authManager.currentUser?.email
                            } else {
                                errorMessage = result.exceptionOrNull()?.message ?: "Unknown login error"
                            }
                            isLoading = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Text("Secure Sign-In with Google")
                }
            }
        }

        errorMessage?.let { error ->
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
        }
    }
}
```

---

## 5. Security & Production Best Practices

1. **Never Hardcode clientID keys:** Inject your `webClientId` via a secure environment variable utilizing the AI Studio Secrets panel. Place your secret as `SERVER_CLIENT_ID` in your `.env` and read it in Kotlin via `BuildConfig.SERVER_CLIENT_ID`.
2. **Handling Exceptions Safely:** 
   - `GetCredentialException` can happen if a user explicitly cancels the bottom sheet prompt. Ensure your UI catches this and gracefully exits loading states rather than popping up misleading error messages.
3. **Keep dynamic verification:** Continue checking the validity of the Firebase user credentials on app startup via `authManager.isUserSignedIn()` inside a coroutine launcher effect in your app's main view.
