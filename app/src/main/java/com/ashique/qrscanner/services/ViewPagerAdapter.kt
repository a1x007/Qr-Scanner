package com.ashique.qrscanner.services

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ashique.qrscanner.R
import com.ashique.qrscanner.activity.QrGenerator
import com.ashique.qrscanner.databinding.LayoutQrColorBinding
import com.ashique.qrscanner.databinding.LayoutQrLogoBinding
import com.ashique.qrscanner.databinding.LayoutQrShapeBinding
import com.ashique.qrscanner.helper.QrUiSetup

class ViewPagerAdapter(private val context: Context) : RecyclerView.Adapter<ViewPagerAdapter.ViewHolder>() {

    private val layouts = listOf(
        R.layout.layout_qr_shape,
        R.layout.layout_qr_logo,
        R.layout.layout_qr_color,

    )

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(viewType, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val binding = when (position) {
            0 -> LayoutQrShapeBinding.bind(holder.itemView)
            1 -> LayoutQrLogoBinding.bind(holder.itemView)
            2 -> LayoutQrColorBinding.bind(holder.itemView)
            else -> throw IllegalArgumentException("Invalid layout position")
        }

        when (position) {
            0 -> QrUiSetup.pixelShapeSetting(binding as LayoutQrShapeBinding) {
                (context as? QrGenerator)?.updateQrCode()
            }
            1 -> QrUiSetup.logoSetting(binding as LayoutQrLogoBinding) {
                (context as? QrGenerator)?.updateQrCode()
            }
            2 -> QrUiSetup.qrColorSetting(binding as LayoutQrColorBinding) {
                (context as? QrGenerator)?.updateQrCode()
            }
        }
    }

    override fun getItemCount(): Int = layouts.size

    override fun getItemViewType(position: Int): Int = layouts[position]

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}
