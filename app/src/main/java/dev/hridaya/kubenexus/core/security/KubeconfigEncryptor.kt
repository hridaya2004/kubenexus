package dev.hridaya.kubenexus.core.security

/**
 * Contract for encrypting and decrypting sensitive kubeconfig credentials stored at rest.
 */
interface KubeconfigEncryptor {

    /**
     * Encrypts the provided plaintext kubeconfig string into an authenticated ciphertext string.
     * If the content is already encrypted, returns the ciphertext as-is (idempotent).
     */
    fun encrypt(plainText: String): String

    /**
     * Decrypts the provided ciphertext string back into the original plaintext kubeconfig.
     * If the input is not encrypted (e.g. legacy plaintext records), returns it as-is for safe backward compatibility.
     */
    fun decrypt(cipherText: String): String

    /**
     * Returns true if the given string is encrypted by this encryptor format.
     */
    fun isEncrypted(text: String): Boolean
}

/**
 * A pass-through no-op encryptor primarily useful as a default fallback or for raw tests.
 */
object NoOpKubeconfigEncryptor : KubeconfigEncryptor {
    override fun encrypt(plainText: String): String = plainText
    override fun decrypt(cipherText: String): String = cipherText
    override fun isEncrypted(text: String): Boolean = false
}
