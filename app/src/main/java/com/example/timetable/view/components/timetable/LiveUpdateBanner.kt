package com.example.timetable.view.components.timetable

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Hinweisbanner, das am oberen Bildschirmrand wichtige Planänderungen wie Ausfälle oder
 * Raumänderungen anzeigt. Das Banner ist interaktiv und kann vom Benutzer geschlossen werden.
 *
 * @param message Der anzuzeigende Infotext (z. B. "Dozent krank" oder "Raumänderung in Raum 120").
 * @param isCancellation Gibt an, ob es sich um einen Vorlesungsausfall handelt. Bestimmt das Farbschema (rot bei Ausfall, gelb/orange bei Raumänderung).
 * @param onDismiss Callback-Methode, wenn der Benutzer auf das Schließen-Symbol tippt.
 * @param modifier Der Modifier für Layout-Anpassungen des Banners.
 */
@Composable
fun LiveUpdateBanner(
    message: String,
    isCancellation: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    // determine color scheme based on whether it is a cancellation or warning
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        color = if (isCancellation) {
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f)
        } else {
            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.9f)
        },
        contentColor = if (isCancellation) {
            MaterialTheme.colorScheme.onErrorContainer
        } else {
            MaterialTheme.colorScheme.onTertiaryContainer
        },
        border = BorderStroke(
            1.dp,
            if (isCancellation) MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
            else MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)
        ),
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // render warning icon and message detail block
            Icon(
                imageVector = Icons.Default.NotificationsActive,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isCancellation) "Ausfall-Hinweis" else "Raumänderung / Info",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    lineHeight = 14.sp
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // dismiss button to clear the alert state
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Schließen",
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
