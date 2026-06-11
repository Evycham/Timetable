package com.example.timetable.view.components.common

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp
import com.example.timetable.view.json.MockLogic
import kotlin.math.sin

/**
 * Animierter Hintergrund, der sich bewegende Wellen auf einem Canvas zeichnet.
 *
 * @param accentColor Die Basisfarbe, die als Grundlage für die halbtransparenten Wellenverläufe dient.
 * Wenn `null`, wird die primäre Farbe des aktuellen `MaterialTheme` oder der aktive Studiengang aus `MockLogic` verwendet.
 */
@Composable
fun AnimatedBackground(
    accentColor: Color? = null
) {
    // set up infinite transition for horizontal wave movement
    val infiniteTransition = rememberInfiniteTransition(label = "WaveTime")

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

    // base color defaults to active faculty color from MockLogic or primary theme color
    val baseColor = accentColor ?: MockLogic.activeFacultyColor ?: MaterialTheme.colorScheme.primary

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
                    baselineFraction = 0.65f,
                    amplitude = 45f,
                    frequency = 0.005f,
                    speedFactor = 1,
                    harmonicSpeedFactor = -1,
                    colorStart = animatedBaseColor.copy(alpha = 0.12f),
                    colorEnd = animatedBaseColor.copy(alpha = 0.02f)
                ),
                WaveConfig(
                    baselineFraction = 0.70f,
                    amplitude = 30f,
                    frequency = 0.008f,
                    speedFactor = -1, // moves in reverse direction
                    harmonicSpeedFactor = 2,
                    colorStart = animatedBaseColor.copy(alpha = 0.08f),
                    colorEnd = animatedBaseColor.copy(alpha = 0.01f)
                ),
                WaveConfig(
                    baselineFraction = 0.75f,
                    amplitude = 20f,
                    frequency = 0.003f,
                    speedFactor = 2,
                    harmonicSpeedFactor = -1,
                    colorStart = animatedBaseColor.copy(alpha = 0.15f),
                    colorEnd = animatedBaseColor.copy(alpha = 0.03f)
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
                        val angle1 = (x * config.frequency) + (time * config.speedFactor)
                        val angle2 =
                            (x * config.frequency * 1.8f) - (time * config.harmonicSpeedFactor)

                        val y = baseline +
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
