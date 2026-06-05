package net.focustation.myapplication.ui.screen.space

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import androidx.core.graphics.ColorUtils

/**
 * 공간기록 지도 마커: 통통한 풍선 핀을 점수색으로 채우되 반투명·그라데이션으로 유리(glassy) 느낌을 준다.
 * 머리에 큰(살짝 비치는) 흰 원 + 점수, 끝(아래 꼭짓점)이 좌표를 가리킨다.
 * 장소명은 마커 캡션(이름표)으로 핀 아래에 따로 표시한다.
 * 색은 점수가 높을수록 빨강 / 중간 노랑 / 낮을수록 초록(히트맵 방향).
 */
private fun scoreColor(score: Int): Int =
    when {
        score >= 67 -> Color.parseColor("#FF5A5A") // 높음 (밝은 코랄레드)
        score >= 34 -> Color.parseColor("#FFD23F") // 중간 (밝은 옐로우)
        else -> Color.parseColor("#2FE0A6") // 낮음 (밝은 민트)
    }

fun buildScoreMarkerBitmap(
    context: Context,
    score: Int,
    selected: Boolean,
): Bitmap {
    val density = context.resources.displayMetrics.density

    fun px(dp: Float) = dp * density

    val headRadius = px(if (selected) 25f else 22f)
    val innerRadius = headRadius * 0.72f
    val shadowPad = px(5f)
    val tipDistance = headRadius * 1.5f

    val centerX = headRadius + shadowPad
    val headCenterY = shadowPad + headRadius
    val headTop = headCenterY - headRadius
    val tipY = headCenterY + tipDistance

    val width = (headRadius * 2 + shadowPad * 2).toInt()
    val height = (tipY + px(2f)).toInt()
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val baseColor = scoreColor(score)

    val headRect =
        RectF(
            centerX - headRadius,
            headCenterY - headRadius,
            centerX + headRadius,
            headCenterY + headRadius,
        )
    val tangentDegrees = Math.toDegrees(Math.acos((headRadius / tipDistance).toDouble())).toFloat()
    val pinPath =
        Path().apply {
            arcTo(headRect, 90f + tangentDegrees, 360f - 2f * tangentDegrees)
            lineTo(centerX, tipY)
            close()
        }

    // 유리 느낌: 위는 밝고 더 투명, 아래는 진하게(세로 그라데이션) + 전체 반투명
    val topColor = ColorUtils.setAlphaComponent(ColorUtils.blendARGB(baseColor, Color.WHITE, 0.3f), 140)
    val bottomColor = ColorUtils.setAlphaComponent(baseColor, 205)
    val fillPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            shader = LinearGradient(centerX, headTop, centerX, tipY, topColor, bottomColor, Shader.TileMode.CLAMP)
            setShadowLayer(px(4f), 0f, px(1.5f), Color.argb(45, 0, 0, 0))
        }
    canvas.drawPath(pinPath, fillPaint)

    // 유리 가장자리(얇은 반투명 흰 테두리)
    val borderPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = px(1.2f)
            this.color = Color.argb(150, 255, 255, 255)
        }
    canvas.drawPath(pinPath, borderPaint)

    // 머리 안쪽 살짝 비치는 흰 원
    val innerPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = Color.argb(205, 255, 255, 255)
            style = Paint.Style.FILL
        }
    canvas.drawCircle(centerX, headCenterY, innerRadius, innerPaint)

    // 점수 (자릿수에 맞춰 크기 조절)
    val label = score.toString()
    val textPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = Color.parseColor("#1A1A1A")
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = innerRadius * if (label.length >= 3) 0.74f else 1.04f
        }
    val baseline = headCenterY - (textPaint.descent() + textPaint.ascent()) / 2f
    canvas.drawText(label, centerX, baseline, textPaint)

    return bitmap
}
