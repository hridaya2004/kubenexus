package dev.hridaya.kubenexus.data.mapper

import dev.hridaya.kubenexus.data.dto.SampleItemDto
import dev.hridaya.kubenexus.domain.model.SampleItem

fun SampleItemDto.toDomain(): SampleItem {
    return SampleItem(
        id = id,
        title = title,
        description = description,
        timestamp = timestamp
    )
}

fun SampleItem.toDto(): SampleItemDto {
    return SampleItemDto(
        id = id,
        title = title,
        description = description,
        timestamp = timestamp
    )
}

fun List<SampleItemDto>.toDomainList(): List<SampleItem> = map { it.toDomain() }
