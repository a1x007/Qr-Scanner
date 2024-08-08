package com.ashique.qrscanner.helper

import android.animation.Animator
import android.animation.AnimatorSet
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
import android.view.animation.AccelerateDecelerateInterpolator
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

    fun View.showHide(visible: Boolean) {
        val slideAnimator = ObjectAnimator.ofFloat(this, "translationY",
            if (visible) 100f else 0f, if (visible) 0f else 100f
        ).setDuration(300)

        val fadeAnimator = ObjectAnimator.ofFloat(this, "alpha",
            if (visible) 0f else 1f, if (visible) 1f else 0f
        ).setDuration(300)

        val animatorSet = AnimatorSet().apply {
            playTogether(slideAnimator, fadeAnimator)
            if (!visible) {
                addListener(object : android.animation.AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        this@showHide.visibility = View.GONE
                    }
                })
            } else {
                this@showHide.visibility = View.VISIBLE
            }
        }

        animatorSet.start()
    }


    fun View.animateViewFromBottom(show: Boolean) {
        val translationY = if (show) 0f else 50f
        val animator = ObjectAnimator.ofFloat(this, "translationY", translationY)
        animator.duration = 300 // Adjust duration as needed
        animator.interpolator = AccelerateDecelerateInterpolator()

        animator.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {
                if (show) {
                    this@animateViewFromBottom.visibility = View.VISIBLE
                }
            }

            override fun onAnimationEnd(animation: Animator) {
                if (!show) {
                   this@animateViewFromBottom.visibility = View.GONE
                }
            }

            override fun onAnimationCancel(animation: Animator) {
                // Handle cancellation if needed
            }

            override fun onAnimationRepeat(animation: Animator) {
                // Not used
            }
        })

        animator.start()
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

    /**
     * This method returns DisplayMetric of current device.
     * If Context is null the default system display metric would be returned which has default
     * density etc...
     */
    fun getDisplayMetric(context: Context?): DisplayMetrics {
        return if (context != null) context.resources.displayMetrics else Resources.getSystem().displayMetrics
    }
}