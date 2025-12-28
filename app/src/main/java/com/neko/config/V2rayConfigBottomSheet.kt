package com.neko.config

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.bottomsheet.BottomSheetBehavior
import io.nekohasekai.sagernet.R
import kotlinx.coroutines.*
import org.json.JSONObject
import java.net.URL

class V2rayConfigBottomSheet : BottomSheetDialogFragment() {

    private lateinit var textConfig: TextView
    private lateinit var textLoading: TextView
    private lateinit var btnGenerate: Button
    private lateinit var btnCopy: Button
    
    private lateinit var autoCompleteProtocol: AutoCompleteTextView 

    private var currentConfig: String = ""
    private var currentPort: Int = 443 
    private var selectedProtocol: String = ""
    private var listProtocols: List<String> = emptyList()

    companion object {
        const val TAG = "V2rayConfigBottomSheet"
        
        private const val PRIMARY_URL = "https://www.afrcloud.site/api/subscription/v2ray?type=mix&domain=all&limit=50&tls=true"
        
        private const val FALLBACK_BASE_URL = "https://raw.githubusercontent.com/barry-far/V2ray-Config/refs/heads/main/Sub"

        private val PROTOCOL_PREFIXES = listOf(
            "vmess://", "vless://", "trojan://",
            "ss://", "http://", "socks://",
            "wireguard://", "hysteria2://"
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
        initializeViews(view)
        setupMaterialDropdown()
        setupButtonListeners()
    }

    private fun initializeViews(view: View) {
        textConfig = view.findViewById(R.id.textConfig)
        textLoading = view.findViewById(R.id.textLoading)
        btnGenerate = view.findViewById(R.id.btnGenerate)
        btnCopy = view.findViewById(R.id.btnCopy)
        
        autoCompleteProtocol = view.findViewById(R.id.autoCompleteProtocol)
    }

    private fun setupMaterialDropdown() {
        val labelAll = getString(R.string.protocol_all)
        listProtocols = listOf(labelAll, "VMess", "VLESS", "Trojan", "Shadowsocks", "WireGuard", "Hysteria2")
        
        selectedProtocol = labelAll

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, listProtocols)
        
        autoCompleteProtocol.setAdapter(adapter)
        autoCompleteProtocol.setText(selectedProtocol, false) 

        autoCompleteProtocol.setOnItemClickListener { _, _, position, _ ->
            selectedProtocol = listProtocols[position]
        }
    }

    private fun setupButtonListeners() {
        btnGenerate.setOnClickListener {
            resetUI()
            val protocol = if (selectedProtocol.isNotEmpty()) selectedProtocol else autoCompleteProtocol.text.toString()
            fetchV2rayConfig(protocol)
        }

        btnCopy.setOnClickListener {
            copyConfigToClipboard()
        }
    }

    private fun resetUI() {
        textLoading.text = getString(R.string.label_loading)
        textConfig.text = ""
        currentConfig = ""
        currentPort = 443 
    }

    private fun copyConfigToClipboard() {
        val text = textConfig.text.toString()
        val context = context ?: return 
        
        if (text.isNotBlank() && !text.startsWith("No") && !text.startsWith("Error")) {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("V2Ray Config", text)
            clipboard.setPrimaryClip(clip)
            
            Toast.makeText(requireContext(), getString(R.string.toast_config_copied), Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), getString(R.string.toast_no_config), Toast.LENGTH_SHORT).show()
        }
    }

    private fun getPrefixForProtocol(protocolName: String): String {
        return when (protocolName) {
            "VMess" -> "vmess://"
            "VLESS" -> "vless://"
            "Trojan" -> "trojan://"
            "Shadowsocks" -> "ss://"
            "WireGuard" -> "wireguard://"
            "Hysteria2" -> "hysteria2://"
            else -> ""
        }
    }

    private fun fetchV2rayConfig(protocol: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            var finalConfig: String? = null
            var errorMsg: String = ""

            try {
                val rawContent = URL(PRIMARY_URL).openStream().bufferedReader().use { it.readText() }
                val decodedContent = tryDecodeSubscription(rawContent)
                val candidate = selectRandomConfigLine(decodedContent, protocol, isPrimary = true)
                
                if (!candidate.startsWith("Error") && !candidate.startsWith("No")) {
                    finalConfig = candidate
                }
            } catch (e: Exception) {
                errorMsg = e.localizedMessage ?: "Primary source error"
            }

            if (finalConfig == null) {
                try {

                    val fallbackUrl = "$FALLBACK_BASE_URL${(1..50).random()}.txt"
                    val content = URL(fallbackUrl).openStream().bufferedReader().use { it.readText() }
                    
                    finalConfig = selectRandomConfigLine(content, protocol, isPrimary = false)
                } catch (e: Exception) {
                    errorMsg = e.localizedMessage ?: "All sources failed"
                }
            }

            if (finalConfig != null && !finalConfig.startsWith("Error") && !finalConfig.startsWith("No")) {
                val serverPort = extractPortFromConfig(finalConfig)
                currentConfig = finalConfig
                currentPort = serverPort
                updateUI(finalConfig, currentPort)
            } else {
                val finalError = Exception(if (errorMsg.isNotEmpty()) errorMsg else "No config found for $protocol")
                handleConfigError(finalError)
            }
        }
    }

    private fun tryDecodeSubscription(content: String): String {
        val trimmed = content.trim()
        if (!trimmed.contains(" ") && !trimmed.contains("\n") && trimmed.length > 20) {
            return try {
                String(Base64.decode(trimmed, Base64.DEFAULT))
            } catch (e: Exception) {
                content
            }
        }
        return content
    }

    private fun selectRandomConfigLine(content: String, protocol: String, isPrimary: Boolean): String {
        val targetPrefix = getPrefixForProtocol(protocol)
        val labelAll = getString(R.string.protocol_all)
        
        val validLines = content.lines()
            .asSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .filter { line ->
                if (protocol == labelAll) {
                    PROTOCOL_PREFIXES.any { line.startsWith(it) }
                } else {
                    line.startsWith(targetPrefix)
                }
            }
            .toList()
        
        return if (validLines.isNotEmpty()) {
            validLines.random()
        } else {
            if (isPrimary) "No config in Primary" else getString(R.string.error_specific_protocol, protocol)
        }
    }

    private suspend fun updateUI(config: String, port: Int) {
        withContext(Dispatchers.Main) {
            textConfig.text = config
            if (port > 0) {
                val type = detectConfigType(config)
                textLoading.text = getString(R.string.status_format, type, port)
            } else {
                textLoading.text = getString(R.string.status_failed)
            }
        }
    }

    private fun handleConfigError(error: Exception) {
        lifecycleScope.launch(Dispatchers.Main) {
            textConfig.text = getString(R.string.error_prefix, error.localizedMessage)
            textLoading.text = getString(R.string.status_load_failed)
        }
    }

    private fun extractPortFromConfig(config: String): Int {
        val explicitPort = when {
            config.startsWith("vmess://") -> extractPortFromVmess(config)
            config.startsWith("vless://") -> extractPortFromVless(config)
            config.startsWith("ss://") -> extractPortFromShadowsocks(config)
            config.startsWith("trojan://") -> extractPortFromTrojan(config)
            config.startsWith("hysteria2://") -> extractPortFromHysteria(config)
            else -> extractPortFromText(config)
        }
        return explicitPort ?: extractPortFromText(config) ?: 443
    }

    private fun extractPortFromVmess(config: String): Int? {
        return try {
            val decoded = decodeBase64Safe(config.removePrefix("vmess://"))
            JSONObject(decoded).optString("port").toIntOrNull()
        } catch (e: Exception) { null }
    }

    private fun extractPortFromVless(config: String): Int? {
        return config.substringAfterLast(':').substringBefore('/').substringBefore('?').toIntOrNull()
    }

    private fun extractPortFromShadowsocks(config: String): Int? {
        val decoded = decodeBase64Safe(config.removePrefix("ss://").substringBefore('#'))
        return decoded.substringAfterLast(':').toIntOrNull()
    }

    private fun extractPortFromTrojan(config: String): Int? {
        return config.substringAfterLast(':').substringBefore('/').substringBefore('?').toIntOrNull()
    }

    private fun extractPortFromHysteria(config: String): Int? {
        return config.substringAfterLast(':').substringBefore('/').substringBefore('?').toIntOrNull()
    }

    private fun extractPortFromText(text: String): Int? {
        val portRegex = """:(\d+)(?=/|\?|$|#)""".toRegex()
        return portRegex.find(text)?.groups?.get(1)?.value?.toIntOrNull()
    }

    private fun decodeBase64Safe(encoded: String): String {
        return try {
            String(Base64.decode(encoded, Base64.NO_WRAP or Base64.URL_SAFE))
        } catch (e: Exception) {
            try {
                String(Base64.decode(encoded, Base64.DEFAULT))
            } catch (e2: Exception) {
                encoded
            }
        }
    }

    private fun detectConfigType(config: String): String {
        return when {
            config.startsWith("vmess://") -> "VMess"
            config.startsWith("vless://") -> "VLESS"
            config.startsWith("trojan://") -> "Trojan"
            config.startsWith("ss://") -> "Shadowsocks"
            config.startsWith("wireguard://") -> "WireGuard"
            config.startsWith("hysteria2://") -> "Hysteria2"
            else -> "Unknown"
        }
    }
}
