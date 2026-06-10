package com.streammundial

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
                    ChannelListScreen()
                }
            }
        }
    }
}

@Composable
fun ChannelListScreen() {
    // Variables para guardar los canales y saber si está cargando
    var channels by remember { mutableStateOf<List<Channel>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Esto se ejecuta en segundo plano al abrir la app
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                // Descargamos la lista deportiva en tiempo real
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

    // Interfaz visual
    if (isLoading) {
        Text("Buscando transmisiones deportivas...", modifier = Modifier.padding(16.dp))
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(channels) { channel ->
                Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                    Text(text = channel.name, style = MaterialTheme.typography.titleMedium)
                    Text(text = channel.group, style = MaterialTheme.typography.bodySmall)
                    HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
                }
            }
        }
    }
}
