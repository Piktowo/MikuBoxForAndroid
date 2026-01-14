package moe.matsuri.nb4a.ui

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.appcompat.widget.AppCompatImageView
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceViewHolder
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import io.nekohasekai.sagernet.R
import io.nekohasekai.sagernet.database.DataStore

class CustomBannerEditTextPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = androidx.preference.R.attr.editTextPreferenceStyle,
    defStyleRes: Int = 0
) : EditTextPreference(context, attrs, defStyleAttr, defStyleRes) {

    private val TAG_PREFERENCE_DEFAULT = "DEFAULT_BANNER_PREFERENCE"

    init {
        layoutResource = R.layout.uwu_banner_profile
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)

        holder.itemView.isClickable = false
        holder.itemView.isFocusable = false

        val bannerLayout = holder.findViewById(R.id.img_banner_layout_preference)
        val bannerImageView = holder.findViewById(R.id.img_banner_preference) as? AppCompatImageView

        if (DataStore.showBannerPreference) {
            
            bannerLayout?.visibility = View.VISIBLE

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
                }
            }

        } else {
            bannerLayout?.visibility = View.GONE
        }

        val editButton = holder.findViewById(R.id.editTextCustom)
        editButton?.let { view ->
            view.isClickable = true
            view.isFocusable = true
            
            view.setOnClickListener {
                this@CustomBannerEditTextPreference.performClick()
            }
        }
    }
}
