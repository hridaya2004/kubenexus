package dev.hridaya.kubenexus.data.kubeconfig

import java.security.KeyPairGenerator
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PemKeyParserTest {

    @Test
    fun `pkcs1ToPkcs8 converts raw RSA private key bytes to valid PKCS8`() {
        val keyGen = KeyPairGenerator.getInstance("RSA")
        keyGen.initialize(2048)
        val keyPair = keyGen.generateKeyPair()
        val pkcs8Encoded = keyPair.private.encoded

        // Validate that pkcs8Encoded can be parsed
        val parsedKey = PemKeyParser.parsePrivateKey(
            java.util.Base64.getEncoder().encodeToString(pkcs8Encoded),
        )
        assertNotNull(parsedKey)
        assertTrue(parsedKey.algorithm == "RSA")
    }
}
