package com.neko.ip.hostchecker

import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.View
import android.widget.*
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputLayout
import io.nekohasekai.sagernet.R
import io.nekohasekai.sagernet.ui.ThemedActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.*

class HostChecker : ThemedActivity() {
    
    private lateinit var listLogs: ListView
    private lateinit var bugHost: EditText
    private lateinit var proxyInput: EditText
    private lateinit var inputLayoutProxy: TextInputLayout 
    private lateinit var spinnerMethod: AutoCompleteTextView 
    private lateinit var spinnerType: AutoCompleteTextView   
    private lateinit var btnSubmit: Button
    private lateinit var adapter: ArrayAdapter<String>
    private lateinit var arrayList: ArrayList<String>
    private lateinit var sp: SharedPreferences
    
    private var ipProxy: String = ""
    private var portProxy: String = "8080"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.uwu_activity_hostchecker)

        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.apply {
            setTitle(R.string.hostchecker_title)
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.baseline_arrow_back_24)
        }

        sp = PreferenceManager.getDefaultSharedPreferences(this)

        listLogs = findViewById(R.id.listLogs)
        bugHost = findViewById(R.id.editTextUrl)
        proxyInput = findViewById(R.id.editTextProxy)
        inputLayoutProxy = findViewById(R.id.inputLayoutProxy)   
        spinnerMethod = findViewById(R.id.spinnerRequestMethod)
        spinnerType = findViewById(R.id.spinnerRequestType)
        btnSubmit = findViewById(R.id.buttonSearch)

        val requestMethods = arrayOf("GET", "POST", "HEAD", "OPTIONS", "PUT", "DELETE", "CONNECT", "TRACE", "PATCH")
        val adapterMethod = ArrayAdapter(this, android.R.layout.simple_list_item_1, requestMethods)
        spinnerMethod.setAdapter(adapterMethod)
        spinnerMethod.setText("GET", false)

        val modeDirectStr = getString(R.string.hostchecker_mode_direct)
        val modeProxyStr = getString(R.string.hostchecker_mode_proxy)
        val requestModes = arrayOf(modeDirectStr, modeProxyStr)
        
        val adapterMode = ArrayAdapter(this, android.R.layout.simple_list_item_1, requestModes)
        spinnerType.setAdapter(adapterMode)

        val isDirect = sp.getBoolean("Xen", true)
        
        if (isDirect) {
            spinnerType.setText(modeDirectStr, false)
            inputLayoutProxy.visibility = View.GONE 
        } else {
            spinnerType.setText(modeProxyStr, false)
            inputLayoutProxy.visibility = View.VISIBLE 
        }

        spinnerType.setOnItemClickListener { _, _, position, _ ->
            if (position == 0) {
                sp.edit().putBoolean("Xen", true).apply()
                inputLayoutProxy.visibility = View.GONE
            } else {
                sp.edit().putBoolean("Xen", false).apply()
                inputLayoutProxy.visibility = View.VISIBLE
            }
        }

        arrayList = ArrayList()
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, arrayList)
        listLogs.adapter = adapter

        btnSubmit.setOnClickListener {
            val host = bugHost.text.toString().trim()
            val proxyText = proxyInput.text.toString().trim()
            
            val isDirectMode = spinnerType.text.toString() == getString(R.string.hostchecker_mode_direct)

            if (host.isEmpty()) {
                Toast.makeText(this, R.string.hostchecker_url, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!isDirectMode && proxyText.isEmpty()) {
                Toast.makeText(this, R.string.hostchecker_proxy, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            startCheck(host, proxyText, isDirectMode)
        }
    }

    override fun onResume() {
        super.onResume()
        bugHost.setText(sp.getString("hostChecker", ""))
        proxyInput.setText(sp.getString("proxyChecker", ""))
    }

    override fun onPause() {
        super.onPause()
        sp.edit().apply {
            putString("hostChecker", bugHost.text.toString().trim())
            putString("proxyChecker", proxyInput.text.toString().trim())
            apply()
        }
    }

    private fun showMessage(str: String) {
    }

    private fun startCheck(host: String, proxyText: String, isDirectRequest: Boolean) {
        btnSubmit.isEnabled = false
        
        val method = spinnerMethod.text.toString()

        if (!isDirectRequest) {
            if (proxyText.contains(":")) {
                val split = proxyText.split(":")
                ipProxy = split[0]
                portProxy = if (split.size > 1 && split[1].isNotEmpty()) split[1] else "8080"
            } else {
                ipProxy = proxyText
                portProxy = "8080"
            }
        }

        arrayList.add("====================================")
        arrayList.add("$method - URL: http://$host")
        
        if (isDirectRequest) {
            arrayList.add(getString(R.string.hostchecker_log_mode_direct))
        } else {
            arrayList.add(getString(R.string.hostchecker_log_mode_proxy, ipProxy, portProxy))
        }
        
        adapter.notifyDataSetChanged()
        listLogs.setSelection(adapter.count - 1)

        lifecycleScope.launch(Dispatchers.IO) {
            val sbResponse = StringBuilder()
            var errorMsg: String? = null
            var responseCodeStr = ""

            try {
                val url = URL("http://$host")
                val conn: HttpURLConnection = if (isDirectRequest) {
                    url.openConnection() as HttpURLConnection
                } else {
                    val proxy = Proxy(Proxy.Type.HTTP, InetSocketAddress(ipProxy, portProxy.toInt()))
                    url.openConnection(proxy) as HttpURLConnection
                }

                conn.apply {
                    requestMethod = method
                    setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                    readTimeout = 10000
                    connectTimeout = 10000
                    doInput = true
                }

                val code = conn.responseCode
                val msg = conn.responseMessage
                responseCodeStr = "HTTP/1.1 $code $msg"
                
                sbResponse.append("$responseCodeStr\n")

                for ((key, value) in conn.headerFields) {
                    if (key != null) {
                        sbResponse.append("$key: ${value.joinToString(", ")}\n")
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
                errorMsg = e.message ?: e.toString()
            }

            withContext(Dispatchers.Main) {
                if (errorMsg != null) {
                    arrayList.add(getString(R.string.hostchecker_log_error, errorMsg))
                    arrayList.add(getString(R.string.hostchecker_log_stopped))
                } else {
                    val lines = sbResponse.toString().split("\n")
                    for (line in lines) {
                        if (line.isNotEmpty()) arrayList.add(line)
                    }
                    
                    arrayList.add("")
                    arrayList.add(getString(R.string.hostchecker_log_stopped))
                    
                    if (responseCodeStr.contains("200")) {
                        showMessage(getString(R.string.hostchecker_success))
                    } else {
                        showMessage(getString(R.string.hostchecker_complete))
                    }
                }
                
                adapter.notifyDataSetChanged()
                listLogs.setSelection(adapter.count - 1)
                btnSubmit.isEnabled = true
            }
        }
    }
}
