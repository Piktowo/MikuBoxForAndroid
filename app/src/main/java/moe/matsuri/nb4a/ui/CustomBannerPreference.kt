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

    private val TAG_PREFERENCE_DEFAULT = "DEFAULT_BANNER_PREFERENCE"

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

            val targetTag = if (savedUriString.isNullOrBlank()) TAG_PREFERENCE_DEFAULT else savedUriString
            val currentTag = bannerImageView.tag

            if (currentTag != targetTag) {
                
                if (!savedUriString.isNullOrBlank()) {
                    Glide.with(context)
                        .load(savedUriString)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .dontAnimate()
                        .error(R.drawable.uwu_banner_image)
                        .skipMemoryCache(false)
                        .into(bannerImageView)
                } else {
                    Glide.with(context).clear(bannerImageView)
                    bannerImageView.setImageResource(R.drawable.uwu_banner_image)
                }

                bannerImageView.tag = targetTag
                
                bannerImageView.resume()
            }
        }

        (holder.findViewById(R.id.uwu_version_name_summary) as? TextView)?.text = BuildConfig.VERSION_NAME
        (holder.findViewById(R.id.uwu_version_code_summary) as? TextView)?.text = BuildConfig.VERSION_CODE.toString()
        (holder.findViewById(R.id.uwu_build_date_summary) as? TextView)?.text = BuildConfig.BUILD_DATE
        (holder.findViewById(R.id.uwu_package_name_summary) as? TextView)?.text = BuildConfig.APPLICATION_ID

        val clickTarget = holder.findViewById(R.id.onClick)
        clickTarget?.setOnClickListener {
            this.performClick()
        }
    }
}
