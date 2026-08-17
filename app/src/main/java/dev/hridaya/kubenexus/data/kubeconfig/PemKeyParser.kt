package dev.hridaya.kubenexus.data.kubeconfig

import java.io.ByteArrayInputStream
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.spec.PKCS8EncodedKeySpec

object PemKeyParser {

    /**
     * Parses an X.509 Certificate from either raw PEM string, or base64-encoded PEM string.
     */
    fun parseCertificate(certData: String): X509Certificate {
        val rawBytes = decodeIfBase64(certData.trim())
        val certStream = ByteArrayInputStream(rawBytes)
        val certFactory = CertificateFactory.getInstance("X.509")
        return certFactory.generateCertificate(certStream) as X509Certificate
    }

    /**
     * Parses a PrivateKey from PKCS#8, PKCS#1 RSA, or SEC1 EC, whether raw PEM or base64-encoded PEM.
     */
    fun parsePrivateKey(keyData: String): PrivateKey {
        val decodedString = try {
            val bytes = decodeBase64(keyData.trim())
            val text = String(bytes, Charsets.UTF_8)
            if (text.contains("BEGIN")) text else String(bytes, Charsets.ISO_8859_1)
        } catch (e: Exception) {
            keyData.trim()
        }

        val text = if (decodedString.contains("BEGIN")) decodedString else keyData.trim()

        return when {
            text.contains("RSA PRIVATE KEY") -> {
                val base64Content = extractPemBody(text, "RSA PRIVATE KEY")
                val pkcs1Bytes = decodeBase64(base64Content)
                val pkcs8Bytes = pkcs1ToPkcs8(pkcs1Bytes)
                val keySpec = PKCS8EncodedKeySpec(pkcs8Bytes)
                KeyFactory.getInstance("RSA").generatePrivate(keySpec)
            }

            text.contains("EC PRIVATE KEY") -> {
                val base64Content = extractPemBody(text, "EC PRIVATE KEY")
                val ecDerBytes = decodeBase64(base64Content)
                val pkcs8Bytes = sec1ToPkcs8(ecDerBytes)
                val keySpec = PKCS8EncodedKeySpec(pkcs8Bytes)
                KeyFactory.getInstance("EC").generatePrivate(keySpec)
            }

            text.contains("PRIVATE KEY") -> {
                val base64Content = extractPemBody(text, "PRIVATE KEY")
                val pkcs8Bytes = decodeBase64(base64Content)
                val keySpec = PKCS8EncodedKeySpec(pkcs8Bytes)
                try {
                    KeyFactory.getInstance("RSA").generatePrivate(keySpec)
                } catch (e: Exception) {
                    KeyFactory.getInstance("EC").generatePrivate(keySpec)
                }
            }

            else -> {
                val derBytes = decodeBase64(text.replace("\\s".toRegex(), ""))
                val keySpec = PKCS8EncodedKeySpec(derBytes)
                try {
                    KeyFactory.getInstance("RSA").generatePrivate(keySpec)
                } catch (e: Exception) {
                    val pkcs8FromPkcs1 = pkcs1ToPkcs8(derBytes)
                    KeyFactory.getInstance("RSA")
                        .generatePrivate(PKCS8EncodedKeySpec(pkcs8FromPkcs1))
                }
            }
        }
    }

    private fun decodeIfBase64(input: String): ByteArray {
        return try {
            val decoded = decodeBase64(input)
            val str = String(decoded, Charsets.UTF_8)
            if (str.contains("-----BEGIN")) {
                decoded
            } else if (input.contains("-----BEGIN")) {
                input.toByteArray(Charsets.UTF_8)
            } else {
                decoded
            }
        } catch (e: Exception) {
            input.toByteArray(Charsets.UTF_8)
        }
    }

    private fun decodeBase64(input: String): ByteArray {
        val clean = input.replace("\\s".toRegex(), "")
        return try {
            java.util.Base64.getDecoder().decode(clean)
        } catch (e: Throwable) {
            try {
                android.util.Base64.decode(clean, android.util.Base64.DEFAULT)
            } catch (e2: Throwable) {
                clean.toByteArray(Charsets.UTF_8)
            }
        }
    }

    private fun extractPemBody(pem: String, type: String): String {
        val startMarker = "-----BEGIN $type-----"
        val endMarker = "-----END $type-----"
        val startIndex = pem.indexOf(startMarker)
        val endIndex = pem.indexOf(endMarker)
        return if (startIndex != -1 && endIndex != -1) {
            pem.substring(startIndex + startMarker.length, endIndex)
                .replace("\\s".toRegex(), "")
        } else {
            pem.replace("-----BEGIN[^-]+-----".toRegex(), "")
                .replace("-----END[^-]+-----".toRegex(), "")
                .replace("\\s".toRegex(), "")
        }
    }

    /**
     * Converts PKCS#1 RSA private key DER bytes to PKCS#8 PrivateKeyInfo DER bytes.
     * PKCS#8 format:
     * SEQUENCE {
     *   INTEGER 0 (version)
     *   SEQUENCE { OID 1.2.840.113549.1.1.1 (rsaEncryption), NULL }
     *   OCTET STRING (pkcs1Bytes)
     * }
     */
    fun pkcs1ToPkcs8(pkcs1Bytes: ByteArray): ByteArray {
        val rsaAlgorithmIdentifier = byteArrayOf(
            0x30.toByte(),
            0x0d.toByte(),
            0x06.toByte(),
            0x09.toByte(),
            0x2a.toByte(),
            0x86.toByte(),
            0x48.toByte(),
            0x86.toByte(),
            0xf7.toByte(),
            0x0d.toByte(),
            0x01.toByte(),
            0x01.toByte(),
            0x01.toByte(),
            0x05.toByte(),
            0x00.toByte()
        )

        val version = byteArrayOf(0x02.toByte(), 0x01.toByte(), 0x00.toByte())

        // OCTET STRING tag (0x04) + length + pkcs1Bytes
        val octetString = encodeAsn1(0x04.toByte(), pkcs1Bytes)

        val payload = version + rsaAlgorithmIdentifier + octetString
        return encodeAsn1(0x30.toByte(), payload)
    }

    /**
     * Converts SEC1 EC private key DER bytes to PKCS#8 PrivateKeyInfo DER bytes.
     */
    private fun sec1ToPkcs8(sec1Bytes: ByteArray): ByteArray {
        val ecAlgorithmIdentifier = byteArrayOf(
            0x30.toByte(),
            0x13.toByte(),
            0x06.toByte(),
            0x07.toByte(),
            0x2a.toByte(),
            0x86.toByte(),
            0x48.toByte(),
            0xce.toByte(),
            0x3d.toByte(),
            0x02.toByte(),
            0x01.toByte(),
            0x06.toByte(),
            0x08.toByte(),
            0x2a.toByte(),
            0x86.toByte(),
            0x48.toByte(),
            0xce.toByte(),
            0x3d.toByte(),
            0x03.toByte(),
            0x01.toByte(),
            0x07.toByte()
        )
        val version = byteArrayOf(0x02.toByte(), 0x01.toByte(), 0x00.toByte())
        val octetString = encodeAsn1(0x04.toByte(), sec1Bytes)
        val payload = version + ecAlgorithmIdentifier + octetString
        return encodeAsn1(0x30.toByte(), payload)
    }

    private fun encodeAsn1(tag: Byte, content: ByteArray): ByteArray {
        val len = content.size
        val lenBytes = when {
            len < 128 -> byteArrayOf(len.toByte())
            len < 256 -> byteArrayOf(0x81.toByte(), len.toByte())
            len < 65536 -> byteArrayOf(0x82.toByte(), (len shr 8).toByte(), (len and 0xFF).toByte())
            else -> byteArrayOf(
                0x83.toByte(),
                (len shr 16).toByte(),
                ((len shr 8) and 0xFF).toByte(),
                (len and 0xFF).toByte()
            )
        }
        return byteArrayOf(tag) + lenBytes + content
    }
}
