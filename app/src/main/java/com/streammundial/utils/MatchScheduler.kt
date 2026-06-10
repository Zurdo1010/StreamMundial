package com.streammundial.utils

import com.streammundial.models.Match
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

object MatchScheduler {
    
    suspend fun getTodaysMatches(): List<Match> = withContext(Dispatchers.IO) {
        val matches = mutableListOf<Match>()
        
        try {
            val document = Jsoup.connect("http://pirlotv.fr/")
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .timeout(10000)
                .get()

            val elements = document.select("a[href]")
            
            for (element in elements) {
                val title = element.text()
                val url = element.attr("href")
                
                if ((title.contains(" vs ", ignoreCase = true) || title.contains(" - ")) && url.length > 10) {
                    matches.add(
                        Match(
                            title = title.trim(),
                            time = "Hoy",
                            sourceUrl = url
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        return@withContext matches.distinctBy { it.title }
    }
}
