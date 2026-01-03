package moe.matsuri.nb4a.ui

import android.content.Context
import android.content.res.Resources
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.core.content.res.ResourcesCompat
import androidx.core.content.res.TypedArrayUtils
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.setPadding
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.shape.CornerFamily
import com.google.android.material.shape.RelativeCornerSize
import com.google.android.material.shape.ShapeAppearanceModel
import io.nekohasekai.sagernet.R
import io.nekohasekai.sagernet.ktx.getColorAttr
import io.nekohasekai.sagernet.utils.showBlur
import kotlin.math.roundToInt

class ColorPickerPreference
@JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyle: Int = TypedArrayUtils.getAttr(
        context,
        androidx.preference.R.attr.editTextPreferenceStyle,
        android.R.attr.editTextPreferenceStyle
    )
) : Preference(
    context, attrs, defStyle
) {

    var inited = false

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)

        val widgetFrame = holder.findViewById(android.R.id.widget_frame) as LinearLayout

        if (!inited) {
            inited = true
            
            widgetFrame.addView(
                getNekoImageViewAtColor(
                    context.getColorAttr(R.attr.colorPrimary),
                    28,
                    0,
                    isSelected = false 
                )
            )
            widgetFrame.visibility = View.VISIBLE
        }
    }

    fun getNekoImageViewAtColor(color: Int, sizeDp: Int, paddingDp: Int, isSelected: Boolean): ImageView {
        val factor = context.resources.displayMetrics.density
        val size = (sizeDp * factor).roundToInt()
        val paddingSize = (paddingDp * factor).roundToInt()

        return ShapeableImageView(context).apply {
            layoutParams = ViewGroup.LayoutParams(size, size)
            setPadding(paddingSize)

            shapeAppearanceModel = ShapeAppearanceModel.builder()
                .setAllCorners(CornerFamily.ROUNDED, 0f) 
                .setAllCornerSizes(
                    if (isSelected) RelativeCornerSize(0.25f)
                    else RelativeCornerSize(0.50f)
                )
                .build()

            val backgroundDrawable = ColorDrawable(color)

            val finalDrawable = if (isSelected) {
                val checkIcon = ResourcesCompat.getDrawable(context.resources, R.drawable.ic_action_done, null)?.mutate()

                if (checkIcon != null) {
                    val iconColor = context.getColorAttr(android.R.attr.textColorPrimaryInverse)
                    DrawableCompat.setTint(checkIcon, iconColor)
                    
                    val layers = arrayOf(backgroundDrawable, checkIcon)
                    val layerDrawable = LayerDrawable(layers)
                    
                    val iconSizeDp = 38
                    val iconSizePx = (iconSizeDp * factor).roundToInt()
                    
                    val inset = ((size - iconSizePx) / 2).coerceAtLeast(0)

                    layerDrawable.setLayerInset(1, inset, inset, inset, inset)
                    
                    layerDrawable
                } else {
                    backgroundDrawable
                }
            } else {
                backgroundDrawable
            }

            setImageDrawable(finalDrawable)
        }
    }

    fun getNekoAtColor(res: Resources, color: Int): Drawable {
        return ColorDrawable(color)
    }

    override fun onClick() {
        super.onClick()

        lateinit var dialog: AlertDialog
        
        val currentSelectedThemeId = getPersistedInt(0)

        val grid = GridLayout(context).apply {
            columnCount = 4
            setPadding(24, 24, 24, 24)

            val colors = context.resources.getIntArray(R.array.material_colors)
            var i = 0

            for (color in colors) {
                i++ 

                val themeId = i
                val isCurrentlySelected = (themeId == currentSelectedThemeId)

                val view = getNekoImageViewAtColor(color, 64, 8, isCurrentlySelected).apply {
                    setOnClickListener {
                        persistInt(themeId)
                        dialog.dismiss()
                        callChangeListener(themeId)
                    }
                }
                addView(view)
            }
        }

        dialog = MaterialAlertDialogBuilder(context).setTitle(title)
            .setView(LinearLayout(context).apply {
                gravity = Gravity.CENTER
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
                )
                addView(grid)
            })
            .setNegativeButton(android.R.string.cancel, null)
            .showBlur()
    }
}
