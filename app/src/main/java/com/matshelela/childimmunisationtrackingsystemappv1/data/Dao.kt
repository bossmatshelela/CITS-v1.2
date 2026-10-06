package com.matshelela.childimmunisationtrackingsystemappv1.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ImmunisationDao {

    // --- Child Profiles ---
    @Query("SELECT * FROM child_profiles ORDER BY name ASC")
    fun getAllChildProfiles(): Flow<List<ChildProfile>>

    @Query("SELECT * FROM child_profiles WHERE id = :id")
    fun getChildProfileById(id: Int): Flow<ChildProfile?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChildProfile(profile: ChildProfile): Long

    @Update
    suspend fun updateChildProfile(profile: ChildProfile)

    @Delete
    suspend fun deleteChildProfile(profile: ChildProfile)

    // --- Vaccination Records ---
    @Query("SELECT * FROM vaccination_records WHERE childId = :childId ORDER BY scheduledDate ASC")
    fun getVaccinationRecordsByChild(childId: Int): Flow<List<VaccinationRecord>>

    @Query("SELECT * FROM vaccination_records ORDER BY scheduledDate DESC")
    fun getAllVaccinationRecords(): Flow<List<VaccinationRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVaccinationRecord(record: VaccinationRecord): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVaccinationRecords(records: List<VaccinationRecord>)

    @Update
    suspend fun updateVaccinationRecord(record: VaccinationRecord)

    // --- Appointments ---
    @Query("SELECT * FROM appointments ORDER BY appointmentDate ASC")
    fun getAllAppointments(): Flow<List<Appointment>>

    @Query("SELECT * FROM appointments WHERE childId = :childId ORDER BY appointmentDate ASC")
    fun getAppointmentsByChild(childId: Int): Flow<List<Appointment>>

    @Query("SELECT * FROM appointments WHERE clinicId = :clinicId ORDER BY appointmentDate ASC")
    fun getAppointmentsByClinic(clinicId: Int): Flow<List<Appointment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppointment(appointment: Appointment): Long

    @Update
    suspend fun updateAppointment(appointment: Appointment)

    @Query("DELETE FROM appointments WHERE id = :id")
    suspend fun deleteAppointmentById(id: Int)

    // --- Secures Messaging ---
    @Query("SELECT * FROM clinic_messages ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<ClinicMessage>>

    @Query("SELECT * FROM clinic_messages WHERE id = :id")
    suspend fun getMessageById(id: Int): ClinicMessage?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ClinicMessage): Long

    // --- Clinics / Health Facilities (Local Room Cache) ---
    @Query("SELECT * FROM clinics ORDER BY name ASC")
    fun getAllClinics(): Flow<List<Clinic>>

    @Query("SELECT * FROM clinics WHERE id = :id")
    fun getClinicById(id: Int): Flow<Clinic?>

    @Query("SELECT * FROM clinics WHERE district = :district ORDER BY name ASC")
    fun getClinicsByDistrict(district: String): Flow<List<Clinic>>

    @Query("SELECT * FROM clinics WHERE name LIKE '%' || :query || '%' OR district LIKE '%' || :query || '%' OR address LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchClinics(query: String): Flow<List<Clinic>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClinic(clinic: Clinic): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClinics(clinics: List<Clinic>)

    @Update
    suspend fun updateClinic(clinic: Clinic)

    @Delete
    suspend fun deleteClinic(clinic: Clinic)

    @Query("DELETE FROM clinics")
    suspend fun deleteAllClinics()

    // --- Doctors & Nurses ---
    @Query("SELECT * FROM doctors_nurses ORDER BY name ASC")
    fun getAllDoctorsNurses(): Flow<List<DoctorNurse>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDoctorNurse(person: DoctorNurse): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDoctorsNurses(people: List<DoctorNurse>)

    @Delete
    suspend fun deleteDoctorNurse(person: DoctorNurse)

    // --- User Feedback ---
    @Query("SELECT * FROM user_feedbacks ORDER BY timestamp DESC")
    fun getAllUserFeedbacks(): Flow<List<UserFeedback>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserFeedback(feedback: UserFeedback): Long

    @Update
    suspend fun updateUserFeedback(feedback: UserFeedback)

    @Delete
    suspend fun deleteUserFeedback(feedback: UserFeedback)

    // --- Unsynced Fetch Queries for Firestore Synchronisation ---
    @Query("SELECT * FROM child_profiles WHERE isSynced = 0")
    suspend fun getUnsyncedChildProfiles(): List<ChildProfile>

    @Query("SELECT * FROM vaccination_records WHERE isSynced = 0")
    suspend fun getUnsyncedVaccinationRecords(): List<VaccinationRecord>

    @Query("SELECT * FROM appointments WHERE isSynced = 0")
    suspend fun getUnsyncedAppointments(): List<Appointment>

    @Query("SELECT * FROM clinic_messages WHERE isSynced = 0")
    suspend fun getUnsyncedClinicMessages(): List<ClinicMessage>

    @Query("SELECT * FROM user_feedbacks WHERE isSynced = 0")
    suspend fun getUnsyncedUserFeedbacks(): List<UserFeedback>
}
