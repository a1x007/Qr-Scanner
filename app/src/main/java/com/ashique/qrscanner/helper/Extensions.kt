package com.ashique.qrscanner.helper

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import codes.side.andcolorpicker.converter.IntegerHSLColorConverter
import codes.side.andcolorpicker.group.PickerGroup
import codes.side.andcolorpicker.group.registerPickers
import codes.side.andcolorpicker.hsl.HSLColorPickerSeekBar
import codes.side.andcolorpicker.model.IntegerHSLColor
import codes.side.andcolorpicker.view.picker.ColorSeekBar
import codes.side.andcolorpicker.view.picker.OnIntegerHSLColorPickListener
import com.google.android.material.animation.AnimatorSetCompat.playTogether

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

     fun Activity.showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
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
                    override fun onAnimationEnd(animation: android.animation.Animator) {
                        this@showHide.visibility = View.GONE
                    }
                })
            } else {
                this@showHide.visibility = View.VISIBLE
            }
        }

        animatorSet.start()
    }

}