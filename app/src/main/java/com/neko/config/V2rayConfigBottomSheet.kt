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
    private lateinit var editLimit: EditText
    private lateinit var autoCompleteProtocol: AutoCompleteTextView 

    private var currentConfig: String = ""
    private var selectedProtocol: String = ""
    private var listProtocols: List<String> = emptyList()

    companion object {
        const val TAG = "V2rayConfigBottomSheet"
        
        // Base URL Primary
        private const val PRIMARY_BASE_URL = "https://www.afrcloud.site/api/subscription/v2ray?type=mix&domain=all&tls=true"
        
        // Fallback URLs
        private const val FALLBACK_BASE_URL = "https://raw.githubusercontent.com/barry-far/V2ray-Config/refs/heads/main/Sub"
        private const val FALLBACK_SECONDARY_URL = "https://raw.githubusercontent.com/Epodonios/v2ray-configs/refs/heads/main/All_Configs_Sub.txt"
        private const val FALLBACK_TERTIARY_URL = "https://raw.githubusercontent.com/MatinGhanbari/v2ray-configs/main/subscriptions/v2ray/all_sub.txt"

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
        editLimit = view.findViewById(R.id.editLimit)
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
            
            val limitStr = editLimit.text.toString()
            val limit = limitStr.toIntOrNull()?.coerceAtLeast(1) ?: 1
            
            fetchV2rayConfig(protocol, limit)
        }

        btnCopy.setOnClickListener {
            copyConfigToClipboard()
        }
    }

    private fun resetUI() {
        textLoading.text = getString(R.string.label_loading)
        textConfig.text = ""
        currentConfig = ""
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

    private fun fetchV2rayConfig(protocol: String, limit: Int) {
        lifecycleScope.launch(Dispatchers.IO) {
            var finalResult: String? = null
            var errorMsg: String = ""

            // 1. Primary
            try {
                val requestLimit = if (limit < 10) 10 else limit
                val primaryUrl = "$PRIMARY_BASE_URL&limit=$requestLimit"
                val rawContent = URL(primaryUrl).openStream().bufferedReader().use { it.readText() }
                val decodedContent = tryDecodeSubscription(rawContent)
                finalResult = selectConfigs(decodedContent, protocol, limit)
            } catch (e: Exception) {
                errorMsg = "Primary: ${e.message}"
            }

            // 2. Fallback 1
            if (finalResult == null) {
                try {
                    val fallbackUrl = "$FALLBACK_BASE_URL${(1..50).random()}.txt"
                    val content = URL(fallbackUrl).openStream().bufferedReader().use { it.readText() }
                    finalResult = selectConfigs(content, protocol, limit)
                } catch (e: Exception) {
                    errorMsg = "Fallback 1: ${e.message}"
                }
            }

            // 3. Fallback 2
            if (finalResult == null) {
                try {
                    val content = URL(FALLBACK_SECONDARY_URL).openStream().bufferedReader().use { it.readText() }
                    val decodedContent = tryDecodeSubscription(content)
                    finalResult = selectConfigs(decodedContent, protocol, limit)
                } catch (e: Exception) {
                    errorMsg = "Fallback 2: ${e.message}"
                }
            }

            // 4. Fallback 3
            if (finalResult == null) {
                try {
                    val content = URL(FALLBACK_TERTIARY_URL).openStream().bufferedReader().use { it.readText() }
                    val decodedContent = tryDecodeSubscription(content)
                    finalResult = selectConfigs(decodedContent, protocol, limit)
                } catch (e: Exception) {
                    errorMsg = "All sources failed"
                }
            }

            if (finalResult != null) {
                currentConfig = finalResult!!
                val count = finalResult!!.lines().filter { it.isNotBlank() }.count()
                updateUI(finalResult!!, count)
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

    private fun selectConfigs(content: String, protocol: String, limit: Int): String? {
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
        
        if (validLines.isEmpty()) return null

        return validLines.shuffled()
            .take(limit)
            .joinToString("\n")
    }

    private suspend fun updateUI(config: String, count: Int) {
        withContext(Dispatchers.Main) {
            textConfig.text = config
            if (count > 0) {
                if (count == 1) {
                    val port = extractPortFromConfig(config)
                    val type = detectConfigType(config)
                    textLoading.text = getString(R.string.status_format, type, port)
                } else {
                    textLoading.text = getString(R.string.status_generated_success, count)
                }
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
        return explicitPort ?: extractPortFromText(config) ?: 0
    }

    private fun extractPortFromVmess(config: String): Int? {
        return try {
            val decoded = decodeBase64Safe(config.removePrefix("vmess://"))
            JSONObject(decoded).optString("port").toIntOrNull()
        } catch (e: Exception) { null }
    }

    private fun extractPortFromVless(config: String): Int? {
        return try {
            val uriPart = config.substringAfterLast('@').substringBefore('?')
            uriPart.substringAfterLast(':').toIntOrNull()
        } catch (e: Exception) { null }
    }

    private fun extractPortFromShadowsocks(config: String): Int? {
        return try {
            val raw = config.removePrefix("ss://")
            val mainPart = if (raw.contains("#")) raw.substringBefore("#") else raw
            if (mainPart.contains("@")) {
                mainPart.substringAfterLast(":").toIntOrNull()
            } else {
                val decoded = decodeBase64Safe(mainPart)
                decoded.substringAfterLast(':').toIntOrNull()
            }
        } catch (e: Exception) { null }
    }

    private fun extractPortFromTrojan(config: String): Int? {
         return try {
            val uriPart = config.substringAfterLast('@').substringBefore('?')
            uriPart.substringAfterLast(':').toIntOrNull()
        } catch (e: Exception) { null }
    }

    private fun extractPortFromHysteria(config: String): Int? {
        return try {
            val uriPart = config.substringAfterLast('@').substringBefore('?')
            uriPart.substringAfterLast(':').toIntOrNull()
        } catch (e: Exception) { null }
    }

    private fun extractPortFromText(text: String): Int? {
        val portRegex = """:(\d{2,5})(?=/|\?|$|#)""".toRegex()
        val match = portRegex.find(text)
        return match?.groups?.get(1)?.value?.toIntOrNull()
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
