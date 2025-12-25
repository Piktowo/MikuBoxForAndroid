package com.neko.speedtest

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.github.anastr.speedviewlib.PointerSpeedometer
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.bottomsheet.BottomSheetBehavior
import io.nekohasekai.sagernet.R
import kotlinx.coroutines.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import kotlin.math.min

class SpeedTestBottomSheet : BottomSheetDialogFragment() {

    private lateinit var speedometer: PointerSpeedometer
    private lateinit var textDownload: TextView
    private lateinit var textUpload: TextView
    private lateinit var startButton: Button
    private lateinit var stopButton: Button

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var testJob: Job? = null
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    private val dummyData by lazy { ByteArray(2 * 1024 * 1024) }

    override fun onCreateView(
        inflater: LayoutInflater, 
        container: ViewGroup?, 
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.uwu_speedtest, container, false)
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

        speedometer = view.findViewById(R.id.speedometer)
        textDownload = view.findViewById(R.id.textDownload)
        textUpload = view.findViewById(R.id.textUpload)
        startButton = view.findViewById(R.id.startButton)
        stopButton = view.findViewById(R.id.stopButton)

        startButton.setOnClickListener { startSpeedTest() }
        stopButton.setOnClickListener { stopSpeedTest() }

        startButton.visibility = View.VISIBLE
        stopButton.visibility = View.GONE
        
        speedometer.visibility = View.VISIBLE 
        speedometer.alpha = 1f
        
        scope.launch(Dispatchers.IO) {
            dummyData.fill(0)
        }
    }

    private fun isInternetAvailable(): Boolean {
        val connectivityManager = requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)
        return capabilities != null && 
               (capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED))
    }

    private fun startSpeedTest() {
        if (!isInternetAvailable()) {
            Toast.makeText(requireContext(), "Please check your internet connection", Toast.LENGTH_SHORT).show()
            return
        }

        startButton.visibility = View.GONE
        stopButton.visibility = View.VISIBLE

        textDownload.text = "Testing..."
        textUpload.text = "Testing..."
        
        speedometer.speedTo(0f)

        testJob = scope.launch {
            try {
                val downloadSpeed = measureDownloadSpeed()
                textDownload.text = "%.2f Mbps".format(downloadSpeed)

                speedometer.speedTo(downloadSpeed.toFloat())
                delay(1000)

                val uploadSpeed = measureUploadSpeed()
                textUpload.text = "%.2f Mbps".format(uploadSpeed)

                speedometer.speedTo(uploadSpeed.toFloat())
                
                delay(2000)

            } catch (e: CancellationException) {
                Log.d("SpeedTest", "Test cancelled by user")
            } catch (e: Exception) {
                Log.e("SpeedTest", "Error during speed test: ${e.message}", e)
                if (isAdded) {
                    Toast.makeText(requireContext(), "Error: ${e.localizedMessage ?: "Unknown error"}", Toast.LENGTH_SHORT).show()
                }
                resetUI()
            } finally {
                withContext(Dispatchers.Main) {
                    if (isAdded) {
                        startButton.visibility = View.VISIBLE
                        stopButton.visibility = View.GONE
                        
                        speedometer.speedTo(0f)
                    }
                }
            }
        }
    }

    private fun stopSpeedTest() {
        testJob?.cancel("Test stopped by user")
        testJob = null
        resetUI()
    }

    private fun resetUI() {
        if (view == null) return
        
        startButton.visibility = View.VISIBLE
        stopButton.visibility = View.GONE
        
        speedometer.visibility = View.VISIBLE 
        speedometer.alpha = 1f
        speedometer.speedTo(0f)
    }

    private suspend fun measureDownloadSpeed(): Double = withContext(Dispatchers.IO) {
        val urls = listOf(
            "https://github.com/topjohnwu/Magisk/releases/download/canary-28103/app-release.apk",
            "https://download.mozilla.org/?product=firefox-latest-ssl&os=win64&lang=en-US",
            "https://dl.google.com/android/studio/install/android-studio-ide-2023.3.1.20-windows.exe"
        )

        var totalBytes = 0L
        var speedMbps = 0.0
        var successful = false

        for (url in urls) {
            if (isActive.not()) break

            try {
                val request = Request.Builder()
                    .url(url)
                    .header("Cache-Control", "no-cache")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body
                        if (body != null) {
                            val stream = body.byteStream()
                            val buffer = ByteArray(8192)
                            var bytesRead: Int
                            val startTime = System.nanoTime()
                            var lastUpdateTime = System.currentTimeMillis()

                            while (stream.read(buffer).also { bytesRead = it } != -1 && isActive) {
                                totalBytes += bytesRead

                                val now = System.currentTimeMillis()
                                if (now - lastUpdateTime > 300) {
                                    val elapsedSeconds = (System.nanoTime() - startTime) / 1_000_000_000.0
                                    speedMbps = (totalBytes * 8) / (elapsedSeconds * 1000 * 1000)
                                    
                                    withContext(Dispatchers.Main) {
                                        if (isAdded) speedometer.speedTo(speedMbps.toFloat())
                                    }
                                    lastUpdateTime = now
                                }

                                val elapsedTime = (System.nanoTime() - startTime) / 1_000_000_000.0
                                if (totalBytes > 10 * 1024 * 1024 || elapsedTime > 15.0) {
                                    break 
                                }
                            }
                            successful = true
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w("DownloadTest", "Failed with URL: $url, error: ${e.message}")
            }

            if (successful) break
        }
        return@withContext if (successful) speedMbps else 0.0
    }

    private suspend fun measureUploadSpeed(): Double = withContext(Dispatchers.IO) {
        val uploadUrls = listOf(
            "https://httpbin.org/post",
            "https://postman-echo.com/post",
            "https://httpbun.com/post"
        )

        val mediaType = "application/octet-stream".toMediaType()
        var speedMbps = 0.0
        var successful = false

        for (uploadUrl in uploadUrls) {
            if (isActive.not()) break

            try {
                val requestBody = object : RequestBody() {
                    override fun contentType() = mediaType
                    override fun contentLength() = dummyData.size.toLong()

                    override fun writeTo(sink: okio.BufferedSink) {
                        val chunkSize = 8192
                        var uploaded = 0L
                        val startTime = System.nanoTime()
                        var lastUpdate = System.currentTimeMillis()

                        while (uploaded < dummyData.size && isActive) {
                            val size = min(chunkSize, dummyData.size - uploaded.toInt())
                            sink.write(dummyData, uploaded.toInt(), size)
                            uploaded += size

                            val now = System.currentTimeMillis()
                            if (now - lastUpdate > 300) {
                                val elapsedSeconds = (System.nanoTime() - startTime) / 1_000_000_000.0
                                speedMbps = (uploaded * 8) / (elapsedSeconds * 1000 * 1000)
                                
                                scope.launch(Dispatchers.Main) {
                                   if (isAdded) speedometer.speedTo(speedMbps.toFloat())
                                }
                                lastUpdate = now
                            }

                            val elapsedTime = (System.nanoTime() - startTime) / 1_000_000_000.0
                            if (elapsedTime > 15.0) {
                                break
                            }
                        }
                    }
                }

                val request = Request.Builder()
                    .url(uploadUrl)
                    .post(requestBody)
                    .header("Cache-Control", "no-cache")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        successful = true
                    }
                }
            } catch (e: Exception) {
                Log.w("UploadTest", "Failed with URL: $uploadUrl, error: ${e.message}")
            }

            if (successful) break
        }
        return@withContext if (successful) speedMbps else 0.0
    }

    override fun onDestroyView() {
        super.onDestroyView()
        testJob?.cancel()
        scope.cancel()
    }
}
