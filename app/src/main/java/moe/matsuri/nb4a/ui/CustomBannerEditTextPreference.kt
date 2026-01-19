package moe.matsuri.nb4a.ui

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.appcompat.widget.AppCompatImageView
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceViewHolder
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.target.Target
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
        
        holder.setIsRecyclable(false)

        holder.itemView.isClickable = false
        holder.itemView.isFocusable = false

        val bannerLayout = holder.findViewById(R.id.img_banner_layout_preference)
        val bannerImageView = holder.findViewById(R.id.img_banner_preference) as? AppCompatImageView

        if (DataStore.showBannerPreference) {
            
            bannerLayout?.visibility = View.VISIBLE

            if (bannerImageView != null) {
                bannerImageView.setLayerType(View.LAYER_TYPE_HARDWARE, null)

                val bannerUriString = DataStore.configurationStore.getString("custom_preference_banner_uri", null)

                val targetTag = if (bannerUriString.isNullOrBlank()) TAG_PREFERENCE_DEFAULT else bannerUriString
                val currentTag = bannerImageView.tag

                if (currentTag != targetTag) {
                    
                    if (!bannerUriString.isNullOrBlank()) {
                        Glide.with(context)
                            .load(bannerUriString)
                            .override(Target.SIZE_ORIGINAL)
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
