package dev.hridaya.kubenexus

import androidx.compose.ui.unit.dp
import dev.hridaya.kubenexus.presentation.common.KubeNexus
import dev.hridaya.kubenexus.presentation.common.KubeNexusLogo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class KubeNexusLogoTest {

    @Test
    fun testKubeNexusLogoVector() {
        val logo = KubeNexusLogo
        assertNotNull(logo)
        assertEquals("KubeNexusLogo", logo.name)
        assertEquals(153.dp, logo.defaultWidth)
        assertEquals(287.dp, logo.defaultHeight)
        assertEquals(153f, logo.viewportWidth, 0.001f)
        assertEquals(287f, logo.viewportHeight, 0.001f)
        assertEquals(logo, KubeNexus)
    }
}
