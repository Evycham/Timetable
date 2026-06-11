package com.example.timetable.view.components.common

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

/**
 * Fügt dem Modifier einen animierten, wischenden Verlaufseffekt (Shimmer) hinzu.
 * Dieser wird typischerweise auf Platzhalterelemente angewendet, um Ladezeiten zu visualisieren.
 */
fun Modifier.shimmer(): Modifier = composed {
    // track size of target layout to adapt shimmer gradient bounds
    var size by remember { mutableStateOf(IntSize.Zero) }
    val transition = rememberInfiniteTransition(label = "shimmer")
    
    val widthPx = size.width.toFloat().coerceAtLeast(100f)
    val heightPx = size.height.toFloat().coerceAtLeast(100f)
    
    // run the animation progress cycle from -2x width to 2x width
    val startOffsetX by transition.animateFloat(
        initialValue = -2 * widthPx,
        targetValue = 2 * widthPx,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerX"
    )

    // draw gradient overlay aligned with the animated start offset
    background(
        brush = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.03f),
                Color.White.copy(alpha = 0.15f),
                Color.White.copy(alpha = 0.03f)
            ),
            start = Offset(startOffsetX, 0f),
            end = Offset(startOffsetX + widthPx, heightPx)
        )
    ).onGloballyPositioned {
        size = it.size
    }
}

/**
 * Platzhalter-Skeleton für eine einzelne Vorlesungskarte.
 * Wird verwendet, während der Stundenplan geladen wird.
 */
@Composable
fun TimetableLessonCardSkeleton() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .height(112.dp),
        color = Color.White.copy(alpha = 0.05f),
        shape = RoundedCornerShape(24.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize().shimmer())
    }
}

/**
 * Platzhalter-Skeleton für ein kompaktes Suchergebnis (Glas-Design).
 * Wird verwendet, um die Verzögerung bei der Modulsuche zu überbrücken.
 */
@Composable
fun CompactGlassCardSkeleton() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(84.dp),
        color = Color.White.copy(alpha = 0.05f),
        shape = RoundedCornerShape(20.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize().shimmer())
    }
}

/**
 * Platzhalter-Skeleton für die gesamte Wochenansicht.
 * Zeigt mehrere Platzhalterkarten in einem Rasterlayout an.
 */
@Composable
fun TimetableGridSkeleton() {
    // list skeleton representing daily/weekly course results loading state
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 50.dp, end = 16.dp, top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        repeat(4) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                color = Color.White.copy(alpha = 0.05f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize().shimmer())
            }
        }
    }
}
