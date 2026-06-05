package net.focustation.myapplication.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import net.focustation.myapplication.R

// 조선굴림(ChosunGu) — 단일 weight. 볼드 미사용.
val ChosunGu = FontFamily(Font(R.font.chosun_gu))

val Typography =
    Typography(
        displayLarge =
            TextStyle(
                fontFamily = ChosunGu,
                fontWeight = FontWeight.Light,
                fontSize = 54.sp,
                lineHeight = 60.sp,
                letterSpacing = 0.sp,
            ),
        displayMedium =
            TextStyle(
                fontFamily = ChosunGu,
                fontWeight = FontWeight.Light,
                fontSize = 44.sp,
                lineHeight = 50.sp,
                letterSpacing = 0.sp,
            ),
        headlineLarge =
            TextStyle(
                fontFamily = ChosunGu,
                fontWeight = FontWeight.Light,
                fontSize = 32.sp,
                lineHeight = 38.sp,
                letterSpacing = 0.sp,
            ),
        headlineMedium =
            TextStyle(
                fontFamily = ChosunGu,
                fontWeight = FontWeight.Light,
                fontSize = 28.sp,
                lineHeight = 34.sp,
                letterSpacing = 0.sp,
            ),
        titleLarge =
            TextStyle(
                fontFamily = ChosunGu,
                fontWeight = FontWeight.Light,
                fontSize = 22.sp,
                lineHeight = 28.sp,
                letterSpacing = 0.sp,
            ),
        titleMedium =
            TextStyle(
                fontFamily = ChosunGu,
                fontWeight = FontWeight.Light,
                fontSize = 20.sp,
                lineHeight = 26.sp,
                letterSpacing = 0.sp,
            ),
        bodyLarge =
            TextStyle(
                fontFamily = ChosunGu,
                fontWeight = FontWeight.Light,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                letterSpacing = 0.sp,
            ),
        bodyMedium =
            TextStyle(
                fontFamily = ChosunGu,
                fontWeight = FontWeight.Light,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                letterSpacing = 0.sp,
            ),
        labelLarge =
            TextStyle(
                fontFamily = ChosunGu,
                fontWeight = FontWeight.Light,
                fontSize = 17.sp,
                lineHeight = 23.sp,
                letterSpacing = 0.sp,
            ),
        labelMedium =
            TextStyle(
                fontFamily = ChosunGu,
                fontWeight = FontWeight.Light,
                fontSize = 15.sp,
                lineHeight = 20.sp,
                letterSpacing = 0.sp,
            ),
        labelSmall =
            TextStyle(
                fontFamily = ChosunGu,
                fontWeight = FontWeight.Light,
                fontSize = 13.sp,
                lineHeight = 17.sp,
                letterSpacing = 0.sp,
            ),
        displaySmall =
            TextStyle(
                fontFamily = ChosunGu,
                fontWeight = FontWeight.Light,
                fontSize = 36.sp,
                lineHeight = 44.sp,
                letterSpacing = 0.sp,
            ),
        headlineSmall =
            TextStyle(
                fontFamily = ChosunGu,
                fontWeight = FontWeight.Light,
                fontSize = 24.sp,
                lineHeight = 32.sp,
                letterSpacing = 0.sp,
            ),
        titleSmall =
            TextStyle(
                fontFamily = ChosunGu,
                fontWeight = FontWeight.Light,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                letterSpacing = 0.sp,
            ),
        bodySmall =
            TextStyle(
                fontFamily = ChosunGu,
                fontWeight = FontWeight.Light,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                letterSpacing = 0.sp,
            ),
    )
