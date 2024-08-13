package com.ashique.qrscanner.helper

import android.animation.LayoutTransition
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.Interpolator
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import java.io.IOException

object Extensions {


    fun AppCompatActivity.setOnBackPressedAction(action: () -> Unit) {
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                action()
            }
        }
        onBackPressedDispatcher.addCallback(this, callback)
    }

    fun Fragment.setOnBackPressedAction(action: () -> Unit) {
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                action()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)
    }

    fun Context.applyDayNightTheme() {
        when (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
            Configuration.UI_MODE_NIGHT_YES -> {
                // Night mode is active
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            }

            Configuration.UI_MODE_NIGHT_NO -> {
                // Day mode is active
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
        }
    }


    inline fun <reified T : Any> Context.navigateTo(extras: Bundle? = null) {
        val intent = Intent(this, T::class.java).apply {
            extras?.let { putExtras(it) }
        }
        startActivity(intent)
    }

    fun Context.showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
        Toast.makeText(this, message, duration).show()
    }

    fun ViewGroup.animateLayout(
        appearDuration: Long = 600,
        disappearDuration: Long = 100,
        changeDuration: Long = 300,
        enableAppearing: Boolean = true,
        enableDisappearing: Boolean = true,
        enableChanging: Boolean = true,
        appearInterpolator: Interpolator = AccelerateDecelerateInterpolator(),
        disappearInterpolator: Interpolator = AccelerateDecelerateInterpolator(),
        changeInterpolator: Interpolator = AccelerateDecelerateInterpolator()
    ) {

        val layoutTransition = LayoutTransition().apply {
            if (enableAppearing) {
                setDuration(LayoutTransition.APPEARING, appearDuration)
                setInterpolator(LayoutTransition.APPEARING, appearInterpolator)
            } else {
                disableTransitionType(LayoutTransition.APPEARING)
            }

            if (enableDisappearing) {
                setDuration(LayoutTransition.DISAPPEARING, disappearDuration)
                setInterpolator(LayoutTransition.DISAPPEARING, disappearInterpolator)
            } else {
                disableTransitionType(LayoutTransition.DISAPPEARING)
            }

            if (enableChanging) {
                setDuration(LayoutTransition.CHANGING, changeDuration)
                setInterpolator(LayoutTransition.CHANGING, changeInterpolator)
            } else {
                disableTransitionType(LayoutTransition.CHANGING)
            }
        }


        this.layoutTransition = layoutTransition
    }


    fun Uri.toDrawable(context: Context): Drawable? {
        return context.contentResolver.openInputStream(this)?.use { inputStream ->
            Drawable.createFromStream(inputStream, toString())
        }
    }

    fun Uri.toBitmapDrawable(context: Context): BitmapDrawable? {
        return try {
            // Convert URI to InputStream
            val inputStream = context.contentResolver.openInputStream(this) ?: return null

            // Decode InputStream to Bitmap
            val bitmap = BitmapFactory.decodeStream(inputStream)

            // Return BitmapDrawable
            BitmapDrawable(context.resources, bitmap)
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }


    fun Uri.toBitmap(context: Context): Bitmap? {
        return try {
            // Convert URI to InputStream
            val inputStream = context.contentResolver.openInputStream(this) ?: return null

            // Decode InputStream to Bitmap
            BitmapFactory.decodeStream(inputStream)
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    fun Drawable.drawableFilter(color: Int) : Drawable{
       this.colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_ATOP)
        return this
    }

     fun applyColorFilter(drawable: Drawable?, color: Int): Drawable? {
        if (drawable == null) return null

        // Create a copy of the drawable to avoid modifying the original
        val newDrawable = drawable.constantState?.newDrawable()?.mutate()

        // Apply the color filter
        newDrawable?.setColorFilter(color, PorterDuff.Mode.SRC_IN)

        return newDrawable
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun Int.intToColor(): Color {
        return Color.valueOf(this)
    }

    fun View.dp(number: Number): Float {
        val metric =
            getDisplayMetric(context)

        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, number.toFloat(), metric)
    }


    fun getDisplayMetric(context: Context?): DisplayMetrics {
        return if (context != null) context.resources.displayMetrics else Resources.getSystem().displayMetrics
    }

    fun createSeekBarListener(
        onProgressChanged: (Int) -> Unit,
        onStop: ((Boolean) -> Unit)? = null
    ): SeekBar.OnSeekBarChangeListener {
        return object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                onProgressChanged(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                onStop?.invoke(true)  // Call onStop if it is not null
            }
        }
    }

}