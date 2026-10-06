package com.matshelela.childimmunisationtrackingsystemappv1.data

import com.google.firebase.firestore.DocumentId

/**
 * Firestore data model representation for child_profiles collection.
 * Path: /child_profiles/{healthPin}
 */
data class FirestoreChildProfile(
    @DocumentId val healthPin: String = "",
    val name: String = "",
    val birthDate: String = "",
    val gender: String = "",
    val motherName: String = "",
    val contactPhone: String = "",
    val district: String = "",
    val clinicId: Int = 0,
    val complianceStatus: String = "Due"
) {
    // Helper mapper from Room model to Firestore
    constructor(roomModel: ChildProfile) : this(
        healthPin = roomModel.healthPin,
        name = roomModel.name,
        birthDate = roomModel.birthDate,
        gender = roomModel.gender,
        motherName = roomModel.motherName,
        contactPhone = roomModel.contactPhone,
        district = roomModel.district,
        clinicId = roomModel.clinicId,
        complianceStatus = roomModel.complianceStatus
    )

    fun toMap(): Map<String, Any?> {
        return mapOf(
            "healthPin" to healthPin,
            "name" to name,
            "birthDate" to birthDate,
            "gender" to gender,
            "motherName" to motherName,
            "contactPhone" to contactPhone,
            "district" to district,
            "clinicId" to clinicId,
            "complianceStatus" to complianceStatus
        )
    }
}

/**
 * Firestore data model representation for vaccination records.
 * Path: /child_profiles/{healthPin}/vaccination_records/{vaccineName_doseNumber}
 */
data class FirestoreVaccinationRecord(
    val childHealthPin: String = "",
    val vaccineName: String = "",
    val doseNumber: Int = 0,
    val scheduledDate: String = "",
    val administeredDate: String? = null,
    val administeredBy: String? = null,
    val batchNumber: String? = null,
    val facilityName: String? = null,
    val notes: String? = null
) {
    constructor(roomModel: VaccinationRecord, childHealthPin: String) : this(
        childHealthPin = childHealthPin,
        vaccineName = roomModel.vaccineName,
        doseNumber = roomModel.doseNumber,
        scheduledDate = roomModel.scheduledDate,
        administeredDate = roomModel.administeredDate,
        administeredBy = roomModel.administeredBy,
        batchNumber = roomModel.batchNumber,
        facilityName = roomModel.facilityName,
        notes = roomModel.notes
    )

    fun toMap(): Map<String, Any?> {
        return mapOf(
            "childHealthPin" to childHealthPin,
            "vaccineName" to vaccineName,
            "doseNumber" to doseNumber,
            "scheduledDate" to scheduledDate,
            "administeredDate" to administeredDate,
            "administeredBy" to administeredBy,
            "batchNumber" to batchNumber,
            "facilityName" to facilityName,
            "notes" to notes
        )
    }
}

/**
 * Firestore data model representation for appointments collection.
 * Path: /appointments/{appointmentId}
 */
data class FirestoreAppointment(
    @DocumentId val id: String = "",
    val childHealthPin: String = "",
    val clinicId: Int = 0,
    val vaccineNames: String = "",
    val appointmentDate: String = "",
    val appointmentTime: String = "",
    val status: String = "Pending Approval",
    val notes: String? = null
) {
    constructor(roomModel: Appointment, childHealthPin: String) : this(
        id = roomModel.id.toString(),
        childHealthPin = childHealthPin,
        clinicId = roomModel.clinicId,
        vaccineNames = roomModel.vaccineNames,
        appointmentDate = roomModel.appointmentDate,
        appointmentTime = roomModel.appointmentTime,
        status = roomModel.status,
        notes = roomModel.notes
    )

    fun toMap(): Map<String, Any?> {
        return mapOf(
            "childHealthPin" to childHealthPin,
            "clinicId" to clinicId,
            "vaccineNames" to vaccineNames,
            "appointmentDate" to appointmentDate,
            "appointmentTime" to appointmentTime,
            "status" to status,
            "notes" to notes
        )
    }
}

/**
 * Firestore data model representation for clinic messaging / chats.
 * Path: /chats/{chatId}/messages/{messageId}
 */
data class FirestoreChatMessage(
    @DocumentId val id: String = "",
    val childHealthPin: String? = null,
    val senderName: String = "",
    val senderRole: String = "",
    val content: String = "",
    val timestamp: Long = 0L,
    val isEncrypted: Boolean = true,
    val recipientRole: String = "",
    val isE2EE: Boolean = false,
    val encryptedAesKey: String? = null
) {
    constructor(roomModel: ClinicMessage, childHealthPin: String?) : this(
        id = roomModel.id.toString(),
        childHealthPin = childHealthPin,
        senderName = roomModel.senderName,
        senderRole = roomModel.senderRole,
        content = roomModel.content,
        timestamp = roomModel.timestamp,
        isEncrypted = roomModel.isEncrypted,
        recipientRole = roomModel.recipientRole,
        isE2EE = roomModel.isE2EE,
        encryptedAesKey = roomModel.encryptedAesKey
    )

    fun toMap(): Map<String, Any?> {
        return mapOf(
            "childHealthPin" to childHealthPin,
            "senderName" to senderName,
            "senderRole" to senderRole,
            "content" to content,
            "timestamp" to timestamp,
            "isEncrypted" to isEncrypted,
            "recipientRole" to recipientRole,
            "isE2EE" to isE2EE,
            "encryptedAesKey" to encryptedAesKey
        )
    }
}

/**
 * Firestore data model representation for user feedback.
 * Path: /feedback/{feedbackId}
 */
data class FirestoreFeedback(
    @DocumentId val id: String = "",
    val submitterName: String = "",
    val submitterRole: String = "",
    val ratingsStars: Int = 5,
    val problemTag: String = "",
    val description: String = "",
    val timestamp: Long = 0L,
    val resolutionStatus: String = "Pending Review"
) {
    constructor(roomModel: UserFeedback) : this(
        id = roomModel.id.toString(),
        submitterName = roomModel.submitterName,
        submitterRole = roomModel.submitterRole,
        ratingsStars = roomModel.ratingsStars,
        problemTag = roomModel.problemTag,
        description = roomModel.description,
        timestamp = roomModel.timestamp,
        resolutionStatus = roomModel.resolutionStatus
    )

    fun toMap(): Map<String, Any?> {
        return mapOf(
            "submitterName" to submitterName,
            "submitterRole" to submitterRole,
            "ratingsStars" to ratingsStars,
            "problemTag" to problemTag,
            "description" to description,
            "timestamp" to timestamp,
            "resolutionStatus" to resolutionStatus
        )
    }
}

/**
 * Firestore data model representation for health facilities / clinics collection.
 * Path: /health_facilities/{facilityId} or /clinics/{clinicId}
 * Cached into Room Database to guarantee full offline location/contact access in remote areas.
 */
data class FirestoreHealthFacility(
    @DocumentId val firestoreId: String = "",
    val localId: Int = 0,
    val name: String = "",
    val district: String = "",
    val province: String = "",
    val address: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val contactPhone: String = "",
    val emergencyContact: String = "",
    val operatingHours: String = "",
    val servicesOffered: String = "",
    val coldChainStatus: String = "Operational (Solar Powered)",
    val totalChildren: Int = 0,
    val efficiencyRate: Double = 90.0,
    val hasIntermittentInternet: Boolean = false,
    val updatedAt: Long = 0L
) {
    constructor(roomModel: Clinic) : this(
        firestoreId = if (roomModel.firestoreId.isNotEmpty()) roomModel.firestoreId else "facility_${roomModel.id}",
        localId = roomModel.id,
        name = roomModel.name,
        district = roomModel.district,
        province = roomModel.province,
        address = roomModel.address,
        latitude = roomModel.latitude,
        longitude = roomModel.longitude,
        contactPhone = roomModel.contactPhone,
        emergencyContact = roomModel.emergencyContact,
        operatingHours = roomModel.operatingHours,
        servicesOffered = roomModel.servicesOffered,
        coldChainStatus = roomModel.coldChainStatus,
        totalChildren = roomModel.totalChildren,
        efficiencyRate = roomModel.efficiencyRate,
        hasIntermittentInternet = roomModel.hasIntermittentInternet,
        updatedAt = System.currentTimeMillis()
    )

    fun toRoomModel(): Clinic {
        return Clinic(
            id = localId,
            name = name,
            district = district,
            province = province.ifEmpty { "Zimbabwe" },
            address = address.ifEmpty { "MoH Health Centre, $district" },
            latitude = if (latitude != 0.0) latitude else -17.8252,
            longitude = if (longitude != 0.0) longitude else 31.0335,
            contactPhone = contactPhone.ifEmpty { "+263 242 700000" },
            emergencyContact = emergencyContact.ifEmpty { "999 / +263 772 100 200" },
            operatingHours = operatingHours.ifEmpty { "08:00 - 16:30 Mon-Fri" },
            servicesOffered = servicesOffered.ifEmpty { "ZEPI Vaccines, Cold Chain Storage, Child Health" },
            coldChainStatus = coldChainStatus.ifEmpty { "Operational" },
            totalChildren = totalChildren,
            efficiencyRate = efficiencyRate,
            hasIntermittentInternet = hasIntermittentInternet,
            firestoreId = firestoreId,
            lastCachedAt = System.currentTimeMillis(),
            isCachedLocally = true
        )
    }

    fun toMap(): Map<String, Any?> {
        return mapOf(
            "localId" to localId,
            "name" to name,
            "district" to district,
            "province" to province,
            "address" to address,
            "latitude" to latitude,
            "longitude" to longitude,
            "contactPhone" to contactPhone,
            "emergencyContact" to emergencyContact,
            "operatingHours" to operatingHours,
            "servicesOffered" to servicesOffered,
            "coldChainStatus" to coldChainStatus,
            "totalChildren" to totalChildren,
            "efficiencyRate" to efficiencyRate,
            "hasIntermittentInternet" to hasIntermittentInternet,
            "updatedAt" to updatedAt
        )
    }
}
