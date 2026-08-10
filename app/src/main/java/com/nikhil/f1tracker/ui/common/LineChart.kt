package com.nikhil.f1tracker.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class ChartPoint(val xLabel: String, val value: Float)
data class ChartSeries(val name: String, val color: Color, val points: List<ChartPoint>)

/**
 * A minimal line chart for a handful of season-over-season data points (change
 * over time is the job; a single shared y-axis keeps two series comparable).
 * Point values are labeled directly since there are only ever a few seasons of
 * history, rather than relying on hover (this app targets touch, not a cursor).
 */
@Composable
fun LineChart(
    series: List<ChartSeries>,
    modifier: Modifier = Modifier,
    valueFormatter: (Float) -> String = { it.toInt().toString() },
) {
    val nonEmptySeries = series.filter { it.points.isNotEmpty() }
    if (nonEmptySeries.isEmpty()) return

    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val labelTextStyle = TextStyle(fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
    val textMeasurer = rememberTextMeasurer()
    val pointCount = nonEmptySeries.first().points.size
    val hasMultipleSeries = nonEmptySeries.size > 1

    Column(modifier = modifier) {
        if (hasMultipleSeries) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                nonEmptySeries.forEach { s -> LegendEntry(s.name, s.color) }
            }
            Spacer(Modifier.height(12.dp))
        }

        val maxValue = (nonEmptySeries.flatMap { it.points.map { p -> p.value } }.maxOrNull() ?: 0f)
            .coerceAtLeast(1f)

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (hasMultipleSeries) 200.dp else 160.dp),
        ) {
            val topPadding = 28.dp.toPx()
            val bottomPadding = if (hasMultipleSeries) 28.dp.toPx() else 8.dp.toPx()
            val chartHeight = size.height - topPadding - bottomPadding
            val xStep = if (pointCount > 1) size.width / (pointCount - 1) else 0f

            val gridLineCount = 3
            repeat(gridLineCount + 1) { i ->
                val y = topPadding + chartHeight * i / gridLineCount
                drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
            }

            nonEmptySeries.forEachIndexed { seriesIndex, s ->
                val offsets = s.points.mapIndexed { index, point ->
                    val x = if (pointCount > 1) xStep * index else size.width / 2
                    val y = topPadding + chartHeight * (1 - point.value / maxValue)
                    Offset(x, y)
                }
                for (i in 0 until offsets.size - 1) {
                    drawLine(
                        color = s.color,
                        start = offsets[i],
                        end = offsets[i + 1],
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round,
                    )
                }
                offsets.forEach { offset -> drawCircle(color = s.color, radius = 4.dp.toPx(), center = offset) }
                offsets.forEachIndexed { index, offset ->
                    val text = valueFormatter(s.points[index].value)
                    val measured = textMeasurer.measure(text, labelTextStyle)
                    val labelY = if (seriesIndex % 2 == 0) {
                        offset.y - measured.size.height - 4.dp.toPx()
                    } else {
                        offset.y + 4.dp.toPx()
                    }
                    val labelX = (offset.x - measured.size.width / 2f)
                        .coerceIn(0f, (size.width - measured.size.width).coerceAtLeast(0f))
                    drawText(
                        textMeasurer = textMeasurer,
                        text = text,
                        style = labelTextStyle,
                        topLeft = Offset(labelX, labelY),
                    )
                }
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            nonEmptySeries.first().points.forEach { point ->
                Text(point.xLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun LegendEntry(name: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(10.dp).background(color, CircleShape))
        Text(name, style = MaterialTheme.typography.labelMedium)
    }
}
