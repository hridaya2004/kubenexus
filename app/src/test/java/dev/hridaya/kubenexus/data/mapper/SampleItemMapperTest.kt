package dev.hridaya.kubenexus.data.mapper

import dev.hridaya.kubenexus.data.dto.SampleItemDto
import dev.hridaya.kubenexus.domain.model.SampleItem
import org.junit.Assert.assertEquals
import org.junit.Test

class SampleItemMapperTest {

    @Test
    fun `toDomain maps DTO fields correctly`() {
        val dto = SampleItemDto(
            id = "1",
            title = "Test Title",
            description = "Test Desc",
            timestamp = 12345L
        )

        val domain = dto.toDomain()

        assertEquals("1", domain.id)
        assertEquals("Test Title", domain.title)
        assertEquals("Test Desc", domain.description)
        assertEquals(12345L, domain.timestamp)
    }

    @Test
    fun `toDto maps domain model fields correctly`() {
        val domain = SampleItem(
            id = "2",
            title = "Domain Title",
            description = "Domain Desc",
            timestamp = 67890L
        )

        val dto = domain.toDto()

        assertEquals("2", dto.id)
        assertEquals("Domain Title", dto.title)
        assertEquals("Domain Desc", dto.description)
        assertEquals(67890L, dto.timestamp)
    }
}
