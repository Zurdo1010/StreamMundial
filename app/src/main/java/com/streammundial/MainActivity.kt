package com.streammundial

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.streammundial.models.Match
import com.streammundial.utils.MatchScheduler
import com.streammundial.utils.StreamScraper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AppNavigation()
                }
            }
        }
    }
}

@Composable
fun AppNavigation() {
    var selectedStreamUrl by remember { mutableStateOf<String?>(null) }

    if (selectedStreamUrl == null) {
        MatchListScreen(onStreamFound = { url -> selectedStreamUrl = url })
    } else {
        VideoPlayerScreen(streamUrl = selectedStreamUrl!!, onBack = { selectedStreamUrl = null })
    }
}

@Composable
fun MatchListScreen(onStreamFound: (String) -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    var matches by remember { mutableStateOf<List<Match>>(emptyList()) }
    var isLoadingAgenda by remember { mutableStateOf(true) }
    
    // Variables para el rastreo del partido individual
    var scrapingMatch by remember { mutableStateOf<Match?>(null) }
    var resultMessage by remember { mutableStateOf("") }

    // Al abrir la app, el bot descarga la agenda de hoy
    LaunchedEffect(Unit) {
        val todaysMatches = MatchScheduler.getTodaysMatches()
        matches = todaysMatches
        isLoadingAgenda = false
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Partidos de Hoy", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        if (isLoadingAgenda) {
            CircularProgressIndicator(modifier = Modifier.padding(32.dp))
            Text("Leyendo la cartelera deportiva...")
        } else if (matches.isEmpty()) {
            Text("No se encontraron partidos programados para hoy en la cartelera principal.")
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(matches) { match ->
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = match.title, style = MaterialTheme.typography.titleMedium)
                            Text(text = match.time, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                            Spacer(modifier = Modifier.height(8.dp))

                            val isThisMatchScraping = scrapingMatch == match
                            
                            Button(
                                onClick = {
                                    scrapingMatch = match
                                    resultMessage = "Buscando enlaces para ${match.title}..."
                                    
                                    // Inicia el rastreo específico para este partido
                                    coroutineScope.launch {
                                        val links = StreamScraper.findStreamsInPage(match.sourceUrl)
                                        scrapingMatch = null
                                        if (links.isNotEmpty()) {
                                            resultMessage = ""
                                            onStreamFound(links.first())
                                        } else {
                                            resultMessage = "El bot no encontró enlaces directos para este partido."
                                        }
                                    }
                                },
                                enabled = scrapingMatch == null, // Bloquea los botones mientras busca uno
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(if (isThisMatchScraping) "Rastreando..." else "Buscar Transmisión")
                            }
                        }
                    }
                }
            }
            // Mensaje de estado del rastreo en la parte inferior
            if (resultMessage.isNotEmpty() || scrapingMatch != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(resultMessage, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun VideoPlayerScreen(streamUrl: String, onBack: () -> Unit) {
    val context = LocalContext.current
    BackHandler { onBack() }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            val mediaItem = MediaItem.fromUri(streamUrl)
            setMediaItem(mediaItem)
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(Unit) {
        onDispose { exoPlayer.release() }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Button(onClick = onBack, modifier = Modifier.padding(8.dp)) {
            Text("← Regresar")
        }
        AndroidView(
            factory = { ctx -> PlayerView(ctx).apply { player = exoPlayer } },
            modifier = Modifier.fillMaxSize()
        )
    }
}
