package com.neko.config

import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.DownsampleStrategy
import com.bumptech.glide.load.resource.gif.GifOptions
import com.bumptech.glide.request.target.Target
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.snackbar.Snackbar
import io.nekohasekai.sagernet.R
import io.nekohasekai.sagernet.database.DataStore
import io.nekohasekai.sagernet.database.ProfileManager
import io.nekohasekai.sagernet.group.RawUpdater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL

class V2rayConfigBottomSheet : BottomSheetDialogFragment() {

    private lateinit var btnGenerate: Button
    private lateinit var editCount: EditText
    private lateinit var editServer: EditText
    private lateinit var autoCompleteCountry: AutoCompleteTextView    
    private lateinit var autoCompleteProtocol: AutoCompleteTextView
    private lateinit var autoCompletePort: AutoCompleteTextView

    private var selectedProtocol: String = ""
    private var listProtocols: List<String> = emptyList()

    companion object {
        const val TAG = "V2rayConfigBottomSheet"
        const val TAG_SHEET_DEFAULT = "DEFAULT_BANNER_SHEET"

        private val SOURCE_URLS = listOf(
            "https://miku.uwuowoumu.workers.dev/api/v1/sub",
            "https://miku.hatsunemikuuwu.workers.dev/api/v1/sub"
        )

        fun newInstance(): V2rayConfigBottomSheet {
            return V2rayConfigBottomSheet()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.uwu_free_config, container, false)
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
            bannerImageView.setLayerType(View.LAYER_TYPE_HARDWARE, null)
            val bannerUriString = DataStore.configurationStore.getString("custom_sheet_banner_uri", null)
            val targetTag = if (bannerUriString.isNullOrBlank()) TAG_SHEET_DEFAULT else bannerUriString
            val currentTag = bannerImageView.tag
            if (currentTag != targetTag) {
                if (!bannerUriString.isNullOrBlank()) {
                    val bannerSavedUriString = Uri.parse(bannerUriString)
                    Glide.with(this)
                        .load(bannerSavedUriString)
                        .downsample(DownsampleStrategy.NONE)
                        .set(GifOptions.DECODE_FORMAT, DecodeFormat.PREFER_ARGB_8888)
                        .format(DecodeFormat.PREFER_ARGB_8888)
                        .override(Target.SIZE_ORIGINAL)
                        .diskCacheStrategy(DiskCacheStrategy.DATA)
                        .skipMemoryCache(false)
                        .error(R.drawable.uwu_banner_image_about)
                        .into(bannerImageView)
                } else {
                    Glide.with(this).clear(bannerImageView)
                    bannerImageView.setImageResource(R.drawable.uwu_banner_image_about)
                }
                bannerImageView.tag = targetTag
            }
        }
        
        val particlesView = view.findViewById<View>(R.id.ParticlesView)
        if (particlesView != null) {
            if (DataStore.disableParticlesSheet) {
                particlesView.visibility = View.GONE
            } else {
                particlesView.visibility = View.VISIBLE
            }
        }
        
        initializeViews(view)
        setupProtocolDropdown()
        setupPortDropdown()
        setupCountryDropdown()
        setupButtonListeners()
    }

    private fun initializeViews(view: View) {
        btnGenerate = view.findViewById(R.id.btnGenerate) 
        editCount = view.findViewById(R.id.editCount)
        editServer = view.findViewById(R.id.editServer)
        autoCompleteCountry = view.findViewById(R.id.autoCompleteCountry)
        autoCompleteProtocol = view.findViewById(R.id.autoCompleteProtocol)
        autoCompletePort = view.findViewById(R.id.autoCompletePort)
    }

    private fun setupProtocolDropdown() {
        listProtocols = listOf("VLESS", "Trojan", "Shadowsocks")
        selectedProtocol = "VLESS" 
        
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, listProtocols)
        autoCompleteProtocol.setAdapter(adapter)
        autoCompleteProtocol.setText(selectedProtocol, false) 
        autoCompleteProtocol.setOnItemClickListener { _, _, position, _ ->
            selectedProtocol = listProtocols[position]
        }
    }

    private fun setupPortDropdown() {
        val commonPorts = listOf("443", "80")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, commonPorts)
        autoCompletePort.setAdapter(adapter)
        autoCompletePort.setText("443", false)
    }

    private fun setupCountryDropdown() {
        val countries = listOf(
            "AE", "AM", "AU", "BE", "BG", "BR", "CA", "CH", "CY", "CZ", "DE", "DK", 
            "EE", "ES", "FI", "FR", "GB", "HK", "HU", "ID", "IE", "IL", "IN", "IT", 
            "JP", "KR", "KZ", "LV", "MD", "MU", "MX", "MY", "NL", "PL", "PT", "RO", 
            "RS", "RU", "SE", "SG", "TH", "TR", "UA", "US", "VN"
        )
        
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, countries)
        autoCompleteCountry.setAdapter(adapter)
        autoCompleteCountry.setText("SG", false)
    }

    private fun setupButtonListeners() {
        btnGenerate.setOnClickListener {
            var rawProtocol = if (selectedProtocol.isNotEmpty()) selectedProtocol else autoCompleteProtocol.text.toString()
            if (rawProtocol.isBlank()) rawProtocol = "VLESS"
            
            val protocolParam = when (rawProtocol) {
                "Shadowsocks" -> "ss"
                else -> rawProtocol.lowercase()
            }

            val countStr = editCount.text.toString()
            val count = countStr.toIntOrNull()?.coerceAtLeast(1) ?: 1
            val serverHost = editServer.text.toString().trim().ifEmpty { "www.google.com" }
            val countryCode = autoCompleteCountry.text.toString().trim().ifEmpty { "SG" }
            val portStr = autoCompletePort.text.toString().trim().ifEmpty { "443" }
            val parentActivity = requireActivity()
            val rootView = parentActivity.findViewById<View>(android.R.id.content)    
            Snackbar.make(rootView, R.string.msg_generating, Snackbar.LENGTH_SHORT).show()    
            dismiss() 
            
            fetchV2rayConfig(
                parentActivity, 
                rootView, 
                protocolParam,
                count, 
                serverHost, 
                countryCode,
                portStr,
                rawProtocol
            )
        }
    }

    private fun getPrefixForProtocol(protocolName: String): String {
        return when (protocolName.lowercase()) {
            "vless" -> "vless://"
            "trojan" -> "trojan://"
            "shadowsocks" -> "ss://" 
            else -> ""
        }
    }

    private fun fetchV2rayConfig(
        activity: FragmentActivity,
        rootView: View,
        protocolParam: String, 
        count: Int, 
        serverHost: String,
        countryCode: String,
        port: String,
        originalProtocolName: String
    ) {
        activity.lifecycleScope.launch(Dispatchers.IO) {
            var finalResult: String? = null
            var lastErrorMsg = ""

            for (baseUrl in SOURCE_URLS) {
                try {
                    val fullUrl = "$baseUrl?&vpn=$protocolParam&cc=$countryCode&domain=$serverHost&port=$port&limit=$count"
                    val rawContent = URL(fullUrl).openStream().bufferedReader().use { it.readText() }
                    val decodedContent = tryDecodeSubscription(rawContent)
                    val result = selectConfigs(decodedContent, originalProtocolName, count)              
                    if (result != null) {
                        finalResult = result
                        break
                    }
                } catch (e: Exception) {
                    lastErrorMsg = e.message ?: "Unknown error"
                }
            }

            if (finalResult != null) {
                autoImportConfig(activity, rootView, finalResult)
            } else {
                withContext(Dispatchers.Main) {
                    val errorMsg = activity.getString(R.string.msg_fetch_failed, lastErrorMsg)
                    Snackbar.make(rootView, errorMsg, Snackbar.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun autoImportConfig(activity: FragmentActivity, rootView: View, configText: String) {
        activity.lifecycleScope.launch(Dispatchers.Default) {
            try {
                val proxies = RawUpdater.parseRaw(configText)         
                if (!proxies.isNullOrEmpty()) {
                    val targetId = DataStore.selectedGroup      
                    for (proxy in proxies) {
                        ProfileManager.createProfile(targetId, proxy)
                    }
                    withContext(Dispatchers.Main) {
                        Snackbar.make(rootView, R.string.msg_import_success, Snackbar.LENGTH_SHORT).show()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Snackbar.make(rootView, R.string.msg_no_proxies, Snackbar.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    val errorMsg = activity.getString(R.string.msg_import_failed, e.message)
                    Snackbar.make(rootView, errorMsg, Snackbar.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun tryDecodeSubscription(content: String): String {
        val trimmed = content.trim()
        if (!trimmed.contains(" ") && !trimmed.contains("\n") && trimmed.length > 20) {
            return try {
                String(Base64.decode(trimmed, Base64.DEFAULT))
            } catch (e: Exception) { content }
        }
        return content
    }

    private fun selectConfigs(content: String, protocol: String, count: Int): String? {
        val targetPrefix = getPrefixForProtocol(protocol)
        
        val validLines = content.lines().asSequence().map { it.trim() }.filter { it.isNotBlank() }
            .filter { line -> line.startsWith(targetPrefix) }
            .toList()

        if (validLines.isEmpty()) return null
        return validLines.shuffled().take(count).joinToString("\n")
    }
}
