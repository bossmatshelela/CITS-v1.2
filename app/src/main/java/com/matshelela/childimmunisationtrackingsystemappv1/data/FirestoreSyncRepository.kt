package com.matshelela.childimmunisationtrackingsystemappv1.data

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.DocumentChange
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FirestoreSyncRepository(private val dao: ImmunisationDao) {

    private val firestore: FirebaseFirestore?
        get() = try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.w(TAG, "FirebaseFirestore instance unavailable: ${e.message}")
            null
        }

    companion object {
        private const val TAG = "FirestoreSyncRepo"
    }

    /**
     * Master synchronization method that triggers synchronization across all tables,
     * including syncing offline child records and caching health facility data from Firestore.
     */
    suspend fun syncAllUnsyncedRecords(): Result<Unit> {
        return try {
            syncChildProfiles()
            syncVaccinationRecords()
            syncAppointments()
            syncClinicMessages()
            syncUserFeedbacks()
            fetchAndCacheHealthFacilities()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error synchronizing database: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * 1. Synchronize Child Profiles.
     * Document key is the unique national Zimbabwe Health PIN (healthPin).
     */
    suspend fun syncChildProfiles() {
        val db = firestore ?: return
        val unsynced = dao.getUnsyncedChildProfiles()
        Log.d(TAG, "Found ${unsynced.size} unsynced child profiles.")
        for (profile in unsynced) {
            try {
                val firestoreProfile = FirestoreChildProfile(profile)
                
                // Write to Firestore under child_profiles collection with healthPin as the key
                db.collection("child_profiles")
                    .document(profile.healthPin)
                    .set(firestoreProfile.toMap())
                    .await()

                // Mark as synced locally
                dao.updateChildProfile(profile.copy(isSynced = true))
                Log.d(TAG, "Successfully synced ChildProfile: ${profile.name} (Health PIN: ${profile.healthPin})")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to sync ChildProfile ID ${profile.id}: ${e.message}")
            }
        }
    }

    /**
     * 2. Synchronize Vaccination Records.
     * Maps local childId to the Child's national healthPin first, then uploads to
     * `/child_profiles/{healthPin}/vaccination_records/{vaccineName}_{doseNumber}` subcollection.
     */
    suspend fun syncVaccinationRecords() {
        val db = firestore ?: return
        val unsynced = dao.getUnsyncedVaccinationRecords()
        Log.d(TAG, "Found ${unsynced.size} unsynced vaccination records.")
        for (record in unsynced) {
            try {
                // Look up the child locally to get their national healthPin
                val child = dao.getChildProfileById(record.childId).first()
                if (child == null) {
                    Log.e(TAG, "Skipping VaccinationRecord ID ${record.id}: Child ID ${record.childId} not found in local Room Database")
                    continue
                }

                val firestoreRecord = FirestoreVaccinationRecord(record, child.healthPin)
                val documentId = "${record.vaccineName.replace(" ", "_")}_Dose${record.doseNumber}"

                // Save to subcollection of specific child
                db.collection("child_profiles")
                    .document(child.healthPin)
                    .collection("vaccination_records")
                    .document(documentId)
                    .set(firestoreRecord.toMap())
                    .await()

                // Update local Room database flag
                dao.updateVaccinationRecord(record.copy(isSynced = true))
                Log.d(TAG, "Successfully synced vaccination record: ${record.vaccineName} Dose ${record.doseNumber} for child ${child.name}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to sync VaccinationRecord ID ${record.id}: ${e.message}")
            }
        }
    }

    /**
     * 3. Synchronize Appointments.
     * Maps childId to healthPin and uploads to global `/appointments/{appointmentId}` collection.
     */
    suspend fun syncAppointments() {
        val db = firestore ?: return
        val unsynced = dao.getUnsyncedAppointments()
        Log.d(TAG, "Found ${unsynced.size} unsynced appointments.")
        for (appointment in unsynced) {
            try {
                val child = dao.getChildProfileById(appointment.childId).first()
                val childPin = child?.healthPin ?: "UNKNOWN_PIN"

                val firestoreAppointment = FirestoreAppointment(appointment, childPin)
                
                db.collection("appointments")
                    .document(appointment.id.toString())
                    .set(firestoreAppointment.toMap())
                    .await()

                dao.updateAppointment(appointment.copy(isSynced = true))
                Log.d(TAG, "Successfully synced Appointment ID ${appointment.id} for child PIN $childPin")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to sync Appointment ID ${appointment.id}: ${e.message}")
            }
        }
    }

    /**
     * Upload a Base64-encoded RSA public key to Firestore for key distribution.
     * Path: `/users/{userId}/public_keys/main`
     */
    suspend fun uploadPublicKey(userId: String, publicKeyBase64: String) {
        val db = firestore ?: return
        try {
            db.collection("users")
                .document(userId)
                .collection("public_keys")
                .document("main")
                .set(mapOf("publicKey" to publicKeyBase64))
                .await()
            Log.d(TAG, "Successfully uploaded public key for user $userId")
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.d(TAG, "Public key upload skipped or deferred for $userId: ${e.message}")
        }
    }

    /**
     * Query a participant's Base64-encoded RSA public key from Firestore.
     * Path: `/users/{userId}/public_keys/main`
     */
    suspend fun fetchPublicKey(userId: String): String? {
        val db = firestore ?: return null
        return try {
            val document = db.collection("users")
                .document(userId)
                .collection("public_keys")
                .document("main")
                .get()
                .await()
            document.getString("publicKey")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch public key for $userId: ${e.message}")
            null
        }
    }

    /**
     * 4. Synchronize Clinic Messages.
     * Path: `/chats/{chatId}/messages/{messageId}`
     */
    suspend fun syncClinicMessages() {
        val unsynced = dao.getUnsyncedClinicMessages()
        Log.d(TAG, "Found ${unsynced.size} unsynced clinic messages.")
        for (message in unsynced) {
            try {
                val child = message.childId?.let { dao.getChildProfileById(it).first() }
                val childPin = child?.healthPin ?: "GENERAL"
                val chatId = childPin

                // Decrypt local XOR content to plaintext
                val plainText = EncryptionHelper.decrypt(message.content)
                
                // Determine recipient userId
                val recipientUserId = if (message.senderRole == "Parent") {
                    "Doctor"
                } else {
                    childPin
                }

                var finalContent = message.content
                var encryptedAesKey: String? = null
                var isE2EE = false

                if (recipientUserId != "GENERAL") {
                    // Query recipient's public key from Firestore
                    val publicKeyBase64 = fetchPublicKey(recipientUserId)
                    if (!publicKeyBase64.isNullOrEmpty()) {
                        val recipientPublicKey = E2EEncryptionHelper.loadPublicKeyFromBase64(publicKeyBase64)
                        if (recipientPublicKey != null) {
                            // Generate single-use symmetric AES key
                            val aesKey = E2EEncryptionHelper.generateSingleUseAesKey()
                            // Encrypt message text locally using the AES key
                            val payload = E2EEncryptionHelper.encryptPayload(plainText, aesKey)
                            // Encrypt symmetric AES key with recipient's public key
                            val encKey = E2EEncryptionHelper.encryptAesKey(aesKey, recipientPublicKey)
                            
                            if (payload != null && encKey != null) {
                                finalContent = payload
                                encryptedAesKey = encKey
                                isE2EE = true
                                Log.d(TAG, "Successfully E2E-encrypted message payload for $recipientUserId")
                            }
                        }
                    }
                }

                val firestoreMessage = FirestoreChatMessage(
                    id = message.id.toString(),
                    childHealthPin = childPin,
                    senderName = message.senderName,
                    senderRole = message.senderRole,
                    content = finalContent,
                    timestamp = message.timestamp,
                    isEncrypted = !isE2EE,
                    recipientRole = message.recipientRole,
                    isE2EE = isE2EE,
                    encryptedAesKey = encryptedAesKey
                )
                
                // Write both the encrypted message payload and the encrypted AES key to Firestore
                val db = firestore
                if (db != null) {
                    db.collection("chats")
                        .document(chatId)
                        .collection("messages")
                        .document(message.id.toString())
                        .set(firestoreMessage.toMap())
                        .await()
                }

                // Save locally with updated isSynced = true
                dao.insertMessage(message.copy(isSynced = true))
                Log.d(TAG, "Successfully synced ClinicMessage ID ${message.id} to chat thread $chatId")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to sync ClinicMessage ID ${message.id}: ${e.message}")
            }
        }
    }

    /**
     * 5. Synchronize User Feedbacks.
     * Path: `/feedback/{feedbackId}`
     */
    suspend fun syncUserFeedbacks() {
        val db = firestore ?: return
        val unsynced = dao.getUnsyncedUserFeedbacks()
        Log.d(TAG, "Found ${unsynced.size} unsynced user feedbacks.")
        for (feedback in unsynced) {
            try {
                val firestoreFeedback = FirestoreFeedback(feedback)
                
                db.collection("feedback")
                    .document(feedback.id.toString())
                    .set(firestoreFeedback.toMap())
                    .await()

                dao.updateUserFeedback(feedback.copy(isSynced = true))
                Log.d(TAG, "Successfully synced UserFeedback ID ${feedback.id}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to sync UserFeedback ID ${feedback.id}: ${e.message}")
            }
        }
    }

    private val registrationsList = java.util.concurrent.CopyOnWriteArrayList<ListenerRegistration>()

    /**
     * Start a real-time listener on the Firestore database to retrieve new messages
     * and persist them into the local Room database. This enables offline-first instant
     * updates on both the chat list and the individual chat windows.
     */
    fun startRealTimeMessageListener(
        currentRole: String, // e.g., "Parent", "Doctor", "MoH Admin"
        childList: List<ChildProfile>,
        onNewMessagesAdded: suspend (List<ClinicMessage>) -> Unit
    ) {
        // Stop any existing listeners first
        stopRealTimeMessageListener()

        Log.d(TAG, "Starting real-time Firestore message listener for role: $currentRole")

        val db = firestore ?: run {
            Log.d(TAG, "Firestore not available, live messaging listener skipped")
            return
        }

        if (currentRole == "Parent") {
            // A parent only needs to listen to messages for their registered children
            val healthPins = childList.map { it.healthPin }.filter { it.isNotEmpty() }
            Log.d(TAG, "Parent listening to child pins: $healthPins")
            if (healthPins.isEmpty()) return

            healthPins.forEach { healthPin ->
                val registration = db.collection("chats")
                    .document(healthPin)
                    .collection("messages")
                    .addSnapshotListener { snapshots, e ->
                        if (e != null) {
                            Log.e(TAG, "Listen failed for chat $healthPin: ${e.message}")
                            return@addSnapshotListener
                        }
                        if (snapshots != null) {
                            CoroutineScope(Dispatchers.IO).launch {
                                val newRoomMessages = mutableListOf<ClinicMessage>()
                                for (doc in snapshots.documentChanges) {
                                    if (doc.type == DocumentChange.Type.ADDED ||
                                        doc.type == DocumentChange.Type.MODIFIED
                                    ) {
                                        val idStr = doc.document.id
                                        val idInt = idStr.toIntOrNull() ?: doc.document.getString("id")?.toIntOrNull() ?: (idStr.hashCode() and 0xfffffff)
                                        val senderName = doc.document.getString("senderName") ?: ""
                                        val senderRole = doc.document.getString("senderRole") ?: ""
                                        val content = doc.document.getString("content") ?: ""
                                        val timestamp = doc.document.getLong("timestamp") ?: System.currentTimeMillis()
                                        val isEncrypted = doc.document.getBoolean("isEncrypted") ?: true
                                        val recipientRole = doc.document.getString("recipientRole") ?: ""
                                        
                                        val childId = childList.find { it.healthPin == healthPin }?.id

                                        // Hybrid On-Device Decryption
                                        val isE2EE = doc.document.getBoolean("isE2EE") ?: false
                                        val encryptedAesKey = doc.document.getString("encryptedAesKey")

                                        var resolvedContent = content
                                        if (isE2EE && !encryptedAesKey.isNullOrEmpty()) {
                                            val isOwnMessage = senderRole == "Parent"
                                            if (isOwnMessage) {
                                                // Sent by us, preserve local plaintext XOR format
                                                val localMsg = dao.getMessageById(idInt)
                                                if (localMsg != null) {
                                                    resolvedContent = localMsg.content
                                                }
                                            } else {
                                                // Received: Decrypt using recovered symmetric key
                                                try {
                                                    val aesKey = E2EEncryptionHelper.decryptAesKey(encryptedAesKey, "e2ee_local_key")
                                                    if (aesKey != null) {
                                                        val decryptedPlaintext = E2EEncryptionHelper.decryptPayload(content, aesKey)
                                                        if (decryptedPlaintext != null) {
                                                            resolvedContent = EncryptionHelper.encrypt(decryptedPlaintext)
                                                        } else {
                                                            resolvedContent = EncryptionHelper.encrypt("[Decryption Error]")
                                                        }
                                                    } else {
                                                        resolvedContent = EncryptionHelper.encrypt("[Private Key Decrypt Fail]")
                                                    }
                                                } catch (ex: Exception) {
                                                    Log.e(TAG, "Decryption error: ${ex.message}")
                                                    resolvedContent = EncryptionHelper.encrypt("[Decryption Failure]")
                                                }
                                            }
                                        }

                                        newRoomMessages.add(
                                            ClinicMessage(
                                                id = idInt,
                                                childId = childId,
                                                senderName = senderName,
                                                senderRole = senderRole,
                                                content = resolvedContent,
                                                timestamp = timestamp,
                                                isEncrypted = isEncrypted,
                                                recipientRole = recipientRole,
                                                isSynced = true,
                                                isE2EE = isE2EE,
                                                encryptedAesKey = encryptedAesKey
                                            )
                                        )
                                    }
                                }
                                if (newRoomMessages.isNotEmpty()) {
                                    onNewMessagesAdded(newRoomMessages)
                                }
                            }
                        }
                    }
                registrationsList.add(registration)
            }
        } else {
            // For Doctors/Admins, we listen to all messages across chats
            Log.d(TAG, "Doctor/Admin listening to all clinical chat messages via CollectionGroup")
            val registration = db.collectionGroup("messages")
                .addSnapshotListener { snapshots, e ->
                    if (e != null) {
                        Log.e(TAG, "CollectionGroup listen failed: ${e.message}")
                        return@addSnapshotListener
                    }
                    if (snapshots != null) {
                        CoroutineScope(Dispatchers.IO).launch {
                            val newRoomMessages = mutableListOf<ClinicMessage>()
                            for (doc in snapshots.documentChanges) {
                                if (doc.type == DocumentChange.Type.ADDED ||
                                    doc.type == DocumentChange.Type.MODIFIED
                                ) {
                                    val idStr = doc.document.id
                                    val idInt = idStr.toIntOrNull() ?: doc.document.getString("id")?.toIntOrNull() ?: (idStr.hashCode() and 0xfffffff)
                                    val senderName = doc.document.getString("senderName") ?: ""
                                    val senderRole = doc.document.getString("senderRole") ?: ""
                                    val content = doc.document.getString("content") ?: ""
                                    val timestamp = doc.document.getLong("timestamp") ?: System.currentTimeMillis()
                                    val isEncrypted = doc.document.getBoolean("isEncrypted") ?: true
                                    val recipientRole = doc.document.getString("recipientRole") ?: ""
                                    val childHealthPin = doc.document.getString("childHealthPin")

                                    val childId = childList.find { it.healthPin == childHealthPin }?.id

                                    // Hybrid On-Device Decryption
                                    val isE2EE = doc.document.getBoolean("isE2EE") ?: false
                                    val encryptedAesKey = doc.document.getString("encryptedAesKey")

                                    var resolvedContent = content
                                    if (isE2EE && !encryptedAesKey.isNullOrEmpty()) {
                                        val isOwnMessage = senderRole == "Doctor"
                                        if (isOwnMessage) {
                                            // Sent by us, preserve local plaintext XOR format
                                            val localMsg = dao.getMessageById(idInt)
                                            if (localMsg != null) {
                                                resolvedContent = localMsg.content
                                            }
                                        } else {
                                            // Received: Decrypt using recovered symmetric key
                                            try {
                                                val aesKey = E2EEncryptionHelper.decryptAesKey(encryptedAesKey, "e2ee_local_key")
                                                if (aesKey != null) {
                                                    val decryptedPlaintext = E2EEncryptionHelper.decryptPayload(content, aesKey)
                                                    if (decryptedPlaintext != null) {
                                                        resolvedContent = EncryptionHelper.encrypt(decryptedPlaintext)
                                                    } else {
                                                        resolvedContent = EncryptionHelper.encrypt("[Decryption Error]")
                                                    }
                                                } else {
                                                    resolvedContent = EncryptionHelper.encrypt("[Private Key Decrypt Fail]")
                                                }
                                            } catch (ex: Exception) {
                                                Log.e(TAG, "Decryption error: ${ex.message}")
                                                resolvedContent = EncryptionHelper.encrypt("[Decryption Failure]")
                                            }
                                        }
                                    }

                                    newRoomMessages.add(
                                        ClinicMessage(
                                            id = idInt,
                                            childId = childId,
                                            senderName = senderName,
                                            senderRole = senderRole,
                                            content = resolvedContent,
                                            timestamp = timestamp,
                                            isEncrypted = isEncrypted,
                                            recipientRole = recipientRole,
                                            isSynced = true,
                                            isE2EE = isE2EE,
                                            encryptedAesKey = encryptedAesKey
                                        )
                                    )
                                }
                            }
                            if (newRoomMessages.isNotEmpty()) {
                                onNewMessagesAdded(newRoomMessages)
                            }
                        }
                    }
                }
            registrationsList.add(registration)
        }
    }

    /**
     * Stop all active real-time message snapshot listeners.
     */
    fun stopRealTimeMessageListener() {
        Log.d(TAG, "Stopping all active real-time Firestore listeners.")
        registrationsList.forEach { it.remove() }
        registrationsList.clear()
    }

    // =========================================================================
    // HEALTH FACILITY CACHING FOR REMOTE & OFFLINE-FIRST ACCESS
    // =========================================================================

    /**
     * Fetches health facilities from Firestore and caches them into the local Room database.
     * In remote rural areas with poor or nonexistent connectivity, this fails safely and
     * preserves the existing Room database cache so users can always access clinic location,
     * GPS coordinates, contact phone, operating hours, and cold chain status offline.
     */
    suspend fun fetchAndCacheHealthFacilities(): Result<Int> {
        val db = firestore ?: run {
            Log.d(TAG, "Firestore not available, preserving local Room cache")
            return Result.success(0)
        }
        return try {
            Log.d(TAG, "Attempting to fetch and cache health facilities from Firestore...")
            val snapshot = db.collection("health_facilities")
                .get()
                .await()

            if (!snapshot.isEmpty) {
                val cachedFacilities = mutableListOf<Clinic>()
                for (doc in snapshot.documents) {
                    val localId = doc.getLong("localId")?.toInt() ?: (doc.id.hashCode() and 0x7fffffff)
                    val name = doc.getString("name") ?: "MoH Clinic"
                    val district = doc.getString("district") ?: "Zimbabwe"
                    val province = doc.getString("province") ?: "Zimbabwe"
                    val address = doc.getString("address") ?: "MoH Facility"
                    val latitude = doc.getDouble("latitude") ?: -17.8252
                    val longitude = doc.getDouble("longitude") ?: 31.0335
                    val contactPhone = doc.getString("contactPhone") ?: "+263 242 700000"
                    val emergencyContact = doc.getString("emergencyContact") ?: "999 / +263 772 100 200"
                    val operatingHours = doc.getString("operatingHours") ?: "08:00 - 16:30 Mon-Fri"
                    val servicesOffered = doc.getString("servicesOffered") ?: "ZEPI Vaccines, Cold Chain Storage"
                    val coldChainStatus = doc.getString("coldChainStatus") ?: "Operational (Solar Powered)"
                    val totalChildren = doc.getLong("totalChildren")?.toInt() ?: 0
                    val efficiencyRate = doc.getDouble("efficiencyRate") ?: 90.0
                    val hasIntermittentInternet = doc.getBoolean("hasIntermittentInternet") ?: false

                    val clinic = Clinic(
                        id = localId,
                        name = name,
                        district = district,
                        province = province,
                        address = address,
                        latitude = latitude,
                        longitude = longitude,
                        contactPhone = contactPhone,
                        emergencyContact = emergencyContact,
                        operatingHours = operatingHours,
                        servicesOffered = servicesOffered,
                        coldChainStatus = coldChainStatus,
                        totalChildren = totalChildren,
                        efficiencyRate = efficiencyRate,
                        hasIntermittentInternet = hasIntermittentInternet,
                        firestoreId = doc.id,
                        lastCachedAt = System.currentTimeMillis(),
                        isCachedLocally = true
                    )
                    cachedFacilities.add(clinic)
                }

                if (cachedFacilities.isNotEmpty()) {
                    dao.insertClinics(cachedFacilities)
                    Log.d(TAG, "Successfully cached ${cachedFacilities.size} health facilities from Firestore into Room database.")
                    return Result.success(cachedFacilities.size)
                }
            } else {
                // If Firestore collection is empty, seed it from the initial local Room database
                val localClinics = dao.getAllClinics().first()
                if (localClinics.isNotEmpty()) {
                    Log.d(TAG, "Firestore health_facilities collection is empty. Seeding ${localClinics.size} facilities to cloud...")
                    uploadHealthFacilities(localClinics)
                }
            }
            Result.success(0)
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Offline or unreachable network: Health facility cache preserved in Room Database. (${e.message})")
            // Return success with 0 to indicate graceful local fallback
            Result.success(0)
        }
    }

    /**
     * Upload a health facility to Firestore.
     * Path: `/health_facilities/{facilityId}`
     */
    suspend fun uploadHealthFacility(clinic: Clinic): Boolean {
        val db = firestore ?: return false
        return try {
            val docId = if (clinic.firestoreId.isNotEmpty()) clinic.firestoreId else "facility_${clinic.id}"
            val firestoreModel = FirestoreHealthFacility(clinic)
            db.collection("health_facilities")
                .document(docId)
                .set(firestoreModel.toMap())
                .await()
            Log.d(TAG, "Successfully uploaded health facility: ${clinic.name} ($docId)")
            true
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to upload health facility ${clinic.name}: ${e.message}")
            false
        }
    }

    /**
     * Batch uploads a list of health facilities to Firestore to maintain cloud synchronization.
     */
    suspend fun uploadHealthFacilities(clinics: List<Clinic>) {
        for (clinic in clinics) {
            uploadHealthFacility(clinic)
        }
    }

    /**
     * Real-time listener for health facilities updates from Firestore,
     * automatically updating the local Room SQLite cache when changes happen online.
     */
    fun startRealTimeHealthFacilitiesListener(onFacilitiesUpdated: suspend (List<Clinic>) -> Unit) {
        val db = firestore ?: run {
            Log.d(TAG, "Firestore not available, live health facility listener skipped")
            return
        }
        try {
            val registration = db.collection("health_facilities")
                .addSnapshotListener { snapshots, e ->
                    if (e != null) {
                        Log.d(TAG, "Health facilities snapshot listener encountered error or offline mode: ${e.message}")
                        return@addSnapshotListener
                    }
                    if (snapshots != null && !snapshots.isEmpty) {
                        CoroutineScope(Dispatchers.IO).launch {
                            val facilities = mutableListOf<Clinic>()
                            for (doc in snapshots.documents) {
                                val localId = doc.getLong("localId")?.toInt() ?: (doc.id.hashCode() and 0x7fffffff)
                                val name = doc.getString("name") ?: "MoH Clinic"
                                val district = doc.getString("district") ?: "Zimbabwe"
                                val province = doc.getString("province") ?: "Zimbabwe"
                                val address = doc.getString("address") ?: "MoH Facility"
                                val latitude = doc.getDouble("latitude") ?: -17.8252
                                val longitude = doc.getDouble("longitude") ?: 31.0335
                                val contactPhone = doc.getString("contactPhone") ?: "+263 242 700000"
                                val emergencyContact = doc.getString("emergencyContact") ?: "999 / +263 772 100 200"
                                val operatingHours = doc.getString("operatingHours") ?: "08:00 - 16:30 Mon-Fri"
                                val servicesOffered = doc.getString("servicesOffered") ?: "ZEPI Vaccines, Cold Chain Storage"
                                val coldChainStatus = doc.getString("coldChainStatus") ?: "Operational (Solar Powered)"
                                val totalChildren = doc.getLong("totalChildren")?.toInt() ?: 0
                                val efficiencyRate = doc.getDouble("efficiencyRate") ?: 90.0
                                val hasIntermittentInternet = doc.getBoolean("hasIntermittentInternet") ?: false

                                facilities.add(
                                    Clinic(
                                        id = localId,
                                        name = name,
                                        district = district,
                                        province = province,
                                        address = address,
                                        latitude = latitude,
                                        longitude = longitude,
                                        contactPhone = contactPhone,
                                        emergencyContact = emergencyContact,
                                        operatingHours = operatingHours,
                                        servicesOffered = servicesOffered,
                                        coldChainStatus = coldChainStatus,
                                        totalChildren = totalChildren,
                                        efficiencyRate = efficiencyRate,
                                        hasIntermittentInternet = hasIntermittentInternet,
                                        firestoreId = doc.id,
                                        lastCachedAt = System.currentTimeMillis(),
                                        isCachedLocally = true
                                    )
                                )
                            }
                            if (facilities.isNotEmpty()) {
                                dao.insertClinics(facilities)
                                onFacilitiesUpdated(facilities)
                            }
                        }
                    }
                }
            registrationsList.add(registration)
        } catch (e: Exception) {
            Log.d(TAG, "Live health facility listener skipped: ${e.message}")
        }
    }
}
