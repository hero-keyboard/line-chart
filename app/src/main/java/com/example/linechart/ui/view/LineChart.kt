package com.example.linechart.ui.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.example.linechart.R

class LineChart : View {
    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        initData(attrs)
    }

    private var isTouch = false
    private var pointTouch = PointF()
    private var minValue = DEFAULT_MIN_VALUE
    private var maxValue = DEFAULT_MAX_VALUE
    private var marginLeft = MARGIN_LEFT_PARENT
    private var marginRight = MARGIN_RIGHT_PARENT
    private var marginTop = MARGIN_TOP_PARENT
    private var marginBottom = MARGIN_BOTTOM_PARENT
    private var lineNumber = 0
    private var spaceBetweenLines = 0F
    private var spaceVertical = 0F
    private var bottomPosition = 0F
    private var heightContent = 0F
    private var widthContent = 0F

    private val path = Path()
    private val pathBackground = Path()
    private val paintHorizontalLine = Paint()
    private val paintText = Paint()
    private val paintLine = Paint()
    private val paintBackground = Paint()
    private val bounds = Rect()

    private val controlPoint1 = arrayListOf<PointF>()
    private val controlPoint2 = arrayListOf<PointF>()
    private val pointS = arrayListOf<PointF>()
    private val textValue = arrayListOf<String>()
    private var valueData = floatArrayOf()
    fun setData(values: FloatArray) {
        valueData = values
    }

    @SuppressLint("CustomViewStyleable")
    private fun initData(attrs: AttributeSet?) {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.LineChartView)
        maxValue = typedArray.getInt(R.styleable.LineChartView_lineMaxValue, DEFAULT_MAX_VALUE)
        minValue = typedArray.getInt(R.styleable.LineChartView_lineMinValue, DEFAULT_MIN_VALUE)
        lineNumber = 1 + (maxValue - minValue) / STEP
        typedArray.recycle()
    }

    private fun calculateControlPoint() {
        for (i in 1 until pointS.size) {
            controlPoint1.add(PointF((pointS[i].x + pointS[i - 1].x) / 2, pointS[i - 1].y))
            controlPoint2.add(PointF((pointS[i].x + pointS[i - 1].x) / 2, pointS[i].y))
        }
    }

    private fun calculatePointS() {
        valueData.forEachIndexed { index, value ->
            val xPosition = (index * spaceVertical) + marginLeft
            val yPosition = bottomPosition - ((-minValue + value) * spaceBetweenLines / STEP)
            pointS.add(PointF(xPosition, yPosition))
        }
    }

    private fun getTextValue() {
        for (i in minValue..maxValue step STEP) {
            textValue.add(i.toString())
        }
    }

    private val gradientColors = intArrayOf(
        ContextCompat.getColor(context, R.color.gradient_start_line_chart),
        ContextCompat.getColor(context, R.color.gradient_end_line_chart),
    )

    private fun initPain() {
        paintHorizontalLine.apply {
            isAntiAlias = true
            strokeJoin = Paint.Join.ROUND
            color = ContextCompat.getColor(context, R.color.color_line_line_chart)
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }

        paintText.apply {
            isAntiAlias = true
            color = ContextCompat.getColor(context, R.color.color_line_line_chart)
            style = Paint.Style.FILL
            textSize = 36F
        }

        paintLine.apply {
            isAntiAlias = true
            color = ContextCompat.getColor(context, R.color.blue)
            style = Paint.Style.STROKE
            strokeWidth = 5f
        }

        paintBackground.apply {
            isAntiAlias = true
            style = Paint.Style.FILL
            shader = LinearGradient(
                widthContent / 2, marginTop, widthContent / 2, bottomPosition, gradientColors,
                null,
                Shader.TileMode.CLAMP
            )
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        marginLeft = w * MARGIN_LEFT_PARENT
        marginRight = w * MARGIN_RIGHT_PARENT
        marginTop = h * MARGIN_TOP_PARENT
        marginBottom = h * MARGIN_BOTTOM_PARENT
        bottomPosition = h - marginBottom
        heightContent = h - marginBottom - marginTop
        widthContent = w - marginLeft - marginRight
        spaceBetweenLines = heightContent / (lineNumber - 1)
        spaceVertical = widthContent / (valueData.size - 1)

        initPain()
        calculatePointS()
        calculateControlPoint()
        getTextValue()
    }


    private fun drawHorizontalLine(canvas: Canvas?) {
        path.reset()
        path.fillType = Path.FillType.EVEN_ODD
        for (i in 0 until lineNumber) {
            path.moveTo(marginLeft, marginTop + (i * spaceBetweenLines))
            path.lineTo(marginLeft + widthContent, marginTop + (i * spaceBetweenLines))
            path.close()
            canvas?.drawPath(path, paintHorizontalLine)
        }
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        drawHorizontalLine(canvas)
        drawTextValue(canvas)
        drawLine(canvas)

        if (isTouch) {
            drawLineVertical(canvas)
            drawViewText(canvas)
        }
    }

    private fun drawViewText(canvas: Canvas?) {

    }

    private fun drawTextValue(canvas: Canvas?) {
        textValue.forEachIndexed { index, value ->
            paintText.getTextBounds(value, 0, value.length, bounds)
            val yPosition = bottomPosition + bounds.height() / 2 - (index * spaceBetweenLines)
            val xPosition = marginLeft - bounds.width() - MARGIN_TEXT_END
            canvas?.drawText(value, xPosition, yPosition, paintText)
        }
    }

    private val pointOnPath = mutableListOf<PointF>()
    private fun findPointOnCubicToPath() {
        var i = 0f
        val pm = PathMeasure(path, false)
        while (i <= 1) {
            val position = floatArrayOf(0f, 0f)
            pm.getPosTan(pm.length * i, position, null)
            i += 0.001F
            pointOnPath.add(PointF(position[0], position[1]))
        }
    }

    private fun drawLine(canvas: Canvas?) {
        if (pointS.isEmpty() && controlPoint1.isEmpty() && controlPoint2.isEmpty()) return
        path.reset()

        path.moveTo(pointS.first().x, pointS.first().y)
        for (i in 1 until pointS.size) {
            path.cubicTo(
                controlPoint1[i - 1].x,
                controlPoint1[i - 1].y,
                controlPoint2[i - 1].x,
                controlPoint2[i - 1].y,
                pointS[i].x,
                pointS[i].y
            )
        }
        canvas?.drawPath(path, paintLine)

        pathBackground.set(path)
        pathBackground.lineTo(marginLeft + widthContent, bottomPosition)
        pathBackground.lineTo(marginLeft, bottomPosition)
        pathBackground.lineTo(pointS.first().x, pointS.first().y)
        canvas?.drawPath(pathBackground, paintBackground)
        findPointOnCubicToPath()
    }

    private fun drawLineVertical(canvas: Canvas?) {
        canvas?.drawCircle(pointTouch.x, pointTouch.y, 10f, paintLine)
        canvas?.drawLine(pointTouch.x, pointTouch.y, pointTouch.x, bottomPosition, paintLine)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event?.action == MotionEvent.ACTION_MOVE || event?.action == MotionEvent.ACTION_DOWN) {
            isTouch = true
            val positionTouchX = event.x
            val positionTouchY = event.y

            val isPointValid = pointOnPath.find { point ->
                point.x < positionTouchX + SPACE_TOUCH
                        && point.x > positionTouchX - SPACE_TOUCH
                        && point.y < positionTouchY + SPACE_TOUCH
                        && point.y > positionTouchY - SPACE_TOUCH
            }

            if (isPointValid != null) {
                pointTouch = isPointValid
                invalidate()
            }
        }
        if (event?.action == MotionEvent.ACTION_UP) {
            isTouch = false
        }
        return true
    }

    companion object {
        private const val DEFAULT_MAX_VALUE = 10
        private const val DEFAULT_MIN_VALUE = -30
        private const val STEP = 10
        private const val MARGIN_TEXT_END = 15F

        private const val SPACE_TOUCH = 20F

        private const val MARGIN_LEFT_PARENT = 0.08f
        private const val MARGIN_TOP_PARENT = 0.05f
        private const val MARGIN_RIGHT_PARENT = 0.03f
        private const val MARGIN_BOTTOM_PARENT = 0.05f
    }
}