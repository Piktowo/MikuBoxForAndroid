package moe.matsuri.nb4a.ui

import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import android.widget.Toast
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceViewHolder
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.nekohasekai.sagernet.R
import io.nekohasekai.sagernet.database.DataStore
import io.nekohasekai.sagernet.utils.DPIController
import io.nekohasekai.sagernet.utils.showBlur

class DpiEditTextPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : EditTextPreference(context, attrs) {

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)

        holder.itemView.setOnLongClickListener {
            val sysConfig = context.applicationContext.resources.configuration
            val systemDpi = sysConfig.densityDpi.takeIf { it > 0 } ?: 440

            MaterialAlertDialogBuilder(context)
                .setTitle(R.string.reset_dpi_title)
                .setMessage(context.getString(R.string.reset_dpi_message, systemDpi))
                .setPositiveButton(R.string.reset) { _, _ ->
                    DataStore.dpiValue = systemDpi
                    text = systemDpi.toString()

                    DPIController.applyDpi(context, systemDpi)

                    Toast.makeText(
                        context,
                        context.getString(R.string.dpi_reset_to_default, systemDpi),
                        Toast.LENGTH_SHORT
                    ).show()

                    (context as? Activity)?.recreate()
                }
                .setNegativeButton(android.R.string.cancel, null)
                .showBlur()

            true
        }
    }
}
