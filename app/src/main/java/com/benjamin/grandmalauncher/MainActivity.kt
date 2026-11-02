package com.benjamin.grandmalauncher

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.util.Locale

private val Ink = Color(0xFF090909)
private val Panel = Color(0xFF1B1719)
private val Rose = Color(0xFFB51F3A)
private val Paper = Color(0xFFFFF4F5)

class MainActivity : ComponentActivity() {
    private val locationPermission = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GrandmaLauncher(
                onRequestLocation = {
                    locationPermission.launch(arrayOf(
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ))
                },
                hasLocationPermission = {
                    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                },
                getLocation = {
                    val client = LocationServices.getFusedLocationProviderClient(this)
                    try { client.lastLocation.await() } catch (_: SecurityException) { null }
                }
            )
        }
    }
}

private data class AppShortcut(val label: String, val icon: ImageVector, val tint: Color)

@Composable
private fun GrandmaLauncher(
    onRequestLocation: () -> Unit,
    hasLocationPermission: () -> Boolean,
    getLocation: suspend () -> Location?
) {
    val scope = rememberCoroutineScope()
    var weather by remember { mutableStateOf("Weather will appear here") }
    var details by remember { mutableStateOf("Allow location while using the app") }
    var showLibrary by remember { mutableStateOf(false) }

    val shortcuts = listOf(
        AppShortcut("YouTube", Icons.Default.PlayArrow, Rose),
        AppShortcut("Messages", Icons.Default.Message, Color(0xFF4B9BFF)),
        AppShortcut("Phone", Icons.Default.Call, Color(0xFF54B875)),
        AppShortcut("Camera", Icons.Default.CameraAlt, Color(0xFFE18A9B)),
        AppShortcut("Photos", Icons.Default.PhotoLibrary, Color(0xFFCB668D)),
        AppShortcut("Calendar", Icons.Default.CalendarMonth, Color(0xFFE6E0E0)),
        AppShortcut("Settings", Icons.Default.Settings, Color(0xFFB9B9C2)),
        AppShortcut("Calculator", Icons.Default.Calculate, Color(0xFF76B98B))
    )

    LaunchedEffect(Unit) {
        if (!hasLocationPermission()) onRequestLocation()
        else {
            val loc = getLocation()
            if (loc != null) {
                try {
                    val result = withContext(Dispatchers.IO) {
                        val url = URL("https://api.open-meteo.com/v1/forecast?latitude=${loc.latitude}&longitude=${loc.longitude}&current=temperature_2m,apparent_temperature,weather_code,wind_speed_10m&daily=temperature_2m_max,temperature_2m_min&temperature_unit=fahrenheit&wind_speed_unit=mph&timezone=auto")
                        JSONObject(url.readText())
                    }
                    val current = result.getJSONObject("current")
                    val daily = result.getJSONObject("daily")
                    weather = "${current.getDouble("temperature_2m").toInt()}°F"
                    details = "Feels like ${current.getDouble("apparent_temperature").toInt()}° · High ${daily.getJSONArray("temperature_2m_max").getDouble(0).toInt()}° · Low ${daily.getJSONArray("temperature_2m_min").getDouble(0).toInt()}°"
                } catch (_: Exception) {
                    weather = "Weather unavailable"
                    details = "Check your internet connection"
                }
            } else {
                details = "Turn on location to see local weather"
            }
        }
    }

    MaterialTheme(colorScheme = darkColorScheme(
        background = Ink, surface = Panel, primary = Rose, onPrimary = Paper,
        onBackground = Paper, onSurface = Paper
    )) {
        Surface(Modifier.fillMaxSize(), color = Ink) {
            Column(
                Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Grandma's Home", color = Paper, style = MaterialTheme.typography.titleMedium)
                    Text("●  ▰", color = Paper)
                }
                Spacer(Modifier.height(24.dp))
                Text("8:15", color = Paper, fontSize = 58.sp)
                Text("Sunday  •  November 1", color = Paper.copy(alpha = .8f))
                Spacer(Modifier.height(20.dp))
                Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(22.dp)) {
                    Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Cloud, contentDescription = null, tint = Rose, modifier = Modifier.size(38.dp))
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text(weather, color = Paper, style = MaterialTheme.typography.headlineSmall)
                            Text(details, color = Paper.copy(alpha = .75f), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                Spacer(Modifier.height(22.dp))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    items(shortcuts) { app ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Surface(
                                onClick = { },
                                color = Panel,
                                shape = RoundedCornerShape(18.dp),
                                modifier = Modifier.size(66.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(app.icon, contentDescription = app.label, tint = app.tint, modifier = Modifier.size(32.dp))
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(app.label, color = Paper, fontSize = 11.sp, maxLines = 1)
                        }
                    }
                }
                Button(
                    onClick = { showLibrary = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Rose),
                    modifier = Modifier.fillMaxWidth().height(54.dp)
                ) {
                    Icon(Icons.Default.Apps, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("All Apps", fontSize = 17.sp)
                }
                TextButton(onClick = onRequestLocation) {
                    Text("Refresh weather location", color = Paper.copy(alpha = .7f))
                }
            }
        }
    }
}
