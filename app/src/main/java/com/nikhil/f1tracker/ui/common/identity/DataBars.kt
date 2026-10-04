package com.nikhil.f1tracker.ui.common.identity

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val BAR_HEIGHT = 8.dp
private const val SECOND_SHARE_ALPHA = 0.35f

/** A filled share of a track, e.g. a win rate or a gap to the leader. */
@Composable
fun FractionBar(fraction: Float, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier.fillMaxWidth().height(BAR_HEIGHT).clip(RoundedCornerShape(BAR_HEIGHT / 2))
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        if (fraction > 0f) {
            Box(Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).height(BAR_HEIGHT).background(color))
        }
    }
}

/** Two-way split, e.g. a teammate head-to-head: solid for the first side, faded for the second. */
@Composable
fun SplitBar(label: String, first: Int, second: Int, color: Color, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(48.dp))
        Text("$first", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(8.dp))
        Row(Modifier.weight(1f).height(BAR_HEIGHT).clip(RoundedCornerShape(BAR_HEIGHT / 2))) {
            if (first + second == 0) {
                Box(Modifier.fillMaxWidth().height(BAR_HEIGHT).background(MaterialTheme.colorScheme.surfaceVariant))
            } else {
                if (first > 0) Box(Modifier.weight(first.toFloat()).height(BAR_HEIGHT).background(color))
                if (second > 0) {
                    Box(Modifier.weight(second.toFloat()).height(BAR_HEIGHT).background(color.copy(alpha = SECOND_SHARE_ALPHA)))
                }
            }
        }
        Spacer(Modifier.width(8.dp))
        Text("$second", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
    }
}

private val GOLD = Color(0xFFD4AF37)
private val SILVER = Color(0xFFB0B7C0)
private val BRONZE = Color(0xFFCD7F32)
private val POINTS_GREEN = Color(0xFF2E9E5B)
private val NO_POINTS_GREY = Color(0xFF5F6368)
private val RETIRED_RED = Color(0xFFD93025)
private const val LAST_POINTS_POSITION = 10

fun finishColor(positionText: String): Color = when (val position = positionText.toIntOrNull()) {
    null -> RETIRED_RED
    1 -> GOLD
    2 -> SILVER
    3 -> BRONZE
    in 4..LAST_POINTS_POSITION -> POINTS_GREEN
    else -> NO_POINTS_GREY
}

/** A finishing position on its podium/points/retired colour. */
@Composable
fun FinishBadge(positionText: String, modifier: Modifier = Modifier, size: Dp = 28.dp) {
    val color = finishColor(positionText)
    Box(modifier.size(size).background(color, RoundedCornerShape(6.dp)), contentAlignment = Alignment.Center) {
        Text(positionText, color = color.onColor(), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
    }
}

/** One race in a form strip: finish on a coloured chip, starting slot underneath. */
@Composable
fun FormChip(positionText: String, grid: Int, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        FinishBadge(positionText)
        Text(
            if (grid > 0) "G$grid" else "PL",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

@Composable
fun FormStrip(outcomes: List<Pair<String, Int>>, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        outcomes.forEach { (positionText, grid) -> FormChip(positionText, grid) }
    }
}
