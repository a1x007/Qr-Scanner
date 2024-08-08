package com.ashique.qrscanner.services

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewpager.widget.PagerAdapter
import com.ashique.qrscanner.R
import com.ashique.qrscanner.activity.QrGenerator
import com.ashique.qrscanner.databinding.LayoutQrColorBinding
import com.ashique.qrscanner.databinding.LayoutQrLogoBinding
import com.ashique.qrscanner.databinding.LayoutQrShapeBinding
import com.ashique.qrscanner.helper.QrUiSetup

class ViewPagerAdapter(private val context: Context) : PagerAdapter() {

    private val layouts = listOf(
        R.layout.layout_qr_shape,
        R.layout.layout_qr_logo,
        R.layout.layout_qr_color
    )

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(layouts[position], container, false)
        container.addView(view)

        val binding = when (position) {
            0 -> LayoutQrShapeBinding.bind(view)
            1 -> LayoutQrLogoBinding.bind(view)
            2 -> LayoutQrColorBinding.bind(view)
            else -> throw IllegalArgumentException("Invalid layout position")
        }

        when (position) {
            0 -> QrUiSetup.shapeSetting(binding as LayoutQrShapeBinding) {
                (context as? QrGenerator)?.updateQrCode()
            }
            1 -> QrUiSetup.logoSetting(binding as LayoutQrLogoBinding) {
                (context as? QrGenerator)?.updateQrCode()
            }
            2 -> QrUiSetup.qrColorSetting(binding as LayoutQrColorBinding) {
                (context as? QrGenerator)?.updateQrCode()
            }
        }

        return view
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        container.removeView(`object` as View)
    }

    override fun getCount(): Int = layouts.size

    override fun isViewFromObject(view: View, `object`: Any): Boolean = view == `object`
}