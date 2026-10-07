package com.nexa.finance.api

import kotlinx.serialization.Serializable

@Serializable
data class CategoryResponse(
    val id: String,
    val name: String,
    val type: String,
    val iconKey: String?,
    val sortOrder: Int,
)
