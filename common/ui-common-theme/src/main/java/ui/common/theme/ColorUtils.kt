package ui.common.theme

import android.graphics.Bitmap
import androidx.annotation.ColorInt
import androidx.annotation.FloatRange
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import androidx.core.math.MathUtils
import androidx.palette.graphics.Palette
import ui.common.theme.theme.contrastComposite
import ui.common.theme.theme.isSystemInLightTheme
import ui.common.theme.theme.toColor
import kotlin.math.*
import android.graphics.Color as AColor

val ADAPTIVE_COLOR_ANIMATION: AnimationSpec<Color> = tween(easing = FastOutSlowInEasing)

data class AdaptiveColorResult(val color: Color, val contentColor: Color, val gradient: Brush)

@Composable
fun materialYouColor(
    color: Color = MaterialTheme.colorScheme.primary.contrastComposite(),
    animationSpec: AnimationSpec<Color> = ADAPTIVE_COLOR_ANIMATION
): AdaptiveColorResult {
    val accentAnimated by animateColorAsState(color, animationSpec)

    return AdaptiveColorResult(
        color,
        color.contentColor(),
        backgroundGradient(accentAnimated)
    )
}

@Composable
fun adaptiveColor(
    image: Bitmap? = null,
    fallback: Color = MaterialTheme.colorScheme.secondary.contrastComposite(),
    animationSpec: AnimationSpec<Color> = ADAPTIVE_COLOR_ANIMATION
): AdaptiveColorResult {
    var accent by remember { mutableStateOf(fallback) }
    val accentAnimated by animateColorAsState(accent, animationSpec)
    val contentColor by derivedStateOf { accent.contentColor() }

    // when initial color changes
    // reset initial accent color if palette hasn't been generated yet
    var paletteGenerated by remember { mutableStateOf(false) }
    LaunchedEffect(fallback) {
        if (!paletteGenerated) {
            accent = fallback
        }
    }

    val isDarkColors = isSystemInDarkTheme()

    LaunchedEffect(image, fallback, isDarkColors) {
        if (image != null)
            Palette.from(image)
                .generate().apply {
                    accent = getAccentColor(isDarkColors, fallback.toArgb(), this).toColor()
                    paletteGenerated = true
                }
    }

    return AdaptiveColorResult(accent, contentColor, backgroundGradient(accentAnimated))
}

@Composable
fun backgroundGradient(
    accent: Color,
    endColor: Color = if (isSystemInLightTheme()) Color.White else Color.Black
): Brush {
    val isDark = isSystemInDarkTheme()
    val first = gradientShift(isDark, accent.toArgb(), 0.25f, 100)
    val second = gradientShift(isDark, accent.toArgb(), 0.13f, 25)

    return Brush.verticalGradient(listOf(first, second, endColor))
}

/**
 * Applies linear gradient background with given [colorStops] and [angle].
 */
fun Modifier.gradientBackground(vararg colorStops: Pair<Float, Color>, angle: Float) = this.then(
    Modifier.drawBehind {
        val angleRad = angle / 180f * PI
        val x = cos(angleRad).toFloat() // Fractional x
        val y = sin(angleRad).toFloat() // Fractional y

        val radius = sqrt(size.width.pow(2) + size.height.pow(2)) / 2f
        val offset = center + Offset(x * radius, y * radius)

        val exactOffset = Offset(
            x = min(offset.x.coerceAtLeast(0f), size.width),
            y = size.height - min(offset.y.coerceAtLeast(0f), size.height)
        )

        drawRect(
            brush = Brush.linearGradient(
                colorStops = colorStops,
                start = Offset(size.width, size.height) - exactOffset,
                end = exactOffset
            ),
            size = size
        )
    }
)

fun getAccentColor(isDark: Boolean, default: Int, palette: Palette): Int {
    when (isDark) {
        true -> {
            val darkMutedColor = palette.getDarkMutedColor(default)
            val lightMutedColor = palette.getLightMutedColor(darkMutedColor)
            val darkVibrant = palette.getDarkVibrantColor(lightMutedColor)
            val lightVibrant = palette.getLightVibrantColor(darkVibrant)
            val mutedColor = palette.getMutedColor(lightVibrant)
            return palette.getVibrantColor(mutedColor)
        }
        false -> {
            val lightMutedColor = palette.getLightMutedColor(default)
            val lightVibrant = palette.getLightVibrantColor(lightMutedColor)
            val mutedColor = palette.getMutedColor(lightVibrant)
            val darkMutedColor = palette.getDarkMutedColor(mutedColor)
            val vibrant = palette.getVibrantColor(darkMutedColor)
            return palette.getDarkVibrantColor(vibrant)
        }
    }
}

private fun gradientShift(isDarkMode: Boolean, color: Int, shift: Float, alpha: Int): Color {
    return Color(
        if (isDarkMode) shiftColor(color, shift) else ColorUtils.setAlphaComponent(
            shiftColor(color, 2f),
            alpha
        )
    )
}

fun Color.contentColor() = getContrastColor(toArgb()).toColor()

fun getContrastColor(@ColorInt color: Int): Int {
    // Counting the perceptive luminance - human eye favors green color...
    val a: Double =
        1 - (0.299 * AColor.red(color) + 0.587 * AColor.green(color) + 0.114 * AColor.blue(color)) / 255
    return if (a < 0.5) AColor.BLACK else AColor.WHITE
}

private fun desaturate(isDarkMode: Boolean, color: Int): Int {
    if (!isDarkMode) {
        return color
    }

    if (color == AColor.TRANSPARENT) {
        // can't desaturate transparent color
        return color
    }
    val amount = .25f
    val minDesaturation = .75f

    val hsl = floatArrayOf(0f, 0f, 0f)
    ColorUtils.colorToHSL(color, hsl)
    if (hsl[1] > minDesaturation) {
        hsl[1] = MathUtils.clamp(
            hsl[1] - amount,
            minDesaturation - 0.1f,
            1f
        )
    }
    return ColorUtils.HSLToColor(hsl)
}

fun shiftColor(@ColorInt color: Int, @FloatRange(from = 0.0, to = 2.0) by: Float): Int {
    return if (by == 1.0f) {
        color
    } else {
        val alpha = AColor.alpha(color)
        val hsv = FloatArray(3)
        AColor.colorToHSV(color, hsv)
        hsv[2] *= by
        (alpha shl 24) + (16777215 and AColor.HSVToColor(hsv))
    }
}
