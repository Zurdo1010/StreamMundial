package com.streammundial.utils

import com.streammundial.models.Channel

object M3UParser {
    
    // Esta función recibe el texto completo de tu lista y lo convierte en canales
    fun parse(m3uContent: String): List<Channel> {
        val channels = mutableListOf<Channel>()
        val lines = m3uContent.lines()
        
        var currentName = "Canal Desconocido"
        var currentLogo = ""
        var currentGroup = "Sin categoría"

        for (line in lines) {
            val trimmed = line.trim()
            
            // Si la línea tiene información del canal (empieza con #EXTINF:)
            if (trimmed.startsWith("#EXTINF:")) {
                // Extraer el nombre (lo que está después de la última coma)
                currentName = trimmed.substringAfterLast(",").trim()
                
                // Extraer el logo si existe
                if (trimmed.contains("tvg-logo=\"")) {
                    currentLogo = trimmed.substringAfter("tvg-logo=\"").substringBefore("\"")
                }
                
                // Extraer el grupo o categoría si existe
                if (trimmed.contains("group-title=\"")) {
                    currentGroup = trimmed.substringAfter("group-title=\"").substringBefore("\"")
                }
            } 
            // Si la línea no está vacía y no es un comentario, es el enlace del video
            else if (trimmed.isNotEmpty() && !trimmed.startsWith("#")) {
                channels.add(Channel(currentName, trimmed, currentGroup, currentLogo))
                
                // Reseteamos las variables para el siguiente canal
                currentName = "Canal Desconocido"
                currentLogo = ""
                currentGroup = "Sin categoría"
            }
        }
        return channels
    }
}
