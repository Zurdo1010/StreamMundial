package com.streammundial.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

object StreamScraper {
    
    suspend fun findStreamsInPage(url: String): List<String> = withContext(Dispatchers.IO) {
        val foundStreams = mutableListOf<String>()
        // Nuestra "lupa" para cazar enlaces directos de video
        val regex = """(https?://[^\s"'<>]+?\.m3u8[^\s"'<>]*)""".toRegex()

        try {
            // 1er Salto: Entramos a la página principal (ej. pirlotv o noveopartidos)
            val document = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120.0.0.0 Safari/537.36")
                .timeout(10000)
                .get()

            // Buscamos enlaces sueltos en la página principal
            foundStreams.addAll(regex.findAll(document.html()).map { it.value })

            // 2do Salto: Buscamos "ventanas" (iframes) donde suelen esconder los videos
            val iframes = document.select("iframe")
            for (iframe in iframes) {
                val iframeSrc = iframe.attr("src")
                if (iframeSrc.isNotEmpty() && iframeSrc.startsWith("http")) {
                    try {
                        // El bot entra a la ventana oculta
                        val iframeDoc = Jsoup.connect(iframeSrc)
                            .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                            .timeout(5000)
                            .get()
                        
                        // Extraemos los enlaces de la ventana oculta
                        foundStreams.addAll(regex.findAll(iframeDoc.html()).map { it.value })
                    } catch (e: Exception) {
                        // Si una ventana falsa falla, la ignoramos y seguimos buscando
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        // Devolvemos la lista limpia, sin enlaces repetidos
        return@withContext foundStreams.distinct()
    }
}
