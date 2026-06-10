package com.streammundial.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

object StreamScraper {
    
    // Esta función recibe una página web, la escanea y devuelve los enlaces de video
    suspend fun findStreamsInPage(url: String): List<String> = withContext(Dispatchers.IO) {
        val foundStreams = mutableListOf<String>()
        
        try {
            // 1. El bot se disfraza de Google Chrome en Windows para que no lo bloqueen
            val document = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120.0.0.0 Safari/537.36")
                .timeout(10000)
                .get()

            // 2. Extraemos todo el código interno de la página
            val htmlContent = document.html()
            
            // 3. Cazamos los enlaces: Buscamos cualquier texto que parezca un link y termine en .m3u8
            val regex = """(https?://[^\s"'<>]+?\.m3u8[^\s"'<>]*)""".toRegex()
            val matches = regex.findAll(htmlContent)

            // 4. Guardamos todos los resultados que encontró
            for (match in matches) {
                foundStreams.add(match.value)
            }
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        // Devolvemos la lista limpia, quitando los enlaces duplicados
        return@withContext foundStreams.distinct()
    }
}
