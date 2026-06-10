package com.streammundial.utils

import com.streammundial.models.Match
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

object MatchScheduler {
    
    suspend fun getTodaysMatches(): List<Match> = withContext(Dispatchers.IO) {
        val matches = mutableListOf<Match>()
        
        try {
            // El bot entra a la cartelera principal de PirloTV
            val document = Jsoup.connect("http://pirlotv.fr/")
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .timeout(10000)
                .get()

            // Buscamos todos los enlaces de la portada
            val elements = document.select("a[href]")
            
            for (element in elements) {
                val title = element.text()
                val url = element.attr("href")
                
                // Filtramos: Solo tomamos los enlaces que tengan un "vs" o un guion 
                // y que parezcan apuntar a una página de transmisión interna
                if ((title.contains(" vs ", ignoreCase = true) || title.contains(" - ")) && url.length > 10) {
                    matches.add(
                        Match(
                            title = title.trim(),
                            time = "Hoy", // La cartelera principal siempre es del día
                            sourceUrl = url
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        // Devolvemos la lista limpia, eliminando partidos duplicados
        return@withContext matches.distinctBy { it.title }
    }
}
