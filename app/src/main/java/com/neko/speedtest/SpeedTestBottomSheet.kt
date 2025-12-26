package com.neko.speedtest

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import com.google.android.material.card.MaterialCardView
import com.github.anastr.speedviewlib.PointerSpeedometer
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.bottomsheet.BottomSheetBehavior
import io.nekohasekai.sagernet.R
import kotlinx.coroutines.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
import kotlin.math.abs

class SpeedTestBottomSheet : BottomSheetDialogFragment() {

    private lateinit var speedometer: PointerSpeedometer
    private lateinit var textDownload: TextView
    private lateinit var textUpload: TextView
    private lateinit var textPing: TextView
    private lateinit var textJitter: TextView
    
    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    
    private lateinit var cardDownload: MaterialCardView
    private lateinit var cardUpload: MaterialCardView
    private lateinit var cardPing: MaterialCardView
    private lateinit var cardJitter: MaterialCardView

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var testJob: Job? = null
    
    private val TEST_DURATION = 15.0 

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS) 
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    private val dummyData by lazy { ByteArray(1024 * 1024) }

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
        textPing = view.findViewById(R.id.textPing)
        textJitter = view.findViewById(R.id.textJitter)

        startButton = view.findViewById(R.id.startButton)
        stopButton = view.findViewById(R.id.stopButton)
        
        cardDownload = view.findViewById(R.id.cardDownload)
        cardUpload = view.findViewById(R.id.cardUpload)
        cardPing = view.findViewById(R.id.cardPing)
        cardJitter = view.findViewById(R.id.cardJitter)
        
        startButton.setOnClickListener { startFullSpeedTest() }
        stopButton.setOnClickListener { stopSpeedTest() }

        cardDownload.setOnClickListener {
            if (testJob?.isActive == true) return@setOnClickListener
            runIndividualTest(isDownload = true)
        }

        cardUpload.setOnClickListener {
            if (testJob?.isActive == true) return@setOnClickListener
            runIndividualTest(isDownload = false)
        }

        cardPing.setOnClickListener {
            if (testJob?.isActive == true) return@setOnClickListener
            runPingOnlyTest()
        }

        cardJitter.setOnClickListener {
            if (testJob?.isActive == true) return@setOnClickListener
            runJitterOnlyTest()
        }
        
        setDefaultText()
        resetButtonState()
        
        scope.launch(Dispatchers.IO) {
            dummyData.fill(1) 
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
    
    private fun setDefaultText() {
        textDownload.setText(R.string.st_test_download)
        textUpload.setText(R.string.st_test_upload)
        textPing.setText(R.string.st_test_ping)
        textJitter.setText(R.string.st_test_jitter)
    }

    private fun prepareUIForTest() {
        startButton.visibility = View.GONE
        stopButton.visibility = View.VISIBLE
    }

    private fun resetButtonState() {
        if (view == null) return      
        startButton.visibility = View.VISIBLE
        stopButton.visibility = View.GONE   
        speedometer.visibility = View.VISIBLE 
        speedometer.alpha = 1f
        speedometer.speedTo(0f)
    }

    private fun stopSpeedTest() {
        testJob?.cancel("Test stopped by user")
        testJob = null
        setDefaultText()
        resetButtonState()
    }

    private suspend fun performLatencyCheck(iterations: Int): List<Long> = withContext(Dispatchers.IO) {
        val targetUrl = "https://www.gstatic.com/generate_204"
        val results = mutableListOf<Long>()

        for (i in 0 until iterations) {
            if (!isActive) break
            
            val start = System.nanoTime()
            try {
                val request = Request.Builder()
                    .url(targetUrl)
                    .header("Connection", "close")
                    .header("Cache-Control", "no-cache")
                    .build()

                client.newCall(request).execute().use { response ->
                    val end = System.nanoTime()
                    if (response.isSuccessful || response.code == 204) {
                        val latencyMs = (end - start) / 1_000_000
                        results.add(latencyMs)
                    }
                }
            } catch (e: Exception) {
            }
            delay(100)
        }
        return@withContext results
    }

    private fun calculateJitter(latencies: List<Long>): Long {
        if (latencies.size < 2) return 0L
        var diffSum = 0L
        for (i in 0 until latencies.size - 1) {
            diffSum += abs(latencies[i] - latencies[i+1])
        }
        return diffSum / (latencies.size - 1)
    }

    private fun runPingOnlyTest() {
        if (!isInternetAvailable()) {
            return
        }
        prepareUIForTest()
        
        textPing.setText(R.string.st_status_testing)
        
        speedometer.speedTo(0f)

        testJob = scope.launch {
            try {
                val latencies = performLatencyCheck(5)
                withContext(Dispatchers.Main) {
                    if (latencies.isNotEmpty()) {
                        textPing.text = "${latencies.average().toLong()} ms"
                    } else {
                        textPing.text = "Error"
                    }
                }
            } catch (e: Exception) {
                if (isAdded) textPing.text = "Error"
            } finally {
                withContext(Dispatchers.Main) { if (isAdded) resetButtonState() }
            }
        }
    }

    private fun runJitterOnlyTest() {
        if (!isInternetAvailable()) {
            return
        }
        prepareUIForTest()
        
        textJitter.setText(R.string.st_status_testing)
        
        speedometer.speedTo(0f)

        testJob = scope.launch {
            try {
                val latencies = performLatencyCheck(5)
                withContext(Dispatchers.Main) {
                    if (latencies.size >= 2) {
                        textJitter.text = "${calculateJitter(latencies)} ms"
                    } else {
                        textJitter.text = "N/A"
                    }
                }
            } catch (e: Exception) {
                if (isAdded) textJitter.text = "Error"
            } finally {
                withContext(Dispatchers.Main) { if (isAdded) resetButtonState() }
            }
        }
    }

    private fun runIndividualTest(isDownload: Boolean) {
        if (!isInternetAvailable()) {
            return
        }

        prepareUIForTest()

        testJob = scope.launch {
            try {
                if (isDownload) {
                    textDownload.setText(R.string.st_status_running)
                    speedometer.speedTo(0f)
                    
                    val downloadSpeed = measureDownloadSpeed()
                    textDownload.text = getString(R.string.st_format_mbps, downloadSpeed)
                    speedometer.speedTo(downloadSpeed.toFloat())
                } else {
                    textUpload.setText(R.string.st_status_running)
                    speedometer.speedTo(0f)

                    val uploadSpeed = measureUploadSpeed()
                    textUpload.text = getString(R.string.st_format_mbps, uploadSpeed)
                    speedometer.speedTo(uploadSpeed.toFloat())
                }
            } catch (e: Exception) {
            } finally {
                withContext(Dispatchers.Main) {
                    if (isAdded) resetButtonState()
                }
            }
        }
    }

    private fun startFullSpeedTest() {
        if (!isInternetAvailable()) {
            return
        }
        
        prepareUIForTest()
        
        textDownload.setText(R.string.st_status_testing)
        textUpload.setText(R.string.st_status_waiting) 
        textPing.setText(R.string.st_status_testing)
        textJitter.setText(R.string.st_status_testing)
        
        speedometer.speedTo(0f)

        testJob = scope.launch {
            try {
                val latencies = performLatencyCheck(5)
                
                withContext(Dispatchers.Main) {
                    if (latencies.isNotEmpty()) {
                        val avgPing = latencies.average().toLong()
                        textPing.text = "$avgPing ms"
                        
                        val jitter = calculateJitter(latencies)
                        textJitter.text = "$jitter ms"
                    } else {
                        textPing.text = "N/A"
                        textJitter.text = "N/A"
                    }
                }
                
                delay(500)

                textDownload.setText(R.string.st_status_running)
                val downloadSpeed = measureDownloadSpeed()
                textDownload.text = getString(R.string.st_format_mbps, downloadSpeed)
                speedometer.speedTo(downloadSpeed.toFloat())           
                
                delay(1000)
                speedometer.speedTo(0f)      
                delay(800)

                textUpload.setText(R.string.st_status_running)
                val uploadSpeed = measureUploadSpeed()
                textUpload.text = getString(R.string.st_format_mbps, uploadSpeed)
                speedometer.speedTo(uploadSpeed.toFloat())
                
                delay(1000)

            } catch (e: CancellationException) {
            } catch (e: Exception) {
            } finally {
                withContext(Dispatchers.Main) {
                    if (isAdded) resetButtonState()
                }
            }
        }
    }

    private suspend fun measureDownloadSpeed(): Double = withContext(Dispatchers.IO) {
        val urls = listOf(
            "https://dl.google.com/android/studio/install/android-studio-ide-2023.3.1.20-windows.exe",
            "https://download.mozilla.org/?product=firefox-latest-ssl&os=win64&lang=en-US",
            "https://github.com/topjohnwu/Magisk/releases/download/canary-28103/app-release.apk"
        )

        var totalBytes = 0L
        var speedMbps = 0.0
        var currentUrlIndex = 0

        val startTime = System.nanoTime()
        var lastUpdateTime = System.currentTimeMillis()

        while (isActive) {
            val elapsedSeconds = (System.nanoTime() - startTime) / 1_000_000_000.0
            if (elapsedSeconds > TEST_DURATION) break
            
            if (currentUrlIndex >= urls.size) break

            val currentUrl = urls[currentUrlIndex]

            try {
                val request = Request.Builder()
                    .url(currentUrl)
                    .header("Cache-Control", "no-cache")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) throw java.io.IOException("HTTP ${response.code}")

                    val body = response.body ?: throw java.io.IOException("Body null")
                    val stream = body.byteStream()
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    
                    while (isActive) {
                        bytesRead = stream.read(buffer)
                        if (bytesRead == -1) break

                        totalBytes += bytesRead

                        val innerElapsed = (System.nanoTime() - startTime) / 1_000_000_000.0
                        if (innerElapsed > TEST_DURATION) break 

                        val now = System.currentTimeMillis()
                        if (now - lastUpdateTime > 200) {
                            speedMbps = (totalBytes * 8) / (innerElapsed * 1_000_000.0)
                            withContext(Dispatchers.Main) {
                                if (isAdded) speedometer.speedTo(speedMbps.toFloat())
                            }
                            lastUpdateTime = now
                        }
                    }
                }
            } catch (e: Exception) {
                currentUrlIndex++
                delay(100)
            }
            if (currentUrlIndex < urls.size && (System.nanoTime() - startTime) / 1_000_000_000.0 < TEST_DURATION) {
            }
        }
        
        return@withContext speedMbps
    }


    private suspend fun measureUploadSpeed(): Double = withContext(Dispatchers.IO) {
        val uploadUrls = listOf(
            "https://httpbun.com/post",
            "https://postman-echo.com/post",
            "https://httpbin.org/post"
        )

        val mediaType = "application/octet-stream".toMediaType()
        val payloadSize = dummyData.size.toLong()

        var totalBytesUploaded = 0L
        var speedMbps = 0.0
        var currentUrlIndex = 0

        val startTime = System.nanoTime()
        var lastUpdateTime = System.currentTimeMillis()

        while (isActive) {
            val elapsedSeconds = (System.nanoTime() - startTime) / 1_000_000_000.0
            if (elapsedSeconds > TEST_DURATION) break
            
            if (currentUrlIndex >= uploadUrls.size) break

            val currentUrl = uploadUrls[currentUrlIndex]

            try {
                val requestBody = dummyData.toRequestBody(mediaType, 0, dummyData.size)
                val request = Request.Builder()
                    .url(currentUrl)
                    .post(requestBody)
                    .header("Cache-Control", "no-cache")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        totalBytesUploaded += payloadSize
                    } else {
                        throw java.io.IOException("HTTP ${response.code}")
                    }
                }

                val now = System.currentTimeMillis()
                if (now - lastUpdateTime > 200) {
                    val currentElapsed = (System.nanoTime() - startTime) / 1_000_000_000.0
                    if (currentElapsed > 0) {
                        speedMbps = (totalBytesUploaded * 8) / (currentElapsed * 1_000_000.0)
                        withContext(Dispatchers.Main) {
                            if (isAdded) speedometer.speedTo(speedMbps.toFloat())
                        }
                        lastUpdateTime = now
                    }
                }

            } catch (e: Exception) {
                currentUrlIndex++
                delay(100)
            }
        }
        
        return@withContext speedMbps
    }

    override fun onDestroyView() {
        super.onDestroyView()
        testJob?.cancel()
        scope.cancel()
    }
}
