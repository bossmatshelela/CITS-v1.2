package com.matshelela.childimmunisationtrackingsystemappv1.viewmodel

import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.matshelela.childimmunisationtrackingsystemappv1.data.*
import com.matshelela.childimmunisationtrackingsystemappv1.util.NetworkConnectivityMonitor
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class UserRole {
    ADMIN_MOH,       // Ministry of Health Administrator
    FACILITY_ADMIN,  // Health Centre Administrator / Sister-in-Charge
    DOCTOR_CLINIC,   // Clinic Doctor / Nurse
    PARENT_GUARDIAN  // Mother / Parent / Guardian
}

sealed class AuthClaimStatus {
    object Idle : AuthClaimStatus()
    object Loading : AuthClaimStatus()
    data class Verified(
        val role: String,
        val mappedUserRole: UserRole,
        val email: String,
        val uid: String,
        val facilityId: String? = null,
        val expirationTime: Long = 0L,
        val isSimulatedDemo: Boolean = false
    ) : AuthClaimStatus()
    data class AccessDenied(
        val attemptedRole: UserRole,
        val requiredClaim: String,
        val actualClaim: String?,
        val message: String
    ) : AuthClaimStatus()
}

sealed class Screen {
    object RoleSelection : Screen()
    object Dashboard : Screen()
    data class ChildDetail(val childId: Int) : Screen()
    object SetupAssistant : Screen() // Custom smart scheduler / ZEPI immunization schedule advisor
    object SecureMessaging : Screen()
    object AnalyticsReporting : Screen()
    object EhrInteroperability : Screen()
    object ProblemFeedback : Screen()
    object AiAdvisorChat : Screen() // Gemini AI Immunization Chat & Voice Assistant
    object SystemDocumentation : Screen() // MoHCC Official System Architecture & Linking Guide
}

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = ImmunisationRepository(database.immunisationDao())
    private val firestoreSyncRepository = FirestoreSyncRepository(database.immunisationDao())
    private val networkConnectivityMonitor = NetworkConnectivityMonitor(application)

    // --- Active Configuration ---
    var currentRole by mutableStateOf(UserRole.PARENT_GUARDIAN)
    var currentScreen by mutableStateOf<Screen>(Screen.RoleSelection)
    var isNetworkOnline by mutableStateOf(true) // Intermittent connection simulator
    var viewDecryptedMode by mutableStateOf(true) // Toggle showing AES encrypted vs decrypted data (HIPAA Demo)

    // --- State Streams ---
    val childProfiles: StateFlow<List<ChildProfile>> = repository.allChildProfiles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val vaccinationRecords: StateFlow<List<VaccinationRecord>> = repository.allVaccinationRecords
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val appointments: StateFlow<List<Appointment>> = repository.allAppointments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val messages: StateFlow<List<ClinicMessage>> = repository.allMessages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val clinics: StateFlow<List<Clinic>> = repository.allClinics
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val doctorsNurses: StateFlow<List<DoctorNurse>> = repository.allDoctorsNurses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userFeedbacks: StateFlow<List<UserFeedback>> = repository.allUserFeedbacks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Currently Selected Contexts for UI Detail Views
    var selectedChildId by mutableStateOf<Int?>(null)

    // --- Health Facilities Offline Cache State & Controls ---
    var isFacilityCacheUpdating by mutableStateOf(false)
    var facilityCacheStatusMessage by mutableStateOf("Room Database Cache: Available Offline")
    var facilitySearchQuery by mutableStateOf("")
    var facilityDistrictFilter by mutableStateOf<String?>(null)

    // --- Input Form States ---
    // Add Child Form
    var childFormName by mutableStateOf("")
    var childFormBirthDate by mutableStateOf("2026-03-10")
    var childFormGender by mutableStateOf("Girl")
    var childFormMotherName by mutableStateOf("")
    var childFormPhone by mutableStateOf("+263 7")
    var childFormDistrict by mutableStateOf("Harare")
    var childFormClinicId by mutableStateOf(1)

    // Add Doctor/Nurse Form
    var doctorFormName by mutableStateOf("")
    var doctorFormRole by mutableStateOf("Doctor") // "Doctor" or "Nurse"
    var doctorFormPhone by mutableStateOf("+263 7")
    var doctorFormClinicId by mutableStateOf(1)
    var doctorFormDesignatorCode by mutableStateOf("")
    
    // Schedule Appointment Form
    var appDate by mutableStateOf("2026-06-30")
    var appTime by mutableStateOf("09:00 AM")
    var appSelectedVaccines by mutableStateOf(setOf<String>())
    var appNotes by mutableStateOf("")
    
    // Administer Dose Form (Doctor Portal)
    var doseAdministeredBy by mutableStateOf("Dr. Sustus Sibanda")
    var doseBatchNumber by mutableStateOf("ZEPI-PENTB-9844")
    var doseNotes by mutableStateOf("")

    // Messaging Form
    var chatInput by mutableStateOf("")

    // --- Security Pin Lock ---
    var adminPin by mutableStateOf("")
    var doctorPin by mutableStateOf("")
    var hasUnlockedRole by mutableStateOf(false)

    // --- Firebase ID Token & Custom Claims State ---
    var authClaimStatus by mutableStateOf<AuthClaimStatus>(
        AuthClaimStatus.Verified(
            role = "PARENT_GUARDIAN",
            mappedUserRole = UserRole.PARENT_GUARDIAN,
            email = "parent.guardian@health.gov.zw",
            uid = "zw_auth_parent_001",
            isSimulatedDemo = true
        )
    )
    var currentVerifiedClaimRole by mutableStateOf<String>("PARENT_GUARDIAN")
    var verifiedEmail by mutableStateOf<String>("parent.guardian@health.gov.zw")
    var verifiedUid by mutableStateOf<String>("zw_auth_parent_001")
    var tokenIssuedAt by mutableStateOf<Long>(System.currentTimeMillis())
    var isSimulatedClaim by mutableStateOf<Boolean>(true)
    var authGateErrorMessage by mutableStateOf<String>("")
    var isClaimsRefreshing by mutableStateOf<Boolean>(false)

    // --- Offline Synchronisation Execution ---
    var syncInProgress by mutableStateOf(false)
    var lastSyncResult by mutableStateOf("")

    // --- EHR interoperability actions ---
    var ehrImportJsonInput by mutableStateOf("")
    var ehrSyncStatusMessage by mutableStateOf("")

    // --- User Feedback Operations ---
    var feedbackFormRating by mutableStateOf(5)
    var feedbackFormTag by mutableStateOf("App Bugs")
    var feedbackFormComment by mutableStateOf("")
    var feedbackFormSubmitterName by mutableStateOf("")
    var feedbackSubmitStatusMessage by mutableStateOf("")

    // --- Facility Admin Intake & Clinician Pairing ---
    var facilityChildName by mutableStateOf("")
    var facilityChildBirthDate by mutableStateOf("2026-03-15")
    var facilityChildGender by mutableStateOf("Girl")
    var facilityChildMotherName by mutableStateOf("")
    var facilityChildPhone by mutableStateOf("+263 7")
    var facilityChildDistrict by mutableStateOf("Bulawayo")
    var facilityChildClinicId by mutableStateOf(2)
    var facilityChildDoctorId by mutableStateOf<Int?>(1)
    var facilityChildHealthChallenges by mutableStateOf("")
    var facilityChildBloodGroup by mutableStateOf("O+")
    var facilityChildBirthWeight by mutableStateOf("3.2 kg")
    var lastGeneratedAccessCode by mutableStateOf("")
    var facilityActionMessage by mutableStateOf("")

    // --- Parent Portal Code Linking ---
    var parentLinkingCodeInput by mutableStateOf("")
    var parentLinkingMessage by mutableStateOf("")
    var parentLinkingIsError by mutableStateOf(false)
    var showParentLinkDialog by mutableStateOf(false)

    // --- Clinical Lifelong Health Challenges Editor ---
    var editingHealthChallengesText by mutableStateOf("")
    var healthChallengesUpdateMessage by mutableStateOf("")

    init {
        try {
            if (com.google.firebase.FirebaseApp.getApps(application).isEmpty()) {
                com.google.firebase.FirebaseApp.initializeApp(application)
            }
        } catch (e: Exception) {
            Log.w("AppViewModel", "FirebaseApp init warning: ${e.message}")
        }

        // Run database prepopulation asynchronously
        viewModelScope.launch {
            repository.verifyAndPrepopulate()
            // Default to selected child being the newly created mock baby if any
            childProfiles.collect { list ->
                if (list.isNotEmpty() && selectedChildId == null) {
                    selectedChildId = list.firstOrNull { it.name.contains("Ruvarashe") }?.id ?: list.first().id
                }
                // Once profiles are ready, restart listener with actual loaded child list
                startRealtimeFirestoreChatListener()
            }
        }
        initializeAndRegisterE2EE()
        fetchAndCacheHealthFacilities(manualTrigger = false)
        listenToHealthFacilitiesUpdates()
        observeDeviceNetworkState()

        try {
            val auth = FirebaseAuth.getInstance()
            auth.addAuthStateListener { firebaseAuth ->
                val user = firebaseAuth.currentUser
                if (user != null) {
                    refreshIdTokenClaims()
                }
            }
        } catch (e: Exception) {
            Log.w("AppViewModel", "FirebaseAuth init non-fatal: ${e.message}")
        }
    }

    private fun observeDeviceNetworkState() {
        viewModelScope.launch {
            networkConnectivityMonitor.isConnectedFlow.collect { connected ->
                isNetworkOnline = connected
                if (connected) {
                    try {
                        // Automatically sync pending offline records in background
                        triggerOfflineSynchronisation()
                        fetchAndCacheHealthFacilities(manualTrigger = false)
                    } catch (e: Exception) {
                        Log.e("AppViewModel", "Network state observation sync error: ${e.message}")
                    }
                }
            }
        }
    }

    fun fetchAndCacheHealthFacilities(manualTrigger: Boolean = false) {
        viewModelScope.launch {
            try {
                isFacilityCacheUpdating = true
                val result = firestoreSyncRepository.fetchAndCacheHealthFacilities()
                val cachedCount = result.getOrDefault(0)
                isFacilityCacheUpdating = false
                facilityCacheStatusMessage = if (cachedCount > 0) {
                    "Cached $cachedCount facilities from Firestore into Room Database"
                } else {
                    "Room Database Cache Active (Offline Mode Safe)"
                }
            } catch (e: Exception) {
                isFacilityCacheUpdating = false
                facilityCacheStatusMessage = "Room Database Cache Active (Offline Mode Safe)"
            }
        }
    }

    private fun listenToHealthFacilitiesUpdates() {
        firestoreSyncRepository.startRealTimeHealthFacilitiesListener { updatedList ->
            Log.d("AppViewModel", "Received live health facility updates: ${updatedList.size} cached locally.")
        }
    }

    fun initializeAndRegisterE2EE() {
        viewModelScope.launch {
            try {
                // Generate hardware-backed RSA keypair locally on device
                val publicKey = E2EEncryptionHelper.generateHardwareKeyPair("e2ee_local_key")
                if (publicKey != null) {
                    val pubKeyBase64 = E2EEncryptionHelper.getPublicKeyBase64("e2ee_local_key")
                    if (pubKeyBase64 != null) {
                        // Upload public key to Firestore for key distribution
                        when (currentRole) {
                            UserRole.DOCTOR_CLINIC -> {
                                firestoreSyncRepository.uploadPublicKey("Doctor", pubKeyBase64)
                            }
                            UserRole.PARENT_GUARDIAN -> {
                                // Fetch currently loaded children non-destructively
                                val children = repository.allChildProfiles.first()
                                children.forEach { child ->
                                    if (child.healthPin.isNotEmpty()) {
                                        firestoreSyncRepository.uploadPublicKey(child.healthPin, pubKeyBase64)
                                    }
                                }
                            }
                            else -> {}
                        }
                    }
                }
            } catch (e: Exception) {
                if (e !is kotlinx.coroutines.CancellationException) {
                    Log.d("AppViewModel", "E2EE Key Registration deferred: ${e.message}")
                }
            }
        }
    }

    fun startRealtimeFirestoreChatListener() {
        viewModelScope.launch {
            val children = childProfiles.value
            val roleStr = when (currentRole) {
                UserRole.PARENT_GUARDIAN -> "Parent"
                UserRole.DOCTOR_CLINIC -> "Doctor"
                UserRole.FACILITY_ADMIN -> "Facility Admin"
                UserRole.ADMIN_MOH -> "MoH Admin"
            }
            firestoreSyncRepository.startRealTimeMessageListener(
                currentRole = roleStr,
                childList = children,
                onNewMessagesAdded = { messages ->
                    messages.forEach { msg ->
                        repository.insertMessage(msg)
                    }
                }
            )
        }
    }

    fun stopRealtimeFirestoreChatListener() {
        firestoreSyncRepository.stopRealTimeMessageListener()
    }

    override fun onCleared() {
        super.onCleared()
        stopRealtimeFirestoreChatListener()
    }

    // --- Firebase Custom Claims Verification Methods ---

    fun isRoleAuthorized(role: UserRole): Boolean {
        val claim = currentVerifiedClaimRole
        return when (role) {
            UserRole.ADMIN_MOH -> claim == "MOH_ADMIN"
            UserRole.FACILITY_ADMIN -> claim == "FACILITY_ADMIN" || claim == "MOH_ADMIN"
            UserRole.DOCTOR_CLINIC -> claim == "DOCTOR_NURSE" || claim == "MOH_ADMIN"
            UserRole.PARENT_GUARDIAN -> claim == "PARENT_GUARDIAN" || claim == "PARENT" || claim == "MOH_ADMIN"
        }
    }

    fun getRequiredClaimForRole(role: UserRole): String {
        return when (role) {
            UserRole.ADMIN_MOH -> "MOH_ADMIN"
            UserRole.FACILITY_ADMIN -> "FACILITY_ADMIN"
            UserRole.DOCTOR_CLINIC -> "DOCTOR_NURSE"
            UserRole.PARENT_GUARDIAN -> "PARENT_GUARDIAN"
        }
    }

    fun mapClaimToUserRole(claim: String): UserRole {
        return when (claim) {
            "MOH_ADMIN" -> UserRole.ADMIN_MOH
            "FACILITY_ADMIN" -> UserRole.FACILITY_ADMIN
            "DOCTOR_NURSE" -> UserRole.DOCTOR_CLINIC
            else -> UserRole.PARENT_GUARDIAN
        }
    }

    fun refreshIdTokenClaims(onComplete: ((Boolean) -> Unit)? = null) {
        isClaimsRefreshing = true
        authGateErrorMessage = ""
        try {
            val auth = FirebaseAuth.getInstance()
            val currentUser = auth.currentUser
            if (currentUser != null) {
                currentUser.getIdToken(true).addOnCompleteListener { task ->
                    isClaimsRefreshing = false
                    if (task.isSuccessful) {
                        val tokenResult = task.result
                        val roleClaim = tokenResult?.claims?.get("role") as? String ?: "PARENT_GUARDIAN"
                        currentVerifiedClaimRole = roleClaim
                        verifiedEmail = currentUser.email ?: "staff.authenticated@mohcc.gov.zw"
                        verifiedUid = currentUser.uid
                        tokenIssuedAt = (tokenResult?.issuedAtTimestamp ?: 0L) * 1000L
                        if (tokenIssuedAt == 0L) tokenIssuedAt = System.currentTimeMillis()
                        isSimulatedClaim = false
                        authClaimStatus = AuthClaimStatus.Verified(
                            role = roleClaim,
                            mappedUserRole = mapClaimToUserRole(roleClaim),
                            email = verifiedEmail,
                            uid = verifiedUid,
                            expirationTime = (tokenResult?.expirationTimestamp ?: 0L) * 1000L,
                            isSimulatedDemo = false
                        )
                        onComplete?.invoke(true)
                    } else {
                        authGateErrorMessage = "Could not refresh Firebase ID token: ${task.exception?.localizedMessage}"
                        onComplete?.invoke(false)
                    }
                }
            } else {
                isClaimsRefreshing = false
                tokenIssuedAt = System.currentTimeMillis()
                onComplete?.invoke(true)
            }
        } catch (e: Exception) {
            isClaimsRefreshing = false
            tokenIssuedAt = System.currentTimeMillis()
            onComplete?.invoke(true)
        }
    }

    fun setSimulatedClaim(claimRole: String, email: String, uid: String) {
        currentVerifiedClaimRole = claimRole
        verifiedEmail = email
        verifiedUid = uid
        tokenIssuedAt = System.currentTimeMillis()
        isSimulatedClaim = true
        authGateErrorMessage = ""
        val mapped = mapClaimToUserRole(claimRole)
        authClaimStatus = AuthClaimStatus.Verified(
            role = claimRole,
            mappedUserRole = mapped,
            email = email,
            uid = uid,
            expirationTime = System.currentTimeMillis() + 3600000L,
            isSimulatedDemo = true
        )
        currentRole = mapped
        if (mapped == UserRole.PARENT_GUARDIAN) {
            hasUnlockedRole = true
        } else {
            hasUnlockedRole = false
        }
    }

    fun attemptUnlock(pin: String): Boolean {
        authGateErrorMessage = ""
        val requiredClaim = getRequiredClaimForRole(currentRole)

        // 1. Strict Claim Verification: Check if active Firebase ID token possesses required claim
        if (!isRoleAuthorized(currentRole)) {
            authGateErrorMessage = "ACCESS RESTRICTED: Firebase ID token possesses claim '$currentVerifiedClaimRole'. Access to ${currentRole.name} requires the cryptographically signed '$requiredClaim' claim."
            authClaimStatus = AuthClaimStatus.AccessDenied(
                attemptedRole = currentRole,
                requiredClaim = requiredClaim,
                actualClaim = currentVerifiedClaimRole,
                message = authGateErrorMessage
            )
            return false
        }

        // 2. Operational PIN verification for facility staff
        val success = if (currentRole == UserRole.ADMIN_MOH && pin == "2026") {
            hasUnlockedRole = true
            currentScreen = Screen.Dashboard
            true
        } else if (currentRole == UserRole.FACILITY_ADMIN && (pin == "7788" || pin == "4321")) {
            hasUnlockedRole = true
            currentScreen = Screen.Dashboard
            true
        } else if (currentRole == UserRole.DOCTOR_CLINIC && pin == "1234") {
            hasUnlockedRole = true
            currentScreen = Screen.Dashboard
            true
        } else if (currentRole == UserRole.PARENT_GUARDIAN) {
            hasUnlockedRole = true
            currentScreen = Screen.Dashboard
            true
        } else {
            authGateErrorMessage = "Claim '$currentVerifiedClaimRole' verified, but entered Operational PIN is invalid."
            false
        }

        if (success) {
            initializeAndRegisterE2EE()
        }
        return success
    }

    fun selectRole(role: UserRole) {
        currentRole = role
        authGateErrorMessage = ""
        if (role == UserRole.PARENT_GUARDIAN) {
            if (isRoleAuthorized(UserRole.PARENT_GUARDIAN)) {
                hasUnlockedRole = true
                currentScreen = Screen.Dashboard
            } else {
                hasUnlockedRole = false
                currentScreen = Screen.RoleSelection
                authGateErrorMessage = "Access Denied: Missing PARENT_GUARDIAN claim."
            }
        } else {
            hasUnlockedRole = false
            currentScreen = Screen.RoleSelection
        }
        startRealtimeFirestoreChatListener()
        initializeAndRegisterE2EE()
    }

    // --- Child Profile Operations ---
    fun registerChildChildProfile() {
        viewModelScope.launch {
            val childId = repository.insertChildProfile(
                ChildProfile(
                    name = childFormName.ifBlank { "Baby Boy/Girl" },
                    birthDate = childFormBirthDate,
                    gender = childFormGender,
                    motherName = childFormMotherName.ifBlank { "Guardian" },
                    contactPhone = childFormPhone,
                    healthPin = "ZW-MOH-2026-${(1000..9999).random()}",
                    district = childFormDistrict,
                    clinicId = childFormClinicId,
                    complianceStatus = "Due"
                )
            )
            repository.createZEPISchedule(childId, childFormBirthDate)
            
            // Increment child counts in clinics
            val clinicList = clinics.value
            val mainClinic = clinicList.find { it.id == childFormClinicId }
            if (mainClinic != null) {
                repository.updateClinic(mainClinic.copy(totalChildren = mainClinic.totalChildren + 1))
            }

            // Reset Form Fields
            childFormName = ""
            childFormMotherName = ""
            childFormPhone = "+263 7"
            currentScreen = Screen.Dashboard
        }
    }

    // --- Facility Admin Intake & Clinician Pairing ---
    fun facilityAdminRegisterChild() {
        viewModelScope.launch {
            val doctor = doctorsNurses.value.find { it.id == facilityChildDoctorId }
            val doctorName = doctor?.name ?: "Dr. Sibusiso Sibanda"
            val healthPin = "ZW-MOH-2026-${(1000..9999).random()}"
            val uniqueCode = "ZW-LINK-${(1000..9999).random()}"

            val child = ChildProfile(
                name = facilityChildName.ifBlank { "Baby Boy/Girl" },
                birthDate = facilityChildBirthDate,
                gender = facilityChildGender,
                motherName = facilityChildMotherName.ifBlank { "Guardian" },
                contactPhone = facilityChildPhone,
                healthPin = healthPin,
                district = facilityChildDistrict,
                clinicId = facilityChildClinicId,
                complianceStatus = "Due",
                accessCode = uniqueCode,
                assignedDoctorId = facilityChildDoctorId,
                assignedDoctorName = doctorName,
                healthChallenges = facilityChildHealthChallenges.ifBlank { "No acute neonatal abnormalities noted at birth. Routine growth monitoring advised." },
                isLinkedToParent = false,
                bloodGroup = facilityChildBloodGroup,
                birthWeight = facilityChildBirthWeight
            )

            val childId = repository.insertChildProfile(child)
            repository.createZEPISchedule(childId, facilityChildBirthDate)

            // Increment clinic count
            val clinic = clinics.value.find { it.id == facilityChildClinicId }
            if (clinic != null) {
                repository.updateClinic(clinic.copy(totalChildren = clinic.totalChildren + 1))
            }

            // Sync with Firestore Cloud immediately
            firestoreSyncRepository.syncChildProfiles()
            firestoreSyncRepository.syncVaccinationRecords()

            lastGeneratedAccessCode = uniqueCode
            facilityActionMessage = "Infant registered successfully! Assigned Clinician: $doctorName. Generated Child Access Code: $uniqueCode (Provide this to mother/guardian to link on their portal)."

            // Reset input fields
            facilityChildName = ""
            facilityChildMotherName = ""
            facilityChildPhone = "+263 7"
            facilityChildHealthChallenges = ""
        }
    }

    fun connectDoctorToChild(childId: Int, doctorId: Int) {
        viewModelScope.launch {
            val child = childProfiles.value.find { it.id == childId } ?: return@launch
            val doctor = doctorsNurses.value.find { it.id == doctorId } ?: return@launch
            val updated = child.copy(
                assignedDoctorId = doctor.id,
                assignedDoctorName = doctor.name,
                isSynced = false
            )
            repository.updateChildProfile(updated)
            firestoreSyncRepository.syncChildProfiles()
            facilityActionMessage = "Connected ${doctor.name} to patient ${child.name} (${child.healthPin})."
        }
    }

    fun updateChildHealthChallenges(childId: Int, newChallenges: String) {
        viewModelScope.launch {
            val child = childProfiles.value.find { it.id == childId } ?: return@launch
            val updated = child.copy(
                healthChallenges = newChallenges,
                isSynced = false
            )
            repository.updateChildProfile(updated)
            firestoreSyncRepository.syncChildProfiles()
            healthChallengesUpdateMessage = "Lifelong child health record & clinical challenges saved to cloud for ${child.name}."
        }
    }

    fun regenerateAccessCode(childId: Int) {
        viewModelScope.launch {
            val child = childProfiles.value.find { it.id == childId } ?: return@launch
            val newCode = "ZW-LINK-${(1000..9999).random()}"
            val updated = child.copy(accessCode = newCode, isSynced = false)
            repository.updateChildProfile(updated)
            firestoreSyncRepository.syncChildProfiles()
            lastGeneratedAccessCode = newCode
            facilityActionMessage = "Generated new Access Code: $newCode for ${child.name}."
        }
    }

    // --- Parent Portal Code Linking Operations ---
    fun linkChildWithCode(code: String) {
        val trimmedCode = code.trim().uppercase()
        if (trimmedCode.isBlank()) {
            parentLinkingMessage = "Please enter the access code provided by your clinic."
            parentLinkingIsError = true
            return
        }

        viewModelScope.launch {
            val child = repository.getChildProfileByAccessCode(trimmedCode)
            if (child != null) {
                val updated = child.copy(isLinkedToParent = true, isSynced = false)
                repository.updateChildProfile(updated)
                selectedChildId = child.id
                firestoreSyncRepository.syncChildProfiles()
                parentLinkingIsError = false
                parentLinkingMessage = "Child linked successfully! Road to Health Card unlocked for ${child.name} (${child.healthPin}). Assigned Clinician: ${child.assignedDoctorName}."
                parentLinkingCodeInput = ""
                showParentLinkDialog = false
            } else {
                parentLinkingIsError = true
                parentLinkingMessage = "Code '$trimmedCode' not found. Please verify the code on your clinic linking slip or ask your Facility Sister-in-Charge."
            }
        }
    }

    fun deleteChildProfile(child: ChildProfile) {
        viewModelScope.launch {
            repository.deleteChildProfile(child)
            // If the deleted child was selected, clear selectedChildId
            if (selectedChildId == child.id) {
                selectedChildId = null
            }
        }
    }

    fun registerDoctorNurse() {
        viewModelScope.launch {
            val code = doctorFormDesignatorCode.ifBlank {
                val prefix = if (doctorFormRole == "Doctor") "MOH-DOC" else "MOH-NRS"
                "$prefix-${(1000..9999).random()}"
            }
            repository.insertDoctorNurse(
                DoctorNurse(
                    name = doctorFormName.ifBlank { "Unknown Clinician" },
                    role = doctorFormRole,
                    contactNo = doctorFormPhone,
                    clinicId = doctorFormClinicId,
                    designatorCode = code
                )
            )
            // Reset form fields
            doctorFormName = ""
            doctorFormPhone = "+263 7"
            doctorFormDesignatorCode = ""
        }
    }

    fun removeDoctorNurse(person: DoctorNurse) {
        viewModelScope.launch {
            repository.deleteDoctorNurse(person)
        }
    }

    // --- Appointment Operations ---
    fun scheduleAppointment(childId: Int) {
        viewModelScope.launch {
            val selectedProfile = childProfiles.value.find { it.id == childId } ?: return@launch
            val appVaccines = appSelectedVaccines.joinToString(", ")
            
            repository.insertAppointment(
                Appointment(
                    childId = childId,
                    clinicId = selectedProfile.clinicId,
                    vaccineNames = appVaccines.ifBlank { "Booster Review" },
                    appointmentDate = appDate,
                    appointmentTime = appTime,
                    status = "Pending Approval",
                    isSynced = isNetworkOnline,
                    notes = appNotes.ifBlank { null }
                )
            )
            
            // Send automatic appointment notification to self
            sendSystemNotification(
                "Appointment Scheduled!",
                "Requested visit for ${selectedProfile.name} on $appDate for [$appVaccines] is pending provider approval.",
                childId
            )
            
            // Clear fields
            appSelectedVaccines = emptySet()
            appNotes = ""
            currentScreen = Screen.Dashboard
        }
    }

    fun approveAppointment(appointment: Appointment) {
        viewModelScope.launch {
            repository.updateAppointment(
                appointment.copy(
                    status = "Approved",
                    isSynced = isNetworkOnline
                )
            )
            
            val child = childProfiles.value.find { it.id == appointment.childId } ?: return@launch
            sendSystemNotification(
                "Appointment Approved! 🗓️",
                "Dr. Sibanda approved appointment for ${child.name} on ${appointment.appointmentDate} at ${appointment.appointmentTime}.",
                appointment.childId
            )
        }
    }

    fun cancelAppointment(appointment: Appointment) {
        viewModelScope.launch {
            repository.updateAppointment(
                appointment.copy(
                    status = "Cancelled",
                    isSynced = isNetworkOnline
                )
            )
        }
    }

    fun completeAndAdministerAppointment(appointment: Appointment, nurseName: String, batch: String, feedbackNote: String) {
        viewModelScope.launch {
            // Mark vaccines administered
            val childId = appointment.childId
            val child = childProfiles.value.find { it.id == childId } ?: return@launch
            val targetVaccineNames = appointment.vaccineNames.split(", ").map { it.trim() }

            val childRecords = vaccinationRecords.value.filter { it.childId == childId }
            for (record in childRecords) {
                // Approximate match check, e.g. "Pentavalent 1" -> matches Pentavalent dose 1
                val matches = targetVaccineNames.any { target -> 
                    target.startsWith(record.vaccineName, ignoreCase = true) && 
                    (target.contains(record.doseNumber.toString()) || record.vaccineName == "BCG") 
                }
                if (matches && record.administeredDate == null) {
                    repository.updateVaccinationRecord(
                        record.copy(
                            administeredDate = appointment.appointmentDate,
                            administeredBy = nurseName,
                            batchNumber = EncryptionHelper.encrypt(batch),
                            facilityName = clinics.value.find { it.id == appointment.clinicId }?.name ?: "MoH Zimbabwe Clinic",
                            notes = if (feedbackNote.isNotBlank()) EncryptionHelper.encrypt(feedbackNote) else null,
                            isSynced = isNetworkOnline
                        )
                    )
                }
            }

            // Update Appointment Status
            repository.updateAppointment(
                appointment.copy(
                    status = "Completed",
                    isSynced = isNetworkOnline
                )
            )

            // Recalculate child's compliance status
            updateChildComplianceStatus(childId)

            sendSystemNotification(
                "Vaccines Administered ✅",
                "${child.name} successfully received [${appointment.vaccineNames}]. Digital record updated and encrypted.",
                childId
            )
        }
    }

    fun administerVaccineRecord(record: VaccinationRecord, administeredDate: String, nurseName: String, batch: String, feedbackNote: String, clinicName: String) {
        viewModelScope.launch {
            repository.updateVaccinationRecord(
                record.copy(
                    administeredDate = administeredDate,
                    administeredBy = nurseName,
                    batchNumber = EncryptionHelper.encrypt(batch),
                    facilityName = clinicName.ifBlank { "MoH Zimbabwe Clinic" },
                    notes = if (feedbackNote.isNotBlank()) EncryptionHelper.encrypt(feedbackNote) else null,
                    isSynced = isNetworkOnline
                )
            )
            // Recalculate child's compliance status
            updateChildComplianceStatus(record.childId)

            val child = childProfiles.value.find { it.id == record.childId }
            sendSystemNotification(
                "Vaccine Administered ✅",
                "${child?.name ?: "Child"} received ${record.vaccineName} (Dose ${record.doseNumber}). Digital record updated and encrypted.",
                record.childId
            )
        }
    }

    private suspend fun updateChildComplianceStatus(childId: Int) {
        val child = childProfiles.value.find { it.id == childId } ?: return
        val records = vaccinationRecords.value.filter { it.childId == childId }
        
        // Let's implement real compliance rules:
        // A child is Compliant if they have no pending vaccine records that are overdue
        // For simplicity, let's look at the counts:
        val pendingCount = records.count { it.administeredDate == null }
        val status = if (pendingCount == 0) "Compliant" else "Due"
        
        repository.updateChildProfile(child.copy(complianceStatus = status))
    }

    // --- Message Exchange (HIPAA Secures Messaging) ---
    fun sendClinicMessage(childId: Int?, recipientRole: String) {
        if (chatInput.isBlank()) return
        viewModelScope.launch {
            val senderName = when (currentRole) {
                UserRole.ADMIN_MOH -> "MoH Administrator"
                UserRole.FACILITY_ADMIN -> "Sister-in-Charge / Facility Admin"
                UserRole.DOCTOR_CLINIC -> "Dr. Sibusiso Sibanda"
                UserRole.PARENT_GUARDIAN -> {
                    val child = childProfiles.value.find { it.id == childId }
                    child?.motherName ?: "Parent"
                }
            }
            
            repository.insertMessage(
                ClinicMessage(
                    childId = childId,
                    senderName = senderName,
                    senderRole = when (currentRole) {
                        UserRole.ADMIN_MOH -> "MoH Admin"
                        UserRole.FACILITY_ADMIN -> "Facility Admin"
                        UserRole.DOCTOR_CLINIC -> "Doctor"
                        UserRole.PARENT_GUARDIAN -> "Parent"
                    },
                    content = EncryptionHelper.encrypt(chatInput),
                    timestamp = System.currentTimeMillis(),
                    recipientRole = recipientRole,
                    isSynced = false
                )
            )
            
            chatInput = ""
        }
    }

    // --- Trigger System Notifications (Reminders & Booster alerts) ---
    private fun sendSystemNotification(title: String, content: String, childId: Int) {
        viewModelScope.launch {
            // Log as system-generated message so it triggers a push visual on screen
            repository.insertMessage(
                ClinicMessage(
                    childId = childId,
                    senderName = "System Alert (MoH Zimbabwe)",
                    senderRole = "MoH Admin",
                    content = EncryptionHelper.encrypt("$title\n$content"),
                    timestamp = System.currentTimeMillis(),
                    recipientRole = "Parent",
                    isSynced = false
                )
            )
        }
    }

    // --- Offline Synchronisation Execution ---
    fun triggerOfflineSynchronisation() {
        if (syncInProgress) return
        viewModelScope.launch {
            try {
                syncInProgress = true
                firestoreSyncRepository.syncAllUnsyncedRecords()
                val syncCount = repository.syncOfflineRecords()
                lastSyncResult = if (syncCount > 0) {
                    "Synchronised $syncCount records and updated health facilities in local Room cache."
                } else {
                    "Room Database cache verified and synced with Zimbabwe Health Cloud."
                }
            } catch (e: Exception) {
                Log.e("AppViewModel", "Offline synchronization failed: ${e.message}", e)
                lastSyncResult = "Room Database cache verified and active offline."
            } finally {
                syncInProgress = false
            }
        }
    }

    // --- EHR interoperability actions ---
    fun importEhrRecord() {
        viewModelScope.launch {
            val success = repository.importFromEhrFhirJson(ehrImportJsonInput)
            ehrSyncStatusMessage = if (success) {
                ehrImportJsonInput = ""
                "Successfully ingested FHIR Bundle! Child profile added and ZEPI immunization timeline generated."
            } else {
                "Error: Invalid FHIR format. Please verify JSON contains Patient resource fields (family, given, birthDate)."
            }
        }
    }
    
    fun getEhrFhirTemplateCode(): String {
        return """
        {
          "resourceType": "Bundle",
          "id": "fhir-patient-zw-2026",
          "type": "transaction",
          "entry": [
            {
              "resource": {
                "resourceType": "Patient",
                "identifier": [{ "value": "ZW-MOH-2026-EH73" }],
                "name": [{ "family": "Nkomo", "given": [ "Mthokozisi" ] }],
                "gender": "male",
                "birthDate": "2026-03-05"
              }
            }
          ]
        }
        """.trimIndent()
    }

    // --- User Feedback Operations ---
    fun submitFeedback() {
        if (feedbackFormComment.isBlank()) {
            feedbackSubmitStatusMessage = "Please describe the problem or feedback!"
            return
        }
        viewModelScope.launch {
            val roleName = when (currentRole) {
                UserRole.ADMIN_MOH -> "MoH Admin"
                UserRole.FACILITY_ADMIN -> "Facility Admin / Sister-in-Charge"
                UserRole.DOCTOR_CLINIC -> "Doctor / Clinic Staff"
                UserRole.PARENT_GUARDIAN -> "Parent / Guardian"
            }
            val submitter = if (feedbackFormSubmitterName.isBlank()) {
                when (currentRole) {
                    UserRole.ADMIN_MOH -> "Ministry Registrar"
                    UserRole.FACILITY_ADMIN -> "Sister-in-Charge"
                    UserRole.DOCTOR_CLINIC -> "Dr. Sibusiso Sibanda"
                    UserRole.PARENT_GUARDIAN -> "Parent"
                }
            } else {
                feedbackFormSubmitterName
            }

            repository.insertUserFeedback(
                UserFeedback(
                    submitterName = submitter,
                    submitterRole = roleName,
                    ratingsStars = feedbackFormRating,
                    problemTag = feedbackFormTag,
                    description = feedbackFormComment,
                    timestamp = System.currentTimeMillis()
                )
            )

            // Reset form
            feedbackFormRating = 5
            feedbackFormComment = ""
            feedbackFormSubmitterName = ""
            feedbackSubmitStatusMessage = "Thank you! Your feedback and problem report have been saved in the system database."
        }
    }

    fun updateFeedbackStatus(feedback: UserFeedback, newStatus: String) {
        viewModelScope.launch {
            repository.updateUserFeedback(feedback.copy(resolutionStatus = newStatus))
        }
    }

    fun deleteFeedback(feedback: UserFeedback) {
        viewModelScope.launch {
            repository.deleteUserFeedback(feedback)
        }
    }
}
