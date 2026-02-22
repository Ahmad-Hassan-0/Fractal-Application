package AppFrontend.Interface.Insights.ModelTraining

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.math.sin

class TrainingWaveView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val wavePaths = Array(4) { Path() }
    private val wavePaints = Array(4) { Paint(Paint.ANTI_ALIAS_FLAG).apply { isDither = true } }

    private var animTime = 0f
    private var lastFrameTime = System.nanoTime()

    /**
     * Fourier-style configuration per layer to match the image structure.
     * Each layer is a sum of 5 waves with specific frequency bands.
     */
    // ORDER: Index 0 (Back) -> Index 3 (Front)
    private val layerHarmonics = arrayOf(
        // Layer 0: Lightest/Back - High Frequency "Teeth"
        floatArrayOf(8f, 16f, 24f, 0f, 0f),

        // Layer 1: Mid-Back - Medium peaks
        floatArrayOf(8f, 12f, 5f, 10f, 14f),

        // Layer 2: Mid-Front - Larger rolling peaks
        floatArrayOf(3f, 5.5f, 2.2f, 4.8f, 6f),

        // Layer 3: Darkest/Front - Deep Base masses
        floatArrayOf(1.2f, 0.8f, 1.5f, 2.1f, 0.5f)
    )

    private val colors = intArrayOf(
        Color.parseColor("#8A181818"), // Matches Layer 0
        Color.parseColor("#96181818"), // Matches Layer 1
        Color.parseColor("#A6181818"), // Matches Layer 2
        Color.parseColor("#FF181818")  // Matches Layer 3
    )

    private val amplitudes = floatArrayOf(140f, 110f, 85f, 60f)

    init {
        wavePaints.forEachIndexed { i, paint ->
            paint.style = Paint.Style.FILL
            paint.color = colors[i]
            // Image has soft edges, 3f blur creates that organic overlapping feel
            paint.maskFilter = BlurMaskFilter(3f, BlurMaskFilter.Blur.NORMAL)
        }
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val currentTime = System.nanoTime()
        val deltaTime = (currentTime - lastFrameTime) / 1_000_000_000f
        lastFrameTime = currentTime
        animTime += (deltaTime * 0.8f) // Controlled speed for alienated feel

        val width = width.toFloat()
        val height = height.toFloat()
        val centerY = height / 2f

        // Draw the waves mirrored exactly like the image
        drawFourierWaveSet(canvas, width, centerY, animTime, false)
        drawFourierWaveSet(canvas, width, centerY, animTime, true)

        postInvalidateOnAnimation()
    }

    private fun drawFourierWaveSet(canvas: Canvas, width: Float, centerY: Float, time: Float, isMirrored: Boolean) {
        val direction = if (isMirrored) 1f else -1f

        // We iterate 0..3 (Back to Front) to preserve overlapping depth
        for (i in 0 until 1) {
            val path = wavePaths[i]
            val frequencies = layerHarmonics[i]
            path.reset()

            for (x in 0..width.toInt() step 1) {
                val xf = x.toFloat()
                val nX = xf / width

                var fourierSum = 0f

                // Construct the 5-sine composite for this specific layer
                frequencies.forEachIndexed { index, freq ->
                    if (freq > 0f) { // Only calculate if the frequency is active
                        val speedMultiplier = 0.5f + (index * 0.2f)

                        // This is the actual wave calculation
                        val component = sin(nX * freq + (time * speedMultiplier))

                        val weight = 1.0f / (index + 1.2f)
                        fourierSum += component * weight
                    }
                }

                val y = centerY + (fourierSum * amplitudes[i] * direction)

                if (xf == 0f) path.moveTo(xf, y) else path.lineTo(xf, y)
            }

            path.lineTo(width, centerY)
            path.lineTo(0f, centerY)
            path.close()
            canvas.drawPath(path, wavePaints[i])
        }
    }
}