package com.nikhil.f1tracker.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nikhil.f1tracker.ui.common.identity.finishColor

/** One race on a [PositionChart]. [finish] is null for a retirement. */
data class RacePosition(val round: Int, val finish: Int?, val positionText: String, val grid: Int?)

private const val WORST_POSITION_SHOWN = 20
private val GRID_LINE_POSITIONS = listOf(1, 5, 10, 15, 20)
private const val ROUND_LABEL_EVERY = 4

/**
 * Finishing position race by race, P1 at the top. Dots use podium/points colours, retirements
 * sit on the bottom edge in red, and the starting grid is a faint dashed line for gains/losses.
 */
@Composable
fun PositionChart(races: List<RacePosition>, lineColor: Color, modifier: Modifier = Modifier) {
    if (races.isEmpty()) return
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val labelStyle = TextStyle(fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    val measurer = rememberTextMeasurer()
    Column(modifier) {
        Canvas(Modifier.fillMaxWidth().height(220.dp)) {
            val left = 28.dp.toPx()
            val top = 8.dp.toPx()
            val bottom = size.height - 20.dp.toPx()
            val width = size.width - left - 8.dp.toPx()
            fun y(position: Int) = top + (bottom - top) * (position.coerceIn(1, WORST_POSITION_SHOWN) - 1) / (WORST_POSITION_SHOWN - 1)
            fun x(index: Int) = left + if (races.size > 1) width * index / (races.size - 1) else width / 2

            GRID_LINE_POSITIONS.forEach { position ->
                drawLine(gridColor, Offset(left, y(position)), Offset(size.width, y(position)), strokeWidth = 1.dp.toPx())
                drawText(measurer, "P$position", Offset(0f, y(position) - 7.dp.toPx()), labelStyle)
            }
            races.forEachIndexed { index, race ->
                if (index % ROUND_LABEL_EVERY == 0 || index == races.lastIndex) {
                    drawText(measurer, "R${race.round}", Offset(x(index) - 8.dp.toPx(), bottom + 4.dp.toPx()), labelStyle)
                }
            }
            val gridPoints = races.mapIndexedNotNull { i, r -> r.grid?.takeIf { it > 0 }?.let { Offset(x(i), y(it)) } }
            gridPoints.zipWithNext().forEach { (a, b) ->
                drawLine(
                    lineColor.copy(alpha = 0.35f), a, b, strokeWidth = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f)),
                )
            }
            val finishPoints = races.mapIndexed { i, r -> Offset(x(i), y(r.finish ?: WORST_POSITION_SHOWN)) }
            finishPoints.zipWithNext().forEach { (a, b) ->
                drawLine(lineColor, a, b, strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
            }
            races.forEachIndexed { i, race ->
                drawCircle(finishColor(race.positionText), radius = 5.dp.toPx(), center = finishPoints[i])
            }
        }
        Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Legend(lineColor, "Finish")
            Legend(lineColor.copy(alpha = 0.35f), "Grid (dashed)")
            Legend(finishColor("R"), "Retired")
        }
    }
}

@Composable
private fun Legend(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.padding(end = 4.dp).size(8.dp).background(color, CircleShape))
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}
