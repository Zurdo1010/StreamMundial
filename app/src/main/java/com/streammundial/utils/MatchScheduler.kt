package com.streammundial.utils

import com.streammundial.models.Match
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

object MatchScheduler {
    
    suspend fun getTodaysMatches(): List<Match> = withContext(Dispatchers.IO) {
        val matches = mutableListOf<Match>()
        
        // El arsenal completo: El bot buscará en estas tres páginas en orden
        val targetUrls = listOf(
            "https://www.noveopartidos.xyz/",
            "http://pirlotv.fr/",
            "https://www.rojadirecto.blog/"
        )

        for (url in targetUrls) {
            try {
                // Conectamos al sitio haciéndonos pasar por un navegador real
                val document = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .timeout(10000)
                    .followRedirects(true)
                    .get()

                // Recolectamos todos los enlaces de la página
                val elements = document.select("a[href]")
                
                for (element in elements) {
                    val title = element.text().trim()
                    val matchUrl = element.attr("abs:href")

                    if (title.isNotEmpty() && matchUrl.startsWith("http")) {
                        // Filtro: buscamos partidos por las palabras clave
                        val isLiveEvent = title.contains(" vs ", ignoreCase = true) || 
                                          title.contains(" v ", ignoreCase = true) || 
                                          title.contains(" - ")
                        
                        if (isLiveEvent && !matchUrl.contains("javascript") && title.length > 4) {
                            matches.add(
                                Match(
                                    title = title,
                                    time = "En Vivo",
                                    sourceUrl = matchUrl
                                )
                            )
                        }
                    }
                }

                // Si encontramos la cartelera, detenemos la búsqueda para ahorrar recursos
                if (matches.isNotEmpty()) {
                    break
                }

            } catch (e: Exception) {
                e.printStackTrace()
                // Si la página se cae, el bucle "for" salta automáticamente a la siguiente URL
            }
        }
        
        // Entregamos la lista final sin juegos repetidos
        return@withContext matches.distinctBy { it.title }
    }
}
