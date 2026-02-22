package AppFrontend.Interface.Insights.UsageInsights

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.math.sin

class HeartBeatView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val density = context.resources.displayMetrics.density

    private val wavePaint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 3f * density
        isAntiAlias = true
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }

    private val baselinePaint = Paint().apply {
        color = Color.BLACK
        strokeWidth = 2f * density
        alpha = 100 // Slightly dimmer for better contrast with the wave
    }

    private val path = Path()
    private var phase = 10f

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        path.reset()

        val midY = height / 2f
        val amplitude = height * 0.22f // Slightly increased for more "drama"

        // Draw horizontal baseline
        canvas.drawLine(0f, midY, width.toFloat(), midY, baselinePaint)

        path.moveTo(0f, midY)

        // Use three distinct, non-related frequencies for "randomness"
        val freq1 = 0.031f / density   // Slow organic swell
        val freq2 = 0.067f / density   // The main rhythmic pulse
//        val freq3 = 0.113f / density   // High-frequency jitter (random noise)

        val stepSize = (1f * density).toInt().coerceAtLeast(1)

        for (x in 0..width step stepSize) {
            val xf = x.toFloat()

            // Summing three sine waves with different speeds (multipliers on 'phase')
            // creates a pattern that takes a very long time to repeat.
            val yOffset = (sin(xf * freq1 + phase) * amplitude) + (sin(xf * freq2 + phase * 1.8f) * (amplitude * 0.45f))

            val y = midY + yOffset
            path.lineTo(xf, y.toFloat())
        }

        canvas.drawPath(path, wavePaint)

        // Control the overall "flow" speed
        phase -= 0.04f
        invalidate()
    }
}