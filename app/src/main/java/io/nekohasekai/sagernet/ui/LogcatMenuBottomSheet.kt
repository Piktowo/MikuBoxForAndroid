package io.nekohasekai.sagernet.ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.bottomsheet.BottomSheetBehavior
import io.nekohasekai.sagernet.R

class LogcatMenuBottomSheet : BottomSheetDialogFragment() {

    interface OnOptionClickListener {
        fun onOptionClicked(viewId: Int)
    }

    private var mListener: OnOptionClickListener? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (parentFragment is OnOptionClickListener) {
            mListener = parentFragment as OnOptionClickListener
        } else {
            throw RuntimeException("$parentFragment must implement OnOptionClickListener")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.uwu_bottom_sheet_logcat_menu, container, false)
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
        
        val clickListener = View.OnClickListener {
            mListener?.onOptionClicked(it.id)
            dismiss()
        }

        val actionIds = listOf(
            R.id.action_clear_logcat,
            R.id.action_send_logcat,
            R.id.action_refresh
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
        const val TAG = "LogcatMenuBottomSheet"
    }
}
