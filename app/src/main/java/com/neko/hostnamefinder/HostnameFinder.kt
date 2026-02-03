package com.neko.hostnamefinder

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import android.util.Base64
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.lifecycle.lifecycleScope
import io.nekohasekai.sagernet.R
import io.nekohasekai.sagernet.ui.ThemedActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URL

class HostnameFinder : ThemedActivity() {

    private lateinit var edtHost: EditText
    private lateinit var btnSearch: Button
    private lateinit var txtResults: TextView
    private lateinit var txtResultTitle: TextView
    private lateinit var btnCopy: Button
    private lateinit var autoCompleteSource: AutoCompleteTextView

    private val sources = listOf(
        "Anubis",
        "YouGetSignal",
        "HackerTarget",
        "Local DNS",
        "VirusTotal"
    )

    private val virusTotalApiKey = "NjhlMDUzYjc5NmI3OWE1YzBiNDczYWJhZDFjNTVkYWY2ZWRlNGU4M2VjMWNkZmMwNTUxYTVhODRkOGEyZjIzMw=="
    
    private val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.uwu_hostname_finder)
        
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.apply {
            setTitle(R.string.hostname_finder)
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.baseline_arrow_back_24)
        }

        edtHost = findViewById(R.id.edtHost)
        btnSearch = findViewById(R.id.btnSearch)
        txtResults = findViewById(R.id.txtResults)
        txtResultTitle = findViewById(R.id.txtResultTitle)
        btnCopy = findViewById(R.id.btnCopy)
        autoCompleteSource = findViewById(R.id.autoCompleteSource)

        txtResults.isSingleLine = false
        txtResults.maxLines = Int.MAX_VALUE
        txtResults.setTextIsSelectable(true)

        setupDropdown()

        btnSearch.setOnClickListener { performSearch() }

        edtHost.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch()
                true
            } else false
        }

        btnCopy.setOnClickListener {
            val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("hostnames", txtResults.text)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, getString(R.string.hf_copied), Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupDropdown() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, sources)
        autoCompleteSource.setAdapter(adapter)
        autoCompleteSource.setText(sources[0], false)
        autoCompleteSource.setOnClickListener { autoCompleteSource.showDropDown() }
    }

    private fun performSearch() {
        val host = edtHost.text.toString().trim()
        if (host.isEmpty()) {
            edtHost.error = getString(R.string.hf_error_input_host)
            return
        }

        txtResults.text = getString(R.string.hf_loading)
        txtResultTitle.text = getString(R.string.hf_result_loading)

        val selectedSource = autoCompleteSource.text.toString()

        when (selectedSource) {
        	"Anubis" -> fetchFromAnubis(host)
            "YouGetSignal" -> fetchFromYouGetSignal(host)
            "HackerTarget" -> fetchFromHackerTarget(host)
            "Local DNS" -> searchWithDns(host)
            "VirusTotal" -> fetchFromVirusTotal(host)
            else -> fetchFromAnubis(host)
        }
    }

    private fun getRequest(urlString: String): String {
        val url = URL(urlString)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.connectTimeout = 20000 
        conn.readTimeout = 20000
        conn.setRequestProperty("User-Agent", USER_AGENT) 
        conn.setRequestProperty("Accept", "application/json, text/html, */*")
        
        return conn.inputStream.bufferedReader().use { it.readText() }
    }

    private fun fetchFromYouGetSignal(domain: String) {
        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    val url = URL("https://domains.yougetsignal.com/domains.php")
                    val conn = url.openConnection() as HttpURLConnection
                    conn.requestMethod = "POST"
                    conn.doOutput = true
                    conn.setRequestProperty("User-Agent", USER_AGENT)
                    conn.outputStream.bufferedWriter().use { it.write("remoteAddress=$domain") }
                    conn.inputStream.bufferedReader().use { it.readText() }
                }

                val json = JSONObject(response)
                
                if (json.has("domainArray")) {
                    val pairs = json.getJSONArray("domainArray")
                    val domains = (0 until pairs.length()).map { i ->
                        pairs.getJSONArray(i).getString(0)
                    }
                    displayResults(domains.sorted(), R.string.hf_result_found_domains)
                } else {
                    displayResults(emptyList(), R.string.hf_result_found_domains)
                }
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }

    private fun fetchFromAnubis(domain: String) {
        lifecycleScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    getRequest("https://jldc.me/anubis/subdomains/$domain")
                }
                val jsonArray = JSONArray(result)
                val subdomains = (0 until jsonArray.length()).map { i ->
                    jsonArray.getString(i)
                }.distinct()

                displayResults(subdomains.sorted(), R.string.hf_result_found_subdomains)
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }

    private fun fetchFromHackerTarget(domain: String) {
        lifecycleScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    getRequest("https://api.hackertarget.com/hostsearch/?q=$domain")
                }
                val hostnames = result.lines()
                    .filter { it.contains(",") }
                    .map { line -> line.split(",")[1] }
                    .distinct()

                displayResults(hostnames.sorted(), R.string.hf_result_found_hostnames)
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }

    private fun fetchFromVirusTotal(domain: String) {
        lifecycleScope.launch {
            try {
                val apiKey = String(Base64.decode(virusTotalApiKey, Base64.DEFAULT))

                val result = withContext(Dispatchers.IO) {
                    val url = "https://www.virustotal.com/api/v3/domains/$domain/subdomains?limit=40"
                    val conn = URL(url).openConnection() as HttpURLConnection
                    conn.setRequestProperty("x-apikey", apiKey)
                    conn.setRequestProperty("User-Agent", USER_AGENT)
                    conn.inputStream.bufferedReader().use { it.readText() }
                }

                val data = JSONObject(result).getJSONArray("data")
                val subdomains = (0 until data.length()).map { i ->
                    data.getJSONObject(i).getString("id")
                }

                displayResults(subdomains.sorted(), R.string.hf_result_found_subdomains)
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }

    private fun searchWithDns(domain: String) {
        lifecycleScope.launch {
            try {
                val addresses = withContext(Dispatchers.IO) {
                    InetAddress.getAllByName(domain).map { it.hostAddress }.distinct()
                }
                displayResults(addresses, R.string.hf_result_found_ips)
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }

    private fun displayResults(items: List<String>, titleResId: Int) {
        if (items.isEmpty()) {
            txtResults.text = "No results found."
            txtResultTitle.text = getString(titleResId, 0)
        } else {
            txtResults.text = items.joinToString("\n")
            txtResultTitle.text = getString(titleResId, items.size)
        }
    }

    private fun handleError(e: Exception) {
        txtResults.text = getString(R.string.hf_error_generic, e.localizedMessage)
        txtResultTitle.text = getString(R.string.hf_result_error)
        e.printStackTrace()
    }
}
