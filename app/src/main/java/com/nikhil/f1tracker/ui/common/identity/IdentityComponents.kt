package com.nikhil.f1tracker.ui.common.identity

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage

private val STRIPE_WIDTH = 4.dp
private val AVATAR_BORDER = 2.dp
private const val LIGHT_COLOR_LUMINANCE = 0.5f

@Composable
fun teamColor(constructorId: String?): Color =
    LocalF1Identities.current.teamColor(constructorId).toColorOr(MaterialTheme.colorScheme.outline)

@Composable
fun driverColor(driverId: String, constructorId: String? = null): Color =
    LocalF1Identities.current.driverColor(driverId, constructorId).toColorOr(MaterialTheme.colorScheme.outline)

/** Readable text colour on top of [this] background. */
fun Color.onColor(): Color = if (luminance() > LIGHT_COLOR_LUMINANCE) Color.Black else Color.White

/** A team-colour bar down the left edge of a row. */
fun Modifier.teamStripe(color: Color): Modifier = drawBehind {
    drawRect(color, size = Size(STRIPE_WIDTH.toPx(), size.height))
}

/** Headshot in a team-colour ring, falling back to the driver's 3-letter code badge. */
@Composable
fun DriverAvatar(
    driverId: String,
    constructorId: String? = null,
    size: Dp = 40.dp,
    modifier: Modifier = Modifier,
) {
    val look = LocalF1Identities.current.driver(driverId)
    val color = driverColor(driverId, constructorId)
    val fallback = @Composable { CodeCircle(look?.code ?: driverId.take(3).uppercase(), color) }
    Box(
        modifier = modifier.size(size).clip(CircleShape).background(color.copy(alpha = 0.25f))
            .border(AVATAR_BORDER, color, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (look?.headshotUrl == null) {
            fallback()
        } else {
            SubcomposeAsyncImage(
                model = look.headshotUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alignment = Alignment.TopCenter,
                error = { fallback() },
                loading = { fallback() },
            )
        }
    }
}

@Composable
private fun CodeCircle(code: String, color: Color) {
    Box(Modifier.background(color, CircleShape).padding(4.dp), contentAlignment = Alignment.Center) {
        Text(code, color = color.onColor(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
    }
}

/** Compact team-coloured 3-letter badge, for dense rows. */
@Composable
fun CodeBadge(code: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(color, RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(code, color = color.onColor(), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
    }
}

/** Driver code badge in the colour of the team the driver raced for in that row. */
@Composable
fun DriverCodeBadge(driverId: String, constructorId: String? = null, modifier: Modifier = Modifier) {
    val code = LocalF1Identities.current.driver(driverId)?.code ?: driverId.take(3).uppercase()
    CodeBadge(code, driverColor(driverId, constructorId), modifier)
}

@Composable
fun TeamDot(constructorId: String?, size: Dp = 12.dp, modifier: Modifier = Modifier) {
    Box(modifier.size(size).background(teamColor(constructorId), CircleShape))
}

private fun Long?.toColorOr(default: Color): Color = this?.let { Color(it) } ?: default
