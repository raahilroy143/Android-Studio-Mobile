package com.mrrvk.assistant.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.cos
import kotlin.math.sin

class HologramView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }

    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        maskFilter = BlurMaskFilter(20f, BlurMaskFilter.Blur.NORMAL)
    }

    private val particlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("monospace", Typeface.BOLD)
        letterSpacing = 0.15f
    }

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
    }

    private var rotation = 0f
    private var pulseScale = 1f
    private var waveOffset = 0f
    private var isActive = false
    private var isListening = false
    private var statusText = "STANDBY"
    private var displayText = ""

    private val particles = mutableListOf<Particle>()
    private val ringRotations = FloatArray(5)

    private val primaryColor = Color.parseColor("#00E5FF")
    private val secondaryColor = Color.parseColor("#00B8D4")
    private val accentColor = Color.parseColor("#18FFFF")
    private val dimColor = Color.parseColor("#004D40")
    private val activeColor = Color.parseColor("#00FF88")
    private val listeningColor = Color.parseColor("#FFAB00")

    data class Particle(
        var x: Float, var y: Float,
        var speed: Float, var angle: Float,
        var size: Float, var alpha: Int,
        var life: Float
    )

    private val rotateAnimator = ValueAnimator.ofFloat(0f, 360f).apply {
        duration = 8000
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener { animation ->
            rotation = animation.animatedValue as Float
            for (i in ringRotations.indices) {
                ringRotations[i] = rotation * (1f + i * 0.3f) * if (i % 2 == 0) 1f else -1f
            }
            invalidate()
        }
    }

    private val pulseAnimator = ValueAnimator.ofFloat(0.95f, 1.05f).apply {
        duration = 2000
        repeatCount = ValueAnimator.INFINITE
        repeatMode = ValueAnimator.REVERSE
        addUpdateListener { animation ->
            pulseScale = animation.animatedValue as Float
        }
    }

    private val waveAnimator = ValueAnimator.ofFloat(0f, (2 * Math.PI).toFloat()).apply {
        duration = 3000
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener { animation ->
            waveOffset = animation.animatedValue as Float
            updateParticles()
        }
    }

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
        repeat(50) {
            particles.add(createParticle())
        }
    }

    private fun createParticle(): Particle {
        val angle = (Math.random() * 360).toFloat()
        val distance = (50 + Math.random() * 200).toFloat()
        return Particle(
            x = distance * cos(Math.toRadians(angle.toDouble())).toFloat(),
            y = distance * sin(Math.toRadians(angle.toDouble())).toFloat(),
            speed = (0.5f + Math.random().toFloat() * 2f),
            angle = angle,
            size = (1f + Math.random().toFloat() * 3f),
            alpha = (50 + (Math.random() * 150)).toInt(),
            life = Math.random().toFloat()
        )
    }

    private fun updateParticles() {
        particles.forEachIndexed { index, particle ->
            particle.life -= 0.01f
            particle.y -= particle.speed
            particle.alpha = (particle.alpha * particle.life).toInt().coerceIn(0, 255)

            if (particle.life <= 0 || particle.alpha <= 0) {
                particles[index] = createParticle()
            }
        }
    }

    fun activate() {
        isActive = true
        statusText = "ACTIVE"
        startAnimations()
        invalidate()
    }

    fun deactivate() {
        isActive = false
        isListening = false
        statusText = "STANDBY"
        invalidate()
    }

    fun setListening(listening: Boolean) {
        isListening = listening
        statusText = if (listening) "LISTENING" else if (isActive) "ACTIVE" else "STANDBY"
        invalidate()
    }

    fun setDisplayText(text: String) {
        displayText = text
        invalidate()
    }

    fun startAnimations() {
        if (!rotateAnimator.isRunning) rotateAnimator.start()
        if (!pulseAnimator.isRunning) pulseAnimator.start()
        if (!waveAnimator.isRunning) waveAnimator.start()
    }

    fun stopAnimations() {
        rotateAnimator.cancel()
        pulseAnimator.cancel()
        waveAnimator.cancel()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val cx = width / 2f
        val cy = height / 2f
        val maxRadius = minOf(width, height) / 2.5f

        canvas.save()
        canvas.translate(cx, cy)

        // Background glow
        drawBackgroundGlow(canvas, maxRadius)

        // Concentric rings
        drawRings(canvas, maxRadius)

        // Rotating arcs
        drawRotatingArcs(canvas, maxRadius)

        // Grid lines
        drawGridLines(canvas, maxRadius)

        // Particles
        drawParticles(canvas)

        // Center core
        drawCore(canvas, maxRadius)

        // Sound wave visualization
        if (isListening) {
            drawSoundWaves(canvas, maxRadius)
        }

        // Status text
        drawStatusText(canvas, maxRadius)

        // Display text
        if (displayText.isNotEmpty()) {
            drawDisplayText(canvas, maxRadius)
        }

        canvas.restore()
    }

    private fun drawBackgroundGlow(canvas: Canvas, radius: Float) {
        val currentColor = when {
            isListening -> listeningColor
            isActive -> activeColor
            else -> primaryColor
        }

        val gradient = RadialGradient(
            0f, 0f, radius * 1.5f,
            intArrayOf(Color.argb(30, Color.red(currentColor), Color.green(currentColor), Color.blue(currentColor)), Color.TRANSPARENT),
            null, Shader.TileMode.CLAMP
        )
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = gradient
        }
        canvas.drawCircle(0f, 0f, radius * 1.5f, bgPaint)
    }

    private fun drawRings(canvas: Canvas, maxRadius: Float) {
        val currentColor = when {
            isListening -> listeningColor
            isActive -> activeColor
            else -> primaryColor
        }

        for (i in 1..5) {
            val radius = maxRadius * (i / 5f) * pulseScale
            val alpha = (180 - i * 25).coerceIn(30, 200)

            circlePaint.color = Color.argb(alpha, Color.red(currentColor), Color.green(currentColor), Color.blue(currentColor))
            circlePaint.strokeWidth = if (i == 3) 3f else 1.5f

            canvas.save()
            canvas.rotate(ringRotations.getOrElse(i - 1) { 0f })

            if (i % 2 == 0) {
                // Dashed ring
                circlePaint.pathEffect = DashPathEffect(floatArrayOf(15f, 10f), 0f)
            } else {
                circlePaint.pathEffect = null
            }

            canvas.drawCircle(0f, 0f, radius, circlePaint)
            circlePaint.pathEffect = null
            canvas.restore()
        }
    }

    private fun drawRotatingArcs(canvas: Canvas, maxRadius: Float) {
        val currentColor = when {
            isListening -> listeningColor
            isActive -> activeColor
            else -> primaryColor
        }

        for (i in 0..2) {
            val radius = maxRadius * (0.6f + i * 0.15f)
            val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 3f
                color = Color.argb(150 - i * 40, Color.red(currentColor), Color.green(currentColor), Color.blue(currentColor))
                strokeCap = Paint.Cap.ROUND
            }

            canvas.save()
            canvas.rotate(rotation * (1 + i * 0.5f) * if (i % 2 == 0) 1f else -1f)

            val oval = RectF(-radius, -radius, radius, radius)
            canvas.drawArc(oval, 0f, 60f, false, arcPaint)
            canvas.drawArc(oval, 120f, 60f, false, arcPaint)
            canvas.drawArc(oval, 240f, 60f, false, arcPaint)
            canvas.restore()
        }
    }

    private fun drawGridLines(canvas: Canvas, maxRadius: Float) {
        val gridColor = Color.argb(25, 0, 229, 255)
        linePaint.color = gridColor

        // Radial lines
        for (i in 0..11) {
            val angle = Math.toRadians((i * 30f + rotation * 0.2f).toDouble())
            val x = (maxRadius * cos(angle)).toFloat()
            val y = (maxRadius * sin(angle)).toFloat()
            canvas.drawLine(0f, 0f, x, y, linePaint)
        }
    }

    private fun drawParticles(canvas: Canvas) {
        val currentColor = when {
            isListening -> listeningColor
            isActive -> activeColor
            else -> accentColor
        }

        particles.forEach { particle ->
            particlePaint.color = Color.argb(
                particle.alpha,
                Color.red(currentColor),
                Color.green(currentColor),
                Color.blue(currentColor)
            )
            canvas.drawCircle(particle.x, particle.y, particle.size, particlePaint)
        }
    }

    private fun drawCore(canvas: Canvas, maxRadius: Float) {
        val currentColor = when {
            isListening -> listeningColor
            isActive -> activeColor
            else -> primaryColor
        }

        val coreRadius = maxRadius * 0.15f * pulseScale

        // Core glow
        glowPaint.color = Color.argb(80, Color.red(currentColor), Color.green(currentColor), Color.blue(currentColor))
        canvas.drawCircle(0f, 0f, coreRadius * 1.5f, glowPaint)

        // Core circle
        val coreFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            shader = RadialGradient(
                0f, 0f, coreRadius,
                intArrayOf(currentColor, Color.argb(100, Color.red(currentColor), Color.green(currentColor), Color.blue(currentColor)), Color.TRANSPARENT),
                null, Shader.TileMode.CLAMP
            )
        }
        canvas.drawCircle(0f, 0f, coreRadius, coreFill)

        // Core border
        circlePaint.color = currentColor
        circlePaint.strokeWidth = 2f
        circlePaint.pathEffect = null
        canvas.drawCircle(0f, 0f, coreRadius, circlePaint)
    }

    private fun drawSoundWaves(canvas: Canvas, maxRadius: Float) {
        val wavePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2f
            color = Color.argb(150, 255, 171, 0)
        }

        val waveRadius = maxRadius * 0.35f
        val path = Path()
        val points = 72

        for (wave in 0..2) {
            path.reset()
            val waveAmplitude = 10f + wave * 5f
            val phaseOffset = waveOffset + wave * 0.5f

            for (i in 0..points) {
                val angle = Math.toRadians((i * 360f / points).toDouble())
                val waveR = waveRadius + waveAmplitude * sin(angle * 6 + phaseOffset).toFloat()
                val x = (waveR * cos(angle)).toFloat()
                val y = (waveR * sin(angle)).toFloat()

                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()

            wavePaint.alpha = 150 - wave * 40
            canvas.drawPath(path, wavePaint)
        }
    }

    private fun drawStatusText(canvas: Canvas, maxRadius: Float) {
        val currentColor = when {
            isListening -> listeningColor
            isActive -> activeColor
            else -> primaryColor
        }

        textPaint.color = currentColor
        textPaint.textSize = maxRadius * 0.08f
        canvas.drawText(statusText, 0f, -maxRadius * 0.25f, textPaint)

        // "MR. RVK" text
        textPaint.textSize = maxRadius * 0.14f
        textPaint.color = Color.WHITE
        canvas.drawText("MR. RVK", 0f, maxRadius * 0.05f, textPaint)
    }

    private fun drawDisplayText(canvas: Canvas, maxRadius: Float) {
        textPaint.color = Color.argb(200, 200, 200, 200)
        textPaint.textSize = maxRadius * 0.065f

        // Word wrap
        val maxWidth = maxRadius * 1.5f
        val words = displayText.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = ""

        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            if (textPaint.measureText(testLine) > maxWidth && currentLine.isNotEmpty()) {
                lines.add(currentLine)
                currentLine = word
            } else {
                currentLine = testLine
            }
        }
        if (currentLine.isNotEmpty()) lines.add(currentLine)

        val startY = maxRadius * 0.55f
        val lineHeight = maxRadius * 0.1f
        lines.forEachIndexed { index, line ->
            canvas.drawText(line, 0f, startY + index * lineHeight, textPaint)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopAnimations()
    }
}
