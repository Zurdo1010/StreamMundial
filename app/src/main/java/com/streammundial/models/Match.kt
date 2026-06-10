package com.streammundial.models

data class Match(
    val title: String,           // Ejemplo: "América vs Atlas"
    val time: String,            // Ejemplo: "20:00 hrs"
    val sourceUrl: String,       // La página web secreta que el bot va a escanear
    var streams: List<String> = emptyList() // Aquí se guardarán los enlaces .m3u8 que el bot encuentre
)
