package com.ashique.qrscanner.helper

class AtkinsonDithering(private val width: Int, private val height: Int) {

    // Error diffusion matrix for Atkinson dithering
    private val diffusionMatrix = arrayOf(
        intArrayOf(1, 0, 1),
        intArrayOf(2, 0, 1),
        intArrayOf(-1, 1, 1),
        intArrayOf(0, 1, 1),
        intArrayOf(1, 1, 1),
        intArrayOf(0, 2, 1)
    )

    private val errorScale = 8

    fun applyDithering(pixels: IntArray): IntArray {
        val outputPixels = pixels.copyOf()

        for (y in 0 until height) {
            for (x in 0 until width) {
                val index = y * width + x
                val oldPixel = outputPixels[index]
                val newPixel = if (oldPixel < 128) 0 else 255
                outputPixels[index] = newPixel
                val quantError = oldPixel - newPixel

                for (diff in diffusionMatrix) {
                    val dx = diff[0]
                    val dy = diff[1]
                    val factor = diff[2]

                    val newX = x + dx
                    val newY = y + dy

                    if (newX in 0 until width && newY in 0 until height) {
                        val newIndex = newY * width + newX
                        outputPixels[newIndex] = clamp(
                            0,
                            255,
                            outputPixels[newIndex] + (quantError * factor / errorScale)
                        )
                    }
                }
            }
        }
        return outputPixels
    }

    private fun clamp(min: Int, max: Int, value: Int): Int {
        return when {
            value < min -> min
            value > max -> max
            else -> value
        }
    }
}
