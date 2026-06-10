package com.streammundial.utils

import com.streammundial.models.Match
import com.streammundial.models.TvChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

object MatchScheduler {
    
    // EXTRACCIÓN MEJORADA DE PARTIDOS Y HORARIOS
    suspend fun getTodaysMatches(): List<Match> = withContext(Dispatchers.IO) {
        val matches = mutableListOf<Match>()
        val targetUrls = listOf(
            "https://www.noveopartidos.xyz/",
            "http://pirlotv.fr/",
            "https://www.rojadirecto.blog/"
        )

        // Buscador flexible de horas (acepta puntos, dos puntos o guiones como 20:45 o 18.30)
        val timeRegex = Regex("""([0-2]?[0-9])[:.\-]([0-5][0-9])""")

        for (url in targetUrls) {
            try {
                val document = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .timeout(10000)
                    .followRedirects(true)
                    .get()

                val elements = document.select("a[href]")
                
                for (element in elements) {
                    val linkText = element.text().trim()
                    val matchUrl = element.attr("abs:href")

                    if (linkText.isNotEmpty() && matchUrl.startsWith("http")) {
                        val isLiveEvent = linkText.contains(" vs ", ignoreCase = true) || 
                                          linkText.contains(" v ", ignoreCase = true) || 
                                          linkText.contains(" - ")
                        
                        if (isLiveEvent && !matchUrl.contains("javascript") && linkText.length > 4) {
                            var matchTime = "En Vivo"
                            
                            // Agregamos un radar más amplio: escaneamos el enlace, el texto del padre y elementos hermanos
                            val parentText = element.parent()?.text() ?: ""
                            val siblingText = element.previousElementSibling()?.text() ?: ""
                            val contextText = "$siblingText | $linkText | $parentText"

                            val timeMatch = timeRegex.find(contextText)
                            if (timeMatch != null) {
                                val hour = timeMatch.groups[1]?.value?.toIntOrNull() ?: 0
                                val minute = timeMatch.groups[2]?.value?.toIntOrNull() ?: 0
                                
                                try {
                                    val sourceFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                                    sourceFormat.timeZone = TimeZone.getTimeZone("Europe/Madrid")
                                    
                                    val formattedStr = String.format(Locale.US, "%02d:%02d", hour, minute)
                                    val date = sourceFormat.parse(formattedStr)
                                    
                                    if (date != null) {
                                        val localFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
                                        localFormat.timeZone = TimeZone.getDefault()
                                        matchTime = localFormat.format(date)
                                    }
                                } catch (e: Exception) {
                                    matchTime = String.format(Locale.US, "%02d:%02d", hour, minute)
                                }
                            }

                            val cleanTitle = linkText.replace(timeRegex, "")
                                .replace(Regex("""^[\s\-:|]+"""), "").trim()

                            matches.add(Match(title = cleanTitle, time = matchTime, sourceUrl = matchUrl))
                        }
                    }
                }
                if (matches.size > 5) break // Si ya recolectamos suficiente cartelera limpia, avanzamos
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return@withContext matches.distinctBy { it.title }
    }

    // NUEVO: SECCIÓN DE NAVEGACIÓN POR CANALES CONSTANTES
    suspend fun getLiveChannels(): List<TvChannel> = withContext(Dispatchers.IO) {
        val channels = mutableListOf<TvChannel>()
        try {
            // El bot entra a buscar el menú lateral o barra de canales fijas
            val document = Jsoup.connect("https://www.noveopartidos.xyz/")
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .timeout(8000)
                .get()

            val elements = document.select("a[href]")
            // Filtro para identificar nombres de canales deportivos estables
            val keywords = listOf("espn", "tudn", "fox", "tyc", "directv", "win", "sky", "clarosports", "bein", "movistar")

            for (element in elements) {
                val name = element.text().trim()
                val channelUrl = element.attr("abs:href")

                if (name.isNotEmpty() && channelUrl.startsWith("http") && name.length < 20) {
                    val isChannel = keywords.any { name.contains(it, ignoreCase = true) }
                    if (isChannel) {
                        channels.add(TvChannel(name = name, sourceUrl = channelUrl))
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Si la red falla o el sitio está protegido, dejamos un respaldo de canales clave asegurado
        if (channels.isEmpty()) {
            channels.add(TvChannel("TUDN", "https://www.noveopartidos.xyz/"))
            channels.add(TvChannel("ESPN 1", "https://www.noveopartidos.xyz/"))
            channels.add(TvChannel("ESPN 2", "https://www.noveopartidos.xyz/"))
            channels.add(TvChannel("Fox Sports 1", "https://www.noveopartidos.xyz/"))
            channels.add(TvChannel("Fox Sports 2", "https://www.noveopartidos.xyz/"))
            channels.add(TvChannel("TyC Sports", "https://www.noveopartidos.xyz/"))
        }

        return@withContext channels.distinctBy { it.name }
    }
}
