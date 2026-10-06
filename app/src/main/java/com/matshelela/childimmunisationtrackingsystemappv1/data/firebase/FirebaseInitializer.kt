package com.matshelela.childimmunisationtrackingsystemappv1.data.firebase

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.matshelela.childimmunisationtrackingsystemappv1.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * FirebaseConfigParameters encapsulates the dynamic AI chatbot configuration
 * fetched securely from Firebase Remote Config.
 */
data class FirebaseConfigParameters(
    val modelName: String = DEFAULT_MODEL_NAME,
    val systemInstruction: String = DEFAULT_SYSTEM_INSTRUCTION,
    val temperature: Float = 0.7f,
    val isInitialized: Boolean = false
) {
    companion object {
        const val KEY_MODEL_NAME = "cits_chatbot_model_name"
        const val KEY_SYSTEM_INSTRUCTION = "cits_chatbot_system_instruction"
        const val KEY_TEMPERATURE = "cits_chatbot_temperature"

        // Default to modern fast Gemini model for high-throughput pediatric advice
        const val DEFAULT_MODEL_NAME = "gemini-3.5-flash"

        const val DEFAULT_SYSTEM_INSTRUCTION = """You are "CITS Assistant," an empathetic, highly knowledgeable pediatric health assistant integrated into the Child Immunisation Tracking System (CITS) app. Your primary role is to help parents understand and track their children's immunization schedules, explain vaccine-preventable diseases in simple terms, and encourage timely vaccinations.

Strict Guardrails:
1. Tone: Warm, reassuring, professional, and clear.
2. Medical Disclaimer: Always include a brief, polite reminder that your guidance is for informational purposes only and does not replace professional medical advice. Recommend consulting their local clinic or pediatrician for medical decisions.
3. Formatting: Use clear bullet points and bold text for schedules or key details so it is easy to read on mobile screens.
4. Boundaries: If asked about topics completely unrelated to pediatric health, child immunization tracking, or childcare, politely decline and pivot back to your main purpose.

Clinical Reference (Zimbabwe Expanded Programme on Immunisation - ZEPI):
- At Birth: BCG (Tuberculosis) and OPV 0 (Oral Polio).
- At 6 Weeks: Pentavalent 1 (DTP-HepB-Hib), OPV 1, PCV 1 (Pneumococcal), and Rotavirus 1.
- At 10 Weeks: Pentavalent 2, OPV 2, PCV 2, and Rotavirus 2.
- At 14 Weeks: Pentavalent 3, IPV (Inactivated Polio), and PCV 3.
- At 6 Months: Vitamin A supplementation (100,000 IU).
- At 9 Months: Measles-Rubella 1 (MR 1), Typhoid Conjugate (TCV), and Vitamin A (200,000 IU).
- At 18 Months: Measles-Rubella 2 (MR 2) and Vitamin A.
- At 5 Years: DTP Booster dose.
- At 9-14 Years: HPV vaccine for cervical cancer prevention."""
    }
}

/**
 * Helper object that asynchronously initializes Firebase services:
 * 1. Firebase Core initialization check.
 * 2. Firebase App Check (Play Integrity for release, Debug provider for debug builds).
 * 3. Firebase Remote Config to dynamically fetch AI model parameters with zero downtime.
 */
object FirebaseInitializer {

    private const val TAG = "FirebaseInitializer"

    private val _configState = MutableStateFlow(FirebaseConfigParameters())
    val configState: StateFlow<FirebaseConfigParameters> = _configState.asStateFlow()

    private var isAppCheckInitialized = false

    /**
     * Initializes App Check and Remote Config asynchronously on Dispatchers.IO.
     * Prevents any main-thread blocking during app launch or network handshakes.
     */
    suspend fun initializeAsync(context: Context): FirebaseConfigParameters = withContext(Dispatchers.IO) {
        try {
            // 1. Ensure Firebase Core is initialized
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
            }

            // 2. Configure Firebase App Check
            configureAppCheck()

            // 3. Configure and fetch Remote Config parameters
            val config = fetchRemoteConfigAsync()
            _configState.value = config
            config
        } catch (e: Exception) {
            Log.e(TAG, "Firebase initialization warning: ${e.message}", e)
            val fallbackConfig = FirebaseConfigParameters(isInitialized = true)
            _configState.value = fallbackConfig
            fallbackConfig
        }
    }

    /**
     * Configures Firebase App Check.
     * Uses Play Integrity in production, or DebugAppCheckProviderFactory during development.
     */
    private fun configureAppCheck() {
        if (isAppCheckInitialized) return
        try {
            val appCheck = FirebaseAppCheck.getInstance()
            val factory = if (BuildConfig.DEBUG) {
                DebugAppCheckProviderFactory.getInstance()
            } else {
                PlayIntegrityAppCheckProviderFactory.getInstance()
            }
            appCheck.installAppCheckProviderFactory(factory)
            isAppCheckInitialized = true
            Log.d(TAG, "Firebase App Check installed with ${factory.javaClass.simpleName}")
        } catch (e: Exception) {
            Log.w(TAG, "App Check installation bypassed/deferred: ${e.message}")
        }
    }

    /**
     * Configures Firebase Remote Config with safe local defaults and fetches cloud updates.
     */
    private suspend fun fetchRemoteConfigAsync(): FirebaseConfigParameters {
        val remoteConfig = FirebaseRemoteConfig.getInstance()

        // Configure fetch intervals (0s in debug for instant testing; 1 hr in production)
        val configSettings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(if (BuildConfig.DEBUG) 0L else 3600L)
            .setFetchTimeoutInSeconds(15L)
            .build()

        remoteConfig.setConfigSettingsAsync(configSettings).await()

        // Define local fallback defaults map
        val defaults = mapOf<String, Any>(
            FirebaseConfigParameters.KEY_MODEL_NAME to FirebaseConfigParameters.DEFAULT_MODEL_NAME,
            FirebaseConfigParameters.KEY_SYSTEM_INSTRUCTION to FirebaseConfigParameters.DEFAULT_SYSTEM_INSTRUCTION,
            FirebaseConfigParameters.KEY_TEMPERATURE to 0.7
        )
        remoteConfig.setDefaultsAsync(defaults).await()

        try {
            // Fetch and activate from cloud
            val activated = remoteConfig.fetchAndActivate().await()
            Log.d(TAG, "Remote Config fetch completed. New parameters activated: $activated")
        } catch (e: Exception) {
            Log.w(TAG, "Remote Config fetch failed (using local defaults): ${e.message}")
        }

        val rawModelName = remoteConfig.getString(FirebaseConfigParameters.KEY_MODEL_NAME).ifBlank {
            FirebaseConfigParameters.DEFAULT_MODEL_NAME
        }

        // Map deprecated or legacy model names to active preview model
        val resolvedModel = if (rawModelName.contains("1.5-flash") || rawModelName.contains("2.0-flash")) {
            FirebaseConfigParameters.DEFAULT_MODEL_NAME
        } else {
            rawModelName
        }

        val systemInstruction = remoteConfig.getString(FirebaseConfigParameters.KEY_SYSTEM_INSTRUCTION).ifBlank {
            FirebaseConfigParameters.DEFAULT_SYSTEM_INSTRUCTION
        }

        val temperature = remoteConfig.getDouble(FirebaseConfigParameters.KEY_TEMPERATURE).toFloat()

        return FirebaseConfigParameters(
            modelName = resolvedModel,
            systemInstruction = systemInstruction,
            temperature = if (temperature in 0.0f..2.0f) temperature else 0.7f,
            isInitialized = true
        )
    }
}
