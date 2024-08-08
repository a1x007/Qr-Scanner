package com.ashique.qrscanner.custom

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Parcel
import android.os.Parcelable
import android.text.TextPaint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.Interpolator
import android.view.animation.LinearInterpolator
import android.widget.Scroller
import androidx.core.view.ViewCompat
import androidx.viewpager.widget.ViewPager
import androidx.viewpager.widget.ViewPager.OnPageChangeListener
import com.ashique.qrscanner.R
import java.util.Locale
import java.util.Random
import kotlin.math.pow

class NavigationTabStrip(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr), OnPageChangeListener {
    companion object {

        // NTS constants
        private const val HIGH_QUALITY_FLAGS = Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG
        private const val PREVIEW_TITLE = "Title"
        private const val INVALID_INDEX = -1

        // Default variables
        private const val DEFAULT_ANIMATION_DURATION = 350
        private const val DEFAULT_STRIP_FACTOR = 2.5F
        private const val DEFAULT_STRIP_WEIGHT = 10.0F
        private const val DEFAULT_CORNER_RADIUS = 5.0F
        private const val DEFAULT_INACTIVE_COLOR = Color.GRAY
        private const val DEFAULT_ACTIVE_COLOR = Color.WHITE
        private const val DEFAULT_STRIP_COLOR = Color.RED
        private const val DEFAULT_TITLE_SIZE = 0
        private const val DEFAULT_TITLE_BOLD = false

        // Title size offer to view height
        private const val TITLE_SIZE_FRACTION = 0.35F

        // Max and min fraction
        private const val MIN_FRACTION = 0.0F
        private const val MAX_FRACTION = 1.0F
    }

    // NTS and strip bounds
    private val mBounds = RectF()
    private val mStripBounds = RectF()
    private val mTitleBounds = Rect()

    // Main paint
    private val mStripPaint = Paint(HIGH_QUALITY_FLAGS).apply {
        style = Paint.Style.FILL
    }

    // Paint for tab title
    private val mTitlePaint = TextPaint(HIGH_QUALITY_FLAGS).apply {
        textAlign = Paint.Align.CENTER
    }

    // Variables for animator
    private val mAnimator = ValueAnimator()
    private val mColorEvaluator = ArgbEvaluator()
    private val mResizeInterpolator = ResizeInterpolator()
    private var mAnimationDuration: Int = 0

    // NTS titles
    private var mTitles: Array<String>? = null

    // Variables for ViewPager
    private var mViewPager: ViewPager? = null
    private var mOnPageChangeListener: OnPageChangeListener? = null
    private var mScrollState: Int = 0

    // Tab listener
    private var mOnTabStripSelectedIndexListener: OnTabStripSelectedIndexListener? = null
    private var mAnimatorListener: ValueAnimator.AnimatorUpdateListener? = null

    // Variables for sizes
    private var mTabSize: Float = 0f

    // Tab title size and margin
    private var mTitleSize: Float = 0f

    // Tab title bold if possible with Paint
    private var mTitleBold: Boolean = false

    // Strip type and gravity
    private var mStripType: StripType? = null
    private var mStripGravity: StripGravity? = null

    // Corners radius for rect mode
    private var mStripWeight: Float = 0f
    private var mCornersRadius: Float = 0f

    // Indexes
    private var mLastIndex: Int = INVALID_INDEX
    private var mIndex: Int = INVALID_INDEX

    // General fraction value
    private var mFraction: Float = 0f

    // Coordinates of strip
    private var mStartStripX: Float = 0f
    private var mEndStripX: Float = 0f
    private var mStripLeft: Float = 0f
    private var mStripRight: Float = 0f

    // Detect if is bar mode or indicator pager mode
    private var mIsViewPagerMode: Boolean = false

    // Detect if we move from left to right
    private var mIsResizeIn: Boolean = false

    // Detect if we get action down event
    private var mIsActionDown: Boolean = false

    // Detect if we get action down event on strip
    private var mIsTabActionDown: Boolean = false

    // Detect when we set index from tab bar nor from ViewPager
    private var mIsSetIndexFromTabBar: Boolean = false

    // Color variables
    private var mInactiveColor: Int = DEFAULT_INACTIVE_COLOR
    private var mActiveColor: Int = DEFAULT_ACTIVE_COLOR

    // Custom typeface
    private var mTypeface: Typeface? = null

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    init {
        // Always draw
        setWillNotDraw(false)
        // Speed and fix for pre 17 API
        ViewCompat.setLayerType(this, LAYER_TYPE_SOFTWARE, null)
        setLayerType(LAYER_TYPE_SOFTWARE, null)

        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.NavigationTabStrip)
        try {
            setStripColor(
                typedArray.getColor(R.styleable.NavigationTabStrip_nts_color, DEFAULT_STRIP_COLOR)
            )
            setTitleSize(
                typedArray.getDimension(
                    R.styleable.NavigationTabStrip_nts_size, DEFAULT_TITLE_SIZE.toFloat()
                )
            )
            setTitleBold(
                typedArray.getBoolean(R.styleable.NavigationTabStrip_nts_bold, DEFAULT_TITLE_BOLD)
            )
            setStripWeight(
                typedArray.getDimension(
                    R.styleable.NavigationTabStrip_nts_weight, DEFAULT_STRIP_WEIGHT
                )
            )
            setStripFactor(
                typedArray.getFloat(R.styleable.NavigationTabStrip_nts_factor, DEFAULT_STRIP_FACTOR)
            )
            setStripType(
                typedArray.getInt(R.styleable.NavigationTabStrip_nts_type, StripType.LINE_INDEX)
            )
            setStripGravity(
                typedArray.getInt(
                    R.styleable.NavigationTabStrip_nts_gravity, StripGravity.BOTTOM_INDEX
                )
            )

            setTypeface(typedArray.getString(R.styleable.NavigationTabStrip_nts_typeface))
            setInactiveColor(
                typedArray.getColor(
                    R.styleable.NavigationTabStrip_nts_inactive_color, DEFAULT_INACTIVE_COLOR
                )
            )
            setActiveColor(
                typedArray.getColor(
                    R.styleable.NavigationTabStrip_nts_active_color, DEFAULT_ACTIVE_COLOR
                )
            )
            setAnimationDuration(
                typedArray.getInteger(
                    R.styleable.NavigationTabStrip_nts_animation_duration,
                    DEFAULT_ANIMATION_DURATION
                )
            )
            setCornersRadius(
                typedArray.getDimension(
                    R.styleable.NavigationTabStrip_nts_corners_radius, DEFAULT_CORNER_RADIUS
                )
            )

            // Get titles
            var titles: Array<String>? = null
            try {
                val titlesResId = typedArray.getResourceId(
                    R.styleable.NavigationTabStrip_nts_titles, 0
                )
                titles = if (titlesResId == 0) null else resources.getStringArray(titlesResId)
            } catch (exception: Exception) {
                titles = null
                exception.printStackTrace()
            } finally {
                if (titles == null) {
                    titles = if (isInEditMode) {
                        Array(Random().nextInt(5) + 1) { PREVIEW_TITLE }
                    } else {
                        emptyArray()
                    }
                }

                setTitles(*titles)
            }

            // Init animator
            mAnimator.setFloatValues(MIN_FRACTION, MAX_FRACTION)
            mAnimator.interpolator = LinearInterpolator()
            mAnimator.addUpdateListener { animation ->
                updateIndicatorPosition(animation.animatedValue as Float)
            }
        } finally {
            typedArray.recycle()
        }
    }


    // Getter for animation duration
    fun getAnimationDuration(): Int = mAnimationDuration

    // Setter for animation duration
    fun setAnimationDuration(animationDuration: Int) {
        mAnimationDuration = animationDuration
        mAnimator.duration = mAnimationDuration.toLong()
        resetScroller()
    }

    // Getter for titles
    fun getTitles(): Array<String>? = mTitles

    // Setter for titles with resource IDs
    fun setTitles(vararg titleResIds: Int) {
        val titles = Array(titleResIds.size) { index ->
            resources.getString(titleResIds[index])
        }
        setTitles(*titles)
    }

    // Setter for titles with strings
    fun setTitles(vararg titles: String) {
        val upperCaseTitles = titles.map { it.uppercase(Locale.ROOT) }.toTypedArray()
        mTitles = upperCaseTitles
        requestLayout()
    }

    // Getter for strip color
    fun getStripColor(): Int = mStripPaint.color

    // Setter for strip color
    fun setStripColor(color: Int) {
        mStripPaint.color = color
        postInvalidate()
    }

    // Setter for strip weight
    fun setStripWeight(stripWeight: Float) {
        mStripWeight = stripWeight
        requestLayout()
    }

    // Getter for strip gravity
    fun getStripGravity(): StripGravity? = mStripGravity

    // Private setter for strip gravity with index
    private fun setStripGravity(index: Int) {
        mStripGravity = when (index) {
            StripGravity.TOP_INDEX -> StripGravity.TOP
            else -> StripGravity.BOTTOM
        }
    }

    // Setter for strip gravity
    fun setStripGravity(stripGravity: StripGravity) {
        mStripGravity = stripGravity
        requestLayout()
    }

    // Getter for strip type
    fun getStripType(): StripType? = mStripType

    // Private setter for strip type with index
    private fun setStripType(index: Int) {
        mStripType = when (index) {
            StripType.POINT_INDEX -> StripType.POINT
            else -> StripType.LINE
        }
    }

    // Setter for strip type
    fun setStripType(stripType: StripType) {
        mStripType = stripType
        requestLayout()
    }

    // Getter for strip factor
    fun getStripFactor(): Float = mResizeInterpolator.factor

    // Setter for strip factor
    fun setStripFactor(factor: Float) {
        mResizeInterpolator.factor = factor
    }

    // Getter for typeface
    fun getTypeface(): Typeface? = mTypeface

    // Setter for typeface with string
    fun setTypeface(typeface: String?) {
        if (typeface.isNullOrEmpty()) return

        val tempTypeface: Typeface = try {
            Typeface.createFromAsset(context.assets, typeface)
        } catch (e: Exception) {
            Typeface.create(Typeface.DEFAULT, Typeface.NORMAL).also {
                e.printStackTrace()
            }
        }

        setTypeface(tempTypeface)
    }

    // Setter for typeface
    fun setTypeface(typeface: Typeface?) {
        mTypeface = typeface
        mTitlePaint.typeface = typeface
        postInvalidate()
    }

    // Getter for active color
    fun getActiveColor(): Int = mActiveColor

    // Setter for active color
    fun setActiveColor(activeColor: Int) {
        mActiveColor = activeColor
        postInvalidate()
    }

    // Getter for inactive color
    fun getInactiveColor(): Int = mInactiveColor

    // Setter for inactive color
    fun setInactiveColor(inactiveColor: Int) {
        mInactiveColor = inactiveColor
        postInvalidate()
    }

    // Getter for corners radius
    fun getCornersRadius(): Float = mCornersRadius

    // Setter for corners radius
    fun setCornersRadius(cornersRadius: Float) {
        mCornersRadius = cornersRadius
        postInvalidate()
    }

    // Getter for title size
    fun getTitleSize(): Float = mTitleSize

    // Setter for title size
    fun setTitleSize(titleSize: Float) {
        mTitleSize = titleSize
        mTitlePaint.textSize = titleSize
        postInvalidate()
    }

    // Getter for title bold
    fun getTitleBold(): Boolean = mTitleBold

    // Setter for title bold
    fun setTitleBold(titleBold: Boolean) {
        mTitleBold = titleBold
        mTitlePaint.isFakeBoldText = titleBold
        postInvalidate()
    }

    // Getter for OnTabStripSelectedIndexListener
    fun getOnTabStripSelectedIndexListener(): OnTabStripSelectedIndexListener? =
        mOnTabStripSelectedIndexListener

    /**
    // Set on tab bar selected index listener where you can trigger action onStart or onEnd
    fun setOnTabStripSelectedIndexListener(onTabStripSelectedIndexListener: OnTabStripSelectedIndexListener?) {
    mOnTabStripSelectedIndexListener = onTabStripSelectedIndexListener

    if (mAnimatorListener == null) {
    mAnimatorListener = object : AnimatorListenerAdapter() {
    override fun onAnimationStart(animation: Animator) {
    mOnTabStripSelectedIndexListener?.onStartTabSelected(mTitles?.get(mIndex) ?: "", mIndex)
    animation.removeListener(this)
    animation.addListener(this)
    }

    override fun onAnimationEnd(animation: Animator) {
    if (mIsViewPagerMode) return

    animation.removeListener(this)
    animation.addListener(this)
    mOnTabStripSelectedIndexListener?.onEndTabSelected(mTitles?.get(mIndex) ?: "", mIndex)
    }
    }
    }

    mAnimator.removeListener(mAnimatorListener)
    mAnimator.addListener(mAnimatorListener)
    }
     **/


    // Set ViewPager and configure mode
    fun setViewPager(viewPager: ViewPager?) {
        // Detect whether ViewPager mode
        if (viewPager == null) {
            mIsViewPagerMode = false
            return
        }

        if (viewPager == mViewPager) return
        mViewPager?.setOnPageChangeListener(null) // Deprecated method; consider updating if needed
        viewPager.adapter
            ?: throw IllegalStateException("ViewPager does not provide adapter instance.")

        mIsViewPagerMode = true
        mViewPager = viewPager
        mViewPager?.addOnPageChangeListener(this)

        resetScroller()
        postInvalidate()
    }

    // Set ViewPager with initial index
    fun setViewPager(viewPager: ViewPager?, index: Int) {
        setViewPager(viewPager)

        mIndex = index
        if (mIsViewPagerMode) mViewPager?.setCurrentItem(index, true)
        postInvalidate()
    }

    // Reset scroller and set scroll duration to animation duration
    private fun resetScroller() {
        mViewPager ?: return
        try {
            val scrollerField = ViewPager::class.java.getDeclaredField("mScroller")
            scrollerField.isAccessible = true
            val scroller = ResizeViewPagerScroller(context)
            scrollerField[mViewPager] = scroller
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Set OnPageChangeListener
    fun setOnPageChangeListener(listener: OnPageChangeListener?) {
        mOnPageChangeListener = listener
    }

    // Get current tab index
    fun getTabIndex(): Int = mIndex

    // Set tab index
    fun setTabIndex(index: Int) {
        setTabIndex(index, false)
    }

    // Set tab index from touch or programmatically
    fun setTabIndex(tabIndex: Int, isForce: Boolean) {
        if (mAnimator.isRunning) return
        if (mTitles?.isEmpty() == true) return

        var index = tabIndex
        var force = isForce

        // This check gives us an opportunity to have a non-selected tab
        if (mIndex == INVALID_INDEX) force = true

        // Snap index to tabs size
        index = index.coerceIn(0, mTitles?.size?.minus(1) ?: 0)

        mIsResizeIn = index < mIndex
        mLastIndex = mIndex
        mIndex = index

        mIsSetIndexFromTabBar = true
        if (mIsViewPagerMode) {
            mViewPager ?: throw IllegalStateException("ViewPager is null.")
            mViewPager!!.setCurrentItem(index, !force)
        }

        // Set startX and endX for animation
        mStartStripX = mStripLeft
        mEndStripX =
            (mIndex * mTabSize) + if (mStripType == StripType.POINT) mTabSize * 0.5F else 0.0F

        // Immediate update if forced, else animate
        if (force) {
            updateIndicatorPosition(MAX_FRACTION)
            // Force onPageScrolled listener and refresh VP
            if (mIsViewPagerMode) {
                mViewPager?.let {
                    if (!it.isFakeDragging) it.beginFakeDrag()
                    if (it.isFakeDragging) {
                        it.fakeDragBy(0.0F)
                        it.endFakeDrag()
                    }
                }
            }
        } else {
            mAnimator.start()
        }
    }


    // Deselect active index and reset pointer
    fun deselect() {
        mLastIndex = INVALID_INDEX
        mIndex = INVALID_INDEX
        mStartStripX = INVALID_INDEX * mTabSize
        mEndStripX = mStartStripX
        updateIndicatorPosition(MIN_FRACTION)
    }

    // Update indicator position based on the fraction
    private fun updateIndicatorPosition(fraction: Float) {
        // Update general fraction
        mFraction = fraction

        // Set the strip left side coordinate
        mStripLeft = mStartStripX + (mResizeInterpolator.getResizeInterpolation(
            fraction,
            mIsResizeIn
        ) * (mEndStripX - mStartStripX))

        // Set the strip right side coordinate
        mStripRight =
            (mStartStripX + if (mStripType == StripType.LINE) mTabSize else mStripWeight) + (mResizeInterpolator.getResizeInterpolation(
                fraction,
                !mIsResizeIn
            ) * (mEndStripX - mStartStripX))

        // Update NTS
        postInvalidate()
    }

    // Notify that data has changed
    private fun notifyDataSetChanged() {
        requestLayout()
        postInvalidate()
    }


    override fun onTouchEvent(event: MotionEvent): Boolean {
        // Return if animation is running
        if (mAnimator.isRunning) return true
        // If not idle state, return
        if (mScrollState != ViewPager.SCROLL_STATE_IDLE) return true

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                // Action down touch
                mIsActionDown = true
                if (!mIsViewPagerMode) return true
                // Detect if we touch down on tab, later to move
                mIsTabActionDown = (event.x / mTabSize).toInt() == mIndex
            }

            MotionEvent.ACTION_MOVE -> {
                // If tab touched, move
                if (mIsTabActionDown) {
                    mViewPager?.setCurrentItem((event.x / mTabSize).toInt(), true)
                }
                if (mIsActionDown) return true
            }

            MotionEvent.ACTION_UP -> {
                // Press up and set tab index relative to current coordinate
                if (mIsActionDown) setTabIndex((event.x / mTabSize).toInt())
            }

            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_OUTSIDE -> {
                // Reset action touch variables
                mIsTabActionDown = false
                mIsActionDown = false
            }

            else -> {
                // Reset action touch variables
                mIsTabActionDown = false
                mIsActionDown = false
            }
        }

        return true
    }


    @SuppressLint("DrawAllocation")
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        // Get measure size
        val width = MeasureSpec.getSize(widthMeasureSpec).toFloat()
        val height = MeasureSpec.getSize(heightMeasureSpec).toFloat()

        // Set bounds for NTS
        mBounds.set(0.0F, 0.0F, width, height)

        if (mTitles?.isEmpty() == true || width == 0F || height == 0F) return

        // Get smaller side
        mTabSize = width / mTitles?.size?.toFloat()!!
        if (mTitleSize.toInt() == DEFAULT_TITLE_SIZE) {
            setTitleSize((height - mStripWeight) * TITLE_SIZE_FRACTION)
        }

        // Set start position of strip for preview or on start
        if (isInEditMode || !mIsViewPagerMode) {
            mIsSetIndexFromTabBar = true

            // Set random in preview mode
            if (isInEditMode) mIndex = Random().nextInt(mTitles!!.size)

            mStartStripX =
                (mIndex * mTabSize) + if (mStripType == StripType.POINT) mTabSize * 0.5F else 0.0F
            mEndStripX = mStartStripX
            updateIndicatorPosition(MAX_FRACTION)
        }
    }


    override fun onDraw(canvas: Canvas) {
        // Set bound of strip
        mStripBounds.set(
            mStripLeft - if (mStripType == StripType.POINT) mStripWeight * 0.5F else 0.0F,
            if (mStripGravity == StripGravity.BOTTOM) mBounds.height() - mStripWeight else 0.0F,
            mStripRight - if (mStripType == StripType.POINT) mStripWeight * 0.5F else 0.0F,
            if (mStripGravity == StripGravity.BOTTOM) mBounds.height() else mStripWeight
        )

        // Draw strip
        if (mCornersRadius == 0F) canvas.drawRect(mStripBounds, mStripPaint)
        else canvas.drawRoundRect(mStripBounds, mCornersRadius, mCornersRadius, mStripPaint)

        // Draw tab titles
        for (i in mTitles?.indices!!) {
            val title = mTitles!![i]

            val leftTitleOffset = (mTabSize * i) + (mTabSize * 0.5F)

            mTitlePaint.getTextBounds(title, 0, title.length, mTitleBounds)
            val topTitleOffset =
                (mBounds.height() - mStripWeight) * 0.5F + mTitleBounds.height() * 0.5F - mTitleBounds.bottom

            // Get interpolated fraction for left last and current tab
            val interpolation = mResizeInterpolator.getResizeInterpolation(mFraction, true)
            val lastInterpolation = mResizeInterpolator.getResizeInterpolation(mFraction, false)

            // Check if we handle tab from touch on NTS or from ViewPager
            // There is a strange logic of ViewPager onPageScrolled method, so it is
            if (mIsSetIndexFromTabBar) {
                when {
                    mIndex == i -> updateCurrentTitle(interpolation)
                    mLastIndex == i -> updateLastTitle(lastInterpolation)
                    else -> updateInactiveTitle()
                }
            } else {
                when {
                    i != mIndex && i != mIndex + 1 -> updateInactiveTitle()
                    i == mIndex + 1 -> updateCurrentTitle(interpolation)
                    i == mIndex -> updateLastTitle(lastInterpolation)
                }
            }

            canvas.drawText(
                title,
                leftTitleOffset,
                topTitleOffset + if (mStripGravity == StripGravity.TOP) mStripWeight else 0.0F,
                mTitlePaint
            )
        }
    }

    // Method to transform current fraction of NTS and position
    private fun updateCurrentTitle(interpolation: Float) {
        mTitlePaint.color =
            mColorEvaluator.evaluate(interpolation, mInactiveColor, mActiveColor) as Int
    }

    // Method to transform last fraction of NTS and position
    private fun updateLastTitle(lastInterpolation: Float) {
        mTitlePaint.color =
            mColorEvaluator.evaluate(lastInterpolation, mActiveColor, mInactiveColor) as Int
    }

    // Method to transform others fraction of NTS and position
    private fun updateInactiveTitle() {
        mTitlePaint.color = mInactiveColor
    }

    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
        mOnPageChangeListener?.onPageScrolled(position, positionOffset, positionOffsetPixels)

        // If we animate, don't call this
        if (!mIsSetIndexFromTabBar) {
            mIsResizeIn = position < mIndex
            mLastIndex = mIndex
            mIndex = position

            mStartStripX =
                (position * mTabSize) + if (mStripType == StripType.POINT) mTabSize * 0.5F else 0.0F
            mEndStripX = mStartStripX + mTabSize
            updateIndicatorPosition(positionOffset)
        }

        // Stop scrolling on animation end and reset values
        if (!mAnimator.isRunning && mIsSetIndexFromTabBar) {
            mFraction = MIN_FRACTION
            mIsSetIndexFromTabBar = false
        }
    }

    override fun onPageSelected(position: Int) {
        // This method is empty, because we call onPageSelected() when scroll state is idle
    }

    override fun onPageScrollStateChanged(state: Int) {
        // If VP idle, reset to MIN_FRACTION
        mScrollState = state
        if (state == ViewPager.SCROLL_STATE_IDLE) {
            mOnPageChangeListener?.onPageSelected(mIndex)
            if (mIsViewPagerMode) {
                mOnTabStripSelectedIndexListener?.onEndTabSelected(
                    mTitles?.get(mIndex) ?: "", mIndex
                )
            }
        }

        mOnPageChangeListener?.onPageScrollStateChanged(state)
    }

    override fun onRestoreInstanceState(state: Parcelable) {
        val savedState = state as SavedState
        super.onRestoreInstanceState(savedState.superState)
        mIndex = savedState.index
        requestLayout()
    }

    override fun onSaveInstanceState(): Parcelable {
        val superState = super.onSaveInstanceState()
        val savedState = SavedState(superState)
        savedState.index = mIndex
        return savedState
    }

    // Save current index instance
    private class SavedState : BaseSavedState {

        var index: Int = 0

        constructor(superState: Parcelable?) : super(superState)

        private constructor(parcel: Parcel) : super(parcel) {
            index = parcel.readInt()
        }

        override fun writeToParcel(dest: Parcel, flags: Int) {
            super.writeToParcel(dest, flags)
            dest.writeInt(index)
        }

        companion object CREATOR : Parcelable.Creator<SavedState> {
            override fun createFromParcel(parcel: Parcel): SavedState {
                return SavedState(parcel)
            }

            override fun newArray(size: Int): Array<SavedState?> {
                return arrayOfNulls(size)
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        requestLayout()

        // Refresh strip and state after config changed to current
        val tempIndex = mIndex
        deselect()
        post {
            setTabIndex(tempIndex, true)
        }
    }

    // Custom scroller with custom scroll duration
    private inner class ResizeViewPagerScroller(context: Context) :
        Scroller(context, AccelerateDecelerateInterpolator()) {

        override fun startScroll(startX: Int, startY: Int, dx: Int, dy: Int, duration: Int) {
            super.startScroll(startX, startY, dx, dy, mAnimationDuration)
        }

        override fun startScroll(startX: Int, startY: Int, dx: Int, dy: Int) {
            super.startScroll(startX, startY, dx, dy, mAnimationDuration)
        }
    }

    // Resize interpolator to create smooth effect on strip according to inspiration design
    private class ResizeInterpolator : Interpolator {

        // Spring factor
        private var _factor: Float = 0f

        // Check whether side we move
        private var resizeIn: Boolean = false

        var factor: Float
            get() = _factor
            set(value) {
                _factor = value
            }

        override fun getInterpolation(input: Float): Float {
            return if (resizeIn) {
                1.0f - (1.0 - input).pow(2.0 * _factor).toFloat()
            } else {
                input.toDouble().pow(2.0 * _factor).toFloat()
            }
        }

        fun getResizeInterpolation(input: Float, resizeIn: Boolean): Float {
            this.resizeIn = resizeIn
            return getInterpolation(input)
        }
    }


    // NTS strip type
    enum class StripType {
        LINE, POINT;

        companion object {
            const val LINE_INDEX = 0
            const val POINT_INDEX = 1
        }
    }

    // NTS strip gravity
    enum class StripGravity {
        BOTTOM, TOP;

        companion object {
            const val BOTTOM_INDEX = 0
            const val TOP_INDEX = 1
        }
    }

    // Out listener for selected index
    interface OnTabStripSelectedIndexListener {
        fun onStartTabSelected(title: String, index: Int)

        fun onEndTabSelected(title: String, index: Int)
    }

}