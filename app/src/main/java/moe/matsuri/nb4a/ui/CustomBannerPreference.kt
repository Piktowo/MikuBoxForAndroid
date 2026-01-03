package moe.matsuri.nb4a.ui

import android.content.Context
import android.util.AttributeSet
import android.widget.TextView
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.flaviofaria.kenburnsview.KenBurnsView
import io.nekohasekai.sagernet.BuildConfig
import io.nekohasekai.sagernet.R
import io.nekohasekai.sagernet.database.DataStore

class CustomBannerPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = androidx.preference.R.attr.preferenceStyle,
    defStyleRes: Int = 0
) : Preference(context, attrs, defStyleAttr, defStyleRes) {

    init {
        layoutResource = R.layout.uwu_banner_theme
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)

        holder.itemView.isClickable = false
        holder.itemView.isFocusable = false

        val bannerImageView = holder.findViewById(R.id.img_banner_preference) as? KenBurnsView
        if (bannerImageView != null) {
            val savedUriString = DataStore.configurationStore.getString("custom_preference_banner_uri", null)

            if (savedUriString != bannerImageView.tag || savedUriString.isNullOrBlank()) {
                if (!savedUriString.isNullOrBlank()) {
                    Glide.with(context)
                        .load(savedUriString)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .dontAnimate()
                        .skipMemoryCache(false)
                        .into(bannerImageView)
                    
                    bannerImageView.tag = savedUriString
                } else {
                    bannerImageView.setImageResource(R.drawable.uwu_banner_image)
                    bannerImageView.tag = null
                }
            }
        }

        val versionTextView = holder.findViewById(R.id.uwu_version_name_summary) as? TextView
        versionTextView?.text = "${BuildConfig.VERSION_NAME}"

        val buildTextView = holder.findViewById(R.id.uwu_version_code_summary) as? TextView
        buildTextView?.text = "${BuildConfig.VERSION_CODE}"
        
        val dateTextView = holder.findViewById(R.id.uwu_build_date_summary) as? TextView
        dateTextView?.text = "${BuildConfig.BUILD_DATE}"
        
        val packageTextView = holder.findViewById(R.id.uwu_package_name_summary) as? TextView
        packageTextView?.text = "${BuildConfig.APPLICATION_ID}"

        val clickTarget = holder.findViewById(R.id.onClick)
        clickTarget?.setOnClickListener {
            this.performClick()
        }
    }
}
