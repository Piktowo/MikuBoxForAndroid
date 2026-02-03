package com.neko.hostpotproxy.ui

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.RemoteException
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import io.nekohasekai.sagernet.R
import io.nekohasekai.sagernet.ui.ThemedActivity
import java.net.Inet4Address
import java.net.NetworkInterface

class ProxySettings : ThemedActivity() {

    private var btnToggle: MaterialButton? = null
    private var btnCopy: MaterialButton? = null
    private var tvInfo: TextView? = null
    private var tvIp: TextView? = null

    private var proxyControl: IProxyControl? = null
    private var isRunning = false

    companion object {
        const val KEY_PREFS = "proxy_pref"
        const val KEY_ENABLE = "proxy_enable"
    }

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            proxyControl = IProxyControl.Stub.asInterface(service)
            updateStatus()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            proxyControl = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.uwu_hotspot_proxy)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setTitle(R.string.hotspot_proxy_title)
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.baseline_arrow_back_24)
        }

        tvInfo = findViewById(R.id.tv_info)
        tvIp = findViewById(R.id.tv_ip_info)
        btnCopy = findViewById(R.id.btn_copy)
        btnToggle = findViewById(R.id.start_toggle)

        // Tampilkan IP saat pertama kali buka
        refreshIpDisplay()

        btnCopy?.setOnClickListener {
            val currentIp = tvIp?.text.toString()
            copyToClipboard(currentIp)
        }

        btnToggle?.setOnClickListener {
            toggleProxyState()
        }

        val intent = Intent(this, ProxyService::class.java)
        bindService(intent, connection, Context.BIND_AUTO_CREATE)

        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshIpDisplay()
    }

    private fun toggleProxyState() {
        if (proxyControl == null) {
            Toast.makeText(this, R.string.service_not_connected, Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val targetStateStart = !isRunning
            val intent = Intent(this, ProxyService::class.java)

            if (targetStateStart) {
                if (Build.VERSION.SDK_INT >= 26) {
                    startForegroundService(intent)
                } else {
                    startService(intent)
                }

                val success = proxyControl?.start() == true

                if (success) {
                    Toast.makeText(this, R.string.proxy_started, Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, R.string.proxy_start_failed, Toast.LENGTH_SHORT).show()
                    stopService(intent)
                }

            } else {
                proxyControl?.stop()
                stopService(intent)
                Toast.makeText(this, R.string.proxy_stopped, Toast.LENGTH_SHORT).show()
            }

            val prefs = getSharedPreferences(KEY_PREFS, MODE_PRIVATE)
            prefs.edit().putBoolean(KEY_ENABLE, targetStateStart).apply()

            updateStatus()

        } catch (e: RemoteException) {
            e.printStackTrace()
            val errorMsg = getString(R.string.error_prefix, e.message)
            Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateStatus() {
        try {
            isRunning = proxyControl?.isRunning() == true
            val port = proxyControl?.getPort() ?: 0

            if (isRunning) {
                tvInfo?.text = getString(R.string.status_running_port, port)
                btnToggle?.setText(R.string.btn_stop)
                refreshIpDisplay()
            } else {
                tvInfo?.setText(R.string.status_stopped)
                btnToggle?.setText(R.string.btn_start)
            }
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }

    private fun refreshIpDisplay() {
        val ip = getLocalIpAddress()
        tvIp?.text = ip
    }

    private fun copyToClipboard(text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val label = getString(R.string.proxy_ip_label)
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)

        Toast.makeText(this, R.string.ip_copied_toast, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        try {
            unbindService(connection)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        super.onDestroy()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    private fun getLocalIpAddress(): String {
        var bestCandidate = "127.0.0.1"

        try {
            val en = NetworkInterface.getNetworkInterfaces()
            while (en.hasMoreElements()) {
                val intf = en.nextElement()

                if (intf.isLoopback || !intf.isUp) continue

                val name = intf.name.lowercase()
                
                if (name.contains("tun")) continue

                val isLikelyHotspot = name.contains("ap") || name.contains("rndis") || name.contains("wlan")

                val enumIp = intf.inetAddresses
                while (enumIp.hasMoreElements()) {
                    val inet = enumIp.nextElement()
                    if (!inet.isLoopbackAddress && inet is Inet4Address) {
                        val ip = inet.hostAddress ?: continue
                        
                        if (isLikelyHotspot) {
                            return ip
                        }
                        
                        bestCandidate = ip
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return bestCandidate
    }
}
