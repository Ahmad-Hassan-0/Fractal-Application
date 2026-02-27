package AppFrontend.Interface.Insights.ModelTraining

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.math.abs
import kotlin.math.sin

class TrainingWaveView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val wavePath = Path()
    private val wavePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

    private var animTime = 0f
    private var lastFrameTime = System.nanoTime()

    // --- CONFIGURATION ---

    // Top Colors (Back -> Front)
    private val topColors = intArrayOf(
        Color.parseColor("#909090"), // Layer 0 (Back)
        Color.parseColor("#606060"), // Layer 1
        Color.parseColor("#303030"), // Layer 2
        Color.parseColor("#121212")  // Layer 3 (Front)
    )

    // Bottom Colors (Back -> Front)
    private val bottomColors = intArrayOf(
        Color.parseColor("#8A181818"),
        Color.parseColor("#A6181818"),
        Color.parseColor("#96181818"),
        Color.parseColor("#FF181818")
    )

    // Wave Parameters
    // Added 'offset' to shift them horizontally so they don't align
    private val layers = listOf(
        WaveLayer(0.8f, 0.7f, 200f, 0f),      // Back
        WaveLayer(1.3f, 0.9f, 180f, 15f),     // Mid-Back (Shifted)
        WaveLayer(1.5f, 1.1f, 140f, 35f),      // Mid-Front (Shifted)
        WaveLayer(1.7f, 1.3f, 100f, 50f)       // Front (Shifted)
    )

    private data class WaveLayer(val freq: Float, val speed: Float, val amp: Float, val offset: Float)

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val currentTime = System.nanoTime()
        val deltaTime = (currentTime - lastFrameTime) / 1_000_000_000f
        lastFrameTime = currentTime
        animTime += deltaTime

        val width = width.toFloat()
        val centerY = height / 2f

        // 1. Draw TOP Waves (Upward)
        for (i in layers.indices) {
            drawWave(canvas, i, width, centerY, animTime, isTop = true)
        }

        // 2. Draw BOTTOM Waves (Downward/Reflection)
        for (i in layers.indices) {
            drawWave(canvas, i, width, centerY, animTime, isTop = false)
        }

        postInvalidateOnAnimation()
    }

    private fun drawWave(canvas: Canvas, layerIndex: Int, width: Float, centerY: Float, time: Float, isTop: Boolean) {
        val layer = layers[layerIndex]
        wavePath.reset()
        wavePath.moveTo(0f, centerY)

        wavePaint.color = if (isTop) topColors[layerIndex] else bottomColors[layerIndex]
        val direction = if (isTop) -1f else 1f

        // Step size 5 for performance
        for (x in 0..width.toInt() step 5) {
            val xf = x.toFloat()
            // Add the layer offset to X so peaks don't align
            val normalizedX = (xf / width * 10f) + layer.offset

            // MOUNTAIN MATH:
            // 1. Base Wave: Main shape
            // 2. Interference Wave: Slightly different freq (freq * 1.2) creates "beating" (mountains)
            // 3. Detail Wave: Higher freq (freq * 2.5) adds roughness
            val yOffset = (
                    sin(normalizedX * layer.freq + time * layer.speed) +
                            sin(normalizedX * (layer.freq * 1.2f) + time * (layer.speed * 0.8f)) * 0.8f +
                            sin(normalizedX * (layer.freq * 2.5f) + time * (layer.speed * 1.2f)) * 0.3f
                    ) * layer.amp * 0.6f * direction // Scale down slightly since we added more sine waves

            // Clamp: Ensure the wave doesn't cross the center line inappropriately
            // Use abs() logic if you want sharp points, but linear addition is smoother
            val finalY = centerY + yOffset
            wavePath.lineTo(xf, finalY)
        }

        wavePath.lineTo(width, centerY)
        wavePath.close()

        canvas.drawPath(wavePath, wavePaint)
    }
}