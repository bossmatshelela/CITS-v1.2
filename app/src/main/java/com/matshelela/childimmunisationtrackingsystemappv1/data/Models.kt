package com.matshelela.childimmunisationtrackingsystemappv1.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Child profile representing a pediatric patient in Zimbabwe.
 */
@Entity(tableName = "child_profiles")
data class ChildProfile(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val birthDate: String, // format: YYYY-MM-DD
    val gender: String, // "Boy" or "Girl"
    val motherName: String,
    val contactPhone: String,
    val healthPin: String, // e.g., "ZW-MOH-2026-X84A" (national ID)
    val district: String, // e.g., "Harare", "Bulawayo", "Chitungwiza", "Mutare", "Gweru"
    val clinicId: Int, // primary registered clinic
    val complianceStatus: String = "Due", // "Compliant", "Due", "Overdue"
    val isSynced: Boolean = true
)

/**
 * Vaccination record for a childhood vaccine dose based on the Zimbabwe Expanded Programme on Immunisation (ZEPI).
 */
@Entity(tableName = "vaccination_records")
data class VaccinationRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val childId: Int,
    val vaccineName: String, // e.g., "BCG", "OPV", "IPV", "Rotavirus", "Pentavalent", "PCV", "Measles-Rubella"
    val doseNumber: Int, // e.g., 1, 2, 3
    val scheduledDate: String, // format: YYYY-MM-DD
    val administeredDate: String?, // YYYY-MM-DD, null if pending
    val administeredBy: String?, // Doctor or nurse identifier
    val batchNumber: String?, // Encrypted or plain
    val facilityName: String?, // Registered clinic where vaccine was status-checked
    val notes: String?, // Encrypted medical notes
    val isSynced: Boolean = true // Sync tracking for clinic connectivity issues
)

/**
 * Child medical clinic appointments requiring approval.
 */
@Entity(tableName = "appointments")
data class Appointment(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val childId: Int,
    val clinicId: Int,
    val vaccineNames: String, // comma-separated, e.g. "Pentavalent 1, OPV 1, PCV 1"
    val appointmentDate: String, // YYYY-MM-DD
    val appointmentTime: String, // e.g., "09:00 AM"
    val status: String = "Pending Approval", // "Pending Approval", "Approved", "Completed", "Cancelled"
    val notes: String? = null,
    val isSynced: Boolean = true
)

/**
 * Secure HIPAA-compliant messages exchanged between parents and doctors.
 */
@Entity(tableName = "clinic_messages")
data class ClinicMessage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val childId: Int?,
    val senderName: String,
    val senderRole: String, // "Doctor", "Parent", "MoH Admin"
    val content: String, // Encrypted content on-device
    val timestamp: Long,
    val isEncrypted: Boolean = true,
    val recipientRole: String, // "Doctor", "Parent", "MoH Admin"
    val isSynced: Boolean = true,
    val isE2EE: Boolean = false,
    val encryptedAesKey: String? = null
)

/**
 * Health Centers / Clinics registered with the Ministry of Health and Child Care of Zimbabwe.
 * Cached locally in Room Database to ensure 100% offline availability in remote areas.
 */
@Entity(tableName = "clinics")
data class Clinic(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val district: String,
    val totalChildren: Int = 0,
    val efficiencyRate: Double = 90.0, // performance metric based on compliant vs total
    val hasIntermittentInternet: Boolean = false,
    val province: String = "Harare",
    val address: String = "MoH Health Centre",
    val latitude: Double = -17.8252,
    val longitude: Double = 31.0335,
    val contactPhone: String = "+263 242 700000",
    val emergencyContact: String = "999 / +263 772 100 200",
    val operatingHours: String = "08:00 - 16:30 Mon-Fri (24/7 Emergency/Maternity)",
    val servicesOffered: String = "ZEPI Vaccines, Cold Chain Storage, Antenatal Care, Growth Monitoring, Vitamin A",
    val coldChainStatus: String = "Operational (Solar Powered)",
    val firestoreId: String = "",
    val lastCachedAt: Long = System.currentTimeMillis(),
    val isCachedLocally: Boolean = true
)

/**
 * Doctor/Nurse medical professional entity.
 * Managed by MoH Central Administrations.
 */
@Entity(tableName = "doctors_nurses")
data class DoctorNurse(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val role: String, // "Doctor" or "Nurse"
    val contactNo: String,
    val clinicId: Int, // Associated Health Center Clinic
    val designatorCode: String // professional license registration ID
)

/**
 * User feedback & rating of application usage or system health worker problems.
 */
@Entity(tableName = "user_feedbacks")
data class UserFeedback(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val submitterName: String,
    val submitterRole: String, // "Parent / Guardian", "Doctor / Clinic Staff", "MoH Admin"
    val ratingsStars: Int, // rating from 1 to 5 stars
    val problemTag: String, // "App Bugs", "Vaccine Stockout", "Wait Times", "Intermittent Internet", "Other"
    val description: String, // description of the feedback/problem
    val timestamp: Long,
    val resolutionStatus: String = "Pending Review", // "Pending Review", "Under Investigation", "Resolved"
    val isSynced: Boolean = true
)


