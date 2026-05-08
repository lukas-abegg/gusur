package com.gusur.app.theme

import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object GusurColors {
    val Ink = Color(0xFF1A0D08)
    val Paper = Color(0xFFFDF4E6)
    val Cream = Color(0xFFF5E6CC)
    val Ember = Color(0xFFFF5722)
    val EmberDeep = Color(0xFFD63A00)
    val Gold = Color(0xFFFFB627)
    val Steam = Color(0xFFE8D5B7)
    val Wood = Color(0xFF6B3410)
    val WoodDeep = Color(0xFF3D1D08)
    val Birch = Color(0xFF6FA83A)
    val Sky = Color(0xFF4A90C2)
    val Plum = Color(0xFF7D2C5E)
}

val GusurTypography = Typography(
    h4 = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        letterSpacing = (-0.03).sp
    ),
    h5 = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        letterSpacing = (-0.02).sp
    ),
    h6 = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        letterSpacing = (-0.01).sp
    ),
    body1 = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp
    ),
    body2 = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp
    ),
    caption = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        letterSpacing = 0.15.sp
    ),
    overline = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 10.sp,
        letterSpacing = 0.15.sp
    )
)

@Composable
fun GusurTheme(content: @Composable () -> Unit) {
    val colors = lightColors(
        primary = GusurColors.Ink,
        secondary = GusurColors.Ember,
        background = GusurColors.Paper,
        surface = GusurColors.Paper,
        onPrimary = GusurColors.Paper,
        onSecondary = GusurColors.Paper,
        onBackground = GusurColors.Ink,
        onSurface = GusurColors.Ink
    )

    MaterialTheme(
        colors = colors,
        typography = GusurTypography,
        content = content
    )
}
