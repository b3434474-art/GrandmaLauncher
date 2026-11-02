package com.benjamin.grandmalauncher

import android.Manifest
import android.app.WallpaperManager
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Ink = Color(0xFF090708)
private val Panel = Color(0xE61A1115)
private val Rose = Color(0xFFB51F3A)
private val Paper = Color(0xFFFFF4F5)

class MainActivity : ComponentActivity() {
    private val permissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var permissionVersion by remember { mutableIntStateOf(0) }
            GrandmaLauncher(
                permissionVersion,
                requestPermission = {
                    permissions.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION))
                },
                locationAllowed = {
                    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                },
                location = {
                    try { LocationServices.getFusedLocationProviderClient(this).lastLocation.await() }
                    catch (_: SecurityException) { null }
                },
                openApp = { packageManager.getLaunchIntentForPackage(it)?.let(::startActivity) },
                currentWallpaper = { try { WallpaperManager.getInstance(this).drawable } catch (_: Exception) { null } }
            )
        }
    }
}

private data class AppEntry(val name: String, val packageName: String, val icon: Drawable)
private data class Tile(val name: String, val icon: ImageVector, val tint: Color)

@Composable
private fun GrandmaLauncher(
    permissionVersion: Int,
    requestPermission: () -> Unit,
    locationAllowed: () -> Boolean,
    location: suspend () -> android.location.Location?,
    openApp: (String) -> Unit,
    currentWallpaper: () -> Drawable?
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var temperature by remember { mutableStateOf("Weather") }
    var forecast by remember { mutableStateOf("Allow location to see local weather") }
    var status by remember { mutableStateOf("") }
    var apps by remember { mutableStateOf(emptyList<AppEntry>()) }
    var library by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var wallpaper by remember { mutableStateOf<Drawable?>(null) }

    val tiles = listOf(
        Tile("YouTube", Icons.Default.PlayArrow, Color(0xFFFF304A)),
        Tile("Messages", Icons.Default.Message, Color(0xFF78B5FF)),
        Tile("Phone", Icons.Default.Call, Color(0xFF70D28E)),
        Tile("Camera", Icons.Default.CameraAlt, Color(0xFFFF91AD)),
        Tile("Photos", Icons.Default.PhotoLibrary, Color(0xFFE66C9D)),
        Tile("Calendar", Icons.Default.CalendarMonth, Color(0xFFFFD5DF)),
        Tile("Settings", Icons.Default.Settings, Color(0xFFD0C3C8)),
        Tile("Calculator", Icons.Default.Calculate, Color(0xFF83D5A1))
    )
    val packages = mapOf(
        "YouTube" to listOf("com.google.android.youtube"),
        "Messages" to listOf("com.google.android.apps.messaging", "com.samsung.android.messaging"),
        "Phone" to listOf("com.google.android.dialer", "com.samsung.android.dialer"),
        "Camera" to listOf("com.android.camera2", "com.sec.android.app.camera"),
        "Photos" to listOf("com.google.android.apps.photos", "com.sec.android.gallery3d"),
        "Calendar" to listOf("com.google.android.calendar", "com.samsung.android.calendar"),
        "Settings" to listOf("com.android.settings"),
        "Calculator" to listOf("com.google.android.calculator", "com.sec.android.app.popupcalculator")
    )

    suspend fun updateWeather() {
        if (!locationAllowed()) { forecast = "Tap here to allow location"; return }
        try {
            val point = location()
            if (point == null) { forecast = "Turn on phone location, then refresh"; return }
            val json = withContext(Dispatchers.IO) {
                val endpoint = "https://api.open-meteo.com/v1/forecast?latitude=${point.latitude}&longitude=${point.longitude}&current=temperature_2m,apparent_temperature,weather_code&daily=temperature_2m_max,temperature_2m_min&temperature_unit=fahrenheit&timezone=auto"
                JSONObject(URL(endpoint).readText())
            }
            val now = json.getJSONObject("current")
            val day = json.getJSONObject("daily")
            val code = now.getInt("weather_code")
            val sky = when (code) {
                0 -> "Clear"
                1, 2 -> "Partly cloudy"
                3 -> "Cloudy"
                45, 48 -> "Foggy"
                in 51..67, in 80..82 -> "Rain"
                in 71..77, 85, 86 -> "Snow"
                in 95..99 -> "Thunderstorms"
                else -> "Current conditions"
            }
            temperature = "${now.getDouble("temperature_2m").toInt()}°F"
            forecast = "$sky · Feels ${now.getDouble("apparent_temperature").toInt()}° · H ${day.getJSONArray("temperature_2m_max").getDouble(0).toInt()}° / L ${day.getJSONArray("temperature_2m_min").getDouble(0).toInt()}°"
            status = "Weather updated"
        } catch (_: Exception) { forecast = "Weather unavailable · Check internet" }
    }

    LaunchedEffect(Unit) {
        wallpaper = currentWallpaper()
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        apps = context.packageManager.queryIntentActivities(intent, 0).mapNotNull { info ->
            val label = info.loadLabel(context.packageManager)?.toString() ?: return@mapNotNull null
            AppEntry(label, info.activityInfo.packageName, info.loadIcon(context.packageManager))
        }.distinctBy { it.packageName }.sortedBy { it.name.lowercase(Locale.getDefault()) }
    }
    LaunchedEffect(permissionVersion) { updateWeather() }

    val date = remember { SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date()) }
    val time = remember { SimpleDateFormat("h:mm", Locale.getDefault()).format(Date()) }

    MaterialTheme(colorScheme = darkColorScheme(primary = Rose, background = Ink, surface = Panel, onSurface = Paper)) {
        Box(Modifier.fillMaxSize().background(Ink)) {
            wallpaper?.let { drawable ->
                Image(remember(drawable) { drawable.toBitmap().asImageBitmap() }, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .48f)))
            }
            Column(Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(12.dp))
                Text(time, color = Paper, fontSize = 56.sp)
                Text(date, color = Paper, fontSize = 16.sp)
                Spacer(Modifier.height(18.dp))
                Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(22.dp)) {
                    Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Cloud, null, tint = Rose, modifier = Modifier.size(38.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(temperature, color = Paper, fontSize = 27.sp)
                            Text(forecast, color = Paper, fontSize = 12.sp)
                            Text(status, color = Paper.copy(alpha = .65f), fontSize = 10.sp)
                        }
                        IconButton(onClick = { scope.launch { updateWeather() } }) { Icon(Icons.Default.Refresh, "Refresh weather", tint = Paper) }
                    }
                }
                Spacer(Modifier.height(18.dp))
                LazyVerticalGrid(GridCells.Fixed(4), Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(tiles) { tile ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Surface(
                                onClick = {
                                    val candidates = packages[tile.name].orEmpty()
                                    val found = candidates.firstNotNullOfOrNull { context.packageManager.getLaunchIntentForPackage(it) }
                                    found?.let(context::startActivity)
                                },
                                color = Panel, shape = RoundedCornerShape(19.dp), modifier = Modifier.size(66.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(tile.icon, tile.name, tint = tile.tint, modifier = Modifier.size(34.dp))
                                }
                            }
                            Spacer(Modifier.height(5.dp))
                            Text(tile.name, color = Paper, fontSize = 11.sp, maxLines = 1)
                        }
                    }
                }
                Button(onClick = { library = true }, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(18.dp), colors = ButtonDefaults.buttonColors(containerColor = Rose)) {
                    Icon(Icons.Default.Apps, null)
                    Spacer(Modifier.width(8.dp))
                    Text("App Library", fontSize = 18.sp)
                }
                TextButton(onClick = requestPermission) { Text("Enable / refresh weather location", color = Paper) }
            }
            if (library) {
                AlertDialog(
                    onDismissRequest = { library = false; query = "" },
                    containerColor = Ink,
                    title = { Text("App Library", color = Paper) },
                    text = {
                        Column(Modifier.fillMaxWidth().heightIn(max = 520.dp)) {
                            BasicTextField(
                                value = query, onValueChange = { query = it }, singleLine = true,
                                textStyle = TextStyle(color = Paper, fontSize = 16.sp),
                                decorationBox = { inner ->
                                    Row(Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(12.dp)).padding(12.dp)) {
                                        Icon(Icons.Default.Search, null, tint = Paper)
                                        Spacer(Modifier.width(8.dp))
                                        Box { if (query.isEmpty()) Text("Search all apps", color = Paper.copy(alpha = .6f)); inner() }
                                    }
                                }
                            )
                            Spacer(Modifier.height(12.dp))
                            LazyVerticalGrid(GridCells.Fixed(3), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                items(apps.filter { it.name.contains(query, true) }) { app ->
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Surface(onClick = { openApp(app.packageName); library = false }, color = Panel, shape = RoundedCornerShape(15.dp), modifier = Modifier.size(58.dp)) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Image(remember(app.packageName) { app.icon.toBitmap(96, 96).asImageBitmap() }, app.name, Modifier.size(38.dp))
                                            }
                                        }
                                        Text(app.name, color = Paper, fontSize = 11.sp, maxLines = 1)
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = { TextButton(onClick = { library = false; query = "" }) { Text("Close", color = Rose) } }
                )
            }
        }
    }
}
