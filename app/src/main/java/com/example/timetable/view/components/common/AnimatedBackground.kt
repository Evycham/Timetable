package com.example.timetable.view.components.common

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlin.math.sin
import androidx.compose.runtime.collectAsState
import com.example.timetable.data.local.preferences.UserSchedulePreferences
import com.example.timetable.data.local.preferences.UserSchedulePreferencesStore
import com.example.timetable.data.local.preferences.userSchedulePreferencesDataStore
import com.example.timetable.utils.enums.Faculty
import com.example.timetable.view.theme.LocalBackgroundAccentColor

/**
 * Animierter Hintergrund, der sich bewegende Wellen auf einem Canvas zeichnet.
 * Inklusive subtilem Parallax-Effekt basierend auf der Neigung des Geräts.
 *
 * @param route Die aktuelle Navigationsroute zur Farbbestimmung.
 * @param accentColor Optionale Akzentfarbe (überschreibt Routen-Farbe).
 */
@Composable
fun AnimatedBackground(
    route: String? = null,
    accentColor: Color? = null
) {
    val context = LocalContext.current
    val preferencesStore = remember { UserSchedulePreferencesStore(context.userSchedulePreferencesDataStore) }
    val preferences by preferencesStore.preferencesFlow.collectAsState(initial = UserSchedulePreferences())
    val localAccentColor = LocalBackgroundAccentColor.current.value
    
    // --- Sensor Logik für Parallax ---
    var rawTiltX by remember { mutableFloatStateOf(0f) }
    var rawTiltY by remember { mutableFloatStateOf(0f) }

    // Glättung der Sensor-Daten für flüssige Bewegung
    val tiltX by animateFloatAsState(
        targetValue = rawTiltX,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "TiltX"
    )
    val tiltY by animateFloatAsState(
        targetValue = rawTiltY,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "TiltY"
    )

    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event != null && event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                    // Werte normalisieren und skalieren für subtilen Effekt (ca. +/- 15dp)
                    rawTiltX = (event.values[0] / 9.81f) * 15f
                    rawTiltY = (event.values[1] / 9.81f) * 15f
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_UI)
        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }
    // ---------------------------------

    // set up infinite transition for horizontal wave movement
    val infiniteTransition = rememberInfiniteTransition(label = "WaveTime")

    // for screen change effect
    val speedMultiplier = remember { Animatable(1f) }

    // accelerate wave movement on route (screen) change
    LaunchedEffect(route) {
        if (route != null) {
            speedMultiplier.animateTo(
                targetValue = 4.0f,
                animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
            )
            speedMultiplier.animateTo(
                targetValue = 1.0f,
                animationSpec = tween(durationMillis = 1800, easing = LinearOutSlowInEasing)
            )
        }
    }

    // animate the phase angle from 0 to 2*pi endlessly
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2 * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(24000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "TimeProgress"
    )

    // base color defaults to faculty color resolved from route/preferences or primary theme color
    val baseColor = accentColor ?: if (preferences.isDynamicColorEnabled) {
        null
    } else {
        localAccentColor ?: remember(route, preferences.groupsCode) {
            val course = route?.substringAfter("timetable/")?.takeIf { !it.contains("{") }
                ?.let { android.net.Uri.decode(it) }
                ?: preferences.groupsCode
            val matchedFaculty = Faculty.entries.find { faculty ->
                course != null && course.startsWith(faculty.prefix, ignoreCase = true)
            }
            matchedFaculty?.color
        }
    } ?: MaterialTheme.colorScheme.primary

    // animate color changes smoothly over 1000ms
    val animatedBaseColor by animateColorAsState(
        targetValue = baseColor,
        animationSpec = tween(durationMillis = 1000),
        label = "BaseColorAnimation"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // draw paths inside blurred canvas to give wave shapes a soft glow effect
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .blur(5.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
        ) {
            val width = size.width
            val height = size.height

            // define configuration for three overlapping wave layers
            // using integer speed factors guarantees seamless loops at time boundary
            val waveConfigs = listOf(
                WaveConfig(
                    baselineFraction = 0.4f,
                    amplitude = 45f,
                    frequency = 0.005f,
                    speedFactor = 1,
                    harmonicSpeedFactor = -1,
                    colorStart = animatedBaseColor.copy(alpha = 0.15f),
                    colorEnd = animatedBaseColor.copy(alpha = 0.01f)
                ),
                WaveConfig(
                    baselineFraction = 0.55f,
                    amplitude = 40f,
                    frequency = 0.008f,
                    speedFactor = -1, // moves in reverse direction
                    harmonicSpeedFactor = 2,
                    colorStart = animatedBaseColor.copy(alpha = 0.2f),
                    colorEnd = animatedBaseColor.copy(alpha = 0.1f)
                ),
                WaveConfig(
                    baselineFraction = 0.75f,
                    amplitude = 35f,
                    frequency = 0.003f,
                    speedFactor = 2,
                    harmonicSpeedFactor = -1,
                    colorStart = animatedBaseColor.copy(alpha = 0.25f),
                    colorEnd = animatedBaseColor.copy(alpha = 0.1f)
                )
            )

            // draw each wave layer by stepping across the screen width
            waveConfigs.forEach { config ->
                val path = Path().apply {
                    moveTo(0f, height)

                    // step by 10px columns to balance rendering quality and performance
                    val stepPx = 10f
                    var x = 0f
                    while (x <= width) {
                        val baseline = height * config.baselineFraction

                        // sum of two sine waves at integer speed multipliers avoids phase jump at loop reset
                        // tiltX verschiebt die Phase horizontal, tiltY verschiebt die Baseline vertikal
                        val angle1 =
                            ((x + tiltX * config.speedFactor * 10f) * config.frequency) + (time * config.speedFactor * speedMultiplier.value)
                        val angle2 =
                            (x * config.frequency * 1.8f) - (time * config.harmonicSpeedFactor * speedMultiplier.value)

                        val y = baseline + (tiltY * config.speedFactor * 3f) +
                                (config.amplitude * sin(angle1)) +
                                (config.amplitude * 0.4f * sin(angle2))

                        lineTo(x, y)
                        x += stepPx
                    }

                    // close path at the bottom edge to fill the shape
                    lineTo(width, height)
                    close()
                }

                // fill the wave path with a soft vertical gradient
                drawPath(
                    path = path,
                    brush = Brush.verticalGradient(
                        colors = listOf(config.colorStart, config.colorEnd),
                        startY = height * (config.baselineFraction - 0.15f),
                        endY = height
                    )
                )
            }
        }
    }
}

/**
 * Konfigurationseinstellungen für eine einzelne Wellenebene.
 *
 * @property baselineFraction Die relative Y-Position des Wellenmittelpunkts auf dem Bildschirm (0.0 = oben, 1.0 = unten).
 * @property amplitude Die maximale Höhe der Wellenbewegung in Pixeln.
 * @property frequency Die horizontale Frequenz (Wellenlänge) der Sinuskurve.
 * @property speedFactor Der ganzzahlige Geschwindigkeits- und Richtungsmultiplikator für die horizontale Bewegung.
 * @property harmonicSpeedFactor Der ganzzahlige Geschwindigkeitsmultiplikator für die harmonische Oberschwingung.
 * @property colorStart Die Startfarbe für den vertikalen Farbverlauf am Scheitelpunkt der Welle.
 * @property colorEnd Die Endfarbe des Farbverlaufs an der unteren Kante des Bildschirms.
 */
private data class WaveConfig(
    val baselineFraction: Float,
    val amplitude: Float,
    val frequency: Float,
    val speedFactor: Int,
    val harmonicSpeedFactor: Int,
    val colorStart: Color,
    val colorEnd: Color
)
