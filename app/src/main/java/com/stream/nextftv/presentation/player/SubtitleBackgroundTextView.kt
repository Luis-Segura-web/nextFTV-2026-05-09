package com.stream.nextftv.presentation.player

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.text.Layout
import android.util.AttributeSet
import android.widget.TextView
import kotlin.math.max

class SubtitleBackgroundTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : TextView(context, attrs) {

    private val lineBackgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.TRANSPARENT
    }
    private val lineRect = RectF()

    private var lineBackgroundColor: Int = Color.TRANSPARENT
    private var lineCornerRadiusPx: Float = 0f
    private var lineHorizontalPaddingPx: Float = 0f
    private var lineVerticalPaddingPx: Float = 0f
    private var lineSpacingGapPx: Float = 0f

    init {
        includeFontPadding = false
    }

    fun setLineBackgroundStyle(
        color: Int,
        cornerRadiusPx: Float,
        horizontalPaddingPx: Int,
        verticalPaddingPx: Int
    ) {
        lineBackgroundColor = color
        lineCornerRadiusPx = cornerRadiusPx
        lineHorizontalPaddingPx = horizontalPaddingPx.toFloat()
        lineVerticalPaddingPx = verticalPaddingPx.toFloat()
        lineSpacingGapPx = (verticalPaddingPx * 0.75f).coerceAtLeast(2f)
        lineBackgroundPaint.color = color
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        drawLineBackgrounds(canvas)
        super.onDraw(canvas)
    }

    private fun drawLineBackgrounds(canvas: Canvas) {
        val textLayout = layout ?: return
        if (lineBackgroundColor == Color.TRANSPARENT || text.isNullOrBlank()) return

        val lineCount = textLayout.lineCount
        val topOffset = totalPaddingTop.toFloat()
        val leftOffset = totalPaddingLeft.toFloat()
        val maxRight = width - totalPaddingRight.toFloat()

        for (lineIndex in 0 until lineCount) {
            if (isLineBlank(textLayout, lineIndex)) continue

            val lineLeft = leftOffset + textLayout.getLineLeft(lineIndex)
            val lineRight = leftOffset + textLayout.getLineRight(lineIndex)
            val lineTop = topOffset + textLayout.getLineTop(lineIndex)
            val lineBottom = topOffset + textLayout.getLineBottom(lineIndex)
            val halfGap = lineSpacingGapPx / 2f

            lineRect.left = max(0f, lineLeft - lineHorizontalPaddingPx)
            lineRect.right = max(lineRect.left, (lineRight + lineHorizontalPaddingPx).coerceAtMost(maxRight))
            lineRect.top = max(0f, lineTop - lineVerticalPaddingPx + halfGap)
            lineRect.bottom = max(lineRect.top, lineBottom + lineVerticalPaddingPx - halfGap)

            canvas.drawRoundRect(lineRect, lineCornerRadiusPx, lineCornerRadiusPx, lineBackgroundPaint)
        }
    }

    private fun isLineBlank(textLayout: Layout, lineIndex: Int): Boolean {
        val start = textLayout.getLineStart(lineIndex)
        val end = textLayout.getLineEnd(lineIndex)
        if (start >= end) return true
        return text?.subSequence(startIndex = start, endIndex = end)?.toString()?.trim().isNullOrEmpty()
    }
}
