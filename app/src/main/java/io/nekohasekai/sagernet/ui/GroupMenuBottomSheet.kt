package io.nekohasekai.sagernet.ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.bottomsheet.BottomSheetBehavior
import io.nekohasekai.sagernet.R
import io.nekohasekai.sagernet.database.DataStore

class GroupMenuBottomSheet : BottomSheetDialogFragment() {

    interface OnOptionClickListener {
        fun onOptionClicked(viewId: Int)
    }

    private var mListener: OnOptionClickListener? = null
    
    private val TAG_SHEET_DEFAULT = "DEFAULT_BANNER_SHEET"

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (parentFragment is OnOptionClickListener) {
            mListener = parentFragment as OnOptionClickListener
        } else {
            if (context is OnOptionClickListener) {
                mListener = context
            } else {
                throw RuntimeException("$parentFragment or $context must implement OnOptionClickListener")
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.uwu_bottom_sheet_group_menu, container, false)
    }
    
    override fun onStart() {
        super.onStart()
        val sheetDialog = dialog as? BottomSheetDialog
        sheetDialog?.behavior?.apply {
            state = BottomSheetBehavior.STATE_EXPANDED
            skipCollapsed = true
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val bannerImageView = view.findViewById<ImageView>(R.id.img_banner_sheet)

        if (bannerImageView != null) {
            val savedUriString = DataStore.configurationStore.getString("custom_sheet_banner_uri", null)

            val targetTag = if (savedUriString.isNullOrBlank()) TAG_SHEET_DEFAULT else savedUriString
            val currentTag = bannerImageView.tag

            if (currentTag != targetTag) {
                
                if (!savedUriString.isNullOrBlank()) {
                    Glide.with(this)
                        .load(savedUriString)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .dontAnimate()
                        .error(R.drawable.uwu_banner_image_about)
                        .into(bannerImageView)
                } else {
                    Glide.with(this).clear(bannerImageView)
                    bannerImageView.setImageResource(R.drawable.uwu_banner_image_about)
                }
                
                bannerImageView.tag = targetTag
            }
        }
        
        val clickListener = View.OnClickListener {
            mListener?.onOptionClicked(it.id)
            dismiss()
        }

        val actionIds = listOf(
            R.id.action_new_group,
            R.id.action_update_all
        )

        actionIds.forEach { id ->
            view.findViewById<View>(id)?.setOnClickListener(clickListener)
        }
    }

    override fun onDetach() {
        super.onDetach()
        mListener = null
    }

    companion object {
        const val TAG = "GroupMenuBottomSheet"
    }
}
