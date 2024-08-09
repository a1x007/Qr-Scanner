package com.ashique.qrscanner.helper

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.LayoutTransition
import android.animation.ObjectAnimator
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.Interpolator
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment

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
}