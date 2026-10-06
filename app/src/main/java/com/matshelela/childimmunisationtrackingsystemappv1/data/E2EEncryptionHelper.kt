package com.matshelela.childimmunisationtrackingsystemappv1.data

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.PublicKey
import java.security.SecureRandom
import java.security.spec.MGF1ParameterSpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.OAEPParameterSpec
import javax.crypto.spec.PSource
import javax.crypto.spec.SecretKeySpec

/**
 * Senior Cryptographic helper implementing true End-to-End Encryption (E2EE)
 * using a hybrid cryptography scheme:
 * - On-device hardware-backed RSA-2448 public/private key generation via Android KeyStore.
 * - Single-use AES-256 GCM symmetric session key generation per message.
 * - RSA-OAEP with SHA-256 and MGF1Padding for protecting the symmetric key during transit.
 */
object E2EEncryptionHelper {
    private const val TAG = "E2EEncryptionHelper"
    private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
    private const val RSA_ALPHANUMERIC_KEY_SIZE = 2048
    
    // Standard RSA OAEP specification compatible with Android Keystore
    private const val RSA_TRANSFORMATION = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding"
    private const val AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding"

    private val oaepParameterSpec = OAEPParameterSpec(
        "SHA-256",
        "MGF1",
        MGF1ParameterSpec.SHA256,
        PSource.PSpecified.DEFAULT
    )

    /**
     * Generates a hardware-backed RSA KeyPair inside the secure Android Keystore.
     * The private key remains safe inside the hardware security module (TEE/SE).
     */
    fun generateHardwareKeyPair(alias: String): PublicKey? {
        return try {
            val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
            
            if (!keyStore.containsAlias(alias)) {
                Log.d(TAG, "Generating new hardware-backed RSA keypair for alias: $alias")
                val keyPairGenerator = KeyPairGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_RSA,
                    KEYSTORE_PROVIDER
                )
                
                val spec = KeyGenParameterSpec.Builder(
                    alias,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setKeySize(RSA_ALPHANUMERIC_KEY_SIZE)
                    .setBlockModes(KeyProperties.BLOCK_MODE_ECB)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_OAEP)
                    .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA1)
                    .build()

                keyPairGenerator.initialize(spec)
                val keyPair = keyPairGenerator.generateKeyPair()
                keyPair.public
            } else {
                Log.d(TAG, "KeyPair already exists for alias: $alias")
                getPublicKey(alias)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error generating hardware keypair: ${e.message}", e)
            null
        }
    }

    /**
     * Gets the Base64 representation of the on-device public key for key distribution.
     */
    fun getPublicKeyBase64(alias: String): String? {
        val publicKey = getPublicKey(alias) ?: return null
        return Base64.encodeToString(publicKey.encoded, Base64.NO_WRAP)
    }

    /**
     * Retrieves the public key from Android KeyStore.
     */
    private fun getPublicKey(alias: String): PublicKey? {
        return try {
            val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
            val certificate = keyStore.getCertificate(alias)
            certificate?.publicKey
        } catch (e: Exception) {
            Log.e(TAG, "Failed to retrieve public key: ${e.message}", e)
            null
        }
    }

    /**
     * Reconstructs a PublicKey object from its Base64 X.509 format.
     */
    fun loadPublicKeyFromBase64(base64PublicKey: String): PublicKey? {
        return try {
            val decodedBytes = Base64.decode(base64PublicKey, Base64.NO_WRAP)
            val keySpec = X509EncodedKeySpec(decodedBytes)
            val keyFactory = KeyFactory.getInstance("RSA")
            keyFactory.generatePublic(keySpec)
        } catch (e: Exception) {
            Log.e(TAG, "Error loading public key from Base64: ${e.message}", e)
            null
        }
    }

    /**
     * Generates a single-use symmetric AES-256 session key.
     */
    fun generateSingleUseAesKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance("AES")
        keyGenerator.init(256)
        return keyGenerator.generateKey()
    }

    /**
     * Encrypts plain message payload using the single-use AES key with AES-256 GCM.
     * Prepends the 12-byte initialization vector (IV) to the ciphertext.
     */
    fun encryptPayload(plainText: String, aesKey: SecretKey): String? {
        return try {
            val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
            val iv = ByteArray(12)
            SecureRandom().nextBytes(iv)
            val gcmSpec = GCMParameterSpec(128, iv)
            
            cipher.init(Cipher.ENCRYPT_MODE, aesKey, gcmSpec)
            val ciphertext = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            
            val combined = iv + ciphertext
            Base64.encodeToString(combined, Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e(TAG, "Error encrypting payload: ${e.message}", e)
            null
        }
    }

    /**
     * Decrypts combined IV + ciphertext using the recovered AES key and AES-256 GCM.
     */
    fun decryptPayload(combinedBase64: String, aesKey: SecretKey): String? {
        return try {
            val combined = Base64.decode(combinedBase64, Base64.NO_WRAP)
            if (combined.size < 12) return null
            
            val iv = combined.copyOfRange(0, 12)
            val ciphertext = combined.copyOfRange(12, combined.size)
            
            val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
            val gcmSpec = GCMParameterSpec(128, iv)
            
            cipher.init(Cipher.DECRYPT_MODE, aesKey, gcmSpec)
            val decryptedBytes = cipher.doFinal(ciphertext)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            Log.e(TAG, "Error decrypting payload: ${e.message}", e)
            null
        }
    }

    /**
     * Encrypts the single-use symmetric AES key using the recipient's RSA public key.
     */
    fun encryptAesKey(aesKey: SecretKey, recipientPublicKey: PublicKey): String? {
        return try {
            val cipher = Cipher.getInstance(RSA_TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, recipientPublicKey, oaepParameterSpec)
            val encryptedBytes = cipher.doFinal(aesKey.encoded)
            Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e(TAG, "Error encrypting AES key: ${e.message}", e)
            null
        }
    }

    /**
     * Decrypts the single-use AES key using the locally stored hardware-backed private key.
     */
    fun decryptAesKey(encryptedAesKeyBase64: String, localAlias: String): SecretKey? {
        return try {
            val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
            val privateKey = keyStore.getKey(localAlias, null) as? PrivateKey ?: return null
            
            val cipher = Cipher.getInstance(RSA_TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, privateKey, oaepParameterSpec)
            
            val encryptedBytes = Base64.decode(encryptedAesKeyBase64, Base64.NO_WRAP)
            val decryptedBytes = cipher.doFinal(encryptedBytes)
            SecretKeySpec(decryptedBytes, "AES")
        } catch (e: Exception) {
            Log.e(TAG, "Error decrypting AES key: ${e.message}", e)
            null
        }
    }
}
