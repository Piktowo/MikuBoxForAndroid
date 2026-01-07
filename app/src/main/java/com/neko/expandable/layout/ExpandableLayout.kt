package com.neko.expandable.layout

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.RippleDrawable
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import com.google.android.material.card.MaterialCardView
import io.nekohasekai.sagernet.R 

class ExpandableLayout(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs), View.OnClickListener {
    private lateinit var arrowIcon: ImageView
    private lateinit var cardExpandable: MaterialCardView
    private lateinit var expandableContent: ExpandableView

    override fun onFinishInflate() {
        super.onFinishInflate()
        expandableContent = findViewById(R.id.expandable_view)
        arrowIcon = findViewById(R.id.arrow_button)
        cardExpandable = findViewById(R.id.card_expandable)
        
        cardExpandable.setOnClickListener(this)
        arrowIcon.setOnClickListener(this)
        initializeLogic()
    }

    override fun onClick(view: View) {
        setOnclick(view)
    }

    private fun setOnclick(view: View) {
        if (expandableContent.isExpanded) {
            expandableContent.collapse()
            arrowIcon.animate().setDuration(300L).rotation(0.0f)
            return
        }
        expandableContent.expand()
        arrowIcon.animate().setDuration(300L).rotation(90.0f)
    }

    private fun initializeLogic() {
        arrowIcon.background = RippleDrawable(ColorStateList(arrayOf(intArrayOf()), intArrayOf(-0x8a8a8b)), null, null)
        arrowIcon.isClickable = true
        if (expandableContent.isExpanded) {
            arrowIcon.rotation = 90.0f
        } else {
            arrowIcon.rotation = 0.0f
        }
    }
}
