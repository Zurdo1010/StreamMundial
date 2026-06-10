package com.streammundial

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.streammundial.models.Channel
import com.streammundial.utils.M3UParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
}

// Controlador de pantallas: Decide si muestra la lista o el reproductor
@Composable
fun AppNavigation() {
    var selectedChannel by remember { mutableStateOf<Channel?>(null) }

    if (selectedChannel == null) {
        // Le pasamos la instrucción de qué hacer al tocar un canal
        ChannelListScreen(onChannelSelected = { canalTocado -> 
            selectedChannel = canalTocado 
        })
    } else {
        // Si hay un canal seleccionado, abrimos el reproductor
        VideoPlayerScreen(
            channel = selectedChannel!!,
            onBack = { selectedChannel = null } // Para regresar a la lista
        )
    }
}

@Composable
fun ChannelListScreen(onChannelSelected: (Channel) -> Unit) {
    var channels by remember { mutableStateOf<List<Channel>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                val m3uContent = URL("https://iptv-org.github.io/iptv/categories/sports.m3u").readText()
                val parsedChannels = M3UParser.parse(m3uContent)
                channels = parsedChannels
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
            Text("Buscando transmisiones deportivas...", modifier = Modifier.padding(16.dp))
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(channels) { channel ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onChannelSelected(channel) } // ¡Aquí activamos el toque!
                        .padding(16.dp)
                ) {
                    Text(text = channel.name, style = MaterialTheme.typography.titleMedium)
                    Text(text = channel.group, style = MaterialTheme.typography.bodySmall)
                    HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
                }
            }
        }
    }
}

@Composable
fun VideoPlayerScreen(channel: Channel, onBack: () -> Unit) {
    val context = LocalContext.current
    
    // Detectar cuando toques el botón de "Atrás" de tu celular
    BackHandler { onBack() }

    // Preparar el motor ExoPlayer
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            val mediaItem = MediaItem.fromUri(channel.streamUrl)
            setMediaItem(mediaItem)
            prepare()
            playWhenReady = true // Autoplay
        }
    }

    // Apagar y liberar memoria cuando regreses a la lista
    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    // Interfaz del reproductor
    Column(modifier = Modifier.fillMaxSize()) {
        Button(onClick = onBack, modifier = Modifier.padding(8.dp)) {
            Text("← Regresar a la lista")
        }
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}
