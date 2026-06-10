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
                    val linkText = element.text().trim()
                    // EL TRUCO: Le pedimos al bot que lea el contenedor "padre" para capturar el texto que rodea al enlace
                    val fullLineText = element.parent()?.text() ?: linkText
                    val matchUrl = element.attr("abs:href")

                    if (linkText.isNotEmpty() && matchUrl.startsWith("http")) {
                        val isLiveEvent = linkText.contains(" vs ", ignoreCase = true) || 
                                          linkText.contains(" v ", ignoreCase = true) || 
                                          linkText.contains(" - ")
                        
                        if (isLiveEvent && !matchUrl.contains("javascript") && linkText.length > 4) {
                            
                            var matchTime = "En Vivo"
                            // Limpiamos el título por si algún guion o número extraño se coló en el nombre de los equipos
                            val cleanTitle = linkText.replace(timeRegex, "").replace(Regex("""^[\s\-:|]+"""), "").trim()

                            // Buscamos la hora en la LÍNEA COMPLETA, no solo en el enlace
                            val timeMatch = timeRegex.find(fullLineText)
                            if (timeMatch != null) {
                                val extractedTime = timeMatch.value
                                
                                try {
                                    val sourceFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                                    sourceFormat.timeZone = TimeZone.getTimeZone("Europe/Madrid")
                                    
                                    val date = sourceFormat.parse(extractedTime)
                                    
                                    if (date != null) {
                                        val localFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
                                        localFormat.timeZone = TimeZone.getDefault()
                                        matchTime = localFormat.format(date)
                                    }
                                } catch (e: Exception) {
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
