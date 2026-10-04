package com.nikhil.f1tracker.ui.common.identity

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * A championship row: team stripe, position, avatar, name, points, and a bar showing the gap
 * to the leader in the team's colour.
 */
@Composable
fun StandingRow(
    position: Int,
    title: String,
    subtitle: String?,
    points: Double,
    leaderPoints: Double,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leading: @Composable () -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick).teamStripe(color)
            .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "$position",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(28.dp),
        )
        leading()
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                Text(
                    "${formatPoints(points)} pts",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
            subtitle?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            FractionBar(
                fraction = if (leaderPoints > 0) (points / leaderPoints).toFloat() else 0f,
                color = color,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

fun formatPoints(points: Double): String =
    if (points == points.toLong().toDouble()) points.toLong().toString() else points.toString()
