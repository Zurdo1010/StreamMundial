package com.streammundial.models

data class Channel(
    val name: String,
    val streamUrl: String,
    val group: String = "Sin categoría",
    val logoUrl: String = ""
)
