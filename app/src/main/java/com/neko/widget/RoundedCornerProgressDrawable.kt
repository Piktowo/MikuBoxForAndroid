package com.neko.widget

import android.content.res.Resources
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.graphics.drawable.DrawableWrapper
import android.graphics.drawable.InsetDrawable

class RoundedCornerProgressDrawable(drawable: Drawable? = null) : InsetDrawable(drawable, 0) {

    override fun onBoundsChange(bounds: Rect) {
        super.onBoundsChange(bounds)
        onLevelChange(level)
    }

    override fun onLayoutDirectionChanged(layoutDirection: Int): Boolean {
        onLevelChange(level)
        return super.onLayoutDirectionChanged(layoutDirection)
    }

    override fun onLevelChange(level: Int): Boolean {
        val drawable = drawable ?: return false
        val innerBounds = drawable.bounds

        val bounds = bounds
        val height = bounds.height()
        val width = bounds.width()

        
        val progressWidth = (width - height) * level / 10000 + height

        drawable.setBounds(
            bounds.left,
            innerBounds.top,
            bounds.left + progressWidth,
            innerBounds.bottom
        )

        return super.onLevelChange(level)
    }

    override fun canApplyTheme(): Boolean {
        return (drawable?.canApplyTheme() == true) || super.canApplyTheme()
    }

    override fun getChangingConfigurations(): Int {
        return super.getChangingConfigurations() or 0x1000
    }

    override fun getConstantState(): ConstantState? {
        val state = super.getConstantState() ?: return null
        return RoundedCornerState(state)
    }

    private class RoundedCornerState(
        val wrappedState: ConstantState
    ) : ConstantState() {

        override fun newDrawable(): Drawable {
            return newDrawable(null, null)
        }

        override fun newDrawable(res: Resources?, theme: Resources.Theme?): Drawable {
            val drawable = wrappedState.newDrawable(res, theme)
            
            if (drawable is DrawableWrapper) {
                return RoundedCornerProgressDrawable(drawable.drawable)
            } else {
                throw NullPointerException("null cannot be cast to non-null type android.graphics.drawable.DrawableWrapper")
            }
        }

        override fun getChangingConfigurations(): Int {
            return wrappedState.changingConfigurations
        }

        override fun canApplyTheme(): Boolean {
            return true
        }
    }
}
