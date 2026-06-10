package com.streammundial.utils

import com.streammundial.models.Match
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

object MatchScheduler {
    
    suspend fun getTodaysMatches(): List<Match> = withContext(Dispatchers.IO) {
        val matches = mutableListOf<Match>()
        
        val targetUrls = listOf(
            "https://www.noveopartidos.xyz/",
            "http://pirlotv.fr/",
            "https://www.rojadirecto.blog/"
        )

        // Nuestra "lupa" matemática para encontrar cualquier texto que parezca una hora (ej. 14:30 o 09:00)
        val timeRegex = Regex("""([0-1]?[0-9]|2[0-3]):([0-5][0-9])""")

        for (url in targetUrls) {
            try {
                val document = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .timeout(10000)
                    .followRedirects(true)
                    .get()

                val elements = document.select("a[href]")
                
                for (element in elements) {
                    val originalTitle = element.text().trim()
                    val matchUrl = element.attr("abs:href")

                    if (originalTitle.isNotEmpty() && matchUrl.startsWith("http")) {
                        val isLiveEvent = originalTitle.contains(" vs ", ignoreCase = true) || 
                                          originalTitle.contains(" v ", ignoreCase = true) || 
                                          originalTitle.contains(" - ")
                        
                        if (isLiveEvent && !matchUrl.contains("javascript") && originalTitle.length > 4) {
                            
                            var matchTime = "En Vivo"
                            var cleanTitle = originalTitle

                            // 1. Extraemos la hora original del texto
                            val timeMatch = timeRegex.find(originalTitle)
                            if (timeMatch != null) {
                                val extractedTime = timeMatch.value
                                
                                // 2. Limpiamos el título para que no traiga la hora incrustada ni guiones sueltos
                                cleanTitle = originalTitle.replace(extractedTime, "")
                                    .replace(Regex("""^[\s\-:|]+"""), "") // Borra basura al inicio del texto
                                    .trim()

                                // 3. Hacemos la conversión de huso horario
                                try {
                                    // Le indicamos que la página origen suele usar la hora de Madrid (CET)
                                    val sourceFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                                    sourceFormat.timeZone = TimeZone.getTimeZone("Europe/Madrid")
                                    
                                    val date = sourceFormat.parse(extractedTime)
                                    
                                    if (date != null) {
                                        // Lo pasamos a la zona horaria del dispositivo en formato am/pm (ej. 03:00 PM)
                                        val localFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
                                        localFormat.timeZone = TimeZone.getDefault()
                                        matchTime = localFormat.format(date)
                                    }
                                } catch (e: Exception) {
                                    // Si la matemática falla, conservamos la hora original del texto
                                    matchTime = extractedTime 
                                }
                            }

                            matches.add(
                                Match(
                                    title = cleanTitle,
                                    time = matchTime,
                                    sourceUrl = matchUrl
                                )
                            )
                        }
                    }
                }

                if (matches.isNotEmpty()) {
                    break
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        return@withContext matches.distinctBy { it.title }
    }
}
