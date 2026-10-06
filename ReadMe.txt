================================================================================
MINISTRY OF HEALTH AND CHILD CARE OF ZIMBABWE (MoHCC)
CHILD IMMUNISATION TRACKING & EHR INTEROPERABILITY SYSTEM
================================================================================

This document contains the core architectural instructions for linking your
offline-first Room Database with Google Firebase and Cloud Firestore, along with
the extracted developer fingerprints (SHA-1 & SHA-256) of your debug build keystore
needed to register the application with your Firebase Project Console.

--------------------------------------------------------------------------------
1. EXTRACTED CERTIFICATE FINGERPRINTS
--------------------------------------------------------------------------------
Copy these fingerprints and add them in your Firebase Console under:
Settings (Gear icon) -> Project settings -> Your apps -> SHA certificate fingerprints

* SHA-1 Fingerprint:
  F1:7C:B0:C1:B6:36:59:D6:FD:A5:0A:70:6F:11:02:A9:D5:3B:25:86

* SHA-256 Fingerprint:
  1E:EE:14:4C:7A:F4:86:B4:A3:15:7E:1B:DD:AB:6E:FE:BB:45:A8:D3:47:93:0C:F4:BB:4B:23:72:04:34:21:4F

* Keystore Details:
  - Alias: androiddebugkey
  - Store Password: android
  - Key Password: android

--------------------------------------------------------------------------------
2. SYSTEM COMPONENTS REQUIRING FIREBASE / FIRESTORE LINKING
--------------------------------------------------------------------------------
The Zimbabwe Child Immunisation Tracking System relies on an offline-first setup,
backed by Room Database, to handle intermittent connectivity in rural areas. The
following components require direct linking to Cloud Firestore / Firebase:

A. Child Profiles & Demographics Registry
   - Purpose: Register infants nationally using a secure Health Identification
     PIN (e.g., ZW-MOH-2025-0421).
   - Firestore Sync Requirement: Allows inter-clinic patient lookup and transfer.
     If a mother registers her child in Harare Central but travels to Mutare, the
     Mutare clinic can instantly retrieve the child's profile from Firestore.

B. Vaccination Records & Administration Logs (ZEPI Schedule)
   - Purpose: Manage progress against the Zimbabwe Expanded Programme on
     Immunisation (ZEPI) schedules (BCG, Oral Polio, Pentavalent, PCV, etc.).
   - Firestore Sync Requirement: Stores administered dose records with unique
     vaccine batch numbers, administration dates, facility names, and healthcare
     workers' digital signatures. Firestore synchronization ensures complete
     historical integrity across clinics, preventing duplicate dosage errors.

C. Clinic Appointments & Booking Schedules
   - Purpose: Allow parents to request vaccination slots and nurses to schedule/
     approve visits.
   - Firestore Sync Requirement: Synchronization enables real-time booking. When
     a mother schedules a slot through the Parent Portal, it is instantly loaded
     on the Clinic Portal's queue.

D. Clinic Messaging & Parent Consultations
   - Purpose: Secure, live support channel where parents ask pediatric/vaccine-
     related questions to doctors or nurses.
   - Firebase Realtime Database/Firestore Sync: Provides instant message updates,
     allowing direct nurse-to-mother chat streams without refreshing.

E. EHR Interoperability & FHIR Data Exchange
   - Purpose: Export and import standardised HL7/FHIR Patient and Immunization
     JSON transaction bundles with MoHCC national systems.
   - Firebase Sync Requirement: Firestore or Firebase Cloud Storage manages secure
     JSON bundle uploads, and triggers Firebase Cloud Functions to ingest records
     into broader national health systems.

F. Ministry of Health (MoH) District Analytics & Vaccine Heatmaps
   - Purpose: Provide district-wide visibility into coverage rates, immunisation
     efficiency, and highlight lost-to-follow-up (LTFU) rates.
   - Firestore Sync Requirement: Aggregated Firestore collection fields are
     monitored to compile live dashboard statistics on general clinic compliance
     rates by district (e.g., Harare, Bulawayo, Gweru, Mutare).

G. User Feedback & Support System
   - Purpose: Collect bugs, technical complaints, or offline issues submitted by
     clinic staff.
   - Firestore Sync Requirement: Feedbacks are directly written to a global support
     database collection to assist MoH technical teams in troubleshooting immediately.

--------------------------------------------------------------------------------
3. METHODS OF PROMINENTLY LINKING SYSTEM TO FIREBASE & CLOUD FIRESTORE
--------------------------------------------------------------------------------
To establish a fully secure, functional link, complete the following steps:

STEP I: Firebase App Console Setup
  1. Open the Firebase Console: https://console.firebase.google.com/
  2. Create a project named "cits-4d830" or select your existing project.
  3. Click "Add App" -> Select "Android" icon.
  4. Enter the Package Name: com.matshelela.childimmunisationtrackingsystemappv1
  5. Under "Debug signing certificate SHA-1", paste the following code:
     F1:7C:B0:C1:B6:36:59:D6:FD:A5:0A:70:6F:11:02:A9:D5:3B:25:86
  6. Register the app, then download the custom `google-services.json` file.

STEP II: Gradle Dependencies Configuration (Already Configured in App)
  The project is pre-configured with the required plugin definitions:
  - Project build.gradle.kts includes:
    id("com.google.gms.google-services") version "4.5.0" apply false
  - App build.gradle.kts includes:
    id("com.google.gms.google-services")
    implementation(platform("com.google.firebase:firebase-bom:34.15.0"))
    implementation("com.google.firebase:firebase-analytics")

  To include Cloud Firestore and Firebase Auth, add the following lines to your
  dependencies block in `/app/build.gradle.kts`:
  
  // Cloud Firestore database dependency
  implementation("com.google.firebase:firebase-firestore")
  
  // Firebase Auth (required for secure doctor/parent authentication)
  implementation("com.google.firebase:firebase-auth")

STEP III: Place the google-services.json
  Ensure the `google-services.json` is located in the `/app/` directory (already
  successfully placed under `/app/google-services.json`). This file contains API
  keys and configuration credentials that direct the SDKs to your database cloud.

STEP IV: Implementing Offline-First Synchronisation (Architectural Pattern)
  To link Room entities to Firestore, modify your repository model to follow the
  write-through cache pattern:

  1. Write Locally first:
     When offline, records are stored in the local Room DB with `isSynced = false`.
  2. Detect Connection:
     Use `isNetworkOnline` state from `AppViewModel` or standard NetworkCallbacks.
  3. Sync with Firestore:
     Iterate through unsynced records, mapping them to Firestore documents, and
     mark `isSynced = true` inside the local Room database:

     ```kotlin
     import com.google.firebase.firestore.FirebaseFirestore

     class ImmunisationRepository(private val dao: ImmunisationDao) {
         private val firestore = FirebaseFirestore.getInstance()

         suspend fun syncOfflineRecordsToFirestore() {
             val unsyncedChildProfiles = dao.getUnsyncedChildProfiles()
             for (profile in unsyncedChildProfiles) {
                 firestore.collection("child_profiles")
                     .document(profile.healthPin)
                     .set(profile)
                     .addOnSuccessListener {
                         // Update local room status
                         coroutineScope.launch {
                             dao.updateChildProfile(profile.copy(isSynced = true))
                         }
                     }
             }
         }
     }
     ```

STEP V: Enable Firestore Security Rules (Security Enforcement)
  Enforce strict role-based access in Firestore rules matching the `UserRole`
  logic (e.g. only validated MOH_ADMIN and DOCTOR_CLINIC users can write to
  collections, while PARENT_GUARDIAN users have read-only access to their specific
  children's records):

  ```javascript
  rules_version = '2';
  service cloud.firestore {
    match /databases/{database}/documents {
      match /child_profiles/{profileId} {
        allow read: if request.auth != null;
        allow write: if request.auth.token.role == "MOH_ADMIN" || request.auth.token.role == "DOCTOR_CLINIC";
      }
    }
  }
  ```

--------------------------------------------------------------------------------
4. IMPLEMENTING SECURE GOOGLE SIGN-IN VIA CREDENTIAL MANAGER & FIREBASE AUTH
--------------------------------------------------------------------------------
Below is the complete architectural implementation guide and production Kotlin code to run Google Sign-In using Android's modern Credential Manager API.

A. Module Gradle Dependencies (/app/build.gradle.kts)
   Ensure the following dependencies are present (already integrated and compiled successfully):
   ```kotlin
   dependencies {
       // Firebase Bill of Materials (BoM)
       implementation(platform("com.google.firebase:firebase-bom:34.15.0"))

       // Firebase Authentication Library
       implementation("com.google.firebase:firebase-auth")

       // Android Credential Manager Library for Modern Auth
       implementation("androidx.credentials:credentials:1.3.0")

       // Play Services credential helper for devices running below Android 14
       implementation("androidx.credentials:credentials-play-services-auth:1.3.0")

       // Google Sign-In SDK
       implementation("com.google.android.gms:play-services-auth:21.2.0")
   }
   ```

B. Modern Credential Manager Request Building
   The Credential Manager consolidates all authentication methods (Passkeys, Google Sign-In, saved passwords) into a unified bottom sheet. Construct a GetCredentialRequest using your Google OAuth Client ID:
   ```kotlin
   import androidx.credentials.GetCredentialRequest
   import com.google.android.libraries.identity.googleid.GetGoogleIdOption

   // Web client ID retrieved from Google Cloud Console for project cits-4d830
   val webClientId = "YOUR_OAUTH_WEB_CLIENT_ID.apps.googleusercontent.com"

   private fun buildGoogleSignInRequest(): GetCredentialRequest {
       val googleIdOption = GetGoogleIdOption.Builder()
           .setFilterByAuthorizedAccounts(false) // Set to true to prioritize pre-authorized accounts
           .setServerClientId(webClientId)
           .setAutoSelectEnabled(false) // Auto-log in if only one account exists
           .build()

       return GetCredentialRequest.Builder()
           .addCredentialOption(googleIdOption)
           .build()
   }
   ```

C. Triggering UI & Firebase Authentication Exchange
   Use Kotlin Coroutines to trigger the native bottom sheet picker, parse the selected Google profile, extract the Google ID Token, and securely authenticate into Firebase via signInWithCredential:
   ```kotlin
   import android.content.Context
   import android.util.Log
   import androidx.credentials.CredentialManager
   import androidx.credentials.GetCredentialResponse
   import androidx.credentials.exceptions.GetCredentialException
   import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
   import com.google.firebase.auth.AuthResult
   import com.google.firebase.auth.FirebaseAuth
   import com.google.firebase.auth.GoogleAuthProvider
   import kotlinx.coroutines.tasks.await

   suspend fun signInWithGoogle(context: Context, activityContext: Context): Result<AuthResult> {
       val credentialManager = CredentialManager.create(context)
       val firebaseAuth = FirebaseAuth.getInstance()

       return try {
           val request = buildGoogleSignInRequest()
           
           // Triggers native credential picker bottom-sheet
           val response: GetCredentialResponse = credentialManager.getCredential(
               context = activityContext,
               request = request
           )

           val credential = response.credential
           if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
               val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
               val idToken = googleIdTokenCredential.idToken

               // Translate Google credentials to Firebase
               val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
               val authResult = firebaseAuth.signInWithCredential(firebaseCredential).await()
               
               Result.success(authResult)
           } else {
               Result.failure(IllegalArgumentException("Unexpected credential type: ${credential.type}"))
           }
       } catch (e: GetCredentialException) {
           Log.e("Auth", "Credential Manager failed: ${e.message}")
           Result.failure(e)
       } catch (e: Exception) {
           Result.failure(e)
       }
   }
   ```

D. Dynamic Sign-Out & Cache-Clearing Logic
   To securely log a health worker out of the EHR session, you must:
   1. Revoke the active Firebase Session.
   2. Clear the Credential Manager state cache (resetting single-click caching behavior):
   ```kotlin
   import androidx.credentials.ClearCredentialStateRequest

   suspend fun signOutUser(context: Context): Result<Unit> {
       return try {
           // 1. Sign out of Firebase session
           FirebaseAuth.getInstance().signOut()

           // 2. Clear credentials state locally
           val credentialManager = CredentialManager.create(context)
           credentialManager.clearCredentialState(ClearCredentialStateRequest())
           
           Result.success(Unit)
       } catch (e: Exception) {
           Result.failure(e)
       }
   }
   ```
================================================================================

--------------------------------------------------------------------------------
5. OFFLINE-FIRST FIRESTORE SYNCHRONISATION ARCHITECTURE & SECURITY RULES
--------------------------------------------------------------------------------
We have fully implemented a production-grade write-through cache synchronization layer for the ZEPI Childhood Immunisation system, ensuring seamless medical registry backups and clinic transfers.

A. Complete File Path Registry (Created and Verified):
   - Mapped Firestore Data Structures: `/app/src/main/java/com/matshelela/childimmunisationtrackingsystemappv1/data/FirestoreModels.kt`
   - Synchronization Engine Repository: `/app/src/main/java/com/matshelela/childimmunisationtrackingsystemappv1/data/FirestoreSyncRepository.kt`
   - Background WorkManager Daemon: `/app/src/main/java/com/matshelela/childimmunisationtrackingsystemappv1/data/FirestoreSyncWorker.kt`

B. Sync Process Flow (Write-Through Pattern):
   1. Unsynced Records Collection: DAO detects entries across five tables where `isSynced = false`.
   2. Subcollection Hierarchies: Maps the flat local database childId integers into the global, searchable child health PIN (healthPin) of Zimbabwe (e.g. `ZW-MOH-2026-X84A`).
   3. Write Transaction: Uploads nested children objects to their corresponding Firestore endpoints.
   4. Local Database Resolution: Upon completion of the firestore coroutine await, updates local entity states to `isSynced = true` to prevent duplicate network payloads.

C. WorkManager Execution Triggers:
   We utilize Jetpack WorkManager to ensure robust system synchronization without consuming intensive background resources. By enqueuing tasks with `NetworkType.CONNECTED` constraints, the sync automatically executes as soon as the clinic tablet or healthcare worker device regains active telemetry:
   ```kotlin
   // Enqueue background synchronisation:
   FirestoreSyncWorker.enqueue(context)
   ```

D. Custom Firestore Security Rules
   Apply these security rules in your Firestore Database settings (Firebase Console -> Firestore Database -> Rules tab). These rules strictly restrict write privileges to official Zimbabwe Ministry of Health administrators and doctors, while limiting parents to viewing their child's verified medical cards:

   ```javascript
   rules_version = '2';
   service cloud.firestore {
     match /databases/{database}/documents {
       
       // Helper functions to inspect JWT Custom Claims
       function isMohAdmin() {
         return request.auth != null && request.auth.token.role == 'MOH_ADMIN';
       }
       
       function isDoctorClinic() {
         return request.auth != null && request.auth.token.role == 'DOCTOR_CLINIC';
       }
       
       function isParentOf(healthPin) {
         return request.auth != null && 
                request.auth.token.role == 'PARENT_GUARDIAN' && 
                request.auth.token.associated_pin == healthPin;
       }

       // 1. Child Profiles Collection
       match /child_profiles/{healthPin} {
         // Parents can read only their registered child profile. Admins and Doctors can read everything.
         allow read: if isMohAdmin() || isDoctorClinic() || isParentOf(healthPin);
         
         // Only verified clinical personnel can register/update a child profile
         allow write: if isMohAdmin() || isDoctorClinic();

         // 1B. Childhood Vaccination Records subcollection (ZEPI Schedule)
         match /vaccination_records/{recordId} {
           // Parents can inspect their own child's dosage logs
           allow read: if isMohAdmin() || isDoctorClinic() || isParentOf(healthPin);
           
           // Only doctors/nurses can sign and log administered vaccinations
           allow write: if isMohAdmin() || isDoctorClinic();
         }
       }

       // 2. Clinic Appointments Collection
       match /appointments/{appointmentId} {
         // Read access is allowed if user is doctor or child's parent
         allow read: if isMohAdmin() || isDoctorClinic() || 
                     isParentOf(resource.data.childHealthPin);
                     
         // Parents can create appointments, Doctors/Admins can edit/approve/schedule them
         allow create: if request.auth != null;
         allow update, delete: if isMohAdmin() || isDoctorClinic();
       }

       // 3. Live Clinic Message / Chat Threads
       match /chats/{chatId}/messages/{messageId} {
         // Chat ID is bound to the child health PIN. Message participants must have access.
         allow read: if isMohAdmin() || isDoctorClinic() || isParentOf(chatId);
         allow write: if request.auth != null && 
                      (isMohAdmin() || isDoctorClinic() || isParentOf(chatId));
       }

       // 4. Support Feedback Collection
       match /feedback/{feedbackId} {
         // Feedback is written by health workers or parents, but read only by Ministry central IT
         allow create: if request.auth != null;
         allow read, update, delete: if isMohAdmin();
       }
     }
   }
   ```
================================================================================
