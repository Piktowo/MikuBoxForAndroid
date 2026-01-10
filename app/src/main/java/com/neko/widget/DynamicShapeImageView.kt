package com.neko.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.AttributeSet
import androidx.preference.PreferenceDataStore
import com.neko.shapeimageview.ShaderImageView
import com.neko.shapeimageview.shader.ShaderHelper
import com.neko.shapeimageview.shader.SvgShader
import io.nekohasekai.sagernet.R
import io.nekohasekai.sagernet.database.DataStore
import io.nekohasekai.sagernet.database.preference.OnPreferenceDataStoreChangeListener
import io.nekohasekai.sagernet.ktx.getColorAttr

class DynamicShapeImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ShaderImageView(context, attrs, defStyleAttr), OnPreferenceDataStoreChangeListener {

    private val KEY_SHAPE = "preference_icon_shape"
    private var currentShapeId: Int = R.raw.uwu_shape_cookie

    override fun createImageViewHelper(): ShaderHelper {
        val safeId = if (currentShapeId == 0) R.raw.uwu_shape_cookie else currentShapeId
        return SvgShader(safeId)
    }

    init {
        scaleType = ScaleType.CENTER_CROP
        loadColorBitmap()
    }

    private fun loadColorBitmap() {
        try {
            val color = context.getColorAttr(R.attr.colorPrimary)
            val bitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(color)
            setImageBitmap(bitmap)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!isInEditMode) {
            DataStore.configurationStore.registerChangeListener(this)
            post { updateShapeFromStore() }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        if (!isInEditMode) {
            DataStore.configurationStore.unregisterChangeListener(this)
        }
    }

    override fun onPreferenceDataStoreChanged(store: PreferenceDataStore, key: String) {
        if (key == KEY_SHAPE) {
            post { updateShapeFromStore() }
        }
    }

    private fun updateShapeFromStore() {
        try {
            val shapeKey = DataStore.preferenceIconShape
            
            val newShapeId = when (shapeKey) {
                "uwu_shape_clover" -> R.raw.uwu_shape_clover
                "uwu_shape_circle" -> R.raw.uwu_shape_circle
                "uwu_shape_diamond" -> R.raw.uwu_shape_diamond
                "uwu_shape_pentagon" -> R.raw.uwu_shape_pentagon
                "uwu_shape_hexagon" -> R.raw.uwu_shape_hexagon
                "uwu_shape_octagon" -> R.raw.uwu_shape_octagon
                "uwu_shape_rounded_square" -> R.raw.uwu_shape_rounded_square
                "uwu_shape_squircle" -> R.raw.uwu_shape_squircle
                "uwu_shape_heart" -> R.raw.uwu_shape_heart
                else -> R.raw.uwu_shape_cookie
            }

            if (currentShapeId != newShapeId || currentShapeId == 0) {
                currentShapeId = newShapeId
                reloadShape()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
