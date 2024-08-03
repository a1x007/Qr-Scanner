package com.ashique.qrscanner.custom

import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.drawable.*
import android.os.Build
import android.util.AttributeSet
import android.util.Log
import android.util.StateSet
import android.view.Gravity
import android.widget.SeekBar
import androidx.appcompat.widget.AppCompatSeekBar
import codes.side.andcolorpicker.R
import com.ashique.qrscanner.R as r
import codes.side.andcolorpicker.converter.ColorConverter
import codes.side.andcolorpicker.converter.ColorConverterHub
import codes.side.andcolorpicker.model.Color
import codes.side.andcolorpicker.model.factory.ColorFactory
import codes.side.andcolorpicker.util.marker

@Suppress(
  "ConstantConditionIf",
  "LeakingThis"
)
abstract class ColorSeekBar<C : Color> @JvmOverloads constructor(
  private val colorFactory: ColorFactory<C>,
  context: Context,
  attrs: AttributeSet? = null,
  defStyle: Int = R.attr.seekBarStyle
) :
  AppCompatSeekBar(
    context,
    attrs,
    defStyle
  ),
  SeekBar.OnSeekBarChangeListener {

  // TODO: Revisit factory-based approach
  private val _pickedColor: C = colorFactory.create()
  var pickedColor: C
    get() {
      return colorFactory.createColorFrom(_pickedColor)
    }
    set(value) {
      if (DEBUG) {
        Log.d(
          TAG,
          "currentColor set() called on $this with $value"
        )
      }
      if (_pickedColor == value) {
        return
      }
      updateInternalPickedColorFrom(value)
      refreshProgressFromCurrentColor()
      refreshProgressDrawable()
      refreshThumb()
      notifyListenersOnColorChanged()
    }

  protected val internalPickedColor: C
    get() {
      return _pickedColor
    }

  protected open val colorConverter: ColorConverter
    get() {
      return ColorConverterHub.getConverterByKey(internalPickedColor.colorKey)
    }

  var notifyListeners = true

  // Dirty hack to stop onProgressChanged while playing with min/max
  private var minUpdating = false
  private var maxUpdating = false

  private val colorPickListeners = hashSetOf<OnColorPickListener<ColorSeekBar<C>, C>>()
  private lateinit var thumbDrawable: GradientDrawable
  private lateinit var thumbObjectAnimator: ObjectAnimator


  private val thumbColoringDrawables = hashSetOf<Drawable>()



  init {
    splitTrack = false

    setOnSeekBarChangeListener(this)

    setupBackground()
    setupProgressDrawable()
    setupThumb()

    refreshProperties()
    refreshProgressFromCurrentColor()
    refreshProgressDrawable()
    refreshThumb()
  }

  /*
   * Setups views's background drawable. Adjusts initial thumb ripple size.
   */
  private fun setupBackground() {
    background?.let { originalBackground ->
      background = originalBackground.mutate().also {
        if (it is RippleDrawable) {
          val rippleSizePx = resources.getDimensionPixelOffset(R.dimen.acp_thumb_ripple_radius)
          it.radius = rippleSizePx
        }
      }
    }
  }

  private fun setupProgressDrawable() {
    if (DEBUG) {
      Log.d(
        TAG,
        "setupProgressDrawable() called on $this"
      )
    }

    val progressPaddingPx = resources.getDimensionPixelOffset(R.dimen.acp_seek_progress_padding)
    val progressHeightPx = resources.getDimensionPixelOffset(R.dimen.acp_seek_progress_height)

    val layers = onSetupProgressDrawableLayers(arrayOf())
    progressDrawable = LayerDrawable(
      layers
    ).also {
      // We're limited to insets on API 21
      // Migrate to layer height / padding on API 23+
      // TODO: Allow to configure whether to use insets or not
      layers.forEachIndexed { index, _ ->
        it.setLayerInset(
          index,
          progressPaddingPx,
          progressPaddingPx,
          progressPaddingPx,
          progressPaddingPx
        )
      }
    }
  }

  // Immutable array to limit hacks
  // Inherited layers may be not fully built by this method invocation time
  protected abstract fun onSetupProgressDrawableLayers(layers: Array<Drawable>): Array<Drawable>

  private fun setupThumb() {
    val backgroundPaddingPx = resources.getDimensionPixelOffset(r.dimen.acp_seek_progress_padding)
    val thumbFullSizePx = resources.getDimensionPixelOffset(r.dimen.acp_thumb_size_full)

    thumbDrawable = GradientDrawable().also {
      it.setColor(android.graphics.Color.WHITE)
      it.shape = GradientDrawable.OVAL
      it.setSize(
        thumbFullSizePx,
        thumbFullSizePx
      )
    }

    thumbColoringDrawables.add(thumbDrawable)

    thumb = ScaleDrawable(
      StateListDrawable().also {
        it.addState(
          StateSet.WILD_CARD,
          thumbDrawable
        )
      },
      Gravity.CENTER,
      1f,
      1f
    ).also {
      it.level = THUMB_DRAWABLE_LEVEL_DEFAULT
    }

    thumbObjectAnimator = ObjectAnimator.ofInt(
      thumb,
      "level",
      THUMB_DRAWABLE_LEVEL_DEFAULT,
      THUMB_DRAWABLE_LEVEL_PRESSED
    ).also {
      it.duration = THUMB_ANIM_DURATION
    }

    thumbOffset -= backgroundPaddingPx / 2
  }

  override fun setMin(min: Int) {
    if (min != 0) {
      throw IllegalArgumentException("Current mode supports 0 min value only, was $min")
    }
    ::minUpdating.marker {
      super.setMin(min)
    }
  }

  override fun setMax(max: Int) {
    ::maxUpdating.marker {
      super.setMax(max)
    }
  }

  /*
   * Silently updates internal picked color from provided value.
   */
  private fun updateInternalPickedColorFrom(value: C) {
    if (DEBUG) {
      Log.d(
        TAG,
        "updateInternalCurrentColorFrom() called on $this"
      )
    }

    onUpdateColorFrom(
      internalPickedColor,
      value
    )
  }

  protected abstract fun onUpdateColorFrom(color: C, value: C)

  /*
   * Refresh or set basic SeekBar properties like min/max.
   */
  protected fun refreshProperties() {
    if (DEBUG) {
      Log.d(
        TAG,
        "refreshProperties() called on $this"
      )
    }

    onRefreshProperties()
  }

  protected abstract fun onRefreshProperties()

  /*
   * Synchronizes progress from picked color.
   */
  protected fun refreshProgressFromCurrentColor() {
    if (DEBUG) {
      Log.d(
        TAG,
        "refreshProgressFromCurrentColor() called on $this"
      )
    }

    val result = onRefreshProgressFromColor(internalPickedColor)
    result?.let {
      progress = it
    }
  }

  protected abstract fun onRefreshProgressFromColor(color: C): Int?

  /*
   * Synchronizes internal picked color from progress while bypassing public API
   * pickedColor setter and consequent calls.
   */
  private fun refreshInternalPickedColorFromProgress() {
    if (DEBUG) {
      Log.d(
        TAG,
        "refreshInternalCurrentColorFromProgress() called on $this"
      )
    }

    val changed = onRefreshColorFromProgress(
      internalPickedColor,
      progress
    )

    if (changed) {
      notifyListenersOnColorChanged()
    }
  }

  protected abstract fun onRefreshColorFromProgress(color: C, progress: Int): Boolean

  /*
   * Refreshes SeekBar's progress drawable according to derived class details.
   * CONTRACT: Derived class is responsible for progressDrawable changes if needed.
   */
  protected fun refreshProgressDrawable() {
    if (DEBUG) {
      Log.d(
        TAG,
        "refreshProgressDrawable() called on $this"
      )
    }

    onRefreshProgressDrawable(progressDrawable as LayerDrawable)
  }

  protected abstract fun onRefreshProgressDrawable(progressDrawable: LayerDrawable)

  /**
   * Refreshes SeekBar's thumb drawable according to derived class details.
   * CONTRACT: Should paint GradientDrawable and first layer of LayerDrawable
   */
  protected fun refreshThumb() {
    if (DEBUG) {
      Log.d(
        TAG,
        "refreshThumb() called on $this"
      )
    }

    onRefreshThumb(thumbColoringDrawables)
  }

  protected abstract fun onRefreshThumb(thumbColoringDrawables: Set<Drawable>)

  fun addListener(listener: OnColorPickListener<ColorSeekBar<C>, C>) {
    colorPickListeners.add(listener)
  }

  fun removeListener(listener: OnColorPickListener<ColorSeekBar<C>, C>) {
    colorPickListeners.remove(listener)
  }

  fun clearListeners() {
    colorPickListeners.clear()
  }

  // TODO: Add (mask) delegating OnSeekBarChangeListener
  final override fun setOnSeekBarChangeListener(l: OnSeekBarChangeListener?) {
    if (l != this) {
      throw IllegalStateException("Custom OnSeekBarChangeListener not supported yet")
    }
    super.setOnSeekBarChangeListener(l)
  }

  private fun notifyListenersOnColorChanged() {
    if (!notifyListeners) {
      if (DEBUG) {
        Log.d(
          TAG,
          "Listeners silenced, but notifyListenersOnColorChanged called"
        )
      }
      return
    }

    colorPickListeners.forEach {
      it.onColorChanged(
        this,
        pickedColor,
        progress
      )
    }
  }

  private fun notifyListenersOnColorPicking(fromUser: Boolean) {
    if (!notifyListeners) {
      if (DEBUG) {
        Log.d(
          TAG,
          "Listeners silenced, but notifyListenersOnColorPicking called"
        )
      }
      return
    }

    colorPickListeners.forEach {
      it.onColorPicking(
        this,
        pickedColor,
        progress,
        fromUser
      )
    }
  }

  private fun notifyListenersOnColorPicked(fromUser: Boolean) {
    if (!notifyListeners) {
      if (DEBUG) {
        Log.d(
          TAG,
          "Listeners silenced, but notifyListenersOnColorPicked called"
        )
      }
      return
    }

    colorPickListeners.forEach {
      it.onColorPicked(
        this,
        pickedColor,
        progress,
        fromUser
      )
    }
  }

  // TODO: Revisit
  override fun onProgressChanged(
    seekBar: SeekBar,
    progress: Int,
    fromUser: Boolean
  ) {
    if (minUpdating || maxUpdating) {
      return
    }

    refreshInternalPickedColorFromProgress()
    refreshProgressDrawable()
    refreshThumb()
    notifyListenersOnColorPicking(fromUser)

    if (!fromUser) {
      notifyListenersOnColorPicked(fromUser)
    }
  }

  override fun onStartTrackingTouch(seekBar: SeekBar) {
    thumbObjectAnimator.setIntValues(
      thumb.level,
      THUMB_DRAWABLE_LEVEL_PRESSED
    )
    thumbObjectAnimator.start()
  }

  override fun onStopTrackingTouch(seekBar: SeekBar) {
    thumbObjectAnimator.setIntValues(
      thumb.level,
      THUMB_DRAWABLE_LEVEL_DEFAULT
    )
    thumbObjectAnimator.start()
    notifyListenersOnColorPicked(true)
  }

  interface Mode {
    val minProgress: Int
    val maxProgress: Int


    val absoluteProgress: Int
      get() {
        return maxProgress - minProgress
      }
  }
  // TODO: Rename
  interface OnColorPickListener<S : ColorSeekBar<C>, C : Color> {
    fun onColorPicking(
      picker: S,
      color: C,
      value: Int,
      fromUser: Boolean
    )

    fun onColorPicked(
      picker: S,
      color: C,
      value: Int,
      fromUser: Boolean
    )

    fun onColorChanged(
      picker: S,
      color: C,
      value: Int
    )
  }

  open class DefaultOnColorPickListener<S : ColorSeekBar<C>, C : Color> :
    OnColorPickListener<S, C> {
    override fun onColorPicking(
      picker: S,
      color: C,
      value: Int,
      fromUser: Boolean
    ) {

    }

    override fun onColorPicked(
      picker: S,
      color: C,
      value: Int,
      fromUser: Boolean
    ) {

    }

    override fun onColorChanged(
      picker: S,
      color: C,
      value: Int
    ) {

    }
  }

  companion object {
    private const val TAG = "ColorSeekBar"
    private const val DEBUG = false

    private const val THUMB_DRAWABLE_LEVEL_DEFAULT = 8000
    private const val THUMB_DRAWABLE_LEVEL_PRESSED = 10000
    private const val THUMB_ANIM_DURATION = 150L
  }
}