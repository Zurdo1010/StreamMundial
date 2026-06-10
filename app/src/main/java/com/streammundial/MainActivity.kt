package com.streammundial

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
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

@Composable
fun AppNavigation() {
    var selectedChannel by remember { mutableStateOf<Channel?>(null) }

    if (selectedChannel == null) {
        ChannelListScreen(onChannelSelected = { canalTocado -> 
            selectedChannel = canalTocado 
        })
    } else {
        VideoPlayerScreen(
            channel = selectedChannel!!,
            onBack = { selectedChannel = null }
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
                // ESTA ES LA MAGIA: Variable para saber si el control remoto está apuntando aquí
                var isFocused by remember { mutableStateOf(false) }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { focusState -> isFocused = focusState.isFocused }
                        // Cambiamos el color de fondo si está enfocado
                        .background(if (isFocused) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                        .clickable { onChannelSelected(channel) }
                        .padding(16.dp)
                ) {
                    Text(
                        text = channel.name, 
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isFocused) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = channel.group, 
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isFocused) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
                }
            }
        }
    }
}

@Composable
fun VideoPlayerScreen(channel: Channel, onBack: () -> Unit) {
    val context = LocalContext.current
    BackHandler { onBack() }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            val mediaItem = MediaItem.fromUri(channel.streamUrl)
            setMediaItem(mediaItem)
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Al botón de regresar también le ponemos foco para salir del video con el control
        var isButtonFocused by remember { mutableStateOf(false) }
        
        Button(
            onClick = onBack, 
            modifier = Modifier
                .padding(8.dp)
                .onFocusChanged { isButtonFocused = it.isFocused },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isButtonFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
            )
        ) {
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
