package com.example.offlinenotes.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

class VaultManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("vault_prefs", Context.MODE_PRIVATE)

    val isVaultSetup: Boolean
        get() = prefs.contains(KEY_SALT) && prefs.contains(KEY_VERIFICATION)

    val isVaultUnlocked: Boolean
        get() = sessionKey != null

    fun setupVault(password: String) {
        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)

        val key = deriveKey(password, salt)
        val (ciphertext, iv) = encrypt(VERIFICATION_PLAIN, key)

        prefs.edit()
            .putString(KEY_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString(KEY_VERIFICATION, Base64.encodeToString(ciphertext, Base64.NO_WRAP))
            .putString(KEY_VERIFICATION_IV, Base64.encodeToString(iv, Base64.NO_WRAP))
            .apply()

        sessionKey = key
    }

    fun unlockVault(password: String): Boolean {
        if (!isVaultSetup) return false

        val saltBase64 = prefs.getString(KEY_SALT, null) ?: return false
        val verificationBase64 = prefs.getString(KEY_VERIFICATION, null) ?: return false
        val ivBase64 = prefs.getString(KEY_VERIFICATION_IV, null) ?: return false

        val salt = Base64.decode(saltBase64, Base64.NO_WRAP)
        val ciphertext = Base64.decode(verificationBase64, Base64.NO_WRAP)
        val iv = Base64.decode(ivBase64, Base64.NO_WRAP)

        val key = deriveKey(password, salt)

        return try {
            val decrypted = decrypt(ciphertext, key, iv)
            if (decrypted == VERIFICATION_PLAIN) {
                sessionKey = key
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    fun lockVault() {
        sessionKey = null
    }

    fun encryptNoteData(plaintext: String): Pair<String, String> {
        val key = sessionKey ?: throw IllegalStateException("Vault is locked")
        val (ciphertext, iv) = encrypt(plaintext, key)
        return Pair(
            Base64.encodeToString(ciphertext, Base64.NO_WRAP),
            Base64.encodeToString(iv, Base64.NO_WRAP)
        )
    }

    fun decryptNoteData(ciphertextBase64: String, ivBase64: String): String {
        val key = sessionKey ?: throw IllegalStateException("Vault is locked")
        val ciphertext = Base64.decode(ciphertextBase64, Base64.NO_WRAP)
        val iv = Base64.decode(ivBase64, Base64.NO_WRAP)
        return decrypt(ciphertext, key, iv)
    }

    fun getVaultSalt(): String? = prefs.getString(KEY_SALT, null)
    fun getVaultVerificationToken(): String? = prefs.getString(KEY_VERIFICATION, null)
    fun getVaultVerificationIv(): String? = prefs.getString(KEY_VERIFICATION_IV, null)

    fun restoreVaultAuth(salt: String, token: String, iv: String) {
        prefs.edit()
            .putString(KEY_SALT, salt)
            .putString(KEY_VERIFICATION, token)
            .putString(KEY_VERIFICATION_IV, iv)
            .apply()
        lockVault()
    }

    private fun deriveKey(password: String, salt: ByteArray): SecretKey {
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val secret = factory.generateSecret(spec)
        return SecretKeySpec(secret.encoded, "AES")
    }

    private fun encrypt(plaintext: String, key: SecretKey): Pair<ByteArray, ByteArray> {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val iv = ByteArray(12)
        SecureRandom().nextBytes(iv)
        val spec = GCMParameterSpec(TAG_LENGTH, iv)
        cipher.init(Cipher.ENCRYPT_MODE, key, spec)
        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        return Pair(ciphertext, iv)
    }

    private fun decrypt(ciphertext: ByteArray, key: SecretKey, iv: ByteArray): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, key, spec)
        val plaintext = cipher.doFinal(ciphertext)
        return String(plaintext, Charsets.UTF_8)
    }

    companion object {
        // FIXED: Elevated to static companion so all fragments share the same unlock state
        private var sessionKey: SecretKey? = null

        private const val ITERATIONS = 600000
        private const val KEY_LENGTH = 256
        private const val TAG_LENGTH = 128
        private const val VERIFICATION_PLAIN = "VAULT_AUTH_VALID"

        private const val KEY_SALT = "vault_salt"
        private const val KEY_VERIFICATION = "vault_verification"
        private const val KEY_VERIFICATION_IV = "vault_verification_iv"
    }
}