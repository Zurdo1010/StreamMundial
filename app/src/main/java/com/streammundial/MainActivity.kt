package com.streammundial

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
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
    var isScraping by remember { mutableStateOf(false) }
    var resultMessage by remember { mutableStateOf("") }

    // Tarjeta de prueba para el rastreador
    val testMatch = Match(
        title = "Club América vs Atlas FC",
        time = "EN VIVO",
        sourceUrl = "http://pirlotv.fr" // El bot atacará esta url
    )

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Partidos de Hoy", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(24.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = testMatch.title, style = MaterialTheme.typography.titleLarge)
                Text(text = testMatch.time, color = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        isScraping = true
                        resultMessage = "El bot está escaneando la página..."
                        coroutineScope.launch {
                            val links = StreamScraper.findStreamsInPage(testMatch.sourceUrl)
                            isScraping = false
                            if (links.isNotEmpty()) {
                                resultMessage = "¡Enlace encontrado! Abriendo reproductor..."
                                onStreamFound(links.first()) // Mandamos el primer link al reproductor
                            } else {
                                resultMessage = "El bot no encontró enlaces directos en esta URL."
                            }
                        }
                    },
                    enabled = !isScraping,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isScraping) "Rastreando..." else "Buscar Transmisión")
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text(resultMessage)
        if (isScraping) {
            CircularProgressIndicator(modifier = Modifier.padding(top = 16.dp))
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
