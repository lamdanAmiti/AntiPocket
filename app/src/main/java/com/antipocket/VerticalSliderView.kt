package com.antipocket

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat

class VerticalSliderView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    enum class SliderMode {
        CALL,
        UNLOCK
    }

    var mode: SliderMode = SliderMode.CALL
        set(value) {
            field = value
            invalidate()
        }

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f * resources.displayMetrics.density
        color = 0xFF000000.toInt()
    }

    private val trackFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = 0xFFFFFFFF.toInt()
    }

    private val knobStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f * resources.displayMetrics.density
        color = 0xFF000000.toInt()
    }

    private val knobFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = 0xFFFFFFFF.toInt()
    }

    private var knobRadius = 40f * resources.displayMetrics.density
    private var trackWidth = 100f * resources.displayMetrics.density
    private var trackPadding = 20f * resources.displayMetrics.density
    private var topMargin = 10f * resources.displayMetrics.density

    private var knobY = 0f
    private var minY = 0f
    private var maxY = 0f
    private var isDragging = false
    private var lastTouchY = 0f

    private var callIcon: Drawable? = null
    private var unlockIcon: Drawable? = null

    var onSlideComplete: (() -> Unit)? = null

    init {
        callIcon = ContextCompat.getDrawable(context, R.drawable.ic_call)
        unlockIcon = ContextCompat.getDrawable(context, R.drawable.ic_unlock)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        minY = knobRadius + trackPadding + topMargin
        maxY = h - knobRadius - trackPadding
        knobY = minY
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val centerX = width / 2f
        val trackLeft = centerX - trackWidth / 2
        val trackRight = centerX + trackWidth / 2
        val trackTop = trackPadding
        val trackBottom = height - trackPadding
        val cornerRadius = trackWidth / 2

        // Draw track fill (white)
        canvas.drawRoundRect(
            trackLeft, trackTop, trackRight, trackBottom,
            cornerRadius, cornerRadius, trackFillPaint
        )

        // Draw track stroke (black outline)
        canvas.drawRoundRect(
            trackLeft, trackTop, trackRight, trackBottom,
            cornerRadius, cornerRadius, trackPaint
        )

        // Draw icon at bottom
        val icon = if (mode == SliderMode.CALL) callIcon else unlockIcon
        icon?.let {
            val iconSize = (knobRadius * 1.2f).toInt()
            val iconLeft = (centerX - iconSize / 2).toInt()
            val iconTop = (maxY - iconSize / 2).toInt()
            it.setBounds(iconLeft, iconTop, iconLeft + iconSize, iconTop + iconSize)
            it.setTint(0xFF000000.toInt())
            it.draw(canvas)
        }

        // Draw knob fill (white)
        canvas.drawCircle(centerX, knobY, knobRadius, knobFillPaint)

        // Draw knob stroke (black outline)
        canvas.drawCircle(centerX, knobY, knobRadius, knobStrokePaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val centerX = width / 2f

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                val dx = event.x - centerX
                val dy = event.y - knobY
                if (dx * dx + dy * dy <= knobRadius * knobRadius * 1.5f) {
                    isDragging = true
                    lastTouchY = event.y
                    parent?.requestDisallowInterceptTouchEvent(true)
                    return true
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (isDragging) {
                    val deltaY = event.y - lastTouchY
                    knobY = (knobY + deltaY).coerceIn(minY, maxY)
                    lastTouchY = event.y
                    invalidate()

                    // Check if reached bottom
                    if (knobY >= maxY - 10) {
                        isDragging = false
                        onSlideComplete?.invoke()
                    }
                    return true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isDragging) {
                    isDragging = false
                    // Spring back to top if not completed
                    if (knobY < maxY - 10) {
                        animateToTop()
                    }
                    parent?.requestDisallowInterceptTouchEvent(false)
                    return true
                }
            }
        }
        return super.onTouchEvent(event)
    }

    private fun animateToTop() {
        animate()
            .setDuration(200)
            .setUpdateListener {
                knobY = minY + (knobY - minY) * (1 - it.animatedFraction)
                invalidate()
            }
            .withEndAction {
                knobY = minY
                invalidate()
            }
            .start()
    }

    fun reset() {
        knobY = minY
        invalidate()
    }
}
